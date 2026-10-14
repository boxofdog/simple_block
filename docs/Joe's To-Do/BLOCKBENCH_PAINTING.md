# Blockbench painting — Joe's to-do

**Joe owns final texture painting and visual approval. Claude owns functioning models, UV maps, templates, rendering, animations and tests.** This is the one actionable painting queue. Historical milestone specs and reports stay in place as technical evidence, not competing to-do lists. Unchecked means not marked owner-approved; placeholder coloring is not finished painting.

## Existing templates — paint when geometry is final

Each existing paint job below now has **direct links to its PNG texture atlas and its matching editable Blockbench project(s)**. The links point to real committed files; click the PNG to view its current UV/color template and the .bbmodel to open/download the source. Multiple models often share **one atlas**—paint the shared texture once, then inspect all linked model states. Browse the [model/UV reference](../../art/blockbench/README.md) for export and safe-editing notes.

- [ ] **Ore skip** — **Paint:** [ore_skip.png](../../src/main/resources/assets/simple_block/textures/block/ore_skip.png) (256×256). **Open in Blockbench:** [assembly / item](../../art/blockbench/ore_skip.bbmodel) · [bin](../../art/blockbench/ore_skip_bin.bbmodel) · [chute](../../art/blockbench/ore_skip_chute.bbmodel) · [closed gate](../../art/blockbench/ore_skip_gate_closed.bbmodel) · [open gate](../../art/blockbench/ore_skip_gate_open.bbmodel) · [closed lid](../../art/blockbench/ore_skip_lid_closed.bbmodel) · [open lid](../../art/blockbench/ore_skip_lid_open.bbmodel) · [bridle](../../art/blockbench/ore_skip_bridle.bbmodel). Tall bin, chute, intake, discharge gate and conditional bridle; check open/closed states.
- [ ] **Cobblestone Engine** — **Paint:** [cobblestone_engine.png](../../src/main/resources/assets/simple_block/textures/block/cobblestone_engine.png) (128×128). **Open:** [engine model](../../art/blockbench/cobblestone_engine.bbmodel). Furnace, smokestack and boiler; verify inside the four-gate minecart frame.
- [ ] **Cable drum, spool, crank, powered furnace frame and hanging motor** — **Paint:** [cable_drum.png](../../src/main/resources/assets/simple_block/textures/block/cable_drum.png) (128×128). **Open:** [assembly / item](../../art/blockbench/cable_drum.bbmodel) · [frame](../../art/blockbench/cable_drum_frame.bbmodel) · [powered frame](../../art/blockbench/cable_drum_frame_powered.bbmodel) · [hanging frame](../../art/blockbench/cable_drum_frame_powered_hanging.bbmodel) · [spool](../../art/blockbench/cable_drum_spool.bbmodel) · [wound spool](../../art/blockbench/cable_drum_spool_wound.bbmodel) · [crank](../../art/blockbench/cable_drum_crank.bbmodel). Shared atlas across frame variants and rotating parts.
- [ ] **Powered winch lever** — **Paint:** [cable_winch.png](../../src/main/resources/assets/simple_block/textures/block/cable_winch.png) (32×32). **Open:** [lever model](../../art/blockbench/cable_drum_lever.bbmodel).
- [ ] **Anchor, return guides, return wheels** — **Paint:** [cable_anchor.png](../../src/main/resources/assets/simple_block/textures/block/cable_anchor.png) (64×64). **Open:** [anchor](../../art/blockbench/cable_anchor.bbmodel) · [guide](../../art/blockbench/cable_anchor_guide.bbmodel) · [front wheel](../../art/blockbench/cable_anchor_wheel_front.bbmodel) · [back wheel](../../art/blockbench/cable_anchor_wheel_back.bbmodel).
- [ ] **Ceiling pulley** (head anchor when a skip hangs from it) — **Paint:** [cable_pulley.png](../../src/main/resources/assets/simple_block/textures/block/cable_pulley.png). **Open:** [pulley model](../../art/blockbench/cable_anchor_pulley.bbmodel). Confirm it appears only while a skip is attached.
- [ ] **Loose cable hook** — **Paint:** [cable_hook.png](../../src/main/resources/assets/simple_block/textures/block/cable_hook.png) (16×16). **Open:** [hook model](../../art/blockbench/cable_hook.bbmodel).
- [ ] **Cable run/strand** — **Paint:** [cable.png](../../src/main/resources/assets/simple_block/textures/entity/cable.png) (16×16). Procedurally generated geometry; **no Blockbench mesh**. Check the moving/scrolling pattern in-game.
- [ ] **Minecart connecting rod** — **Paint:** [connecting_rod.png](../../src/main/resources/assets/simple_block/textures/block/connecting_rod.png) (32×32). **Open:** [item / assembly](../../art/blockbench/copper_connecting_rod.bbmodel) · [base](../../art/blockbench/connecting_rod_base.bbmodel) · [shaft](../../art/blockbench/connecting_rod_shaft.bbmodel) · [closed head](../../art/blockbench/connecting_rod_head_closed.bbmodel) · [open head](../../art/blockbench/connecting_rod_head_open.bbmodel).
- [ ] **Loading Rail** — **Paint:** [loading_track.png](../../src/main/resources/assets/simple_block/textures/block/loading_track.png) (64×64). **Open:** [stop down](../../art/blockbench/loading_track.bbmodel) · [stop raised](../../art/blockbench/loading_track_raised.bbmodel). Check hinged stop up/down.
- [ ] **Cable item** — **Paint:** [cable.png](../../src/main/resources/assets/simple_block/textures/item/cable.png) (16×16). **Open:** [item model](../../art/blockbench/cable.bbmodel).
- [ ] **Cable coil item** — **Paint:** [cable_coil.png](../../src/main/resources/assets/simple_block/textures/item/cable_coil.png) (16×16). **Open:** [coil model](../../art/blockbench/cable_coil.bbmodel).
- [ ] **M08 Blast Furnace** — **Paint:** [blast_furnace.png](../../src/main/resources/assets/simple_block/textures/block/blast_furnace.png) (shared atlas). **Open:** [brick/iron body](../../art/blockbench/blast_furnace.bbmodel) · [lit firebox](../../art/blockbench/blast_furnace_fire.bbmodel) · [crown](../../art/blockbench/blast_furnace_crown.bbmodel) · [hopper](../../art/blockbench/blast_furnace_hopper.bbmodel) · [casting channel](../../art/blockbench/blast_furnace_channel.bbmodel) · [pouring metal](../../art/blockbench/blast_furnace_channel_pour.bbmodel) · [block mold](../../art/blockbench/blast_furnace_mold.bbmodel) · [molten mold](../../art/blockbench/blast_furnace_mold_molten.bbmodel). The cast block uses vanilla iron/copper/gold block textures, not a separate paint template. Paint brick courses, iron plates/rivets and glow; inspect idle, lit, pouring and full-mold states from front and above.
- [ ] **M08 Block of Charcoal** — **Paint:** [charcoal_block.png](../../src/main/resources/assets/simple_block/textures/block/charcoal_block.png) (64×64). **Open:** [block model](../../art/blockbench/charcoal_block.bbmodel). Compare against vanilla Block of Coal.

