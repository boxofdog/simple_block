# Blockbench sources (Milestone 04 cable models, Milestone 05 haulage and engine models)

These are the editable sources of the cable and engine models. Open them in Blockbench (Java Block/Item format).
The textures are **UV templates, not artwork**: every cube's faces are flat colour blocks, shaded by
side (top lightest, bottom darkest) with a darker outline, so you can see which area of the texture
lands on which face. Paint over them.

| Source | Game model | Texture (shared by the models on its line) |
|---|---|---|
| `cable_anchor.bbmodel`, `cable_anchor_guide`, `cable_anchor_wheel_front`, `cable_anchor_wheel_back` | `models/block/cable_anchor*.json`: the anchor (block and item); M05: with a return guide (anchors along a loop); with the return wheel (the loop's last anchor; front and back are mirror images, so the wheel is always beyond the roller) | `textures/block/cable_anchor.png` (64 x 64) |
| `cable_drum.bbmodel`, `cable_drum_frame`, `cable_drum_spool`, `cable_drum_spool_wound`, `cable_drum_crank` | M05: `models/block/cable_drum_frame.json` is the placed block (the frame only); the spool (bare or wound) and the crank are drawn turning by the drum's renderer; `cable_drum.json` is all three together, for the item | `textures/block/cable_drum.png` (128 x 128) |
| `cobblestone_engine.bbmodel` | M05: `models/block/cobblestone_engine.json`, drawn in place of the furnace minecart | `textures/block/cobblestone_engine.png` (128 x 128) |
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
   its layout: the drum models use the same frame, spool and crank areas, the anchor models the same
   plate, arm and roller areas, and the five rod models the same areas for the same parts.
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
    the base is 5 pixels tall; the shaft is one block tall and is stretched to length;
  - M05, the drum's axle: y 9.5, z 8 (`CableDrumRenderer.AXLE_Y`, `AXLE_Z`). The spool and the crank
    turn about it; the crank is on the east end (x 16.5 to 21 of a north-facing drum), and a use
    counts as cranking from 14.5 pixels out on that side within 6 pixels of the axle
    (`CableDrumBlock.isCrank`). A hanging drum is the same model turned upside down; its cable leaves
    at y 11 (`CableRoutes.HANGING_EXIT_Y`);
  - M05, the loop: the return strand runs 4.75 pixels beside the roller's cable, on the arm side, at
    the roller's height (`CableLoops.RETURN_OFFSET`); the return wheel turns it 5 pixels beyond the
    roller (`CableLoops.WHEEL_OFFSET`);
  - M05, the engine: its wheels stand on y 0, the rail, and its length runs along x (the direction
    the cart travels), 22 pixels from x -3 to 19 (`CobblestoneEngineRenderer`).
- **Orientation:** the anchor and the drum are modelled facing north. The anchor's cable runs
  north-south with the hanger arm on the west; the drum pays out to the north. The game turns them
  by the block's facing. The rod's stem is on the east side of the cable, the side away from the
  anchors' arms; keep it there or the head would hit the arms.
- Java block models only allow rotations of 0, ±22.5 and ±45 degrees on one axis per cube.
- **Overlapping cubes must not share a face plane** (BUG-001). The octagonal flanges, wheels and
  rollers are built from four overlapping bands (straight, crossways and two at 45 degrees); where two
  bands' end faces lay in the same plane with different parts of the texture, the game drew them
  fighting each other, as flickering stripes. The generator now sets each band's ends back a tenth of
  a pixel from the next. Keep a gap like that if you reshape them.
- The cable between supports is not a model: it is drawn by code as a thin square tube using
  `textures/entity/cable.png` (16 x 16), where each of the tube's four sides shows the whole texture,
  u across the side and v along one block of cable.

## Regenerating

`tools/models/generate_models.py` made these files from a geometry description. Running it again
does **not** overwrite anything that exists (it lists what it kept); `--force` overwrites every file,
painted textures included. Once you have edited a model or texture in Blockbench, treat Blockbench
as the source and do not use `--force`. (M05 regenerated the cable files with `--force` for the new
parts and the BUG-001 fix; none of them had been painted yet.)
