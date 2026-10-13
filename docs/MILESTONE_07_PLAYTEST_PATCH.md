# M07 owner playtest patch — physical freight geometry, conditional bridle and pulley
**Date:** 2026-10-13  
**Status:** OWNER-REQUESTED CORRECTIVE IMPLEMENTATION BRIEF — M07 has code and CI Build #22, but is **NOT owner-accepted**. This patch describes defects encountered in the owner's in-game playtest. It is explicitly requested for Claude Code to read and implement/test as a focused M07 follow-up, not an M08 task. Preserve the original M07 commits/report and write a dated follow-up with fresh tests/CI.

**Priority:** Make the machinery's physical arrangement actually explain, and gate, the freight transfer. The existing `SkipStations` code does determine LOAD vs UNLOAD from skip-relative height; the complaint is **not** that inventory currently determines direction. The defect is that the spatial test accepts a cart that the physical chute cannot reach, and the resulting visuals fail to demonstrate a legitimate connection. Item availability/type/capacity may allow or block a physically valid exchange; they must **never invent one**.

## 1. Skip → minecart unloading: fix the height, NOT the skip model

**Owner complaint:** The bottom end of the existing lower angled chute/ramp is supposed to **rest against the receiving minecart**, delivering material into its open top. At present, a minecart immediately horizontally beside the skip's *bottom block at the same Y level* can receive ore although the ramp does not meet its top. **Do not reshape, shorten, lengthen, move or redesign the ore skip to justify that incorrect arrangement.** The skip's existing silhouette, 1×1×3 body, inclined lower chute, gate, cargo opening and paintable Blockbench art remain the reference design.

**New docking position:** A receiving minecart must be horizontally adjacent on the chute-facing side **and one rail/block level LOWER** than the skip's bottom chute section — the **vertical/horizontal diagonal, or “kitty-corner” block**. The chute lip then meets the minecart at its rim/open top. Example in a local coordinate frame (verify the renderer's world-space Y/rail/cart offsets before finalizing):
- Skip bottom/chute starts at `(x, y, z)`, facing horizontal direction `F`.
- **Eligible UNLOAD rail/cart:** `(x + F.dx, y - 1, z + F.dz)`, with a freight cart *docked* on a Loading Rail whose front faces the skip.
- **INELIGIBLE, must not unload:** `(x + F.dx, y, z + F.dz)` (same-elevation, merely side-adjacent cart), even if the cart is empty and skip is full. Also reject any other height where the existing chute's lower lip misses the minecart top.

These coordinates explain the intent; adjust only the station position/small alignment tolerance if actual vanilla cart-on-rail offsets demand it, and **prove the physical contact in a real client** from the side and front. Do not shift the ore skip's visual/collision body to force a match. No terrain or cart may be intersected by the chute/lip, and no solids may interrupt the material path. Only a **hung** skip is allowed to unload via its chute. It is **acceptable and intended** that a skip directly beside a minecart at the same height cannot unload; it must first be raised/held in the correct position by its cable.

**The receiving cart's front stays closed** during skip → cart unloading; the skip's own discharge gate opens/slides. The loading track remains the existing Loading Rail, not a new station block.

## 2. Minecart → skip loading: actually lower the cart's folding gate

**Owner complaint:** A cart positioned for upper intake does not visibly lower its front door to dump into the skip.

Keep the direction rule from M07: a loaded freight cart docked at the correctly oriented **upper/bridle-height** Loading Rail is a **SOURCE to the skip**, not a recipient, regardless of inventory state. However, simply passing inventory from cart to skip, or asserting a client `gateOpen` boolean, is **not proof of correct behavior**.

- Require a real, cable-attached **bridle** and a hung skip, correct cart orientation/docking, correct vertical position and an unobstructed geometry from the cart's front gate/ramp into the **top intake**.
- Visibly **hinge/fold the cart's actual front panel DOWN** to form a believable downward ramp to the skip mouth, with no clipping through the bridle or other blocks. Open the skip's intake lid accordingly; close both after completion, interruption or cart release.
- Verify the player can SEE the gate drop and the ramp span the gap. Inspect actual renderer/model transforms, synced state and animation rather than relying on server counts or a flag alone. Preserve the ordinary vanilla minecart chassis and texture and the freight cart's established 256-unit cargo behavior.
- Wrong facing, wrong height, no bridle/cable, blocked ramp, non-freight/passenger cart, undocked cart, wrong-type or full cargo must never produce phantom transfer.

