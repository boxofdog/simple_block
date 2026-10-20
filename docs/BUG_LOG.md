# Simple Block — Bug log

**Canonical bug record.** Keep symptoms, evidence, diagnosis and fixes here; milestone files link bug IDs and own their separate verification appendices. **A successful automated fix is not owner playtest acceptance.** The statuses below remain unchanged by this readability edit. [UX wording issues](UX_TEXT_LOG.md) are tracked separately; filing a bug does not authorize an unrelated scope change.

## At a glance

| ID | Area | Status |
|---|---|---|
| BUG-001 | [M04 cable drum — side geometry](#bug-001) | **RESOLVED in M05 |
| BUG-002 | [M06 coal winch — cannot reverse from near track endpoint](#bug-002) | **FIX IMPLEMENTED AND TESTED** in M06 `fc8f99a`, Build #20 plus Build #23 regres |
| BUG-003 | [Cable-grip connecting rod / upper anchor height (M07 playtest)](#bug-003) | **OPEN — fix implemented and tested |
| BUG-004 | [Cable-driven cart on inclining route (M07 playtest)](#bug-004) | **OPEN — fix implemented and tested |
| BUG-005 | [Inclined cable visual geometry (M07 playtest)](#bug-005) | **OPEN — fix implemented and tested |
| BUG-006 | [M07 cart → skip transfer — rear end](#bug-006) | **OPEN — fix implemented and tested in M13 (`ba68617`); awaiting owner retest** |
| BUG-007 | [Minecart coupling — chains above rails](#bug-007) | **OPEN — fix implemented and tested in M13 (`7168b80`); awaiting owner retest** |
| BUG-008 | [Coal winch lever — inaccessible oblique hitbox](#bug-008) | **OPEN — fix implemented and tested in M13 (`80ecf37`); awaiting owner retest** |
| BUG-009 | [M10 Spelunker's Rope — rope and coil geometry](#bug-009) | **REQUESTED — implemented and tested in M13 (`15d55a7`); awaiting owner review** |
| BUG-010 | [M10 Overalls — weight bar hidden](#bug-010) | **OPEN — fix implemented and tested in M13 (`95e955a`); awaiting owner retest** |
| BUG-011 | [M08 Blast Furnace — casting light](#bug-011) | **REQUESTED — implemented and tested in M13 (`960926a`); awaiting owner review** |
| BUG-012 | [M08 Blast Furnace — molten cinders](#bug-012) | **REQUESTED — implemented and tested in M13 (`960926a`); awaiting owner review** |
| BUG-013 | [M06/M07 coal winch — engine-box coal GUI](#bug-013) | **REQUESTED — implemented and tested in M13 (`4c0cf56`); awaiting owner review** |
| BUG-014 | [M11 freight minecart — read-only cargo GUI](#bug-014) | **REQUESTED — implemented and tested in M13 (`2d99113`, `e8b052a`); awaiting owner review** |
| BUG-015 | [M08 Blast Furnace — z-fighting](#bug-015) | **OPEN — fix implemented and tested in M13 (`d290c2d`); awaiting owner retest** |
| BUG-016 | [M10 Trail Blazer — ghost highlight silhouette](#bug-016) | **REQUESTED — implemented and tested in M13 (`2bcd197`); awaiting owner review** |
| BUG-017 | [M10 Old Pro — off-hand pickaxe mining](#bug-017) | **OPEN — not reproduced in M13 (`292fcb5`): works with the full outfit; awaiting owner retest** |
| BUG-018 | [M07 two-drum cables — ground/hanging attachment and coil ownership](#bug-018) | **OPEN — fix implemented and tested in M13 (`a7b297a`); awaiting owner retest** |
| BUG-019 | [M07 two-drum cables — receiver facing restriction](#bug-019) | **OPEN — fix implemented and tested in M13 (`a7b297a`); awaiting owner retest** |
| BUG-020 | [M07 cable anchors — selective mid-route detach](#bug-020) | **REQUESTED — implemented and tested in M13 (`13bb744`); awaiting owner review** |
| BUG-021 | [M07 cable anchors — inclined-span obstruction clearance](#bug-021) | **REQUESTED — implemented and tested in M13 (`635efff`); awaiting owner review** |
| BUG-022 | [M07 Ore Skip — z-fighting / overlapping rendered faces](#bug-022) | **OPEN — fix implemented and tested in M13 (`d290c2d`); awaiting owner retest** |

## Symptoms, evidence and verification

### BUG-001

**Area:** M04 cable drum — side geometry

**Observed:** **Ghosting / flickering stripe-like texture artifacts** on the side of the drum/frame, particularly around the angled side panels.

**Diagnosis / fix / checks:** **Cause:** the octagonal flanges are four overlapping cubes whose end faces lay in the same plane with different texture regions (z-fighting). **Fix (M05, `e662ec2`):** band ends set back 0.1-0.2 px in `tools/models/generate_models.py`; models regenerated. **Verified visually** in the real client from four angles: [before](evidence/milestone-05/screenshots/bug001/) stripes at every angle, after clean (`before-*.png` / `after-*.png`).

**Status:** **RESOLVED in M05 (visually verified on the test client's software renderer; owner check on a desktop GPU still welcome)**

### BUG-002

**Area:** M06 coal winch — cannot reverse from near track endpoint

**Observed:** **Owner playtest (2026-10-13):** with a gripped freight cart at the *near/winch end* of the track, attempts to select FEED instead repeatedly select PULL; the lever immediately returns to NEUTRAL and the cart is stuck until broken/reset. Normal cycling works away from the endpoint.

**Diagnosis / fix / checks:** **Likely cause, source-inspected (not yet independently reproduced):** `CoalWinch.cycleLever` always advances `NEUTRAL → PULL → FEED`, while `CableDrive.stop` reports `track_end`/`end` for impossible PULL motion and `tickWinch` immediately forces NEUTRAL. The next right-click therefore selects PULL *again*, so the operator can never select FEED to move away. **Fix requirement:** retain safe automatic stop/no fuel burn at genuine obstructions, but always allow the player to select the *opposite, physically unobstructed* direction without breaking/dismantling the cart. Claude to choose a simple, legible control fix; test both endpoints, repeated interactions, reversals, unloading and dock behavior in real client plus regression tests. Do not solve by allowing movement off the track or by deleting the safety stop.

**Status:** **FIX IMPLEMENTED AND TESTED** in M06 `fc8f99a`, Build #20 plus Build #23 regression. **M06 SHIPPED 2026-10-13**; no separate human re-reproduction asserted. Original owner-observed defect and investigation text retained for provenance.

### BUG-003

**Area:** Cable-grip connecting rod / upper anchor height (M07 playtest)

**Observed:** **Owner observed:** a minecart connecting rod fails to connect to the routed cable when the vertical separation is greater than roughly three blocks. Needs another 1–2 blocks of usable height flexibility.

**Diagnosis / fix / checks:** Reproduce at successive anchor/cable heights (including 3, 4 and 5 blocks); distinguish reach/attachment rules from model appearance; preserve physical coupling and no distant phantom grip.  **Fix (M07 playtest, `9cdadf7`):** the rod's reach is now 1 to 6 blocks over the cart (`cable.rod_reach_min`/`_max`), so it grips cable from anchors up to five blocks over the rail; six is refused (no distant grip). Tests: 3, 4 and 5 grip, 6 does not; real client sees the grip at 5.69.

**Status:** **OPEN — fix implemented and tested (local server suite 195/195 and client suite on `9cdadf7`; CI in the M07 appendix); awaiting owner in-game confirmation**

### BUG-004

**Area:** Cable-driven cart on inclining route (M07 playtest)

**Observed:** **Owner observed:** cart stops at the point an inclined cable section begins, apparently regardless of the incline height. Rod contacting overhead terrain is a possible hypothesis, **not a confirmed cause**.

**Diagnosis / fix / checks:** Reproduce level-to-slope transition at several clearance heights; inspect grip, route geometry, navigation/rail movement, support and obstruction checks, and stop reason. Do not silently fix by ignoring actual collisions or bypassing cable safety rules.  **Cause (reproduced, `9cdadf7`):** not the rod touching terrain. The drive's look-ahead judged the cable's reach from the cart's current rail height, and at the foot of a climb under level anchors the cable rose past the old 3-block reach, so the line reported `end`. **Fix:** the look-ahead uses the height of the track ahead (interpolated along ascending rails), with the wider BUG-003 reach. Test: cart hauled up a 4-block climb between level anchors, gripping throughout; the old reach is its control.

**Status:** **OPEN — fix implemented and tested (local server suite 195/195 and client suite on `9cdadf7`; CI in the M07 appendix); awaiting owner in-game confirmation**

### BUG-005

**Area:** Inclined cable visual geometry (M07 playtest)

**Observed:** **Owner observed:** cable segment on an incline visibly clips through the solid block immediately above the anchor.

**Diagnosis / fix / checks:** Reproduce and alter only the inclined-segment rendering/geometry or anchor exit treatment so it clears the overhead block; keep mechanical travel, support and proper visual cable continuity intact. Verify in real client from multiple angles.  **Fix (rendering only, `9cdadf7`):** a span climbing steeply from an anchor under a solid block is drawn level just under that block to the cell's edge, then climbs; mechanics, support and reach unchanged. The tilted-roller option was not needed. Client test (singleplayer and dedicated): the straight run would cross the plank, the drawn path does not; [screenshots](evidence/milestone-07/screenshots/) `m07pt-*incline-cable.png`. Multi-angle owner check still needed.

**Status:** **OPEN — fix implemented and tested (local server suite 195/195 and client suite on `9cdadf7`; CI in the M07 appendix); awaiting owner in-game visual confirmation**

## Owner playtest observations — 2026-10-14

The entries below are **16 separate owner findings** across M07/M08/M10/M11 and existing train/winch behavior. Some are reproducible symptoms; others are explicit UI, art or operation requests. **All remain unimplemented/unverified until a later, SHA-backed patch and owner retest say otherwise.** [M13](MILESTONE_13.md) consolidates these owner playtest changes and links here for canonical defect details; former per-milestone `## Playtesting Changes` sections were transferred to M13. Do not treat a REQUESTED entry as a proven underlying software defect or silently implement alternative behavior.

### BUG-006

**Area:** M07 cart → skip transfer — rear end

**Classification:** Defect. **Owner playtest (2026-10-14):** A freight minecart unloads into the ore skip from its front end, but not when approaching/loading from the cart's rear end.

**Owner addendum (2026-10-15):** Replace the skip's one-sided top lid with **two separate lid doors** that open **outward to the left and right sides** of the skip, leaving a clear central receiving opening for ore dumped from **either the front or the rear**. The lid halves visibly open for a valid cart-to-skip intake and close when the transfer ends or is interrupted. This is an owner-directed geometry/animation change as well as a transfer-direction fix; do not simply loosen an invisible acceptance check while the lid still blocks one approach.

**Expected / reproduction and acceptance:** Test both short end gates, orientations, rail/skip levels, bridle/intake alignment and docked Loading Rail conditions. A correctly positioned cart can pour into the skip from **either end**, with the matching minecart end gate and **both lateral-opening skip lid doors** visibly clearing the cargo path. Check opened lid clearance against neighboring blocks, hoist bridle, support cables and station structures; preserve the skip's lower discharge chute, transfer direction, capacities and item conservation. Close/restore the lid appropriately on successful finish, refusal, interruption, reload and client/server sync.

**Status:** **OPEN — fix implemented and tested in M13 (`ba68617`); awaiting owner retest**. Cargo goes over either short end of a docked skip; the closed lid is two side doors. Server test `skipTakesCargoFromEitherEnd`. No owner acceptance is claimed.

### BUG-007

**Area:** Minecart coupling — chains above rails

**Classification:** Defect. **Owner playtest (2026-10-14):** Attempting to couple two minecarts repeatedly places chain blocks above the rails instead.

**Expected / reproduction and acceptance:** Inspect chain-held right-click target priority, rail block placement, hitboxes and cart proximity from common camera angles. An intentional two-cart coupling interaction should take precedence without unintentionally disabling ordinary chain placement elsewhere; test Survival and client/server behavior.

**Status:** **OPEN — fix implemented and tested in M13 (`7168b80`); awaiting owner retest**. A chain used just behind a cart (on the rail) couples it. Server test `chainOnTheRailBehindACartCouples`. No owner acceptance is claimed.

### BUG-008

**Area:** Coal winch lever — inaccessible oblique hitbox

**Classification:** Defect. **Owner playtest (2026-10-14):** The winch lever is difficult to target; even though it protrudes, hits from oblique angles fail.

**Expected / reproduction and acceptance:** Compare visual lever geometry with actual interaction/collision/raycast selection from straight, diagonal and side angles. Improve targetability while preserving distinct lever and furnace/engine-box controls and avoiding accidental command changes.

**Status:** **OPEN — fix implemented and tested in M13 (`80ecf37`); awaiting owner retest**. The lever is part of the drum's selection shape and picked from any side (client pick mixin). Server test `winchLeverIsHitFromObliqueAngles`. No owner acceptance is claimed.

### BUG-009

**Area:** M10 Spelunker's Rope — rope and coil geometry

**Classification:** Requested visual refinement. **Owner playtest (2026-10-14):** The surplus-rope bundle looks like a plain tube instead of rope actually wrapping into a coil; the hanging rope and its anchor look too thin/light.

**Expected / reproduction and acceptance:** Make the bundle visibly wound, hanging rope thicker, and anchor chunkier. Preserve climb/collision/retrieval mechanics and verify near/far readability in client. No resource or new tool required.

**Status:** **REQUESTED — implemented and tested in M13 (`15d55a7`); awaiting owner review**. 3-pixel rope, copper plate anchors with a shackle and knot, a coil of three offset turns; only the rope's own unpainted art regenerated. Client screenshots `m13-rope-near/far`. No owner acceptance is claimed.

### BUG-010

**Area:** M10 Overalls — weight bar hidden

**Classification:** Defect. **Owner playtest (2026-10-14):** Equipping Overalls to reveal the extra inventory row hides the weighted-cargo limit bar.

**Expected / reproduction and acceptance:** Reposition/layout the weight bar so it stays readable with either 27- or 36-slot inventory configurations, including overload state, locked slots, scaling and UI overlays.

**Status:** **OPEN — fix implemented and tested in M13 (`95e955a`); awaiting owner retest**. Where the strip under the panel would leave the screen (double chest, GUI height 240) the bar takes the inventory label's row. Client check of inventory, chest and double chest with and without Overalls; negative control caught. No owner acceptance is claimed.

### BUG-011

**Area:** M08 Blast Furnace — casting light

**Classification:** Requested VFX refinement. **Owner playtest (2026-10-14):** The mold and furnace heat grates stay unlit while the mold is actively filling with hot metal.

**Expected / reproduction and acceptance:** During active molten casting, make mold and heat grates visibly emit light; coordinate emission with real cast state, interruptions and completion, without leaving stray light on stop/unload or creating false fuel/progress signals.

**Status:** **REQUESTED — implemented and tested in M13 (`960926a`); awaiting owner review**. Mold light 11 and channel light 9 only while pouring (none through the hold or a fuel-out); grilles, tap and running metal render full-bright. Server and midnight client checks. No owner acceptance is claimed.

### BUG-012

**Area:** M08 Blast Furnace — molten cinders

**Classification:** Requested VFX refinement. **Owner playtest (2026-10-14):** The mold-filling sequence lacks the small rising cinders/embers expected from hot metal, like lava's particles.

**Expected / reproduction and acceptance:** Add restrained local cinder effects tied to actual molten pour/fill (not constant idle particles); avoid excessive particle volume and preserve current casting timing and item state.

**Status:** **REQUESTED — implemented and tested in M13 (`960926a`); awaiting owner review**. Lava cinders rise from the mold's molten surface only while pouring (client counted 15 in 10 s; none stopped). No owner acceptance is claimed.

### BUG-013

**Area:** M06/M07 coal winch — engine-box coal GUI

**Classification:** Requested interaction/UI change. **Owner playtest (2026-10-14):** The coal winch's engine box lacks a small inventory screen for inserting coal.

**Expected / reproduction and acceptance:** Provide a compact coal-input interface by targeting the engine box, separate from reliable lever interaction; preserve actual fuel inventory/capacity, coal-only behavior, control mirroring at the receiving drum and server synchronization.

**Status:** **REQUESTED — implemented and tested in M13 (`4c0cf56`); awaiting owner review**. Empty hand on the engine box opens a furnace-style coal screen (one coal slot, burn flame). Server and real-client checks. No owner acceptance is claimed.

### BUG-014

**Area:** M11 freight minecart — read-only cargo GUI

**Classification:** Requested interaction/UI change. **Owner playtest (2026-10-14):** With on-click cargo text removed, owner now wants an intentional, small inventory-style cart inspection screen.

**Expected / reproduction and acceptance:** Show exactly one grayed-out, noninteractive cargo slot representing the current item and count; show NO player inventory. Inspection is read-only, cannot move items, and must coexist with loading/unloading/coupling/cushions and current visual fill. This *later* owner request supersedes only M11's no-replacement-GUI clause, not the ban on hover labels or narration.

**Status:** **REQUESTED — implemented and tested in M13 (`2d99113`, `e8b052a`); awaiting owner review**. Empty main hand opens a read-only "Freight Cart" window (no slots); Shift + use still unloads; a held use after a stack runs out keeps loading, no window. Server (with negative control) and client checks. No owner acceptance is claimed.

### BUG-015

**Area:** M08 Blast Furnace — z-fighting

**Classification:** Defect. **Owner playtest (2026-10-14):** Textures visibly flicker at several places, notably the front cast-metal plate and the output mold.

**Expected / reproduction and acceptance:** Inspect coplanar model faces/animated layers, UV/depth offsets and render states from multiple angles and during idle/active pour. Fix z-fighting without regenerating/overwriting the owner's hand-painted PNGs, repacking UVs, or reducing the furnace detail.

**Status:** **OPEN — fix implemented and tested in M13 (`d290c2d`); awaiting owner retest**. A face scan found flush faces with different texels (posts/cornice, door frames, tap, mold clamps); separated by 0.05 px, UVs and painted PNG untouched; the scan now finds none. Grazing-angle screenshots; flicker is not visible in still frames, so owner retest decides. No owner acceptance is claimed.

### BUG-016

**Area:** M10 Trail Blazer — ghost highlight silhouette

**Classification:** Requested visual refinement. **Owner playtest (2026-10-14):** Trail Blazer's through-wall highlight looks unlike a form-fitting outline of the underlying torch/ladder/rope model.

**Expected / reproduction and acceptance:** Explore a silhouette-following outline on the actual model; if disproportionately difficult, ask owner about removing this highlight rather than keeping a misleading blob. Preserve the agreed Trail Blazer mechanics until the owner chooses an alternative.

**Status:** **REQUESTED — implemented and tested in M13 (`2bcd197`); awaiting owner review**. Owner direction (2026-10-20): translucent lines along the model's own edges (torch stick, rope/anchor/coil; ladder traced as rails and rungs). Benchmark, 113 ghosts, software rendering: 43.3 fps vs old boxes 43.0 (a filled outline-pass silhouette, `4a67b67`, measured 32.1 and was replaced). No owner acceptance is claimed.

### BUG-017

**Area:** M10 Old Pro — off-hand pickaxe mining

**Classification:** Unconfirmed defect / control discovery. **Owner playtest (2026-10-14):** Owner reports a pickaxe in the off hand does not mine blocks, and is unsure of the intended control.

**Expected / reproduction and acceptance:** First verify the full Headlamp + Overalls + Mining Boots 'Old Pro' bonus is active (the off-hand mining feature is not unconditional). Document/test exact break input while the main hand holds a sword/axe, then reproduce in client; if equipped and invoked correctly but failing, fix without breaking vanilla mainhand actions or duplicating drops.

**Status:** **OPEN — not reproduced in M13 (`292fcb5`): works with the full outfit; awaiting owner retest**. Real client: holding attack on stone with an iron sword in hand and iron pickaxe in the offhand breaks it with the pickaxe (it wears, the sword does not) only with Headlamp + Overalls + Mining Boots (Old Pro); without the outfit the sword mines. Control: ordinary attack button. No owner acceptance is claimed.

### BUG-018

**Area:** M07 two-drum cables — ground/hanging attachment and coil ownership

**Classification:** Defect / refinement. **Owner playtest (2026-10-14):** Attaching an existing cable end to a receiving winch fails when the end is on the ground unless a coil is installed, yet works when hanging; preloading a coil on the receiver can result in its coil popping off after connection.

**Expected / reproduction and acceptance:** Make attachment of a held, routed cable end to an otherwise empty receiving winch behave consistently whether grounded or hanging. The receiver must NOT require its own cable coil. Resolve handling of a preloaded receiver coil without duplication/loss or involuntary incorrect ejection; preserve one source coil's ownership and safe detach/refund rules.

**Status:** **OPEN — fix implemented and tested in M13 (`a7b297a`); awaiting owner retest**. A held cable end is made fast to a drum clicked anywhere (ground or hanging). Server test `secondDrumTakesTheEndAnyWay`. No owner acceptance is claimed.

### BUG-019

**Area:** M07 two-drum cables — receiver facing restriction

**Classification:** Defect / operating refinement. **Owner playtest (2026-10-14):** A receiving cable drum cannot accept the connection if it faces the 'wrong' direction.

**Expected / reproduction and acceptance:** Allow a receiving drum to accept a valid cable end regardless of drum facing; keep cable geometry, route obstruction, per-end PULL/FEED controls and visual winding direction coherent. Do not assume facing can override an actually obstructed path.

**Status:** **OPEN — fix implemented and tested in M13 (`a7b297a`); awaiting owner retest**. The receiving drum accepts the end whichever way it faces. Server test `secondDrumTakesTheEndAnyWay`. No owner acceptance is claimed.

### BUG-020

**Area:** M07 cable anchors — selective mid-route detach

**Classification:** Requested operating change. **Owner playtest (2026-10-14):** Owner wants to right-click an already routed anchor with a held cable end and unroute the upstream portion back to that chosen anchor, leaving the loose end hanging there.

**Expected / reproduction and acceptance:** Implement intentional selected-anchor disconnect/retraction rather than only full-route disconnect. Specify which segments are retracted versus retained from the user's stated 'up to that one' behavior, respect skip/grip safety and no duplication, and visually leave a free hanging end at the selected anchor.

**Status:** **REQUESTED — implemented and tested in M13 (`13bb744`); awaiting owner review**. Using the held end on an earlier anchor of its route winds the route back to it. Server test `heldEndWindsBackToAnEarlierAnchor`. No owner acceptance is claimed.

### BUG-021

**Area:** M07 cable anchors — inclined-span obstruction clearance

**Classification:** Requested operating refinement. **Owner playtest (2026-10-14):** Anchors and inclined cable runs are finicky when routing past terrain. Owner proposes that an incline's receiving anchor drop its hanging portion just enough to clear an obstruction, within reasonable limits.

**Expected / reproduction and acceptance:** Investigate obstruction cases and a bounded downward roller/hanger offset while the mounting block stays fixed. Establish a conservative maximum drop and tests for clearance, diagonal spans, rod reach, anchor rendering and support validity; do not auto-route through solid blocks or silently extend cable beyond physical constraints.

**Status:** **REQUESTED — implemented and tested in M13 (`635efff`); awaiting owner review**. On an inclined span the receiving anchor's roller hangs up to 4 steps (2 px) lower until the span clears. Server test `inclineAnchorDropsJustEnough`. No owner acceptance is claimed.

### BUG-022

**Area:** M07 Ore Skip — z-fighting / overlapping rendered faces

**Classification:** Visual defect. **Owner report (2026-10-17):** The **Ore Skip** exhibits z-fighting (visually flickering/interfering faces). The specific affected surfaces, camera angles, and exact reproduction conditions have **not yet been documented**; do not confuse this with the separate M08 Blast Furnace [BUG-015](#bug-015).

**Expected / reproduction and acceptance:** Inspect the Ore Skip's assembled model and each relevant component/animated state (bin, chute, gates, lid, bridle) in the real client from multiple camera angles and during operation to **identify which overlapping/coplanar faces actually flicker**. Check geometry overlap, face visibility, depth separation and render layering before selecting the smallest geometry/render fix. Eliminate visible flicker without changing freight behavior, removing intended detail, shifting owner-approved UVs, or overwriting the owner's hand-painted `ore_skip.png` texture or editable Blockbench sources. Capture before/after client evidence and obtain owner visual retest. This defect is within [M13's M07 visual regression work](MILESTONE_13.md); it does not claim implementation authorization.

**Status:** **OPEN — fix implemented and tested in M13 (`d290c2d`); awaiting owner retest**. Scan of the skip as the renderer stacks it: corner posts flush with rims, chute back bar through its posts; separated by 0.05 px, painted `ore_skip.png` untouched; scan clean. No owner acceptance is claimed.

Keep a defect open until its specific owner-observed symptom has been checked in game. Historical analysis can describe hypotheses that were later disproved; don't mistake them for the final cause.
