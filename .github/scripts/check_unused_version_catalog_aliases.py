#!/usr/bin/env python3
"""Fail when Gradle version catalog aliases are no longer referenced.

The checker intentionally has no third-party dependencies so it can run in CI with
Ubuntu's system Python. It understands the common Gradle Kotlin DSL access forms:

* libs.some.alias / libs.plugins.some.alias / libs.bundles.some.alias / libs.versions.some.alias
* libs.findLibrary("some-alias"), libs.findBundle("some-alias"), libs.findVersion("some-alias")
* libsCatalog.findVersion("some-alias") and related VersionCatalog lookups

Known intentional exceptions can be documented in
.github/dependency-policy/version-catalog-allowlist.txt using lines like:

    libraries.example-temporary-alias # Kept until migration issue #123 removes the final caller.
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
class UsageText:
    lookup_text: str
    accessor_text: str


@dataclass
class Usage:
    libraries: set[str] = field(default_factory=set)
    plugins: set[str] = field(default_factory=set)
    bundles: set[str] = field(default_factory=set)
    versions: set[str] = field(default_factory=set)


@dataclass
class CatalogCheck:
    catalog: Catalog
    allowlist: Allowlist
    unused: dict[str, list[str]]
    errors: list[str]
    root: Path
    catalog_path: Path
    allowlist_path: Path
    usage_file_count: int


@dataclass
class CatalogPaths:
    root: Path
    catalog_path: Path
    allowlist_path: Path


class KotlinSourceCleaner:
    """Small tokenizer for Gradle Kotlin DSL comments and strings."""

    def __init__(self, source: str) -> None:
        self.source = source
        self.output: list[str] = []
        self.index = 0
        self.escaped = False

    def strip_comments(self) -> str:
        while self.has_current():
            if self.consume_line_comment() or self.consume_block_comment():
                continue
            if self.copy_triple_string() or self.copy_quoted_string('"') or self.copy_quoted_string("'"):
                continue
            self.emit_current()
        return "".join(self.output)

    def mask_strings(self) -> str:
        while self.has_current():
            if self.mask_triple_string() or self.mask_quoted_string('"') or self.mask_quoted_string("'"):
                continue
            self.emit_current()
        return "".join(self.output)

    def has_current(self) -> bool:
        return self.index < len(self.source)

    def current(self) -> str:
        return self.source[self.index]

    def next_char(self) -> str:
        return self.source[self.index + 1] if self.index + 1 < len(self.source) else ""

    def next_three(self) -> str:
        return self.source[self.index:self.index + 3]

    def emit_current(self) -> None:
        self.output.append(self.current())
        self.index += 1

    def emit_masked_current(self) -> None:
        self.output.append("\n" if self.current() == "\n" else " ")
        self.index += 1

    def consume_line_comment(self) -> bool:
        if self.current() != "/" or self.next_char() != "/":
            return False
        self.output.extend("  ")
        self.index += 2
        self.mask_until_line_end()
        return True

    def mask_until_line_end(self) -> None:
        while self.has_current() and self.current() != "\n":
            self.emit_masked_current()
        if self.has_current():
            self.emit_current()

    def consume_block_comment(self) -> bool:
        if self.current() != "/" or self.next_char() != "*":
            return False
        self.output.extend("  ")
        self.index += 2
        self.mask_until_block_comment_end()
        return True

    def mask_until_block_comment_end(self) -> None:
        while self.has_current():
            if self.current() == "*" and self.next_char() == "/":
                self.output.extend("  ")
                self.index += 2
                return
            self.emit_masked_current()

    def copy_triple_string(self) -> bool:
        if self.next_three() != '"""':
            return False
        self.copy_literal_until_triple_quote(mask_contents=False)
        return True

    def mask_triple_string(self) -> bool:
        if self.next_three() != '"""':
            return False
        self.copy_literal_until_triple_quote(mask_contents=True)
        return True

    def copy_literal_until_triple_quote(self, mask_contents: bool) -> None:
        self.output.extend(self.next_three())
        self.index += 3
        while self.has_current():
            if self.next_three() == '"""':
                self.output.extend(self.next_three())
                self.index += 3
                return
            self.emit_masked_current() if mask_contents else self.emit_current()

    def copy_quoted_string(self, quote: str) -> bool:
        if self.current() != quote:
            return False
        self.copy_literal_until_quote(quote, mask_contents=False)
        return True

    def mask_quoted_string(self, quote: str) -> bool:
        if self.current() != quote:
            return False
        self.copy_literal_until_quote(quote, mask_contents=True)
        return True

    def copy_literal_until_quote(self, quote: str, mask_contents: bool) -> None:
        self.output.append(self.current())
        self.index += 1
        self.escaped = False
        while self.has_current():
            if self.consume_escape(mask_contents):
                continue
            if self.current() == quote:
                self.emit_current()
                return
            self.emit_masked_current() if mask_contents else self.emit_current()

    def consume_escape(self, mask_contents: bool) -> bool:
        if not self.escaped and self.current() != "\\":
            return False

        was_escape_prefix = self.current() == "\\" and not self.escaped
        self.escaped = was_escape_prefix
        self.emit_masked_current() if mask_contents else self.emit_current()
        return True


