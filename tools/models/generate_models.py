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

# Texture groups: models that share one texture (and one UV layout).
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
    'block/ore_skip': {'ore_skip_chute': SKIP_CHUTE, 'ore_skip_bin': SKIP_BIN, 'ore_skip_gate_closed': SKIP_GATE_CLOSED,
                       'ore_skip_gate_open': SKIP_GATE_OPEN, 'ore_skip_lid_closed': SKIP_LID_CLOSED, 'ore_skip_lid_open': SKIP_LID_OPEN,
                       'ore_skip_bridle': SKIP_BRIDLE, 'ore_skip': ORE_SKIP_ITEM},
}
# Where each model's JSON goes (block models are used by blockstates, block items and renderers).
MODEL_PATHS = {name: ('item/' if name in ('copper_connecting_rod', 'cable', 'cable_coil', 'ore_skip') else 'block/') + name
               for models in GROUPS.values() for name in models}
# Models also shown as items (they get the standard block display transforms).
ITEM_DISPLAY = {'cable_anchor', 'cable_drum', 'copper_connecting_rod', 'cable', 'cable_coil', 'loading_track', 'ore_skip'}

# ---- UV layout ----

PALETTE = [(214, 140, 84), (122, 168, 214), (150, 200, 120), (220, 196, 98), (190, 132, 200), (110, 196, 186), (226, 122, 132),
           (168, 160, 150), (240, 170, 120), (130, 140, 220)]
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


def template_png(size, regions, keys):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for i, k in enumerate(keys):
        base = PALETTE[i % len(PALETTE)]
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
        png = template_png(size, regions, keys)
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
    for item, path in (('cable', 'item/cable'), ('cable_coil', 'item/cable_coil'), ('copper_connecting_rod', 'item/copper_connecting_rod'),
                       ('cable_anchor', 'block/cable_anchor'), ('cable_drum', 'block/cable_drum'), ('loading_track', 'block/loading_track'), ('ore_skip', 'item/ore_skip')):
        write_json(os.path.join(ASSETS, 'items', item + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NAMESPACE}:{path}'}})

    print('written:', *written, sep='\n  ')
    if skipped:
        print('kept (exists; --force overwrites):', *skipped, sep='\n  ')


if __name__ == '__main__':
    main()
