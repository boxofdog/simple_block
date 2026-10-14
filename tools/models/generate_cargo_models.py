#!/usr/bin/env python3
"""Scaffolds the M11 freight-cart loads: models, Blockbench sources and placeholder textures.

A load has a form (its shape: bars, loose gems, stacked blocks, rough rocks, a heap, or a covered load)
and a material (its texture). Each form has four rising fill bands. This writes:

  art/blockbench/cargo/<form>_<band>.bbmodel                editable source of each form and band
  assets/simple_block/models/block/cargo/form/<form>_<band>.json  the same geometry, texture variable #cargo
  assets/simple_block/models/block/cargo/<look>_<band>.json  one per material and band: the form's model
                                                             with that material's texture
  assets/simple_block/blockstates/cargo_fill.json            look x band -> model (train/CargoFillBlock)
  assets/simple_block/textures/block/cargo/<look>.png        16 x 16 placeholder per material

Every material texture has the same layout, so one painting guide fits all of them:
  A  top-left  (0-7, 0-7)    the top of the load's body (stretched over it)
  B  top-right (8-15, 0-7)   the sides of the body, from the top row down as far as the body is high
  C  bottom-left             the top of each piece (bar, gem, block, rock, lump), from its top-left corner
  D  bottom-right            the sides and underside of each piece, likewise
Placeholder colours are sampled from vanilla's own textures of the item (dark, middle and light tones).

Model space is the cart's inside, as the renderer places it: x 0.25-15.75 along the cart, z 2.25-13.75
across it, y 0 on the floor; nothing reaches above y 7.5 (the cart's rim is at 8) or outside the walls,
so open gates never cut through the load. It is a one-off scaffold: it never overwrites an existing
file unless run with --force.

usage: python3 tools/models/generate_cargo_models.py [--force]
"""
import io
import json
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import generate_models as gm  # noqa: E402  (Cube, element_json, bbmodel)

ROOT = gm.ROOT
ASSETS = gm.ASSETS
ART = os.path.join(gm.ART, 'cargo')
FORCE = '--force' in sys.argv
written, skipped = [], []

X0, X1, Z0, Z1 = 0.25, 15.75, 2.25, 13.75
# Height of the load's top in each band (1-64, 65-128, 129-192, 193-256 items).
HEIGHT = {1: 1.75, 2: 3.5, 3: 5.25, 4: 7.0}

# Texture regions (see the module docstring): the body uses A and B, the pieces C and D.
REGIONS = {
    'body': {'up': (0, 0, 8, 8), 'down': (8, 0, 8, 8), 'north': (8, 0, 8, 8), 'south': (8, 0, 8, 8), 'east': (8, 0, 8, 8), 'west': (8, 0, 8, 8)},
    'piece': {'up': (0, 8, 8, 8), 'down': (8, 8, 8, 8), 'north': (8, 8, 8, 8), 'south': (8, 8, 8, 8), 'east': (8, 8, 8, 8), 'west': (8, 8, 8, 8)},
}

# Looks by form, as train/CargoLooks.Look: (look, palette key or tuple).
FORMS = {
    'ingots': ['iron_ingot', 'copper_ingot', 'gold_ingot', 'netherite_ingot'],
    'gems': ['diamond', 'emerald', 'lapis_lazuli'],
    'blocks': ['iron_block', 'copper_block', 'gold_block', 'diamond_block', 'netherite_block', 'stone', 'deepslate'],
    'rocks': ['iron_ore', 'copper_ore', 'gold_ore', 'deepslate_iron_ore', 'deepslate_copper_ore', 'deepslate_gold_ore', 'raw_iron', 'raw_copper',
              'raw_gold', 'coal'],
    'heap': ['sand', 'gravel', 'cobblestone', 'dirt', 'cobbled_deepslate', 'redstone'],
    'covered': ['covered'],
}

