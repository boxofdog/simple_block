#!/usr/bin/env python3
"""M13 BUG-015/BUG-022: lists z-fighting candidates among model elements: faces that lie in one plane, face
the same way and overlap. Models given together share one space; MODEL.json@DY raises a model DY pixels (as a
renderer stacking parts does). --diff keeps only pairs whose faces show different parts of the texture
(identical texels cannot visibly flicker). Rotated elements are compared only with the same rotation.

usage: python3 tools/models/find_coplanar_faces.py [--diff] model.json[@dy] [model2.json[@dy] ...]
e.g. the Ore Skip as OreSkipRenderer stacks it, from src/main/resources/assets/simple_block/models/block:
  find_coplanar_faces.py --diff ore_skip_chute.json ore_skip_bin.json@16 ore_skip_gate_closed.json@16 \
      ore_skip_lid_left.json@48 ore_skip_lid_right.json@48 ore_skip_lid_rim.json@48 ore_skip_bridle.json@48
"""
import json, sys, itertools
AX = {'north': (2, 0), 'south': (2, 1), 'west': (0, 0), 'east': (0, 1), 'down': (1, 0), 'up': (1, 1)}
FLAGS = [x for x in sys.argv[1:] if x.startswith('--')]
els = []
for arg in [x for x in sys.argv[1:] if not x.startswith('--')]:
    path, _, dy = arg.partition('@')
    dy = float(dy or 0)
    for e in json.load(open(path)).get('elements', []):
        e = dict(e, **{'from': [e['from'][0], e['from'][1] + dy, e['from'][2]], 'to': [e['to'][0], e['to'][1] + dy, e['to'][2]]})
        if e.get('rotation'):
            r = dict(e['rotation']); r['origin'] = [r['origin'][0], r['origin'][1] + dy, r['origin'][2]]; e['rotation'] = r
        els.append((path.split('/')[-1].replace('.json', ''), e))
out = []
for (ma, a), (mb, b) in itertools.combinations(els, 2):
    if a.get('rotation') or b.get('rotation'):
        # Only same rotation (axis, angle, origin) can share planes exactly; compare in the rotated frame.
        if a.get('rotation') != b.get('rotation'):
            continue
    for face, (axis, hi) in AX.items():
        if face not in a.get('faces', {}) or face not in b.get('faces', {}):
            continue
        pa = a['to'][axis] if hi else a['from'][axis]
        pb = b['to'][axis] if hi else b['from'][axis]
        if abs(pa - pb) > 1e-4:
            continue
        o = [i for i in range(3) if i != axis]
        area = 1.0
        for i in o:
            lo = max(a['from'][i], b['from'][i]); hi2 = min(a['to'][i], b['to'][i])
            if hi2 - lo <= 1e-4:
                area = 0; break
            area *= hi2 - lo
        if area > 0:
            fa, fb = a['faces'][face], b['faces'][face]
            same = fa.get('uv') == fb.get('uv') and fa.get('texture') == fb.get('texture')
            if '--diff' in FLAGS and same:
                continue
            out.append(f"{ma}:{a.get('name')} / {mb}:{b.get('name')}  face {face} at {pa}  overlap {area:.2f}px2  {'same uv' if same else 'DIFFERENT uv'}")
print('\n'.join(out) or 'none')
