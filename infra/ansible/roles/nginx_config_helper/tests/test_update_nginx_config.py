from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


HELPER_PATH = Path(__file__).resolve().parents[1] / "files" / "update-nginx-config.py"
SPEC = importlib.util.spec_from_file_location("update_nginx_config", HELPER_PATH)
assert SPEC is not None and SPEC.loader is not None
update_nginx_config = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(update_nginx_config)


class SplitUpstreamsSkipExistingTest(unittest.TestCase):
    def test_skip_existing_keeps_equal_fragment_unchanged(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            workdir = Path(tmp)
            source = workdir / "00-managed.conf"
            output_dir = workdir / "fragments"
            output_dir.mkdir()
            fragment = output_dir / "backend.conf"
            source.write_text(
                "# BEGIN BEAT MANAGED UPSTREAM backend\n"
                "upstream backend {\n"
                "    server backend-blue:8080;\n"
                "}\n"
                "# END BEAT MANAGED UPSTREAM backend\n",
            )
            fragment.write_text(
                "# BEGIN BEAT MANAGED UPSTREAM backend\n"
                "upstream backend {\n"
                "    server backend-blue:8080;\n"
                "}\n"
                "# END BEAT MANAGED UPSTREAM backend\n\n",
            )

            changed = update_nginx_config.split_upstreams(
                source,
                output_dir,
                [("backend", "backend.conf")],
                require_all=True,
                skip_existing=True,
            )

            self.assertFalse(changed)
            self.assertEqual(
                update_nginx_config.normalize_text(source.read_text()),
                update_nginx_config.normalize_text(fragment.read_text()),
            )

    def test_skip_existing_rejects_divergent_fragment(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            workdir = Path(tmp)
            source = workdir / "00-managed.conf"
            output_dir = workdir / "fragments"
            output_dir.mkdir()
            fragment = output_dir / "backend.conf"
            source.write_text(
                "# BEGIN BEAT MANAGED UPSTREAM backend\n"
                "upstream backend {\n"
                "    server backend-green:8080;\n"
                "}\n"
                "# END BEAT MANAGED UPSTREAM backend\n",
            )
            fragment.write_text(
                "# BEGIN BEAT MANAGED UPSTREAM backend\n"
                "upstream backend {\n"
                "    server backend-blue:8080;\n"
                "}\n"
                "# END BEAT MANAGED UPSTREAM backend\n",
            )

            with self.assertRaisesRegex(SystemExit, "differs from legacy upstream 'backend'"):
                update_nginx_config.split_upstreams(
                    source,
                    output_dir,
                    [("backend", "backend.conf")],
                    require_all=True,
                    skip_existing=True,
                )

            self.assertIn("backend-blue:8080", fragment.read_text())


if __name__ == "__main__":
    unittest.main()
