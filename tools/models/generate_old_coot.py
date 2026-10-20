#!/usr/bin/env python3
"""M14 Old Coot: one geometry description, written as
  src/client/java/.../client/coot/OldCootGeometry.java   the model the game draws (generated: do not edit)
  art/blockbench/old_coot.bbmodel                         editable Blockbench project (Modded Entity, box UV)
  src/main/resources/assets/simple_block/textures/entity/old_coot.png      placeholder texture (128 x 64)
  src/main/resources/assets/simple_block/textures/item/old_coot_spawn_egg.png  placeholder egg

A stubborn little mine hermit: short, broad, hunched and stumpy, about 1.15 blocks (18.5 model pixels)
from his feet to the top of his head (the hat sits above), his head jutting forward and low from a rounded
back, thick arms hanging forward, short bowed legs: a real 3D Minecraft villager nose (the 2 x 4 x 2 block jutting from the
face, in front of the beard), a short scraggly white beard with little cubes jutting out of it, scruffy
white hair at the back and sides, a prospector's hat with the front of its brim turned up, work clothes
and short thick arms (the right one holds the iron pickaxe).

Model space is vanilla's entity space: x across (+x his left), y down (feet at y 24), z front to back
(-z his face). Texture offsets are packed automatically (box UV).

The texture and the egg are placeholders for Joe to paint: they are written only if missing (--force
rewrites everything, including them, so never use it once they are painted). The Java and the project
are always rewritten from the parts below.

usage: python3 tools/models/generate_old_coot.py [--force]
"""
import base64
import io
import json
import os
import sys
import uuid

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
JAVA = os.path.join(ROOT, 'src', 'client', 'java', 'io', 'github', 'boxofdog', 'simpleblock', 'client', 'coot', 'OldCootGeometry.java')
BBMODEL = os.path.join(ROOT, 'art', 'blockbench', 'old_coot.bbmodel')
TEXTURE = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block', 'textures', 'entity', 'old_coot.png')
EGG = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block', 'textures', 'item', 'old_coot_spawn_egg.png')
FORCE = '--force' in sys.argv
WIDTH, HEIGHT = 128, 64

SKIN, NOSE, BEARD, HAIR, HAT, BAND, SHIRT, COAT, TROUSERS, BOOTS = (
    (176, 128, 98), (198, 110, 90), (236, 234, 226), (214, 212, 204), (110, 84, 58), (62, 46, 34), (120, 112, 98), (88, 92, 104),
    (84, 68, 52), (54, 44, 36))

