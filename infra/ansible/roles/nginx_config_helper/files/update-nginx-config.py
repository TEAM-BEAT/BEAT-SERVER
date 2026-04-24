#!/usr/bin/env python3

from __future__ import annotations

import argparse
import fcntl
import os
import re
import tempfile
from pathlib import Path

UPSTREAM_INCLUDE_MARKER = "BEAT MANAGED GENERATED UPSTREAM INCLUDES"
ROUTE_INCLUDE_MARKER = "BEAT MANAGED GENERATED ROUTE INCLUDES"


def normalize_text(text: str) -> str:
    return re.sub(r"\n{3,}", "\n\n", text.strip() + "\n")


def read_text_or_empty(path: Path) -> str:
    return path.read_text() if path.exists() else ""


def write_normalized(path: Path, text: str) -> bool:
    normalized = normalize_text(text)
    path.parent.mkdir(parents=True, exist_ok=True)
    lock_path = path.parent / (path.name + ".lock")
    with open(lock_path, "w") as lock_fh:
        fcntl.flock(lock_fh.fileno(), fcntl.LOCK_EX)
        current = read_text_or_empty(path)
        if path.exists() and normalize_text(current) == normalized:
            return False
        fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".tmp_" + path.name)
        try:
            with os.fdopen(fd, "w") as f:
                f.write(normalized)
            os.replace(tmp, path)
        except Exception:
            try:
                os.unlink(tmp)
            except OSError:
                pass
            raise
    return True


def upsert_managed_block(body: str, marker: str, block_body: str) -> str:
    pattern = re.compile(
        rf"\n?# BEGIN {re.escape(marker)}.*?# END {re.escape(marker)}\n?",
        flags=re.S,
    )
    replacement = f"# BEGIN {marker}\n{block_body.rstrip()}\n# END {marker}\n"
    if pattern.search(body):
        body = pattern.sub(replacement, body)
    else:
        body = f"{body.rstrip()}\n\n{replacement}" if body.strip() else replacement
    return re.sub(r"\n{3,}", "\n\n", body).strip() + "\n"


def bootstrap_includes(path: Path, upstream_include_glob: str, route_include_glob: str) -> bool:
    text = path.read_text()

    upstream_block = (
        f"# BEGIN {UPSTREAM_INCLUDE_MARKER}\n"
        f"include {upstream_include_glob};\n"
        f"# END {UPSTREAM_INCLUDE_MARKER}\n"
    )
    if upstream_block not in text:
        server_match = re.search(r"^\s*server\s*\{", text, flags=re.M)
        insert_at = server_match.start() if server_match else 0
        text = f"{text[:insert_at].rstrip()}\n\n{upstream_block}\n{text[insert_at:].lstrip()}"

    route_block = (
        f"    # BEGIN {ROUTE_INCLUDE_MARKER}\n"
        f"    include {route_include_glob};\n"
        f"    # END {ROUTE_INCLUDE_MARKER}\n"
    )
    route_pattern = re.compile(
        rf"\n?\s*# BEGIN {re.escape(ROUTE_INCLUDE_MARKER)}.*?# END {re.escape(ROUTE_INCLUDE_MARKER)}\n?",
        flags=re.S,
    )
    if route_pattern.search(text):
        text = route_pattern.sub(f"\n{route_block}", text)
    else:
        server_open_pattern = re.compile(r"(\n\s*server\s*\{\n)")
        if not server_open_pattern.search(text):
            raise SystemExit("Could not locate server block to insert generated route include")
        text = server_open_pattern.sub(rf"\1{route_block}\n", text, count=1)

    return write_normalized(path, text)


def upsert_upstream(path: Path, upstream_name: str, container_name: str, backend_port: str) -> bool:
    marker = f"BEAT MANAGED UPSTREAM {upstream_name}"
    block_body = (
        f"upstream {upstream_name} {{\n"
        f"    server {container_name}:{backend_port};\n"
        f"}}"
    )
    text = upsert_managed_block(read_text_or_empty(path), marker, block_body)
    return write_normalized(path, text)


def ensure_route(path: Path, upstream_name: str, external_path: str, upstream_path: str) -> bool:
    normalized_external_path = external_path.rstrip("/")
    normalized_upstream_path = upstream_path if upstream_path.startswith("/") else f"/{upstream_path}"
    if not normalized_upstream_path.endswith("/"):
        normalized_upstream_path = f"{normalized_upstream_path}/"

    marker = f"BEAT MANAGED ROUTE {normalized_external_path}"
    block_body = (
        f"location {normalized_external_path}/ {{\n"
        f"    proxy_pass http://{upstream_name}{normalized_upstream_path};\n"
        f"    proxy_redirect off;\n"
        f"    proxy_set_header Host $host;\n"
        f"    proxy_set_header X-Real-IP $remote_addr;\n"
        f"    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
        f"    proxy_set_header X-Forwarded-Proto $scheme;\n"
        f"    proxy_set_header X-Request-ID $request_id;\n"
        f"}}"
    )
    text = upsert_managed_block(read_text_or_empty(path), marker, block_body)
    return write_normalized(path, text)



