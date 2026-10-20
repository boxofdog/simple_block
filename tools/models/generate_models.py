#!/usr/bin/env python3
"""Scaffolds the Milestone 04 cable models: Blockbench sources, in-game models and UV templates.

From one geometry description (below) it writes, for each model:
  art/blockbench/<model>.bbmodel                          editable Blockbench project (Java Block/Item)
  src/main/resources/assets/simple_block/models/...json   the same geometry as the game loads it
  src/main/resources/assets/simple_block/textures/...png  a UV template: each cube's faces as flat,
                                                          shaded colour blocks with a darker outline
plus the blockstates and item definitions, and the cable strip template textures/entity/cable.png.

It is a one-off scaffold. Once a model or texture has been edited in Blockbench, Blockbench is the
source: export the model from there (File > Export > Export Block/Item Model) and save the painted
texture over the template. This script never overwrites an existing file unless run with --force.

usage: python3 tools/models/generate_models.py [--force]
"""
import base64
import io
import json
import math
import os
import sys
import uuid

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block')
ART = os.path.join(ROOT, 'art', 'blockbench')
FORCE = '--force' in sys.argv
NAMESPACE = 'simple_block'
TAN = math.tan(math.radians(22.5))
FACES = ('north', 'east', 'south', 'west', 'up', 'down')

written, skipped = [], []


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


