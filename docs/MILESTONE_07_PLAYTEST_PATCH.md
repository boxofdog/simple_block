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

## 4. A downward cable turn at the ceiling anchor must LOOK like a pulley

**Owner complaint:** The last overhead copper anchor currently continues to look like a horizontal roller even where the cable turns downward to hoist the skip.

- When an **existing ceiling-mounted anchor** is actually redirecting a cable from the routed overhead line **vertically DOWN toward a skip**, render it as a **pulley/sheave wheel mounted to the underside of the ceiling/support**, with its cable visibly wrapping/turning over it and dropping straight to the bridle apex.
- The existing copper anchor block/item, placement, strength, routes and one-coil accounting remain unchanged. This is a **topology-driven appearance** of the same anchor, not another crafted station part or required machine.
- Ordinary horizontal-support anchors must remain ordinary supports. The pulley must track cable connection/disconnection, the current hanging state, orientation, multiplayer synchronization and world save/reload. Do not show a suspended cable segment when no skip is attached.
- Provide editable Blockbench art/UV and actual-client screenshots of both normal anchor and working overhead 90-degree downward turn. Preserve unobstructed hoist path and collision behavior.

## 5. Detection and physical causality (implementation target)

**The transfer mode is still determined by geometry — NEVER by cargo being full/empty.** Use the actual anchored geometry and visible moving parts, not just “there is a Loading Rail with a cart.” `SkipStations.station()` currently checks a horizontally adjacent rail at skip-bottom Y for unload, or bridle Y for load, as well as rail facing and docking; **that limited geometry is the issue**.

`cart → skip`: docked cart at upper intake/bridle height, valid attached bridle, cart front oriented to the skip, visibly deployable ramp into the open top; then type/capacity checks.

`skip → cart`: docked cart at the **one-block-lower diagonal** receiving position, hung skip, existing lower chute lip reaching the cart's upper interior, no obstructing blocks; skip gate opens while minecart front stays shut; then type/capacity checks.

If the chute cannot reach, the route is blocked, or the attachment is absent, **do not transfer at all** even if the item/cargo totals could technically change. Keep whole-train locking, conservative powered-winch stop/NEUTRAL and partial coal-burn preservation; no automatic restart.

**State/visual sync:** Avoid a fake open gate that snaps shut immediately because the model and the logical state disagree. Animated motion should be legible in-game (real hinge/slide if practical), and movement/transfer should pause as required until the material path is actually open. Do not silently enlarge the skip's body or cargo capacity to make the station match.

## 5A. REMOVE “Your hand slips”: carrying an installed cable end has NO distance leash (owner direction 2026-10-13)

**Owner verdict:** Once a Cable Coil has been installed on the drum, carrying the loose rope/cable end should **not involuntarily release it** just because the player walks farther than 12 blocks (currently an extra 3 blocks of hand allowance produces ~15 blocks total). This is unnecessary friction for mine construction and especially for walking the cable straight down a deep shaft to attach a skip. **REMOVE the hand-slipping rule; do not merely increase its distance, change its message, or preserve it for nonvertical carrying.**

- In `cable/CableHook.java`, the current `tick()` compares the held hook's eye with the last support, against `CableRoutes.maxSpan() + CableRoutes.HOLD_SLACK`, then invokes `release()` and emits `simple_block.cable.slipped`. **Delete this distance-triggered automatic release** and its obsolete feedback/tuning constant if no longer used. A held end stays in the player's hand until the player deliberately puts it down/attaches it, dies/disconnects/changes dimension, or another already-legitimate invalidation occurs. There is **no universal carrying-distance cap**, whether down a shaft or across a work site.
- **KEEP the actual fixed-anchor support limit:** `CableRoutes.routeThrough()` must still reject a new **permanent span between successive anchors (or from the drum to the first anchor)** longer than the configured `cable.max_span` (currently 12 blocks), including the established obstruction checks. Do not turn unlimited coil length into unlimited unsupported fixed spans. The *temporary loose end in someone's hand* and the *vertical descent from a terminal pulley to a hung skip* are different from a finished unsupported span between fixed supports.
- **Deep shafts:** with the route properly installed and the final anchor/pulley over the shaft, the player can stand at the bottom (far beyond 15 blocks), bring the end down and attach it directly to the correctly positioned skip. **No temporary scaffolding, no platform at the top, no staged hoisting just to get the hook attached.** Require a clear cable run, head-pulley alignment and the new bridle-attachment clearance rules from §§3–4; do not make a silently clipping or teleporting suspended cable.
- **Chunk-distance trap:** the existing hook is also coupled to a loaded `CableDrumBlockEntity` via `CableHook.drum(level)`; its `tick()` discards it when the drum is not loaded, even if the player is still carrying the end. **Inspect and fix this as part of removal of the leash:** carrying the loose end a long distance must not cause it to disappear, involuntarily reset to the last support, or strand its owner/cable if the drum unloads. Use existing level-saved `CableLines`/route identity and safe revalidation where feasible; **never force-load the drum, shaft or remote anchors, never create duplicate hooks or cables, and never advance unloaded minecarts/skips.** Ensure operations that require live drum state resolve safely when possible, rather than silently destroying the player's carried end.
- Preserve intentional **manual put-down** (drum/last anchor interaction), a single held end per player, inventory and hook identity, voluntary cable routing, proper hand/cable rendering without an unbounded mesh/performance problem, route rollback, and one-coil material conservation. If an owner/dimension/entity is genuinely lost, recover safely with truthful feedback; don't disguise that as the old arbitrary distance leash.

