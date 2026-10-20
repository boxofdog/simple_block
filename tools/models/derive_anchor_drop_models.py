#!/usr/bin/env python3
"""M13 (BUG-021): the cable anchor with its roller hung lower, in 2 px steps (drop 1-4).

Derived from the existing anchor models (and their UVs, so any painting carries over): the plate and its
gusset stay under the beam, the parts hanging from the plate (the arm, the pulley's cheeks, the return
wheel's bracket) are lengthened downwards, and everything below them (axle, roller, flanges, guide,
return wheel, pulley wheel) moves down. Writes models/block/<anchor model>_drop<n>.json and rewrites
blockstates/cable_anchor.json with the drop property. Never overwrites a model unless run with --force.

usage: python3 tools/models/derive_anchor_drop_models.py [--force]
"""
import copy
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block')
MODELS = os.path.join(ASSETS, 'models', 'block')
FORCE = '--force' in sys.argv
BASES = ['cable_anchor', 'cable_anchor_guide', 'cable_anchor_wheel_front', 'cable_anchor_wheel_back', 'cable_anchor_pulley']
STEP = 2.0
PLATE_UNDERSIDE = 15.0


def lowered(model, drop):
    out = copy.deepcopy(model)
    out['credit'] = 'Made by tools/models/derive_anchor_drop_models.py (M13, BUG-021) from ' + model.get('credit', 'the anchor model')
    for e in out['elements']:
        y0, y1 = e['from'][1], e['to'][1]
        if y0 >= PLATE_UNDERSIDE - 1.0 - 1e-6:
            continue  # the plate and the gusset under it stay
        if y1 >= PLATE_UNDERSIDE - 1e-6:
            e['from'][1] = round(y0 - drop, 4)  # hangs from the plate: longer
            continue
        e['from'][1] = round(y0 - drop, 4)
        e['to'][1] = round(y1 - drop, 4)
        if 'rotation' in e:
            e['rotation']['origin'][1] = round(e['rotation']['origin'][1] - drop, 4)
    return out


def main():
    for base in BASES:
        model = json.load(open(os.path.join(MODELS, base + '.json')))
        for n in range(1, 5):
            path = os.path.join(MODELS, f'{base}_drop{n}.json')
            if os.path.exists(path) and not FORCE:
                print('kept (exists):', os.path.relpath(path, ROOT))
                continue
            with open(path, 'w') as f:
                f.write(json.dumps(lowered(model, n * STEP), indent='\t') + '\n')
            print('written:', os.path.relpath(path, ROOT))
    states = os.path.join(ASSETS, 'blockstates', 'cable_anchor.json')
    variants = json.load(open(states))['variants']
    if any('drop=' in k for k in variants):
        print('kept (has drop):', os.path.relpath(states, ROOT))
        return
    out = {}
    for key, value in variants.items():
        for n in range(0, 5):
            v = dict(value)
            if n:
                v['model'] = value['model'] + f'_drop{n}'
            out[f'drop={n},{key}'] = v
    with open(states, 'w') as f:
        f.write(json.dumps({'variants': out}, indent='\t') + '\n')
    print('written:', os.path.relpath(states, ROOT))


if __name__ == '__main__':
    main()
