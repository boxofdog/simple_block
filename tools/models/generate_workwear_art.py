#!/usr/bin/env python3
"""Scaffolds the M10 flat placeholder art: armor-layer textures, 2D item icons and effect icons.

Writes, under src/main/resources/assets/simple_block:
  equipment/<name>.json                               which armor-layer textures each piece uses
  textures/entity/equipment/humanoid[_leggings]/*.png  64x32 vanilla armor-layer layout, flat colours
  textures/item/<name>.png, models/item/<name>.json, items/<name>.json   16x16 icons (item/generated)
  textures/mob_effect/<effect>.png                    18x18 effect icons

The Headlamp and the rope blocks are 3D models from tools/models/generate_models.py instead. Like that
script this is a one-off scaffold for painting over: it never overwrites an existing file unless run
with --force.

usage: python3 tools/models/generate_workwear_art.py [--force]
"""
import io
import json
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block')
FORCE = '--force' in sys.argv
written, skipped = [], []

DENIM, DENIM_DARK, BRASS = (58, 92, 150, 255), (40, 64, 110, 255), (214, 170, 72, 255)
LEATHER, LEATHER_DARK = (140, 92, 58, 255), (98, 62, 38, 255)
STEEL, STEEL_DARK, IRON = (176, 180, 188, 255), (110, 114, 122, 255), (206, 206, 206, 255)
HEMP, HEMP_DARK = (186, 150, 98, 255), (132, 102, 62, 255)
COPPER, COPPER_DARK = (204, 116, 74, 255), (150, 78, 50, 255)


def write(path, data):
    rel = os.path.relpath(path, ROOT)
    if os.path.exists(path) and not FORCE:
        skipped.append(rel)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(data)
    written.append(rel)


def write_json(path, obj):
    write(path, (json.dumps(obj, indent='\t') + '\n').encode())


def png(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


# ---- Armor layers (vanilla 64x32 humanoid layout) ----
# Leg box net at u 0-16, v 16-32 (front face 4-8 x 20-32), body at u 16-40, v 16-32 (front 20-28 x 20-32).

def leg_rows(draw, top, colour, dark):
    """Fills the four sides of the leg box from row `top` (of 12) down."""
    draw.rectangle([0, 20 + top, 15, 31], fill=colour)
    draw.line([0, 20 + top, 15, 20 + top], fill=dark)


def overalls_body():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Bib on the front, back panel, straps over the shoulders (no sleeves: the arms stay bare).
    d.rectangle([20, 24, 27, 31], fill=DENIM)
    d.rectangle([32, 24, 39, 31], fill=DENIM)
    for x in (21, 26):
        d.rectangle([x, 20, x, 23], fill=DENIM_DARK)
        d.rectangle([x + 12, 20, x + 12, 23], fill=DENIM_DARK)
    d.rectangle([20, 16, 27, 19], fill=(0, 0, 0, 0))
    d.point([(21, 24), (26, 24)], fill=BRASS)
    d.rectangle([22, 26, 25, 28], outline=DENIM_DARK)
    return img


def overalls_legs():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    leg_rows(d, 0, DENIM, DENIM_DARK)
    d.rectangle([20, 28, 27, 31], fill=DENIM)
    d.rectangle([32, 28, 39, 31], fill=DENIM)
    d.line([4, 31, 7, 31], fill=DENIM_DARK)
    return img


def boots(spiked):
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    leg_rows(d, 7, LEATHER, LEATHER_DARK)
    if spiked:
        for x in range(0, 16, 2):
            d.point((x, 31), fill=STEEL)
        d.line([4, 30, 7, 30], fill=STEEL_DARK)
    else:
        d.rectangle([4, 29, 7, 31], fill=IRON)
        d.line([4, 29, 7, 29], fill=STEEL_DARK)
    return img


# ---- Item icons ----

def icon_overalls():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 2, 4, 5], fill=DENIM_DARK)
    d.rectangle([11, 2, 11, 5], fill=DENIM_DARK)
    d.rectangle([4, 5, 11, 9], fill=DENIM)
    d.rectangle([3, 9, 12, 10], fill=DENIM)
    d.rectangle([3, 11, 7, 15], fill=DENIM)
    d.rectangle([8, 11, 12, 15], fill=DENIM)
    d.point([(4, 5), (11, 5)], fill=BRASS)
    d.rectangle([6, 6, 9, 8], outline=DENIM_DARK)
    return img