class CatalogParser:
    def __init__(self) -> None:
        self.catalog = Catalog()
        self.current_section: str | None = None
        self.pending: tuple[str, str, int, int] | None = None
        self.pending_value: list[str] = []

    def parse(self, catalog_path: Path) -> Catalog:
        if not catalog_path.exists():
            raise FileNotFoundError(f"Catalog not found: {catalog_path}")
        for line_number, raw_line in enumerate(catalog_path.read_text(encoding="utf-8").splitlines(), 1):
            self.consume_line(line_number, strip_comment(raw_line).rstrip())
        self.finish_pending()
        return self.catalog

    def consume_line(self, line_number: int, line: str) -> None:
        stripped = line.strip()
        if self.consume_section_header(stripped):
            return
        if self.consume_pending_entry(line):
            return
        self.start_entry(line_number, line, stripped)

    def consume_section_header(self, stripped: str) -> bool:
        match = re.match(r"^\[([A-Za-z0-9_.-]+)]\s*$", stripped)
        if not match:
            return False
        self.finish_pending()
        self.current_section = match.group(1)
        return True

    def consume_pending_entry(self, line: str) -> bool:
        if self.pending is None:
            return False
        self.pending_value.append(line)
        self.update_pending_balance(bracket_balance_delta(line))
        return True

    def update_pending_balance(self, delta: int) -> None:
        if self.pending is None:
            return
        section, alias, start_line, balance = self.pending
        self.pending = (section, alias, start_line, balance + delta)
        if balance + delta <= 0:
            self.finish_pending()

    def start_entry(self, line_number: int, line: str, stripped: str) -> None:
        if self.current_section not in CATALOG_SECTIONS or not stripped:
            return
        match = re.match(r"^([A-Za-z0-9_.-]+)\s*=\s*(.*)$", line)
        if match:
            self.pending = (
                self.current_section,
                match.group(1),
                line_number,
                bracket_balance_delta(match.group(2)),
            )
            self.pending_value = [match.group(2)]
        if self.pending is not None and self.pending[3] <= 0:
            self.finish_pending()

    def finish_pending(self) -> None:
        if self.pending is None:
            return
        section, alias, start_line, _balance = self.pending
        value = "\n".join(self.pending_value)
        self.catalog.entries[section][alias] = CatalogEntry(section, alias, start_line, value)
        self.record_entry_metadata(section, alias, value)
        self.pending = None
        self.pending_value = []

    def record_entry_metadata(self, section: str, alias: str, value: str) -> None:
        if section in ("libraries", "plugins"):
            refs = set(re.findall(r"version\.ref\s*=\s*[\"']([^\"']+)[\"']", value))
            target = self.catalog.library_version_refs if section == "libraries" else self.catalog.plugin_version_refs
            target[alias] = refs
        elif section == "bundles":
            self.catalog.bundle_members[alias] = set(re.findall(r"[\"']([^\"']+)[\"']", value))


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


def strip_kotlin_comments(source: str) -> str:
    return KotlinSourceCleaner(source).strip_comments()


def mask_kotlin_strings(source: str) -> str:
    return KotlinSourceCleaner(source).mask_strings()


def alias_to_accessor(alias: str) -> str:
    return re.sub(r"[-_.]+", ".", alias)


def accessor_pattern(prefix: str, alias: str) -> re.Pattern[str]:
    accessor = re.escape(alias_to_accessor(alias))
    return re.compile(
        rf"(?<![A-Za-z0-9_.]){re.escape(prefix)}\.{accessor}(?:\.get\(\))?(?![A-Za-z0-9_.])"
    )


def lookup_pattern(method: str, alias: str) -> re.Pattern[str]:
    return re.compile(rf"\b{re.escape(method)}\s*\(\s*[\"']{re.escape(alias)}[\"']\s*\)")


def bracket_balance_delta(text: str) -> int:
    """Return a lightweight TOML inline table/list balance delta."""
    return text.count("[") + text.count("{") - text.count("]") - text.count("}")


def alias_is_used(usage_text: UsageText, accessor_prefix: str, lookup_method: str, alias: str) -> bool:
    return (
        accessor_pattern(accessor_prefix, alias).search(usage_text.accessor_text) is not None
        or lookup_pattern(lookup_method, alias).search(usage_text.lookup_text) is not None
    )