def read_json(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


# ---- Geometry ----

class Cube:
    """A model element: a box from/to in model pixels, optionally rotated about one axis."""

    def __init__(self, name, frm, to, rot=None, uv_key=None):
        self.name = name
        self.frm = [float(v) for v in frm]
        self.to = [float(v) for v in to]
        self.rot = rot  # (axis, angle, origin)
        self.uv_key = uv_key or name

    def size(self):
        return [self.to[i] - self.frm[i] for i in range(3)]

    def moved(self, dx=0.0, dy=0.0, dz=0.0, name=None):
        d = (dx, dy, dz)
        rot = None
        if self.rot:
            axis, angle, origin = self.rot
            rot = (axis, angle, [origin[i] + d[i] for i in range(3)])
        return Cube(name or self.name, [self.frm[i] + d[i] for i in range(3)], [self.to[i] + d[i] for i in range(3)], rot, self.uv_key)


# BUG-001: the bands of an octagonal prism overlap, so their end faces used to lie in one plane and
# z-fought (flickering stripes on the drum's flanges). Each band after the first is now a little shorter
# at both ends (RECESS model pixels), so wherever two end faces overlap one is clearly in front.
RECESS = 0.1
AXES = 'xyz'


def prism(name, axis, a1, a2, c1, c2, r):
    """An octagonal prism along `axis` from a1 to a2, centred on (c1, c2) in the other two axes (in
    xyz order): four bands at 0, 90 and +-45 degrees. The 90-degree band is recessed by RECESS at each
    end, the two diagonal ones by twice that (see BUG-001)."""
    i = AXES.index(axis)
    j, k = [n for n in range(3) if n != i]
    w = r * TAN

    def box(lo_j, hi_j, lo_k, hi_k, inset):
        frm, to = [0.0] * 3, [0.0] * 3
        frm[i], to[i] = a1 + inset, a2 - inset
        frm[j], to[j] = c1 + lo_j, c1 + hi_j
        frm[k], to[k] = c2 + lo_k, c2 + hi_k
        return frm, to

    origin = [0.0] * 3
    origin[i], origin[j], origin[k] = (a1 + a2) / 2, c1, c2
    return [
        Cube(name + '_a', *box(-w, w, -r, r, 0.0)),
        Cube(name + '_b', *box(-r, r, -w, w, RECESS)),
        Cube(name + '_c', *box(-r, r, -w, w, 2 * RECESS), rot=(axis, 45, origin), uv_key=name + '_b'),
        Cube(name + '_d', *box(-r, r, -w, w, 2 * RECESS), rot=(axis, -45, origin), uv_key=name + '_b'),
    ]


def prism_x(name, x1, x2, cy, cz, r):
    return prism(name, 'x', x1, x2, cy, cz, r)


def ring_y(name, cx, cz, y1, y2, radius, thickness, key=None):
    """An octagonal ring of bars lying flat around a vertical axis (a loop of cable). The diagonal bars
    are a little thinner than the straight ones where they overlap at the corners (see BUG-001)."""
    key = key or name
    half = (radius + thickness / 2) * TAN
    t = thickness / 2
    bar_x = lambda z, inset: ([cx - half, y1 + inset, z - t], [cx + half, y2 - inset, z + t])
    bar_z = lambda x, inset: ([x - t, y1 + inset, cz - half], [x + t, y2 - inset, cz + half])
    origin = [cx, (y1 + y2) / 2, cz]
    cubes = []
    for i, bar in enumerate([lambda e: bar_x(cz - radius, e), lambda e: bar_x(cz + radius, e), lambda e: bar_z(cx - radius, e),
                             lambda e: bar_z(cx + radius, e)]):
        cubes.append(Cube(f'{name}_{i}', *bar(0.0), uv_key=key))
        cubes.append(Cube(f'{name}_{i}r', *bar(RECESS), rot=('y', 45, origin), uv_key=key))
    return cubes


def mirror_x(cube, name):
    return Cube(name, [16 - cube.to[0], cube.frm[1], cube.frm[2]], [16 - cube.frm[0], cube.to[1], cube.to[2]], uv_key=name)


def mirror_z(cube):
    rot = None
    if cube.rot:
        axis, angle, origin = cube.rot
        rot = (axis, -angle if axis != 'z' else angle, [origin[0], origin[1], 16 - origin[2]])
    return Cube(cube.name, [cube.frm[0], cube.frm[1], 16 - cube.to[2]], [cube.to[0], cube.to[1], 16 - cube.frm[2]], rot, cube.uv_key)


# Copper cable anchor, as placed facing north: the cable runs north-south over the roller; the
# hanger arm is on the west side only, so a connecting rod's stem can pass on the east side.
ANCHOR = [
    Cube('plate', [4, 15, 4], [12, 16, 12]),
    Cube('arm', [4.5, 8.5, 7], [6, 15, 9]),
    Cube('gusset', [6, 14, 7.5], [7.5, 15, 8.5]),
    Cube('axle', [3.5, 9.25, 7.5], [6, 10.25, 8.5]),
    Cube('roller', [6.5, 8.25, 6.5], [9.5, 11.25, 9.5]),
    Cube('flange_west', [6, 7.75, 6], [6.5, 12.5, 10]),
    Cube('flange_east', [9.5, 7.75, 6], [10, 12.5, 10]),
]
# M05 cable loop: an intermediate anchor carries the return strand on a small guide roller on the arm
# side (strand at x 3.25, beside the working strand at x 8, same height).
ANCHOR_GUIDE = ANCHOR + [
    Cube('guide_flange', [2.25, 8.5, 6.75], [2.75, 11.75, 9.25]),
    Cube('guide_roller', [2.75, 8.75, 7], [4, 11.25, 9]),
]
# The last anchor of a loop carries the return wheel: a flat pulley beyond the anchor (towards model
# north), on a bracket under the beam, turning the working strand (x 8) back into the return strand
# (x 3.25). A second model has it to the south; the block state picks the one facing away from the drum.
ANCHOR_WHEEL_FRONT = ANCHOR + [
    Cube('wheel_bracket', [5.125, 13.25, 2.5], [6.125, 15, 4.5]),
    Cube('wheel_pin', [5.125, 10.25, 2.5], [6.125, 13.25, 3.5]),
] + prism('wheel', 'y', 11.25, 12.75, 5.625, 3, 1.75) + prism('wheel_flange_low', 'y', 10.75, 11.25, 5.625, 3, 2.75) \
    + prism('wheel_flange_high', 'y', 12.75, 13.25, 5.625, 3, 2.75)
for c in ANCHOR_WHEEL_FRONT:
    if c.name.startswith('wheel_flange_'):
        c.uv_key = c.uv_key.replace('_low', '').replace('_high', '')
ANCHOR_WHEEL_BACK = ANCHOR + [mirror_z(c) for c in ANCHOR_WHEEL_FRONT[len(ANCHOR):]]

# M07 playtest patch §4: the head anchor of a shaft while a skip hangs on its cable, drawn as a pulley
# wheel under its ceiling plate (block state return=pulley). Facing north: the cable comes in from the
# south over the top of the wheel (x 8, y 12, the anchor's roller point) and drops on the north side
# (z 4) straight down to the skip's bridle; the wheel turns on an axle across the cable (x).
PULLEY = [
    Cube('mount_plate', [4, 15, 4], [12, 16, 12]),
    Cube('cheek_west', [5, 7, 6.5], [6, 15, 9.5], uv_key='cheek'),
    Cube('cheek_east', [10, 7, 6.5], [11, 15, 9.5], uv_key='cheek'),
    Cube('axle', [4.5, 7.5, 7.5], [11.5, 8.5, 8.5]),
] + prism_x('wheel', 7, 9, 8, 8, 4) + prism_x('flange_west', 6.25, 7, 8, 8, 4.5) + prism_x('flange_east', 9, 9.75, 8, 8, 4.5)
for c in PULLEY:
    if c.name.startswith('flange_'):
        c.uv_key = 'flange' + c.uv_key[c.uv_key.rindex('_'):]

# Cable drum, facing north (the cable pays out to the north): timber side frames (the block model),
# and, drawn turning by the block entity renderer about the axle (x, at y 9.5, z 8), the spool (flanges
# and the barrel, bare or wound with cable up to y 14) and the hand crank on the east side.
_frame_west = [
    Cube('rail_west', [0, 0, 1], [2, 2, 15]),
    Cube('post_front_west', [0, 2, 2], [2, 6.5, 4]),
    Cube('post_back_west', [0, 2, 12], [2, 6.5, 14]),
    Cube('beam_west', [0, 6.5, 2], [2, 8, 14]),
    Cube('bearing_west', [0, 8, 6.5], [2, 11, 9.5]),
    Cube('axle_cap_west', [-0.5, 8.75, 7.25], [0, 10.25, 8.75]),
]
DRUM_FRAME = _frame_west + [mirror_x(c, c.name.replace('west', 'east')) for c in _frame_west]
_flanges = prism_x('flange_west', 2, 3, 9.5, 8, 6) + prism_x('flange_east', 13, 14, 9.5, 8, 6)
for c in _flanges:
    c.uv_key = c.uv_key.replace('_west', '').replace('_east', '')
DRUM_SPOOL = _flanges + prism_x('barrel', 3, 13, 9.5, 8, 3.5)
DRUM_SPOOL_WOUND = _flanges + prism_x('wound_cable', 3, 13, 9.5, 8, 4.5)
DRUM_CRANK = [
    Cube('crank_hub', [16.5, 8.5, 7], [17.5, 10.5, 9]),
    Cube('crank_arm', [17.5, 9, 7.5], [18.5, 14.5, 8.5]),
    Cube('crank_handle', [18.5, 13, 7.25], [21, 14.5, 8.75]),
]
DRUM = DRUM_FRAME + DRUM_SPOOL + DRUM_CRANK

# M07: a coal winch's frame. The furnace motor REPLACES the stand on the motor (west) side: a heavy
# plinth, a firebox housing with its door on the cable side, a bearing housing joined straight onto the
# axle (where the west bearing was), a gear plate on the axle and a smokestack. The east stand stays.
_frame_east = [mirror_x(c, c.name.replace('west', 'east')) for c in _frame_west]
DRUM_MOTOR = [
    Cube('motor_plinth', [-6, 0, 1], [2, 3, 15]),
    Cube('motor_firebox', [-6, 3, 2.5], [1.5, 11, 13.5]),
    Cube('motor_crown', [-5.5, 11, 3.5], [1, 13, 12.5]),
    Cube('motor_door', [-4.5, 4, 2], [-1, 8.5, 2.5]),
    Cube('motor_ash_door', [-4.5, 3.25, 2], [-1, 3.75, 2.5]),
    Cube('motor_bearing', [-0.5, 8, 6.5], [2.5, 11, 9.5]),
    Cube('motor_gear', [1.5, 6.5, 5], [2, 12.5, 11]),
    Cube('motor_stack', [-4.5, 13, 9], [-1.5, 22, 12]),
    Cube('motor_stack_cap', [-5, 22, 8.5], [-1, 23.5, 12.5]),
]
DRUM_FRAME_POWERED = _frame_east + DRUM_MOTOR
# Hanging under a beam the block state turns the frame upside down (x 180), so the plinth is bolted to
# the beam. There the smokestack would point down; instead a short flue leaves the firebox outboard and
# rises beside the beam (built upside down here; it ends below the top of the block, clear of the beam).
DRUM_FRAME_POWERED_HANGING = _frame_east + [c for c in DRUM_MOTOR if not c.name.startswith('motor_stack')] + [
    Cube('motor_flue', [-8, 4.5, 9], [-6, 7, 12]),
    Cube('motor_flue_riser', [-10, 1, 8.75], [-7.5, 7, 12.25]),
    Cube('motor_flue_cap', [-10.5, 0, 8.25], [-7, 1, 12.75]),
]

# M06 coal winch: the crank replaced by a lever on the east bearing (M07: the furnace motor is part of
# the powered frame, DRUM_FRAME_POWERED). The lever is drawn tilted about the axle by the renderer:
# upright for NEUTRAL, back (south) for PULL, forward (north) for FEED.
DRUM_LEVER = [
    Cube('lever_plate', [16, 8, 6.5], [17, 11, 9.5]),
    Cube('lever_bar', [17, 9, 7.5], [18, 17, 8.5]),
    Cube('lever_knob', [16.75, 16.5, 7.25], [18.25, 18, 8.75]),
]

# M06 Loading Rail (id loading_track), facing north (a docked cart's front points north): a straight
# rail on sleepers with a hinged triangular iron stop on the inside edge of the west rail (owner's
# correction, 2026-10-13). Folded, it lies flat along the rail under a passing cart's floor (below
# y 3); raised (a cart docked), it stands just behind the docked cart's rear wall (z 18), reaching past
# the block's south edge.
_rail_bed = [
    Cube('sleeper_1', [1, 0, 1], [15, 1, 3], uv_key='sleeper'),
    Cube('sleeper_2', [1, 0, 5], [15, 1, 7], uv_key='sleeper'),
    Cube('sleeper_3', [1, 0, 9], [15, 1, 11], uv_key='sleeper'),
    Cube('sleeper_4', [1, 0, 13], [15, 1, 15], uv_key='sleeper'),
    Cube('rail_west', [3, 1, 0], [4, 2, 16], uv_key='rail'),
    Cube('rail_east', [12, 1, 0], [13, 2, 16], uv_key='rail'),
]
LOADING_TRACK = _rail_bed + [
    Cube('stop_hinge', [3.75, 1, 16], [6.75, 2.5, 17], uv_key='stop_hinge'),
    Cube('stop_folded', [4, 1, 8.5], [6.5, 2.5, 16], uv_key='stop_folded'),
    Cube('stop_folded_tip', [4.5, 1, 7], [6, 2, 8.5], uv_key='stop_folded_tip'),
]
LOADING_TRACK_RAISED = _rail_bed + [
    Cube('stop_hinge', [3.75, 1, 16], [6.75, 2.5, 17], uv_key='stop_hinge'),
    Cube('stop_base', [4, 1, 17], [6.5, 2, 21], uv_key='stop_base'),
    Cube('stop_face', [4, 2, 17], [6.5, 10, 18.5], uv_key='stop_face'),
    Cube('stop_step_1', [4, 2, 18.5], [6.5, 7.5, 19.5], uv_key='stop_step_1'),
    Cube('stop_step_2', [4, 2, 19.5], [6.5, 5, 20.5], uv_key='stop_step_2'),
]

# The loose end's hook, 8 px tall, standing on the model's floor at the centre (the entity's position).
HOOK = [
    Cube('socket', [7, 6, 7], [9, 8, 9]),
    Cube('shank', [7.5, 2.5, 7.5], [8.5, 6, 8.5]),
    Cube('bend', [5.5, 1.5, 7.5], [8.5, 2.5, 8.5]),
    Cube('tip', [4.5, 1.5, 7.5], [5.5, 4, 8.5]),
]

# Connecting rod, in parts the minecart renderer stacks: the base on the cart, a shaft stretched to the
# cable, and the head. The stem stands at x 12 (east of the cart's middle line); the head reaches over
# to grip the cable from above at x 8, y 4 of the head, the cable running north-south.
ROD_BASE = [
    Cube('mount', [9, 0, 6], [14, 1, 10]),
    Cube('pivot', [10.5, 1, 7], [13.5, 4, 9]),
    Cube('spring', [11, 4, 7.25], [13, 5, 8.75]),
]
ROD_SHAFT = [Cube('shaft', [11.5, 0, 7.5], [12.5, 16, 8.5])]
ROD_HEAD_CLOSED = [
    Cube('collar', [11, 0, 7], [13, 2, 9]),
    Cube('arm_closed', [11.25, 2, 7.25], [12.75, 6.75, 8.75]),
    Cube('top_bar', [7, 6, 7], [11.25, 6.75, 9]),
    Cube('grip_roller', [7, 4.75, 7], [9, 6, 9]),
    Cube('side_guide', [10.5, 3, 7], [11.25, 5, 9]),
]
ROD_HEAD_OPEN = [
    Cube('collar', [11, 0, 7], [13, 2, 9]),
    Cube('arm_open', [11.25, 2, 7.25], [12.75, 7.5, 8.75]),
    Cube('top_bar', [7, 6.75, 7], [11.25, 7.5, 9]),
    Cube('grip_roller', [7, 5.5, 7], [9, 6.75, 9]),
    Cube('side_guide', [10.5, 3, 7], [11.25, 5, 9]),
]
ROD_ITEM = ([c.moved(dx=-4) for c in ROD_BASE]
            + [Cube('shaft', [7.5, 5, 7.5], [8.5, 9, 8.5])]
            + [c.moved(dx=-4, dy=9) for c in ROD_HEAD_CLOSED])

CABLE_ITEM = ring_y('loop', 8, 8, 7, 8.5, 4.5, 1.5, key='strand')
COIL_ITEM = (ring_y('turn_1', 8, 8, 4, 6, 5.5, 2, key='strand')
             + ring_y('turn_2', 8, 8, 6, 8, 5.5, 2, key='strand')
             + ring_y('turn_3', 8, 8, 8, 10, 5.5, 2, key='strand'))

# Cobblestone Engine (M07 remodel, owner's brief section 5A): a heavy stone-fired furnace engine
# seated INSIDE the unchanged vanilla minecart, which is drawn as usual underneath it. Model x runs
# along the cart (the vanilla cart's walls stand at x -2..0 and 16..18), z across it (walls at z 0..2
# and 14..16); y 0 is the rail. The cart floor's top is at y 3 and its rim at y 11. A cobblestone
# firebox fills the back of the cart and stands well above the rim, its fire door above the rim; a riveted boiler runs forward from
# it, with a steam dome, and a tall smokestack with a collar and a flared cap rises from the front.
ENGINE = [
    Cube('firebox', [7, 3, 2.5], [15.5, 18, 13.5]),
    Cube('firebox_crown', [6.5, 18, 2], [16, 19.5, 14]),
    Cube('firebox_door', [15.5, 12, 5], [16.25, 16.5, 11]),
    Cube('firebox_latch', [16.25, 14, 7.5], [16.75, 14.5, 8.5]),
    Cube('firebox_corner_left', [14.5, 3, 2], [16, 18, 3.5], uv_key='firebox_corner'),
    Cube('firebox_corner_right', [14.5, 3, 12.5], [16, 18, 14], uv_key='firebox_corner'),
    Cube('boiler_saddle', [1, 3, 4], [7, 9, 12]),
    Cube('boiler_band_front', [1.5, 8, 3], [2.5, 18.5, 13], uv_key='boiler_band'),
    Cube('boiler_band_back', [5, 8, 3], [6, 18.5, 13], uv_key='boiler_band'),
    Cube('steam_dome', [4, 17.5, 6.5], [7, 20.5, 9.5]),
    Cube('stack_collar', [0.5, 17, 5.5], [5, 19, 10.5]),
    Cube('smokestack', [1, 19, 6], [4.5, 29, 10]),
    Cube('stack_band', [0.75, 23, 5.75], [4.75, 24, 10.25]),
    Cube('stack_cap', [0, 29, 5], [5.5, 31, 11]),
] + prism_x('boiler', 0.5, 7, 13.5, 8, 4.5)

# M07 upright ore skip, facing north (its front, with the chute's mouth and the station side, is to
# the north, z 0). Its body is one block deep and three tall; seen from the front it looks like a
# minecart turned on its side, wider than its one-block footprint: model x -6..22 (6 px into the cell
# on each side, which must be clear). Drawn by the skip's renderer in parts, one block model per part:
# the open bottom chute (block 1), the two-block ore bin (blocks 2-3, y 0..32 here), its sliding
# discharge gate (closed under the bin, or slid sideways to let ore into the chute), the hinged lid of
# the top intake (closed, or flipped up at the back) and the triangular bridle (block 4): two legs from
# lugs on the bin's sides to one apex, where the hoisting cable is made fast.
_skip_posts = lambda h: [
    Cube('post_front_left', [-6, 0, 1], [-4, h, 3], uv_key='post'),
    Cube('post_front_right', [20, 0, 1], [22, h, 3], uv_key='post'),
    Cube('post_back_left', [-6, 0, 13], [-4, h, 15], uv_key='post'),
    Cube('post_back_right', [20, 0, 13], [22, h, 15], uv_key='post'),
]
SKIP_CHUTE = _skip_posts(16) + [
    Cube('chute_cheek_left', [-4, 2, 3], [-3, 16, 13], uv_key='chute_cheek'),
    Cube('chute_cheek_right', [19, 2, 3], [20, 16, 13], uv_key='chute_cheek'),
    Cube('chute_floor', [-3, 7.5, 0], [19, 9, 17], rot=('x', -22.5, [8, 8.25, 8.5])),
    Cube('chute_lip', [-3, 1, -1.5], [19, 2.5, 1.5]),
    Cube('chute_back_bar', [-6, 0, 13], [22, 2, 15]),
]
SKIP_BIN = _skip_posts(32) + [
    Cube('wall_left', [-5.5, 2, 3], [-4, 30, 13]),
    Cube('wall_right', [20, 2, 3], [21.5, 30, 13]),
    Cube('wall_front', [-4, 2, 1], [20, 30, 2.5]),
    Cube('wall_back', [-4, 2, 13.5], [20, 30, 15]),
    Cube('sill_front', [-4, 0, 1], [20, 2, 3]),
    Cube('sill_back', [-4, 0, 13], [20, 2, 15]),
    Cube('band_low', [-6.5, 9, 0.5], [22.5, 10.5, 15.5], uv_key='band'),
    Cube('band_high', [-6.5, 21, 0.5], [22.5, 22.5, 15.5], uv_key='band'),
    Cube('rim_front', [-6.5, 30, 0.5], [22.5, 32, 2.5], uv_key='rim_x'),
    Cube('rim_back', [-6.5, 30, 13.5], [22.5, 32, 15.5], uv_key='rim_x'),
    Cube('rim_left', [-6.5, 30, 2.5], [-4, 32, 13.5], uv_key='rim_z'),
    Cube('rim_right', [20, 30, 2.5], [22.5, 32, 13.5], uv_key='rim_z'),
    Cube('lug_left', [-8, 26, 6.5], [-6, 31, 9.5], uv_key='lug'),
    Cube('lug_right', [22, 26, 6.5], [24, 31, 9.5], uv_key='lug'),
]
SKIP_GATE_CLOSED = [Cube('gate_plate', [-4, 0.5, 3], [20, 1.5, 13]), Cube('gate_handle', [20, 0, 7], [21.5, 2, 9])]
SKIP_GATE_OPEN = [Cube('gate_plate', [6, 0.5, 3], [30, 1.5, 13]), Cube('gate_handle', [30, 0, 7], [31.5, 2, 9])]
SKIP_LID_CLOSED = [Cube('lid_plate', [-4, 0, 2.5], [20, 1.5, 13.5]), Cube('lid_hinge', [-4, 0, 13.5], [20, 1.5, 15])]
SKIP_LID_OPEN = [Cube('lid_plate', [-4, 0, 13.5], [20, 11, 15], uv_key='lid_plate_open'), Cube('lid_hinge', [-4, 0, 13.5], [20, 1.5, 15])]
SKIP_BRIDLE = [
    Cube('leg_left', [-0.25, -4.1, 7.25], [1.25, 17.1, 8.75], rot=('z', -45, [0.5, 6.5, 8]), uv_key='leg'),
    Cube('leg_right', [14.75, -4.1, 7.25], [16.25, 17.1, 8.75], rot=('z', 45, [15.5, 6.5, 8]), uv_key='leg'),
    Cube('apex', [6, 13, 6.5], [10, 16, 9.5]),
]
ORE_SKIP_ITEM = [
    Cube('bin', [3, 4, 5], [13, 12, 11]),
    Cube('band', [2.5, 7, 4.5], [13.5, 8, 11.5]),
    Cube('chute', [4, 1, 5], [12, 4, 10]),
    Cube('leg_left', [4.5, 10, 7.5], [5.5, 16, 8.5], rot=('z', -22.5, [5, 13, 8])),
    Cube('leg_right', [10.5, 10, 7.5], [11.5, 16, 8.5], rot=('z', 22.5, [11, 13, 8])),
]

# M08 blast furnace, facing north (its front to z -16), after the owner's sketch and notes
# (2026-10-13): a very large industrial brick-and-iron fireplace. The furnace is 3x3 blocks and four
# high; its controller is the middle block of the second layer, so its model spans x and z -16..32 and
# y -16..32 (layers 1-3): a stone footing, the brick body (stepping in a little above a string course)
# and, across its front, an enclosed iron facade between iron corner posts, with banding, a cornice,
# two firebox doors with grilles and, in the middle, the tap housing with its tap hole. The fire (the
# doors' grilles and the tap hole glowing) is a separate model added while lit. The fourth layer is
# the brick crown with a stone coping, drawn by the top layer's middle block (y 0..16), leaving the
# back middle block to the receiving hopper (drawn by that block, 0..16): an iron charging funnel
# set into the crown, its rim a little proud of it. Out of the front, on the ground, the channel
# block (an iron trough stepping down from the tap on a trestle) and the mold block (a low iron flask
# with four corner clamps and two top ties framing a whole block's shape, so the cast standing in it
# reads as a full metal block); the metal running in the channel and the molten metal in the mold
# are separate models, and the cast block in the mold uses the game's own iron, copper and gold
# block textures.
def _walls(name, y1, y2, x1, z1, x2, z2, t):
    """Four walls t thick inside the box x1..x2, z1..z2."""
    return [
        Cube(name + '_north', [x1, y1, z1], [x2, y2, z1 + t], uv_key=name + '_ns'),
        Cube(name + '_south', [x1, y1, z2 - t], [x2, y2, z2], uv_key=name + '_ns'),
        Cube(name + '_west', [x1, y1, z1 + t], [x1 + t, y2, z2 - t], uv_key=name + '_we'),
        Cube(name + '_east', [x2 - t, y1, z1 + t], [x2, y2, z2 - t], uv_key=name + '_we'),
    ]


def _door(name, x1, x2):
    """A firebox door on the facade: frame, iron door with three grille slits."""
    return [
        Cube(name + '_jamb_left', [x1, -12, -16], [x1 + 1.5, 3, -14.5], uv_key='door_jamb'),
        Cube(name + '_jamb_right', [x2 - 1.5, -12, -16], [x2, 3, -14.5], uv_key='door_jamb'),
        Cube(name + '_lintel', [x1, 1.5, -16], [x2, 3, -14.5], uv_key='door_lintel'),
        Cube(name + '_sill', [x1, -12, -16], [x2, -10.5, -14.5], uv_key='door_lintel'),
        Cube(name + '_leaf', [x1 + 1.5, -10.5, -15.5], [x2 - 1.5, 1.5, -15], uv_key='door_leaf'),
    ] + [Cube(name + f'_grille_{i}', [x1 + 3, y, -15.6], [x2 - 3, y + 1, -15.5], uv_key='grille') for i, y in enumerate((-6, -3.5, -1))]


FURNACE = [
    Cube('footing', [-16, -16, -16], [32, -14, 32]),
    Cube('body', [-15, -14, -13], [31, 16, 31]),
    Cube('string_course', [-15.5, 15, -13.5], [31.5, 17, 31.5]),
    Cube('body_upper', [-14, 17, -12], [30, 32, 30]),
    # The iron front.
    Cube('facade', [-12, -14, -15], [28, 14, -13]),
    Cube('post_left', [-15, -14, -16], [-12, 16, -13], uv_key='post'),
    Cube('post_right', [28, -14, -16], [31, 16, -13], uv_key='post'),
    Cube('cornice', [-15, 14, -16], [31, 16, -13]),
    Cube('band_low', [-12, -13, -15.5], [28, -12, -15]),
    Cube('band_high', [-12, 11, -15.5], [28, 12, -15]),
    Cube('tap_housing', [3, -1, -15.5], [13, 9, -15]),
    Cube('tap_hole', [6, 2, -16], [10, 5, -15.5]),
] + _door('door_left', -10, 1) + _door('door_right', 15, 26)
FURNACE_FIRE = [
    Cube(f'fire_{side}_{i}', [x1 + 3, y, -15.7], [x2 - 3, y + 1, -15.6], uv_key='fire')
    for side, (x1, x2) in (('left', (-10, 1)), ('right', (15, 26))) for i, y in enumerate((-6, -3.5, -1))
] + [Cube('fire_tap', [6.5, 2.5, -16], [9.5, 4.5, -15.9])]
FURNACE_CROWN = [
    Cube('crown_front', [-14, 0, -12], [30, 12, 16]),
    Cube('crown_back_left', [-14, 0, 16], [0, 12, 30], uv_key='crown_back'),
    Cube('crown_back_right', [16, 0, 16], [30, 12, 30], uv_key='crown_back'),
    Cube('coping_front', [-15, 12, -13], [31, 14, 16]),
    Cube('coping_back_left', [-15, 12, 16], [0, 14, 31], uv_key='coping_back'),
    Cube('coping_back_right', [16, 12, 16], [31, 14, 31], uv_key='coping_back'),
]
FURNACE_HOPPER = (
    [Cube('hopper_throat', [3, 0, 3], [13, 3, 13]),
     Cube('hopper_floor', [1, 3, 1], [15, 4, 15])]
    + _walls('hopper_wall', 4, 15, 0, 0, 16, 16, 2)
    + _walls('hopper_rim', 15, 16.5, -1, -1, 17, 17, 2)
)
CHANNEL = (
    [Cube('tap_spout', [5.5, 18, 15], [10.5, 22, 17]),
     Cube('trough_upper_floor', [5, 18, 6], [11, 19, 15]),
     Cube('trough_upper_left', [5, 19, 6], [6, 21, 15], uv_key='trough_side'),
     Cube('trough_upper_right', [10, 19, 6], [11, 21, 15], uv_key='trough_side'),
     Cube('trough_lower_floor', [5, 16, -3], [11, 17, 7]),
     Cube('trough_lower_left', [5, 17, -3], [6, 19, 7], uv_key='trough_side'),
     Cube('trough_lower_right', [10, 17, -3], [11, 19, 7], uv_key='trough_side'),
     Cube('trestle_left', [4, 0, 7], [5, 18, 9], uv_key='trestle'),
     Cube('trestle_right', [11, 0, 7], [12, 18, 9], uv_key='trestle'),
     Cube('trestle_brace', [5, 8, 7.5], [11, 9, 8.5]),
     Cube('trestle_foot', [3, 0, 6], [13, 1, 10])]
)
CHANNEL_POUR = [
    Cube('molten_upper', [6, 19, 6], [10, 19.3, 16]),
    Cube('molten_lower', [6, 17, -3], [10, 17.3, 7]),
    Cube('molten_step', [6, 17.3, 6], [10, 19, 7]),
    Cube('molten_fall', [7, 15, -3], [9, 17, -2]),
]
MOLD = (
    [Cube('mold_base', [0, 0, 0], [16, 1, 16]),
     Cube('mold_cavity_floor', [1, 1, 1], [15, 1.1, 15])]
    + _walls('mold_wall', 1, 6, 0, 0, 16, 16, 1)
    + [Cube(f'mold_clamp_{i}', [x, 0, z], [x + 2, 16, z + 2], uv_key='mold_clamp')
       for i, (x, z) in enumerate(((0, 0), (14, 0), (0, 14), (14, 14)))]
    + [Cube(f'mold_tie_{i}', [x, 15, z1], [x + 2, 16, z2], uv_key='mold_tie_ns') for i, (x, z1, z2) in enumerate(((0, 2, 14), (14, 2, 14)))]
)
MOLD_MOLTEN = [Cube('mold_molten', [1, 1.1, 1], [15, 15, 15])]
# M08 playtest: eighths the mold's molten fill is drawn in (BlastFurnaceMoldBlock.FILL).
MOLD_FILL_STEPS = 8
CHARCOAL_BLOCK = [Cube('charcoal_block', [0, 0, 0], [16, 16, 16])]

# Texture groups: models that share one texture (and one UV layout).
# ---- M10 workwear and rope ----

# The Headlamp, worn and as an item (vanilla draws a head item's model like a carved pumpkin's: a
# 16-pixel model covers the head with a pixel or so to spare). A copper mining helmet with a vanilla-style
# lantern upright on its crown, under a copper cap in place of the hanging hook.
HEADLAMP = [
    Cube('helmet_shell', [1, 10, 1], [15, 16.5, 15]),
    Cube('helmet_brim', [0.5, 9.5, 0.5], [15.5, 10.5, 15.5]),
    Cube('lamp_mount', [6, 11, 0], [10, 15, 1]),
    Cube('lantern_collar', [5.5, 16.5, 5.5], [10.5, 17, 10.5]),
    Cube('lantern_body', [5, 17, 5], [11, 24, 11]),
    Cube('lantern_top', [6, 24, 6], [10, 26, 10]),
    Cube('copper_cap', [5.5, 26, 5.5], [10.5, 27, 10.5]),
    Cube('cap_knob', [7, 27, 7], [9, 28, 9]),
]
# M13 BUG-009: a thicker (3-pixel) rope, a chunkier anchor (a thick plate, a shackle loop the rope is knotted
# through) and a coil wound from three stacked, slightly offset turns.
ROPE_LINE = [Cube('rope', [6.5, 0, 6.5], [9.5, 16, 9.5])]
ROPE_COIL = ([Cube('rope', [6.5, 5, 6.5], [9.5, 16, 9.5])] + ring_y('coil_low', 8, 8, 0, 2, 5, 2)
             + ring_y('coil_mid', 8.5, 7.5, 2, 4, 4.5, 2) + ring_y('coil_top', 8, 8, 4, 6, 4, 2))
ROPE_ANCHOR = [
    Cube('anchor_plate', [4, 14, 4], [12, 16, 12]),
    Cube('anchor_loop_left', [6, 10, 7.5], [7, 14, 8.5]),
    Cube('anchor_loop_right', [9, 10, 7.5], [10, 14, 8.5]),
    Cube('anchor_loop_bottom', [6, 9, 7.5], [10, 10, 8.5]),
    Cube('anchor_knot', [6, 7, 6], [10, 9, 10]),
    Cube('rope', [6.5, 0, 6.5], [9.5, 7, 9.5]),
]
ROPE_ANCHOR_SIDE = [
    Cube('anchor_plate_side', [4, 7, 0], [12, 15, 2]),
    Cube('anchor_arm', [6.5, 11, 2], [9.5, 13, 8]),
    Cube('anchor_knot', [6, 8, 6], [10, 11, 10]),
    Cube('rope', [6.5, 0, 6.5], [9.5, 8, 9.5]),
]

# M15 the Cobblestone Engine's rear selector lever, in the engine model's frame (x runs from the smokestack
# to the firebox door at the back): a plate on the back of the cart with the pivot boss, and the handle the
# renderer swings about the pivot (16.5, 8, 12.5) across the cart: left THROUGH, upright NEUTRAL, right SERVICE.
ENGINE_LEVER_BASE = [
    Cube('lever_plate', [16, 5.5, 10.5], [17, 10.5, 14.5]),
    Cube('lever_boss', [16.75, 7, 11.5], [17.75, 9, 13.5]),
]
ENGINE_LEVER_HANDLE = [
    Cube('lever_handle', [17, 8, 12], [18, 18, 13]),
    Cube('lever_grip', [16.75, 17, 11.75], [18.25, 20, 13.25]),
]

GROUPS = {
    'block/cable_anchor': {'cable_anchor': ANCHOR, 'cable_anchor_guide': ANCHOR_GUIDE, 'cable_anchor_wheel_front': ANCHOR_WHEEL_FRONT,
                           'cable_anchor_wheel_back': ANCHOR_WHEEL_BACK},
    'block/cable_pulley': {'cable_anchor_pulley': PULLEY},
    'block/cable_drum': {'cable_drum': DRUM, 'cable_drum_frame': DRUM_FRAME, 'cable_drum_frame_powered': DRUM_FRAME_POWERED,
                         'cable_drum_frame_powered_hanging': DRUM_FRAME_POWERED_HANGING,
                         'cable_drum_spool': DRUM_SPOOL,
                         'cable_drum_spool_wound': DRUM_SPOOL_WOUND, 'cable_drum_crank': DRUM_CRANK},
    'block/cable_winch': {'cable_drum_lever': DRUM_LEVER},
    'block/loading_track': {'loading_track': LOADING_TRACK, 'loading_track_raised': LOADING_TRACK_RAISED},
    'block/cable_hook': {'cable_hook': HOOK},
    'block/connecting_rod': {
        'connecting_rod_base': ROD_BASE,
        'connecting_rod_shaft': ROD_SHAFT,
        'connecting_rod_head_open': ROD_HEAD_OPEN,
        'connecting_rod_head_closed': ROD_HEAD_CLOSED,
        'copper_connecting_rod': ROD_ITEM,
    },
    'item/cable': {'cable': CABLE_ITEM},
    'item/cable_coil': {'cable_coil': COIL_ITEM},
    'block/cobblestone_engine': {'cobblestone_engine': ENGINE},
    'block/cobblestone_engine_lever': {'cobblestone_engine_lever_base': ENGINE_LEVER_BASE, 'cobblestone_engine_lever_handle': ENGINE_LEVER_HANDLE},
    'block/ore_skip': {'ore_skip_chute': SKIP_CHUTE, 'ore_skip_bin': SKIP_BIN, 'ore_skip_gate_closed': SKIP_GATE_CLOSED,
                       'ore_skip_gate_open': SKIP_GATE_OPEN, 'ore_skip_lid_closed': SKIP_LID_CLOSED, 'ore_skip_lid_open': SKIP_LID_OPEN,
                       'ore_skip_bridle': SKIP_BRIDLE, 'ore_skip': ORE_SKIP_ITEM},
    'block/blast_furnace': {'blast_furnace': FURNACE, 'blast_furnace_fire': FURNACE_FIRE,
                                           'blast_furnace_crown': FURNACE_CROWN,
                                           'blast_furnace_hopper': FURNACE_HOPPER,
                                           'blast_furnace_channel': CHANNEL,
                                           'blast_furnace_channel_pour': CHANNEL_POUR,
                                           'blast_furnace_mold': MOLD,
                                           'blast_furnace_mold_molten': MOLD_MOLTEN},
    'item/headlamp': {'headlamp': HEADLAMP},
    'block/spelunker_rope': {'spelunker_rope': ROPE_LINE, 'spelunker_rope_coil': ROPE_COIL, 'spelunker_rope_anchor': ROPE_ANCHOR,
                             'spelunker_rope_anchor_side': ROPE_ANCHOR_SIDE},
    'block/charcoal_block': {'charcoal_block': CHARCOAL_BLOCK},
}
# Where each model's JSON goes (block models are used by blockstates, block items and renderers).
MODEL_PATHS = {name: ('item/' if name in ('copper_connecting_rod', 'cable', 'cable_coil', 'ore_skip', 'headlamp') else 'block/') + name
               for models in GROUPS.values() for name in models}
# Models also shown as items (they get the standard block display transforms).
ITEM_DISPLAY = {'headlamp', 'cable_anchor', 'cable_drum', 'copper_connecting_rod', 'cable', 'cable_coil', 'loading_track', 'ore_skip', 'charcoal_block'}

# ---- UV layout ----

PALETTE = [(214, 140, 84), (122, 168, 214), (150, 200, 120), (220, 196, 98), (190, 132, 200), (110, 196, 186), (226, 122, 132),
           (168, 160, 150), (240, 170, 120), (130, 140, 220)]
# Template colours by UV key for the M08 furnace's template, so that it reads as materials:
# brick, firebrick, stone, iron, fire and molten metal. Keys not listed take the palette.
BRICK, STONE, IRON, DARK_IRON, DARK, FIRE, MOLTEN = (156, 82, 62), (128, 128, 124), (112, 114, 120), (66, 68, 74), (34, 28, 26), (255, 170, 40), (255, 112, 24)
# M13 BUG-009: the Spelunker's Rope template reads as hemp rope on copper fittings, each coil turn a shade apart.
HEMP, COPPER = (194, 158, 104), (190, 110, 70)
ROPE_COLOURS = {'rope': HEMP, 'coil_low': (176, 140, 90), 'coil_mid': (204, 170, 116), 'coil_top': (186, 150, 98),
                'anchor_knot': (168, 132, 84), 'anchor_plate': COPPER, 'anchor_plate_side': COPPER, 'anchor_arm': COPPER,
                'anchor_loop_left': COPPER, 'anchor_loop_right': COPPER, 'anchor_loop_bottom': COPPER}
MATERIAL_COLOURS = {
    'footing': STONE, 'body': BRICK, 'string_course': STONE, 'body_upper': BRICK,
    'facade': IRON, 'post': DARK_IRON, 'cornice': DARK_IRON, 'band_low': DARK_IRON, 'band_high': DARK_IRON, 'tap_housing': DARK_IRON,
    'tap_hole': DARK, 'door_jamb': DARK_IRON, 'door_lintel': DARK_IRON, 'door_leaf': IRON, 'grille': DARK,
    'fire': FIRE, 'fire_tap': MOLTEN,
    'crown_front': BRICK, 'crown_back': BRICK, 'coping_front': STONE, 'coping_back': STONE,
    'hopper_throat': DARK, 'hopper_floor': DARK, 'hopper_wall_ns': IRON, 'hopper_wall_we': IRON, 'hopper_rim_ns': DARK_IRON, 'hopper_rim_we': DARK_IRON,
    'tap_spout': DARK_IRON, 'trough_upper_floor': IRON, 'trough_lower_floor': IRON, 'trough_side': IRON, 'trestle': DARK_IRON,
    'trestle_brace': DARK_IRON, 'trestle_foot': STONE,
    'molten_upper': MOLTEN, 'molten_lower': MOLTEN, 'molten_step': MOLTEN, 'molten_fall': MOLTEN, 'mold_molten': MOLTEN,
    'mold_base': DARK_IRON, 'mold_cavity_floor': DARK, 'mold_wall_ns': IRON, 'mold_wall_we': IRON, 'mold_clamp': DARK_IRON, 'mold_tie_ns': DARK_IRON,
}
SHADE = {'up': 1.0, 'north': 0.86, 'south': 0.86, 'east': 0.74, 'west': 0.74, 'down': 0.6}


def net_layout(size):
    """Box-UV net: up and down on the top row, east, north, west, south below (pixel rects)."""
    dx, dy, dz = (max(1, math.ceil(v - 1e-6)) for v in size)
    rects = {
        'up': (dz, 0, dx, dz), 'down': (dz + dx, 0, dx, dz),
        'east': (0, dz, dz, dy), 'north': (dz, dz, dx, dy), 'west': (dz + dx, dz, dz, dy), 'south': (2 * dz + dx, dz, dx, dy),
    }
    return 2 * (dx + dz), dz + dy, rects


def face_size(cube, face):
    dx, dy, dz = cube.size()
    return {'up': (dx, dz), 'down': (dx, dz), 'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy)}[face]


def pack(group_cubes):
    """Shelf-packs one net per uv key into the smallest square power-of-two texture."""
    keys, first = [], {}
    for cube in group_cubes:
        if cube.uv_key not in first:
            first[cube.uv_key] = cube
            keys.append(cube.uv_key)
    nets = {k: net_layout(first[k].size()) for k in keys}
    order = sorted(keys, key=lambda k: -nets[k][1])
    size = 16
    while True:
        x = y = shelf = 0
        placed = {}
        ok = True
        for k in order:
            w, h, _ = nets[k]
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if w > size or y + h > size:
                ok = False
                break
            placed[k] = (x, y)
            x += w
            shelf = max(shelf, h)
        if ok:
            break
        size *= 2
    regions = {}
    for k in keys:
        ox, oy = placed[k]
        regions[k] = {f: (ox + r[0], oy + r[1], r[2], r[3]) for f, r in nets[k][2].items()}
    return size, regions, keys


def face_uv(cube, face, regions):
    """The face's UV rect in texture pixels: the top-left of its region, at one texel per model pixel."""
    rx, ry, rw, rh = regions[cube.uv_key][face]
    w, h = face_size(cube, face)
    return [rx, ry, rx + min(w, rw), ry + min(h, rh)]


def template_png(size, regions, keys, colours=None):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for i, k in enumerate(keys):
        base = (colours or {}).get(k, PALETTE[i % len(PALETTE)])
        for face, (x, y, w, h) in regions[k].items():
            c = tuple(int(v * SHADE[face]) for v in base) + (255,)
            edge = tuple(int(v * SHADE[face] * 0.62) for v in base) + (255,)
            draw.rectangle([x, y, x + w - 1, y + h - 1], fill=c)
            if w >= 3 and h >= 3:
                draw.rectangle([x, y, x + w - 1, y + h - 1], outline=edge)
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


# ---- Output ----

def element_json(cube, regions, size, texture='#0'):
    scale = 16.0 / size
    e = {'name': cube.name, 'from': cube.frm, 'to': cube.to}
    if cube.rot:
        axis, angle, origin = cube.rot
        e['rotation'] = {'angle': angle, 'axis': axis, 'origin': origin}
    e['faces'] = {f: {'uv': [round(v * scale, 4) for v in face_uv(cube, f, regions)], 'texture': texture} for f in FACES}
    return e


def model_json(name, cubes, texture_id, regions, size, item):
    model = {
        'credit': f'Generated by tools/models/generate_models.py; editable source art/blockbench/{name}.bbmodel',
        'parent': 'minecraft:block/block',
        'texture_size': [size, size],
        'textures': {'0': texture_id, 'particle': texture_id},
        'elements': [element_json(c, regions, size) for c in cubes],
    }
    if not item:
        del model['parent']
    if name == 'headlamp':
        # The lantern stands above the helmet: show the whole of it in slots and on the ground.
        model['display'] = {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -3, 0], 'scale': [0.48, 0.48, 0.48]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.22, 0.22, 0.22]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2.5, 0], 'scale': [0.42, 0.42, 0.42]},
        }
    return model


