# Milestone 02 report: World, Night, Inventory

Scope: `docs/MILESTONE_02.md` and `docs/M02_*.md`. All four phases (02A–02D) are implemented and tested.

- **Tests:** 63 required server game tests pass in `./gradlew build`: Milestone 01, 02A–02D, and vanilla's built-in one. The real-client test passes.
- **Negative controls:** each restriction phase was run against a copy with its change removed, and the restriction tests failed as expected.
- **Not covered by tests:** gameplay *feel* (how the balance plays over hours) is not established by any of this and needs your playtesting. See section 7.

Evidence lives in `docs/evidence/milestone-02/`:
- [`gametest-evidence.md`](evidence/milestone-02/gametest-evidence.md): every test's logged numbers.
- [`negative-controls.md`](evidence/milestone-02/negative-controls.md)
- [`ore-sampling/ore-sampling-summary.md`](evidence/milestone-02/ore-sampling/ore-sampling-summary.md)
- Screenshots in `inventory-and-weight/`, `mobs-and-light/` and `food-and-hud/`.

## 1. Baseline

Recorded before any Milestone 02 code, in [`baseline-build.txt`](evidence/milestone-02/baseline-build.txt):
- **Toolchain:** Minecraft 26.3 (unobfuscated, Mojang names), Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25.0.4.
- **Mod:** `simple_block` 0.1.0, with 6 mixins.
- **Baseline result:** `./gradlew clean build` passed 22/22 game tests, and the client smoke test passed.
- **CI:** `build.yml` publishes the `simple-block-mod` artifact, and `windows-updater.yml` tests the updater.

## 2. What changed, by phase

| Phase | Commit | Features | Main files |
|---|---|---|---|
| 02A | `22029f2` and earlier | GEO-01, MINE-01, ORE-01, ORE-02 | `feature/RemovedRocks`, `feature/DeepslateGate`, `worldgen/*`, `mixin/geology`, `mixin/mining`, data overrides, `tuning/Tuning` |
| 02B | `0d003d3` | INV-01..04 | `feature/InventorySlots`, `InventorySort`, `HeavyLoad`, `HeavyTransfers`, `mixin/inventory`, `network/SortInventoryPayload`, client `InventoryScreenAddons`, `simple_block/heavy_weights.json`, `tools/heavy-weights` |
| 02C | `192cae9`, `9c5a486` | LIGHT-01, MOB-01..03 | `feature/LightLevels`, `Entrancement`, `ZombieDoors`, `CreeperEvaporation`, `mixin/light`, `mixin/mobs` |
| 02D | this commit | FOOD-01 and saturation display | `feature/Exhaustion`, `SaturationSync`, `mixin/food`, `network/SaturationPayload`, client `SaturationHud` |

Tests are in `src/gametest/`:
- `GeologyGameTests`, `MiningGameTests`, `WorldgenGameTests`, `InventoryGameTests`, `HeavyLoadGameTests`, `MobGameTests` and `FoodGameTests`.
- `SurvivalSmokeClientTest` has client steps for every phase.
- `sampler/OreSampler` does the normal-world ore measurement.

Each feature has its own `mixin/<feature>` package, and is listed in `SimpleBlock`'s Javadoc and the README table.

## 3. What each feature does

- **GEO-01:**
  - Granite, diorite, andesite and tuff no longer generate. Blobs are removed and large-vein filler is dropped.
  - Structure blocks become same-shape stone-family or deepslate-family blocks; trial chambers become deepslate bricks.
  - Recipes, mason trades and chest loot that gave these rocks are removed. The blocks stay registered.
- **MINE-01:** natural deepslate and every deepslate ore can't be broken without an iron, diamond or netherite pickaxe. The server refuses the break. Crafted deepslate keeps vanilla rules.
- **ORE-01:** background iron:
  - Sparser in stone (0.70× vanilla) and richer in deepslate.
  - Much richer in deepslate under deep ocean, plus ore bodies there.
  - Banded iron formations in mountains, alluvial iron at desert/badlands/river landforms, and pockets beside natural lava.
- **ORE-02:** diamonds at 0.354× vanilla.
- **INV-01..04:**
  - 27 usable slots; the top storage row is covered.
  - A Sort button sorts the storage rows, bottom row first.
  - Shift-clicks fill the hotbar from the left.
  - One shared 64-ingot allowance for iron, gold, copper, diamond and netherite; worn armor is exempt.
    - A diamond weighs like an ingot.
    - Netherite weighs like diamond: an ingot 1, netherite gear what its diamond version weighs.
  - The inventory shows a load bar without text, filled in one colour per material (iron, copper, gold, diamond, netherite), with a red frame when full. Hovering it shows the total out of 64 and each material's exact weight.