def display_path(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def parse_catalog(catalog_path: Path) -> Catalog:
    return CatalogParser().parse(catalog_path)


def parse_allowlist(path: Path) -> Allowlist:
    reasons: dict[tuple[str, str], str] = {}
    errors: list[str] = []
    if not path.exists():
        return Allowlist(reasons, errors)

    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        parse_allowlist_line(path, line_number, raw_line, reasons, errors)
    return Allowlist(reasons, errors)


def parse_allowlist_line(
    path: Path,
    line_number: int,
    raw_line: str,
    reasons: dict[tuple[str, str], str],
    errors: list[str],
) -> None:
    stripped = raw_line.strip()
    if not stripped or stripped.startswith("#"):
        return
    if "#" not in raw_line:
        errors.append(f"{path}:{line_number}: allowlist entries must include a # reason comment")
        return
    record_allowlist_entry(path, line_number, raw_line, reasons, errors)


def record_allowlist_entry(
    path: Path,
    line_number: int,
    raw_line: str,
    reasons: dict[tuple[str, str], str],
    errors: list[str],
) -> None:
    qualified, reason = split_allowlist_entry(raw_line)
    match = re.match(r"^(versions|plugins|libraries|bundles)\.([A-Za-z0-9_.-]+)$", qualified)
    if not reason:
        errors.append(f"{path}:{line_number}: allowlist reason comment is empty")
    elif match:
        reasons[(match.group(1), match.group(2))] = reason
    else:
        errors.append(f"{path}:{line_number}: expected '<versions|plugins|libraries|bundles>.<alias> # reason'")


def split_allowlist_entry(raw_line: str) -> tuple[str, str]:
    entry, reason = raw_line.split("#", 1)
    return entry.strip(), reason.strip()


def gradle_kts_files(root: Path) -> list[Path]:
    ignored_dirs = {".git", ".gradle", ".omx", "build"}
    files: list[Path] = []
    for path in root.rglob("*.gradle.kts"):
        if not any(part in ignored_dirs for part in path.relative_to(root).parts):
            files.append(path)
    return sorted(files)


def read_usage_text(files: Iterable[Path]) -> UsageText:
    lookup_chunks = []
    accessor_chunks = []
    for path in files:
        without_comments = strip_kotlin_comments(read_text(path))
        lookup_chunks.append(without_comments)
        accessor_chunks.append(mask_kotlin_strings(without_comments))
    return UsageText(
        lookup_text="\n".join(lookup_chunks),
        accessor_text="\n".join(accessor_chunks),
    )


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text()


def collect_usage(catalog: Catalog, usage_text: UsageText) -> Usage:
    usage = Usage()
    add_used_aliases(usage.libraries, catalog.entries["libraries"], usage_text, "libs", "findLibrary")
    add_used_aliases(usage.bundles, catalog.entries["bundles"], usage_text, "libs.bundles", "findBundle")
    add_used_aliases(usage.plugins, catalog.entries["plugins"], usage_text, "libs.plugins", "findPlugin")
    add_used_aliases(usage.versions, catalog.entries["versions"], usage_text, "libs.versions", "findVersion")
    add_transitive_catalog_usage(catalog, usage)
    return usage


def add_used_aliases(
    target: set[str],
    entries: dict[str, CatalogEntry],
    usage_text: UsageText,
    accessor_prefix: str,
    lookup_method: str,
) -> None:
    for alias in entries:
        if alias_is_used(usage_text, accessor_prefix, lookup_method, alias):
            target.add(alias)


def add_transitive_catalog_usage(catalog: Catalog, usage: Usage) -> None:
    for bundle_alias in usage.bundles:
        usage.libraries.update(catalog.bundle_members.get(bundle_alias, set()))
    for library_alias in usage.libraries:
        usage.versions.update(catalog.library_version_refs.get(library_alias, set()))
    for plugin_alias in usage.plugins:
        usage.versions.update(catalog.plugin_version_refs.get(plugin_alias, set()))


def unknown_references(catalog: Catalog) -> list[str]:
    errors: list[str] = []
    errors.extend(missing_bundle_members(catalog))
    errors.extend(missing_version_refs("libraries", catalog.library_version_refs, catalog))
    errors.extend(missing_version_refs("plugins", catalog.plugin_version_refs, catalog))
    return errors


def missing_bundle_members(catalog: Catalog) -> list[str]:
    library_aliases = set(catalog.entries["libraries"])
    return [
        f"bundles.{bundle_alias} references missing libraries.{member}"
        for bundle_alias, members in sorted(catalog.bundle_members.items())
        for member in sorted(members - library_aliases)
    ]


def missing_version_refs(section: str, refs_by_alias: dict[str, set[str]], catalog: Catalog) -> list[str]:
    version_aliases = set(catalog.entries["versions"])
    return [
        f"{section}.{alias} references missing versions.{ref}"
        for alias, refs in sorted(refs_by_alias.items())
        for ref in sorted(refs - version_aliases)
    ]


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


def parse_args() -> argparse.Namespace:
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
    return parser.parse_args()


def resolve_paths(args: argparse.Namespace) -> CatalogPaths:
    root = args.root.resolve()
    return CatalogPaths(
        root=root,
        catalog_path=args.catalog if args.catalog.is_absolute() else root / args.catalog,
        allowlist_path=args.allowlist if args.allowlist.is_absolute() else root / args.allowlist,
    )


def build_check(paths: CatalogPaths) -> CatalogCheck:
    catalog = parse_catalog(paths.catalog_path)
    allowlist = parse_allowlist(paths.allowlist_path)
    usage_files = gradle_kts_files(paths.root)
    usage = collect_usage(catalog, read_usage_text(usage_files))
    unused = unused_entries(catalog, usage)
    errors = list(allowlist.errors)
    errors.extend(unknown_references(catalog))
    errors.extend(allowlist_reference_errors(catalog, unused, allowlist))
    return CatalogCheck(
        catalog=catalog,
        allowlist=allowlist,
        unused=unused,
        errors=errors,
        root=paths.root,
        catalog_path=paths.catalog_path,
        allowlist_path=paths.allowlist_path,
        usage_file_count=len(usage_files),
    )


def allowlist_reference_errors(catalog: Catalog, unused: dict[str, list[str]], allowlist: Allowlist) -> list[str]:
    catalog_keys = {entry.key for section in catalog.entries.values() for entry in section.values()}
    unused_keys = {(section, alias) for section, aliases in unused.items() for alias in aliases}
    errors = [
        f"allowlist references missing catalog alias: {key[0]}.{key[1]}"
        for key in sorted(set(allowlist.reasons) - catalog_keys)
    ]
    errors.extend(
        f"allowlist entry is no longer needed: {key[0]}.{key[1]}"
        for key in sorted(set(allowlist.reasons) - unused_keys)
        if key in catalog_keys
    )
    return errors


def unallowed_unused_entries(check: CatalogCheck) -> dict[str, list[str]]:
    allowed_keys = set(check.allowlist.reasons)
    return {
        section: [alias for alias in aliases if (section, alias) not in allowed_keys]
        for section, aliases in check.unused.items()
    }


def print_check_header(check: CatalogCheck) -> None:
    print(f"Checked {display_path(check.catalog_path, check.root)} against {check.usage_file_count} Gradle Kotlin DSL files.")
    if check.allowlist_path.exists():
        print(f"Loaded allowlist: {display_path(check.allowlist_path, check.root)}")


def print_allowed_unused(check: CatalogCheck) -> None:
    for section in CATALOG_SECTIONS:
        aliases = [alias for alias in check.unused[section] if (section, alias) in check.allowlist.reasons]
        print_allowed_section(check, section, aliases)


def print_allowed_section(check: CatalogCheck, section: str, aliases: list[str]) -> None:
    if not aliases:
        return
    print(f"Allowed unused {section} aliases:")
    for alias in aliases:
        print(f"  - {section}.{alias}: {check.allowlist.reasons[(section, alias)]}")


def print_unallowed_unused(check: CatalogCheck, unallowed: dict[str, list[str]]) -> bool:
    failed = False
    for section in CATALOG_SECTIONS:
        if unallowed[section]:
            failed = True
            print_unused_section(check, section, unallowed[section])
    return failed


def print_unused_section(check: CatalogCheck, section: str, aliases: list[str]) -> None:
    print(f"Unused {section} aliases:", file=sys.stderr)
    for alias in aliases:
        entry = check.catalog.entries[section][alias]
        print(f"  - {entry.qualified} ({display_path(check.catalog_path, check.root)}:{entry.line})", file=sys.stderr)


def print_errors(errors: list[str]) -> bool:
    if not errors:
        return False
    print("Catalog checker configuration errors:", file=sys.stderr)
    for error in errors:
        print(f"  - {error}", file=sys.stderr)
    return True


def print_failure_hint(check: CatalogCheck) -> None:
    print(
        "Add a real usage, remove the stale alias, or document a temporary exception in "
        f"{display_path(check.allowlist_path, check.root)}.",
        file=sys.stderr,
    )


def run_check(args: argparse.Namespace) -> int:
    check = build_check(resolve_paths(args))
    print_check_header(check)
    print_allowed_unused(check)
    failed = print_unallowed_unused(check, unallowed_unused_entries(check))
    failed = print_errors(check.errors) or failed
    if failed:
        print_failure_hint(check)
        return 1
    print("No unused version catalog aliases found.")
    return 0


def main() -> int:
    return run_check(parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
