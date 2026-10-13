# Milestone 05 report: Working haulage and the Cobblestone Engine

**HISTORICAL REPORT (M01–M07 format).** This record is preserved for its original implementation and tests. References to the retired decisions ledger now point to the dated archive, **not active implementation authority**. Under the current [roadmap rule](../../ROADMAP.md), new authorized work appends only a **concise dated implementation/verification note at the bottom of the main milestone document**, not a new standalone report.

Scope: the owner's brief, `docs/MILESTONE_05.md`, including the owner's mid-milestone clarification of the crank controls (left-click feeds, right-click pulls) and the two owner decisions taken during M05 (coupling by iron chain; engine driven from a seat plus a ground toggle). Implemented and tested on a server and in a real client; **not playtested by a person yet**.

- **Implementation commits on `main`:** `e662ec2` (work packages A-D) and `16b1098` (crank controls per the owner's clarification). CI [Build #17](https://github.com/boxofdog/simple_block/actions/runs/38306694723) on `16b1098`: **success**, `simple-block-mod` artifact published.
- **Tests:** **114/114** server game tests pass in `./gradlew build`: the 95 from M01-M04 plus 11 `CableLoopGameTests` and 8 `TrainGameTests`. All three real-client tests pass: `SurvivalSmokeClientTest`, `CableClientTest` (now also taking the BUG-001 close-ups) and the new `HaulageClientTest`, which drives both mechanical systems with real key presses and includes a real chunk unload and a save-and-reopen.
- **Negative controls:** 11, one per new rule (10 server, 1 client); each fails exactly the test aimed at it (section 6).
- **BUG-001:** cause found (coplanar faces of overlapping cubes), fixed in the model generator, and **verified visually** from four angles before and after (section 5).
- **Dedicated server:** starts with the M05 code (`runServer`: "Cable: 4 blocks per cable, 32 per coil", "Done (3.349s)"). The `stop` command piped into Gradle's stdin produced "An unexpected error occurred while trying to execute that command" with no stack trace; the process was then killed. Not investigated; no remote client was connected (NOT RUN).
- **Assets:** editable Blockbench sources and UV-template textures for every new model; no finished art (section 4).
- **Not built (by the brief):** skips, counterweights, coal-powered winch, extreme rails, stations, train controls beyond power and brake.

Evidence is in `docs/evidence/milestone-05/`:
- [`gametest-evidence.md`](../../evidence/milestone-05/gametest-evidence.md): what every M05 test logged, server and client.
- [`negative-controls.md`](../../evidence/milestone-05/negative-controls.md)
- [`baseline-build.txt`](../../evidence/milestone-05/baseline-build.txt)
- `screenshots/` (real client), with `screenshots/bug001/before-*.png` and `after-*.png`.

## 1. Baseline

- **Toolchain:** Minecraft 26.3 (unobfuscated), Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25. Unchanged.
- **Starting point:** `main` at `ee622cd`; `./gradlew build` passed 95/95.
- While M05 was in progress the owner pushed many documentation commits (M06-M09 planning and the crank-control clarification). I rebased onto them twice; they changed no code, and the M05 crank was changed to match the clarification (`16b1098`).

## 2. Reading of the brief, and choices

| Brief | Implemented |
|---|---|
| A. Keep the M04 open route | Unchanged: coil loading, hook pickup, routing, extending, sneak-use disconnect and dismantle all work as in M04 (the 10 M04 cable tests and the M04 client test pass). |
| A. Optional closed loop, return strand beside the working strand on the anchors' arm side | Use a **copper anchor on the last anchor**: it becomes the return wheel; the anchors between get return guides; the return strand runs 4.75 pixels beside the working one at roller height. The drum and anchors turn to line up with the route. |
| A. Conserve cable; refuse when short, obstructed or turning | The return spans plus 3 blocks for the turns come off the drum and go back on opening, or drop as coils when the drum breaks. Refused, with nothing used, if the drum is short, the return path hits a solid block, or the route **turns** (see "Deviations"). |
| A. Drum as reversible capstan; rod grips only the working strand | On a loop, cranking moves the working strand at 0.06 blocks/tick, out or back, and nothing piles onto the spool. Only working-strand spans are in the grip index; a rod under the return strand still grips the working one. |
| A. Stop short of the ends; hold; safe when broken; saves and unloads | Carts stop 1.5 blocks short of the drum and the wheel (or where the cable would leave the rod's reach) and are held there; letting go of the crank holds them. Breaking any part opens the loop and the cart stops where it is. The loop is saved with the drum; a loop with any part in a chunk that does not tick does not move. |
| A. Uphill | Speed divided by 1 + 1.0 x the mass of the gripping trains on a climbing rail. Measured: 17.7 ticks a block on the level, 44.0 up a 45-degree climb empty, 136.0 with 256 cobblestone. |
| B. Visible crank on every drum, held, reversible, not colliding with M04 gestures | A crank on the axle end of every drum, bare or wound. **Hold left-click to feed** the cable away, **hold right-click to pull** it back (owner's clarification). Left-click on the crank never breaks the drum: the client sends a small packet instead, which the server checks (reach, and that the hit is on the crank). Clicks elsewhere on the drum do what they did in M04, breaking included. Any held item works the crank, so a coil is never wound on from it. |
| B. Animated; discoverable without long text | The spool and crank turn with the cable's travel, and the cable's texture moves along the loop. The only crank text is "Close the loop to haul" on an open route. |
| B. Overhead mounting, keeping ground placement | Place a drum against the underside of a wooden beam (anchor supports): it hangs upside down, its cable leaving at about roller height. Standing drums are unchanged. |
| C. Furnace on a minecart makes an engine; breaking drops exactly a minecart and a furnace | Use a furnace on an **empty** minecart (no cargo, cushion, rod or rider). Broken, by hand or explosion, it drops one minecart and one furnace. The engine is **vanilla's furnace-minecart entity**, renamed and redrawn. |
| C. Address the furnace-minecart recipe | Recipe and its advancement disabled (`fabric:load_conditions` false). The item still exists in the creative inventory, named "Cobblestone Engine". |
| C. New editable model | `cobblestone_engine.bbmodel`, drawn in place of the cart body and furnace. |
| C. Coal; both directions; pushes and pulls a mixed consist | Coal or charcoal, 3,600 ticks of driving each, burnt only while driving. Ground: empty-hand use starts it away from you or stops it. Seat (owner's choice): in a cushioned cart coupled to it, hold forward to drive towards where you look along the track, back to brake. A freight, engine, cushioned and rod cart train ran 8.5 blocks each way, spacing kept. |
| C. Coupling (owner-confirmed) | Iron chain on one cart, then the next within 3 blocks; held 1.6 apart; shears take it off and drop the chain; a destroyed cart drops its chains; a stretched coupling past 4 blocks snaps. |
| C. Mass and grade limit it; a sustained climb stalls it | In 20 ticks on the level an engine alone covered 1.81 blocks, one with 3 loaded carts 0.26. Up an 8-block 45-degree climb the engine alone reached the top; the loaded train stalled part-way (x 15.3 of 12-20) and held without rolling back. |
| D. BUG-001 | Section 5. |

**Deviations and choices to confirm:**

1. **Loops on straight routes only.** The brief asked to test turns "explicitly", by refusal or operation. Loops refuse a turning route, naming the anchor where it turns. A loop round a corner needs curve pulleys and a rule for which side the return runs.
2. **The crank on an open route is locked** (it does not wind or pay out a loose line). Winding a loose line in and out stays with the M04 gestures (sneak-use, hook). Cranking a loose line belongs with the future vertical hoist.
3. **The drum model is now three parts** (frame, spool, crank) so the spool can turn. It looks as in M04 plus the crank. The drum's placed-block model is the frame; the item model shows all three.
4. **The engine reuses vanilla's furnace minecart entity** rather than adding a new entity: its item exists only in creative.
5. **A stopped or stalled engine holds its train** (brakes on). A train nothing drives behaves like rod-linked carts (push one and the others follow).

## 3. What changed

| Part | Files |
|---|---|
| Loops | `cable/CableLoops` (closing, opening, geometry); `cable/CableAnchorBlock` (`return` state: none, guide, wheel), `CableAnchorBlockEntity` (the wheel's anchor); `cable/CableRoutes` (loops in the M04 gestures; the drum's exit point; obstacle check by points); `cable/CableIndex` (grips with direction and position along the strand) |
| Crank and haulage | `cable/CableDrive` (crank input, capstan speed, hauling and holding carts, end clearance, grade load); `network/CrankPayload`; client `mixin/MultiPlayerGameModeMixin` (left-click on the crank); `cable/CableDrumBlock` (crank zone, `hanging` state), `CableDrumBlockEntity` (loop, speed, travel), `CableDrumPartBlock` (model carrier); `cable/ConnectingRods` |
| Trains | `train/TrainContent`, `train/Couplings`, `train/CobblestoneEngine`, `train/TrainPhysics` (mass, grade); `mixin/train/AbstractMinecartMixin` (couplings on every minecart), `MinecartFurnaceMixin` (the engine), `VehicleEntityMixin` (drops); `mixin/minecart/MinecartMixin` (chain, shears and furnace uses first) |
| Data | `data/minecraft/recipe/furnace_minecart.json` and its advancement (disabled); `lang/en_us.json` (new messages, engine name); `tuning/Tuning.java` (12 keys, section 7) |
| Client | `client/cable/CableDrumRenderer` (spool, crank, loop), `CableGeometry` (moving texture); `client/train/CobblestoneEngineRenderer`, `CouplingChains`; `mixin/AbstractMinecartRendererMixin`, `MinecartRenderStateMixin` |
| Assets | `tools/models/generate_models.py` (new parts, BUG-001 fix) and everything it writes: `art/blockbench/*.bbmodel`, models, blockstates, UV templates |
| Tests | `CableLoopGameTests` (11), `TrainGameTests` (8), `client/HaulageClientTest`; BUG-001 close-ups in `client/CableClientTest` |
| Docs | this report and its evidence; `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` section 5.0; README (M05 table); `art/blockbench/README.md`; `docs/UX_TEXT_LOG.md` (UX-005..008); `SimpleBlock` Javadoc |

## 4. Models and textures (for painting)

New editable sources in `art/blockbench/` (see its README for fixed points the code relies on):

- **Anchor:** with a return guide (anchors along a loop) and with the return wheel (front and back mirror images). Texture `cable_anchor.png` grew to 64 x 64 for the new parts.
- **Drum:** frame (the placed block), bare and wound spool, crank; `cable_drum.json` is all three for the item. Texture `cable_drum.png` 128 x 128.
- **Cobblestone Engine:** wheels, chassis, buffers, boiler, smokestack, firebox and door; 22 pixels long. Texture `cobblestone_engine.png` 128 x 128.
- **Chains** between coupled carts use vanilla's `iron_chain` texture as two crossed strips, like the chain block.

All textures are UV templates (flat colour per face, outlined), not artwork. The M04 cable files were regenerated (with `--force`) for the new parts and the BUG-001 fix; git history shows none had been edited since M04, so no painted work was overwritten.

## 5. BUG-001: drum-side ghosting

- **Cause:** the octagonal flanges (and the anchor's roller, the wheel and the coil/cable item loops) are made of four overlapping cubes: a straight band, a crossways band and two at 45 degrees. Their end faces lay in the **same plane** with different parts of the texture, so the game drew them fighting each other (z-fighting): the flickering diagonal stripes the owner saw.
- **Fix:** in `tools/models/generate_models.py` each band's ends are set back from the next one's (0.1 pixel for the crossways band, 0.2 for the diagonals), and the models regenerated. The Blockbench README now warns against coplanar overlaps.
- **Verification (visual):** the same four close-ups of a wound drum's flange (straight on, two oblique angles, from above) in the real client, before (`screenshots/bug001/before-*.png`) and after (`after-*.png`). Before: crossed stripes and mixed texture across the flange faces at every angle. After: clean, uniform flange faces at every angle. Verified on the software renderer used for client tests, not on a desktop GPU.

## 6. Test matrix

GT = server game test (fresh test world, stand-in players, every action through the server's own handlers). Client = real client with key presses.

| Test | Result | Method and evidence | Limitations |
|---|---|---|---|
| LOOP-01/02 close and open | PASS | GT `loopClosesWithAnAnchorAndOpensAgain`: 23 laid out, 26 of return off the drum; anchor goes into the wheel; guides and wheel states; drum faces the route; sneak-use opens it, route kept, cable and anchor back; breaking the wheel anchor opens it and drops 2 anchors. Client: closed with an anchor, 28 of return, client sees the loop | |
| LOOP-03 refusals | PASS | GT `loopClosingIsRefusedWhenItCannotWork`: short of cable, stone in the return path, a turning route: each refused, anchor kept, cable unchanged | |
| LOOP-04..06 haul, hold, reverse | PASS | GT `crankHaulsACartAlongTheLoopAndBack`: x 8.5 out to 22.0 (wheel at 23.5), held, back to 4.24 (drum at 1.5), held. Client: left-click held 160 ticks, cart 25.5 → 35.5; let go, held; right-click held, back to 28.1 | |
| LOOP-07 uphill and mass | PASS | GT `haulingUphillIsSlowerForHeavierLoads`: 17.7 / 44.0 / 136.0 ticks a block (level / empty climb / 256 cobblestone climb) | One grade (45-degree rail) |
| LOOP-08 broken while hauling | PASS | GT `breakingTheLoopStopsTheCart`: anchor broken mid-haul, loop opens, cart moved 0.000 in 60 ticks, cable kept | |
| LOOP-09 saved | PASS | GT `loopIsSavedWithTheDrum`; client: real chunk unload and save-and-reopen (below) | |
| LOOP-10 return never gripped | PASS | GT `returnStrandIsNeverGripped` | |
| LOOP-11 hauls a coupled train | PASS | GT `loopHaulsACoupledTrain`: empty cart, rod cart, cart with 128 raw copper: out 9.7 blocks, held, back, spacing kept | |
| LOOP-12 broken drum refunds | PASS | GT `breakingALoopedDrumGivesAllTheCableBack`: 64 blocks back as 2 coils, wheel anchor dropped, anchors free | |
| CRANK-01 crank vs other gestures | PASS | GT `crankLeavesTheOtherGesturesAlone`: right-click on the crank with a coil loads nothing; sneak on the crank doesn't dismantle; open route locked; drum-body sneak-use still dismantles; empty drum spins | |
| CRANK-02 overhead drum | PASS | GT `drumHangsUnderABeam`: hangs, cable exits at 11/16, winds and cranks; falls with its plank. Client: placed under a plank with the use key | |
| CRANK-03 animated, left-click safe | PASS | Client: client spool speed 0.06, travel advancing while cranked; drum still there after 160 ticks of held left-click. Negative control: without the client hook the drum breaks | Software renderer |
| ENG-01 conversion, drops | PASS | GT `furnaceTurnsAnEmptyMinecartIntoAnEngine`: loaded and cushioned carts refused (furnace kept); empty cart becomes an engine in place, keeping its coupling; broken: 1 minecart, 1 furnace, 1 chain; exploded: 1 minecart, 1 furnace; never a furnace minecart. Client: furnace used on a placed minecart | |
| ENG-02 recipe | PASS | GT `furnaceMinecartIsNotCraftable`: recipe and advancement absent, the grid crafts nothing | |
| ENG-03 fuel, ground toggle | PASS | GT `engineRunsOnCoalFromTheGround`: no fuel won't start; oak log refused; coal + charcoal 7,200; runs away from the player both ways; stops and holds; no fuel burnt while stopped. Client: coal by key press, started and stopped by key press | |
| ENG-04 driver seat | PASS | GT `riderDrivesTheTrainFromACushionedCart` and client: forward looking along the track drives that way; turning round reverses; back brakes to a stop, which holds | |
| ENG-05 mixed consist both ways | PASS | GT `engineHaulsAMixedTrainBothWays`: freight (256 raw iron), engine, cushioned, rod cart: 8.5 blocks east, then 7.5 west, gaps 1.2-2.2 | |
| ENG-06 mass and climb stall | PASS | GT `sustainedClimbStallsALoadedTrain` (numbers above) | One grade |
| ENG-07 saved | PASS | GT `enginesAndCouplingsAreSaved`: fuel, setting, heading, speed and couplings on both carts. Client: below | |
| COUP-01 chains and shears | PASS | GT `chainsCoupleAndShearsUncouple`: too far refused; one chain used; shears drop it; third coupling refused; free train settles 1.6 apart; broken middle cart drops 2 chains, frees both. Client: coupled by key presses, chain drawn | |
| SAFE-01 chunk unload | PASS | Client: player teleported 4,000 blocks away until the yard's chunks unloaded and the engine and rod cart were saved out of the level; back: loop, return length, cable, rod cart position and grip, engine position and fuel, couplings, cart counts all identical (no duplication, no runaway) | Unload of only part of a loop or train (a chunk border through it) is code-reviewed only |
| SAFE-02 save and reopen | PASS | Client: world saved, closed and reopened: the same snapshot, and the crank still hauls (28.1 → 31.9) | |
| REG M01-M04 | PASS | All 95 earlier game tests in the 114-test build; `SurvivalSmokeClientTest` and `CableClientTest` pass | |
| BUILD, CI | PASS | `./gradlew build` 114/114 locally; CI Build #17 on `16b1098` green, artifact `simple-block-mod` published | |

## 7. Tuning (starting values, all in `tuning/Tuning.java`)

| Key | Value | Meaning |
|---|---|---|
| `cable.crank_speed` | 0.06 | Working strand speed, blocks/tick (1.2 blocks/s) |
| `cable.grade_load` | 1.0 | Uphill haul: speed / (1 + this x mass on a climbing rail) |
| `cable.end_clearance` | 1.5 | Blocks short of the drum and the wheel a cart stops |
| `rail.cart_mass` / `rail.cargo_mass` / `rail.passenger_mass` | 1.0 / 2.0 / 0.5 | Empty cart; full load (pro rata); a rider |
| `train.engine_mass` | 2.0 | |
| `train.engine_force` | 0.02 | Engine pull |
| `train.grade_force` | 0.0078 | Pull-back per unit of mass on a climbing rail |
| `train.rolling_resistance` | 0.0005 | Per unit of mass |
| `train.brake` | 0.02 | Deceleration, blocks/tick per tick |
| `train.max_speed` | 0.2 | Blocks/tick (4 blocks/s) |

Fixed: 3,600 ticks of driving per coal (up to 32,000); couplings 1.6 apart, 3 to couple, snap past 4.

## 8. Known gaps

- **Feel:** no person has cranked a loop, driven an engine or coupled a train yet.
- **Loops round corners:** refused (section 2).
- **Engine and loop driving one train in the same tick:** whichever is handled first wins; a stopped engine never holds a train the cable is hauling.
- **A drum broken while one of its anchors is unloaded:** that anchor keeps its return part (and the wheel's anchor, which drops when it is broken) until a player deals with it.
- **Hopper, TNT and other special minecarts** don't couple.
- **Dedicated server with a remote client, two players at once, a desktop GPU:** NOT RUN.
- **Dedicated server shutdown:** see the summary at the top.

## 9. Build, run and update

| Command | What it does |
|---|---|
| `./gradlew build` | Jar plus all 114 server game tests |
| `XDG_RUNTIME_DIR=/tmp/xdg xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` | The three real-client tests |
| `JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dfabric-api.gametest.filter=simple_block-gametest:train_game_tests_*" ./gradlew runGameTest` | Just the train tests (`cable_loop_game_tests_*` for the loops) |

Playtest builds: the Build workflow publishes `simple-block-mod` on every push to `main`; `tools/windows/Update-SimpleBlock.bat` installs it. In survival: lay a straight route, wind enough cable for twice its length, use a copper anchor on the last anchor, fit a rod to a cart under it and hold left-click on the drum's crank. For an engine: use a furnace on an empty minecart, give it coal, and use it with an empty hand.

## 10. Next

M05 stops here. The roadmap's next milestone (M06, freight loading track and the coal-powered winch) is a draft and is **not** started.

---

## Owner milestone disposition — 2026-10-13 (roadmap reconsolidation)

**M05: SHIPPED.** Originally accepted by the owner 2026-10-12 and expressly reaffirmed by the owner 2026-10-13 after independent GitHub/source/CI review. [Build #17](https://github.com/boxofdog/simple_block/actions/runs/38306694723) is green with a published `simple-block-mod` artifact; 114/114 server tests and three real-client tests are documented above. No new human playtest or assistant-run local execution is claimed by this status. Detailed implementation/CI handoff remains in this report, and the current brief status in [ROADMAP.md](../../ROADMAP.md). M04 and M06 are also SHIPPED; M07 remains PLAYTESTING.