- **LIGHT-01:** torches (wall and copper too) emit 12. Lanterns stay 15.
- **MOB-01:**
  - At Overworld night, hostile ground mobs with a wander goal steer their wander decisions towards lit ground (block light > 0) within 20 blocks.
  - They spread over it, mill about and sometimes lose interest.
  - Light behind walls makes them loiter outside.
  - Chasing a player comes first.
- **MOB-02:**
  - Zombies, husks and zombie villagers break wooden doors only while chasing a player, at vanilla's Hard pace (240 ticks), on every difficulty except Peaceful.
  - Iron and copper doors get banged on but are never damaged.
  - Vanilla's passive door breaking is off.
- **MOB-03:** a creeper in direct sun stops, smokes and puffs away in 0.5 s. No explosion, no damage, no drops.
- **FOOD-01:**
  - Sprinting, jumping, sprint-jumping, swimming, moving under or on water, and breaking blocks cost 0.5× vanilla exhaustion.
  - Attacking, taking damage, both kinds of regeneration and the Hunger effect are unchanged.
  - Saturation is shown on the food bar, on by default: the icons it covers are drawn in gold and underlined.
  - The server sends the exact value whenever it changes. Vanilla only sends it with health or food changes.

All rules are enforced on the logical server; the client only displays.

## 4. Test matrix

Methods:
- **GT:** server game test in `./gradlew build`; the class/test name is given.
- **Client:** `runClientGameTest`, the real client under Xvfb with software Vulkan.
- **Sampler:** `OreSampler` on 8 normal-world seeds against a vanilla-worldgen control.
- **Dedicated:** `runServer` start/stop.