## 3. The skip is PLACED WITHOUT a bridle; attaching the cable ADDS it

**Owner correction:** An ordinary freshly placed ore skip is just its **three-block-high body and integrated chute/bin**. **No cable bridle is visible or collidable** until the player successfully connects the existing loose cable hook to the skip.

- **Unattached:** render no bridle and have only the actual **1×1×3 body collision** (plus the existing lateral visual-clearance policy for the body). Do not reserve/impersonate a fourth-block bridle hitbox when it is absent. Allow standing placement as an unbridled skip where the body legitimately fits; don't pretend it arrived pre-rigged.
- **Attached:** add the existing triangular two-leg bridle visibly above the bin; **its fourth block is collidable**, and the existing main hoisting cable joins its apex. Require clearance at attachment; reject attachment safely if a solid block/entity occupies the newly added bridle space. Preserve the original body unchanged.
- **Detach, anchor break, cable removal and restore:** remove the visual/physical bridle when the hoist link is gone; on a valid saved/reloaded attached skip reconstruct it consistently. A failed attachment must leave the skip unbridled and must not consume or duplicate hooks/coils/cargo. Preserve safe fall/settling rules, no unloaded-entity travel, and existing overhead/side-clearance checks.
- Do not add a new bridle item, recipe or manual attachment step: it appears as the visual/physical consequence of hooking up the cable.

## 4. Ceiling anchor becomes a pulley ONLY after attaching the hook to a skip

**Owner correction (2026-10-13):** The loose hook **already hangs freely** from the last installed anchor, even before a skip exists or a cable is attached to one. A vertically hanging loose end, on its own, **does not convert the anchor into a pulley**. The top anchor changes its visual appearance to a ceiling-mounted pulley wheel **only after the loose hook is successfully hooked to a skip**.

- **Before attaching to a skip:** final anchor retains its ordinary ceiling-mounted anchor/roller model; the real cable hangs down to its **free hook** and can be paid out or wound in with the existing manual/powered drum (see §5A). This is not a fictitious dangling hook that always resets to the anchor's original height. Show the descending cable to the free hook, without a premature pulley model or bridle.
- **After successful attachment:** the **same ceiling anchor** renders a physically recognizable pulley wheel mounted beneath the support; its cable bends over the wheel and descends vertically to the new bridle apex on the skip. This is a visual variant of the existing anchor, **not** a new anchor item, machine, recipe or separate station.
- **Detach/disconnect:** remove the skip's bridle as specified in §3 and revert the wheel's visual appearance to an ordinary ceiling anchor when it is no longer hoisting a skip. The loose cable end and hook should persist at their appropriate paid-out height, not magically teleport to the top.
- Render and sync the **free-end cable depth**, the conditionally attached bridle, and the conditionally converted wheel correctly on save/reload, chunk boundaries and multiplayer clients. Match the drawn cable's actual path/length and avoid block clipping; the existing copper anchor's support, route, collision and one-coil accounting remain unchanged. Do not reskin unrelated ordinary anchors or M05's separate closed-loop terminal pulley behavior.
- Provide editable Blockbench/UV assets and actual-client evidence for the **normal ceiling anchor + hanging loose hook**, the **winched-down free hook with still-normal anchor**, the **same anchor converted to a pulley after successful skip attachment**, and its **reversion after detachment**.

## 5. Detection and physical causality (implementation target)

**The transfer mode is still determined by geometry — NEVER by cargo being full/empty.** Use the actual anchored geometry and visible moving parts, not just “there is a Loading Rail with a cart.” `SkipStations.station()` currently checks a horizontally adjacent rail at skip-bottom Y for unload, or bridle Y for load, as well as rail facing and docking; **that limited geometry is the issue**.

`cart → skip`: docked cart at upper intake/bridle height, valid attached bridle, cart front oriented to the skip, visibly deployable ramp into the open top; then type/capacity checks.

`skip → cart`: docked cart at the **one-block-lower diagonal** receiving position, hung skip, existing lower chute lip reaching the cart's upper interior, no obstructing blocks; skip gate opens while minecart front stays shut; then type/capacity checks.

If the chute cannot reach, the route is blocked, or the attachment is absent, **do not transfer at all** even if the item/cargo totals could technically change. Keep whole-train locking, conservative powered-winch stop/NEUTRAL and partial coal-burn preservation; no automatic restart.

