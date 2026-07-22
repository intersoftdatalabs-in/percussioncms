#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Static assertion that the install/upgrade ANT script does not delete
integrator-supplied JDBC drivers via glob patterns.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Inspects ``modules/perc-distribution-tree/src/main/resources/distribution/
rxconfig/Installer/install.xml`` — specifically the ``<delete>`` block inside
the ``<target name="install_jdbc_drivers">`` element — and asserts that every
``<include name="...">`` value is an exact bundled-driver filename, not a glob.

Companion to ``InstallXmlDeleteSetTest`` (which does the same check via JUnit
XPath on the parsed XML). This script is the operator-facing cross-platform
entry point. The build-time gate is the canonical Java main
``com.percussion.distribution.install.CheckNoGlobDeletes`` invoked by
``exec-maven-plugin:java`` in ``modules/perc-distribution-tree/pom.xml``.

Exit codes (matching the original POSIX script and the Java main):

  0  ok — no glob-based ``<delete>`` patterns found
  1  invocation error / file missing
  7  one or more ``<include>`` entries inside ``install_jdbc_drivers
     <delete>`` are glob patterns (contain ``*`` or ``?``) — this is the
     failure the script exists to catch

For feature 002-jdbc-drivers-cleanup (FR-003, FR-008.b, SC-006).

## Behavioral Notes (FR-009b)

- The original POSIX script used ``awk``/``grep``/``sed`` to slice the
  ``<target name="install_jdbc_drivers">`` element and extract ``<include>``
  values via a regex. The Python port uses ``xml.etree.ElementTree``
  (stdlib) which is both more robust against edge-case whitespace and
  simpler to reason about — same observable behavior, no shell-quoting
  hazards.
- Glob detection is identical: any ``<include>`` whose name contains ``*``
  or ``?`` is a glob. ``fnmatch.translate`` is **not** used here because
  the shell-style bracket syntax (``[abc]``) is irrelevant — the original
  script's regex ``grep -E '[\\*\\?]'`` catches the same two characters.
- The path discovery for the default ``install.xml`` uses
  ``Path(__file__).resolve().parents[N]`` (R7); no hardcoded separators.
"""

from __future__ import annotations

import argparse
import logging
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable, List, Optional

LOG = logging.getLogger("check-no-glob-deletes")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_GLOB_FOUND = 7

TARGET_NAME = "install_jdbc_drivers"


def _default_install_xml(script_path: Path) -> Path:
    """Resolve the default install.xml from the script location.

    scripts/ lives at ``modules/perc-distribution-tree/scripts/``; install.xml
    lives at ``modules/perc-distribution-tree/src/main/resources/distribution/
    rxconfig/Installer/install.xml``.
    """
    module_dir = script_path.resolve().parent.parent
    return (
        module_dir
        / "src"
        / "main"
        / "resources"
        / "distribution"
        / "rxconfig"
        / "Installer"
        / "install.xml"
    )


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="check-no-glob-deletes.py",
        description=(
            "Static assertion that install.xml's "
            "<target name=\"install_jdbc_drivers\"> <delete> block does not "
            "use glob patterns."
        ),
    )
    p.add_argument(
        "--install-xml",
        type=Path,
        default=None,
        help=(
            "Path to install.xml "
            "(default: modules/perc-distribution-tree/src/main/resources/"
            "distribution/rxconfig/Installer/install.xml resolved from the "
            "script location)."
        ),
    )
    return p


def _find_target(
    root: ET.Element, target_name: str
) -> Optional[ET.Element]:
    """Return the first ``<target>`` whose ``name`` attribute equals
    ``target_name``. ANT XML typically uses no namespace; if a default
    namespace is present we still match by local name.
    """
    for el in root.iter():
        if el.tag.endswith("target") and el.attrib.get("name") == target_name:
            return el
    return None


def _find_first_child_delete(target: ET.Element) -> Optional[ET.Element]:
    """Return the first ``<delete>`` element directly or indirectly inside
    ``target``. The original POSIX script specifically inspects the *first*
    ``<delete>`` element inside the target — a future change that adds a
    second ``<delete>`` for unrelated cleanup should not change this check's
    scope; the failure surface is the ``<include>`` list under the first
    ``<delete>`` only.
    """
    for el in target.iter():
        if el is target:
            continue
        if el.tag.endswith("delete"):
            return el
    return None


def _collect_include_names(delete: ET.Element) -> List[str]:
    """Return every ``<include name="...">`` value directly under ``delete``.
    Nested ``<fileset>`` wrappers are intentionally not walked: the original
    POSIX script's two-pass extraction only inspects the top-level
    ``<include>`` children of the first ``<delete>`` inside the target, and
    deviating from that scope would expand the failure surface beyond the
    JUnit ``InstallXmlDeleteSetTest`` companion.
    """
    return [
        child.attrib["name"]
        for child in delete
        if child.tag.endswith("include") and "name" in child.attrib
    ]


def check(install_xml: Path) -> int:
    """Run the assertion and return the exit code. Does not call
    ``sys.exit`` — callers (the CLI main and pytest tests) decide what to do.
    """
    if not install_xml.is_file():
        LOG.error("ERROR: install.xml not found: %s", install_xml)
        return EXIT_INVOCATION

    try:
        tree = ET.parse(install_xml)
    except ET.ParseError as exc:
        LOG.error("ERROR: install.xml is not valid XML: %s (%s)", install_xml, exc)
        return EXIT_INVOCATION
    root = tree.getroot()

    target = _find_target(root, TARGET_NAME)
    if target is None:
        LOG.error(
            "ERROR: <target name=\"%s\"> not found in %s",
            TARGET_NAME,
            install_xml,
        )
        return EXIT_INVOCATION

    delete = _find_first_child_delete(target)
    if delete is None:
        LOG.error(
            "ERROR: no <delete> element found inside <target name=\"%s\">",
            TARGET_NAME,
        )
        return EXIT_INVOCATION

    includes = _collect_include_names(delete)
    globs = [name for name in includes if "*" in name or "?" in name]
    if globs:
        LOG.error(
            "ERROR: glob-based <delete> patterns found in %s target of %s:",
            TARGET_NAME,
            install_xml,
        )
        for g in globs:
            LOG.error("  %s", g)
        LOG.error(
            "Fix: replace each glob with the exact bundled-driver filename "
            "(see BundledJdbcDrivers constant in the test sources)."
        )
        return EXIT_GLOB_FOUND

    LOG.info(
        "OK: %s <delete> uses exact filenames only; no glob patterns found",
        TARGET_NAME,
    )
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    install_xml = args.install_xml or _default_install_xml(Path(__file__))
    return check(install_xml.resolve())


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())