| Row | Result | Method and evidence | Limitations |
|---|---|---|---|
| BASE-01 | PASS | Inspection recorded in `baseline-build.txt` | |
| BASE-02 | PASS | Baseline `clean build`: 22/22, client smoke passed | |
| BASE-03 | PASS | M01 tests (sleep, storm, phantoms, elytra, shulker, audits, regression) all pass in the 63-test build; client bed/shulker/movement/elytra steps pass | |
| BASE-04 | PASS | Client: fresh survival world, 16 screenshots across `inventory-and-weight/`, `mobs-and-light/`, `food-and-hud/` | Software renderer, not a desktop GPU |
| BASE-05 | PASS | CI Build runs #4, #5, #6, #8 and #9 published `simple-block-mod` with `build-info.json`; mod id, artifact name and workflow file unchanged | #7 failed on a test-timing flake, fixed in #8 (below) |
| BASE-06 | PASS | Every rule is tested on the game-test server, a dedicated-style server with no client classes. Dedicated: `runServer` with the final code starts (49 tuning defaults loaded, "Done (3.3 s)") and stops cleanly | Remote client joining a dedicated server: NOT RUN |
| GEO-01 | PASS | Sampler: removed rock 0 per seed in the mod vs about 1.2 M per seed in the control (8 seeds) | |
| GEO-02 | PASS | Sampler: y 0–8 deepslate share follows vanilla's own 1/8 steps, removed-rock share 0 at every y; `ordinary-terrain-slice.png`; GT `worldgenSubstitutesKeepShapeAndFollowDepth` | |
| GEO-03 | PASS | Sampler: iron and copper veins keep all ore and raw blocks (filler becomes stone/deepslate), trial chambers convert 1:1 to deepslate family with every spawner and vault | Caves are judged from slices, not counted |
| GEO-04 | PASS | GT `noRecipeMakesRemovedRocks`, `chestLootNeverGivesRemovedRocks`, `noVillagerTradeInvolvesRemovedRocks`; blocks stay obtainable by command/creative (registered) | |
| MINE-01 | PASS | GT `deepslateAndDeepslateOresNeedIronPickaxe`, `serverRejectsBreakingDeepslateWithStonePickaxe` | |
| MINE-02 | PASS | GT same, plus `stoneOresKeepVanillaTiers` | |
| MINE-03 | PASS | GT `craftedDeepslateKeepsVanillaRequirements` (19 blocks incl. slabs, stairs, walls) | |
| ORE-01 | PASS | Sampler: ordinary stone 240, deepslate 390 per 100k rock; deepslate/stone 1.43–1.82 per seed | |
| ORE-02 | PASS | Sampler: deep-ocean deepslate 870–1017 vs ordinary deepslate 350–442 (×1.97–2.65 per seed) | |
| ORE-03 | PASS | Sampler: mountain bands, placer, lava-adjacent deposit counts with example coordinates per seed; slice/map images | Ocean-floor *stone* iron is 0.45–0.5× vanilla (decision 8) |
| ORE-04 | PASS | Sampler: diamonds 0.354× vanilla pooled (0.328–0.382 per seed, ±0.006 at 95%); exposure unchanged (1.5%) | |
| LIGHT-01 | PASS | GT `torchAndLanternLight`: emission and block light 12/11/15, spawn check 0/50 in torchlight vs 50/50 in a dark box; Client: client light data torch 12, lantern 15 | |
| MOB-01 | PASS | GT `zombiesDriftToLitGroundAtNight`: 24 zombies, 1200-tick phases. Over 9 runs, lit/dark presence was 64–89% / 0–19% with the attraction vs 24–39% / 19–40% without (control) | Statistical test; see stability notes in the evidence |
| MOB-02 | PASS | GT `lightBehindWallsStillAttracts`: 32 zombies. Round lit box 40–58% vs dark box 4–8%; nobody inside | Light shut in a box is found more slowly than light on open ground |
| MOB-03 | PASS | GT open-ground test: at the end the zombies on lit ground stand on almost as many different blocks as there are zombies (e.g. 23 on 23, 18 on 17) | |
| MOB-04 | PASS | GT `noAttractionInDaytime`: husks at noon spend 21–36% of the time on lit ground vs 20–39% on the dark mirror (no pull) | Nether/End are excluded by code (Overworld clock only), not by a test |
| MOB-05 | PASS | GT `pursuitOverridesThenAttractionResumes`: zombies leave the light to chase a survival player, drop them when creative, then spend 83–100% of the time back on lit ground | Passes on vanilla too (74%), because it checks that combat still wins |
| MOB-06 | PASS | GT `torchAndLanternLight` spawn checks; attraction never touches spawning | |
| MOB-07 | PASS | GT `attractionIsCheap`: 40 zombies, 400-tick windows off then on, 0.9–2.3 ms per tick; with it on never slower than off (7 runs); 39–119 µs per attraction decision | Game-test server, not a loaded multiplayer server |
| ZOMB-01 | PASS | GT `pursuingZombieBreaksOakDoor`, `…CrimsonDoor` on Normal: door shut in the zombie's face, broken in 239 ticks (vanilla Hard 240) | Two wood types tested; all `#minecraft:wooden_doors` share the code path |
| ZOMB-02 | PASS | GT `metalDoorsAreAttackedButNotDamaged`: iron and copper doors attacked 202–300 of 300 ticks, all blocks unchanged | |
| ZOMB-03 | PASS | GT `unawareZombiesLeaveDoorsAlone`: 4 zombies, lit room behind an oak door, 900 night ticks, door intact, break goal never ran | |
| CREEP-01 | PASS | GT `creeperDissolvesInSunlight`: 9-tick effect (0.45 s), no drops, floor intact; Client screenshots `creeper-smoking-in-sunlight.png`, `creeper-gone.png` | |
| CREEP-02 | PASS | GT `creepersInShadeOrTorchlightStay` (roofed pen, torch-lit closed room), `creepersAtNightStay` | |
| CREEP-03 | PASS | Client: creeper gone on server and client, no drops, player unharmed; screenshots | |
| INV-01 | PASS | GT `menusOfferExactly27Slots`, `disabledRowRefusesClicks`; Client: 27 slots on screen (`inventory-load-bar-mixed.png`, `food-and-hud/inventory-gui-scale-3.png`) | |
| INV-02 | PASS | GT chest, furnace, player inventory, crafting table, pickups, command-placed items | Anvil and villager trading screens: NOT RUN (same slot code, not exercised) |
| INV-03 | PASS | GT `sortMergesAndOrdersBottomRowFirst`; Client: Sort through the real packet | |
| INV-04 | PASS | GT sort keeps named and damaged items apart, no loss or duplication | |
| INV-05 | PASS | GT `shiftClickFillsHotbarFromTheLeft` | |
| HVY-01..06 | PASS | GT `fullLoadRefusesAnyMoreMetal`, `diamondsAndNetheriteCountAndMaterialsAddUp`, `exactIntegerAccounting`, `weightTableMatchesTheDesign`, `noRecipeCreatesCarryingCapacity`, `craftingAtTheCapIsWeightNeutral`, `offhandCursorCountWornArmorDoesNot` | |
| HVY-07 | PASS | GT `transfersLeaveTheRemainderInTheSource`, `bundlesCreativeAndModeChanges` | Save/reload and log out/in: NOT RUN (the load is recomputed from the inventory each time; nothing is stored) |
| HVY-08 | PASS | Client: the bar's per-material amounts equal the server's (iron, gold, copper, a diamond pickaxe, netherite), and the frame is red at 64; hovering with the real mouse shows the tooltip with the same numbers (6.44 gold with 4 nuggets), and it follows the inventory while hovering (`inventory-load-bar-mixed.png`, `inventory-load-bar-tooltip.png`, `inventory-heavy-full.png`) | |
| HVY-09 | PASS | GT `noSpeedPenaltyAtTheCap` | |
| FOOD-01 | PASS | GT `activityExhaustionIsHalved`: sprint, jump, sprint-jump, swim, under water, mining all ×0.50 of vanilla; Client: a real 11-block sprint cost 0.049 per block (vanilla 0.1) | |
| FOOD-02 | PASS | GT `combatRegenerationAndHungerEffectUnchanged`: attack, hit taken, both regeneration paths, Hunger effect all ×1.00 | |
| FOOD-03 | PASS | Client: client saturation equals the server after eating (12.8), exertion (11.8) and regeneration (8.8); `hud-saturation-after-eating.png` | |
| FOOD-04 | PASS | Client: GUI scales 1–3 (`hud-saturation-gui-scale-*.png`), inventory at scale 3, F1 hides everything, creative shows no food bar or overlay | Scale 4 and larger windows not shot |
| FINAL-01 | PASS | `./gradlew clean build`: 63/63; negative controls for 02A–02D in `negative-controls.md` | |
| FINAL-02 | PASS | M01 tests all pass; no redstone, minecart, cable, skip, locomotive, freight or block-art work | |
| FINAL-03 | PASS | This report | |
| FINAL-04 | PASS | See BASE-05. CI Build run #9 (commit `45fa838`, all of 02D and this report) passed and published `simple-block-mod`; the updater (`tools/windows`) is unchanged and installs it | |

