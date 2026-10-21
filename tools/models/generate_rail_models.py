#!/usr/bin/env python3
"""Scaffolds the 3D ordinary rail (I-013): models, Blockbench sources and the rail's blockstate.

The ordinary rail (minecraft:rail) is drawn with the Loading Rail's exact profile, so the two sit flush and
join: sleepers 14 x 1 x 2 px (x 1-15) every 4 px, railheads 1 x 1 px at x 3-4 and 12-13, their tops 2 px
above the rail block's floor. Only the look changes: the block, its shapes, physics, placement, recipe
and drops stay vanilla's. Every face takes vanilla's own rail texture (minecraft:block/rail): sleepers its
wooden rows (each sleeper its own pair), railheads its steel columns, so resource packs still apply. The
Loading Rail's sleepers and rails use the same faces (vanilla_rail_faces, applied by
tools/models/apply_vanilla_rail_texture.py); only its stop keeps textures/block/loading_track.png.

  assets/simple_block/models/block/rail/straight.json   north-south (east-west turned 90 degrees)
  assets/simple_block/models/block/rail/curve.json      south to east, round the south-east corner (the
                                                        other curves turned)
  assets/simple_block/models/block/rail/ascending.json  rising to the north (the other slopes turned)
  assets/minecraft/blockstates/rail.json                vanilla's rail shapes -> these models
  art/blockbench/rail_<name>.bbmodel                    editable sources

Curves follow the quarter circles the railheads make round the block's corner (radii 3.5 and 12.5 px to
their middles), in pieces turned by the 22.5-degree steps block models allow. Slopes are the straight
track tilted 45 degrees, its layers kept 1 px thick measured upright so the railheads meet the flat
rails above and below at the right height.

It never overwrites an existing file unless run with --force.

usage: python3 tools/models/generate_rail_models.py [--force]
"""
import json
import math
import os
import sys
import uuid

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets')
ART = os.path.join(ROOT, 'art', 'blockbench')
FORCE = '--force' in sys.argv
TEXTURE = 'minecraft:block/rail'
written, skipped = [], []

# Vanilla's rail texture (16 x 16, north up): steel railheads in columns 2-3 and 12-13 (the outer column
# lighter), wooden sleepers in row pairs 1-2, 5-6, 9-10 and 13-14 across columns 1-14 (the first row lit,
# the second its shaded edge).
RAIL_TOP_COLUMN = 2
RAIL_SIDE_COLUMN = 3
SLEEPER_ROWS = (1, 5, 9, 13)


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


def r(v):
    return round(v, 4)


def rail_faces(length, along):
    """A railhead's faces on vanilla's steel columns, cropped to its length (stretched if longer than a block)."""
    v = min(length, 16.0)
    top = [RAIL_TOP_COLUMN, 0, RAIL_TOP_COLUMN + 1, v]
    side = [RAIL_SIDE_COLUMN, 0, RAIL_SIDE_COLUMN + 1, v]
    end = [RAIL_TOP_COLUMN, 0, RAIL_TOP_COLUMN + 1, 1]
    if along == 'z':
        return {'up': {'uv': top}, 'down': {'uv': top}, 'east': {'uv': side, 'rotation': 90}, 'west': {'uv': side, 'rotation': 90},
                'north': {'uv': end}, 'south': {'uv': end}}
    return {'up': {'uv': top, 'rotation': 90}, 'down': {'uv': top, 'rotation': 90}, 'north': {'uv': side, 'rotation': 90},
            'south': {'uv': side, 'rotation': 90}, 'east': {'uv': end}, 'west': {'uv': end}}


def sleeper_faces(length, along, index=0):
    """A sleeper's faces on vanilla's wooden rows (pair {@code index}), centred and cropped to its length."""
    row = SLEEPER_ROWS[index % 4]
    u0 = 8 - min(length, 14.0) / 2
    u1 = 16 - u0
    top = [u0, row, u1, row + 2]
    edge = [u0, row + 1, u1, row + 2]
    end = [1, row, 2, row + 2]
    if along == 'x':
        return {'up': {'uv': top}, 'down': {'uv': top}, 'north': {'uv': edge}, 'south': {'uv': edge},
                'east': {'uv': end, 'rotation': 90}, 'west': {'uv': end, 'rotation': 90}}
    return {'up': {'uv': top, 'rotation': 90}, 'down': {'uv': top, 'rotation': 90}, 'east': {'uv': edge}, 'west': {'uv': edge},
            'north': {'uv': end, 'rotation': 90}, 'south': {'uv': end, 'rotation': 90}}


