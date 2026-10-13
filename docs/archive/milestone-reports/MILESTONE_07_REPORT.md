# Milestone 07 report: Upright ore skip, vertical hoisting, freight transfer and machinery remodels

**HISTORICAL REPORT (M01–M07 format).** This record is preserved for its original implementation and tests. References to the retired decisions ledger now point to the dated archive, **not active implementation authority**. Under the current [roadmap rule](../../ROADMAP.md), new authorized work appends only a **concise dated implementation/verification note at the bottom of the main milestone document**, not a new standalone report.

Brief: [MILESTONE_07.md](../../MILESTONE_07.md). Choices where the brief left room: [DECISIONS.md §5.5.3](../DECISIONS_PRE_RETIREMENT_2026-10-13.md). Evidence: [`docs/evidence/milestone-07/`](../../evidence/milestone-07) (server results and negative controls in `server-tests-and-controls.md`).

**Summary.** Everything in the brief is implemented and tested:

- **Remodels:** the engine is a furnace engine inside the unchanged vanilla minecart, and the coal winch's motor now replaces the drum's motor-side stand.
- **The ore skip** (shaped recipe, 1×1×3 body plus a collidable bridle, a 512 single-commodity slot with a real screen):
  - hoisted on one cable by the hand crank and the coal winch;
  - stops safely, with the winch's coal rules unchanged;
  - exchanges cargo at the M06 Loading Rail, where the cart's height beside the skip decides the direction.

**Testing:**

- server game tests (139/139);
- the real client by key presses and mouse clicks (8 client tests, three new);
- over a real connection to a dedicated server.

Two limitations are recorded in section 8:

- the winch is still controlled at its drum (M06);
- a skip is hung within the 12-block hold reach of the head anchor, then lowered.

No human playtest has been done. M06 owner acceptance is still pending; M07 is built on the M06 code as corrected on 2026-10-13. M08–M10 are untouched.

