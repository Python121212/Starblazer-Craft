"""Fail CI if a resource-only or incomplete JAR is accidentally distributed."""
from pathlib import Path
from zipfile import ZipFile
import tomllib

root = Path(__file__).resolve().parents[1]
metadata = tomllib.loads((root / 'src/main/resources/META-INF/mods.toml').read_text())
path = root / 'build/libs' / f"starblazer-craft-1.20.1-{metadata['mods'][0]['version']}.jar"
with ZipFile(path) as jar:
    assert jar.testzip() is None
    for name in (
        'dev/starblazer/craft/StarblazerCraft.class',
        'dev/starblazer/craft/WrenchItem.class',
        'dev/starblazer/craft/environment/AtmosphereProfile.class',
        'dev/starblazer/craft/environment/AtmosphereEvents.class',
        'dev/starblazer/craft/environment/AtmosphereClient.class',
        'dev/starblazer/craft/environment/AtmosphereSky.class',
        'META-INF/mods.toml', 'pack.mcmeta',
        'assets/starblazercraft/models/item/wrench.json',
        'assets/starblazercraft/textures/item/wrench.png',
        'data/starblazercraft/tags/blocks/assembly_cores.json',
    ):
        assert name in jar.namelist(), name
    assert not any(n.startswith(('docs/', 'tests/', 'scripts/')) for n in jar.namelist())
print(f'Verified compiled MOD JAR: {path.name}')