**State/visual sync:** Avoid a fake open gate that snaps shut immediately because the model and the logical state disagree. Animated motion should be legible in-game (real hinge/slide if practical), and movement/transfer should pause as required until the material path is actually open. Do not silently enlarge the skip's body or cargo capacity to make the station match.

## 5A. Cable construction: limited horizontal hand-carry, unlimited winch-controlled vertical lowering (owner correction 2026-10-13)

**This section SUPERSEDES the earlier order to remove the hand-slip limit completely.** The owner wants two distinct behaviors. A free cable end should not be dragged hundreds of blocks horizontally without supports; equally, a mine shaft must not need scaffolding merely to attach a skip at depth.

**A. Horizontal routing — temporary hand-carry still requires supports**
- Keep the established **12-block maximum unsupported span between permanently installed anchors**, including drum → first anchor, in `CableRoutes.routeThrough()`. The reusable coil may create unlimited **total routed length**, but only through properly spaced anchors.
- While **holding the loose cable end to route horizontally**, at **12 blocks of horizontal separation from its last support** the line becomes taut. Do not allow it to create a valid fixed support span longer than 12 blocks.
- Allow modest reach/slack to interact, then at about **16 blocks horizontally** from the last support the end is automatically **put down at that support** (not destroyed, consumed or permanently lost). The player can recover it there and continue routing by adding anchors. Prefer to use the already present hook release behavior; **add no new in-game flavor text, warnings, overlay strings or narration** for becoming taut or putting the hook down. The previous “Your hand slips” wording must not be surfaced as a new or renamed notification.
- This horizontal limit is based on **horizontal displacement**, not total 3D Euclidean distance. Do not mistakenly enforce it on vertical descent directly under the terminal anchor. Do not permit “unlimited carry anywhere” by labeling arbitrary diagonal or horizontal routes as vertical.

**B. Vertical shaft — pay out/retrieve the ALREADY-HANGING free hook using the existing winch**
- With an installed cable route ending in a ceiling-mounted anchor **directly above a shaft**, the **loose end and hook already exist and hang beneath that anchor even before connection to a skip**. **FEED lowers the hanging hook, PULL raises it**, using the existing manual crank or coal-powered winch. Do **not** require a new lower-hook object, bridle, special station, extra pulley item, or manually dragging cable down the shaft.
- There is **no fixed vertical travel/depth cap** imposed by `cable.max_span` or the 12/16-block horizontal hand-carry rule. The free hook can descend through a deep clear shaft (e.g., 30, 100+ blocks) while staying directly under the final anchor, and can be taken in again. The rendering must display the cable from the unchanged terminal anchor down to its actual loose hook position.
- **Only a valid hoist attachment transforms the ceiling anchor into a pulley model** (§4). The earlier presence of a free dangling cable and the act of winching it down **do not** trigger that appearance change.
- The player descends to the skip, picks up the nearby lowered hook, and connects it; only THEN does the skip gain its collidable bridle and the top anchor acquire its visible pulley. On a failed attachment nothing is consumed and the free hook stays where it was. Disconnecting a suspended skip must not magically relocate or multiply the loose end. If the player briefly picks it up at depth, horizontal reach is still measured from its **current hanging-end location / vertical drop point**, not as a 30+ block distance from the ceiling — keep it usable for a short local connection to the skip.
- A hanging free hook is a real cable end with a **server-owned paid-out vertical length/height**. Persist and synchronize it via existing cable/drive save architecture rather than reconstructing it at the top on each tick or reload. Winch speed/direction and safe stop semantics apply; consume coal only for actual moving cable and never while stationary, blocked, at endpoints, or in unloaded geometry. For an **unloaded hook/shaft/drum**, halt safely without force-loading or simulating movement through unloaded chunks. No duplicate hooks, coils or loose-end states on reconnect or chunk reload. Preserve active hooked skips' original controls and motion.
- When routing horizontally rather than operating a final overhead free-hanging end, apply the ordinary anchor spacing and short leash. A free hook may be temporarily in the player's hand for connection; it must not enable a 500-block unsupported horizontal cable across a world.

**Required regression evidence:** horizontal carry at 12 (taut), 16 (automatically returned to last support), and re-pick-up; >12-block proposed permanent fixed span rejected; successive supported segments still allow an unlimited-length route; terminal hook **lowered and raised with manual AND powered winch before any skip attachment**; >30-block deep shaft with a no-scaffold connection at the bottom; normal anchor remains normal while free hook hangs/moves and changes to pulley **only after successful hookup**; detach restores ordinary anchor and unbridled skip; no new messages; save/reload, multi-player, cable/pulley/bridle synch, movement/reversals, real-client screenshots and no item or fuel exploits. Include negative controls showing the old 15-block 3D leash fails the deep-shaft case and that unconditional unlimited horizontal carrying fails the new 16-block return rule.

