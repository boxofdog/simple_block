# Blockbench sources (Milestone 04 cable models)

These are the editable sources of the cable models. Open them in Blockbench (Java Block/Item format).
The textures are **UV templates, not artwork**: every cube's faces are flat colour blocks, shaded by
side (top lightest, bottom darkest) with a darker outline, so you can see which area of the texture
lands on which face. Paint over them.

| Source | Game model | Texture (shared by the models on its line) |
|---|---|---|
| `cable_anchor.bbmodel` | `models/block/cable_anchor.json` (block and item) | `textures/block/cable_anchor.png` (32 x 32) |
| `cable_drum.bbmodel`, `cable_drum_wound.bbmodel` | `models/block/cable_drum.json` (bare: block and item), `cable_drum_wound.json` (with cable) | `textures/block/cable_drum.png` (128 x 128) |
| `cable_hook.bbmodel` | `models/block/cable_hook.json` (drawn at the cable's loose end) | `textures/block/cable_hook.png` (16 x 16) |
| `connecting_rod_base`, `_shaft`, `_head_open`, `_head_closed`, `copper_connecting_rod` | `models/block/connecting_rod_*.json` (drawn on a minecart), `models/item/copper_connecting_rod.json` (the item) | `textures/block/connecting_rod.png` (32 x 32) |
| `cable.bbmodel` | `models/item/cable.json` | `textures/item/cable.png` (16 x 16) |
| `cable_coil.bbmodel` | `models/item/cable_coil.json` | `textures/item/cable_coil.png` (16 x 16) |

Paths are under `src/main/resources/assets/simple_block/`. Each `.bbmodel` links its texture by a
relative path to that file, so **saving the texture in Blockbench writes the game's texture
directly**; each also carries a copy of the template inside it in case the link breaks.

## Painting

1. Open the `.bbmodel`, paint in Blockbench's paint mode (or in any image editor on the PNG).
2. Save the texture (Ctrl+S on the texture, or File > Save Texture). Models that share a texture share
   its layout: the two drum models use the same frame and flange areas, and the five rod models use
   the same areas for the same parts.
3. Run the game (`./gradlew runClient`) or rebuild the jar to see it.

The texture can be made larger (for example 64 x 64 for the anchor) as long as the UVs are scaled
with it: in Blockbench, change the texture size and let it rescale the UVs, then export the model
(below).

## Changing the shape

Edit the cubes in Blockbench, then **File > Export > Export Block/Item Model** over the game model
listed above (each project remembers its export path). Keep these in mind:

- **Fixed points the code relies on** (model pixels, 16 to a block):
  - the anchor's roller top, where the cable runs: y 12, x 8 (`CableRoutes.ROLLER_Y`);
  - the top of the drum's wound cable, where the cable leaves: y 14, over the middle
    (`CableRoutes.DRUM_EXIT_Y`);
  - the hook's eye at its top, y 8 (the entity is 0.5 blocks tall);
  - the rod head grips the cable at y 4 of the head model, x 8 (`CargoMinecartRenderer`);
    the base is 5 pixels tall; the shaft is one block tall and is stretched to length.
- **Orientation:** the anchor and the drum are modelled facing north. The anchor's cable runs
  north-south with the hanger arm on the west; the drum pays out to the north. The game turns them
  by the block's facing. The rod's stem is on the east side of the cable, the side away from the
  anchors' arms; keep it there or the head would hit the arms.
- Java block models only allow rotations of 0, ±22.5 and ±45 degrees on one axis per cube.
- The cable between supports is not a model: it is drawn by code as a thin square tube using
  `textures/entity/cable.png` (16 x 16), where each of the tube's four sides shows the whole texture,
  u across the side and v along one block of cable.

## Regenerating

`tools/models/generate_models.py` made these files from a geometry description. Running it again
does **not** overwrite anything that exists (it lists what it kept); `--force` overwrites every file,
painted textures included. Once you have edited a model or texture in Blockbench, treat Blockbench
as the source and do not use `--force`.