# Dark, middle and light tones sampled from vanilla's textures of each item (5-30 %, 30-70 %, 70-97 % by brightness).
PALETTE = {
    'iron_ingot': ((89, 89, 89), (156, 156, 156), (225, 225, 225)),
    'copper_ingot': ((142, 65, 39), (174, 84, 51), (234, 129, 96)),
    'gold_ingot': ((166, 89, 13), (241, 200, 55), (253, 247, 124)),
    'netherite_ingot': ((38, 30, 31), (62, 55, 56), (88, 85, 88)),
    'diamond': ((20, 109, 109), (46, 204, 191), (196, 253, 241)),
    'emerald': ((0, 69, 0), (9, 178, 61), (129, 247, 174)),
    'lapis_lazuli': ((11, 48, 117), (35, 81, 170), (76, 117, 215)),
    'iron_block': ((150, 150, 150), (210, 210, 210), (240, 240, 240)),
    'copper_block': ((166, 89, 64), (194, 109, 79), (218, 125, 97)),
    'gold_block': ((200, 140, 30), (251, 213, 55), (255, 240, 120)),
    'diamond_block': ((40, 170, 172), (93, 243, 230), (170, 253, 245)),
    'netherite_block': ((40, 33, 34), (69, 65, 67), (92, 88, 92)),
    'stone': ((100, 100, 100), (126, 126, 126), (145, 145, 145)),
    'deepslate': ((50, 50, 56), (78, 78, 79), (105, 105, 105)),
    'raw_iron': ((76, 67, 51), (151, 125, 100), (238, 206, 184)),
    'raw_copper': ((90, 63, 46), (127, 90, 60), (207, 129, 94)),
    'raw_gold': ((150, 88, 20), (228, 158, 22), (251, 232, 97)),
    'coal': ((25, 22, 28), (36, 32, 35), (60, 60, 60)),
    'sand': ((200, 182, 136), (218, 207, 163), (232, 224, 184)),
    'gravel': ((100, 95, 94), (131, 128, 127), (160, 157, 156)),
    'cobblestone': ((90, 90, 90), (125, 124, 124), (162, 162, 162)),
    'dirt': ((100, 70, 47), (130, 93, 65), (165, 121, 86)),
    'cobbled_deepslate': ((50, 50, 55), (72, 72, 77), (102, 102, 102)),
    'redstone': ((60, 3, 1), (120, 8, 0), (200, 20, 5)),
    'covered': ((58, 66, 40), (84, 94, 58), (110, 120, 78)),
}
# Ores: rock of the host stone flecked with the metal's raw colours.
ORES = {
    'iron_ore': ('stone', 'raw_iron'), 'copper_ore': ('stone', 'raw_copper'), 'gold_ore': ('stone', 'raw_gold'),
    'deepslate_iron_ore': ('deepslate', 'raw_iron'), 'deepslate_copper_ore': ('deepslate', 'raw_copper'),
    'deepslate_gold_ore': ('deepslate', 'raw_gold'),
}


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

def box(name, x1, y1, z1, x2, y2, z2, key='piece', rot=None):
    return gm.Cube(name, [round(x1, 3), round(y1, 3), round(z1, 3)], [round(x2, 3), round(y2, 3), round(z2, 3)], rot, key)


def body(name, inset_x, inset_z, top, bottom=0.0):
    return box(name, X0 + inset_x, bottom, Z0 + inset_z, X1 - inset_x, top, Z1 - inset_z, 'body')


