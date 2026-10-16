# Blockbench painting — Joe's to-do

**Joe owns final texture painting and visual approval. Claude owns functioning models, UV maps, templates, rendering, animations and tests.** This is the one actionable painting queue. Historical milestone specs and reports stay in place as technical evidence, not competing to-do lists. Unchecked means not marked owner-approved; placeholder coloring is not finished painting.

## Existing templates — paint when geometry is final

Each existing paint job below now has **direct links to its PNG texture atlas and its matching editable Blockbench project(s)**. The links point to real committed files; click the PNG to view its current UV/color template and the .bbmodel to open/download the source. Multiple models often share **one atlas**—paint the shared texture once, then inspect all linked model states. Browse the [model/UV reference](../../art/blockbench/README.md) for export and safe-editing notes.

- [x] **Ore skip** — **Paint:** [ore_skip.png](../../src/main/resources/assets/simple_block/textures/block/ore_skip.png) (256×256). **Open in Blockbench:** [assembly / item](../../art/blockbench/ore_skip.bbmodel) · [bin](../../art/blockbench/ore_skip_bin.bbmodel) · [chute](../../art/blockbench/ore_skip_chute.bbmodel) · [closed gate](../../art/blockbench/ore_skip_gate_closed.bbmodel) · [open gate](../../art/blockbench/ore_skip_gate_open.bbmodel) · [closed lid](../../art/blockbench/ore_skip_lid_closed.bbmodel) · [open lid](../../art/blockbench/ore_skip_lid_open.bbmodel) · [bridle](../../art/blockbench/ore_skip_bridle.bbmodel). Tall bin, chute, intake, discharge gate and conditional bridle; check open/closed states.
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
- [x] **M08 Blast Furnace** — **Paint:** [blast_furnace.png](../../src/main/resources/assets/simple_block/textures/block/blast_furnace.png) (shared atlas). **Open:** [brick/iron body](../../art/blockbench/blast_furnace.bbmodel) · [lit firebox](../../art/blockbench/blast_furnace_fire.bbmodel) · [crown](../../art/blockbench/blast_furnace_crown.bbmodel) · [hopper](../../art/blockbench/blast_furnace_hopper.bbmodel) · [casting channel](../../art/blockbench/blast_furnace_channel.bbmodel) · [pouring metal](../../art/blockbench/blast_furnace_channel_pour.bbmodel) · [block mold](../../art/blockbench/blast_furnace_mold.bbmodel) · [molten mold](../../art/blockbench/blast_furnace_mold_molten.bbmodel). The cast block uses vanilla iron/copper/gold block textures, not a separate paint template. Paint brick courses, iron plates/rivets and glow; inspect idle, lit, pouring and full-mold states from front and above.
- [ ] **M08 Block of Charcoal** — **Paint:** [charcoal_block.png](../../src/main/resources/assets/simple_block/textures/block/charcoal_block.png) (64×64). **Open:** [block model](../../art/blockbench/charcoal_block.bbmodel). Compare against vanilla Block of Coal.

- [ ] **M10 Headlamp** (worn and held: one 3D model) — **Paint:** [headlamp.png](../../src/main/resources/assets/simple_block/textures/item/headlamp.png) (64×64). **Open:** [helmet, lantern and cap](../../art/blockbench/headlamp.bbmodel). Copper helmet shell and brim, lamp mount, an upright vanilla-style lantern (glass and glow), a copper cap over it instead of the hanging hook. Check it worn (third person) and in the hand and inventory.
- [ ] **M10 Overalls** — **Paint:** [body layer](../../src/main/resources/assets/simple_block/textures/entity/equipment/humanoid/overalls.png) and [legs layer](../../src/main/resources/assets/simple_block/textures/entity/equipment/humanoid_leggings/overalls.png) (64×32, vanilla armor-layer layout), [item icon](../../src/main/resources/assets/simple_block/textures/item/overalls.png) (16×16). Denim bib, straps and trousers; the arms stay bare. No Blockbench model (vanilla armor geometry).
- [ ] **M10 Spiked Boots and Mining Boots** — **Paint:** [spiked boots layer](../../src/main/resources/assets/simple_block/textures/entity/equipment/humanoid/spiked_boots.png), [mining boots layer](../../src/main/resources/assets/simple_block/textures/entity/equipment/humanoid/mining_boots.png) (64×32), icons [spiked](../../src/main/resources/assets/simple_block/textures/item/spiked_boots.png) · [mining](../../src/main/resources/assets/simple_block/textures/item/mining_boots.png) (16×16). Leather boots with dripstone spikes / an iron toe cap.
- [ ] **M10 bonus effect icons** — **Paint:** [Spelunker's Speed](../../src/main/resources/assets/simple_block/textures/mob_effect/spelunkers_speed.png) · [Trail Blazer](../../src/main/resources/assets/simple_block/textures/mob_effect/trail_blazer.png) · [Mule](../../src/main/resources/assets/simple_block/textures/mob_effect/mule.png) · [Old Pro](../../src/main/resources/assets/simple_block/textures/mob_effect/old_pro.png) (18×18 each).
- [ ] **M10 Spelunker's Rope** — **Paint:** [spelunker_rope.png](../../src/main/resources/assets/simple_block/textures/block/spelunker_rope.png) (64×64, shared by rope, coil and anchors), icons [rope](../../src/main/resources/assets/simple_block/textures/item/rope.png) · [spelunker's rope](../../src/main/resources/assets/simple_block/textures/item/spelunker_rope.png) (16×16). **Open:** [rope](../../art/blockbench/spelunker_rope.bbmodel) · [ground coil](../../art/blockbench/spelunker_rope_coil.bbmodel) · [anchor under a block](../../art/blockbench/spelunker_rope_anchor.bbmodel) · [anchor on a side](../../art/blockbench/spelunker_rope_anchor_side.bbmodel). Hemp rope, copper anchor.