def vanilla_rail_faces(name, frm, to):
    """The vanilla-textured faces of a Loading Rail or rail element by its name (sleeper_N, rail_*), or None."""
    if name.startswith('sleeper_'):
        index = int(name.split('_')[1]) - 1 if name.split('_')[1].isdigit() else 0
        return sleeper_faces(to[0] - frm[0], 'x', index)
    if name.startswith('rail_'):
        return rail_faces(to[2] - frm[2], 'z')
    return None


def element(name, frm, to, faces, rotation=None):
    e = {'name': name, 'from': [r(v) for v in frm], 'to': [r(v) for v in to]}
    if rotation:
        axis, angle, origin = rotation
        e['rotation'] = {'angle': angle, 'axis': axis, 'origin': [r(v) for v in origin]}
    e['faces'] = {k: dict(v, texture='#rail') for k, v in faces.items()}
    return e


# ---- Straight ----

def straight():
    els = [element(f'sleeper_{i + 1}', [1, 0, 1 + 4 * i], [15, 1, 3 + 4 * i], sleeper_faces(14, 'x', i)) for i in range(4)]
    els.append(element('rail_west', [3, 1, 0], [4, 2, 16], rail_faces(16, 'z')))
    els.append(element('rail_east', [12, 1, 0], [13, 2, 16], rail_faces(16, 'z')))
    return els


# ---- Curve, south to east round the corner (16, 16) ----

def rail_piece(name, cx, cz, length, along, angle, top):
    """A railhead piece centred on (cx, cz), along x or z, turned about y by angle."""
    h = length / 2.0
    if along == 'z':
        frm, to = [cx - 0.5, 1, cz - h], [cx + 0.5, top, cz + h]
    else:
        frm, to = [cx - h, 1, cz - 0.5], [cx + h, top, cz + 0.5]
    rot = ('y', angle, [cx, 1, cz]) if angle else None
    return element(name, frm, to, rail_faces(length, along), rot)


def curve():
    els = []
    c = 16.0
    for label, radius in (('outer', 12.5), ('inner', 3.5)):
        end = radius * math.sin(math.radians(11.25)) + 0.25
        # Straight ends, along the south edge's track and the east edge's.
        els.append(element(f'rail_{label}_south', [c - radius - 0.5, 1, c - end], [c - radius + 0.5, 2, c], rail_faces(end, 'z')))
        els.append(element(f'rail_{label}_east', [c - end, 1, c - radius - 0.5], [c, 2, c - radius + 0.5], rail_faces(end, 'x')))
        # Chords between: centred on the chord's middle, turned to the tangent there.
        for k, (mid, along, angle) in enumerate(((22.5, 'z', -22.5), (45.0, 'z', -45.0), (67.5, 'x', 22.5))):
            half = math.radians(11.25)
            d = radius * math.cos(half)
            cx, cz = c - d * math.cos(math.radians(mid)), c - d * math.sin(math.radians(mid))
            length = 2 * radius * math.sin(half) + 0.5
            # Overlapping pieces alternate a hair in height so their tops never fight.
            els.append(rail_piece(f'rail_{label}_{int(mid * 10)}', cx, cz, length, along, angle, 2.0 + 0.01 * (k % 2 == 0)))
    # Sleepers across the track: near each end as on the straight, and one radial in the middle.
    els.append(element('sleeper_south', [1, 0, 13], [15, 1, 15], sleeper_faces(14, 'x', 3)))
    els.append(element('sleeper_east', [13, 0.01, 1], [15, 1.01, 15], sleeper_faces(14, 'z', 0)))
    m = c - 8.0 * math.cos(math.radians(45))
    els.append(element('sleeper_middle', [m - 6, 0.005, m - 1], [m + 6, 1.005, m + 1], sleeper_faces(12, 'x', 2), ('y', -45.0, [m, 0, m])))
    return els


# ---- Ascending: the straight tilted 45 degrees, rising to the north ----

