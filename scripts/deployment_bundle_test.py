"""Regression checks for repeated deployment and missing-secret failures (synthetic keys only)."""
import os
from pathlib import Path
import shutil
import stat
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent


class DeploymentBundleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for name in ("scripts/create-deployment-bundle.py", "scripts/check-hair-analysis-model.sh",
                     "scripts/check-recommendation-ai.sh", "config/hair-analysis.prod.env",
                     "docker-compose.prod.yml"):
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)

    def tearDown(self):
        self.temp.cleanup()

    def bundle(self, key="sk-test-current", prod="JWT_SECRET=test\nOPENAI_API_KEY=sk-obsolete"):
        return subprocess.run(["python3", str(self.root / "scripts/create-deployment-bundle.py")],
                              env=dict(os.environ, PROD_ENV=prod, OPENAI_API_KEY=key),
                              text=True, capture_output=True)

    def test_repeated_deploy_keeps_dedicated_secret_and_other_configuration(self):
        for revision in ("first", "second"):
            result = self.bundle(prod=f"JWT_SECRET={revision}\nOPENAI_API_KEY=sk-obsolete")
            self.assertEqual(result.returncode, 0)
            content = (self.root / "deploy-bundle/.env").read_text()
            self.assertEqual(content.count("OPENAI_API_KEY="), 1)
            self.assertIn("OPENAI_API_KEY=sk-test-current\n", content)
            self.assertIn(f"JWT_SECRET={revision}\n", content)
            self.assertIn("HAIR_ANALYSIS_ENABLED=true", content)
            self.assertNotIn("sk-test-current", result.stdout + result.stderr)
        self.assertEqual(stat.S_IMODE((self.root / "deploy-bundle/.env").stat().st_mode), 0o600)
        self.assertTrue((self.root / "deploy-bundle/check-recommendation-ai.sh").is_file())

    def test_missing_or_malformed_secret_leaves_previous_bundle_intact(self):
        self.assertEqual(self.bundle().returncode, 0)
        previous = (self.root / "deploy-bundle/.env").read_bytes()
        for key in ("", "sk-line\nINJECTED=value", "sk-with space"):
            result = self.bundle(key=key)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual((self.root / "deploy-bundle/.env").read_bytes(), previous)
        self.assertNotEqual(self.bundle(prod="").returncode, 0)

    def test_key_rotation_replaces_stale_values(self):
        self.assertEqual(self.bundle().returncode, 0)
        self.assertEqual(self.bundle(key="sk-rotated").returncode, 0)
        content = (self.root / "deploy-bundle/.env").read_text()
        self.assertIn("OPENAI_API_KEY=sk-rotated\n", content)
        self.assertNotIn("sk-test-current", content)
        self.assertNotIn("sk-obsolete", content)

    def test_workflows_keep_secret_injection_and_checks(self):
        deploy = (ROOT / ".github/workflows/deploy.yml").read_text()
        ci = (ROOT / ".github/workflows/ci.yml").read_text()
        self.assertIn("OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}", deploy)
        self.assertIn("python3 scripts/create-deployment-bundle.py", deploy)
        self.assertIn("check-recommendation-ai.sh", deploy)
        self.assertIn("python3 scripts/deployment_bundle_test.py", ci)
        self.assertIn("python3 scripts/deployment_bundle_test.py", deploy)
        self.assertIn("OPENAI_API_KEY: ${OPENAI_API_KEY:?OPENAI_API_KEY is required}",
                      (ROOT / "docker-compose.prod.yml").read_text())


if __name__ == "__main__":
    unittest.main()