# (part, parent, pivot, rotation (radians, x y z), [(cube name, min, size, colour)])
# Every size is whole pixels (box UV); the scraggly beard's half-sunk tufts are whole cubes set partly into it.
PARTS = [
    # Hunched: the broad, deep body leans forward from the hips, its back rounded up into a hump, so the
    # head juts forward and low from between the shoulders instead of sitting on top.
    ('body', None, (0, 20, 0.5), (0.4, 0, 0), [('body', (-5.5, -8, -4), (11, 8, 8), COAT)]),
    ('hump', 'body', (0, 0, 0), (0, 0, 0), [('hump', (-4.5, -9.5, 0.5), (9, 4, 5), COAT)]),
    ('head', None, (0, 13.5, -4.5), (0, 0, 0), [('head', (-4, -8, -4), (8, 8, 8), SKIN)]),
    # The villager nose: the protruding block, as on a villager (2 wide, 4 tall, 2 out from the face), ruddy.
    ('nose', 'head', (0, -3, 0), (0, 0, 0), [('nose', (-1, -1, -6), (2, 4, 2), NOSE)]),
    # The prospector's hat: a tall battered crown with a band and a dented top, a wide flat brim at the
    # sides and back, and the front of the brim turned straight up (its own part, hinged at the crown).
    ('hat', 'head', (0, 0, 0), (0, 0, 0), [
        ('crown', (-4.5, -11.5, -4.5), (9, 4, 9), HAT),
        ('crown_top', (-3.5, -12.5, -3.5), (7, 1, 7), HAT),
        ('band', (-5, -9, -5), (10, 1, 10), BAND),
        ('brim', (-6.5, -8, -4.5), (13, 1, 11), HAT),
    ]),
    ('brim_front', 'head', (0, -7.5, -4.5), (-1.2, 0, 0), [('brim_front', (-6.5, -0.5, -3), (13, 1, 3), HAT)]),
    # Scruffy white hair at the back and sides of the head, under the brim, with an uneven lower edge.
    ('hair', 'head', (0, 0, 0), (0, 0, 0), [
        ('hair_back', (-4.5, -7.5, 3.5), (9, 6, 2), HAIR),
        ('hair_left', (3.2, -7.5, -1), (2, 5, 5), HAIR),
        ('hair_right', (-5.2, -7.5, -1), (2, 5, 5), HAIR),
        ('hair_tuft_0', (-3.5, -2, 4.6), (2, 2, 1), HAIR),
        ('hair_tuft_1', (0.5, -2, 4.3), (2, 1, 1), HAIR),
        ('hair_tuft_2', (4.4, -3, 0.5), (1, 2, 2), HAIR),
        ('hair_tuft_3', (-5.4, -3, 1.2), (1, 2, 2), HAIR),
    ]),
    # The beard: short, from the cheeks and around the mouth to a few pixels below the chin, with small
    # cubes jutting forward out of it at different depths (some half sunk in) and a ragged lower edge.
    # The nose stands out in front of it.
    ('beard', 'head', (0, 0, 0), (0, 0, 0), [
        ('cheek_left', (3.6, -5, -4.6), (1, 6, 4), BEARD),
        ('cheek_right', (-4.6, -5, -4.6), (1, 6, 4), BEARD),
        ('beard', (-4.5, -3, -4.9), (9, 6, 1), BEARD),
        ('beard_low', (-3.5, 3, -4.7), (7, 1, 1), BEARD),
        ('tip_0', (-3, 4, -4.6), (1, 1, 1), BEARD),
        ('tip_1', (-0.5, 4, -4.8), (2, 1, 1), BEARD),
        ('tip_2', (2, 4, -4.5), (1, 1, 1), BEARD),
        ('jut_0', (-4.3, -2.5, -5.6), (1, 1, 1), BEARD),
        ('jut_1', (2.4, -2, -5.4), (1, 1, 1), BEARD),
        ('jut_2', (-2.6, 0.2, -5.8), (1, 1, 1), BEARD),
        ('jut_3', (1.3, 0.6, -5.3), (1, 1, 1), BEARD),
        ('jut_4', (3.3, 0.8, -5.7), (1, 1, 1), BEARD),
        ('jut_5', (-0.6, 1.4, -6.1), (1, 1, 1), BEARD),
        ('jut_6', (-3.9, 1.9, -5.4), (1, 1, 1), BEARD),
        ('jut_7', (0.9, 2.6, -5.6), (2, 1, 1), BEARD),
        ('jut_8', (-2.2, 3.1, -5.3), (1, 1, 1), BEARD),
        ('jut_9', (2.6, 3.3, -5.2), (1, 1, 1), BEARD),
    ]),
    # Two stray tufts sticking out at odd angles.
    ('stray_left', 'beard', (3.5, 2, -5.4), (-0.5, 0, -0.6), [('stray_left', (-0.5, -0.5, -1), (1, 1, 2), BEARD)]),
    ('stray_right', 'beard', (-3, 4, -5), (-0.6, 0, 0.5), [('stray_right', (-0.5, -0.5, -1), (1, 2, 1), BEARD)]),
    # Thick arms hanging forward from the hunched shoulders, splayed a little, nearly to his knees; the
    # right shoulder sits a touch lower than the left.
    ('right_arm', None, (-5.5, 14.5, -4), (0, 0, 0.1), [('right_arm', (-4, -1.5, -1.5), (4, 8, 3), SHIRT)]),
    ('left_arm', None, (5.5, 14, -4), (0, 0, -0.1), [('left_arm', (0, -1.5, -1.5), (4, 8, 3), SHIRT)]),
    # Short, stumpy, wide-set and slightly bowed legs.
    ('right_leg', None, (-2.8, 20, 0.5), (0, 0, 0.08), [('right_leg', (-2, 0, -2), (4, 4, 4), BOOTS)]),
    ('left_leg', None, (2.8, 20, 0.5), (0, 0, -0.08), [('left_leg', (-2, 0, -2), (4, 4, 4), BOOTS)]),
]

