"""Manually adapt the supplied reference to a 16px, solid extruded wrench.

Reference-only changes do not run this script or CI automatically. Run explicitly
when the user requests a new design, then regenerate the Markdown catalog.
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starblazercraft'


def generate():
    source = Image.open(ROOT / '画像/IMG_0185.png').convert('RGBA')
    # Remove the white reference background before nearest-neighbor sampling.
    source.putdata([(r, g, b, 0 if min(r, g, b) > 245 else a)
                    for r, g, b, a in source.getdata()])
    texture = source.resize((16, 16), Image.Resampling.NEAREST)
    texture.save(ASSETS / 'textures/item/wrench.png')
    path = ASSETS / 'models/item/wrench.json'
    model = json.loads(path.read_text())
    model['textures'] = {'metal': 'starblazercraft:item/wrench', 'particle': 'starblazercraft:item/wrench'}
    elements = []
    # Merge contiguous opaque pixels per row. True 2-unit thickness, not a flat
    # generated sprite. Front and back retain the reference's grayscale shading.
    for y in range(16):
        x = 0
        while x < 16:
            if texture.getpixel((x, y))[3] == 0:
                x += 1
                continue
            start = x
            while x < 16 and texture.getpixel((x, y))[3]:
                x += 1
            faces = {
                'south': {'texture': '#metal', 'uv': [start, y, x, y + 1]},
                'north': {'texture': '#metal', 'uv': [x, y, start, y + 1]},
                'east': {'texture': '#metal', 'uv': [x - 1, y, x, y + 1]},
                'west': {'texture': '#metal', 'uv': [start, y, start + 1, y + 1]},
                'up': {'texture': '#metal', 'uv': [start, y, x, y + 1]},
                'down': {'texture': '#metal', 'uv': [start, y, x, y + 1]},
            }
            elements.append({'from': [start, 15 - y, 7], 'to': [x, 16 - y, 9], 'faces': faces})
    model['elements'] = elements
    path.write_text(json.dumps(model, indent=2) + '\n')
    print(f'Wrench: 16x16, {len(elements)} extruded row segments')


if __name__ == '__main__':
    generate()
