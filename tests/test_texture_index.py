import json
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]


class TextureIndexTests(unittest.TestCase):
    def test_index_is_current(self):
        subprocess.run([sys.executable, 'scripts/generate_texture_index.py', '--check'], cwd=ROOT, check=True)

    def test_wrench_is_solid_and_has_both_hand_transforms(self):
        model = json.loads((ROOT / 'src/main/resources/assets/starblazercraft/models/item/wrench.json').read_text())
        self.assertGreaterEqual(len(model['elements']), 3)
        for element in model['elements']:
            self.assertTrue(all(b > a for a, b in zip(element['from'], element['to'])))
            self.assertEqual(set(element['faces']), {'north', 'south', 'east', 'west', 'up', 'down'})
        for pose in ('firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'):
            self.assertIn(pose, model['display'])