def scatter(rng, count, y_at, size_range, margin, area, rot_axes, name):
    """Loose pieces dropped over an area (x1, z1, x2, z2), each sitting on y_at(x, z), kept inside the walls."""
    x1, z1, x2, z2 = area
    pieces = []
    for i in range(count):
        s = rng.uniform(*size_range)
        h = s * rng.uniform(0.7, 1.0)
        # Turned up to 45 degrees, a piece reaches out to half its diagonal.
        reach = max(margin, s * 0.72)
        cx = rng.uniform(max(x1, X0 + reach), min(x2, X1 - reach))
        cz = rng.uniform(max(z1, Z0 + reach), min(z2, Z1 - reach))
        y = y_at(cx, cz)
        top = min(y + h, 7.5)
        axis = rng.choice(rot_axes)
        angle = rng.choice((-45.0, -22.5, 22.5, 45.0)) if axis == 'y' else rng.choice((-22.5, 22.5))
        rot = (axis, angle, [round(cx, 3), round(y, 3), round(cz, 3)])
        pieces.append(box(f'{name}{i}', cx - s / 2, y, cz - s / 2, cx + s / 2, top, cz + s / 2, 'piece', rot))
    return pieces


def ingots(band):
    cubes = []
    top = 0.0
    if band >= 2:
        top = HEIGHT[band] - 1.5
        cubes.append(body('stack', 0, 0, top))
    if band % 2 == 1:
        # A layer of bars along the cart (the lowest band: only its middle rows).
        rows = range(1, 4) if band == 1 else range(5)
        for j in rows:
            for i in range(4):
                x, z = 0.4 + i * 3.85, 2.5 + j * 2.25
                cubes.append(box(f'bar{i}_{j}', x, top, z, x + 3.6, top + 1.5, z + 1.9))
    else:
        # A layer of bars across it.
        for j in range(3):
            for i in range(8):
                x, z = 0.4 + i * 1.9, 2.4 + j * 3.75
                cubes.append(box(f'bar{i}_{j}', x, top, z, x + 1.8, top + 1.5, z + 3.6))
    return cubes


def mound(band, steps, base_fraction=0.55):
    """A mound's body: tiers each inset further, rising to just under the band's height."""
    cubes, tops = [], []
    full = HEIGHT[band]
    for k in range(steps):
        top = full * base_fraction + (full * (1 - base_fraction) - 1.0) * k / max(1, steps - 1) if steps > 1 else full * base_fraction
        cubes.append(body(f'tier{k}', 1.6 * k, 1.1 * k, round(top, 3)))
        tops.append((1.6 * k, 1.1 * k, top))
    return cubes, tops


def surface(tops):
    """The height of a mound's top at (x, z): the highest tier covering it."""
    def at(x, z):
        h = 0.0
        for ix, iz, top in tops:
            if X0 + ix <= x <= X1 - ix and Z0 + iz <= z <= Z1 - iz:
                h = max(h, top)
        return h
    return at


def loose(band, seed, size_range, counts, rot_axes, name):
    rng = random.Random(seed * 10 + band)
    if band == 1:
        return scatter(rng, counts[0], lambda x, z: 0.0, size_range, 1.6, (X0, Z0, X1, Z1), rot_axes, name)
    cubes, tops = mound(band, band)
    at = surface(tops)
    cubes += scatter(rng, counts[band - 1], at, size_range, 1.6, (X0, Z0, X1, Z1), rot_axes, name)
    return cubes


def gems(band):
    return loose(band, 1, (1.5, 2.1), (12, 12, 14, 16), ('y',), 'gem')


def rocks(band):
    return loose(band, 2, (2.2, 3.3), (8, 9, 10, 12), ('y', 'x', 'z'), 'rock')


def blocks(band):
    cells = {1: [(i, 1, 0) for i in range(4)],
             2: [(i, j, 0) for j in range(3) for i in range(4)],
             3: [(i, j, 0) for j in range(3) for i in range(4)] + [(i, j, 1) for j in range(3) for i in (1, 2)],
             4: [(i, j, k) for k in range(2) for j in range(3) for i in range(4)]}[band]
    return [box(f'block{i}_{j}_{k}', 0.3 + i * 3.85, k * 3.7, 2.3 + j * 3.8, 0.3 + i * 3.85 + 3.7, (k + 1) * 3.7, 2.3 + j * 3.8 + 3.7)
            for i, j, k in cells]