## Touch-up only — already visually excellent

- [ ] **Universal four-gate minecart — MINOR TEXTURE TOUCH-UPS ONLY, not a full repaint.** **Owner's 2026-10-13 in-game review:** Claude's rebuilt minecart, including the new short-end and long-side gates, **reuses the existing vanilla minecart texture**. The resulting overall appearance is excellent and is **accepted as the visual direction**; preserve the familiar gray/iron vanilla texture, its shape and the shared cart identity for freight, passenger and Cobblestone Engine configurations. Joe only needs to touch up any small UV seams, corner-post mismatches, hinge/rim/gate transition details or stray pixels **if actually visible in game**. Check both **bottom-hinged short-end ramps** and **top-hinged long-side gates**, open and closed, from several angles. **Do not create a replacement texture atlas, retexture every panel, or remodel/repaint the entire chassis just to satisfy the old checklist.** Committed (Claude, 2026-10-13): geometry in code, client/train/GatedMinecartModel (layer simple_block:gated_minecart), editable source [editable four-gate minecart model](../../art/blockbench/gated_minecart.bbmodel) and [renderer model source](../../src/client/java/io/github/boxofdog/simpleblock/client/train/GatedMinecartModel.java) (Modded Entity, box UV, 64x32), texture = vanilla's textures/entity/minecart.png (not copied into the repo: a touch-up means adding an override PNG at assets/minecraft/textures/entity/minecart.png in this mod, or a resource pack). The owner's screenshot review is a visual review, **not proof the implementation is pushed, CI-green, or M07 owner-accepted**.

## Waiting for Claude's new geometry/UV templates

- [ ] **3D ordinary rail remodel** — paint the future rail/sleeper UVs once a separate roadmap implementation supplies the model and templates. Match the Loading Rail's gauge, level and joints.
- [ ] **M11 cart cargo fills** — paint after Claude supplies five actual silhouette families, fill-height geometry, unique material mapping and reusable UV templates. See material list below and [M11](../MILESTONE_11.md). Don't paint speculative layouts before their UVs exist.

## M11 — cargo material painting checklist

**Each identity must look correct at LOW / MEDIUM / HIGH / FULL.** Empty carts expose their real floor. **Hopper-filling/unloading gradually crosses these levels in real time** (including after transferring by hand), so paint for recognizable level-to-level transitions; hoppers themselves are vanilla and need no new Joe painting task. Reuse geometry and atlases where sensible: **17 material identities x 4 fill levels = 68 render combinations, not necessarily 68 separate paintings.**

### Ingot piles (bar shapes)
- [ ] Gold ingots
- [ ] Copper ingots
- [ ] Iron ingots
- [ ] Netherite ingots

### Loose gem piles (not ingot bars or storage blocks)
- [ ] Diamonds

### Neat cubic storage-block stacks
- [ ] Gold blocks
- [ ] Copper blocks
- [ ] Iron blocks
- [ ] Diamond blocks
- [ ] Netherite blocks

### Rough ore-rock piles (not refined cubes)
- [ ] Gold ore
- [ ] Copper ore
- [ ] Iron ore

### Bulk loose material mounds
- [ ] Sand
- [ ] Gravel
- [ ] Cobblestone
- [ ] Dirt

**Visual review:** Confirm all material forms differ in more than flat hue (e.g. iron ingot vs iron ore vs iron block); verify true pile shape at each height and no clipping through the four gates. Adjust actual texture list when Claude delivers the final paintable files.

## Painting and safe editing workflow

1. Open the matching art/blockbench/*.bbmodel and paint its linked UV PNG in Blockbench or a pixel editor. Shared PNGs are shared between the component models listed above.
2. Save the texture from Blockbench (Texture > Save Texture or Ctrl+S on texture), then see it in-game using the client or rebuilt jar. Export edited geometry only when intended; scale UVs if changing PNG dimensions.
3. **Never force-regenerate painted assets:** tools/models/generate_models.py --force overwrites existing models and PNGs. Keep owner-painting backups and tell Claude not to overwrite them.
4. Check this list only once Joe has **personally accepted** a paint job. Claude adds newly created asset paths and art handoff notes here, not to the implementation brief.

Technical source inventory, shared UV notes and renderer pivot constraints: [art/blockbench/README.md](../../art/blockbench/README.md).
