import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('assets', ROOT / 'scripts/asset_catalog.py')
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)


class AssetTests(unittest.TestCase):
    def test_repository_assets(self):
        models, textures, sizes = assets.collect([ROOT / 'src/main/resources/assets', ROOT / 'docs/examples/assets'])
        self.assertGreaterEqual(len(models), 3)
        self.assertEqual(sizes['starblazercraft:block/example_block_top'], (16, 16))
        block = next(m for m in models if m['id'] == 'starblazercraft:block/example_block')
        self.assertEqual(len(block['elements'][0]['faces']), 6)

    def test_errors(self):
        for model in [
            {'parent': 'test:block/missing'},
            {'parent': 'test:block/a'},
            {'textures': {'a': '#b', 'b': '#a'}},
            {'textures': {'a': 'test:block/missing'}},
        ]:
            with self.subTest(model=model), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                p = root / 'test/models/block/a.json'
                p.parent.mkdir(parents=True)
                p.write_text(json.dumps(model))
                with self.assertRaises(ValueError):
                    assets.collect([root])

    def test_invalid_png(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'bad.png'
            path.write_bytes(b'not a PNG')
            with self.assertRaises(ValueError):
                assets.png_size(path)

    def test_java_mod_configuration(self):
        import tomllib
        metadata = tomllib.loads((ROOT / 'src/main/resources/META-INF/mods.toml').read_text())
        self.assertEqual(metadata['modLoader'], 'javafml')
        self.assertTrue((ROOT / 'src/main/java/dev/starblazer/craft/WrenchItem.java').exists())

    def test_preview_build(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            assets.build(output)
            data = json.loads((output / 'catalog.json').read_text())
            self.assertTrue((output / 'index.html').exists())
            for texture in data['textures'].values():
                self.assertTrue((output / texture['url']).is_file())


if __name__ == '__main__':
    unittest.main()
