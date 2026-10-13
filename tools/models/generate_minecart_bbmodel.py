#!/usr/bin/env python3
"""Writes art/blockbench/gated_minecart.bbmodel, the editable source of every minecart's body (the
owner's M07 cart redesign), from the same geometry as GatedMinecartModel.createBodyLayer().

The body is an entity model (Blockbench: Modded Entity, box UV) on vanilla's minecart texture
(textures/entity/minecart.png, 64 x 32), which is not copied into this repository: open the project and
load that texture (from the game's jar, or your resource pack) onto it. Each part is a group whose pivot
is the part's hinge; turning the group about it shows the gate opening (end gates about z, side gates
about x). The game draws the geometry in GatedMinecartModel: after changing the shape here, update the
numbers there (Blockbench's File > Export > Export Java Entity gives them).

usage: python3 tools/models/generate_minecart_bbmodel.py [--force]
"""
import json
import os
import sys
import uuid

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'art', 'blockbench', 'gated_minecart.bbmodel')

# (part, pivot (Java ModelPart, y down), rotation (degrees, Java), [(texOffs, box min, box size)])
PARTS = [
    ('bottom', (0, 4, 0), (90, 0, 0), [((0, 10), (-10, -8, -1), (20, 16, 2))]),
    ('column_00', (0, 0, 0), (0, 0, 0), [((0, 0), (-10, -5, -8), (2, 8, 2))]),
    ('column_01', (0, 0, 0), (0, 0, 0), [((14, 0), (-10, -5, 6), (2, 8, 2))]),
    ('column_10', (0, 0, 0), (0, 0, 0), [((0, 0), (8, -5, -8), (2, 8, 2))]),
    ('column_11', (0, 0, 0), (0, 0, 0), [((14, 0), (8, -5, 6), (2, 8, 2))]),
    ('rim_minus', (0, 0, 0), (0, 0, 0), [((0, 0), (-8, -5, -8), (16, 1, 2))]),
    ('rim_plus', (0, 0, 0), (0, 0, 0), [((0, 0), (-8, -5, 6), (16, 1, 2))]),
    ('gate_end_plus', (-10, 3, 0), (0, 270, 0), [((6, 0), (-6, -8, -2), (12, 8, 2))]),
    ('gate_end_minus', (10, 3, 0), (0, 90, 0), [((6, 0), (-6, -8, -2), (12, 8, 2))]),
    ('gate_side_plus', (0, -4, 8), (0, 0, 0), [((0, 1), (-8, 0, -2), (16, 7, 2))]),
    ('gate_side_minus', (0, -4, -8), (0, 180, 0), [((0, 1), (-8, 0, -2), (16, 7, 2))]),
]


def uid(*names):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'simple_block/gated_minecart/' + '/'.join(names)))


def box_faces(u, v, w, h, d):
    """Blockbench's box UV layout for a cube of size w x h x d at texture offset (u, v)."""
    return {
        'north': {'uv': [u + d, v + d, u + d + w, v + d + h], 'texture': 0},
        'east': {'uv': [u, v + d, u + d, v + d + h], 'texture': 0},
        'south': {'uv': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'texture': 0},
        'west': {'uv': [u + d + w, v + d, u + 2 * d + w, v + d + h], 'texture': 0},
        'up': {'uv': [u + d + w, v + d, u + d, v], 'texture': 0},
        'down': {'uv': [u + d + 2 * w, v, u + d + w, v + d], 'texture': 0},
    }


def main():
    if os.path.exists(OUT) and '--force' not in sys.argv:
        print('kept (exists; --force overwrites):', OUT)
        return
    elements, outliner = [], []
    for name, pivot, rot, cubes in PARTS:
        # Java (x, y down, z) to Blockbench (-x, 24 - y, z); rotations about x and y change sign.
        origin = [-pivot[0], 24 - pivot[1], pivot[2]]
        children = []
        for i, ((u, v), lo, size) in enumerate(cubes):
            ax, ay, az = (pivot[k] + lo[k] for k in range(3))
            w, h, d = size
            cid = uid(name, str(i))
            elements.append({
                'name': name, 'box_uv': True, 'rescale': False, 'locked': False, 'light_emission': 0, 'render_order': 'default',
                'allow_mirror_modeling': True, 'from': [-(ax + w), 24 - (ay + h), az], 'to': [-ax, 24 - ay, az + d], 'autouv': 0,
                'color': len(elements) % 8, 'origin': origin, 'uv_offset': [u, v], 'faces': box_faces(u, v, w, h, d), 'type': 'cube',
                'uuid': cid,
            })
            children.append(cid)
        outliner.append({
            'name': name, 'origin': origin, 'rotation': [-rot[0], -rot[1], rot[2]], 'color': 0, 'uuid': uid(name), 'export': True,
            'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'children': children,
        })
    model = {
        'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
        'name': 'gated_minecart',
        'model_identifier': 'gated_minecart',
        'modded_entity_entity_class': '',
        'modded_entity_version': 'Fabric 1.17+',
        'modded_entity_flip_y': True,
        'visible_box': [1, 1, 0],
        'variable_placeholders': '',
        'variable_placeholder_buttons': [],
        'unhandled_root_fields': {},
        'resolution': {'width': 64, 'height': 32},
        'elements': elements,
        'outliner': outliner,
        'textures': [],
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w') as f:
        json.dump(model, f, indent=1)
        f.write('\n')
    print('written:', OUT)


if __name__ == '__main__':
    main()