## 6. Required proof and regression tests

Record PASS/FAIL/NOT RUN with actual evidence; add negative controls that fail under the present incorrect behavior:

| Test | Expected |
|---|---|
| UNLOAD diagonal | Hung skip at appropriate height, cart on Loading Rail horizontally in front **one block lower**; chute **lip contacts** cart rim, gate opens, transfer succeeds without cargo loss |
| UNLOAD side-by-side wrong height | Cart directly next to skip's bottom block **same Y**; **zero transfer** even when compatible, loaded and docked |
| UNLOAD without cable | Unattached skip adjacent to a lower docked cart: **zero transfer**, no floating bridle |
| LOAD cart gate | At upper bridle-height station, the **actual cart door visibly drops** and reaches skip intake; skip lid opens; count conserved; then gate and lid close |
| LOAD no bridle/wrong height | No cable-linked bridle, wrong level/facing or wall blocking ramp: no transfer, no impossible animation |
| Bridle lifecycle | Newly placed skip looks **1×1×3** and collides as such; attaching cable adds the visible/collidable **fourth block**; detaching/anchor cut removes it; save/reload preserves the correct state |
| Attach collision | Fourth-block ceiling obstruction prevents bridle attachment without consuming cable/items or trapping the entity; existing body is unchanged |
| Pulley transition | Ceiling anchor **remains ordinary while a free hook hangs/lowers/raises**, becomes a ceiling pulley **only when successfully hooked to the skip**, then reverts on disconnect; real-client/reload/multiplayer proof |
| Orientation and clearance | Repeat both transfers and visual contact for all four horizontal directions, ground/hanging infrastructure, partial/full/wrong-type sources, walls and occluded paths |
| Persistence and interruption | Interrupt mid-transfer, release the cart, disconnect cable, break track, unload/reload or reconnect: correct closed visuals, item conservation and safely stopped winch |
| Horizontal leash and supports | Held hook goes taut at 12 horizontal blocks and puts down around 16; no new flavor text; can recover and route via additional anchors; new fixed spans >12 blocks still fail |
| Free hook winching | Existing dangling hook is raised/lowered **without a skip** by manual and coal winch, without new items, force-loading or unwanted fuel burn |
| Deep-shaft connection | Winch hook down >30 blocks from an **ordinary** ceiling anchor; player picks it up locally and hooks to skip on shaft floor with no temporary platform; safe cable/bridle rendering and one-coil conservation |
| Permanent span limit unchanged | Fixed anchor-to-anchor spans >12 blocks are rejected regardless of cumulative installed cable length; horizontal carrying does not become unlimited |
| Regression | M01–M06 tests, M07 512-slot UI/heavy limit, freight cart capacity, anchor/coil conservation, coupled trains, long lines, manual and powered hoist, CI build, actual-client screenshots |

**Visual acceptance cannot be substituted by a Boolean “gate open” test.** Show clear side/front screenshots in the real client proving chute-to-cart and lowered-cart-gate-to-skip contact. Owner-supplied unloading reference is the intended silhouette; do not redesign the skip to accommodate the incorrect earlier rail height.

## 7. Handoff and exclusions

- Commit the correction implementation and updated editable art in a **separate M07 follow-up**; link code SHA(s), relevant failing-before/passing-after tests, CI Build and artifact, actual-client screenshots and the results in a dated amendment to `docs/MILESTONE_07_REPORT.md`.
- Preserve M07 historical CI #22 evidence as historical: it passed tests for the **old** geometry and did **not** establish acceptance of this new physical arrangement.
- **M07 owner acceptance stays unchecked** until the owner playtests these fixes. Do not start M08/M09/M10 as part of this patch. No new machine, new freight cart, more inventory slots, new metal, or redesign of the skip body.
- For this corrective pass, **do not add extra flavor text or invented in-game messages**. Existing useful error handling can remain where necessary, but the distance behavior should be conveyed by the mechanics themselves.
- If the original models' exact contact geometry conflicts with the nominal `y-1` rail offset, calculate/measure the real cart rim and chute lip, take a client screenshot and make the **smallest station-alignment correction**; do not reshape the ore skip or quietly revert to same-height side-by-side unloading.