def bbmodel(name, cubes, texture_path, texture_rel, png, regions, size, export_path):
    elements, outliner = [], []
    for i, c in enumerate(cubes):
        uid = str(uuid.uuid5(uuid.NAMESPACE_URL, f'simple_block/{name}/{i}/{c.name}'))
        rotation = [0, 0, 0]
        origin = [8, 8, 8]
        if c.rot:
            axis, angle, origin = c.rot
            rotation['xyz'.index(axis)] = angle
        elements.append({
            'name': c.name, 'box_uv': False, 'rescale': False, 'locked': False, 'light_emission': 0, 'render_order': 'default',
            'allow_mirror_modeling': True, 'from': c.frm, 'to': c.to, 'autouv': 0, 'color': i % 8, 'origin': origin, 'rotation': rotation,
            'faces': {f: {'uv': face_uv(c, f, regions), 'texture': 0} for f in FACES},
            'type': 'cube', 'uuid': uid,
        })
        outliner.append(uid)
    texture_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, f'simple_block/{name}/texture'))
    folder, tex_name = texture_path.split('/')
    return {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': name,
        'model_identifier': '',
        'parent': '',
        'ambientocclusion': True,
        'front_gui_light': False,
        'visible_box': [1, 1, 0],
        'variable_placeholders': '',
        'variable_placeholder_buttons': [],
        'export_path': export_path,
        'unhandled_root_fields': {},
        'resolution': {'width': size, 'height': size},
        'elements': elements,
        'outliner': outliner,
        'textures': [{
            'path': '', 'name': tex_name + '.png', 'folder': folder, 'namespace': NAMESPACE, 'id': '0', 'group': '',
            'width': size, 'height': size, 'uv_width': size, 'uv_height': size, 'particle': True, 'use_as_default': False,
            'layers_enabled': False, 'sync_to_project': '', 'render_mode': 'default', 'render_sides': 'auto', 'frame_time': 1,
            'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': False, 'saved': True,
            'uuid': texture_uuid, 'relative_path': texture_rel,
            'source': 'data:image/png;base64,' + base64.b64encode(png).decode('ascii'),
        }],
    }


