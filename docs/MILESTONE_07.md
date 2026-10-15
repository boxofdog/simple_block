---
milestone: M07
status: PLAYTESTING
---

# M07 — Vertical Ore Skip, Freight Transfer and Four-Gate Minecart

**Status: PLAYTESTING; not owner-accepted or SHIPPED.** M07 began with the ore skip and four-gate cart, received BUG-003–005 playtest fixes, and now includes the **two-drum, one-cable** operating mode. Its [latest dated verification appendix](#implementation-and-verification--2026-10-14-1) covers Build #42 and remaining playtest checks. Earlier appendices document historical stages; their formerly open tasks are not automatically still open. This document grants no new implementation authorization.

**One authoritative M07 specification:** The original M07 brief, owner playtest corrections §§1–10, the later **universal four-gate minecart** design and the formerly separate post-patch finishing note are reconciled **here**. Later owner corrections take precedence over the original geometry/staging text. No second finishing pass, separate milestone report or patch-implementation gate is pending. The original patch text is preserved in the [historical correction archive](archive/milestone-patches/MILESTONE_07_PLAYTEST_PATCH.md) for provenance, **not active instructions**.

## Purpose and limits

Connect **mine → upright ore skip → existing freight minecart/Loading Rail** with the M05/M06 cable drum, coal winch, anchors and routed cable. The skip hoists and transfers real cargo; the visually opening cart and skip gates explain why the material can move.

**Out of scope:** another hoist motor, free power grid, compulsory shaft guides, swaying/pendulum physics, counterweights, new rail/station type, new freight item/entity, automatic return/shuttle, extra inventories, M08 furnace, M09 mobs, M10 workwear or M11 progressive hopper cargo fills. Preserve existing vanilla identities and accepted M01–M06 behavior except for owner-directed M07 integrations.

## 1. Ore skip: form, conditional bridle and storage

- The **upright skip** is **1 block wide × 1 long × 3 tall** in its body collision envelope: a two-block-high open-top ore bin over an **integral angled bottom chute** with sliding discharge gate. Preserve its existing sideways-minecart silhouette and Blockbench shape; **do not reshape the skip to compensate for incorrect station heights**. The art extends outside the logical 1×1 body footprint, so require **one clear lateral block on each side** of the body/travel path, plus safe door/chute and obstacle clearance. It never tilts or swings; it travels vertically only when hoisted.
- **Placed, not attached:** only the **three-block body** exists visibly and physically. **No fourth-block bridle collision or geometry**, no permanent hidden reserve above the unhooked skip.
- **Successfully hooked to cable:** a triangular **two-leg bridle** appears above the bin, meeting at **one apex** for one main cable. The newly added **fourth vertical block collides** and must fit at attachment and throughout travel. Failed attachment consumes nothing and leaves the skip unbridled. Detach/break the cable or support: remove the bridle and its collision. Sync/persist this conditional state and attached/free-hook state across reloading and clients.
- **Actual cargo:** one type of eligible bulk freight at a time, maximum **512 items** displayed as **one special overstacking cargo slot** in the skip inventory. The exception does **not** apply to normal 64-item inventories/cursors or the **256-item vanilla freight minecart**. Full/wrong-type refusal, partial pickup/deposit, UI/shift-click, broken entity/drops, multiplayer/save-load and interruption must conserve every item; never spill an intact 512-stack into ordinary storage.

**Shaped skip crafting recipe (one skip per craft):**

```text
[empty]       [Cable]         [empty]
[empty]       [iron trapdoor] [empty]
[iron ingot]  [minecart]      [iron ingot]
```

Use the existing Cable item and vanilla ingredients. Exact occupied cells matter; negative recipes with misplaced ingredients must fail. The skip is a **separate vertical carrier**, not a replacement freight-minecart entity.

## 2. One routed cable and a winched free hook

- Reuse M06's copper/wood anchors and existing manual/coal-powered drum. **One installed Cable Coil permits unlimited cumulative supported route length**, but **permanent horizontal segments remain at most 12 blocks between supports**; solid/route clearance rules still apply. Horizontal hand-carry becomes taut at **12 blocks from the last support** and automatically puts the loose hook **back at that support at roughly 16 blocks**; no extra messages or “hand slips” flavor text. These distances concern **horizontal separation, not shaft depth**.
- At a shaft's final ceiling anchor the **real loose hook hangs down before a skip is attached**. Drive the **existing manual or coal winch** to **FEED it down or PULL it up**; there is **no arbitrary 12/16-block vertical depth limit**. Players may retrieve the lowered hook locally at the bottom of a deep shaft, hook their skip without scaffolding and reel it up. Persist its actual paid-out length/height; picking it up locally must not measure its leash from the distant ceiling. No hooks/coils may be created, consumed or teleported by disconnect, chunk load or retry.
- The final ceiling anchor remains an **ordinary hanging roller/anchor** while the loose hook hangs or moves. **Only successful attachment to a skip** gives the same anchor the **ceiling-pulley visual**; detaching reverts it. M05 closed-loop return wheels on unrelated routes are unaffected. The bridle and pulley must not appear on failed attachment.
- Hoisted skips remain upright under the wheel. Lift/lower with existing powered/manual controls. **The currently delivered Build #24 lever behavior** for the relevant winch is **right-click PULL, left-click FEED; the same interaction stops that direction in NEUTRAL**. Preserve the released/blocked safety behavior, actual winch force/speed and fuel rules; don't resurrect superseded M06 control assumptions. Physical stops and Loading Rail docking halt safely, powered motion returns to NEUTRAL, and partial coal-burn remainder survives stop/restart and save/reload. Coal is consumed only for actual cable-driven movement; no automatic restart.
- **Visible cable travel is real:** the overhead run and vertical strand scroll in the correct direction/speed while a skip or dangling hook is winched, tied to actual drive phase and interpolated with the drum. Cable geometry follows the skip/hook and wheel; the texture **stops** when the mechanism does, without independent time-based motion, inverted pulley movement or double-drawn strands. No phantom travel through unloaded chunks; if geometry/entities cannot tick safely, stop and retain cargo, route, hook and fuel.

## 3. Loading Rail: physical two-way cargo transfer

Use the existing **Loading Rail** (registered ID `simple_block:loading_track`, not a new block). It docks and holds ordinary **uncushioned freight carts** and their coupled train, releases when the hinged inside-edge triangular stop is used, and prevents engines/winches from pushing through the lock. Every transfer requires a **genuinely docked and correctly oriented** freight cart, a **hung, cable-attached** skip with its valid bridle/pulley, proper alignment and an **unobstructed, visually plausible** route for the material.

| Position of docked cart | Direction | Real visible movement |
|---|---|---|
| Horizontally adjacent at the **upper bin/bridle intake level** | **Cart → skip** | The minecart's **real end gate hinges down/out** as a ramp to the open skip bin; top intake opens |
| Horizontally adjacent on the chute-facing side, **one rail/block level LOWER than the skip's bottom chute** (diagonally below / “kitty-corner”) | **Skip → cart** | Skip's sliding gate discharges over its chute, whose lip meets the cart's rim/open top; **cart gates stay closed** |

**Critical negative geometry:** A cart beside the bottom chute at the **same Y** must **NOT** receive material. For a skip bottom at `(x,y,z)`, chute-facing direction `F`, the eligible receiving cart/Loading Rail is near `(x+F.dx,y-1,z+F.dz)`, **not** `(x+F.dx,y,z+F.dz)`. Small measured contact offsets are engineering details; **do not solve alignment by reshaping the skip**.

The **geometric station position**, never the presence of cargo or empty space, decides transfer direction. Inventory type, count and free capacity then determine whether transfer is allowed. Wrong height, missing bridle, blocked ramp/chute, wrong facing, undocked/occupied passenger cart, incompatible material, full receiver or unavailable chunks yield **no imaginary transfer**. A valid receiving cart holds up to 256 and skip up to 512, with **incremental, partial, server-authoritative, item-conserving** movement; interrupt safely on release, disconnect, save/load, block break or missing capacity. Open only real gate geometry, close after transfer or interruption, and keep control/stop/fuel safety intact.

Both **end-on and side-on correctly docked carts** can meet valid skip geometry; a coupled wagon may unload sideways without uncoupling. That does **not** imply the following wagons automatically re-dock (see open M07 owner decision below).

## 4. Universal four-gate minecart and machinery models

- **Shared minecart chassis:** retain vanilla-textured recognizable floor, four corner posts and long-side fixed rims. The actual **four end/side walls are functional hinged gates**, not an extra flap over an unchanged solid vanilla box: both **end gates hinge downward/outward**; both **long-side gates hinge upward/outward**. A closed gate is a flush wall with no doubled faces or z-fighting; an opened gate leaves a **genuinely open end/side**. The cart variant facing the skip determines which gate opens. Ordinary passenger/cushioned minecarts and the Cobblestone Engine **keep their gates closed**. Preserve vanilla minecart item/entity identities, textures, collision and all existing freight/coupling rules. This four-gate owner correction supersedes the patch's earlier **freight-only single-end-wall rendering limitation**.
- **Cobblestone Engine remodel:** substantial pre-industrial furnace/firebox and prominent smokestack **inside the recognizable vanilla-minecart-bodied engine**, not a new locomotive chassis. Preserve fuel/traction, furnace-on-empty-cart conversion, controls, coupling, drops and identity. Provide editable Blockbench source, paintable UVs and real-client art checks.
- **Coal winch visual:** upgrading a manual drum with a furnace **replaces** the motor-side structural stand with a substantial furnace/motor integrated into the axle/frame. No little detached sidecar box or duplicate old stand. Manual state keeps its original stand and crank; upgraded state keeps the same drum, routes, lever and fuel/drive mechanics. Check ground and hanging mounting and cable clearance; editable Blockbench/UV assets are required. Owner paints final textures.

## 5. Cable terminal and Loading Rail approach

A cable-hauled freight minecart approaching a **real Loading Rail near the far/terminal cable support** must **reach, center and dock on that rail without laying meaningless extra cable beyond it**. Use a station-aware final approach without globally disabling normal `cable.end_clearance`, rod-grip, track-edge or unloaded-chunk safety. With **no** valid Loading Rail, stop at the last genuinely safe rail/cable position; never derail or teleport across a gap. Reverse/release, near-drum control and powered fuel stops remain functional.

## 6. Verification cases and owner playtest gate

| Area | Cases to preserve or investigate |
|---|---|
| Skip/recipe/inventory | Exact shaped craft; 1×1×3 body; conditional collidable fourth bridle; one commodity/512 slot, 513th/wrong-type refusal, no extra normal inventory capacity |
| Free hook and route | Hand-held 12/16 **horizontal** leash, permanent span ≤12; FEED/PULL unattached free hook >30 blocks down; ordinary anchor before hookup, ceiling pulley only after; one coil returned, no duplication |
| Direction and physical gates | End/side orientation, actual hinged open wall and clear ramp; kitty-corner **one level below** receives; same-height adjacency, obstruction, unattached skip and wrong-facing carts do **not** transfer |
| Cargo and recovery | Partially full/wrong-type source and receiver, interrupted transfers, saved/reloaded inventory and hook/bridle, removed stations, unloading chunks and dedicated-server/client synchronization |
| Drive and visuals | Powered/manual hoist, moving vertical + overhead cable while actually driven, true stop on NEUTRAL, safe coal remainder, grounded/hung winch models, unchanged engine behavior |
| Far/near rail ends | Last-support station docking **without** extra cable, no-station safe end, reverse, curved/sloped route, coupled consist, chunk boundary and near-drum stop diagnostic |

**Acceptance still pending:** [M11](MILESTONE_11.md) now supports cart-by-cart side-gate hopper unloading, but broader M07 train advancement/re-docking remains subject to the owner's integrated playtest and decision. Preserve SHIPPED M06 safety rules. Curved/sloped/consist and partially loaded-chunk approaches, desktop visuals and two-drum operation still warrant owner review; historical test gaps are documented in the appendices.

**Archival evidence:** the owner correction [patch as originally written](archive/milestone-patches/MILESTONE_07_PLAYTEST_PATCH.md) and the pre-consolidation [M07 report](archive/milestone-reports/MILESTONE_07_REPORT.md) preserve superseded staging, former shape assumptions, exact prior test plans and older builds. They do **not** override this unified M07 specification or the verification appendix below.

---

## Playtesting Changes

**Owner feedback — 2026-10-13 (after M09 Build #37); not implementation verification or acceptance.** The ore skip's hauling and cargo handling work extremely well in playtesting. M07 remains PLAYTESTING; defect details live in the bug log, while new operating requirements live here.

**Open bugs:** See [BUG-003–005 in the canonical bug log](BUG_LOG.md) for connecting-rod reach, incline travel and incline cable clipping, including symptoms, diagnostics, status and required tests. Do not duplicate their details here.

**Owner-directed changes (not bug-registry duplicates):**

1. **Loading Rail stop and ore transfers (owner-requested change):** **automatically lower the hinged rail stop after a minecart load/unload transfer completes**, so a still-docked receiving cart cannot inadvertently receive the same ore again when the hoisted skip is raised past it. Keep the cart held during the actual transfer and maintain all inventory/alignment/refusal safeguards. Clarify in implementation how this interacts with coupled carts and the separately open sequential-wagon redocking problem; do **not** undo M06's already-shipped safety rules or lower the stop on unsuccessful/refused transfer attempts.
2. **Two-ended winch — implemented, pending owner playtest:** a straight, single cable joins two drums, with control at either end; the original one-drum return loop remains available. The last command wins, NEUTRAL stops, fuel stays at the powered home drum, and one coil covers the line. This is manually commanded hauling, **not** an automatic shuttle. See [Build #42 verification](#implementation-and-verification--2026-10-14-1).

3. **Heavy suspended-skip drop (owner-directed):** when a suspended ore skip **loses its winch/cable attachment** (e.g., drum or support removal, route disconnect), it should **fall rapidly and visibly under gravity**, like a heavy anchor falling, then play a satisfying metallic **CLANG** **when it actually lands or strikes a solid obstruction**. Do not play the clang at the instant of disconnection or while still falling. The present implementation already settles an unhooked skip downward at a fixed `skip.fall_speed` of **0.25 blocks/tick** but has no landing impact sound; implement appropriately weighty fall acceleration/terminal speed and an impact event that works across client/server. Preserve collision-safe landing and all 512 cargo items; **do not turn detachment into deletion, duplication or automatic cargo spillage**. A skip deliberately unhooked while safely supported stays at rest. Test suspended disconnects, short and long falls, intermediate obstructions, supported unhooking, reload and multiplayer sound synchronization.

**Change 2 — owner decisions (2026-10-14) and what was built:** last command wins; fuel only at the home end; keep both line types. One drum is the existing loop with its return wheel, worked from the drum. Two drums is one cable from drum to drum, worked from either end. Hand-cranked, both drums have identical cranks. With a coal winch, the second drum gets the lever logic but not the coal engine. The route stays straight, and either drum can unhook the cable (owner, 2026-10-14). Built as `cable/FarDrums`: holding the cable end, use an empty drum to make the end fast to it. There is no return strand or wheel. Each end's controls work relative to that end, and the levers show the same setting, mirrored. Coal goes only in the winch, and the second drum refuses coal, a furnace or a coil. Sneak-using either drum, or breaking either drum or an anchor, unhooks the cable and leaves an open route. Verification is in the appendix below.

**Exploratory design option — not yet approved:** for cable inclines, keep the anchor's mounting base fixed while shifting/tilting its roller toward the uphill corner. Any adopted geometry must agree across rendering, route clearance and connecting-rod grip. Investigate the actual defects under [BUG-004](BUG_LOG.md) and [BUG-005](BUG_LOG.md) before selecting an implementation; this candidate does not establish the cause.

**Confirmed working — terminal Loading Rail approach:** In an actual owner playtest, a single cable-hauled freight cart approached a terminal Loading Rail with the **last cable anchor directly above the rail** and slid smoothly onto the station. The transition was unobtrusive and worked without extra cable beyond the last support. **This specific straight/single-cart end-of-line approach is owner-verified and needs no correction.** Curve/slope approaches, coupled-train approaches, partial-chunk conditions and sequential wagon redocking remain separate, unverified/open cases.

### Owner playtest follow-up — 2026-10-14 (unverified)

The owner reports additional issues and operating refinements in [BUG-006–008, BUG-013, BUG-018–021](BUG_LOG.md). Each bug's observed symptom and reproduction requirements remain in that single register; this section records the intended operating design, **not a completed implementation**.

- **Freight and controls (2026-10-15 lid addendum):** a correctly positioned cart should feed a skip from **either short end**. Make the skip's **top lid two separate doors opening outward to the left and right sides** of its receiving bin so neither front nor rear dumping path is obstructed; animate both doors opening for valid intake and closing on finish/interruption. Preserve the lower discharge-chute gate, conditional bridle, clearances and cargo conservation. Deliberate chain coupling should not accidentally place a chain above the rail, and the protruding winch lever must be targetable from useful oblique angles. Canonical detail and verification cases: [BUG-006](BUG_LOG.md#bug-006).
- **Coal winch inspection:** clicking the **engine box** should open a small coal-input inventory screen; the existing lever controls remain separate and reliable. Do not add a new fuel type or a second engine at the receiving drum.
- **Two-drum connection:** an empty receiving drum accepts a routed cable end whether it is grounded or hanging and regardless of receiver facing. One original coil suffices; **no coil is required on the receiver**, and attempting an attachment to an already coiled receiver must not create, lose, or spuriously eject cable materials. Maintain actual route obstruction, one-cable conservation and mirrored winch command semantics.
- **Selective routing and inclines:** using a held cable end on a chosen previously routed anchor should retract the appropriate existing section and leave a hanging end at that anchor, rather than forcing a full unroute. Investigate a **bounded**, physically credible lowering of an incline's receiving anchor/roller to clear local obstructions; the exact allowable extension remains to be tuned, and no solid-block bypass is authorized.

**Patch gate:** diagnose, implement, run focused client/server checks and obtain owner retesting. None of these observations changes M07's PLAYTESTING status or proves the behavior shipped.

**Implementation/acceptance gate:** These are fixes and a new owner-directed operating requirement, **not evidence they have been coded, tested or approved**. The earlier sequential coupled-wagon redocking issue remains open. Update verification with new build/test evidence after Claude implements a correction; **do not mark M07 SHIPPED** on the strength of the working skip alone.

## Implementation and verification — 2026-10-13

**Metadata:** M07 | PLAYTESTING | Ore skip, vertical hoisting, height-based Loading Rail transfers, owner playtest patch §§1–10 and the shared four-gate minecart.

**Implementation**
- Commits: `886f6f15fb7b9ad51c2a3b7c6bcf2087f1e72efa` engine and winch remodels; `a63013ab70912736c6054f2c2463cf6d8f320fb0` skip, hoisting, stations; `49d7add4a3793b1ac4bb61e1f7f54156eb4d5cbc` playtest patch and four-gate cart (report `dbda9560b5ccb9901eead06dc02c77f03837c936`); `ded41b3b8275f001ca511371841141e536fd4c9a` lever: right-click PULL, left-click FEED
- Delivered: conditional bridle and ceiling pulley; free hook winched down deep shafts; kitty-corner unloading and bridle loading, end-on or side-on; terminal Loading Rail docking; moving hoist cable.
- Deviations: Owner direction replaced the vanilla-cart body rule with the four-gate cart for every minecart.

**CI**
- Build: #24 | Run ID: 38443850502 | PASS
- Source SHA: `ded41b3b8275f001ca511371841141e536fd4c9a`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38443850502
- Artifact: `simple-block-mod` id 11837962048, available. Build #23 (run 38438092565) tested `49d7add`, PASS, artifact 11834545069; `dbda956` is docs-only.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 152/152 | local on `49d7add` and `ded41b3`; [evidence](evidence/milestone-07-patch/server-tests-and-controls.md) |
| Client | PASS | 9/9 | on `49d7add`; after `ded41b3` only `FreightStationClientTest` re-run; [log](evidence/milestone-07-patch/client-test-log.txt) |
| Dedicated server | PASS | 1/1 | `SkipServerClientTest` |
| Negative controls | PASS | 17/17 | [controls](evidence/milestone-07-patch/server-tests-and-controls.md) |

**Known gaps**
- GAP-01: MAJOR — a coupled train cannot unload car by car at one side-on rail after release; next: owner decision.
- GAP-02: MINOR — terminal approach on curves, slopes, coupled trains and partial chunk unload NOT RUN; next: targeted tests.
- GAP-03: MINOR — near-drum docking reports `idle`, not `docked`; next: assess in playtest.
- GAP-04: MINOR — `gated_minecart.bbmodel` not opened in Blockbench; next: open and compare.
- GAP-05: MINOR — full client suite not re-run after `ded41b3`; next: run `runClientGameTest`.

**Owner playtest**
- Winch the free hook down a deep shaft, hook a skip, and see the bridle and pulley appear.
- Load a cart at the bridle and unload into a cart one level below the chute.
- Unload a coupled car side-on at a Loading Rail.
- Dock a winched cart on a Loading Rail under the last support.
- Drive the lever: right-click PULL, left-click FEED, same click NEUTRAL.

## Implementation and verification — 2026-10-14

**Metadata:** M07 | PLAYTESTING | Owner playtest fixes: BUG-003–005, Loading Rail release after an exchange, heavy skip drop; change 2 proposed only.

**Implementation**
- Commits: `9cdadf7269d1e288f06dd79058627ce79fab1901` playtest fixes; `e240d807a1ede1d070db32693b9868d6dc749fcd` M09 test stabilization (CI head)
- Delivered: rod reach 1–6 blocks; carts hauled up climbs between level anchors; steep spans drawn under the anchor's block; a finished exchange lets that cart go and folds its stop; an unhooked skip accelerates down and clangs on landing only, 512 cargo kept.
- Deviations: Change 2 (two-ended winch) not built; proposal above awaits owner decisions. Release applies to the exchanging car only; the sequential-redocking question (GAP-01 of 2026-10-13) is unchanged.

**CI**
- Build: #39 | Run ID: 38522399370 | PASS
- Source SHA: `e240d807a1ede1d070db32693b9868d6dc749fcd`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38522399370
- Artifact: `simple-block-mod` id 11874229564, available. Build #38 (run 38516998674) on `9cdadf7` failed only JOCK-03 (M09), fixed in `e240d80`.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 195/195 | local on `e240d80`; Build #39 log: all 195 required tests passed; [evidence](evidence/milestone-07/playtest-fixes-2026-10-14.txt) |
| Client | PASS | 12/12 | full suite on `9cdadf7`, incl. `PlaytestFixesClientTest` |
| Dedicated server | PASS | 1/1 | `PlaytestFixesClientTest` dedicated pass: cable bend, rod grip, clang |
| Negative controls | PASS | 3/3 | old reach, no release, steady silent fall: 5 tests failed as expected; [evidence](evidence/milestone-07/playtest-fixes-2026-10-14.txt) |

**Known gaps**
- GAP-01: MAJOR — two-ended winch not implemented; next: owner answers proposal (a)–(c).
- GAP-02: MINOR — BUG-005 checked from one angle by test; next: owner views several angles.
- GAP-03: MINOR — climbs tested on one 4-block slope; next: playtest steeper and curved climbs.
- GAP-04: MINOR — rare M09 full-suite flakes (`open_door_draws_them_in`, `breach_redistributes`), pass alone; next: watch CI.

**Owner playtest**
- Hang anchors 4–5 blocks over a rail; confirm the rod grips.
- Haul a cart up a slope under level anchors.
- View an incline cable under its anchor block from several angles.
- Finish a skip exchange, wind the skip on; confirm the cart is released and not refilled.
- Break a hung skip's drum; watch the drop and hear the landing clang.

## Implementation and verification — 2026-10-14

**Metadata:** M07 | PLAYTESTING | Playtest change 2: two drums and one cable, worked from either end; the one-drum loop kept.

**Implementation**
- Commits: `cf081601b846657355b8738d2d6bfe14b2c6379e` two-drum lines; `febfe33c9cede18f87085a326f2aca18138ce17c` shorter attach messages; `1c8434b7adc9b7cac505f007cd38333a3ac54255` HOST-06 test bound (CI head)
- Delivered: the held cable end is made fast to an empty second drum (straight route, no return wheel). Each drum's crank or lever works from its own end, and the last command wins. A coal winch's second drum shows the lever, mirrored, with no motor. Coal goes only in the winch. Sneak-using either drum, or breaking either drum or an anchor, unhooks the line, including while the first drum is unloaded.
- Deviations: NONE; the earlier proposal (a fitting on the return anchor) was replaced by the owner's two-drum decision.

**CI**
- Build: #42 | Run ID: 38526791742 | PASS
- Source SHA: `1c8434b7adc9b7cac505f007cd38333a3ac54255`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38526791742
- Artifact: `simple-block-mod` id 11876078125, available. Build #40 (run 38525185843) on `cf08160`: PASS; Build #41 on `febfe33` failed only the M09 HOST-06 flake, bounded in `1c8434b`.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 198/198 | local on `cf08160`; Build #42 log: all 198 required tests passed; [evidence](evidence/milestone-07/two-drums-2026-10-14.txt) |
| Client | PASS | 13/13 | full suite on `cf08160`, incl. `TwoDrumClientTest`; [screenshots](evidence/milestone-07/screenshots/) `m07twin-*` |
| Dedicated server | PASS | 1/1 | `TwoDrumClientTest` dedicated pass: cable onto the second drum, mirrored levers, cart hauled |
| Negative controls | PASS | 3/3 | no attaching, no mirroring, no straight check; [evidence](evidence/milestone-07/two-drums-2026-10-14.txt) |

**Known gaps**
- GAP-01: MINOR — unhooking while the first drum is unloaded is untested; next: long-line test.
- GAP-02: MINOR — the second drum's barrel uses the wound-spool model; it does not wind or unwind visibly; next: owner view.
- GAP-03: MINOR — two-drum lines must be straight (owner rule); next: playtest.

**Owner playtest**
- Run a drum's cable through anchors and use a second empty drum with the end.
- Crank from each drum; check right-click pulls the cart towards that drum.
- Fit a furnace to the first drum; work the lever from the second drum.
- Try coal or a furnace on the second drum; confirm both are refused.
- Sneak-use either drum; confirm the end returns to the last anchor.
