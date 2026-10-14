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

Keep a defect open until its specific owner-observed symptom has been checked in game. Historical analysis can describe hypotheses that were later disproved; don't mistake them for the final cause.
