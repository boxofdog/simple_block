#!/usr/bin/env python3
"""Scaffolds the M12 Monster Effigies and compressed monster blocks: models, Blockbench sources and
placeholder textures.

  art/blockbench/effigy/<type>_effigy.bbmodel                  editable source of each impaled monument
  assets/simple_block/models/block/effigy/<type>_effigy.json   the same geometry (two blocks tall, facing
                                                               south; the renderer turns it to the effigy's angle)
  assets/simple_block/textures/block/effigy/<type>_effigy.png  UV template (flat shaded colour per part)
  assets/simple_block/models/block/effigy/<type>_effigy_particle.json  the effigy blocks' own (empty) model
  assets/simple_block/models/item/<type>_effigy.json           the item: the monument, scaled down
  assets/simple_block/textures/block/<type>_block.png          16 x 16 placeholder for the compressed block
  blockstates and item definitions for all of them.

Every monument is a wooden stake on a small stone cairn, the monster's body and head run through by it.
It is a one-off scaffold: it never overwrites an existing file unless run with --force.

usage: python3 tools/models/generate_effigy_models.py [--force]
"""
import io
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import generate_models as gm  # noqa: E402  (Cube, pack, template_png, model_json, bbmodel)

ROOT = gm.ROOT
ASSETS = gm.ASSETS
ART = os.path.join(gm.ART, 'effigy')
FORCE = '--force' in sys.argv
NS = gm.NAMESPACE
written, skipped = [], []
C = gm.Cube


def write(path, data, binary=False):
    rel = os.path.relpath(path, ROOT)
    if os.path.exists(path) and not FORCE:
        skipped.append(rel)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb' if binary else 'w') as f:
        f.write(data)
    written.append(rel)


def write_json(path, obj):
    write(path, json.dumps(obj, indent='\t') + '\n')


# The stake and cairn every monument stands on (model pixels; the front faces south, +z).
MOUNT = [
    C('cairn', [4, 0, 4], [12, 2, 12]),
    C('cairn_top', [5.5, 2, 5.5], [10.5, 3, 10.5]),
    C('stake', [7, 0, 7], [9, 26, 9]),
]

FIGURES = {
    # A zombie's head and shirted torso, its arms held out in front as in life.
    'zombie': [
        C('torso', [4, 12, 6], [12, 24, 10]),
        C('arm_left', [12, 20, 6], [16, 24, 16]),
        C('arm_right', [0, 20, 6], [4, 24, 16]),
        C('head', [4, 24, 4], [12, 32, 12]),
    ],
    # A skeleton's skull, spine and ribcage, its thin arms hanging.
    'skeleton': [
        C('spine', [7, 12, 8.5], [9, 24, 9.5]),
        C('ribs', [5, 16, 6], [11, 23, 10]),
        C('pelvis', [5, 12, 7], [11, 14, 9]),
        C('arm_left', [12, 11, 7], [14, 23, 9]),
        C('arm_right', [2, 11, 7], [4, 23, 9]),
        C('shoulders', [4, 22, 7], [12, 24, 9]),
        C('head', [4, 24, 4], [12, 32, 12]),
    ],
    # A creeper's head and body, its four stubby feet dangling.
    'creeper': [
        C('body', [4, 10, 6], [12, 22, 10]),
        C('foot_front_left', [8, 6, 10], [12, 10, 14], uv_key='foot'),
        C('foot_front_right', [4, 6, 10], [8, 10, 14], uv_key='foot'),
        C('foot_back_left', [8, 6, 2], [12, 10, 6], uv_key='foot'),
        C('foot_back_right', [4, 6, 2], [8, 10, 6], uv_key='foot'),
        C('head', [4, 22, 4], [12, 30, 12]),
    ],
    # An enderman's head and narrow body, its long arms hanging almost to the cairn.
    'enderman': [
        C('torso', [4.5, 14, 6.5], [11.5, 24, 9.5]),
        C('arm_left', [12, 4, 7], [14, 24, 9]),
        C('arm_right', [2, 4, 7], [4, 24, 9]),
        C('head', [4, 24, 4], [12, 32, 12]),
    ],
}

WOOD, STONE = (122, 92, 58), (120, 120, 116)
COLOURS = {
    'zombie': {'torso': (60, 140, 150), 'arm_left': (90, 140, 70), 'arm_right': (90, 140, 70), 'head': (96, 150, 76)},
    'skeleton': {k: (210, 210, 200) for k in ('spine', 'ribs', 'pelvis', 'arm_left', 'arm_right', 'shoulders', 'head')},
    'creeper': {'body': (90, 170, 80), 'foot': (70, 150, 64), 'head': (100, 186, 88)},
    'enderman': {'torso': (28, 24, 34), 'arm_left': (24, 20, 30), 'arm_right': (24, 20, 30), 'head': (34, 28, 42)},
}

