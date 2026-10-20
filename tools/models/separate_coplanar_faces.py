#!/usr/bin/env python3
"""M13 BUG-015/BUG-022: separates z-fighting faces, where two elements' faces lie flush in one plane and show
different parts of the texture. The element that should show grows by EPSILON pixels on every side (never past
the model's -16..32 bounds); where it cannot grow on a shared face because it is at that bound, the other
element's face steps back EPSILON instead. Only element bounds change, by the same amounts in the game model
and its Blockbench project (they share coordinates): UVs, textures and every other element stay as they are.

The original geometry is on a 0.1-pixel grid; an element already moved (its bounds off that grid) is left
alone, so re-running changes nothing. usage: python3 tools/models/separate_coplanar_faces.py
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
MODELS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simple_block', 'models', 'block')
ART = os.path.join(ROOT, 'art', 'blockbench')
EPSILON = 0.05
LOW, HIGH = -16.0, 32.0

# model -> elements whose faces should win where they lie flush on another element's.
GROW = {
    # The cornice caps the posts; lintels and sills lie across the jambs' ends.
    'blast_furnace': ['cornice', 'door_left_lintel', 'door_left_sill', 'door_right_lintel', 'door_right_sill'],
    # The lit tap glows in front of the dark tap hole it shares a face with.
    'blast_furnace_fire': ['fire_tap'],
    # The clamps sit outside the mold's walls and base.
    'blast_furnace_mold': ['mold_clamp_0', 'mold_clamp_1', 'mold_clamp_2', 'mold_clamp_3'],
    # The corner posts show over the rims (and the chute's back bar) flush with them.
    'ore_skip_bin': ['post_front_left', 'post_front_right', 'post_back_left', 'post_back_right'],
    'ore_skip_chute': ['post_back_left', 'post_back_right'],
}
# model -> {element: faces stepped back}, for the front (north, z -16) faces a winner there cannot grow past.
STEP_BACK = {
    'blast_furnace': {'post_left': ['north'], 'post_right': ['north'], 'door_left_jamb_left': ['north'],
                      'door_left_jamb_right': ['north'], 'door_right_jamb_left': ['north'], 'door_right_jamb_right': ['north'],
                      'tap_hole': ['north']},
    # The skip bin's corner posts reach its top (32), where the rims lie flush across them.
    'ore_skip_bin': {'post_front_left': ['up'], 'post_front_right': ['up'], 'post_back_left': ['up'], 'post_back_right': ['up']},
}
FACE = {'west': (0, 0), 'east': (0, 1), 'down': (1, 0), 'up': (1, 1), 'north': (2, 0), 'south': (2, 1)}


def on_grid(element):
    return all(abs(v * 10 - round(v * 10)) < 1e-6 for v in element['from'] + element['to'])


def grow(element):
    element['from'] = [round(max(LOW, v - EPSILON), 4) for v in element['from']]
    element['to'] = [round(min(HIGH, v + EPSILON), 4) for v in element['to']]


def step_back(element, faces):
    for face in faces:
        axis, high = FACE[face]
        if high:
            element['to'][axis] = round(element['to'][axis] - EPSILON, 4)
        else:
            element['from'][axis] = round(element['from'][axis] + EPSILON, 4)


def load(path):
    with open(path) as f:
        return json.load(f)


def save(path, model):
    with open(path, 'w') as f:
        f.write(json.dumps(model, indent='\t') + '\n')


for name in sorted(set(GROW) | set(STEP_BACK)):
    paths = [os.path.join(MODELS, name + '.json'), os.path.join(ART, name + '.bbmodel')]
    models = [load(p) for p in paths]
    changed = []
    for model in models:
        elements = {e.get('name'): e for e in model['elements']}
        missing = (set(GROW.get(name, [])) | set(STEP_BACK.get(name, {}))) - set(elements)
        if missing:
            raise SystemExit(f'{name}: no elements {sorted(missing)}')
        todo = [n for n in dict.fromkeys(GROW.get(name, []) + list(STEP_BACK.get(name, {}))) if on_grid(elements[n])]
        for element_name in todo:
            element = elements[element_name]
            element['from'] = [float(v) for v in element['from']]
            element['to'] = [float(v) for v in element['to']]
            if element_name in GROW.get(name, []):
                grow(element)
            step_back(element, STEP_BACK.get(name, {}).get(element_name, []))
        changed = todo
    for path, model in zip(paths, models):
        save(path, model)
    print(name, changed)
