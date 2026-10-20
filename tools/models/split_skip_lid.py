#!/usr/bin/env python3
"""M13 (BUG-006): splits the ore skip's one-piece lid into two doors that open outward to the sides.

Derived from the existing lid (models/block/ore_skip_lid_closed.json and its Blockbench source), so the
owner's painted ore_skip.png is used as it is: each door takes its half of the lid plate's painted
regions (the cut edge reuses the plate's end-face paint), and the hinge bar along the back stays as a
fixed rim. Nothing is repacked and no texture is written. Writes:

  models/block/ore_skip_lid_left.json, _lid_right.json, _lid_rim.json   (and their .bbmodel sources)

It never overwrites an existing file unless run with --force.

usage: python3 tools/models/split_skip_lid.py [--force]
"""
import copy
import json
import os
import sys
import uuid

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
MODELS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block', 'models', 'block')
ART = os.path.join(ROOT, 'art', 'blockbench')
FORCE = '--force' in sys.argv
SPLIT_X = 8.0


def lerp(a, b, t):
    return round(a + (b - a) * t, 4)


def part_uv(uv, t0, t1, flip=False):
    """The u range [t0, t1] (fractions along x) of a face's uv rect; flipped for faces whose u runs east to west."""
    u0, v0, u1, v1 = uv
    if flip:
        t0, t1 = 1.0 - t1, 1.0 - t0
    return [lerp(u0, u1, t0), v0, lerp(u0, u1, t1), v1]


def door(plate, left):
    """One half of the lid plate: x from its own outer edge to the split, faces cut from the plate's."""
    x0, x1 = plate['from'][0], plate['to'][0]
    t = (SPLIT_X - x0) / (x1 - x0)
    t0, t1 = (0.0, t) if left else (t, 1.0)
    d = copy.deepcopy(plate)
    d['name'] = 'lid_door_left' if left else 'lid_door_right'
    if left:
        d['to'][0] = SPLIT_X
    else:
        d['from'][0] = SPLIT_X
    f = d['faces']
    for face in ('up', 'down', 'south'):
        f[face]['uv'] = part_uv(plate['faces'][face]['uv'], t0, t1)
    f['north']['uv'] = part_uv(plate['faces']['north']['uv'], t0, t1, flip=True)
    # The cut edge takes the paint of the plate's own end face on that side.
    if left:
        f['east']['uv'] = list(plate['faces']['east']['uv'])
    else:
        f['west']['uv'] = list(plate['faces']['west']['uv'])
    return d


def write(path, obj):
    rel = os.path.relpath(path, ROOT)
    if os.path.exists(path) and not FORCE:
        print('kept (exists):', rel)
        return
    with open(path, 'w') as out:
        out.write(json.dumps(obj, indent='\t') + '\n')
    print('written:', rel)


def main():
    model = json.load(open(os.path.join(MODELS, 'ore_skip_lid_closed.json')))
    plate = next(e for e in model['elements'] if e['name'] == 'lid_plate')
    hinge = next(e for e in model['elements'] if e['name'] == 'lid_hinge')
    project = json.load(open(os.path.join(ART, 'ore_skip_lid_closed.bbmodel')))
    bb_plate = next(e for e in project['elements'] if e['name'] == 'lid_plate')
    bb_hinge = next(e for e in project['elements'] if e['name'] == 'lid_hinge')
    parts = {
        'ore_skip_lid_left': ([door(plate, True)], [door(bb_plate, True)]),
        'ore_skip_lid_right': ([door(plate, False)], [door(bb_plate, False)]),
        'ore_skip_lid_rim': ([copy.deepcopy(hinge)], [copy.deepcopy(bb_hinge)]),
    }
    for name, (elements, bb_elements) in parts.items():
        out = dict(model)
        out['credit'] = 'Made by tools/models/split_skip_lid.py from ore_skip_lid_closed (M13, BUG-006); editable source art/blockbench/' + name + '.bbmodel'
        out['elements'] = elements
        write(os.path.join(MODELS, name + '.json'), out)
        bb = copy.deepcopy(project)
        bb['name'] = name
        bb['export_path'] = os.path.relpath(os.path.join(MODELS, name + '.json'), ART)
        for i, e in enumerate(bb_elements):
            e['uuid'] = str(uuid.uuid5(uuid.NAMESPACE_URL, 'simple_block/' + name + '/' + str(i)))
        bb['elements'] = bb_elements
        bb['outliner'] = [e['uuid'] for e in bb_elements]
        write(os.path.join(ART, name + '.bbmodel'), bb)


if __name__ == '__main__':
    main()
