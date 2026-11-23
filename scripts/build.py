"""Build a reproducible, metadata-only Forge mod using Python 3.11+."""
from pathlib import Path
import tomllib
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
metadata = tomllib.loads((RESOURCES / "META-INF/mods.toml").read_text(encoding="utf-8"))
version = metadata["mods"][0]["version"]
output = ROOT / "build/libs" / f"starblazer-craft-1.20.1-{version}.jar"
output.parent.mkdir(parents=True, exist_ok=True)
entries = {"META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n"}
entries.update({p.relative_to(RESOURCES).as_posix(): p.read_bytes()
                for p in sorted(RESOURCES.rglob("*")) if p.is_file()})
with ZipFile(output, "w", compression=ZIP_DEFLATED) as jar:
    for name, content in entries.items():
        info = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        info.compress_type = ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        jar.writestr(info, content)
print(output.relative_to(ROOT))