SHADE = {'up': 1.0, 'down': 0.55, 'north': 0.85, 'south': 0.75, 'east': 0.7, 'west': 0.8}


def footprint(size):
    w, h, d = (int(round(v)) for v in size)
    return 2 * d + 2 * w, d + h


def pack():
    """Box-UV texture offsets, shelf-packed, tallest first; returns {cube name: (u, v)}."""
    cubes = [(name, size) for _, _, _, _, cs in PARTS for name, _, size, _ in cs]
    cubes.sort(key=lambda c: (-footprint(c[1])[1], -footprint(c[1])[0]))
    offsets, x, y, row = {}, 0, 0, 0
    for name, size in cubes:
        fw, fh = footprint(size)
        if x + fw > WIDTH:
            x, y, row = 0, y + row, 0
        if y + fh > HEIGHT:
            raise SystemExit('the parts do not fit a %d x %d texture' % (WIDTH, HEIGHT))
        offsets[name] = (x, y)
        x += fw
        row = max(row, fh)
    return offsets


def box_faces(u, v, size):
    w, h, d = (int(round(k)) for k in size)
    return {
        'north': [u + d, v + d, u + d + w, v + d + h],
        'east': [u, v + d, u + d, v + d + h],
        'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
        'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
        'up': [u + d + w, v + d, u + d, v],
        'down': [u + d + 2 * w, v, u + d + w, v + d],
    }


def texture(offsets):
    img = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for _, _, _, _, cubes in PARTS:
        for name, _, size, colour in cubes:
            u, v = offsets[name]
            for face, (x0, y0, x1, y1) in box_faces(u, v, size).items():
                lo_x, hi_x, lo_y, hi_y = min(x0, x1), max(x0, x1), min(y0, y1), max(y0, y1)
                if hi_x <= lo_x or hi_y <= lo_y:
                    continue
                c = tuple(int(k * SHADE[face]) for k in colour) + (255,)
                draw.rectangle([lo_x, lo_y, hi_x - 1, hi_y - 1], fill=c)
            if name == 'head':
                # Two dark eyes and a brow on the face (north), above the nose.
                fx, fy = u + 8, v + 8
                draw.rectangle([fx + 1, fy + 2, fx + 2, fy + 2], fill=(40, 40, 40, 255))
                draw.rectangle([fx + 5, fy + 2, fx + 6, fy + 2], fill=(40, 40, 40, 255))
                draw.rectangle([fx + 1, fy + 1, fx + 6, fy + 1], fill=(220, 218, 210, 255))
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def egg():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([3, 1, 12, 14], fill=(92, 70, 50, 255), outline=(48, 36, 26, 255))
    for x, y in ((5, 4), (8, 7), (6, 10), (9, 3), (10, 10)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(236, 234, 226, 255))
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def java(offsets):
    def f(v):
        return ('%.2f' % v).rstrip('0').rstrip('.') + 'F' if v != int(v) else '%d.0F' % v
    lines = []
    names = {None: 'root'}
    for part, parent, pivot, rot, cubes in PARTS:
        builder = 'CubeListBuilder.create()'
        for name, lo, size, _ in cubes:
            u, v = offsets[name]
            builder += '\n\t\t\t.texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)' % (u, v, f(lo[0]), f(lo[1]), f(lo[2]), f(size[0]), f(size[1]), f(size[2]))
        pose = 'PartPose.offset(%s, %s, %s)' % tuple(f(p) for p in pivot) if rot == (0, 0, 0) else \
            'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % (tuple(f(p) for p in pivot) + tuple(f(r) for r in rot))
        var = part.replace('_', ' ').title().replace(' ', '')
        var = var[0].lower() + var[1:]
        names[part] = var
        lines.append('\t\tPartDefinition %s = %s.addOrReplaceChild("%s", %s,\n\t\t\t%s);' % (var, names[parent], part, builder, pose))
    return '''package io.github.boxofdog.simpleblock.client.coot;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * M14 Old Coot geometry, generated by {@code tools/models/generate_old_coot.py} from the same parts as
 * {@code art/blockbench/old_coot.bbmodel}: change the parts there and run it, do not edit this file.
 */
final class OldCootGeometry {
	private OldCootGeometry() {
	}

	@SuppressWarnings("unused")
	static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
%s
		return LayerDefinition.create(mesh, %d, %d);
	}
}
''' % ('\n'.join(lines), WIDTH, HEIGHT)