Client worldgen was not checked visually in a normal world; the client test world is superflat. Worldgen was measured with the sampler on normal worlds on a dedicated server instead.

## 5. Configuration

Players override keys in `config/simple_block/tuning.json` (overrides only). `config/simple_block/tuning-defaults.json` is rewritten at every start with all 49 keys and their defaults. Worldgen keys only affect chunks generated after a restart.

| Keys | Defaults |
|---|---|
| `ore.diamond.keep_chance` | 0.35 |
| `ore.iron.stone.{veins_per_64_layers,min_y,max_y,size}` | 7.5, 0, 320, 7 |
| `ore.iron.deepslate.{veins_per_64_layers,min_y,max_y,size}` | 8.0, -64, 4, 9 |
| `ore.iron.deep_ocean.{veins_per_64_layers,min_y,max_y,size}` | 8.0, -64, 4, 12 |
| `ore.iron.deep_ocean_body.{chance_per_chunk,min_y,max_y,veins,spread,size}` | 0.08, -60, -8, 6, 6, 16 |
| `ore.iron.banded.{chance_per_chunk,min_surface_y,min_depth,max_depth,half_length,half_width,layers,layer_spacing,max_dip,density}` | 0.12, 90, 4, 36, 16, 8, 3, 3, 0.25, 0.6 |
| `ore.iron.placer.{attempts_per_chunk,water_search_radius,depression_radius,depression_depth,radius,max_depth,density}` | 2.0, 5, 8, 3, 4, 4, 0.45 |
| `ore.iron.geothermal.{attempts_per_chunk,min_y,max_y,search_radius,selected_chance,radius,density}` | 3.0, -58, 40, 6, 0.5, 3, 0.45 |
| `mob.entrancement.{enabled,samples,memory,lose_interest_chance,step}` | true, 24, 8, 0.15, 10 |
| `mob.creeper.evaporation_ticks` | 10 |

`mob.entrancement.enabled` exists for the negative-control tests and is not meant as a gameplay switch.

These values are locked by the design, so they are constants in their features rather than keys:
- torch 12 and lantern 15;
- 20-block attraction radius;
- 27 slots;
- 64-ingot cap;
- 240-tick door break;
- 0.5× activity exhaustion.

## 6. Decisions that need you

