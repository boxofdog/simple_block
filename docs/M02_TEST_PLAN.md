# Milestone 02 — QA and Acceptance Plan

**Principle:** Compile success ≠ behavior verified. This repository reportedly already has a strong Milestone 01 test harness capable of running actual Minecraft and capturing screenshots. Reuse and extend it rather than inventing an incompatible test pipeline.

## Reporting convention

For every row, record **PASS / FAIL / NOT RUN**, method (unit/integration/GameTest/dev-client/manual), evidence path/test name/console output, and any limitation. A screenshot proves only what is visible in it. Dedicated-server/multiplayer checks should be attempted where affordable; if not run, label them honestly. Keep all Milestone 01 regression checks in the complete build.

### Baseline / production workflow

- [ ] BASE-01 — Inspect live Minecraft/Fabric/Java/mod metadata, Git status, installed test infrastructure, and current project instructions; record output before edits.
- [ ] BASE-02 — Record baseline `./gradlew build` and existing test results. Do not mistake preexisting failures for regressions.
- [ ] BASE-03 — After implementation, run all earlier Milestone 01 tests: sleep/time/storm, phantom, gliding, shulker restrictions.
- [ ] BASE-04 — Launch a real client in a new testing world; capture visual evidence. Mark any missing direct gameplay interactions NOT RUN.
- [ ] BASE-05 — Existing GitHub Actions production/remapped JAR build and updater compatibility remain functional; no breaking rename of artifact or mod ID.
- [ ] BASE-06 — Where feasible, dedicated-server/server-authority smoke test; no client-only enforcement for gameplay rules.

### Geology and mining

- [ ] GEO-01 — Across representative above-transition generated Overworld terrain, **no granite/diorite/andesite/tuff** naturally generated; replaced by stone.
- [ ] GEO-02 — Across representative below-transition generated terrain, those blocks replaced by deepslate; coherent transition with no new horizontal fault artifacts.
- [ ] GEO-03 — Natural cave systems, structures, and ore features still function; verify tuff-heavy structure features and former granite/tuff-associated ore deposits.
- [ ] GEO-04 — No ordinary survival recipes/loot/trades reintroduce removed rock families; creative/command existence is documented separately.
- [ ] MINE-01 — Natural plain deepslate wood/stone pick cannot mine/harvest; iron/diamond/netherite picks can.
- [ ] MINE-02 — All deepslate ore blocks have required iron-tier mining; stone variant ores otherwise keep normal tiers.
- [ ] MINE-03 — Cobbled/polished/decorative deepslate retains vanilla mining requirement, including slabs, stairs and walls.
- [ ] ORE-01 — Background iron exists in ordinary stone and deepslate in typical biomes; samples normalized to eligible rock volume show deepslate > stone.
- [ ] ORE-02 — Comparable deep-ocean deepslate samples show greater iron density than ordinary deepslate.
- [ ] ORE-03 — Demonstrate real generated deposits: mountain BIF bands; arid river/canyon alluvium; natural lava-adjacent pockets; oceanic deep deposits. Report example seeds/coordinates (within disposable test worlds).
- [ ] ORE-04 — Diamond ratio about **0.30–0.40** of vanilla with caveats about sample uncertainty; preserve plausible accessibility.

**Ore methodology:** Generate at least **five matching test seeds** for vanilla/control and modded normal worlds. Select comparable dimensions, Y bands, and biome groups; normalize by eligible stone/deepslate volumes. Report sample size, generated block counts, means/medians, ratio, and exceptions. Document whether a seed or class lacked a target biome. If full automated comparisons are unavailable, provide honest partial results rather than invented statistics. Superflat worlds are not valid ore-generation samples.

### Lighting and hostile AI

- [ ] LIGHT-01 — Torch emits 12 and lantern emits 15 in actual game block-light data and client display; spawning rules behave appropriately in sufficiently lit areas.
- [ ] MOB-01 — During night, mobs near **block-lit terrain within 20 blocks** show significantly more time spent in/near illuminated regions than mobs near dark control region (compare with unmodified behavior or disabled goal).
- [ ] MOB-02 — A fully opaque wall does not prevent detecting light behind it; mobs remain outside and mill along the accessible perimeter, not clip/teleport through.
- [ ] MOB-03 — Mobs do not cluster exclusively at one bright coordinate; continue aimless distribution and occasional drift.
- [ ] MOB-04 — During day, the attraction goal is inactive even with torches/lanterns present; outside Overworld day/night logic likewise inactive for this milestone.
- [ ] MOB-05 — Player detection suspends entrancement and restores vanilla aggression; after loss, mob may wander/loiter again.
- [ ] MOB-06 — Normal spawn prevention in illumination remains; no “mobs spawn because attracted to light” regression.
- [ ] MOB-07 — With dozens of eligible hostile mobs, no severe TPS/pathfinding degradation attributable to per-tick huge-volume scanning; report methodology and observation.
- [ ] ZOMB-01 — On Normal difficulty while actively chasing a player, a zombie breaks each representative wooden door at Hard-mode break speed; test several wood variants and any shared tags.
- [ ] ZOMB-02 — A zombie attacks an iron/metal door in pursuit but cannot damage/destroy it.
- [ ] ZOMB-03 — Unaware zombies merely loiter outside illuminated closed doors; do not destroy them just because light is behind them.
- [ ] CREEP-01 — Direct daytime sun creates a **visible brief smoke/ash poof** with removal, no blast, block damage or death loot.
- [ ] CREEP-02 — Cave, opaque roof, nighttime, and artificial torch light do **not** trigger evaporation.
- [ ] CREEP-03 — Client particles and server entity state stay synchronized; visual evidence recorded.

