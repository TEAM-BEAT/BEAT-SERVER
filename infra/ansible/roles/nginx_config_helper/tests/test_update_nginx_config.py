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


if __name__ == "__main__":
    unittest.main()