## Touch-up only — already visually excellent

- [ ] **Universal four-gate minecart — minor touch-ups only.** The owner accepted the familiar vanilla minecart **visual direction**; repaint only genuinely visible UV seams, hinges, corners or gates. Inspect the **bottom-hinged end ramps** and **top-hinged long-side gates** in both positions. **Do not repaint/remodel the whole cart.** Sources: [Blockbench model](../../art/blockbench/gated_minecart.bbmodel) · [renderer](../../src/client/java/io/github/boxofdog/simpleblock/client/train/GatedMinecartModel.java). It shares vanilla `textures/entity/minecart.png` (64×32); a true repaint would need an explicit texture override or resource pack. Visual approval is not M07 SHIPPED acceptance.

## Waiting for Claude's new geometry/UV templates

- [ ] **M12 Monster Effigies — waiting for templates.** Four impaled monster monuments (zombie, skeleton, creeper, enderman) plus four compressed monster blocks. Claude supplies distinct editable Blockbench models, UVs and placeholder PNGs during M12; add exact asset links here once delivered. Joe paints final art after the geometry is stable. [M12 brief](../MILESTONE_12.md).

- [ ] **3D ordinary rail remodel** — delivered 2026-10-14: ordinary rails now use the Loading Rail's own texture and UVs (`textures/block/loading_track.png`), so painting the Loading Rail paints every rail too. Sources `art/blockbench/rail_straight`, `rail_curve`, `rail_ascending`. Check the painted sleepers and railheads also read well on curves and slopes.
- [ ] **M11 cart cargo fills** — geometry and UV templates delivered 2026-10-14 (placeholders in game): see the checklist below and [M11](../MILESTONE_11.md).

## M11 — cargo material painting checklist

**Each identity must look correct at LOW / MEDIUM / HIGH / FULL.** Empty carts expose their real floor. **Hopper-filling/unloading gradually crosses these levels in real time** (including after transferring by hand), so paint for recognizable level-to-level transitions; hoppers themselves are vanilla and need no new Joe painting task.

