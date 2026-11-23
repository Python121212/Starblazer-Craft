"""Build the real Java Forge mod. Requires Java 17 and Gradle 8.8."""
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
if not shutil.which("gradle"):
    raise SystemExit("Gradle 8.8 is required. Use GitHub Actions or install Gradle and Java 17.")
subprocess.run(["gradle", "--no-daemon", "build"], cwd=ROOT, check=True)
