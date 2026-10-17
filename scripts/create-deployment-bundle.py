#!/usr/bin/env python3
"""Build every release's environment from GitHub Secrets; never print secret values."""
import os
from pathlib import Path
import re
import shutil
import sys
import tempfile


def main():
    prod_env = os.environ.get("PROD_ENV", "")
    api_key = os.environ.get("OPENAI_API_KEY", "")
    if not prod_env.strip():
        raise ValueError("PROD_ENV repository secret is required")
    if not re.fullmatch(r"sk-[A-Za-z0-9_-]+", api_key):
        raise ValueError("OPENAI_API_KEY repository secret is missing or malformed")

    root = Path(__file__).resolve().parent.parent
    overrides = (root / "config/hair-analysis.prod.env").read_text()
    # A previous value in PROD_ENV must never override the dedicated repository secret.
    lines = [line for line in (prod_env + "\n" + overrides).splitlines()
             if not re.match(r"\s*(?:export\s+)?OPENAI_API_KEY\s*=", line)]
    content = "\n".join(lines) + "\nOPENAI_API_KEY=" + api_key + "\n"
    bundle = root / "deploy-bundle"
    bundle.mkdir(mode=0o700, exist_ok=True)
    os.chmod(bundle, 0o700)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", dir=bundle, delete=False) as output:
            temporary = Path(output.name)
            os.fchmod(output.fileno(), 0o600)
            output.write(content)
        temporary.replace(bundle / ".env")
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    shutil.copyfile(root / "docker-compose.prod.yml", bundle / "docker-compose.yml")
    for name in ("check-hair-analysis-model.sh", "check-recommendation-ai.sh"):
        shutil.copyfile(root / "scripts" / name, bundle / name)
    print("Deployment bundle created; OPENAI_API_KEY supplied by repository secret.")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
