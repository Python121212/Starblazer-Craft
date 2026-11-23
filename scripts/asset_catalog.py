"""Validate local assets and build an offline, dependency-free preview site."""
import argparse
import json
from pathlib import Path
import shutil
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
BUILTINS = {
    'cube_all': dict.fromkeys(('up', 'down', 'north', 'south', 'west', 'east'), '#all'),
    'cube_bottom_top': {'up': '#top', 'down': '#bottom', **dict.fromkeys(('north', 'south', 'west', 'east'), '#side')},
    'cube_column': {'up': '#end', 'down': '#end', **dict.fromkeys(('north', 'south', 'west', 'east'), '#side')},
    'cube': {side: '#' + side for side in ('up', 'down', 'north', 'south', 'west', 'east')},
    'orientable': {'up': '#top', 'down': '#top', 'north': '#front', **dict.fromkeys(('south', 'west', 'east'), '#side')},
}


def png_size(path):
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f'{path}: PNG signature')
    pos, size, image_data = 8, None, b''
    ended = False
    while pos < len(data):
        if pos + 12 > len(data):
            raise ValueError(f'{path}: truncated PNG')
        n = struct.unpack('>I', data[pos:pos + 4])[0]
        kind, payload = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + n]
        if pos + n + 12 > len(data) or zlib.crc32(kind + payload) & 0xffffffff != struct.unpack('>I', data[pos + 8 + n:pos + 12 + n])[0]:
            raise ValueError(f'{path}: PNG CRC/length')
        if kind == b'IHDR':
            size = struct.unpack('>II', payload[:8])
            if not all(size):
                raise ValueError(f'{path}: zero dimensions')
        if kind == b'IDAT':
            image_data += payload
        pos += n + 12
        if kind == b'IEND':
            ended = True
            break
    if not ended or size is None or not image_data:
        raise ValueError(f'{path}: incomplete PNG')
    zlib.decompress(image_data)
    return size


def resource(value, namespace):
    parts = value.split(':')
    ns, name = parts if len(parts) == 2 else (namespace, value)
    if not ns or not name or '..' in name or name.startswith('/') or '\\' in name:
        raise ValueError(f'Invalid resource location: {value}')
    return ns + ':' + name


def collect(roots):
    models, textures, states = {}, {}, {}
    for root in roots:
        if not root.exists():
            continue
        for ns in sorted(root.iterdir()):
            if not ns.is_dir():
                continue
            for folder, suffix, target in [('models', '.json', models), ('textures', '.png', textures), ('blockstates', '.json', states)]:
                for path in sorted((ns / folder).rglob('*' + suffix)):
                    key = ns.name + ':' + path.relative_to(ns / folder).with_suffix('').as_posix()
                    if key in target:
                        raise ValueError(f'Duplicate asset: {key}; keep examples and real assets distinct')
                    target[key] = path
    parsed = {k: json.loads(p.read_text(encoding='utf-8')) for k, p in models.items()}
    dimensions = {k: png_size(p) for k, p in textures.items()}
    for key, path in states.items():
        data = json.loads(path.read_text(encoding='utf-8'))
        applications = list(data.get('variants', {}).values()) + [v['apply'] for v in data.get('multipart', [])]
        for application in applications:
            for entry in application if isinstance(application, list) else [application]:
                ref = resource(entry['model'], key.split(':')[0])
                if ref not in models and not ref.startswith('minecraft:'):
                    raise ValueError(f'{key}: missing blockstate model {ref}')

    def resolve(key, chain=()):
        if key in chain:
            raise ValueError(f'Parent cycle: {chain + (key,)}')
        if key not in parsed:
            if key.startswith('minecraft:block/') and key.split('/')[-1] in BUILTINS:
                return {'textures': {}, 'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {s: {'texture': t} for s, t in BUILTINS[key.split('/')[-1]].items()}}]}
            if key in ('minecraft:item/generated', 'minecraft:item/handheld', 'minecraft:block/block'):
                return {'textures': {}, 'elements': []}
            if key.startswith('minecraft:'):
                return {'textures': {}, 'elements': [], 'warning': '未対応のバニラ親モデル: ' + key}
            raise ValueError(f'Missing model: {key}')
        data = parsed[key]
        base = resolve(resource(data['parent'], key.split(':')[0]), chain + (key,)) if 'parent' in data else {'textures': {}, 'elements': []}
        return {**base, **data, 'textures': {**base['textures'], **data.get('textures', {})}}

    catalog = []
    for key in sorted(models):
        model = resolve(key)
        def texture_ref(ref, chain=()):
            if ref.startswith('#'):
                name = ref[1:]
                if name in chain or name not in model['textures']:
                    raise ValueError(f'{key}: missing/cyclic texture {ref}')
                return texture_ref(model['textures'][name], chain + (name,))
            value = resource(ref, key.split(':')[0])
            if value not in textures and not value.startswith('minecraft:'):
                raise ValueError(f'{key}: missing texture {value}')
            return value
        refs = {name: texture_ref(ref) for name, ref in model['textures'].items()}
        elements = json.loads(json.dumps(model['elements']))
        for element in elements:
            for field in ('from', 'to'):
                if len(element[field]) != 3 or not all(isinstance(v, (int, float)) for v in element[field]):
                    raise ValueError(f'{key}: invalid element {field}')
            for face in element.get('faces', {}).values():
                face['texture'] = texture_ref(face['texture'])
        catalog.append({'id': key, 'elements': elements, 'textures': refs, 'warning': model.get('warning', ''), 'source': parsed[key]})
    return catalog, textures, dimensions


def build(output):
    catalog, textures, dimensions = collect([ROOT / 'src/main/resources/assets', ROOT / 'docs/examples/assets'])
    output.mkdir(parents=True, exist_ok=True)
    for path in (ROOT / 'tools/preview').iterdir():
        shutil.copy2(path, output / path.name)
    images = {}
    for key, path in textures.items():
        ns, name = key.split(':')
        relative = 'textures/' + ns + '/' + name + '.png'
        dest = output / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)
        images[key] = {'url': relative, 'size': dimensions[key]}
    (output / 'catalog.json').write_text(json.dumps({'models': catalog, 'textures': images}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Validated {len(catalog)} models / {len(textures)} PNGs; preview: {output}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, default=ROOT / 'build/preview')
    build(parser.parse_args().output)