| | |
|---|---|
| Implementation commits on `main` | [`886f6f1`](https://github.com/boxofdog/simple_block/commit/886f6f1) visual remodels (§5A/5B) · [`a63013a`](https://github.com/boxofdog/simple_block/commit/a63013ab70912736c6054f2c2463cf6d8f320fb0) ore skip, hoisting, stations |
| CI | [Build #21](https://github.com/boxofdog/simple_block/actions/runs/38384835982) on `886f6f1`: success. [Build #22](https://github.com/boxofdog/simple_block/actions/runs/38390569443) on `a63013a`: **success**, `simple-block-mod` artifact id 11808882080 (398,728 bytes) |
| Server game tests | `./gradlew build`: **139/139** (M06 corrections had 131; +8 `SkipGameTests`) |
| Client game tests | `./gradlew runClientGameTest`: all 8 pass (M01–M06 tests unchanged, new `MachineryArtClientTest`, `SkipClientTest`, `SkipServerClientTest`); full log in `docs/evidence/milestone-07/client-game-tests.txt` |
| Dedicated server | `SkipServerClientTest` starts a dedicated server and connects the real client over the network (placement, screen clicks, a chute exchange) |

## 1. Baseline

- **Starting point:** `main` at `859f780`, the M06 corrections closeout. Code `fc8f99a`, [CI Build #20](https://github.com/boxofdog/simple_block/actions/runs/38382953782) green, 131 server game tests, and all five client tests pass.
- **Toolchain:** unchanged (Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25).
- **M06 interfaces used as built:**
  - the level-saved cable lines and their drive (crank and coal winch, stops, coal rules, the BUG-002 lever skip);
  - the drum's loose cable end;
  - the Loading Rail's dock, its whole-train hold and its hinged stop and release;
  - the freight cart's synced front and gate models.

  None of it is re-implemented. M07 adds a hoist link to the line and a ramp flag to the cart.

## 2. Reading of the brief, and choices

Starting values are in `tuning/Tuning.java` (section 7).

- **Engine (§5A).**
  - **Before:** the M05 renderer drew its own engine instead of the cart body.
  - **Now:** the vanilla minecart body (vanilla model layer and texture, untouched) is drawn as usual. The new engine model is drawn inside it, with the cart's own pose, so it follows rails, slopes and the hurt wobble.
  - **The model:** a cobblestone firebox filling the back of the cart, its fire door above the rim; a boiler running forward with bands; a steam dome; and a smokestack with a collar, a band and a flared cap, about 1.9 blocks above the rail (it fits a 2-high tunnel).
  - Entity, creation, controls, fuel, traction, coupling, collision and drops are unchanged.
- **Winch motor (§5B).** The drum has a new block state `powered`, set when the furnace is fitted.
  - **Powered frame:** a frame model in which the west (motor-side) stand is replaced by the motor: a heavy plinth, a firebox with doors, a bearing housing on the axle where the stand's bearing was, a gear plate and a smokestack.
  - **Manual drums** keep the old frame.
  - **Hanging drums** use a variant whose flue leaves the firebox outboard and rises beside the beam, so it never points down or into the beam.
  - The M06 separate furnace box is removed. Shape, hit zones, lever, coal and drive are unchanged.
- **Skip shape (§1).**
  - **Entity and hitbox:** `simple_block:ore_skip`, a vehicle entity with a 1×4×1 hitbox: body (chute, two-block bin) plus the bridle block. It is solid to other entities and cannot be pushed by players or pistons.
  - **Facing:** it faces the player who places it.
  - **Silhouette:** its picture is a minecart on its side, 1.75 blocks wide across its front, so for every block of the skip (body and bridle) the cells on both sides must have no collision shape. This is checked when it is placed, hung and every time it moves; its own collision stays 1×1×4.
  - **Moving:** it moves only straight up and down, in steps it has room for, ending exactly in contact with what stopped it. It is stopped by:
    - blocks in its column or side cells;
    - living things, carts and other skips in its column;
    - the head of the shaft.
- **The 512 slot (§2).** The cargo is an item and a count on the entity (synced), never an `ItemStack` of 512.
  - **Screen:** the hopper background with one slot. The slot is drawn by the screen, showing the item, its count and "count / 512". Clicks on it are menu buttons handled on the server:
    - **left:** take a stack, or put in all of the cursor's;
    - **right:** take half a stack, or put one in;
    - **shift:** a stack into the inventory.

    Shift-click on cargo in the inventory puts it in. Drags, double-clicks and number keys only ever touch the player's own slots.
  - **Limits:** at most one normal stack ever comes out, within the heavy-load cap. Eligibility is the freight cart's (`#simple_block:minecart_cargo`, no components); once loaded, the same item only.
  - **Hand loading:** cargo in hand loads straight in.
  - **Breaking:** drops the skip and the cargo as ordinary stacks.
- **Hoisting (§3).**
  - **Hanging it:** route the drum's cable to an anchor over the shaft, pick up the end and use it on the skip standing straight under that anchor. The line records the skip (`hoist`, saved with the level's lines), so the drum may be far away and unloaded.
  - **The cable:** one main cable, from the bridle apex up over the anchor's roller and along the route; the hanging length costs nothing.
  - **Driving it:** with the same drive as M05/M06 loops, PULL lifts and FEED lowers. That is the crank's right-click and held left-click, or the coal winch's lever.
  - **Lift load:** lifting is slowed by 1 + 0.5 × mass (empty 1.5; +4 for a full 512).
  - **Stops**, all of which put the coal winch to NEUTRAL with coal burning only while it moves:
    - `end`: the apex 0.3 under the roller;
    - `blocked`: anything in the way;
    - `docked`: a station;
    - `idle`: the skip's chunk not loaded.

    BUG-002's lever skip applies to the skip's stops too.
  - **Taking it off:** sneak-use with an empty hand, only while it rests on something. Any route change lets it go, and it settles onto what is under it. Disconnecting anchors, dismantling and breaking the drum or the skip make or lose no coil, cable or cargo.
- **Stations (§4).**
  - **What counts:** a Loading Rail in the cell in front of the skip, facing into the shaft (a docked cart's gate points at the skip), with a freight cart docked on it.
  - **Direction by height:**
    - cart level with the bridle (skip bottom + 3): **cart → skip**. The cart's front gate folds down as a ramp and the skip's lid opens.
    - cart level with the chute (skip bottom): **skip → cart**. The skip's discharge gate slides open; the cart's front stays shut.

    What either holds never decides the direction. Anything else does nothing: another height, the body's height, a rail facing away, a different commodity.
  - **Stopping:** a skip wound past a station height stops there exactly (`docked`). It passes that station once it moves on, so there is no repeated lock.
  - **The exchange:** doors open over 10 ticks; then 4 items a tick, each tick taking from one and giving to the other, as many as the receiver takes (partial transfers allowed); doors close over 10 ticks. The skip holds still meanwhile.
  - **Afterwards:** the train stays docked (release at the rail) and nothing restarts.
  - **Not saved mid-way:** after a reload an aligned exchange restarts. The cart's ramp is a synced, unsaved flag renewed by the skip each tick, so it can never stay down after the skip is gone.

## 3. What changed

- **M07 visual commit `886f6f1`:**
  - `cable/CableDrumBlock` (`POWERED`), `CoalWinch.fitFurnace`;
  - client `CableDrumRenderer` (furnace box removed), `CobblestoneEngineRenderer` and `mixin/AbstractMinecartRendererMixin` (vanilla body kept);
  - the generator (new engine; drum motor frames);
  - regenerated `cable_drum*`, `cobblestone_engine` models, textures and bbmodels;
  - `MachineryArtClientTest`.
- **M07 skip commit (`skip/` package, new):**
  - `OreSkip`, `SkipCargo`, `SkipMenu`, `SkipStations`, `SkipItem`, `SkipPartBlock`, `SkipContent`; client `skip/OreSkipRenderer`, `skip/SkipScreen`;
  - `cable/CableHoists` (new), and small changes to `CableLines` (`hoist`, `drivable()`), `CableDrive`, `CoalWinch`, `CableLoops` and `CableRoutes`;
  - `train/LoadingTracks` and `mixin/train/AbstractMinecartMixin` (the ramp flag), client `CargoMinecartRenderer` (open gate);
  - tuning `skip.*`;
  - recipe and advancement `ore_skip`, lang;
  - models, textures and bbmodels for the skip;
  - `SkipGameTests`, `SkipClientTest`, `SkipServerClientTest`, the `skip_shaft` test structure;
  - README and the Blockbench README.

## 4. Long shafts and unloaded chunks

- **The rule:** the skip moves only in its own entity tick, so only while its chunk is entity-ticking; its column is one chunk, so a moving skip never enters unloaded geometry.
- **The drum** can be anywhere: the line is driven from the level's saved lines, as for M06 long lines.
- **When the skip's chunk is unloaded:** it neither moves nor reports, the coal winch stops with `idle` after 40 ticks and burns nothing, and the skip stays where it was.
- **Nothing is force-loaded, simulated or teleported.**
- **Real-client check:** PULL is set, then the player goes 400 blocks away. The skip left the loaded world 2 ticks later at y 0.06; 100 ticks later, back, it was at y 0.06 with the lever at NEUTRAL (`idle`) and the coal burn unchanged (6397 → 6397).
- **Saved and reopened:** cargo, height, the cable link and the coal were identical, and PULL lifted it again.

## 5. Models and textures (for painting)

- **Editable sources** in `art/blockbench/` (see its README):
  - `ore_skip_chute`, `ore_skip_bin`, `ore_skip_gate_closed`/`_open`, `ore_skip_lid_closed`/`_open`, `ore_skip_bridle`, `ore_skip` (item);
  - `cobblestone_engine`;
  - `cable_drum_frame_powered` and `cable_drum_frame_powered_hanging`.
- **Textures** are UV templates (flat shaded colour blocks per face), not artwork. All were generated by `tools/models/generate_models.py`; no owner-painted file was overwritten.
- **Before/after screenshots** are in `docs/evidence/milestone-07/screenshots/`:
  - engine: `m07-art-engine-*`, with the M05 engine as `...BEFORE-m05`;
  - winch, ground and hanging: `m07-art-winch-*-before`/`-after`/`-motor-side`/`-lever-side`, with the M06 sidecar box as `...BEFORE-m06-sidecar`;
  - skip: `m07-skip-*` and `m07-server-*`.

## 6. Test matrix

| ID | Category (brief §6) | Check | Result | Evidence | Negative control |
|---|---|---|---|---|---|
| SKIP-01 | Recipe / item | Exact 3×3: cable top middle, iron trapdoor centre, iron ingot / minecart / iron ingot bottom, rest empty → 1 skip | PASS | 9 wrong layouts make nothing: swapped, misplaced minecart, no cable, one ingot, wooden trapdoor, iron blocks, furnace minecart, cable in the corner, an extra cable | the 9 wrong layouts |
| SKIP-02 | Geometry | Placed from the item on a block's top: 1×4×1 hitbox (body + bridle), facing the player's way back, solid to entities; refused with a block in a side cell, a side cell at bridle level or the bridle cell; front/back blocks fine | PASS | box 1×4×1 standing on the block; facing north; side cells along x | side-cell check removed: SKIP-02 FAIL (placed with a block in a side cell), SKIP-04 FAIL (went past a side block) |
| SKIP-03 | Inventory / persistence | Hand: 576 raw iron offered, 512 taken. Coal and sticks refused, slot unchanged. Screen buttons: L 448/cursor 64; L 512/0; R 480/32; R 481/31; shift 448 + 33 into the inventory (heavy cap). Shift-click from the inventory back to 512. Freight cart still 256. Save/load identical. Broken: 8×64 raw iron + 1 skip | PASS | totals 512 after every click; every drop a normal stack | stack caps removed: SKIP-03 FAIL ("button 1: skip 448, cursor 64") |
| SKIP-04 | Hoisting / stop / fuel | Cable end used on the skip: hung, one main cable (3 points), no loose hook, one coil. Crank pull 40 ticks 1.0 → 2.44; crank feed 20 ticks → 1.12. Coal winch PULL → stops at the head (apex 11.45 under the roller at 11.75), lever NEUTRAL (`end`); no coal burnt while stopped; sneak-use refused while hanging. Next click FEED (BUG-002). A block in a side cell stops it at exactly 4.000 (`blocked`); coal burnt only while moving (30 ticks). Down to the floor (`blocked`), unhung on the floor, hook back | PASS | see SKIP-04 log | (side-cell control above) |
| SKIP-05 | Hoisting (reversal) / safe detach | A 3-tick PULL clicked straight to FEED keeps going down (the drive passes through zero). Head anchor disconnected while lifted: let go, falls exactly onto the floor, 300 coal kept, coil kept | PASS | | near-zero speed fix reverted: SKIP-05 FAIL ("lever after reversing through zero (blocked)") |
| SKIP-06 | Cart → skip; skip → cart; station interaction | Coal winch PULL stops exactly at the top station (`docked`, lever NEUTRAL). The cart tips 112 of its 200 into a skip already holding 400 (→ 512, cart 88; direction by height, not by cargo). Ramp down and lid open seen, discharge gate never. Doors closed after; cart still held. FEED down: the skip fills the empty bottom cart 256 (skip 512 → 256) with the discharge gate open and the cart front shut. Total 600 kept | PASS | | station stop disabled: SKIP-06 FAIL (went past to 7.45) |
| SKIP-07 | Direction negative controls | 80 ticks. Empty skip / loaded cart at the chute: 0/100. Full skip / empty cart at the bridle: 512/0. Cart beside the body: 0/100. Rail facing away: 0/100. Coal cart at an iron skip: 10/100. No exchange running | PASS | | height rule shifted: SKIP-07 FAIL ("cart beside the body: skip 100, cart 0") |
| SKIP-08 | Interrupted / partial | Cart released after 24 of 256: skip 24 + cart 232, nothing more moves, doors close. Partial: a cart with 200 takes 56 (→ 256), the skip keeps 244 | PASS | | |
| CL-ART | Winch motor art | Real client. Ground and hanging drums: manual keeps its stand; a furnace (key press) switches the frame to the motor (client sees `powered`), shape unchanged; before/after, motor side, lever side | PASS | `m07-art-winch-*` | |
| CL-ART | Engine art | Real client. Made by key presses; drawn by the engine renderer in the vanilla minecart; stopped, side, coupled to a freight cart, running, on a curve and on a slope | PASS | `m07-art-engine-*` | |
| CL-SKIP | Skip in a real client | Placed by key, facing north. Loaded by hand ("Raw Iron 512 / 512"). Screen shows 512. Real mouse clicks on the slot: L 448/cursor 64; L 512/0; shift 448/inventory 64 (client and server agree) | PASS | `m07-skip-placed`, `m07-skip-screen-*` | |
| CL-HANG | Hoisting in a real client | Cable routed by hand through 2 anchors; the end used on the skip: hung, client draws its cable. Furnace and coal by key | PASS | `m07-skip-hung-on-the-cable` | |
| CL-UNLOAD | Long shaft / chunk boundary | PULL set, then 400 blocks away: the skip left the loaded world after 2 ticks at y 0.06; back after 100 ticks at y 0.06; lever NEUTRAL (`idle`); coal 6397 → 6397. No force-loading | PASS | | |
| CL-STATION | Stations in a real client | PULL stops at y 5 (`docked`); client sees ramp and lid; 200 into the skip (300 → 500). FEED to y 0; client sees the discharge gate open and the cart front shut; 256 into the bottom cart (skip 244) | PASS | `m07-skip-loading-ramp`, `m07-skip-unloading-chute` | |
| CL-REOPEN | Persistence | Save and reopen: height, count 244, hung, line hoist, coal identical; PULL lifts it again (0 → 2.20) | PASS | | |
| CL-SERVER | Dedicated server / multiplayer | Over the network: placed by key (north), loaded by key, the remote screen shows 64; right-click → server 32, client 32, cursor 32; left-click back → 64; a cart beside the chute filled 64 with the client seeing the gate open | PASS | `m07-server-*` | |
| REG | Regression | All earlier server tests and all M01–M06 client tests | PASS (139/139 server; all 8 client tests incl. M01–M06) | | |
| — | Not run | Human playtest; desktop GPU; two players on one skip at once; a real shaft deeper than the 16-block test structure in a server test (the client test shaft is 10 deep) | NOT RUN | | |


## 7. Tuning (starting values, `tuning/Tuning.java`)

| Key | Value | Meaning |
|---|---|---|
| `skip.mass` | 1.5 | empty skip, in empty-minecart units |
| `skip.cargo_mass` | 4.0 | added by a full 512 (scaled by the load) |
| `skip.lift_load` | 0.5 | lifting speed = drive speed / (1 + lift_load × mass): empty, the coal winch lifts at about 0.086 blocks/tick and a full skip at about 0.044; the crank at 0.034 and 0.018 |
| `skip.transfer_per_tick` | 4 | items moved a tick at a station (a full cart in 64 ticks) |
| `skip.fall_speed` | 0.25 | blocks/tick a skip with no cable settles at |

Locked values: the 512 capacity (`SkipCargo.CAPACITY`), the 1×1×3 body plus bridle, door timing 10 ticks, and the apex stop 0.3 under the roller.

## 8. Known gaps and open questions

- **Hanging reach:**
  - the cable end slips from the hand beyond the 12-block span plus a player's reach of its last support, so a skip is hung within about 15 blocks of the head anchor and then lowered. For a deeper shaft, stand the skip on a temporary block near the top, hang it, remove the block and lower.
  - the skip must stand straight under the route's last anchor.
- **Winch control (M06 limitation):** the lever and crank are at the drum, which must be loaded to start or stop a haul. A skip far from its drum keeps working while it is loaded.
- **After a stop on the way down,** FEED stays skipped until the load moves (the M06 BUG-002 rule), even if the obstruction is removed; PULL a little first.
- **Side clearance** is checked for the whole skip (body and bridle). A shaft is therefore three cells wide across the skip's front for its whole travel, including the surface floor beside the head of the shaft.
- **Pre-existing flake:** the M02 statistical mob test `lightBehindWallsStillAttracts` failed once in a local full build (passed twice alone; listed in the M06 report). Not M07 code.
- **Not tested:** a human playtest, a desktop GPU, two players at once on one skip.
- **Template art only;** the skip's look awaits the owner's painting.
- **Balance:** every number is a starting value with no playtest behind it.

## 9. Build, run and update

- `./gradlew build` builds the jar and runs the server game tests.
- `xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` runs the real-client tests (needs `mesa-vulkan-drivers`).
- Playtest build: `tools/windows/Update-SimpleBlock.bat` installs the latest `main` Build artifact (`simple-block-mod`).

**Suggested playtest (new world):**
1. **Look at the remodels.** Make a Cobblestone Engine (a furnace on an empty minecart): a furnace engine sits in a plain minecart. Put a furnace on a cable drum: the motor replaces the stand on one side. Try one hung under a beam too.
2. **Craft and place a skip.** Craft one (cable / iron trapdoor / ingot, minecart, ingot). Dig a shaft three blocks wide across and one deep, and stand the skip on the floor, front towards where the rail will be.
3. **Fill it and try its screen.** Load raw iron by hand. Open it with an empty hand: click the slot (left, right, shift) and watch the count.
4. **Lay the cable.** Run a drum's cable through anchors to an anchor under a beam straight over the skip (spans of at most 12). Pick up the end, walk to the skip and use it on the skip.
5. **Hoist it.** Crank (right-click lifts, hold left-click lowers), then fit a furnace and coal and use the lever. It stops at the top and the bottom, and the lever drops to NEUTRAL.
6. **Load a cart in.** Put a Loading Rail in front of the skip at the top, its facing into the shaft, one block above the skip's top (level with the bridle). Bring a loaded freight cart onto it: it docks. Lift the skip: it stops level with the cart, the cart's gate folds down and the ore goes in.
7. **Unload into a cart.** Put a Loading Rail in front of the skip's chute at the bottom, with an empty cart docked. Lower the skip: it stops and fills the cart through its chute.
8. **Walk far away** while it hoists, come back, then save and reopen.

## 10. Next

M07 is complete pending ChatGPT's independent review and the owner's playtest. M06's owner acceptance (including BUG-002 and the hinged stop) is also still pending. M08–M10 are not authorized.

---

## 11. Amendment, 2026-10-13: owner playtest patch (§§1–10) and the four-gate cart

Brief: [MILESTONE_07_PLAYTEST_PATCH.md](../../MILESTONE_07_PLAYTEST_PATCH.md) §§1–10, implemented as one pass as the owner asked, **plus** the owner's later direct instruction that every minecart share one four-gate body. Sections 1–10 above are the **historical** record of Build #22; that build passed the tests of the **old** geometry and does not stand for this one. Choices: [DECISIONS.md §5.5.4](../DECISIONS_PRE_RETIREMENT_2026-10-13.md). Evidence: [`docs/evidence/milestone-07-patch/`](../../evidence/milestone-07-patch), which holds:
- `server-tests-and-controls.md`: the server results and every negative control;
- `client-test-log.txt`;
- the `m07p-*`, `m07c-*` and `m07-server-*` screenshots.

M07 owner acceptance stays **unchecked**. M08–M11 are untouched.

| | |
|---|---|
| Implementation commit on `main` | [`49d7add`](https://github.com/boxofdog/simple_block/commit/49d7add4a3793b1ac4bb61e1f7f54156eb4d5cbc) |
| CI | [Build #23](https://github.com/boxofdog/simple_block/actions/runs/38438092565) on `49d7add`: **success**, `simple-block-mod` artifact id 11834545069 (408,005 bytes) |
| Server game tests | `./gradlew build`: **152/152** (139 before: +SKIP-09..13, +WIN-07a..h; the SKIP-02/04/06/07/08, CRANK-01 and WIN-01 tests rewritten for the new rules) |
| Client game tests | all **9** pass (new `MinecartBodyClientTest`; `SkipClientTest` rewritten around a 36-block shaft; `SkipServerClientTest` now hangs and unloads over the network; `FreightStationClientTest` docks with its last support over the rail) |
| Negative controls | 17, each fails its test (14 server, 1 more server for §10, 2 real-client), sources restored after each |

### 11.1 What changed

- **§1 Unloading place.** Measured from the vanilla models:
  - the chute's lip sits 1/16 above the skip's bottom;
  - a cart's rim sits 12/16 above its rail block.

  So the skip unloads when its bottom is at the rail + 11/16, with the cart one level below the chute in the cell in front, kitty-corner. The skip model is unchanged. A cart level with the chute, side by side, never exchanges. Only a hung skip has stations.
- **§2 Loading.** The cart's gate on the skip's side opens into the intake while the lid opens: 10-tick animated doors, interpolated on the client. Both close afterwards. Nothing transfers without a real station.
- **§3 Bridle.**
  - A placed skip is 1×3 with no bridle.
  - Hooking the cable on adds the bridle as a fourth, colliding block, but only if that space and its side cells are free. If they aren't, the existing "cable would run through" message shows and nothing is used.
  - Taking it off removes the bridle, and saved worlds keep the state.
- **§4 Pulley.**
  - The head anchor turns into a ceiling pulley (`return=pulley`, a new model) only when a skip is hooked on. A free hook never makes a pulley.
  - It reverts when the skip is taken off or the route changes.
  - The loose end stays at its paid-out height.
- **§5A Cable hook.** Carrying the end is measured across the ground:
  - it goes taut at 12 blocks;
  - it is put down beyond 16 blocks;
  - there is no message (the old "slipped" message is removed).

  The span limit stays 12. Before any skip is attached, the crank and the coal winch FEED or PULL the free hook down or up the shaft with no depth cap, with these rules:
  - the paid-out length is saved with the line;
  - no coal burns while the hook is still;
  - nothing moves while its chunk is not ticking.

  In the client test the hook was wound 33 blocks down a 36-block shaft onto the skip, picked up from the floor without scaffolding, and hooked on.
- **§8 → four-gate cart.**
  - First pass: the freight gate replaced the cart's real end wall (that wall was left out of the model).
  - Then the owner asked for one body for every minecart. `GatedMinecartModel` now replaces vanilla's cart model in every minecart renderer:
    - vanilla's floor and texture;
    - four corner columns, plus a rim along the top of each long side;
    - two end gates (12 px wide, no rim over them), hinged at the bottom and folding 112.5° into a ramp;
    - two side gates (16 px wide), hinged under the rims and swinging 100° up and out.
  - Its UVs reuse vanilla's wall areas, so a closed cart looks like vanilla's.
  - Only a freight cart tipping into a skip opens a gate, the one facing the skip. Cushioned carts, the Cobblestone Engine and the rest keep all four shut.
  - Stations now accept a Loading Rail end-on or side-on in front of the skip. A docked car of a coupled train can tip sideways through its long gate without uncoupling (SKIP-13 and the real-client side-on test).
  - The M06 `freight_gate` technical block and its models are retired. The editable source is `art/blockbench/gated_minecart.bbmodel` (Modded Entity, on vanilla's 64×32 minecart texture, which is not copied into the repository).
- **§9 Moving cable.** On a route that doesn't loop, the drum shows its drive speed only on ticks the skip or hook actually moved, and spool and cable texture follow that. The texture phase starts at the drum and carries on from the route's end down the shaft. In the client, 3 ticks of PULL moved it +0.20 and 3 ticks of FEED −0.34, and it stayed exact when stopped at a station.
- **§10 Far-end stop.** Reproduced first (WIN-07, failing before the fix): with the return wheel at x 23.5, FEED always stopped at x 21.950 with stop `end`.
  - Cause: `cable.end_clearance` 1.5 (`grip.along` 20.45 of 22.0).
  - Effect: a Loading Rail under the last support, one block before it, or a terminal rail there was never entered. Rails 2 or 3 blocks before it docked. PULL to a rail 2 blocks from a drum on the ground stopped at x 4.25 on the look-ahead grip check, because the cable dips to the drum.
  - Fix: if a Loading Rail the cart will dock at lies within 1.5 blocks ahead, the clearance and look-ahead grip checks are waived for that last stretch, and docking stops the cart. Track-end, derailment and unloaded-chunk checks are unchanged.
  - Without a station the 1.5-block clearance stays. The cart stops at x 21.95 on the rails, still gripping, whether the track goes on or ends under the support.

### 11.2 Test matrix (patch §6)

| Check | Result | Evidence |
|---|---|---|
| Unload only below, kitty-corner, lip on rim; same-level never | PASS | SKIP-06/07/12; client `m07p-unload-lip-on-rim-{side,front,east,south,west}` |
| Load at the bridle with gate ramp and lid | PASS | SKIP-06/12; client gate swing 0.10→0.50→1.00, lid 1.00; `m07p-load-*` |
| Conditional bridle, refused if obstructed, save/reload | PASS | SKIP-02/09; client box 3.0 → 4.0 → 3.0 |
| Pulley only while hooked, reverts | PASS | SKIP-09; client NONE → PULLEY → NONE |
| Free hook wound by crank and coal winch, no coal at rest, unloaded chunk | PASS | SKIP-10; client: 400 blocks away, drop and coal unchanged |
| Deep shaft (>30) without scaffolding | PASS | client, 36-block shaft |
| Horizontal leash 12/16, span limit 12 | PASS | SKIP-11 |
| Four-gate body: shut cart = vanilla look; each gate opens alone; cushioned and engine shut | PASS | `MinecartBodyClientTest`, `m07c-*` |
| Side-on transfer, coupled train stays coupled | PASS | SKIP-13; client `m07c-side-on-train-*` |
| Moving hoist cable, both ways, still when stopped | PASS | client phase check and `m07p-hoist-cable-{pull,feed}-frame-{1,2}` |
| Far-end Loading Rail: under, 1, 2 or 3 blocks before the last support, terminal; no station; near the drum | PASS | WIN-07a..h; client station dock with its last support over the rail |
| Dedicated server sync (bridle, pulley, unload) | PASS | `SkipServerClientTest` |
| Regression M01–M07 | PASS | 152/152 server, 9/9 client |
| §10 on curves and slopes, with a coupled train, after a partial chunk unload | NOT RUN | not covered by a dedicated test |
| Blockbench opening `gated_minecart.bbmodel` | NOT RUN | generated by script; not opened in Blockbench here |
| Human playtest, desktop GPU, two players on one skip | NOT RUN | |

### 11.3 Known gaps and open questions

- **Owner instruction supersedes earlier rules:** the four-gate body now covers the Cobblestone Engine and every other minecart. This replaces the earlier rule (CLAUDE.md, patch §8) that the engine sit in an unchanged vanilla minecart and that other cart models stay vanilla.
- **Working a whole train car by car:** the M06 release rule makes every car of a released train pass over that Loading Rail once. So side-on transfer serves the car that docks, but after a release the next car of the same train does not dock there. Changing that would change settled M06 behaviour, so it is left for the owner to decide.
- **Near the drum:** a cart docking 2 blocks from a drum on the ground loses the dipping cable just short of the rail and rolls onto it. The winch then stops as `idle` rather than `docked`; the lever still goes to NEUTRAL.
- **Client test harness:** once, closing the reopened world in `SkipClientTest` hung inside Fabric's client-gametest threading (the render thread waited on the server halt while the server and test threads waited on the test phaser). The same test passed in every other run, including the full suite. The hung run was stopped and the evidence re-run.
- **Art:** the pulley texture is a template. The cart body uses vanilla's texture, and per the owner's review only touch-ups are wanted (see [Joe's To-Do](../../Joe's%20To-Do/BLOCKBENCH_PAINTING.md)).

**Suggested playtest:** repeat §9's steps 2–8 with these changes:
- hang the skip at the bottom of a deep shaft by winching the free hook down;
- put the lower Loading Rail one level below the chute;
- try a side-on rail with a coupled train;
- run a cart to a Loading Rail under the last support;
- look at closed and open carts of every kind.

---

## 12. Independent review and owner status, 2026-10-13

**Review disposition: SOURCE/CI REVIEW COMPLETE; M07 STATUS = PLAYTESTING; OWNER ACCEPTANCE NOT GIVEN.** At the owner's request, ChatGPT reviewed code `49d7add`, follow-up report `dbda956`, Build #23, documented server/client/negative results, and relevant minecart/skip/cable code paths. The [Build #23](https://github.com/boxofdog/simple_block/actions/runs/38438092565) job log explicitly states **"All 152 required tests passed"**, with successful build and published `simple-block-mod` artifact. Claude's committed evidence reports **9/9 real-client tests and 17/17 negative controls**; ChatGPT did not re-run the game, inspect the Blockbench file in its GUI or personally execute those client tests.

**Confirmed code-path review:** Four real minecart walls now hinge in `GatedMinecartModel`, the end gates bottom-down and side gates top-up; `LoadingTracks` synchronizes gate direction; `SkipStations` accepts adjacent end-on or side-on rail geometry, governs direction by skip height, and unloads kitty-corner from the chute; `CableDrive.stop` permits a final approach only toward a genuine Loading Rail and otherwise keeps the standard endpoint guard; free-hook winching and visible cable travel are present in the code/evidence. The original Build #22 report remains historical. No claim is made that unrun scenarios have been validated.

**Outstanding for M07 playtesting and/or targeted regression:**
1. **Sequential coupled-train unloading:** One docked wagon can unload sideways while coupled, but under existing M06 train release behavior every wagon bypasses that rail until it has moved away. A complete train cannot yet be presumed to stop each following car in turn at the same station. Owner must decide desired sequential re-docking/advancing behavior before considering this workflow closed.
2. **Terminal rail approach cases NOT RUN:** curved rails, slopes, coupled consists, or partial chunk unload. Green CI does not establish behavior for these cases.
3. Generated `gated_minecart.bbmodel` has not been opened in Blockbench GUI; no new local GPU/human execution is claimed. Further desktop/multiplayer/mid-transfer persistence edge cases may be useful.
4. **Near-drum dock diagnostic:** the cart arrives on a rail ~2 blocks from a ground-level drum, but the winch may report `idle` rather than `docked`, although dock and lever neutrality operate correctly. Review in playtest for player-facing confusion.

**Other milestone dispositions:** M04, M05 and M06 are **SHIPPED by owner directive on 2026-10-13**, without changing this M07 acceptance gate. M08–M11 remain planned/not implementation-authorized. See [ROADMAP.md](../../ROADMAP.md) for the current milestone status and review receipts.