def ascending():
    # Laid flat at the pivot (8, 8, 8), along z, then tilted: the underside then runs from the south edge's
    # floor (z 16, y 0) to the north edge 16 px up (z 0, y 16). Layers 1 px thick upright, 1/sqrt(2) across.
    length = 16 * math.sqrt(2)
    t = 1 / math.sqrt(2)
    z0 = 8 - length / 2
    rot = ('x', 45.0, [8, 8, 8])
    els = []
    for i in range(4):
        zc = z0 + length * (i + 0.5) / 4
        els.append(element(f'sleeper_{i + 1}', [1, 8, zc - 1], [15, 8 + t, zc + 1], sleeper_faces(14, 'x', i), rot))
    for name, x in (('rail_west', 3), ('rail_east', 12)):
        els.append(element(name, [x, 8 + t, z0 - 0.5], [x + 1, 8 + 2 * t, z0 + length + 0.5], rail_faces(length + 1, 'z'), rot))
    return els


MODELS = {'straight': straight, 'curve': curve, 'ascending': ascending}


def bbmodel(name, elements):
    """A Blockbench (Java Block/Item) project of the same elements; vanilla's rail texture is not in this repository."""
    out, outliner = [], []
    for i, e in enumerate(elements):
        uid = str(uuid.uuid5(uuid.NAMESPACE_URL, f'simple_block/rail_{name}/{i}/{e["name"]}'))
        rotation, origin = [0, 0, 0], [8, 8, 8]
        if 'rotation' in e:
            rotation['xyz'.index(e['rotation']['axis'])] = e['rotation']['angle']
            origin = e['rotation']['origin']
        faces = {}
        for k, f in e['faces'].items():
            faces[k] = {'uv': f['uv'], 'texture': 0}
            if 'rotation' in f:
                faces[k]['rotation'] = f['rotation']
        out.append({'name': e['name'], 'box_uv': False, 'rescale': False, 'locked': False, 'render_order': 'default', 'from': e['from'], 'to': e['to'],
                    'autouv': 0, 'color': i % 8, 'origin': origin, 'rotation': rotation, 'faces': faces, 'type': 'cube', 'uuid': uid})
        outliner.append(uid)
    return {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': f'rail_{name}', 'model_identifier': '', 'parent': '', 'ambientocclusion': True, 'front_gui_light': False,
        'visible_box': [1, 1, 0], 'export_path': f'../../src/main/resources/assets/simple_block/models/block/rail/{name}.json',
        'resolution': {'width': 16, 'height': 16}, 'elements': out, 'outliner': outliner,
        'textures': [vanilla_rail_bb_texture(f'simple_block/rail_{name}/texture')],
    }


def vanilla_rail_bb_texture(seed):
    """Vanilla's rail texture as a Blockbench texture entry (load it onto the project from the game)."""
    return {'path': '', 'name': 'rail.png', 'folder': 'block', 'namespace': 'minecraft', 'id': 'rail', 'width': 16, 'height': 16,
            'uv_width': 16, 'uv_height': 16, 'particle': True, 'render_mode': 'default', 'visible': True, 'saved': True,
            'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, seed))}


def main():
    for name, build in MODELS.items():
        elements = build()
        write_json(os.path.join(ASSETS, 'simple_block', 'models', 'block', 'rail', name + '.json'), {
            'credit': f'Generated by tools/models/generate_rail_models.py; editable source art/blockbench/rail_{name}.bbmodel',
            'textures': {'rail': TEXTURE, 'particle': TEXTURE},
            'elements': elements,
        })
        write_json(os.path.join(ART, f'rail_{name}.bbmodel'), bbmodel(name, elements))
    m = lambda name, y=0: dict({'model': f'simple_block:block/rail/{name}'}, **({'y': y} if y else {}))
    write_json(os.path.join(ASSETS, 'minecraft', 'blockstates', 'rail.json'), {'variants': {
        'shape=north_south': m('straight'), 'shape=east_west': m('straight', 90),
        'shape=ascending_north': m('ascending'), 'shape=ascending_east': m('ascending', 90),
        'shape=ascending_south': m('ascending', 180), 'shape=ascending_west': m('ascending', 270),
        'shape=south_east': m('curve'), 'shape=south_west': m('curve', 90),
        'shape=north_west': m('curve', 180), 'shape=north_east': m('curve', 270)}})
    print(f'{len(written)} written, {len(skipped)} kept (exists; --force overwrites)')


if __name__ == '__main__':
    main()
