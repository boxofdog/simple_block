#!/usr/bin/env python3
"""Puts the Loading Rail's sleepers and rails on vanilla's rail texture, leaving its stop on loading_track.png.

The Loading Rail shares the ordinary rail's profile (tools/models/generate_rail_models.py), so its sleeper_N and
rail_* elements take exactly the same vanilla faces (vanilla_rail_faces). The hinged stop (stop_*) is the
only part of the Loading Rail unlike a vanilla rail and keeps textures/block/loading_track.png for the owner
to paint. Rewrites, in place, only those elements' faces and the texture list of:

  assets/simple_block/models/block/loading_track.json, loading_track_raised.json
  art/blockbench/loading_track.bbmodel, loading_track_raised.bbmodel  (vanilla's texture added as texture 1,
                                                                       not embedded: load it from the game)

Running it again changes nothing.

usage: python3 tools/models/apply_vanilla_rail_texture.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate_rail_models import ART, ASSETS, TEXTURE, vanilla_rail_bb_texture, vanilla_rail_faces  # noqa: E402

NAMES = ('loading_track', 'loading_track_raised')


def model(name):
    path = os.path.join(ASSETS, 'simple_block', 'models', 'block', name + '.json')
    with open(path) as f:
        data = json.load(f)
    data['textures']['rail'] = TEXTURE
    data['textures']['particle'] = TEXTURE
    for e in data['elements']:
        faces = vanilla_rail_faces(e.get('name', ''), e['from'], e['to'])
        if faces is None:
            continue
        e['faces'] = {k: dict(v, texture='#rail', **({'cullface': e['faces'][k]['cullface']} if 'cullface' in e['faces'].get(k, {}) else {}))
                      for k, v in faces.items()}
    with open(path, 'w') as f:
        f.write(json.dumps(data, indent='\t') + '\n')
    return os.path.relpath(path)


def project(name):
    path = os.path.join(ART, name + '.bbmodel')
    with open(path) as f:
        data = json.load(f)
    textures = data['textures']
    index = next((i for i, t in enumerate(textures) if t.get('namespace') == 'minecraft' and t.get('name') == 'rail.png'), None)
    if index is None:
        textures.append(vanilla_rail_bb_texture(f'simple_block/{name}/vanilla_rail'))
        index = len(textures) - 1
    for t in textures:
        t['particle'] = t.get('namespace') == 'minecraft'
    for e in data['elements']:
        faces = vanilla_rail_faces(e.get('name', ''), e['from'], e['to'])
        if faces is None:
            continue
        e['faces'] = {k: dict({'uv': v['uv'], 'texture': index}, **({'rotation': v['rotation']} if 'rotation' in v else {})) for k, v in faces.items()}
    with open(path, 'w') as f:
        f.write(json.dumps(data, indent='\t') + '\n')
    return os.path.relpath(path)


def main():
    for name in NAMES:
        print('updated', model(name))
        print('updated', project(name))


if __name__ == '__main__':
    main()
