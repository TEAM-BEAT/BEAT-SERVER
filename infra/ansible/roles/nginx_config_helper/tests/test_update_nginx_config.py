from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path


HELPER_PATH = Path(__file__).resolve().parents[1] / "files" / "update-nginx-config.py"
SPEC = importlib.util.spec_from_file_location("update_nginx_config", HELPER_PATH)
assert SPEC is not None and SPEC.loader is not None
update_nginx_config = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(update_nginx_config)


class HelperChangedOutputTest(unittest.TestCase):
    def test_cli_emits_one_line_json_changed_output(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            fragment = Path(tmp) / "backend.conf"
            command = [
                "python3",
                str(HELPER_PATH),
                "upsert-upstream",
                "--path",
                str(fragment),
                "--upstream-name",
                "backend",
                "--container-name",
                "apis-blue",
                "--backend-port",
                "4001",
            ]

            first_run = subprocess.run(command, check=True, capture_output=True, text=True)
            second_run = subprocess.run(command, check=True, capture_output=True, text=True)

            self.assertEqual(["{\"changed\": true}"], first_run.stdout.strip().splitlines())
            self.assertEqual({"changed": True}, json.loads(first_run.stdout))
            self.assertEqual(["{\"changed\": false}"], second_run.stdout.strip().splitlines())
            self.assertEqual({"changed": False}, json.loads(second_run.stdout))

    def test_write_normalized_keeps_lock_out_of_generated_directory(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            workdir = Path(tmp)
            generated_dir = workdir / "generated"
            lock_dir = workdir / "locks"
            fragment = generated_dir / "upstreams" / "backend.conf"
            previous_lock_dir = os.environ.get("BEAT_NGINX_LOCK_DIR")
            os.environ["BEAT_NGINX_LOCK_DIR"] = str(lock_dir)
            try:
                changed = update_nginx_config.write_normalized(fragment, "upstream backend { server apis:4001; }")
            finally:
                if previous_lock_dir is None:
                    os.environ.pop("BEAT_NGINX_LOCK_DIR", None)
                else:
                    os.environ["BEAT_NGINX_LOCK_DIR"] = previous_lock_dir

            self.assertTrue(changed)
            self.assertTrue(fragment.exists())
            self.assertFalse(list(generated_dir.rglob("*.lock")))
            self.assertTrue((lock_dir / "backend.conf.lock").exists())


class ManagedBlockMarkerCollisionTest(unittest.TestCase):
    def test_upsert_rejects_block_body_containing_reserved_end_marker(self) -> None:
        marker = "BEAT MANAGED UPSTREAM backend"
        block_body = (
            "upstream backend {\n"
            "    # END BEAT MANAGED UPSTREAM backend\n"
            "    server apis-blue:4001;\n"
            "}"
        )

        with self.assertRaisesRegex(SystemExit, "contains reserved end marker"):
            update_nginx_config.upsert_managed_block("", marker, block_body)


class BootstrapIncludesTest(unittest.TestCase):
    def test_missing_config_path_fails_with_explicit_bootstrap_guidance(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            missing_path = Path(tmp) / "default.conf"

            with self.assertRaises(SystemExit) as raised:
                update_nginx_config.bootstrap_includes(
                    missing_path,
                    "/etc/nginx/generated/upstreams/*.conf",
                    "/etc/nginx/generated/routes/*.conf",
                )

            self.assertEqual(
                f"nginx config does not exist at {missing_path}; run nginx_base_config first",
                str(raised.exception),
            )

    def test_route_include_is_inserted_only_into_443_server_block(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "default.conf"
            config_path.write_text(
                """\
log_format main '$request';

server {
    listen 80;
    server_name example.com;

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name example.com;

    location / {
        proxy_pass http://backend;
    }
}
"""
            )

            changed = update_nginx_config.bootstrap_includes(
                config_path,
                "/etc/nginx/generated/upstreams/*.conf",
                "/etc/nginx/generated/routes/*.conf",
            )
            second_run_changed = update_nginx_config.bootstrap_includes(
                config_path,
                "/etc/nginx/generated/upstreams/*.conf",
                "/etc/nginx/generated/routes/*.conf",
            )
            text = config_path.read_text()
            server_blocks = text.split("server {")
            route_marker = update_nginx_config.ROUTE_INCLUDE_MARKER

            self.assertTrue(changed)
            self.assertFalse(second_run_changed)
            self.assertEqual(1, text.count(f"# BEGIN {route_marker}"))
            self.assertNotIn(route_marker, server_blocks[1])
            self.assertIn("listen 80;", server_blocks[1])
            self.assertIn(route_marker, server_blocks[2])
            self.assertIn("listen 443 ssl;", server_blocks[2])

    def test_existing_route_include_is_moved_from_80_to_443_server_block(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "default.conf"
            route_marker = update_nginx_config.ROUTE_INCLUDE_MARKER
            config_path.write_text(
                f"""\
server {{
    listen 80;
    # BEGIN {route_marker}
    include /old/routes/*.conf;
    # END {route_marker}
}}

server {{
    listen 443 ssl;
    server_name example.com;
}}
"""
            )

            changed = update_nginx_config.bootstrap_includes(
                config_path,
                "/etc/nginx/generated/upstreams/*.conf",
                "/etc/nginx/generated/routes/*.conf",
            )

            text = config_path.read_text()
            server_blocks = text.split("server {")
            self.assertTrue(changed)
            self.assertEqual(1, text.count(f"# BEGIN {route_marker}"))
            self.assertNotIn(route_marker, server_blocks[1])
            self.assertIn(route_marker, server_blocks[2])
            self.assertIn("include /etc/nginx/generated/routes/*.conf;", server_blocks[2])

    def test_route_include_fails_when_443_server_block_is_missing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "default.conf"
            config_path.write_text(
                """\
server {
    listen 80;
    server_name example.com;
}
"""
            )

            with self.assertRaisesRegex(SystemExit, "listening on 443"):
                update_nginx_config.bootstrap_includes(
                    config_path,
                    "/etc/nginx/generated/upstreams/*.conf",
                    "/etc/nginx/generated/routes/*.conf",
                )


if __name__ == "__main__":
    unittest.main()
