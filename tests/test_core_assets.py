import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'

class CoreAssetTests(unittest.TestCase):
    def load(self, name):
        return json.loads((ROOT / name).read_text(encoding='utf-8'))

    def test_core_is_recognized_by_wrench(self):
        self.assertIn('starblazercraft:core', self.load('data/starblazercraft/tags/blocks/assembly_cores.json')['values'])

    def test_correct_tool_and_drop(self):
        for tag in ('mineable/pickaxe', 'needs_iron_tool'):
            self.assertIn('starblazercraft:core', self.load(f'data/minecraft/tags/blocks/{tag}.json')['values'])
        loot = self.load('data/starblazercraft/loot_tables/blocks/core.json')
        self.assertEqual(loot['pools'][0]['entries'][0]['name'], 'starblazercraft:core')
        self.assertEqual(loot['pools'][0]['conditions'][0]['condition'], 'minecraft:survives_explosion')

    def test_models_and_names(self):
        self.assertEqual(self.load('assets/starblazercraft/blockstates/core.json')['variants']['']['model'], 'starblazercraft:block/core')
        self.assertEqual(self.load('assets/starblazercraft/models/item/core.json')['parent'], 'starblazercraft:block/core')
        for language in ('ja_jp', 'en_us'):
            self.assertIn('block.starblazercraft.core', self.load(f'assets/starblazercraft/lang/{language}.json'))
