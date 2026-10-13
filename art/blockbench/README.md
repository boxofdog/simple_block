# Blockbench sources (Milestone 04 cable models, Milestone 05 haulage and engine models, Milestone 06 winch and station models, Milestone 07 remodels, Milestone 08 furnace)

These are the editable sources of the cable and engine models. Open them in Blockbench (Java Block/Item format).
The textures are **UV templates, not artwork**: every cube's faces are flat colour blocks, shaded by
side (top lightest, bottom darkest) with a darker outline, so you can see which area of the texture
lands on which face. Paint over them.

| Source | Game model | Texture (shared by the models on its line) |
|---|---|---|
| `cable_anchor.bbmodel`, `cable_anchor_guide`, `cable_anchor_wheel_front`, `cable_anchor_wheel_back` | `models/block/cable_anchor*.json`: the anchor (block and item); M05: with a return guide (anchors along a loop); with the return wheel (the loop's last anchor; front and back are mirror images, so the wheel is always beyond the roller) | `textures/block/cable_anchor.png` (64 x 64) |
| `cable_drum.bbmodel`, `cable_drum_frame`, `cable_drum_frame_powered`, `cable_drum_frame_powered_hanging`, `cable_drum_spool`, `cable_drum_spool_wound`, `cable_drum_crank` | M05: `models/block/cable_drum_frame.json` is the placed block (the frame only); the spool (bare or wound) and the crank are drawn turning by the drum's renderer; `cable_drum.json` is all three together, for the item. M07: a coal winch's frame (`powered=true`) is `cable_drum_frame_powered`: the furnace motor (plinth, firebox, bearing on the axle, gear plate, smokestack) **in place of** the west (motor-side) stand. Hung under a beam (`_hanging`, drawn upside down) its smokestack is replaced by an outboard flue that rises beside the beam | `textures/block/cable_drum.png` (128 x 128) |
| `cobblestone_engine.bbmodel` | M07 remodel: `models/block/cobblestone_engine.json`, a furnace engine (firebox, boiler, steam dome, tall smokestack) drawn **inside** the minecart body (`gated_minecart`, its gates shut). The cart's walls are at model x -2..0 and 16..18, z 0..2 and 14..16; its floor top at y 3 and rim at y 11 | `textures/block/cobblestone_engine.png` (128 x 128) |
| `cable_drum_lever` | M06: the coal winch's lever (where the crank was), drawn by the drum's renderer in place of the crank (M07: the separate furnace box is gone; the motor is part of the powered frame above) | `textures/block/cable_winch.png` (32 x 32) |
| `loading_track.bbmodel`, `loading_track_raised` | M06 (corrected 2026-10-13): the Loading Rail (block and item): rails on sleepers and the hinged triangular stop on the inside edge of the west rail, folded flat (`loading_track`, also the item) or raised behind a docked cart (`loading_track_raised`) | `textures/block/loading_track.png` (64 x 64) |
| `gated_minecart.bbmodel` | M07 (owner's cart redesign): every minecart's body, an **entity** model (Blockbench: Modded Entity, box UV) on vanilla's `textures/entity/minecart.png` (64 x 32), which is not in this repository: load it onto the project from the game. Vanilla's floor, four corner columns, a rim along the top of each long side, two end gates (hinged at their outer bottom edge, folding out and down into ramps) and two side gates (hinged under the rims, swinging up and out). Each group's pivot is its hinge. The game draws `client/train/GatedMinecartModel` (the same numbers); after reshaping it here, carry the change over (File > Export > Export Java Entity). Written by `tools/models/generate_minecart_bbmodel.py`. (Replaces the M06 `freight_gate` models.) | vanilla `textures/entity/minecart.png` |
| `cable_anchor_pulley` | M07 playtest patch: the head anchor while a skip hangs from it, a ceiling pulley (mount plate, cheeks, axle, wheel) | `textures/block/cable_pulley.png` |
| `ore_skip_chute`, `ore_skip_bin`, `ore_skip_gate_closed`, `ore_skip_gate_open`, `ore_skip_lid_closed`, `ore_skip_lid_open`, `ore_skip_bridle`, `ore_skip` | M07: the ore skip, drawn in parts by its renderer, facing north (front, chute mouth, to z 0): the open chute (block 1), the two-block bin (blocks 2-3, model y 0..32), its sliding discharge gate under the bin (closed, or slid sideways), the intake lid (closed, or flipped up at the back), the triangular bridle (block 4: two legs to one apex); `ore_skip` is the item. The body is drawn from x -6 to 22, 6 px into the cell on each side (which the skip keeps clear) | `textures/block/ore_skip.png` (256 x 256) |
| `pre_industrial_blast_furnace`, `_fire`, `_crown`, `_hopper`, `_channel`, `_channel_pour`, `_mold`, `_mold_molten` | M08 (owner's direction, 2026-10-13: a very large brick-and-iron fireplace), facing north (front to z -16). `pre_industrial_blast_furnace` is drawn by the controller (the middle of the second layer): stone footing, brick body with a string course, and the enclosed iron front (corner posts, cornice, bands, two firebox doors with grilles, the tap housing and tap hole), spanning x and z -16..32, y -16..32. `_fire` (grilles and tap glowing) is added while lit. `_crown` (drawn by the top layer's middle block) is the brick crown with its stone coping, leaving the back middle block to `_hopper`, an iron charging funnel with a rim a little proud of the crown. `_channel` is the block in front of the furnace: an iron trough stepping down from the tap on a trestle; `_channel_pour` the metal running in it. `_mold` is the block beyond: a low iron flask with corner clamps and top ties framing a whole block; `_mold_molten` fills it while pouring, and the cast block in it uses the game's own `iron_block`, `copper_block` and `gold_block` textures (`pre_industrial_blast_furnace_mold_iron/_copper/_gold.json`, written by the generator, no Blockbench source). The template's flat colours are by material (brick, stone, iron, dark iron, fire, molten metal); the furnace item uses the main model, scaled down | `textures/block/pre_industrial_blast_furnace.png` |
| `charcoal_block` | M08: the Block of Charcoal (block and item), one cube | `textures/block/charcoal_block.png` (64 x 64) |
| `cable_hook.bbmodel` | `models/block/cable_hook.json` (drawn at the cable's loose end) | `textures/block/cable_hook.png` (16 x 16) |
| `connecting_rod_base`, `_shaft`, `_head_open`, `_head_closed`, `copper_connecting_rod` | `models/block/connecting_rod_*.json` (drawn on a minecart), `models/item/copper_connecting_rod.json` (the item) | `textures/block/connecting_rod.png` (32 x 32) |
| `cable.bbmodel` | `models/item/cable.json` | `textures/item/cable.png` (16 x 16) |
| `cable_coil.bbmodel` | `models/item/cable_coil.json` | `textures/item/cable_coil.png` (16 x 16) |

Paths are under `src/main/resources/assets/simple_block/`. Each `.bbmodel` links its texture by a
relative path to that file, so **saving the texture in Blockbench writes the game's texture
directly**; each also carries a copy of the template inside it in case the link breaks.

## Owner painting checklist

The **actionable painting instructions, all owner art checkboxes and safe texture-saving workflow** have moved to [docs/Joe's To-Do/BLOCKBENCH_PAINTING.md](../../docs/Joe's%20To-Do/BLOCKBENCH_PAINTING.md). The table above and the technical geometry/pivot guidelines below stay here as a **reference** for Claude and Blockbench editing. Add new templates and final art paths to the owner's centralized queue.

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
  - M06, the coal winch: the lever turns about the drum's axle (y 9.5, z 8), 35 degrees back for
    PULL and forward for FEED (`CableDrumRenderer.LEVER_TILT`); a use on the lever counts where it
    counted on the crank. The furnace is not turned.
  - M06, the Loading Rail faces north (a docked cart's front points north). Its stop sits on the west
    rail's inside edge (x 4 to 6.5). Folded it stays below y 3 so a cart's floor passes over it; raised
    it stands from z 17 (a docked cart's rear wall is at z 18) to z 21, up to y 10, past the block's
    south edge. The raised stop is also the block's outline for clicking (`LoadingTrackBlock.RAISED_SHAPES`).
  - M06, the freight gate: x 0 is the outside of the cart's end wall, y 0 the underside of the cart
    floor, z 0 to 16 across the cart (`CargoMinecartRenderer.GATE_*`). Closed it stands up over the
    wall; open it lies flat outwards.
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
