#!/usr/bin/env python3
"""Merge Maven + npm third-party license inventories into a single THIRD-PARTY.txt.

Issue #1689: org.codehaus.mojo:license-maven-plugin produces the Maven half;
this script reads product package-lock.json files for production npm deps and
writes one merged inventory consumed by the installer assembly.

Cross-platform (Windows / Linux / macOS). Python 3.9+. Stdlib only.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


DEFAULT_MAVEN_NAME = "THIRD-PARTY-MAVEN.txt"
DEFAULT_NPM_INTERMEDIATE = "THIRD-PARTY-NPM.txt"
DEFAULT_MERGED_NAME = "THIRD-PARTY.txt"
DEFAULT_LOCK_LIST = "src/license/npm-package-locks.txt"


@dataclass(frozen=True, order=True)
class NpmPackage:
    name: str
    version: str
    license: str
    source: str  # lockfile path (repo-relative) for provenance

    def format_line(self) -> str:
        lic = self.license if self.license else "Unknown license"
        ver = self.version if self.version else "unknown"
        return f"     ({lic}) {self.name} (npm:{self.name}:{ver} - {self.source})"


def repo_root_from_script() -> Path:
    return Path(__file__).resolve().parent.parent


def read_lock_list(list_file: Path, root: Path) -> list[Path]:
    if not list_file.is_file():
        return []
    locks: list[Path] = []
    for raw in list_file.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        # Portable: list entries use forward slashes; resolve via Path parts.
        rel = Path(*line.replace("\\", "/").split("/"))
        path = (root / rel).resolve()
        locks.append(path)
    return locks


def _package_name_from_lock_key(key: str) -> str | None:
    """Map package-lock 'packages' key to npm package name.

    Keys look like:
      node_modules/react
      node_modules/@scope/pkg
      node_modules/foo/node_modules/bar
    """
    if not key or key == ".":
        return None
    # Use the last node_modules segment (nested installs).
    marker = "node_modules/"
    if marker not in key.replace("\\", "/"):
        # Workspace / file: link entry (e.g. ../../../vendor/mkd-language)
        # Prefer packages that are real registry installs under node_modules.
        return None
    norm = key.replace("\\", "/")
    idx = norm.rfind(marker)
    name = norm[idx + len(marker) :]
    return name or None


def collect_npm_from_lock(lock_path: Path, root: Path) -> list[NpmPackage]:
    if not lock_path.is_file():
        raise FileNotFoundError(f"package-lock.json not found: {lock_path}")
    data = json.loads(lock_path.read_text(encoding="utf-8"))
    packages = data.get("packages")
    if not isinstance(packages, dict):
        raise ValueError(f"Unsupported package-lock (missing packages map): {lock_path}")

    try:
        rel_source = lock_path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        rel_source = str(lock_path)

    found: dict[tuple[str, str], NpmPackage] = {}
    for key, meta in packages.items():
        if not isinstance(meta, dict):
            continue
        if meta.get("dev") is True or meta.get("devOptional") is True:
            continue
        name = _package_name_from_lock_key(str(key))
        if not name:
            continue
        version = str(meta.get("version") or "").strip()
        if not version:
            # file: linked packages sometimes omit version in the packages map
            continue
        license_raw = meta.get("license")
        if isinstance(license_raw, dict):
            # rare { "type": "MIT", "url": "..." }
            license_str = str(license_raw.get("type") or license_raw.get("name") or "Unknown license")
        elif isinstance(license_raw, list):
            license_str = " OR ".join(str(x) for x in license_raw)
        elif license_raw:
            license_str = str(license_raw)
        else:
            # Optional fallback: node_modules package.json when present
            license_str = _license_from_node_modules(lock_path.parent, name) or "Unknown license"

        pkg = NpmPackage(name=name, version=version, license=license_str, source=rel_source)
        found[(name, version)] = pkg

    return sorted(found.values())


def _license_from_node_modules(package_root: Path, name: str) -> str | None:
    # Portable join of scoped package path segments.
    parts = name.split("/")
    pkg_json = package_root.joinpath("node_modules", *parts, "package.json")
    if not pkg_json.is_file():
        return None
    try:
        meta = json.loads(pkg_json.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    lic = meta.get("license")
    if isinstance(lic, dict):
        return str(lic.get("type") or lic.get("name") or "") or None
    if isinstance(lic, list):
        return " OR ".join(str(x) for x in lic) or None
    if lic:
        return str(lic)
    return None


def format_npm_section(packages: Iterable[NpmPackage]) -> str:
    pkgs = list(packages)
    lines = [
        f"Lists of {len(pkgs)} third-party npm dependencies (production).",
        "",
    ]
    for p in pkgs:
        lines.append(p.format_line())
    lines.append("")
    return "\n".join(lines)


def merge_inventories(maven_text: str, npm_text: str) -> str:
    maven_body = maven_text.strip()
    npm_body = npm_text.strip()
    parts = [
        "Percussion CMS third-party dependency license inventory",
        "Generated at build time — do not hand-edit. See src/license/README.md (issue #1689).",
        "",
        "================================================================================",
        "Maven third-party dependencies",
        "================================================================================",
        "",
        maven_body if maven_body else "(no Maven inventory — run license:aggregate-add-third-party first)",
        "",
        "================================================================================",
        "npm third-party dependencies (production)",
        "================================================================================",
        "",
        npm_body if npm_body else "(no npm production dependencies found)",
        "",
    ]
    return "\n".join(parts)


def generate(
    root: Path,
    out_dir: Path,
    maven_name: str = DEFAULT_MAVEN_NAME,
    npm_name: str = DEFAULT_NPM_INTERMEDIATE,
    merged_name: str = DEFAULT_MERGED_NAME,
    lock_list: Path | None = None,
    require_maven: bool = False,
) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    maven_path = out_dir / maven_name
    npm_path = out_dir / npm_name
    merged_path = out_dir / merged_name

    if maven_path.is_file():
        maven_text = maven_path.read_text(encoding="utf-8")
    else:
        if require_maven:
            raise FileNotFoundError(
                f"Maven inventory missing: {maven_path}. "
                "Run mvnw license:aggregate-add-third-party first."
            )
        maven_text = ""

    list_file = lock_list if lock_list is not None else root / DEFAULT_LOCK_LIST
    locks = read_lock_list(list_file, root)
    npm_packages: dict[tuple[str, str], NpmPackage] = {}
    missing_locks: list[Path] = []
    for lock in locks:
        if not lock.is_file():
            missing_locks.append(lock)
            continue
        for pkg in collect_npm_from_lock(lock, root):
            npm_packages[(pkg.name, pkg.version)] = pkg

    if missing_locks:
        missing = ", ".join(str(p) for p in missing_locks)
        print(f"WARNING: package-lock.json not found (skipped): {missing}", file=sys.stderr)

    npm_sorted = sorted(npm_packages.values())
    npm_text = format_npm_section(npm_sorted)
    npm_path.write_text(npm_text, encoding="utf-8", newline="\n")

    merged = merge_inventories(maven_text, npm_text)
    merged_path.write_text(merged, encoding="utf-8", newline="\n")
    print(
        f"Wrote {merged_path} "
        f"(Maven present={maven_path.is_file()}, npm packages={len(npm_sorted)})"
    )
    return merged_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=None,
        help="Repository root (default: parent of scripts/)",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=None,
        help="Output directory (default: <root>/target/generated-sources/license)",
    )
    parser.add_argument(
        "--lock-list",
        type=Path,
        default=None,
        help=f"File listing package-lock.json paths (default: <root>/{DEFAULT_LOCK_LIST})",
    )
    parser.add_argument(
        "--require-maven",
        action="store_true",
        help="Fail if THIRD-PARTY-MAVEN.txt is missing",
    )
    args = parser.parse_args(argv)

    root = (args.root or repo_root_from_script()).resolve()
    out_dir = (
        args.out_dir.resolve()
        if args.out_dir
        else root / "target" / "generated-sources" / "license"
    )
    lock_list = args.lock_list.resolve() if args.lock_list else root / DEFAULT_LOCK_LIST

    try:
        generate(
            root=root,
            out_dir=out_dir,
            lock_list=lock_list,
            require_maven=args.require_maven,
        )
    except FileNotFoundError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
