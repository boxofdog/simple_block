# Milestone 04 report: Cable infrastructure

**HISTORICAL REPORT (M01–M07 format).** This record is preserved for its original implementation and tests. References to the retired decisions ledger now point to the dated archive, **not active implementation authority**. Under the current [roadmap rule](ROADMAP.md), new authorized work appends only a **concise dated implementation/verification note at the bottom of the main milestone document**, not a new standalone report.

Scope: the owner's brief, recorded verbatim in `docs/MILESTONE_04.md`. Implemented and tested in a server and in a real client; not playtested by a person yet.

- **Tests:** 95 required server game tests pass in `./gradlew build` and in CI (Build #16): the 85 from Milestones 01-03 plus 10 new ones in `CableGameTests`, which also passed three extra filtered runs.
- **Negative controls:** with the M04 rules removed (anchors on any block, no span limit, two cables per anchor, no grip, no obstacle check), the matching tests fail; see section 6.
- **Dedicated server:** starts with the M04 code.
- **Real client:** a new client test, `CableClientTest`, builds a short line in a fresh world and drives every component with real key presses: drum, coil, anchors, picking up and dragging the end, routing, a rod on a rolling minecart, save and reopen, disconnecting and dismantling. It passes, and the earlier `SurvivalSmokeClientTest` still passes alongside it.
- **Assets:** editable Blockbench sources for every model, with UV-template textures, in `art/blockbench/` (section 4).
- **Not built (by the brief):** engines, counterweights, inclined-lift automation, skips, locomotives. **Also not built:** anything that moves the cable or pulls a cart (section 2).
- **Open design questions** for you are in section 7 and in `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md`.

Evidence is in `docs/evidence/milestone-04/`:
- [`gametest-evidence.md`](evidence/milestone-04/gametest-evidence.md): the numbers each test logged, server and client.
- [`negative-controls.md`](evidence/milestone-04/negative-controls.md)
- [`baseline-build.txt`](evidence/milestone-04/baseline-build.txt)
- `screenshots/` (real client).

## 1. Baseline

- **Toolchain:** Minecraft 26.3 (unobfuscated), Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25.
- **Starting point:** `main` at `a36d9eb` (cushions in minecarts). `./gradlew build` passed 85/85 there.
- **Existing code:** the mod had no blocks, items, entities, block entities or assets of its own before M04. The minecart already carried M03 state (cargo, cushion), which the rod joins.

## 2. Reading of the brief, and what M04 deliberately leaves out

The brief asks for placement, routing, anchors, "the winch structure" and "minecart cable engagement", and rules out engines, counterweights, lifts, skips and locomotives. I read that as: **nothing moves**. The drum is a winch in structure (it holds and pays out cable), but nothing turns it; the cable never runs; a rod that grips the cable does not pull or brake the cart. Engagement is a real state, computed by the server every tick and shown on the cart, so the next milestone can make a moving cable pull the carts that grip it. If you meant M04 to include hand-cranking the winch or carts being pulled, that is the first thing to tell me.

Where the brief left rules open, I chose the narrowest workable rule and recorded it in `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` ("Milestone 04 implementation choices"). The main ones:

| Brief | Implemented |
|---|---|
| Cable craftable, combined into coils, finite | 1 cable = 4 blocks; 3 iron nuggets + string make 2 cables; 8 cables in a ring make a coil (32 blocks), and a coil goes back into 8 cables |
| A coil applied to the drum makes it a winch | Each coil adds 32 blocks; a drum holds 128 (tuning `cable.drum_capacity`). The barrel shows cable once there is any |
| A visible loose end with a hook | An entity owned by the drum; it lies at the foot of the drum, or hangs just under the last anchor once there is a route. Not saved: the drum puts it back after loading |
| Pick it up and drag it like a lead | Use the hook. It follows the hand; it slips out of the hand if you walk further than the cable could reach (the longest span or the cable left, whichever is less, plus 3 blocks), and on death, leaving or changing dimension |
| Route through anchors, consumed by length | Each span from the last support to the anchor is a straight line, at most 12 blocks (tuning `cable.max_span`), clear of solid blocks, and uses its length rounded up to a whole block |
| Extend, disconnect, dismantle | Extend: wind on more coils and carry on from the last anchor. Disconnect: sneak + use an anchor (empty hands), or break it: the cable from there on winds back onto the drum. Dismantle: sneak + use the drum, first to wind everything in, then to take it off as coils; or break the drum, which drops all its cable as coils. Cable is never lost or made |
| Runs, turns, elevation | Any anchor within a clear 12-block line works; the route can turn and climb at every anchor |
| Anchor only under wooden blocks | `#simple_block:cable_anchor_supports` (logs/wood/stems, planks, wooden slabs, stairs, fences, bamboo) and the block must offer the middle of its underside, as a hanging lantern needs |
| Rod auto-engages when passing beneath the cable | Like a tram's trolley pole: it grips any route cable within 0.5 blocks sideways of the cart and 1.5 to 3 blocks above the rail (tuning), stays gripped across anchors, lets go where the cable ends |
| Pass supports without snagging | Neither the anchors nor the rod collide with anything, so nothing can snag in the game; the models give the rod's head and the anchor's arm opposite sides, and the test checks a gripping cart rolls exactly like one with no cable |

## 3. What changed

| Part | Files |
|---|---|
| Registration | `cable/CableContent` (blocks, block entities, items, the hook entity, creative tab entries, fixed lengths); called first in `SimpleBlock.onInitialize` |
| Anchor | `cable/CableAnchorBlock`, `cable/CableAnchorBlockEntity` (which drum's cable it carries); `data/simple_block/tags/block/cable_anchor_supports.json` |
| Drum | `cable/CableDrumBlock`, `cable/CableDrumBlockEntity` (cable wound on, route, saving, client sync) |
| Routing | `cable/CableRoutes` (all the rules: winding on, using drums and anchors, spans, disconnecting, breaking, checking a loaded route); `cable/CableHook` (the loose end); `cable/CableIndex` (laid-out spans per level, for the rod) |
| Rod | `cable/ConnectingRods`, `cable/ConnectingRodPartBlock` (model carrier only); `mixin/minecart/MinecartMixin` (rod and grip as two more synced values, the tick hook, the use cascade, drop and save) |
| Data | recipes and recipe advancements in `data/simple_block/{recipe,advancement}`; loot tables for the drum and the anchor; `data/minecraft/tags/block/mineable/{axe,pickaxe}.json`; `simple_block/heavy_weights.json` (cable 1/6 iron, coil 4/3, drum 2; anchor 1/6 copper, rod 19/9) and `tools/heavy-weights/` (now takes the mod's own recipes too); `tuning/Tuning.java` (4 `cable.*` keys); `lang/en_us.json` |
| Client | `client/cable/CableDrumRenderer` (spans), `client/cable/CableHookRenderer` (hook and loose cable), `client/cable/CableGeometry` (the cable tube), `client/CargoMinecartRenderer` (the rod), `SimpleBlockClient` |
| Assets | `art/blockbench/*.bbmodel` and its README; `assets/simple_block/{models,textures,blockstates,items}`; `tools/models/generate_models.py` (one-off scaffold, never overwrites) |
| Tests | `CableGameTests` (10 tests), `gametest/structure/cable_yard.snbt` (a 30 x 10 x 10 empty test structure), `client/CableClientTest` |
| Docs | this report, `docs/MILESTONE_04.md` (the brief), `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` (M04 sections), README (M04 table), `CLAUDE.md` (points at M04), `SimpleBlock` Javadoc |

## 4. Models and textures (for painting)

Every model has an editable source in `art/blockbench/`, opened in Blockbench as a Java Block/Item project. `art/blockbench/README.md` lists them, explains how saving the texture in Blockbench writes the game's file directly, and lists the points the code relies on if you reshape a model.

- **Textures are UV templates, not artwork:** each cube's faces are flat colour blocks (lighter on top, darker below) with a darker outline, so you can see what lands where. No finished texture was made.
- **Models:** anchor (mounting plate, one-sided hanger arm, flanged roller); drum, bare and wound (timber side frames, octagonal spool flanges and barrel); hook; connecting rod in parts (base, a one-block shaft stretched to length, the head open and closed) plus the rod item; cable and coil items (loops of cable).
- **Drawn by code, not models:** the cable between supports and the loose cable to the hook, a thin square tube textured with `textures/entity/cable.png` (each side shows the whole texture, along one block of cable).
- **Placement detail:** the anchor's roller axis comes from the direction you face when placing it (face along the cable). Only the axis matters, so the arm is always west of a north-south cable and north of an east-west one; the rod is always drawn on the other side.

Screenshots of the template-textured models in the game: `cable-drum-wound-hook.png`, `cable-anchors-hung.png`, `minecart-rod-fitted.png`, `minecart-rod-gripping.png`.

## 5. Test matrix

GT = server game test in `CableGameTests` (fresh test world, stand-in players, every action through the server's own packet handlers). Client = `CableClientTest` in the real client, with key presses.

| Test | Result | Method and evidence | Limitations |
|---|---|---|---|
| CAB-01 recipes | PASS | GT `cableComponentsAreCraftable`: each of the 6 recipes crafts from a 3 x 3 grid through the game's recipe manager; the drum with its top row empty and mixed planks; an upside-down drum layout crafts nothing | Crafting by hand in the client: NOT RUN |
| CAB-02 anchors only under wood | PASS | GT `anchorHangsOnlyUnderWood`: placed by a player on the underside of 9 wooden blocks (planks, logs, stripped log, stem, bottom slab, stairs, fence, bamboo block, bamboo mosaic); refused under stone, iron, bricks, glass, leaves, a top slab, upside-down stairs, on a floor and on a wall; drops as an item when its plank is removed. Client: 3 hung under planks with the use key, refused under stone | Every wooden variant is covered by the tag, but only these 9 are placed in the test |
| CAB-03 drum and coil | PASS | GT `coilMakesTheDrumAWinch`: bare, then wound with 32 blocks per coil up to 128; a 5th coil is refused and stays in hand; one loose end appears at the drum. Client: placed facing the player, coil wound on, the client sees the hook | |
| CAB-04 pick up and route | PASS | GT `cableRoutesThroughAnchorsAndUsesItsLength`: anchors only report until the end is picked up; then the cable runs drum → A → B → C → D. Client: picked up the hook with the use key, routed through 3 anchors, put down on the last | |
| CAB-05 consumed by length | PASS | Same GT: spans used 8, 10, 8 and 4 blocks (their lengths rounded up), not one per anchor; wound plus laid out stays 32. Client: 25 laid out + 7 left = 32 | |
| CAB-06 runs, turns, climbs | PASS | Same GT: a straight 10-block run, then a span that turns 4 blocks sideways and climbs 2 | |
| CAB-07 refusals | PASS | Same GT: a 16.5-block span is refused (limit 12); a span through a stone block is refused, then accepted once the stone is gone; a 6-block span with 2 blocks left is refused | |
| CAB-08 one cable per anchor | PASS | GT `anchorCarriesOneCable`: a second drum's end is refused at an anchor carrying the first drum's cable; a player holding one end can't pick up another | |
| CAB-09 extend | PASS | GT `routesExtendDisconnectAndDismantle`: on one coil A-D are laid and E is refused; a second coil wound on while holding the end; E then routes | |
| CAB-10 disconnect | PASS | Same GT: sneak-use C winds C, D and E back (23 blocks) and frees them; breaking B winds back to A. Client: sneak-use of the second anchor leaves the route at the first (16 blocks back) | |
| CAB-11 dismantle | PASS | Same GT: sneak-use of the drum winds the rest back (64 on the drum), the next gives 2 coils and leaves a bare drum with no hook; breaking a drum with a route drops 2 coils and the drum and frees its anchor. Client: rewound, then 1 coil out, bare drum | |
| CAB-12 dragging like a lead | PASS | GT `heldEndFollowsTheHandAndSlipsBeyondReach`: held 7 blocks out, 0.25 blocks from the hand; 18 blocks out it slips back to the drum; it is put down when the holder leaves the game. Client: screenshot of the end in hand, and dragged to the last anchor | Death and dimension change use the same check as leaving; not tested separately |
| CAB-13 saved and checked on loading | PASS | GT `routeIsSavedAndCheckedOnLoading`: the route and wound cable are saved with the drum; an anchor removed without the drum knowing is found on loading: the cable winds back to the anchor before it and frees the one after. **Client: real save, quit and reopen: the route is back and the loose end hangs under the last anchor again, on server and client** | |
| CAB-14 rod fitting | PASS | GT `connectingRodFitsAMinecart`: fitted (a second refused); the cart still takes cargo; sneak-use unloads the cargo first, then takes the rod off; saved with the cart; dropped when the cart is broken. Client: fitted with the use key | |
| CAB-15 rod grips the cable | PASS | GT `rodGripsTheCableAndPassesAnchors`: a cart with a rod on powered rails under a 3-anchor route grips on every one of 40 ticks between the first and last anchor (reach 2.69 blocks) and lets go past the last. Client: frozen between the first two anchors, the rod grips at 2.69 on server and client and is drawn stretched to the cable; past the last anchor it has let go | |
| CAB-16 passes supports without snagging | PASS | Same GT: the cart under the cable and a control cart with a rod on a track with no cable stay at exactly the same position every tick until they pass the last anchor | Nothing can snag because nothing collides; whether the *shapes* would clear in reality is a design question (section 7) |
| CAB-17 server authority | PASS | Every rule runs on the server; clients get the route, the hook's holder and the rod's grip by sync. The client test compares server and client route, hook position and grip. Dedicated: `runServer` starts with the cable content registered ("Cable: 4 blocks per cable, 32 per coil", "Done (0.278s)") | Remote client on a dedicated server: NOT RUN |
| CAB-18 M01-M03 regression | PASS | All 85 earlier game tests pass in the 95-test build (four local full builds and CI; see the note below); `SurvivalSmokeClientTest` passes (bed, shulker, movement, elytra, inventory, light, creeper, food, minecart cargo, cushion, storage) | One of the four local full builds had the M02 test `pursuingZombieBreaksOakDoor` time out waiting for the door; it passed 3 of 3 on its own and in the next full build. M04 touches no zombie, door or pathfinding code |
| CAB-19 build, artifact | PASS | `./gradlew build` 95/95 locally (four full builds, one with the door flake below); CI Build #16 green on `215a24a` and published `simple-block-mod`; the jar carries the models, textures, block states, recipes and tags | Software renderer in the client test, not a desktop GPU |

## 6. Negative controls

Per `CLAUDE.md`, the restriction tests were run against copies of the tree with the rule removed (details and output: `evidence/milestone-04/negative-controls.md`):

| Rule removed | Test that fails |
|---|---|
| Anchors need a wooden support (only the "underside" check kept) | `anchorHangsOnlyUnderWood` |
| Spans at most 12 blocks | `cableRoutesThroughAnchorsAndUsesItsLength` |
| One cable per anchor | `anchorCarriesOneCable` |
| The rod grips (its tick does nothing) | `rodGripsTheCableAndPassesAnchors` |
| Spans must be clear of solid blocks | `cableRoutesThroughAnchorsAndUsesItsLength` |

The other cable tests pass in both controls, so each failure is specific to the rule removed.

## 7. Decisions that need you

All are in `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` ("Milestone 04 design questions left open"):

1. **Does M04 stop short of movement?** (Section 2.) I assumed yes.
2. **How the grip holds the cable past a roller.** The head grips from above, because a jaw closed under the cable would hit each roller. A real grip needs a jaw that opens at supports, or supports shaped around the grip.
3. **Two tracks, diagonal cables.** The anchor's arm is always on one fixed side and the rod's stem on the other. A second track beside the first, or a cable running diagonally, has no rule yet.
4. **Turns.** At a turn the cable cuts the corner in a straight line while the track curves, so on tight curves the grip lets go and takes the cable again after the turn. Real haulage uses curve pulleys: should there be a curve anchor?
5. **Slack, sag, tension:** none. The cable is drawn taut and straight.
6. **The numbers:** 4 blocks per cable, 32 per coil, 128 per drum, 12 per span, 1.5-3 blocks of rod reach, and the recipes are starting values with no playtest behind them.
7. **What anchors may hang from:** only wood (as the brief says). Whether iron or a wall bracket should ever carry one is open.

## 8. Known gaps

- **Feel:** nobody has laid a long route by hand yet. Carrying the end across 12-block spans, the 3-block slack before it slips, and clicking the drum while the end lies at its foot all need a playtest. (The client test found that a hook resting *in front of* the drum's middle intercepts clicks meant for the drum; it now lies at the drum's foot.)
- **Rendering:** the cable is a straight tube without sag. The rod is drawn upright from the cart's position whatever the cart's tilt on a slope, and always on the fixed side.
- **Large routes:** a drum checks its route when loaded, once every anchor's chunk is loaded too. An anchor broken while its drum's chunk was unloaded is found then; until it is, its cable is still drawn.
- **Hand crafting in the client, a dedicated server with a remote client, two real players at once:** NOT RUN for M04.
- **A timing-sensitive M02 test:** `MobGameTests.pursuingZombieBreaksOakDoor` (a zombie chasing a player through an oak door) timed out once in a full build while many tests ran at once, and passed 3 of 3 alone and in the next full build. Not investigated further; if it fails in CI the Build can be re-run.

## 9. Build, run and update

| Command | What it does |
|---|---|
| `./gradlew build` | Jar plus all 95 server game tests |
| `./gradlew runClient` / `runServer` | Dev client / dedicated server |
| `XDG_RUNTIME_DIR=/tmp/xdg xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` | Both real-client tests (smoke, then cable) |
| `JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dfabric-api.gametest.filter=simple_block-gametest:cable_game_tests_*" ./gradlew runGameTest` | Just the cable tests |
| `python3 tools/models/generate_models.py` | Re-creates missing model files only (see `art/blockbench/README.md`) |

Playtest builds: pushing to `main` publishes the jar through the Build workflow, and `tools/windows/Update-SimpleBlock.bat` installs it as before. In creative, everything is in the Tools & Utilities tab after the minecart. In survival: craft cable from iron nuggets and string, a coil from 8 cables, a drum, anchors from copper nuggets and a rod.

## 10. Next

The roadmap's next cable step is making it move: power for the winch (by hand first, or the deferred engine) and carts pulled by the rod's grip. Before that, sections 7.1-7.4 decide what "gripping" must survive.

---

## Status disposition, 2026-10-13 (later owner directive)

**M04 — SHIPPED.** The owner explicitly signed off on M04, M05 and M06 after an independent GitHub review (the former status label was PASSED). ChatGPT inspected the M04 report/code lineage and independently confirmed [Build #16](https://github.com/boxofdog/simple_block/actions/runs/38197364425) was green with a published `simple-block-mod` artifact. This is **owner-directed acceptance**, not a claim of a new standalone human M04 playtest or local assistant-run client session. Previous open acceptance notes in the original report are a historical snapshot; the authoritative live status is [ROADMAP.md](ROADMAP.md). M04's originally static cable foundation remains the shipped baseline later extended by M05–M07.