def icon_boots(spiked):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x0 in (2, 9):
        d.rectangle([x0, 4, x0 + 3, 11], fill=LEATHER)
        d.rectangle([x0, 11, x0 + 5, 13], fill=LEATHER)
        d.line([x0, 4, x0 + 3, 4], fill=LEATHER_DARK)
        if spiked:
            for x in range(x0, x0 + 6, 2):
                d.point((x, 14), fill=STEEL)
        else:
            d.rectangle([x0 + 3, 11, x0 + 5, 13], fill=IRON)
    return img


def icon_rope(anchor):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if anchor:
        d.rectangle([5, 1, 10, 2], fill=COPPER)
        d.rectangle([7, 3, 8, 4], fill=COPPER_DARK)
        top = 5
    else:
        top = 1
    for y in range(top, 15):
        d.point((7 + (y % 2), y), fill=HEMP)
        d.point((8 - (y % 2), y), fill=HEMP_DARK)
    if not anchor:
        d.ellipse([3, 10, 12, 15], outline=HEMP)
    return img


# ---- Effect icons (18x18) ----

def effect(kind):
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if kind == 'spelunkers_speed':
        for y in (3, 7, 11, 15):
            d.line([5, y, 12, y], fill=HEMP)
        d.line([5, 2, 5, 16], fill=HEMP_DARK)
        d.line([12, 2, 12, 16], fill=HEMP_DARK)
        d.polygon([(14, 2), (17, 6), (11, 6)], fill=(255, 255, 255, 255))
    elif kind == 'trail_blazer':
        d.line([4, 15, 4, 6], fill=HEMP_DARK, width=2)
        d.ellipse([2, 2, 7, 7], fill=(255, 210, 80, 255))
        d.line([13, 15, 13, 6], fill=HEMP_DARK, width=2)
        d.ellipse([11, 2, 16, 7], fill=(110, 160, 255, 255))
    elif kind == 'mule':
        d.rectangle([3, 7, 14, 13], fill=LEATHER)
        d.rectangle([5, 4, 12, 7], fill=(230, 230, 230, 255))
        d.line([3, 14, 3, 16], fill=LEATHER_DARK)
        d.line([14, 14, 14, 16], fill=LEATHER_DARK)
    elif kind == 'old_pro':
        d.line([3, 15, 13, 5], fill=LEATHER_DARK, width=2)
        d.arc([6, 1, 17, 12], 200, 340, fill=STEEL, width=2)
        d.rectangle([0, 16, 17, 17], fill=(200, 40, 40, 255))
    return img


def main():
    m = lambda path: {'model': {'type': 'minecraft:model', 'model': f'simple_block:{path}'}}
    eq = os.path.join(ASSETS, 'textures', 'entity', 'equipment')
    write(os.path.join(eq, 'humanoid', 'overalls.png'), png(overalls_body()))
    write(os.path.join(eq, 'humanoid_leggings', 'overalls.png'), png(overalls_legs()))
    write(os.path.join(eq, 'humanoid', 'spiked_boots.png'), png(boots(True)))
    write(os.path.join(eq, 'humanoid', 'mining_boots.png'), png(boots(False)))
    write_json(os.path.join(ASSETS, 'equipment', 'overalls.json'), {'layers': {
        'humanoid': [{'texture': 'simple_block:overalls'}], 'humanoid_leggings': [{'texture': 'simple_block:overalls'}]}})
    for name in ('spiked_boots', 'mining_boots'):
        write_json(os.path.join(ASSETS, 'equipment', name + '.json'), {'layers': {'humanoid': [{'texture': f'simple_block:{name}'}]}})
    icons = {'overalls': icon_overalls(), 'spiked_boots': icon_boots(True), 'mining_boots': icon_boots(False), 'rope': icon_rope(False),
             'spelunker_rope': icon_rope(True)}
    for name, img in icons.items():
        write(os.path.join(ASSETS, 'textures', 'item', name + '.png'), png(img))
        write_json(os.path.join(ASSETS, 'models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'simple_block:item/{name}'}})
        write_json(os.path.join(ASSETS, 'items', name + '.json'), m('item/' + name))
    for kind in ('spelunkers_speed', 'trail_blazer', 'mule', 'old_pro'):
        write(os.path.join(ASSETS, 'textures', 'mob_effect', kind + '.png'), png(effect(kind)))
    print('written:', *written, sep='\n  ')
    if skipped:
        print('kept (exists; --force overwrites):', *skipped, sep='\n  ')


if __name__ == '__main__':
    main()
