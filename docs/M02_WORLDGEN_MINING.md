# M02 Worldgen and Mining Specification

## GEO-01: Remove unwanted natural geology

**Affected rock families (exact approved list for now):** granite, diorite, andesite, tuff. Include naturally generated polished or structural variants wherever those appear in overworld features/structures, and disable ordinary survival recipes/loot/trades that would reintroduce these materials or their crafted variants. Do **not** add calcite, dripstone, gravel, basalt, blackstone, etc. to the list without approval.

- In **newly generated Overworld terrain**, replace blocks from those families with **stone at stone elevations** and **deepslate at deepslate elevations**. Respect the game's stone-to-deepslate transition instead of inserting a visibly artificial horizontal cutoff; if an explicit boundary is required, document the chosen Y and how the transition region is handled.
- Apply to caves, cave features, ore-vein filler, and applicable naturally generated structures, while preserving feature shape, placement, and functionality. Tuff-heavy structures (e.g. trial-chamber palettes) need targeted checking; do not disable an entire structure just to avoid its tuff blocks.
- Don't just recolor textures, and don't perform heavy replacement scans every server tick. Prefer modifying the actual generation inputs/palettes/structure processors with a documented mechanism.
- These materials should not routinely be obtainable through survival worldgen, crafting, or loot. **Keep underlying registrations** when needed for engine compatibility or creative commands: this mod does not promise deletion from the game's internal registry.
- Avoid silently deleting ore where it previously replaced tuff/granite. Revisit native large iron- and copper-vein generation explicitly.
- No preservation/migration requirement for old saves.

## MINE-01: Deepslate progression gate

- Plain, naturally generated deepslate **and all deepslate ore blocks** can be broken/harvested by **iron-tier or better pickaxes**. Wooden and stone picks cannot successfully mine/harvest them (not simply “break but yield no drops” if the desired gate can be enforced sensibly for the selected MC version).
- Include the deepslate ores that remain in vanilla this milestone, even if their associated item (e.g. redstone) will be reevaluated in a *later* milestone.
- **Player-made deepslate products** (cobbled deepslate, polished deepslate, bricks/tiles, slabs, stairs, walls, etc.) use their **ordinary vanilla harvest requirements**. The iron-only rule does **not** apply to every block with “deepslate” in its name.
- The user does not demand block provenance tracking. Distinguish categories by block kind; do not add a per-block “naturally generated vs placed” database.
- Aboveground vanilla ore harvest tiers (e.g. stone iron ore) remain otherwise unchanged.
- Test harvest behavior, breaking speed if relevant, correct drops, and creative mode separately.

## ORE-01: Iron depth and geography

**Governing rule:** Modest ordinary iron is obtainable in all usual Overworld biomes at stone depth. **Per comparable volume of eligible rock**, baseline iron in ordinary deepslate is **moderately richer** than in ordinary stone. **Deep-ocean deepslate is richer again.** Biome-specific geological deposits add *localized* riches without eliminating ordinary baseline deposits.

### Background iron

- Stone layers: small, relatively sparse ore veins, sufficient for eventually obtaining initial iron tools.
- Deepslate layers: greater ore abundance than stone across ordinary non-oceanic biomes, rewarded only after players obtain an iron pickaxe.
- Deep-ocean deepslate: richer still than ordinary deepslate, with worthwhile developed-mine deposits.
- Non-special biomes must not be barren of iron, and newly created worlds must never require reaching inaccessible deepslate to obtain the first iron pickaxe.

### Geological enrichment (additional deposits)

| Region | Worldgen identity | Constraints |
|---|---|---|
| Mountains and peaks | Banded iron formation–inspired broad, lengthy, thin/stratified ore-rich bands; occasional cliffside exposures | Bands should be geographically discoverable and rewarding, not an iron ore layer coating every mountain |
| Deep ocean in deepslate | Larger, clustered iron ore bodies and high background concentration beneath deep water | Distinguish from ordinary deepslate; define eligibility using biome tags or an audited biome list |
| Deserts, arid canyons, and suitable riverbanks | Shallow alluvial/placer-inspired deposits with patchy accumulations alongside sand/gravel/river terrain | Not a universal buff to every desert block; prioritize characteristic landforms when obtainable from worldgen context |
| Natural underground lava/geothermal pockets | Irregular, medium-to-rich localized iron pockets around selected *natural* lava formations | Not every lava block, not player-created lava, and no per-tick ore generation |

Prefer existing iron ore and deepslate iron ore to new rock types or inventory items. Biome family and terrain tags should be explicit and easy to inspect. If exact riverbank/geothermal placement requires nontrivial worldgen mechanisms, implement documented deterministic rules that approximate the intended pattern rather than arbitrary random biome-wide boosts.

### Tuning / measurement

The **relative ordering and geological identities above are locked**; absolute numbers are not. Put feature counts, deposit geometry, depth boundaries, biome multipliers, and base spawn rates in a centralized, editable configuration. A working initial tuning hypothesis may use ~60–75% of vanilla ordinary iron at stone depths, but this is **not an agreed hard limit** and rich deposits may increase total iron in the world.

Benchmark new worlds using at least 5 shared seeds (more if cheap), fixed sample volumes/chunks, and comparable Y bands and biome categories. Report ore blocks per 100,000 eligible rock blocks (or similarly normalized measure), not only a total ore count that is biased by terrain type. Check ore accessibility and exposure as well as raw abundance. Include vanilla or pre-change control measurements, fresh-world screenshots or schematic slices, and generated-deposit examples.

## ORE-02: Diamonds

- Reduce ordinary diamond ore generation to **approximately 30–40% of vanilla** in comparable depth/rock samples. Use **35%** as an initial measurable target, not a guarantee that every playthrough takes a certain amount of time.
- Preserve the nature of diamond as a rare deep resource. No special surface diamond bonus or massive diamond-banded veins.
- Ensure ordinary iron still enables deepslate mining and the ore tier itself is not made unreachable.
- Test multiple same-seed control samples against vanilla. Report absolute and relative density and variation, not just “diamonds are rarer”.

## Must test

- Y transition: removed rock becomes appropriate stone/deepslate; no obvious sheet, holes, missing chunks, or odd cave wall artifacts.
- Features/structures: former tuff/granite content changes correctly while ore, trial structures, and copper veins remain operational.
- Tools: wood/stone cannot harvest plain deepslate or any deepslate ore; iron, diamond, netherite can; crafted deepslate variants remain vanilla.
- Iron: accessible ordinary stone; ordinary deepslate > ordinary stone; deep-ocean deepslate > ordinary deepslate; special deposits exist in targeted landscapes.
- Diamond: on average in 30–40% range versus vanilla, within sampling error. Record exceptions and statistical uncertainty.
- Fresh chunks/new worlds only. Do not claim “worldgen verified” from an empty superflat world; use normal terrain sampling.