def heap(band, seed=3):
    rng = random.Random(seed * 10 + band)
    if band == 1:
        cubes = [body('layer', 0, 0, 0.75), body('crown', 3.5, 2.5, HEIGHT[1])]
        tops = [(0, 0, 0.75), (3.5, 2.5, HEIGHT[1])]
    else:
        cubes, tops = mound(band, band + 1, 0.4)
    cubes += scatter(rng, 3 + band, surface(tops), (1.6, 2.4), 2.2, (X0, Z0, X1, Z1), ('y',), 'lump')
    return cubes


def covered(band):
    # A tarpaulin over the load: a smooth mound with two tie ropes across it, no loose pieces.
    if band == 1:
        cubes = [body('tarp', 1.0, 1.0, HEIGHT[1])]
        top = HEIGHT[1]
    else:
        cubes, tops = mound(band, band + 1, 0.45)
        top = tops[-1][2]
    for i, x in enumerate((4.5, 11.0)):
        cubes.append(box(f'rope{i}', x, 0.0, Z0 + 0.25, x + 0.5, top + 0.25, Z1 - 0.25))
    return cubes


SHAPES = {'ingots': ingots, 'gems': gems, 'blocks': blocks, 'rocks': rocks, 'heap': heap, 'covered': covered}


# ---- Textures ----

def noise(draw, rect, tones, rng, density=1.0):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            draw.point((x, y), fill=tones[rng.choices(range(len(tones)), weights=[1, 2 * density, 1][:len(tones)])[0]] + (255,))


def frame(draw, rect, light, dark):
    x0, y0, w, h = rect
    draw.line([x0, y0, x0 + w - 1, y0], fill=light + (255,))
    draw.line([x0, y0, x0, y0 + h - 1], fill=light + (255,))
    draw.line([x0, y0 + h - 1, x0 + w - 1, y0 + h - 1], fill=dark + (255,))
    draw.line([x0 + w - 1, y0, x0 + w - 1, y0 + h - 1], fill=dark + (255,))


def texture(look, form):
    rng = random.Random(look)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    A, B, C, D = (0, 0, 8, 8), (8, 0, 8, 8), (0, 8, 8, 8), (8, 8, 8, 8)
    if look in ORES:
        host, metal = PALETTE[ORES[look][0]], PALETTE[ORES[look][1]]
        for r, tones in ((A, host), (B, host), (C, host), (D, host)):
            noise(d, r, tones, rng)
        for r in (A, B, C, D):
            for _ in range(7):
                x, y = r[0] + rng.randrange(r[2]), r[1] + rng.randrange(r[3])
                d.point((x, y), fill=metal[2] + (255,))
                if x + 1 < r[0] + r[2]:
                    d.point((x + 1, y), fill=metal[1] + (255,))
        return img
    dark, mid, light = PALETTE[look]
    if form == 'ingots':
        # Body top: rows of bar tops; body side: stacked bar ends; pieces: polished bar faces.
        d.rectangle([0, 0, 7, 7], fill=mid + (255,))
        for y in (1, 3, 5, 7):
            d.line([0, y, 7, y], fill=dark + (255,))
        d.rectangle([8, 0, 15, 7], fill=dark + (255,))
        for y in (0, 2, 4, 6):
            for x in (8, 12):
                d.rectangle([x, y, x + 2, y], fill=light + (255,))
                d.point((x + 3, y), fill=mid + (255,))
        d.rectangle([0, 8, 7, 15], fill=mid + (255,))
        frame(d, C, light, dark)
        d.rectangle([8, 8, 15, 15], fill=dark + (255,))
        d.line([8, 8, 15, 8], fill=mid + (255,))
    elif form == 'gems':
        noise(d, A, (dark, mid, light), rng, 0.6)
        noise(d, B, (dark, dark, mid), rng)
        for r in (C, D):
            x0, y0 = r[0], r[1]
            d.rectangle([x0, y0, x0 + 7, y0 + 7], fill=mid + (255,))
            for i in range(8):
                d.point((x0 + i, y0 + i), fill=light + (255,))
                d.point((x0 + 7 - i, y0 + i), fill=dark + (255,))
            d.point((x0, y0), fill=(255, 255, 255, 255))
    elif form == 'blocks':
        for r in (A, B, C, D):
            x0, y0 = r[0], r[1]
            d.rectangle([x0, y0, x0 + 7, y0 + 7], fill=mid + (255,))
            noise(d, (x0 + 1, y0 + 1, 3, 3), (mid, mid, light), rng)
            frame(d, (x0, y0, 4, 4), light, dark)
            frame(d, r, light, dark)
    elif form == 'covered':
        # Olive canvas tarpaulin (body) with a darker weave; hemp tie ropes (pieces).
        for r in (A, B):
            x0, y0 = r[0], r[1]
            d.rectangle([x0, y0, x0 + 7, y0 + 7], fill=mid + (255,))
            for i in range(0, 8, 2):
                d.line([x0, y0 + i, x0 + 7, y0 + i], fill=light + (255,))
            for i in range(1, 8, 3):
                d.line([x0 + i, y0, x0 + i, y0 + 7], fill=dark + (255,))
        for r in (C, D):
            x0, y0 = r[0], r[1]
            d.rectangle([x0, y0, x0 + 7, y0 + 7], fill=(186, 150, 98, 255))
            for i in range(0, 8, 2):
                d.point((x0, y0 + i), fill=(132, 102, 62, 255))
    else:
        # Rocks and heaps: rough noise, pieces with a lit top-left edge.
        noise(d, A, (dark, mid, light), rng)
        noise(d, B, (dark, mid, mid), rng)
        noise(d, C, (dark, mid, light), rng, 1.5)
        noise(d, D, (dark, dark, mid), rng)
        d.line([0, 8, 7, 8], fill=light + (255,))
        d.line([0, 8, 0, 15], fill=light + (255,))
    return img


