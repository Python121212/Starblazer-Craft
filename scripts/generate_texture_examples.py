"""Generate original pixel-art examples and a nearest-neighbor guide (stdlib only)."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'docs/examples/assets/starblazercraft'


def write_png(path, pixels):
    height, width = len(pixels), len(pixels[0])
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    raw = b''.join(b'\0' + bytes(c for pixel in row for c in pixel) for row in pixels)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def texture(kind):
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if kind == 'item':
                distance = abs(x - 7) + abs(y - 7)
                color = (0, 0, 0, 0) if distance > 6 else ((25, 69, 93, 255) if distance == 6 else ((167, 246, 255, 255) if x + y < 13 else (48, 176, 205, 255)))
            else:
                edge = x in (0, 15) or y in (0, 15)
                if kind == 'top':
                    color = (29, 59, 83, 255) if edge else ((93, 209, 216, 255) if x in (3, 12) or y in (3, 12) else (48, 112, 143, 255))
                else:
                    color = (29, 59, 83, 255) if edge or y in (4, 12) else ((67, 149, 172, 255) if (x + y) % 4 == 0 else (43, 99, 130, 255))
            row.append(color)
        pixels.append(row)
    return pixels


def main():
    textures = [texture('top'), texture('side'), texture('item')]
    for relative, pixels in zip(('block/example_block_top.png', 'block/example_block_side.png', 'item/example_crystal.png'), textures):
        write_png(ASSETS / 'textures' / relative, pixels)
    # Three panels, 16x nearest-neighbor enlargement; checkerboard reveals alpha.
    preview = [[(20, 29, 44, 255) for _ in range(832)] for _ in range(288)]
    for panel, pixels in enumerate(textures):
        for y in range(256):
            for x in range(256):
                p = pixels[y // 16][x // 16]
                background = (205, 211, 219, 255) if (x // 16 + y // 16) % 2 else (244, 246, 249, 255)
                color = p if p[3] else background
                # Grid lines are for documentation only, not included in textures.
                if x % 16 == 0 or y % 16 == 0:
                    color = tuple(int(c * .75) for c in color[:3]) + (255,)
                preview[y + 16][16 + panel * 272 + x] = color
    write_png(ROOT / 'docs/images/texture-guide.png', preview)


if __name__ == '__main__':
    main()
