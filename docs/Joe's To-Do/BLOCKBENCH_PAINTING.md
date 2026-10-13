# Blockbench painting — Joe's to-do

**Joe owns final texture painting and visual approval. Claude owns functioning models, UV maps, templates, rendering, animations and tests.** This is the one actionable painting queue. Historical milestone specs and reports stay in place as technical evidence, not competing to-do lists. Unchecked means not marked owner-approved; placeholder coloring is not finished painting.

## Existing templates — paint when geometry is final

All asset paths below are inside src/main/resources/assets/simple_block/, with editable source projects in art/blockbench/. Each row refers to a **shared PNG atlas**, often used by multiple Blockbench projects.

- [ ] **Ore skip** — textures/block/ore_skip.png (256x256); ore_skip*.bbmodel: tall bin, chute, intake, discharge gate, conditional bridle, item; check open/closed states.
- [ ] **Cobblestone Engine** — textures/block/cobblestone_engine.png (128x128); cobblestone_engine.bbmodel: furnace, smokestack, boiler. Verify once the new four-gate minecart frame is finalized.
- [ ] **Cable drum, spool, crank, powered furnace frame and hanging motor** — textures/block/cable_drum.png (128x128); cable_drum*.bbmodel. One atlas serves the frame variants and rotating parts.
- [ ] **Powered winch lever** — textures/block/cable_winch.png (32x32); cable_drum_lever.bbmodel.
- [ ] **Anchor, return guides, return wheels** — textures/block/cable_anchor.png (64x64); cable_anchor.bbmodel, cable_anchor_guide.bbmodel, cable_anchor_wheel_*.bbmodel.
- [ ] **Ceiling pulley** (the head anchor while a skip hangs from it) — its own atlas textures/block/cable_pulley.png; cable_anchor_pulley.bbmodel. Confirm it appears only while a skip is attached.
- [ ] **Loose cable hook** — textures/block/cable_hook.png (16x16); cable_hook.bbmodel.
- [ ] **Cable run/strand** — textures/entity/cable.png (16x16); procedurally generated geometry rather than a Blockbench mesh. Test moving/scrolling pattern.
- [ ] **Minecart connecting rod** — textures/block/connecting_rod.png (32x32); connecting_rod_*.bbmodel plus copper_connecting_rod.bbmodel.
- [ ] **Loading Rail** — textures/block/loading_track.png (64x64); loading_track*.bbmodel; hinged stop up/down.
- [ ] **Cable item** — textures/item/cable.png (16x16); cable.bbmodel.
- [ ] **Cable coil item** — textures/item/cable_coil.png (16x16); cable_coil.bbmodel.

## Touch-up only — already visually excellent

- [ ] **Universal four-gate minecart — MINOR TEXTURE TOUCH-UPS ONLY, not a full repaint.** **Owner's 2026-10-13 in-game review:** Claude's rebuilt minecart, including the new short-end and long-side gates, **reuses the existing vanilla minecart texture**. The resulting overall appearance is excellent and is **accepted as the visual direction**; preserve the familiar gray/iron vanilla texture, its shape and the shared cart identity for freight, passenger and Cobblestone Engine configurations. Joe only needs to touch up any small UV seams, corner-post mismatches, hinge/rim/gate transition details or stray pixels **if actually visible in game**. Check both **bottom-hinged short-end ramps** and **top-hinged long-side gates**, open and closed, from several angles. **Do not create a replacement texture atlas, retexture every panel, or remodel/repaint the entire chassis just to satisfy the old checklist.** Committed (Claude, 2026-10-13): geometry in code, client/train/GatedMinecartModel (layer simple_block:gated_minecart), editable source art/blockbench/gated_minecart.bbmodel (Modded Entity, box UV, 64x32), texture = vanilla's textures/entity/minecart.png (not copied into the repo: a touch-up means adding an override PNG at assets/minecraft/textures/entity/minecart.png in this mod, or a resource pack). The owner's screenshot review is a visual review, **not proof the implementation is pushed, CI-green, or M07 owner-accepted**.

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