def uid(*names):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'simple_block/old_coot/' + '/'.join(names)))


def bbmodel(offsets, png):
    elements, outliner_by_part, roots = [], {}, []
    world = {}
    for part, parent, pivot, rot, cubes in PARTS:
        base = world[parent] if parent else (0, 0, 0)
        world[part] = tuple(base[k] + pivot[k] for k in range(3))
    for part, parent, pivot, rot, cubes in PARTS:
        px, py, pz = world[part]
        origin = [-px, 24 - py, pz]
        children = []
        for name, lo, size, _ in cubes:
            ax, ay, az = px + lo[0], py + lo[1], pz + lo[2]
            w, h, d = size
            u, v = offsets[name]
            cid = uid(part, name)
            elements.append({
                'name': name, 'box_uv': True, 'rescale': False, 'locked': False, 'light_emission': 0, 'render_order': 'default',
                'allow_mirror_modeling': True, 'from': [-(ax + w), 24 - (ay + h), az], 'to': [-ax, 24 - ay, az + d], 'autouv': 0,
                'color': len(elements) % 8, 'origin': origin, 'uv_offset': [u, v],
                'faces': {k: {'uv': uv, 'texture': 0} for k, uv in box_faces(u, v, size).items()}, 'type': 'cube', 'uuid': cid,
            })
            children.append(cid)
        group = {'name': part, 'origin': origin, 'rotation': [-round(rot[0] * 57.2958, 2), -round(rot[1] * 57.2958, 2), round(rot[2] * 57.2958, 2)],
                 'color': 0, 'uuid': uid(part), 'export': True, 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True,
                 'autouv': 0, 'children': children}
        outliner_by_part[part] = group
        if parent:
            outliner_by_part[parent]['children'].append(group)
        else:
            roots.append(group)
    return {
        'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
        'name': 'old_coot', 'model_identifier': 'old_coot', 'modded_entity_entity_class': '', 'modded_entity_version': 'Fabric 1.17+',
        'modded_entity_flip_y': True, 'visible_box': [1, 1.25, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'unhandled_root_fields': {}, 'resolution': {'width': WIDTH, 'height': HEIGHT}, 'elements': elements, 'outliner': roots,
        'textures': [{'path': '', 'name': 'old_coot.png', 'folder': 'entity', 'namespace': 'simple_block', 'id': '0', 'width': WIDTH,
                      'height': HEIGHT, 'uv_width': WIDTH, 'uv_height': HEIGHT, 'particle': False, 'render_mode': 'default', 'visible': True,
                      'mode': 'bitmap', 'saved': True, 'uuid': uid('texture'),
                      'source': 'data:image/png;base64,' + base64.b64encode(png).decode()}],
    }


def write(path, data, binary=False, keep=False):
    if keep and os.path.exists(path) and not FORCE:
        print('kept (exists; painted art is never replaced without --force):', os.path.relpath(path, ROOT))
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb' if binary else 'w') as f:
        f.write(data)
    print('written:', os.path.relpath(path, ROOT))


def main():
    offsets = pack()
    png = texture(offsets)
    write(TEXTURE, png, binary=True, keep=True)
    write(EGG, egg(), binary=True, keep=True)
    with open(TEXTURE, 'rb') as f:
        current = f.read()
    write(JAVA, java(offsets))
    write(BBMODEL, json.dumps(bbmodel(offsets, current), indent=1) + '\n')


if __name__ == '__main__':
    main()
