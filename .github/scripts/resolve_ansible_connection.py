#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import tempfile
import textwrap
from pathlib import Path
from typing import Any


class ResolverError(RuntimeError):
    pass


REQUIRED_FIELDS = ("ansible_host", "ansible_port", "ssh_host_fingerprint")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Resolve SSH connection metadata from an Ansible inventory.",
    )
    parser.add_argument("--inventory-path", required=True, help="Ansible inventory file path")
    parser.add_argument("--module", required=True, help="Deployment module name, e.g. apis")
    parser.add_argument(
        "--json-output",
        help="Optional file path for writing resolved metadata as JSON without logging it.",
    )
    return parser.parse_args()


def is_encrypted(value: Any) -> bool:
    return isinstance(value, str) and value.startswith("ENC[")


def require_string(value: Any, *, name: str) -> str:
    if value is None:
        raise ResolverError(f"Missing required field: {name}")
    if isinstance(value, bool):
        raise ResolverError(f"Invalid boolean value for field: {name}")
    return str(value)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def subprocess_env() -> dict[str, str]:
    env = os.environ.copy()
    env.setdefault("ANSIBLE_CONFIG", str(repo_root() / "infra/ansible/ansible.cfg"))
    return env


def run_command(command: list[str], *, cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    try:
        return subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
            cwd=cwd,
            env=subprocess_env(),
        )
    except FileNotFoundError as exc:
        raise ResolverError(f"Command is not available: {command[0]}") from exc
    except subprocess.CalledProcessError as exc:
        stderr = (exc.stderr or "").strip()
        detail = f": {stderr}" if stderr else ""
        raise ResolverError(f"{command[0]} failed{detail}") from exc


def run_ansible_inventory(*args: str) -> dict[str, Any]:
    completed = run_command(["ansible-inventory", *args])
    try:
        return json.loads(completed.stdout)
    except json.JSONDecodeError as exc:
        raise ResolverError("ansible-inventory returned invalid JSON") from exc


def resolve_target_host(inventory_path: str, module: str) -> tuple[str, dict[str, Any]]:
    group_name = f"{module}_servers"
    inventory = run_ansible_inventory("-i", inventory_path, "--list")
    group = inventory.get(group_name)
    if not isinstance(group, dict):
        raise ResolverError(f"Inventory group not found: {group_name}")

    hosts = group.get("hosts")
    if not isinstance(hosts, list) or not hosts:
        raise ResolverError(f"Inventory group has no hosts: {group_name}")
    if len(hosts) != 1:
        raise ResolverError(
            f"Inventory group {group_name} must resolve to exactly one host, found {len(hosts)}"
        )

    host_name = require_string(hosts[0], name=f"{group_name}.hosts[0]")
    host_vars = run_ansible_inventory("-i", inventory_path, "--host", host_name)
    return host_name, host_vars


def materialize_connection_with_ansible_playbook(inventory_path: str, host_name: str) -> dict[str, str]:
    playbook = textwrap.dedent(
        f"""
        - hosts: {host_name}
          gather_facts: false
          tasks:
            - name: Materialize connection metadata without logging values
              delegate_to: localhost
              copy:
                dest: "{{{{ lookup('env', 'RESOLVER_OUTPUT_PATH') }}}}"
                content: |
                  {{{{ {{
                    'ssh_host': ansible_host,
                    'ssh_port': ansible_port,
                    'ssh_host_fingerprint': ssh_host_fingerprint
                  }} | to_json }}}}
        """
    ).strip()

    with tempfile.NamedTemporaryFile("w", suffix=".yml", delete=False) as playbook_file:
        playbook_file.write(playbook)
        playbook_path = Path(playbook_file.name)

    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as output_file:
        output_path = Path(output_file.name)

    env = subprocess_env()
    env["RESOLVER_OUTPUT_PATH"] = str(output_path)

    try:
        try:
            subprocess.run(
                ["ansible-playbook", "-i", inventory_path, str(playbook_path)],
                check=True,
                capture_output=True,
                text=True,
                cwd=repo_root(),
                env=env,
            )
        except FileNotFoundError as exc:
            raise ResolverError("ansible-playbook command is not available") from exc
        except subprocess.CalledProcessError as exc:
            stderr = (exc.stderr or "").strip()
            detail = f": {stderr}" if stderr else ""
            raise ResolverError(f"ansible-playbook failed while materializing connection metadata{detail}") from exc

        try:
            payload = json.loads(output_path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ResolverError("Resolver output file was not produced by ansible-playbook") from exc
        except json.JSONDecodeError as exc:
            raise ResolverError("ansible-playbook produced invalid resolver JSON") from exc

        return {
            "ssh_host": require_string(payload.get("ssh_host"), name="ssh_host"),
            "ssh_port": require_string(payload.get("ssh_port"), name="ssh_port"),
            "ssh_host_fingerprint": require_string(
                payload.get("ssh_host_fingerprint"),
                name="ssh_host_fingerprint",
            ),
        }
    finally:
        playbook_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)


def resolve_connection(inventory_path: str, module: str) -> dict[str, str]:
    host_name, host_vars = resolve_target_host(inventory_path, module)

    direct_values = {
        "ssh_host": host_vars.get("ansible_host"),
        "ssh_port": host_vars.get("ansible_port"),
        "ssh_host_fingerprint": host_vars.get("ssh_host_fingerprint"),
    }
    if all(not is_encrypted(value) for value in direct_values.values()):
        return {
            key: require_string(value, name=key)
            for key, value in direct_values.items()
        }

    # community.sops host vars can remain encrypted in ansible-inventory --host
    # output for this repo, so we materialize the three required fields via a
    # minimal ansible-playbook rather than parsing decrypted inventory YAML.
    materialized = materialize_connection_with_ansible_playbook(inventory_path, host_name)
    encrypted_fields = [key for key, value in materialized.items() if is_encrypted(value)]
    if encrypted_fields:
        field_list = ", ".join(encrypted_fields)
        raise ResolverError(
            f"Resolved connection metadata is still encrypted for fields: {field_list}"
        )
    return materialized


def write_github_outputs(outputs: dict[str, str]) -> None:
    github_output = os.environ.get("GITHUB_OUTPUT")
    if not github_output:
        return

    with Path(github_output).open("a", encoding="utf-8") as fh:
        for key, value in outputs.items():
            fh.write(f"{key}={value}\n")


def write_json_output(path: str | None, outputs: dict[str, str]) -> None:
    if not path:
        return
    output_path = Path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(outputs), encoding="utf-8")


def main() -> int:
    args = parse_args()
    try:
        resolved = resolve_connection(args.inventory_path, args.module)
        write_github_outputs(resolved)
        write_json_output(args.json_output, resolved)
    except ResolverError as exc:
        print(f"::error::{exc}", file=sys.stderr)
        return 1

    print(
        f"Resolved SSH connection metadata for module '{args.module}' from '{args.inventory_path}'.",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
