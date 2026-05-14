#!/usr/bin/env python3
"""Fail when Gradle version catalog aliases are no longer referenced.

The checker intentionally has no third-party dependencies so it can run in CI with
Ubuntu's system Python. It understands the common Gradle Kotlin DSL access forms:

* libs.some.alias / libs.plugins.some.alias / libs.bundles.some.alias / libs.versions.some.alias
* libs.findLibrary("some-alias"), libs.findBundle("some-alias"), libs.findVersion("some-alias")
* libsCatalog.findVersion("some-alias") and related VersionCatalog lookups

Known intentional exceptions can be documented in
.github/dependency-policy/version-catalog-allowlist.txt using lines like:

    plugins.kotlin-jpa # Kept for modules that may opt into JPA conventions.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

CATALOG_SECTIONS = ("versions", "plugins", "libraries", "bundles")
DEFAULT_ALLOWLIST = ".github/dependency-policy/version-catalog-allowlist.txt"


@dataclass(frozen=True)
class CatalogEntry:
    section: str
    alias: str
    line: int
    value: str = ""

    @property
    def key(self) -> tuple[str, str]:
        return (self.section, self.alias)

    @property
    def qualified(self) -> str:
        return f"{self.section}.{self.alias}"


@dataclass
class Catalog:
    entries: dict[str, dict[str, CatalogEntry]] = field(
        default_factory=lambda: {section: {} for section in CATALOG_SECTIONS}
    )
    library_version_refs: dict[str, set[str]] = field(default_factory=dict)
    plugin_version_refs: dict[str, set[str]] = field(default_factory=dict)
    bundle_members: dict[str, set[str]] = field(default_factory=dict)


@dataclass
class Allowlist:
    reasons: dict[tuple[str, str], str]
    errors: list[str]


@dataclass
class Usage:
    libraries: set[str] = field(default_factory=set)
    plugins: set[str] = field(default_factory=set)
    bundles: set[str] = field(default_factory=set)
    versions: set[str] = field(default_factory=set)


def strip_comment(line: str) -> str:
    in_single = False
    in_double = False
    escaped = False
    for index, char in enumerate(line):
        if escaped:
            escaped = False
            continue
        if char == "\\":
            escaped = True
            continue
        if char == '"' and not in_single:
            in_double = not in_double
            continue
        if char == "'" and not in_double:
            in_single = not in_single
            continue
        if char == "#" and not in_single and not in_double:
            return line[:index]
    return line


def alias_to_accessor(alias: str) -> str:
    return re.sub(r"[-_.]+", ".", alias)


def accessor_pattern(prefix: str, alias: str) -> re.Pattern[str]:
    accessor = re.escape(alias_to_accessor(alias))
    return re.compile(rf"(?<![A-Za-z0-9_.]){re.escape(prefix)}\.{accessor}(?![A-Za-z0-9_.])")


def lookup_pattern(method: str, alias: str) -> re.Pattern[str]:
    return re.compile(rf"\b{re.escape(method)}\s*\(\s*[\"']{re.escape(alias)}[\"']\s*\)")


def bracket_balance_delta(text: str) -> int:
    """Return a lightweight TOML inline table/list balance delta."""
    return text.count("[") + text.count("{") - text.count("]") - text.count("}")


def alias_is_used(usage_text: str, accessor_prefix: str, lookup_method: str, alias: str) -> bool:
    return (
        accessor_pattern(accessor_prefix, alias).search(usage_text) is not None
        or lookup_pattern(lookup_method, alias).search(usage_text) is not None
    )


def display_path(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def parse_catalog(catalog_path: Path) -> Catalog:
    if not catalog_path.exists():
        raise FileNotFoundError(f"Catalog not found: {catalog_path}")

    catalog = Catalog()
    current_section: str | None = None
    pending: tuple[str, str, int, int] | None = None  # section, alias, start line, bracket balance
    pending_value: list[str] = []

    def finish_pending() -> None:
        nonlocal pending, pending_value
        if pending is None:
            return
        section, alias, start_line, _balance = pending
        value = "\n".join(pending_value)
        catalog.entries[section][alias] = CatalogEntry(section, alias, start_line, value)
        if section in ("libraries", "plugins"):
            refs = set(re.findall(r"version\.ref\s*=\s*[\"']([^\"']+)[\"']", value))
            if section == "libraries":
                catalog.library_version_refs[alias] = refs
            else:
                catalog.plugin_version_refs[alias] = refs
        elif section == "bundles":
            catalog.bundle_members[alias] = set(re.findall(r"[\"']([^\"']+)[\"']", value))
        pending = None
        pending_value = []

    for line_number, raw_line in enumerate(catalog_path.read_text(encoding="utf-8").splitlines(), 1):
        without_comment = strip_comment(raw_line).rstrip()
        stripped = without_comment.strip()
        section_match = re.match(r"^\[([A-Za-z0-9_.-]+)]\s*$", stripped)
        if section_match:
            finish_pending()
            current_section = section_match.group(1)
            continue

        if pending is not None:
            pending_value.append(without_comment)
            balance = pending[3] + bracket_balance_delta(without_comment)
            pending = (pending[0], pending[1], pending[2], balance)
            if balance <= 0:
                finish_pending()
            continue

        if current_section not in CATALOG_SECTIONS or not stripped:
            continue

        key_match = re.match(r"^([A-Za-z0-9_.-]+)\s*=\s*(.*)$", without_comment)
        if not key_match:
            continue
        alias, remainder = key_match.group(1), key_match.group(2)
        balance = bracket_balance_delta(remainder)
        pending = (current_section, alias, line_number, balance)
        pending_value = [remainder]
        if balance <= 0:
            finish_pending()

    finish_pending()
    return catalog


def parse_allowlist(path: Path) -> Allowlist:
    reasons: dict[tuple[str, str], str] = {}
    errors: list[str] = []
    if not path.exists():
        return Allowlist(reasons, errors)

    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "#" not in raw_line:
            errors.append(f"{path}:{line_number}: allowlist entries must include a # reason comment")
            continue
        entry, reason = raw_line.split("#", 1)
        qualified = entry.strip()
        reason = reason.strip()
        if not reason:
            errors.append(f"{path}:{line_number}: allowlist reason comment is empty")
            continue
        match = re.match(r"^(versions|plugins|libraries|bundles)\.([A-Za-z0-9_.-]+)$", qualified)
        if not match:
            errors.append(
                f"{path}:{line_number}: expected '<versions|plugins|libraries|bundles>.<alias> # reason'"
            )
            continue
        reasons[(match.group(1), match.group(2))] = reason
    return Allowlist(reasons, errors)


def gradle_kts_files(root: Path) -> list[Path]:
    ignored_dirs = {".git", ".gradle", ".omx", "build"}
    files: list[Path] = []
    for path in root.rglob("*.gradle.kts"):
        if any(part in ignored_dirs for part in path.relative_to(root).parts):
            continue
        files.append(path)
    return sorted(files)


def read_usage_text(files: Iterable[Path]) -> str:
    chunks = []
    for path in files:
        try:
            chunks.append(path.read_text(encoding="utf-8"))
        except UnicodeDecodeError:
            chunks.append(path.read_text())
    return "\n".join(chunks)


def collect_usage(catalog: Catalog, usage_text: str) -> Usage:
    usage = Usage()

    for alias in catalog.entries["libraries"]:
        if alias_is_used(usage_text, "libs", "findLibrary", alias):
            usage.libraries.add(alias)

    for alias in catalog.entries["bundles"]:
        if alias_is_used(usage_text, "libs.bundles", "findBundle", alias):
            usage.bundles.add(alias)

    for alias in catalog.entries["plugins"]:
        if alias_is_used(usage_text, "libs.plugins", "findPlugin", alias):
            usage.plugins.add(alias)

    for alias in catalog.entries["versions"]:
        if alias_is_used(usage_text, "libs.versions", "findVersion", alias):
            usage.versions.add(alias)

    # A used bundle makes its member library aliases used too.
    for bundle_alias in usage.bundles:
        usage.libraries.update(catalog.bundle_members.get(bundle_alias, set()))

    # version.ref aliases are used only when their owning catalog entry is used.
    for library_alias in usage.libraries:
        usage.versions.update(catalog.library_version_refs.get(library_alias, set()))
    for plugin_alias in usage.plugins:
        usage.versions.update(catalog.plugin_version_refs.get(plugin_alias, set()))

    return usage


def unknown_references(catalog: Catalog) -> list[str]:
    errors: list[str] = []
    library_aliases = set(catalog.entries["libraries"])
    version_aliases = set(catalog.entries["versions"])

    for bundle_alias, members in sorted(catalog.bundle_members.items()):
        for member in sorted(members - library_aliases):
            errors.append(f"bundles.{bundle_alias} references missing libraries.{member}")

    for library_alias, refs in sorted(catalog.library_version_refs.items()):
        for ref in sorted(refs - version_aliases):
            errors.append(f"libraries.{library_alias} references missing versions.{ref}")
    for plugin_alias, refs in sorted(catalog.plugin_version_refs.items()):
        for ref in sorted(refs - version_aliases):
            errors.append(f"plugins.{plugin_alias} references missing versions.{ref}")
    return errors


def unused_entries(catalog: Catalog, usage: Usage) -> dict[str, list[str]]:
    used_by_section = {
        "versions": usage.versions,
        "plugins": usage.plugins,
        "libraries": usage.libraries,
        "bundles": usage.bundles,
    }
    return {
        section: sorted(set(catalog.entries[section]) - used_by_section[section])
        for section in CATALOG_SECTIONS
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Check unused Gradle version catalog aliases.")
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="Repository root (default: cwd)")
    parser.add_argument(
        "--catalog",
        type=Path,
        default=Path("gradle/libs.versions.toml"),
        help="Version catalog path relative to --root unless absolute",
    )
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=Path(DEFAULT_ALLOWLIST),
        help="Allowlist path relative to --root unless absolute",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    catalog_path = args.catalog if args.catalog.is_absolute() else root / args.catalog
    allowlist_path = args.allowlist if args.allowlist.is_absolute() else root / args.allowlist

    catalog = parse_catalog(catalog_path)
    allowlist = parse_allowlist(allowlist_path)
    errors = list(allowlist.errors)
    errors.extend(unknown_references(catalog))

    usage_files = gradle_kts_files(root)
    usage = collect_usage(catalog, read_usage_text(usage_files))
    unused = unused_entries(catalog, usage)

    catalog_keys = {entry.key for section in catalog.entries.values() for entry in section.values()}
    unused_keys = {(section, alias) for section, aliases in unused.items() for alias in aliases}
    allowed_keys = set(allowlist.reasons)

    for key in sorted(allowed_keys - catalog_keys):
        errors.append(f"allowlist references missing catalog alias: {key[0]}.{key[1]}")
    for key in sorted(allowed_keys - unused_keys):
        if key in catalog_keys:
            errors.append(f"allowlist entry is no longer needed: {key[0]}.{key[1]}")

    unallowed_unused = {
        section: [alias for alias in aliases if (section, alias) not in allowed_keys]
        for section, aliases in unused.items()
    }

    print(f"Checked {display_path(catalog_path, root)} against {len(usage_files)} Gradle Kotlin DSL files.")
    if allowlist_path.exists():
        print(f"Loaded allowlist: {display_path(allowlist_path, root)}")

    for section in CATALOG_SECTIONS:
        allowed = [alias for alias in unused[section] if (section, alias) in allowed_keys]
        if allowed:
            print(f"Allowed unused {section} aliases:")
            for alias in allowed:
                print(f"  - {section}.{alias}: {allowlist.reasons[(section, alias)]}")

    failed = False
    for section in CATALOG_SECTIONS:
        if unallowed_unused[section]:
            failed = True
            print(f"Unused {section} aliases:", file=sys.stderr)
            for alias in unallowed_unused[section]:
                entry = catalog.entries[section][alias]
                print(f"  - {entry.qualified} ({display_path(catalog_path, root)}:{entry.line})", file=sys.stderr)

    if errors:
        failed = True
        print("Catalog checker configuration errors:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)

    if failed:
        print(
            "Add a real usage, remove the stale alias, or document a temporary exception in "
            f"{display_path(allowlist_path, root)}.",
            file=sys.stderr,
        )
        return 1

    print("No unused version catalog aliases found.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