def main():
    for texture, models in GROUPS.items():
        cubes = [c for cs in models.values() for c in cs]
        size, regions, keys = pack(cubes)
        png = template_png(size, regions, keys, MATERIAL_COLOURS if texture == 'block/blast_furnace'
                           else ROPE_COLOURS if texture == 'block/spelunker_rope' else None)
        tex_file = os.path.join(ASSETS, 'textures', texture + '.png')
        write(tex_file, png, binary=True)
        texture_id = f'{NAMESPACE}:{texture}'
        for name, model_cubes in models.items():
            path = MODEL_PATHS[name]
            item = name in ITEM_DISPLAY
            model_file = os.path.join(ASSETS, 'models', path + '.json')
            write_json(model_file, model_json(name, model_cubes, texture_id, regions, size, item))
            bb_file = os.path.join(ART, name + '.bbmodel')
            write_json(bb_file, bbmodel(name, model_cubes, texture, os.path.relpath(tex_file, ART), png, regions, size,
                                        os.path.relpath(model_file, ART)))

    # The cable strip drawn between supports: each of the cable's four sides maps the whole texture,
    # u across the side, v along one block of cable length.
    img = Image.new('RGBA', (16, 16), (150, 150, 156, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 15, 15], outline=(96, 96, 102, 255))
    draw.line([0, 0, 15, 0], fill=(196, 196, 202, 255))
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    write(os.path.join(ASSETS, 'textures', 'entity', 'cable.png'), buf.getvalue(), binary=True)

    m = lambda path, **kw: dict({'model': f'{NAMESPACE}:{path}'}, **{k: v for k, v in kw.items() if v})
    # Anchors: only the axis of the facing matters for the plain and guide models (arm west of a
    # north-south cable, north of an east-west one); a return wheel sits on the side the facing points to.
    anchor = {}
    for facing in ('north', 'south', 'east', 'west'):
        y = 90 if facing in ('east', 'west') else 0
        anchor[f'facing={facing},return=none'] = m('block/cable_anchor', y=y)
        anchor[f'facing={facing},return=guide'] = m('block/cable_anchor_guide', y=y)
        anchor[f'facing={facing},return=wheel'] = m('block/cable_anchor_wheel_' + ('front' if facing in ('north', 'east') else 'back'), y=y)
        anchor[f'facing={facing},return=pulley'] = m('block/cable_anchor_pulley', y={'north': 0, 'east': 90, 'south': 180, 'west': 270}[facing])
    write_json(os.path.join(ASSETS, 'blockstates', 'cable_anchor.json'), {'variants': anchor})
    # Drums: the block model is the frame; the spool and crank are drawn turning by the renderer. A drum
    # hung under a beam is the same frame upside down.
    rotations = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    drum_variants = {}
    for facing, y in rotations.items():
        for wound in ('false', 'true'):
            for hanging in ('false', 'true'):
                for powered in ('false', 'true'):
                    frame = 'block/cable_drum_frame' + ('_powered' if powered == 'true' else '') + ('_hanging' if powered == 'true' and hanging == 'true' else '')
                    drum_variants[f'facing={facing},hanging={hanging},powered={powered},wound={wound}'] = m(frame, x=180 if hanging == 'true' else 0, y=y)
    write_json(os.path.join(ASSETS, 'blockstates', 'cable_drum.json'), {'variants': drum_variants})
    write_json(os.path.join(ASSETS, 'blockstates', 'cable_drum_part.json'), {'variants': {
        f'part={part}': m(f'block/cable_drum_{part}') for part in ('spool', 'spool_wound', 'crank', 'lever')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'loading_track.json'), {'variants': {
        f'facing={facing},raised={raised}': m('block/loading_track' + ('_raised' if raised == 'true' else ''), y=y)
        for facing, y in rotations.items() for raised in ('false', 'true')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'cable_hook.json'), {'variants': {'': m('block/cable_hook')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'connecting_rod_part.json'), {'variants': {
        f'part={part}': m(f'block/connecting_rod_{part}') for part in ('base', 'shaft', 'head_open', 'head_closed')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'cobblestone_engine_model.json'), {'variants': {'': m('block/cobblestone_engine')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'ore_skip_part.json'), {'variants': {
        f'part={part}': m(f'block/ore_skip_{part}') for part in ('chute', 'bin', 'gate_closed', 'gate_open', 'lid_closed', 'lid_open', 'bridle')}})
    # M08 blast furnace: the controller draws the hearth and stack, plus the fire and the pour while lit or
    # pouring; of the other 35 blocks only the middle of the top layer has a model (the hopper), the rest
    # an empty one that only gives the breaking particles.
    write_json(os.path.join(ASSETS, 'blockstates', 'blast_furnace.json'), {'multipart': [
        part for facing, y in rotations.items() for part in (
            {'when': {'facing': facing}, 'apply': m('block/blast_furnace', y=y)},
            {'when': {'facing': facing, 'lit': 'true'}, 'apply': m('block/blast_furnace_fire', y=y)})]})
    write_json(os.path.join(ASSETS, 'models', 'block', 'blast_furnace_part.json'), {
        'textures': {'particle': f'{NAMESPACE}:block/blast_furnace'}, 'elements': []})
    # The crown is drawn by the top layer's middle block; the hopper by its back middle block, which
    # depends on the facing (dx and dz are world offsets from the controller, 1 in the middle).
    back = {'north': ('1', '2'), 'south': ('1', '0'), 'east': ('0', '1'), 'west': ('2', '1')}
    write_json(os.path.join(ASSETS, 'blockstates', 'blast_furnace_part.json'), {'multipart': [
        {'apply': m('block/blast_furnace_part')}]
        + [{'when': {'dx': '1', 'layer': '3', 'dz': '1', 'facing': facing}, 'apply': m('block/blast_furnace_crown', y=y)}
           for facing, y in rotations.items()]
        + [{'when': {'dx': back[facing][0], 'layer': '3', 'dz': back[facing][1], 'facing': facing},
            'apply': m('block/blast_furnace_hopper')} for facing in rotations]})
    write_json(os.path.join(ASSETS, 'blockstates', 'blast_furnace_channel.json'), {'multipart': [
        part for facing, y in rotations.items() for part in (
            {'when': {'facing': facing}, 'apply': m('block/blast_furnace_channel', y=y)},
            {'when': {'facing': facing, 'pouring': 'true'}, 'apply': m('block/blast_furnace_channel_pour', y=y)})]})
    # M08 playtest: the mold fills eighth by eighth with the cast's progress (fill 1..8: molten metal from
    # the cavity's floor up); above the molten metal stands the block it last cast (content), if any.
    write_json(os.path.join(ASSETS, 'blockstates', 'blast_furnace_mold.json'), {'multipart': [
        {'apply': m('block/blast_furnace_mold')}]
        + [{'when': {'fill': str(k)}, 'apply': m(f'block/blast_furnace_mold_molten_{k}')} for k in range(1, MOLD_FILL_STEPS + 1)]
        + [{'when': {'content': metal, 'fill': str(k)}, 'apply': m(f'block/blast_furnace_mold_{metal}' + (f'_{k}' if k else ''))}
           for metal in ('iron', 'copper', 'gold') for k in range(MOLD_FILL_STEPS)]})
    cavity_floor, cavity_top = 1.1, 15.0
    level = lambda k: round(cavity_floor + (cavity_top - cavity_floor) * k / MOLD_FILL_STEPS, 4)
    molten = read_json(os.path.join(ASSETS, 'models', 'block', 'blast_furnace_mold_molten.json'))
    for k in range(1, MOLD_FILL_STEPS + 1):
        model = json.loads(json.dumps(molten))
        for element in model['elements']:
            element['to'][1] = level(k)
            for face, spec in element['faces'].items():
                if face not in ('up', 'down'):
                    u1, v1, u2, v2 = spec['uv']
                    spec['uv'] = [u1, round(v2 - (v2 - v1) * k / MOLD_FILL_STEPS, 4), u2, v2]
        write_json(os.path.join(ASSETS, 'models', 'block', f'blast_furnace_mold_molten_{k}.json'), model)
    # The cast block in the mold: the cavity filled with the game's own block texture of that metal (k: the
    # part of it above k eighths of molten metal).
    for metal, texture in (('iron', 'iron_block'), ('copper', 'copper_block'), ('gold', 'gold_block')):
        for k in range(MOLD_FILL_STEPS):
            bottom = level(k)
            write_json(os.path.join(ASSETS, 'models', 'block', f'blast_furnace_mold_{metal}' + (f'_{k}' if k else '') + '.json'), {
                'textures': {'cast': f'minecraft:block/{texture}', 'particle': f'minecraft:block/{texture}'},
                'elements': [{'name': 'cast', 'from': [1, bottom, 1], 'to': [15, 15, 15], 'faces': {
                    face: {'uv': [1, 1, 15, 15] if face in ('up', 'down') else [1, 1, 15, round(1 + (15 - bottom), 4)], 'texture': '#cast'}
                    for face in FACES}}]})
    write_json(os.path.join(ASSETS, 'models', 'item', 'blast_furnace.json'), {
        'parent': f'{NAMESPACE}:block/blast_furnace',
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [0.2, 0.2, 0.2]},
                    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.12, 0.12, 0.12]},
                    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.25, 0.25, 0.25]},
                    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.18, 0.18, 0.18]},
                    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.2, 0.2, 0.2]},
                    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.2, 0.2, 0.2]}}})
    write_json(os.path.join(ASSETS, 'blockstates', 'charcoal_block.json'), {'variants': {'': m('block/charcoal_block')}})
    # M10: the rope anchor hangs under a block (support=up) or on a side (the side model faces north, its support).
    write_json(os.path.join(ASSETS, 'blockstates', 'spelunker_rope_anchor.json'), {'variants': dict(
        [('support=up', m('block/spelunker_rope_anchor'))]
        + [(f'support={side}', m('block/spelunker_rope_anchor_side', y=y)) for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))])})
    write_json(os.path.join(ASSETS, 'blockstates', 'spelunker_rope.json'), {'variants': {'': m('block/spelunker_rope')}})
    write_json(os.path.join(ASSETS, 'blockstates', 'spelunker_rope_coil.json'), {'variants': {'': m('block/spelunker_rope_coil')}})
    for item, path in (('cable', 'item/cable'), ('cable_coil', 'item/cable_coil'), ('copper_connecting_rod', 'item/copper_connecting_rod'),
                       ('cable_anchor', 'block/cable_anchor'), ('cable_drum', 'block/cable_drum'), ('loading_track', 'block/loading_track'), ('ore_skip', 'item/ore_skip'),
                       ('blast_furnace', 'item/blast_furnace'), ('charcoal_block', 'block/charcoal_block'), ('headlamp', 'item/headlamp')):
        write_json(os.path.join(ASSETS, 'items', item + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NAMESPACE}:{path}'}})

    print('written:', *written, sep='\n  ')
    if skipped:
        print('kept (exists; --force overwrites):', *skipped, sep='\n  ')


if __name__ == '__main__':
    main()