**What there is to paint: one 16 x 16 PNG per material, 31 in all** (the brief's 17, the 13 other cart cargoes the owner kept, and the covered fallback), in `src/main/resources/assets/simple_block/textures/block/cargo/<material>.png`. The four fill levels and all materials of a form share one geometry, so each PNG serves all four levels. Every PNG has the same layout:

| Quarter | Pixels | Painted onto |
|---|---|---|
| A, top-left | 0-7, 0-7 | the top of the load's body (stretched over it) |
| B, top-right | 8-15, 0-7 | the body's sides, from the top row down |
| C, bottom-left | 0-7, 8-15 | the top of each piece (bar, gem, block, rock, lump), from its top-left corner |
| D, bottom-right | 8-15, 8-15 | the sides and underside of each piece |

Geometry: `art/blockbench/cargo/<form>_<1-4>.bbmodel` (forms `ingots`, `gems`, `blocks`, `rocks`, `heap`, `covered`; levels 1-4), each opening with its form's first material. Placeholders: flat patterns in colours sampled from vanilla's own textures. **Never run `tools/models/generate_cargo_models.py --force` once a PNG is painted.**

### Ingot piles (bar shapes) — form `ingots`
- [ ] Gold ingots — `gold_ingot.png`
- [ ] Copper ingots — `copper_ingot.png`
- [ ] Iron ingots — `iron_ingot.png`
- [ ] Netherite ingots — `netherite_ingot.png`

### Loose gem piles (not ingot bars or storage blocks) — form `gems`
- [ ] Diamonds — `diamond.png`
- [ ] Emeralds — `emerald.png` (kept cargo)
- [ ] Lapis lazuli — `lapis_lazuli.png` (kept cargo)

### Neat cubic storage-block stacks — form `blocks`
- [ ] Gold blocks — `gold_block.png`
- [ ] Copper blocks — `copper_block.png`
- [ ] Iron blocks — `iron_block.png`
- [ ] Diamond blocks — `diamond_block.png`
- [ ] Netherite blocks — `netherite_block.png`
- [ ] Stone — `stone.png` (kept cargo)
- [ ] Deepslate — `deepslate.png` (kept cargo)

### Rough ore-rock piles (not refined cubes) — form `rocks`
- [ ] Gold ore — `gold_ore.png`
- [ ] Copper ore — `copper_ore.png`
- [ ] Iron ore — `iron_ore.png`
- [ ] Deepslate gold, copper and iron ore — `deepslate_gold_ore.png`, `deepslate_copper_ore.png`, `deepslate_iron_ore.png` (kept cargo)
- [ ] Raw gold, copper and iron — `raw_gold.png`, `raw_copper.png`, `raw_iron.png` (kept cargo; the commonest real loads)
- [ ] Coal — `coal.png` (kept cargo)

### Bulk loose material mounds — form `heap`
- [ ] Sand — `sand.png`
- [ ] Gravel — `gravel.png`
- [ ] Cobblestone — `cobblestone.png`
- [ ] Dirt — `dirt.png`
- [ ] Cobbled deepslate — `cobbled_deepslate.png` (kept cargo)
- [ ] Redstone — `redstone.png` (kept cargo)

### Fallback — form `covered`
- [ ] Covered load (olive tarpaulin, hemp tie ropes) — `covered.png`: shown only for a cargo item with no look of its own; names no material.

**Visual review:** Confirm all material forms differ in more than flat hue (e.g. iron ingot vs iron ore vs iron block); verify true pile shape at each height and no clipping through the four gates. Fill heights and silhouettes are owner-reviewable before art freeze.

## M12 — Monster Effigies and compressed blocks

Placeholder flat-colour templates, generated by `tools/models/generate_effigy_models.py` (2026-10-14). The effigies are grim impaled monuments: stone cairn, wooden stake, the monster's head and body run through. Models face south; keep their outline within the two blocks.

- [ ] Zombie Effigy — `art/blockbench/effigy/zombie_effigy.bbmodel`, texture `src/main/resources/assets/simple_block/textures/block/effigy/zombie_effigy.png`
- [ ] Skeleton Effigy — `art/blockbench/effigy/skeleton_effigy.bbmodel`, texture `.../textures/block/effigy/skeleton_effigy.png`
- [ ] Creeper Effigy — `art/blockbench/effigy/creeper_effigy.bbmodel`, texture `.../textures/block/effigy/creeper_effigy.png`
- [ ] Enderman Effigy — `art/blockbench/effigy/enderman_effigy.bbmodel`, texture `.../textures/block/effigy/enderman_effigy.png` (eyes not drawn yet)
- [ ] Zombie Block (packed rotten flesh) — `.../textures/block/zombie_block.png` (16 x 16, all faces)
- [ ] Skeleton Block (packed bones) — `.../textures/block/skeleton_block.png`
- [ ] Creeper Block (packed gunpowder) — `.../textures/block/creeper_block.png`
- [ ] Enderman Block (packed ender pearls) — `.../textures/block/enderman_block.png`

**Visual review:** each effigy recognisable at a distance and from behind; the facing (the monster's face) is the way its fear cone points.

## Painting and safe editing workflow

1. Open the matching `art/blockbench/*.bbmodel` and its shared paintable PNG. For a model showing the old embedded texture in Blockbench, **right-click the texture/UV map → Change File → select your painted PNG**. Keep the existing UV layout; multiple component models often share one atlas.
2. Save the texture from Blockbench (Texture > Save Texture or Ctrl+S on texture), then see it in-game using the client or rebuilt jar. Export edited geometry only when intended; scale UVs if changing PNG dimensions.
3. **Never force-regenerate painted assets:** tools/models/generate_models.py --force overwrites existing models and PNGs. Keep owner-painting backups and tell Claude not to overwrite them.
4. Check this list only once Joe has **personally accepted** a paint job. Claude adds newly created asset paths and art handoff notes here, not to the implementation brief.

Technical source inventory, shared UV notes and renderer pivot constraints: [art/blockbench/README.md](../../art/blockbench/README.md).