def parse_mapping(value: str) -> tuple[str, str]:
    if "=" not in value:
        raise argparse.ArgumentTypeError("mapping must be formatted as upstream_name=fragment_file")
    upstream_name, fragment_file = value.split("=", 1)
    if not upstream_name or not fragment_file:
        raise argparse.ArgumentTypeError("mapping must include both upstream_name and fragment_file")
    return upstream_name, fragment_file


def extract_managed_or_upstream_block(text: str, upstream_name: str) -> str | None:
    marker = f"BEAT MANAGED UPSTREAM {upstream_name}"
    managed_pattern = re.compile(
        rf"# BEGIN {re.escape(marker)}\n.*?# END {re.escape(marker)}",
        flags=re.S,
    )
    managed_match = managed_pattern.search(text)
    if managed_match:
        return managed_match.group(0).strip() + "\n"

    upstream_pattern = re.compile(
        rf"(^|\n)(upstream\s+{re.escape(upstream_name)}\s*\{{.*?\n\s*\}})",
        flags=re.S,
    )
    upstream_match = upstream_pattern.search(text)
    if upstream_match:
        return upstream_match.group(2).strip() + "\n"
    return None


def split_upstreams(
    source: Path,
    output_dir: Path,
    mappings: list[tuple[str, str]],
    require_all: bool,
    skip_existing: bool,
) -> bool:
    if not source.exists():
        return False

    text = source.read_text()
    changed = False
    for upstream_name, fragment_file in mappings:
        fragment_path = output_dir / fragment_file
        if skip_existing and fragment_path.exists():
            continue
        block = extract_managed_or_upstream_block(text, upstream_name)
        if block is None:
            if require_all:
                raise SystemExit(f"Required upstream '{upstream_name}' was not found in {source}")
            continue
        changed = write_normalized(fragment_path, block) or changed
    return changed


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    subcommands = parser.add_subparsers(dest="command", required=True)

    bootstrap_parser = subcommands.add_parser("bootstrap-includes")
    bootstrap_parser.add_argument("--path", required=True)
    bootstrap_parser.add_argument("--upstream-include-glob", required=True)
    bootstrap_parser.add_argument("--route-include-glob", required=True)

    upsert_upstream_parser = subcommands.add_parser("upsert-upstream")
    upsert_upstream_parser.add_argument("--path", required=True)
    upsert_upstream_parser.add_argument("--upstream-name", required=True)
    upsert_upstream_parser.add_argument("--container-name", required=True)
    upsert_upstream_parser.add_argument("--backend-port", required=True)

    ensure_route_parser = subcommands.add_parser("ensure-route")
    ensure_route_parser.add_argument("--path", required=True)
    ensure_route_parser.add_argument("--upstream-name", required=True)
    ensure_route_parser.add_argument("--external-path", required=True)
    ensure_route_parser.add_argument("--upstream-path", required=True)

    split_upstreams_parser = subcommands.add_parser("split-upstreams")
    split_upstreams_parser.add_argument("--source", required=True)
    split_upstreams_parser.add_argument("--output-dir", required=True)
    split_upstreams_parser.add_argument("--mapping", action="append", type=parse_mapping, required=True)
    split_upstreams_parser.add_argument("--require-all", action="store_true")
    split_upstreams_parser.add_argument("--skip-existing", action="store_true")

    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.command == "bootstrap-includes":
        changed = bootstrap_includes(
            Path(args.path),
            args.upstream_include_glob,
            args.route_include_glob,
        )
    elif args.command == "upsert-upstream":
        changed = upsert_upstream(
            Path(args.path),
            args.upstream_name,
            args.container_name,
            args.backend_port,
        )
    elif args.command == "ensure-route":
        changed = ensure_route(
            Path(args.path),
            args.upstream_name,
            args.external_path,
            args.upstream_path,
        )
    else:
        changed = split_upstreams(
            Path(args.source),
            Path(args.output_dir),
            args.mapping,
            args.require_all,
            args.skip_existing,
        )
    print(f"changed={'true' if changed else 'false'}")


if __name__ == "__main__":
    main()
