#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate stub Javadoc comments for Java files missing documentation.

Cross-platform (Windows / Linux / macOS). Stdlib only.

Replaces ``modules/ai-shared-develop/src/main/resources/skills/
javadoc/scripts/generate-javadoc-stubs.sh``.

The original ``.sh`` used a chain of grep/awk/sed to extract method
signatures from each ``.java`` file and emit a ``/** TODO ... */``
header. The Python port uses regex on the file contents (avoids the
``.sh``'s fragile multi-step shell-quoting).

A ``--dry-run`` flag prints the files that would be modified without
actually writing. This gates pytest and lets operators preview the
generated stubs.

## Behavioral Notes (FR-009b)

- The original ``.sh`` walked directories with ``find -name '*.java'``.
  The Python port uses ``Path.rglob`` (std-lib, cross-platform).
- The original used ``grep -oP '...\\s+\\K\\w+(?=\\s*\\()'`` to extract
  the method name. The Python port uses ``re.findall`` on a single
  pass over the file content (no shell, no fragile escaping).
- ``JDK_VERSION`` auto-detection from ``pom.xml`` is preserved via
  regex on the ``<source>`` element (matches the original behavior).
- Path discovery uses ``pathlib.Path``; no hardcoded separators.

Exit codes:

  0  success (or dry-run completed)
  1  invocation error
  2  input path missing
"""

from __future__ import annotations

import argparse
import logging
import re
import sys
from pathlib import Path
from typing import Iterable, Optional

LOG = logging.getLogger("generate-javadoc-stubs")

EXIT_OK = 0
EXIT_INVOCATION = 1
EXIT_INPUT_MISSING = 2

DEFAULT_JDK_VERSION = "21"

# Match a single-line method declaration starting with public/protected/
# private + optional static + a return type + identifier + parentheses.
# Examples that match:
#   public void foo(int x) { ... }
#   protected static String bar() { ... }
#   private List<String> baz() { ... }
METHOD_RE = re.compile(
    r"^\s+(?:public|protected|private)\s+(?:static\s+)?"
    # Return type: primitive, ``Object`` / ``String``, generic like
    # ``List<String>``, or a capitalized identifier. Generics must be
    # listed BEFORE the bare-identifier alternative so ``List<String>``
    # matches the generic arm first (otherwise ``List`` matches the
    # identifier arm and the regex fails to consume ``<``).
    r"(?P<return>(?:void|int|long|boolean|byte|short|float|double|String|Object)"
    r"|<[^>]+>"
    r"|[A-Z][A-Za-z0-9_]*<[^>]+>"  # Parametrized identifier like ``List<String>``
    r"|[A-Z][A-Za-z0-9_]*"          # Bare identifier like ``Foo``
    r")\s+"
    r"(?P<name>[A-Za-z_]\w*)\s*\((?P<params>[^)]*)\)",
    re.MULTILINE,
)


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="generate-javadoc-stubs.py",
        description=(
            "Generate stub Javadoc comments for Java files missing "
            "documentation. Walks directories recursively."
        ),
    )
    p.add_argument(
        "input",
        type=Path,
        help="Java file or directory to process.",
    )
    p.add_argument(
        "-o",
        "--output",
        type=Path,
        default=None,
        help=(
            "Optional output file (single-file mode). When set, the "
            "generated stub is written here instead of being prepended "
            "to the input file."
        ),
    )
    p.add_argument(
        "--jdk-version",
        default=None,
        help=(
            "JDK version to mention in the stub header. "
            "Auto-detected from pom.xml if not set (matches original .sh)."
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print which files would be modified without writing.",
    )
    return p


def _detect_jdk_version(repo_root: Path) -> str:
    """Detect JDK version from ``<source>`` in ``pom.xml`` if available."""
    pom = repo_root / "pom.xml"
    if not pom.is_file():
        return DEFAULT_JDK_VERSION
    text = pom.read_text(encoding="utf-8", errors="replace")
    # The original used ``grep -A1 '<source>' pom.xml | grep -oP '\d+'``;
    # this is the same search via re.
    m = re.search(r"<source>\s*(\d+)", text)
    if m:
        return m.group(1)
    return DEFAULT_JDK_VERSION


def _extract_methods(source: str) -> list[tuple[str, str, str]]:
    """Return a list of ``(return_type, name, params_text)`` tuples."""
    out = []
    for m in METHOD_RE.finditer(source):
        out.append((m.group("return"), m.group("name"), m.group("params")))
    return out


def _build_stub(classname: str, methods: list[tuple[str, str, str]], jdk_version: str) -> str:
    """Build the Javadoc stub text for a class."""
    lines = ["/**", f" * TODO: Add description for {classname}", " *"]
    for ret, name, params in methods:
        # Strip type information from params; emit ``@param name`` per arg.
        param_names = [
            p.strip().split()[-1] if p.strip() else ""
            for p in params.split(",")
        ]
        for pn in param_names:
            if pn:
                lines.append(f" * @param {pn}")
        if ret.strip() != "void":
            lines.append(" * @return")
    lines.append(" */")
    lines.append(f"// generated for JDK {jdk_version}")
    return "\n".join(lines)


def _has_existing_javadoc(source: str) -> bool:
    """True if the file already has a Javadoc comment block at the top
    of any class / method (``/** ... */``).
    """
    return bool(re.search(r"^\s*/\*\*", source, re.MULTILINE))


def _generate_for_file(
    java_file: Path,
    *,
    jdk_version: str,
    output: Optional[Path],
    dry_run: bool,
) -> str:
    """Return the stub text for ``java_file``. Caller decides whether to
    write it (dry-run / no --output writes nothing; real run prepends).
    """
    source = java_file.read_text(encoding="utf-8", errors="replace")
    if _has_existing_javadoc(source):
        return ""
    classname = java_file.stem
    methods = _extract_methods(source)
    return _build_stub(classname, methods, jdk_version)


def run(
    *,
    input_path: Path,
    output: Optional[Path],
    jdk_version: Optional[str],
    dry_run: bool,
) -> int:
    """Top-level entry point."""
    if not input_path.exists():
        LOG.error("Error: input path %s not found", input_path)
        return EXIT_INPUT_MISSING

    version = jdk_version or _detect_jdk_version(
        input_path if input_path.is_dir() else input_path.parent
    )
    LOG.info("Generating Javadoc stubs for JDK %s ...", version)

    files = (
        sorted(input_path.rglob("*.java"))
        if input_path.is_dir()
        else [input_path]
    )
    generated = 0
    for java in files:
        if not java.is_file() or not java.name.endswith(".java"):
            continue
        stub = _generate_for_file(java, jdk_version=version, output=output, dry_run=dry_run)
        if not stub:
            continue
        if output is not None and not dry_run:
            output.write_text(stub + "\n", encoding="utf-8")
        elif not dry_run and output is None:
            existing = java.read_text(encoding="utf-8", errors="replace")
            java.write_text(stub + "\n" + existing, encoding="utf-8")
        else:
            LOG.info("DRY-RUN: would generate stub for %s", java)
        generated += 1
    LOG.info("Generated %d stub(s) for JDK %s", generated, version)
    return EXIT_OK


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    return run(
        input_path=args.input,
        output=args.output,
        jdk_version=args.jdk_version,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    sys.exit(main())