**Required regression proof:** (a) hold the hook and walk >15, >32 and >100 blocks horizontally without an automatic drop; (b) attach a suspended skip from the bottom of a clear shaft >20–30 blocks deep without a temporary top platform; (c) cross far enough for the drum chunk to unload and return, with the hook still usable and **only one** hook/coil; (d) deliberate hook put-down still works; (e) reject a new unsupported 13+ block anchor-to-anchor span (configured 12) even while holding the end hundreds of blocks away; (f) verify obstruction, multiplayer/relog/owner invalidation, rendering and item conservation in real clients. Add a **negative control** proving the new long-distance hand-carrying test fails against the prior `slipped` behavior.

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
| Pulley transition | Normal ceiling support stays standard; an anchor guiding cable DOWN to a hung skip visibly becomes ceiling pulley; removal/reload/multiplayer update it |
| Orientation and clearance | Repeat both transfers and visual contact for all four horizontal directions, ground/hanging infrastructure, partial/full/wrong-type sources, walls and occluded paths |
| Persistence and interruption | Interrupt mid-transfer, release the cart, disconnect cable, break track, unload/reload or reconnect: correct closed visuals, item conservation and safely stopped winch |
| Hold cable end at any distance | Keep a hook in hand beyond 15, 32 and 100 blocks, including after the drum unloads; **no “hand slips”** and no dropped/duplicated hook or cable |
| Deep-shaft connection | Carry cable down from terminal ceiling pulley and attach a skip >20–30 blocks below it, without temporary platforms; safe vertical cable rendering and appropriate bridle/pulley |
| Permanent span limit unchanged | Fixed anchors still reject >12-block spans (or configured maximum), despite unlimited distance while carrying the loose end |
| Regression | M01–M06 tests, M07 512-slot UI/heavy limit, freight cart capacity, anchor/coil conservation, coupled trains, long lines, manual and powered hoist, CI build, actual-client screenshots |

**Visual acceptance cannot be substituted by a Boolean “gate open” test.** Show clear side/front screenshots in the real client proving chute-to-cart and lowered-cart-gate-to-skip contact. Owner-supplied unloading reference is the intended silhouette; do not redesign the skip to accommodate the incorrect earlier rail height.

## 7. Handoff and exclusions

- Commit the correction implementation and updated editable art in a **separate M07 follow-up**; link code SHA(s), relevant failing-before/passing-after tests, CI Build and artifact, actual-client screenshots and the results in a dated amendment to `docs/MILESTONE_07_REPORT.md`.
- Preserve M07 historical CI #22 evidence as historical: it passed tests for the **old** geometry and did **not** establish acceptance of this new physical arrangement.
- **M07 owner acceptance stays unchecked** until the owner playtests these fixes. Do not start M08/M09/M10 as part of this patch. No new machine, new freight cart, more inventory slots, new metal, or redesign of the skip body.
- If the original models' exact contact geometry conflicts with the nominal `y-1` rail offset, calculate/measure the real cart rim and chute lip, take a client screenshot and make the **smallest station-alignment correction**; do not reshape the ore skip or quietly revert to same-height side-by-side unloading.