def png(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


# ---- Output ----

def main():
    variants = {}
    for form, looks in FORMS.items():
        for band in range(1, 5):
            name = f'{form}_{band}'
            cubes = SHAPES[form](band)
            model_file = os.path.join(ASSETS, 'models', 'block', 'cargo', 'form', name + '.json')
            write_json(model_file, {
                'credit': f'Generated by tools/models/generate_cargo_models.py; editable source art/blockbench/cargo/{name}.bbmodel',
                'textures': {'particle': '#cargo'},
                'elements': [gm.element_json(c, REGIONS, 16, '#cargo') for c in cubes],
            })
            # The Blockbench source shows the form's first material.
            sample = looks[0]
            tex_file = os.path.join(ASSETS, 'textures', 'block', 'cargo', sample + '.png')
            project = gm.bbmodel(f'cargo_{name}', cubes, f'cargo/{sample}', os.path.relpath(tex_file, ART), png(texture(sample, form)), REGIONS, 16,
                                 os.path.relpath(model_file, ART))
            project['textures'][0]['folder'] = 'block/cargo'
            write_json(os.path.join(ART, name + '.bbmodel'), project)
            for look in looks:
                write_json(os.path.join(ASSETS, 'models', 'block', 'cargo', f'{look}_{band}.json'),
                           {'parent': f'simple_block:block/cargo/form/{name}', 'textures': {'cargo': f'simple_block:block/cargo/{look}'}})
                variants[f'band={band},look={look}'] = {'model': f'simple_block:block/cargo/{look}_{band}'}
        for look in looks:
            write(os.path.join(ASSETS, 'textures', 'block', 'cargo', look + '.png'), png(texture(look, form)), binary=True)
    write_json(os.path.join(ASSETS, 'blockstates', 'cargo_fill.json'), {'variants': dict(sorted(variants.items()))})
    print(f'{len(written)} written, {len(skipped)} kept (exists; --force overwrites)')


if __name__ == '__main__':
    main()