# The compressed blocks' placeholder tones (dark, middle, light), from the drop's own colours.
BLOCK_TONES = {
    'zombie': [(92, 46, 30), (140, 72, 44), (176, 104, 70)],
    'skeleton': [(188, 184, 168), (222, 220, 206), (244, 242, 232)],
    'creeper': [(52, 52, 52), (88, 88, 88), (124, 124, 124)],
    'enderman': [(8, 52, 46), (28, 104, 92), (70, 160, 140)],
}


def png(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def block_texture(kind):
    """A 16 x 16 speckle of the drop's tones, with a darker frame so the block reads as packed stacks."""
    rng = random.Random(kind)
    dark, mid, light = BLOCK_TONES[kind]
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            roll = rng.random()
            img.putpixel((x, y), (light if roll < 0.25 else dark if roll < 0.4 else mid) + (255,))
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i), (i, 7), (7, i)):
            img.putpixel(p, dark + (255,))
    return png(img)


def main():
    m = lambda path: {'model': f'{NS}:{path}'}
    figures = {}
    for kind, figure in FIGURES.items():
        name = f'{kind}_effigy'
        cubes = MOUNT + figure
        size, regions, keys = gm.pack(cubes)
        colours = dict(COLOURS[kind], cairn=STONE, cairn_top=STONE, stake=WOOD)
        tex = gm.template_png(size, regions, keys, colours)
        texture = f'block/effigy/{name}'
        tex_file = os.path.join(ASSETS, 'textures', texture + '.png')
        write(tex_file, tex, binary=True)
        model_file = os.path.join(ASSETS, 'models', 'block', 'effigy', name + '.json')
        model = gm.model_json(name, cubes, f'{NS}:{texture}', regions, size, False)
        model['credit'] = f'Generated by tools/models/generate_effigy_models.py; editable source art/blockbench/effigy/{name}.bbmodel'
        write_json(model_file, model)
        project = gm.bbmodel(name, cubes, f'effigy/{name}', os.path.relpath(tex_file, ART), tex, regions, size, os.path.relpath(model_file, ART))
        project['textures'][0]['folder'] = 'block/effigy'
        write_json(os.path.join(ART, name + '.bbmodel'), project)
        # The effigy blocks themselves draw nothing (the renderer draws the turned figure): only particles.
        write_json(os.path.join(ASSETS, 'models', 'block', 'effigy', name + '_particle.json'),
                   {'textures': {'particle': f'{NS}:{texture}'}, 'elements': []})
        write_json(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': {
            'half=lower': m(f'block/effigy/{name}_particle'), 'half=upper': m(f'block/effigy/{name}_particle')}})
        write_json(os.path.join(ASSETS, 'models', 'item', name + '.json'), {
            'parent': f'{NS}:block/effigy/{name}',
            'display': {'gui': {'rotation': [20, 200, 0], 'translation': [0, -4, 0], 'scale': [0.42, 0.42, 0.42]},
                        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
                        'fixed': {'rotation': [0, 180, 0], 'translation': [0, -4, 0], 'scale': [0.45, 0.45, 0.45]},
                        'thirdperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -2, 0], 'scale': [0.3, 0.3, 0.3]},
                        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, -2, 0], 'scale': [0.3, 0.3, 0.3]}}})
        write_json(os.path.join(ASSETS, 'items', name + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
        figures[f'type={kind}'] = m(f'block/effigy/{name}')

        block = f'{kind}_block'
        write(os.path.join(ASSETS, 'textures', 'block', block + '.png'), block_texture(kind), binary=True)
        write_json(os.path.join(ASSETS, 'models', 'block', block + '.json'),
                   {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{block}'}})
        write_json(os.path.join(ASSETS, 'blockstates', block + '.json'), {'variants': {'': m(f'block/{block}')}})
        write_json(os.path.join(ASSETS, 'items', block + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{block}'}})
    write_json(os.path.join(ASSETS, 'blockstates', 'effigy_figure.json'), {'variants': figures})

    print('written:', *written, sep='\n  ')
    if skipped:
        print('kept (exists; --force overwrites):', *skipped, sep='\n  ')


if __name__ == '__main__':
    main()
