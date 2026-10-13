# Milestone 06 report: Freight station, coal winch, cable simplification and clean-up

Brief: [MILESTONE_06.md](MILESTONE_06.md). Choices where the brief left room: [DECISIONS.md §5.5.1](DECISIONS.md). Evidence: [`docs/evidence/milestone-06/`](evidence/milestone-06/).

**Summary.** Everything in the brief's work packages is implemented and tested on the server (129/129 game tests), in the real client by key presses (five client tests, two new), and over a real connection to a dedicated server. The long-route problem (§7) is solved with level-saved cable lines that are driven wherever the cart is loaded, without force-loading. One limitation is recorded: the winch's controls are on the drum, so starting or stopping a haul needs the drum loaded. No human playtest has been done. Skips, transfers, the smelter, armor and the cargo-fill visuals are not touched. M07's engine remodel is not part of this milestone.

| | |
|---|---|
| Implementation commits on `main` | [`c38973c`](https://github.com/boxofdog/simple_block/commit/c38973c) cable overhaul, level-saved lines, coal winch core · [`e031463`](https://github.com/boxofdog/simple_block/commit/e031463) Loading Track and winch tests · [`85ad4fc`](https://github.com/boxofdog/simple_block/commit/85ad4fc) redstone rails, creative menu, no shoving · [`6114a11`](https://github.com/boxofdog/simple_block/commit/6114a11) client proof, docs index, test isolation · merged with the owner's docs as [`2069ba5`](https://github.com/boxofdog/simple_block/commit/2069ba550c9f7f0ae881f711a7c515ea42c329fb) · [`e042fe9`](https://github.com/boxofdog/simple_block/commit/e042fe96ec5eb6d6c2f1e1969767bf3c15ada7cc) long-line test isolation and tick budget (the commit CI verified) |
| CI | [Build #18](https://github.com/boxofdog/simple_block/actions/runs/38358146896) on `2069ba5`: failed on the pre-existing M02 flake `metalDoorsAreAttackedButNotDamaged` (section 6). [Build #19](https://github.com/boxofdog/simple_block/actions/runs/38359763597) on `e042fe9`: **success**, `simple-block-mod` artifact published (340,861 bytes) |
| Server game tests | `./gradlew build`: **129/129** (M05 had 114; +15 new, M04/M05 tests adapted) |
| Client game tests | `./gradlew runClientGameTest`: all 5 pass (`SurvivalSmokeClientTest`, `CableClientTest`, `HaulageClientTest`, new `FreightStationClientTest`, new `DedicatedServerClientTest`) |
| Dedicated server | `runServer` reaches "Done" with M06 ("Cable: one coil per drum, spans of up to 12 blocks"); `DedicatedServerClientTest` runs a dedicated server and connects the real client to it over the network |

## 1. Baseline

- Started from `main` at `6b6a332`: M05 code, [CI Build #17](https://github.com/boxofdog/simple_block/actions/runs/38306694723) green with 114 server game tests. Toolchain unchanged: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25.
- The existing suite was kept green at every checkpoint (114, then 116, 125 and 129 tests as M06 tests were added). The M01–M05 client tests were re-run against the M06 code before the new client tests were added: all pass.
- The owner's docs commits made during the work (M07 brief, M05 acceptance, M07 authorization) were merged into `main` unchanged. M05's report and acceptance receipt were not edited.

## 2. Reading of the brief, and choices

All numbers are starting values in `tuning/Tuning.java` (section 7). The owner-settled decisions are implemented as written; these are the choices left to Claude:

- **Cable recipes in the middle column.** Vanilla's shaped matcher trims empty columns, so `" N "` would also match the left or right column. A small mixin on the crafting table, the crafter and the crafter's preview refuses the cable and coil recipes unless the pattern is in the middle column (`cable/CenterColumnRecipes`, `mixin/cable/*`).
- **One coil, any length.** A drum holds one coil or none. Routing, extending and closing a loop take nothing from it, and taking it down gives back exactly that coil (and the wheel's anchor). The 12-block span limit, the clear-path checks and M05's straight-loop rule are unchanged. A coil's heavy weight is recomputed from the recipe as 1/2 ingot. Pre-M06 worlds are not migrated: new worlds only.
- **Long lines (§7).** See section 4.
- **Coal winch.** The lever replaces the crank and uses the same hit zone; empty-hand use elsewhere on the drum inspects. Its top speed is 0.15 blocks/tick (the crank does 0.06). The speed changes by at most 0.01 a tick, so a direct PULL→FEED slows, stops and reverses over about 30 ticks. One coal gives 1,600 ticks of actual hauling, and the winch holds up to 64 coal.
  - **Burning:** a coal is lit only when a load starts to move. Burn time counts down only on ticks when a gripped cart actually got on along the strand, by at least a quarter of the commanded distance, so a blocked, stalled or idle winch burns nothing.
  - **Stops that put the lever back to NEUTRAL:** docked, derailed, end of track, end of line, edge of the loaded world, blocked (10 ticks without progress), idle (40 ticks with no cart gripping) and out of coal. The lever won't leave NEUTRAL on an open route.
  - **Saving:** the lever always loads at NEUTRAL. Coal and the burn time left are saved exactly.
  - **Breaking the drum** gives back the furnace and the unlit coal. The coal already alight is lost.
- **Loading Track.** A straight, level rail that never re-shapes to meet its neighbours, facing the way the player faced when placing it. A station post stands on the track's left and reaches the top of the block, because the docked cart's hitbox covers the rail; the post is what a player can click. The outline includes the post, but there is no collision, so carts pass freely.
  - **Docking:** a freight cart (a plain minecart without a cushion: cargo, empty or with a rod) that enters the block is centred, stopped and docked, with its front turned to the track's facing. Engines and cushioned carts pass over.
  - **Holding:** the whole train is held at the end of every tick, whatever drives it. Engines in it are switched off, refuse to start and burn nothing, and a hauling cable stops.
  - **Release:** an empty-hand use of the track releases every car of the train. No car docks again at the track it was released from until it has left that track (or is 16 blocks away), so a released train passes over its station once.
  - **Saving:** the dock, the release and the front are saved with the cart.
- **The cart's front and gate.** Each freight cart carries a synced front direction. Docking sets it, and it stays on the same physical end of the cart through curves and reversals (it turns with each curve rail entered). The closed folding gate is drawn on that end wall. The open (ramp) model exists for M07 and is unused.
- **Redstone rails.** Recipes and recipe-book advancements are disabled, and the rails are removed from all non-block loot. Abandoned mineshaft chests are the only vanilla source (base and trade-rebalance tables); no vanilla structure places these rails (checked across all structure NBT). Placed rails still work.
- **Creative menu.** One filter (`feature/RetiredItems`) replaces the ender chest's and chest minecart's own tab filters and also covers search. The audit list is in section 6, RET-01. No other retired item was found.
- **No shoving.** A player walking into a minecart that can't carry a passenger doesn't move it. The cart's half of the push when such a cart runs into a player is also skipped. Cart–cart pushing, mobs, engines, cables, couplings and gravity are unchanged.

## 3. What changed

| Area | Files |
|---|---|
| Level-saved lines and the drive | `cable/CableLines` (new), `cable/CableDrive`, `cable/CableIndex`, `cable/CableDrumBlockEntity`, `cable/CableAnchorBlockEntity`, `cable/CableAnchorBlock`, `cable/CableRoutes`, `cable/CableLoops`, `cable/CableHook`, `train/Couplings` (`allTicking`) |
| Recipes | `data/simple_block/recipe/cable*.json`, `cable/CenterColumnRecipes`, `mixin/cable/{CraftingMenuMixin,CrafterMixin,CrafterMenuMixin}`, `simple_block/heavy_weights.json` |
| Coal winch | `cable/CoalWinch` (new), `cable/CableDrumBlock`, `cable/CableDrumPartBlock` (furnace, lever parts), client `cable/CableDrumRenderer`, `tuning/Tuning` (`winch.*`; `cable.drum_capacity` removed) |
| Loading Track and freight front | `train/LoadingTrackBlock`, `train/LoadingTracks`, `train/FreightGatePartBlock` (new), `train/TrainContent`, `train/Couplings` (docked trains held), `train/CobblestoneEngine` (switched off when docked), `mixin/train/AbstractMinecartMixin` (dock, release, synced front), client `CargoMinecartRenderer` and `mixin/AbstractMinecartRendererMixin` (gate); data: recipe, advancement, loot table, `#minecraft:rails`, `#minecraft:mineable/pickaxe` |
| Retired items and rails | `feature/RetiredItems` (new), `feature/EnderChests`, `feature/ChestMinecarts` (tab filters moved), `data/minecraft/recipe/{powered,detector,activator}_rail.json`, matching advancement overrides |
| No shoving | `feature/CartPushing` (new), `mixin/pushing/{AbstractMinecartMixin,EntityMixin}` |
| Lang | `assets/simple_block/lang/en_us.json`: winch, station and new cable messages |
| Tests | New: `RecipeGameTests`, `LongLineGameTests`, `LoadingTrackGameTests`, `CoalWinchGameTests`, `RetiredItemsGameTests`, client `FreightStationClientTest`, `DedicatedServerClientTest`; test environment `long_line.json`. Adapted: `CableGameTests`, `CableLoopGameTests` (one coil), `CableGameTests` CAB-15/16 and `MinecartCargoGameTests` T03 (test-only propulsion instead of powered rails), `TrainGameTests` ENG-01 (counts its own structure only), `CableClientTest`, `HaulageClientTest` |
| Docs | README (M06 table), `SimpleBlock` Javadoc, `art/blockbench/README.md`, DECISIONS §5.5.1, this report |

## 4. Long routes and unloaded chunks (§7)

**Problem.** In M05 the drum owned the route and the moving state. `CableLoops.ticking` refused any motion unless the drum and every anchor were entity-ticking. On a long line, one far anchor in an unloaded chunk stopped everything.

**Solution.**
1. **Lines are level data.** Each drum's line (anchors, strand points, loop, the drive's lever, coal and burn time, any pending cut) is kept in `simple_block:cable_lines` saved data. The drum's block entity still owns the route and copies its geometry into the line whenever it changes.
2. **The grip index is built from the lines,** filed by chunk. A rod grips a strand whose drum is unloaded, and a lookup only looks at nearby spans however long the lines are.
3. **The drive runs every level tick for every line,** with no drum tick needed.
4. **Carts move only in their own entity ticks.** A gripped cart's train is driven only while every car, and the track just ahead, is entity-ticking. Otherwise it is held and the winch stops (`unloaded`, lever to NEUTRAL). Nothing loads chunks, moves an unloaded entity, or simulates one.
5. **An anchor broken while its drum is unloaded** cuts the saved line at once: the strand ends at the previous support, a loop opens, and the wheel's anchor drops. The drum applies the cut to its route when it next loads.
6. **Anchors reconcile** against the saved line on load and every 40 ticks, so a far anchor that missed a dismantle frees itself when it loads.

**Shown by:**
- **LONG-01..05 (server):** a 387-block, 39-anchor line on one coil. With the drum 200 blocks away and the far end not ticking, a FEED haul moves the cart in its loaded section and stops at the end of its rails. A second cart is hauled up to the edge of the ticking world and stops there (`unloaded`). No coal burns while stopped. Dismantling from afar gives 1 coil, 0 cable and the wheel's anchor, and far anchors reconcile.
- **Real client (`FreightStationClientTest`):** a 390-block line routed anchor by anchor with natural chunk loading (simulation distance 5). The cart is hauled to the edge of the entity-ticking world (stopped at x 94.86, edge at x 96, `unloaded`). The player goes far away so the drum unloads, then comes back. The world is saved and reopened. Line, cart position, coal and partly burnt coal (1,025 ticks left) are exactly as before, the lever is at NEUTRAL, and PULL hauls the cart back.
- **Negative control:** restoring the M05 rule (drum and every anchor entity-ticking) makes LONG-01 fail: "fed east with the drum unloaded: x 8.5 -> 8.5".

**Limitation (recorded, not hidden).** The lever and the hand crank are on the drum, so a player must be near the drum to start, reverse or stop a haul. A haul runs as far as the loaded world around the train reaches and stops at its edge. A train never moves through unloaded chunks: it waits at the boundary with the lever at NEUTRAL. A remote control would be the obvious way round this, but the brief excludes remote run commands. Two smaller points:
- A line whose cart sits in an unloaded chunk while the lever is set stops after 40 idle ticks.
- The test environment's stand-in players don't load chunks, so the server test forces its section of track. The real-client test uses natural chunk loading.

## 5. Models and textures (for painting)

The new editable sources are in `art/blockbench/`, with UV-template textures (flat colours, not artwork), all made by `tools/models/generate_models.py`. Existing files and owner art were not overwritten.

- `cable_drum_furnace`, `cable_drum_lever` share `textures/block/cable_winch.png`. The lever tilts 35° about the axle: back for PULL, forward for FEED.
- `loading_track` uses `textures/block/loading_track.png`: rails on sleepers, the post and a pointer arm showing the front.
- `freight_gate_closed`, `freight_gate_open` share `textures/block/freight_gate.png`.

The fixed points the code relies on are listed in `art/blockbench/README.md`. Screenshots are in `docs/evidence/milestone-06/screenshots/`.

## 6. Test matrix

Server tests run with `./gradlew build`. Client tests run with `./gradlew runClientGameTest` under Xvfb, with Mesa Vulkan. Full evidence is in `docs/evidence/milestone-06/server-game-tests.txt`, `client-game-tests.txt` and `negative-controls-and-runs.md`.

| ID | What | Result | Evidence (abridged) | Negative control |
|---|---|---|---|---|
| REC-01..04 | Cable recipes in a real crafting-table menu | PASS | middle-column nuggets → 2 cable; left/right column, old nuggets+string, middle+string → nothing; 3 cable in the middle column → coil; left column, top row, old 8-ring → nothing; coil → 3 cable | Middle-column mixins removed: FAIL "3 nuggets, left column … made 2 cable" |
| CAB-01 | All recipes craft (M06 cable recipes) | PASS | cable ×2, coil, coil → 3 cable, drum, anchor ×2, rod | — |
| CAB / LOOP (adapted) | M04/M05 cable behaviour on one coil | PASS | LOOP-01/02 loop closed with one coil, no extra cable; LOOP-09 saved; LOOP-12 broken drum → 1 coil; CAB-13 cut on load keeps the coil | — |
| LONG-01..05 | One coil, 387 blocks, drum and far end unloaded; haul, edge stop, no idle burn, remote dismantle | PASS | "39 anchors, 387 blocks … cart x 8.50 → 28.45, NEUTRAL (track_end) after 143 ticks, burnt 140 … edge at x 54: x 40.50 → 52.95, NEUTRAL (unloaded) … dismantled: 1 coil, 0 cable, the wheel's anchor" | (a) M05 all-anchors-ticking rule: FAIL "x 8.5 → 8.5". (b) Former 128-block drum restored: FAIL "anchors routed on one coil: was 13 of 39" |
| LT-01 | Loading Track recipe (rail + anchor anywhere; powered rail refused) | PASS | 1 loading_track each way; powered rail + anchor: nothing | — |
| LT-02 | Docks freight carts mid-track, front the track's way from either direction; cushioned cart and engine pass; dock/front saved | PASS | both docked at x 15.5, front EAST; cushioned at 20.1 and engine at 19.1 passed | Docking disabled: FAIL |
| LT-03 | Engine + 2 freight carts: whole train held, engine off, won't start, no fuel; release moves nothing; restart passes the track; the next arrival docks | PASS | "40 ticks docked: all three held, engine refused to start, no fuel burnt; released: nothing moved; restarted: passed without docking; coming back: the rear freight car docked" | Docking disabled: FAIL |
| LT-04 | Track broken → train freed; the front keeps to its end round a curve | PASS | front WEST at station; trailing east, round the curve south: NORTH | Docking disabled: FAIL |
| WIN-01 | Furnace on a routed drum keeps route and coil; open route refuses; lever NEUTRAL→PULL→FEED→NEUTRAL; coal one per click, charcoal refused; empty-hand inspect leaves the lever; crank left-click inert; broken → furnace, coal, coil | PASS | lever [NEUTRAL, PULL, FEED, NEUTRAL]; broken: 1 furnace, 2 coal, 1 coil | — |
| WIN-02 | PULL, then straight to FEED: smooth reversal; runs to the end; NEUTRAL by itself; faster than the crank; burns exactly while moving | PASS | speed change ≤ 0.010/tick; top 0.150 vs crank 0.060; burnt 144 ticks for 144 ticks moving; 60 ticks later burn unchanged | — |
| WIN-03 | Hauled onto a Loading Track: docked, NEUTRAL, no burn; release starts nothing; PULL leaves without relock; rails ending → NEUTRAL (track_end) on the rails | PASS | docked at 17.50 (docked); 80 ticks no movement or burn; left to 12.85; stopped at 12.70 on the rails | — |
| WIN-04 | Hauled into an obstacle: NEUTRAL (blocked), no coal burnt while pressed against it | PASS | stopped 0.98 behind, NEUTRAL after 10 ticks, 0 ticks burnt | Burn whenever the lever runs: FAIL "burnt 9 ticks while blocked" |
| WIN-05 | Partly burnt coal saved exactly (level lines codec and drum); lever loads NEUTRAL | PASS | 2 coal + 1,565 ticks → 2 coal + 1,565 ticks, NEUTRAL | — |
| RET-01 | No retired item in any creative tab or the search; IDs registered; rails, loading track, furnace, minecart, chest, blast furnace, gold/chainmail armor still there | PASS | 4,059 tab entries + search; 59 retired items absent: elytra, 17 shulker boxes, ender chest, chest minecart, furnace minecart, powered/detector/activator rail, 35 granite/diorite/andesite/tuff items | `RetiredItems` off: FAIL (granite etc. in Building Blocks) |
| RET-02 | Redstone rail recipes gone; rail recipe stays | PASS | powered rail pattern: nothing; rail pattern: 16 rail | Rail recipe overrides removed: FAIL |
| RET-03 | Abandoned mineshaft chests give no redstone rails, still rails | PASS | 500 chests: 595 rail stacks, 0 redstone rail stacks | `RetiredItems` off: FAIL (463 stacks) |
| PUSH-01 | Player shove (both push paths): freight cart (empty/loaded) and engine don't move, cushioned cart does; gravity still rolls a freight cart | PASS | freight 0.000, engine 0.000, cushioned 7.299 blocks; rolled down a slope 22.5 → 26.5 | Pushing mixins removed: FAIL "freight cart shoved by 7.16" |
| T03, CAB-15/16 | Fixtures that used powered rails, now with test-only propulsion | PASS | loop: loaded and empty 1 lap each, cargo kept; rod cart in step with the control cart | — |
| Client: winch | Real keys: furnace fitted, 3 coal, inspect message, lever cycle seen by the client | PASS | "Coal winch: 3 coal, 0 s alight, lever neutral"; [PULL/PULL, FEED/FEED, NEUTRAL/NEUTRAL] | — |
| Client: station | Player places a track facing east; winch FEED docks the rod cart; lever back to NEUTRAL on server and client; client sees the front; post releases; nothing restarts | PASS | docked after 110 ticks at x 40.5, NEUTRAL (docked), client front east; "Released: start the engine or the winch to move off" | — |
| Client: train | Engine and 2 carts coupled by chain and started by key: docked, held 40 ticks, engine off; post releases, nothing moves; restarted, passes over | PASS | cars held at 40.98/42.58/44.5; after restart 45.45/47.05/48.65, none docked | — |
| Client: shoving | Real walking: into a freight cart / a cushioned cart | PASS | freight 30.50 → 30.50; cushioned 30.50 → 40.58 | — |
| Client: creative | Real creative screen, search tab: "rail", "track", 12 retired-item terms; every tab | PASS | 'rail': [rail]; 'track': [loading_track]; retired found: none; in tabs: none | — |
| Client: long line | §7 above, with unload, save and reopen | PASS | stopped at x 94.86 (edge x 96); after reopen identical, burn 1,025, NEUTRAL; PULL 100 ticks: 94.86 → 81.21 | — |
| Dedicated server | Real client connected to a dedicated server: place track, cart docks, client sees front, walking into it moves nothing, post releases | PASS | docked at x 40.50, client front east; walked 30 ticks: 40.50; released | — |
| M01–M05 client tests | Unchanged suites against M06 code | PASS | all client evidence lines as before (bed, shulker, elytra, inventory, cargo, cable, loop, crank, engine, unload, reopen) | — |
| Human playtest, desktop GPU, two simultaneous players | — | NOT RUN | — | — |

**Flakiness found and fixed.** Repeated full builds turned up these failures:
- **LONG-01 timed out waiting for its section to tick.** The test server ticks as fast as it can, while chunks load on their own clock: about 1,300 ticks of waiting can pass in a second. The test now has a 20,000-tick budget.
- **ENG-01 counted an engine from a neighbouring M06 test.** It now counts its own structure only.
- **The long line was built up to 200 blocks out from its structure,** where another test's area could be. It now runs along z −2000 from its origin, and takes down everything it built when it finishes.
- **Pre-existing, not changed (M02 statistical mob tests):** CI Build #18 failed on `metalDoorsAreAttackedButNotDamaged` ("block changed"), which the M04 report already lists as a flake. One local run failed `lightBehindWallsStillAttracts` (random-walk statistics; see M03 report). Neither test touches M06 code.

## 7. Tuning (starting values, `tuning/Tuning.java`)

| Key | Value | Meaning |
|---|---|---|
| `winch.powered_speed` | 0.15 | coal winch strand speed, blocks/tick (crank `cable.crank_speed` 0.06), divided by 1 + grade load as for the crank |
| `winch.acceleration` | 0.01 | most the winch speed changes in a tick (PULL→FEED takes about 30 ticks) |
| `winch.burn_ticks_per_coal` | 1600 | ticks of actual hauling per coal |
| `winch.coal_capacity` | 64 | coal a winch holds |
| removed: `cable.drum_capacity` | — | one coil, any length |

Fixed (constants): blocked after 10 ticks without progress, idle stop after 40 ticks without a gripping cart, re-dock clearance 16 blocks.

## 8. Known gaps and open questions

- **Remote control (§4 limitation):** the winch is controlled at the drum. Hauls stop at the edge of the loaded world; nothing travels through unloaded chunks.
- **Cart front before the first dock:** a new freight cart's gate is on whichever end it got first. Docking settles it.
- **Release reach:** the post is the reliable target. If a player cannot reach it (for example a track hemmed in by blocks), the train can't be released without breaking the track, and breaking it frees the train.
- **Placed redstone rails keep working** (registry and old-structure compatibility). Only acquisition is retired.
- **Lit coal on breaking:** breaking a winch loses the partly burnt coal alight; unlit coal is returned.
- **M05 follow-up untouched:** coupled carts reaching a cable terminal ahead of the gripping cart (owner to inspect) is unchanged in M06.
- **Balance:** every number is a starting value with no playtest behind it.

## 9. Build, run and update

- `./gradlew build` builds the jar and runs the 129 server game tests.
- `xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` runs the real-client tests (needs `mesa-vulkan-drivers`).
- `./gradlew runClient` / `runServer` for a dev game.
- Playtest build: run `tools/windows/Update-SimpleBlock.bat`; it installs the latest `main` Build artifact (`simple-block-mod`).

**Suggested playtest (new world):**
1. Craft cable (3 nuggets down the middle), a coil (3 cable down the middle), a drum and anchors. Lay a long straight line, more than 100 blocks if you like: anchors under wooden beams at most 12 blocks apart. Close the loop with an anchor on the last one.
2. Put a furnace on the drum, then 2–3 coal. Right-click the lever: PULL, FEED, NEUTRAL. Watch the lever lean.
3. Place a Loading Track in the line (the way you face is the cart's front). Put a minecart with a rod under the cable and FEED it onto the track: it docks and the lever drops to NEUTRAL. Use the track's post to release, then PULL it back.
4. Build an engine and two coupled freight carts running at another Loading Track: the train stops and the engine switches off. Release with the post and start the engine again.
5. Walk into a freight cart (it stays) and into a cushioned one (it moves).
6. In creative, search "rail", "shulker", "granite": no retired items.
7. Walk far away from a running haul and come back; quit and reopen. The line, the cart and the coal should be as you left them, with the lever at NEUTRAL.

## 10. Next

M06 is complete pending ChatGPT's independent review and the owner's playtest. M07 (skips, transfers and the engine remodel) is now authorized by the owner and will be a separate milestone with its own commits, tests and report.