1. **Heavy accounting unit:** 1/216 ingot (rails 3/8, hanging signs 11/27) instead of ninths. The capacity is the same 64.
2. **What counts as light for MOB-01:** any spawn-preventing block light attracts, including lava and glow lichen.
3. **Crafted rock in structures** keeps one palette per structure: tuff → deepslate family, granite/diorite/andesite → stone family. Raw rock follows depth.
4. **Copper torch:** gets 12, like the torch.
5. **Deepslate without an iron pickaxe** can't be broken at all, rather than breaking with no drops. `infested_deepslate` is not gated.
6. **Mason level-3 trades** shrink to the dripstone trade only.
7. **Diamonds and netherite** (your decision after 02D):
   - A diamond weighs like an ingot: diamond ore 1, a block 9, gear by the diamonds it uses.
   - Jukeboxes (1) and enchanting tables (2) count their diamonds.
   - A netherite ingot weighs 1 and a block 9; netherite gear weighs what its diamond version does.
   - Netherite scrap and ancient debris weigh nothing.
   - Still open: loot-only diamond horse armor and nautilus armor can't be crafted or smelted, and weigh one diamond.
   - Remaining `heavy_weights.json` notes:
   - Loot-only gear weighs 1/9.
   - Golden food counts its gold, maps count their compass, and statues count a copper block.
   - Silk-touched ore weighs 1, but Fortune can turn that into more.
8. **Ocean-floor stone iron** is 0.45–0.5× vanilla, because vanilla concentrates iron around y 16. The ordinary-stone target of 0.70× is met.
9. **Copper doors** count as metal: attacked, never broken. Vanilla's own "wooden door" check would include them, because they open by hand.
10. **Zombie chase memory:** a zombie counts as chasing for 300 ticks (15 s) after it last had a player target, so a door shut in its face still gets broken; vanilla forgets after about 3 s.
    - A zombie that loses sight far from the door forgets as in vanilla.
    - Doors can be opened on a path only during a chase.
11. **Attraction exclusions:**
    - Flying and water-bound mobs (the Wither, guardians) are excluded, because land destinations would break their movement.
    - Mobs without a wander goal are unaffected: slimes, phantoms, ghasts, wardens, piglins and others.
    - Drowned are attracted. The full list is in `Entrancement`'s Javadoc.
12. **Rain protects creepers** from evaporating, exactly as it protects zombies from burning.
13. **Exhaustion factor as a constant:** `M02_HUNGER_HUD.md` asks for a config key, while `M02_DECISIONS_AND_TUNING.md` locks 50% for the first balance pass. It is a constant; making it a key is one line.
14. **Saturation display look:** gold-tinted vanilla food icons plus a gold underline, drawn without new art. Saturation only shows when the mod is on both client and server; the server sends it only to clients that have the mod.

## 7. Known gaps and unverified feel

- **Not run:**
  - A remote client on a dedicated server.
  - A desktop-GPU client.
  - Anvil and villager screens with 27 slots.
  - Heavy load across save/reload.
  - Client-side visual check of normal-world terrain.
- **Feel to judge by playing:**
  - How strongly night mobs gather at bases. On open ground, 24 zombies 10–20 blocks from a lit area spend about 70–90% of their time on lit ground.
  - Whether the 15% lose-interest chance and the 10-block wander step feel right.
  - The heavy cap in real mining trips.
  - Hunger pace at 0.5×.
  - Ore abundance in real exploration.
- **Creeper while smoking:** the creeper's whole tick is skipped for the 0.5 s it smokes, so it does not fall or get pushed during the effect.
- **Light found slowly in closed buildings:** light shut inside a building with no light leaking out is found by the per-decision sampling more slowly than light spilling onto open ground. Typical houses with doors and windows leak light.

## 8. Build, run and update

| Command | What it does |
|---|---|
| `./gradlew build` | Jar plus all server game tests (fresh superflat world each run) |
| `./gradlew runClient` | Play in a dev client |
| `./gradlew runServer` | Dedicated server |
| `XDG_RUNTIME_DIR=/tmp/xdg xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` | Real-client test on a headless machine (needs `mesa-vulkan-drivers`) |
| `./gradlew runOreSampler -PoreSamplerLabel=<label>` | Worldgen measurement (see the ore-sampling summary for the control worktree) |

- **Filtering game tests:** run a subset with `JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dfabric-api.gametest.filter=simple_block-gametest:mob_game_tests_*" ./gradlew runGameTest`.
- **Installing for playtesting:** every push to `main` runs `.github/workflows/build.yml`, which publishes `simple-block-mod` (the jar plus `build-info.json`). On Windows, run **Update Simple Block** (`tools/windows/Update-SimpleBlock.bat`, setup in `docs/PLAYTESTING_SETUP.md`), then start Minecraft with the Fabric profile and **create a new world**.

## 9. Next

Playtest Milestone 02 in a new world and settle the decisions above. The next milestone should come from `docs/DESIGN_REGISTER.md` (mobility and freight, mechanical systems) once you authorise it. None of it was started here.