**AI methodology:** For each scenario, use known player position/line-of-sight conditions, reasonably repeatable spawn locations, at least 20 mobs or repeated samples when practical, and timestamped coordinates or time-in-region data. Summarize actual observed behavior; don't substitute “goal object was installed” for testing behavior.

### Inventory and carry weight

- [ ] INV-01 — Inventory UI has **2 rows of 9** general slots and the standard **9-slot hotbar**, without leftover accessible storage.
- [ ] INV-02 — Interactions with chests, crafting table, furnace, anvil, villager, armor/offhand, and item pickup use the 27-slot capacity correctly.
- [ ] INV-03 — Sort merges appropriate stacks and orders by registry ID **bottom row left-to-right before upper row**; hotbar, offhand, armor and crafting untouched.
- [ ] INV-04 — Sort does not combine items with different durability, components, enchantments, or max stack sizes; no loss or duplication.
- [ ] INV-05 — Quick transfer to a free hotbar slot fills from **slot 1 toward slot 9**; existing stack merges still work.
- [ ] HVY-01 — 64 iron ingots carried => cannot pick up more iron/gold/copper; source remainder remains.
- [ ] HVY-02 — 32 iron + 32 gold = full; copper attempted pickup rejected.
- [ ] HVY-03 — At 63 units, block of iron/gold/copper (9 units) cannot enter; at 55 units, one block fills exactly.
- [ ] HVY-04 — Nuggets are exactly 1/9 each and no rounding bypass exists. Raw-material blocks, Silk Touch ores, and manufactured items count as specified.
- [ ] HVY-05 — Recipe transform of 9 ingots to storage block, production of pickaxe/bucket/rails/etc., and reversible crafting never creates extra carried heavy capacity.
- [ ] HVY-06 — Offhand and cursor holding count; equipped armor exempt but carried armor counts; no transfer exploit.
- [ ] HVY-07 — Ground pickup, shift-click from container/furnace, crafted outputs, ordinary clicks, save/reload, creative-to-survival transition all enforce cap without destroying items.
- [ ] HVY-08 — The heavy-load indicator agrees with authoritative actual inventory contents and remains legible in UI.
- [ ] HVY-09 — Player walking/sprinting speed is **not** penalized at capacity; unrelated stone/wood remains unrestricted.

### Hunger and UI

- [ ] FOOD-01 — Identical scripted sprint/jump/swim workloads produce approximately half ordinary exhaustion against baseline; verify each activity's actual vanilla behavior.
- [ ] FOOD-02 — Combat exhaustion and natural regeneration cost remain unchanged.
- [ ] FOOD-03 — Saturation HUD shows nonzero saturation correctly and updates after eating/exertion/healing; screenshot at a readable GUI scale.
- [ ] FOOD-04 — Hunger display and inventory UI behave correctly at common GUI scales and when hidden/not relevant.

### Final release and report

- [ ] FINAL-01 — Fresh clean build and tests pass; tests check real behavior and include negative controls where worthwhile.
- [ ] FINAL-02 — No Milestone 01 regression or accidental redstone/freight system work.
- [ ] FINAL-03 — New milestone report includes actual statistics, feature default values, artifacts, warnings, screenshot references, and PASS/FAIL/NOT RUN for all applicable rows.
- [ ] FINAL-04 — CI publishes the correct installable mod artifact, and existing Windows updater remains usable without hand-copying files.

## Suggested evidence structure

```text
docs/evidence/milestone-02/
  baseline-build.txt
  test-results.md
  rock-replacement-before-after/
  ore-sampling.csv
  ore-sampling-summary.md
  artificial-light-pathing/
  doors-and-creepers/
  inventory-and-weight/
  hunger-hud/
```

These are suggested paths only; reuse the repo's actual established evidence conventions when they already exist.
