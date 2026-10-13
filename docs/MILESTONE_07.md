# M07 — Vertical Ore Skip, Freight Transfer and Four-Gate Minecart

**Status: PLAYTESTING (2026-10-13).** M07 is **implemented and handed to the owner for playtesting**, not owner-accepted or SHIPPED. The latest M07-relevant implementation is code `49d7add` (Build #23), followed by winch-lever adjustment `ded41b3` (Build #24). The dated [verification appendix](#implementation-and-verification--2026-10-13) below holds code SHAs, CI, test evidence and remaining gaps. **This consolidation is documentation-only and authorizes no new gameplay work.**

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

**Status remains PLAYTESTING, not SHIPPED.** In particular, owner must decide **sequential wagon advancement/re-docking** after M06's released-train latch: one docked side-on wagon currently unloads, but subsequent wagons pass that rail until they have left its release region. **Do not silently change SHIPPED M06 mechanics.** Also still to be checked or not fully established: curve/slope/consist/chunk terminal approaches, full post-Build-24 real-client regression, GUI-open of `gated_minecart.bbmodel`, desktop owner multiplayer/playtest and the minor near-drum `idle` versus `docked` reason.

**Archival evidence:** the owner correction [patch as originally written](archive/milestone-patches/MILESTONE_07_PLAYTEST_PATCH.md) and the pre-consolidation [M07 report](archive/milestone-reports/MILESTONE_07_REPORT.md) preserve superseded staging, former shape assumptions, exact prior test plans and older builds. They do **not** override this unified M07 specification or the verification appendix below.

---

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
