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


class WrenchSilhouetteTests(unittest.TestCase):
    def test_production_texture_matches_model_silhouette(self):
        from PIL import Image
        texture = Image.open(ROOT / 'src/main/resources/assets/starblazercraft/textures/item/wrench.png').convert('RGBA')
        self.assertEqual(texture.size, (16, 16))
        opaque = set()
        for y in range(16):
            for x in range(16):
                r, g, b, a = texture.getpixel((x, y))
                if a:
                    self.assertEqual(r, g)
                    self.assertEqual(g, b)
                    opaque.add((x, y))
        self.assertGreater(len(opaque), 20)
        self.assertLess(len(opaque), 200)
        model = json.loads((ROOT / 'src/main/resources/assets/starblazercraft/models/item/wrench.json').read_text())
        covered = set()
        for element in model['elements']:
            left, bottom, back = element['from']
            right, top, front = element['to']
            self.assertEqual(front - back, 2)
            self.assertEqual(top - bottom, 1)
            for x in range(left, right):
                self.assertNotIn((x, 16 - top), covered)
                covered.add((x, 16 - top))
        self.assertEqual(covered, opaque)
