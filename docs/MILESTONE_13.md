---
milestone: M13
status: IMPLEMENTING
---

# M13 — Widespread bug-fixing, playtesting and feature-refinement patch

**Owner scoped 2026-10-15; owner refinements settled 2026-10-19. Status: PLANNED - READY; implementation has NOT been authorized.** M13 centralizes playtest-driven corrections, cross-milestone regressions and usability refinements from M07–M11, plus the bow-launched Spelunker's Rope, useful nighttime bed rest, inventory drag-to-transfer and chest sorting. It does **not** reimplement or change the lifecycle states of original M01–M12 features. Their original requirements, implementation appendices and CI evidence remain in their milestone files. [BUG_LOG.md](BUG_LOG.md) is the sole authority for bug symptoms, diagnoses, status and fix verification.

## Patch scope

| Area | M13 playtesting and refinement focus |
|---|---|
| M03–M07 and M11 industrial freight | Cart/skip intakes, split skip lids, winch lever, rail-stop automation, coil/two-drum connections, cable inclines and selective disconnect, skipped-drop gravity/CLANG, coupling, owner retests. [BUG-003–008](BUG_LOG.md), [BUG-013 and BUG-018–021](BUG_LOG.md), [BUG-022](BUG_LOG.md). |
| M08 metallurgy and capacity | Heat-grate/mold light, cinder particles, z-fighting and owner retests of previously delivered rename and casting animation. [BUG-011/012/015](BUG_LOG.md). |
| M09/M12 mobs and defenses | Verify light attraction, doors, creeper retaliation, rare jockey occurrence and effigy interactions. **No M12 defect has been reported in the moved notes.** |
| M10 access and workwear | Rope/coil visual refinements, weighted load bar, Trail Blazer silhouettes, Old Pro offhand control and bow-launched rope. [BUG-009/010/016/017](BUG_LOG.md). |
| M11 cargo UX | A read-only, one-disabled-slot cargo inspection window, with no player inventory or return of hover/action-bar narration. [BUG-014](BUG_LOG.md). |
| M01 nighttime survival | Beds permit nighttime rest to recover health by consuming hunger **only when vanilla permits lying down (no nearby monsters)**; never skip time or weather; valid spawn-setting continues unchanged. |
| M02/M08/M10 inventory QoL | Shift + right-click drag across occupied inventory/container slots to quick-move each eligible stack, retaining shift-click destination priority, weight/carrying limits and slot protections; add a Sort button to chest screens without sorting the player's inventory. |
| M01–M12 overall | Broad regression of resource conservation, survival, weight/inventory, hostile AI, freight systems, ropes, art, client/server and saved-world behavior. |

The present repair/retest pool is open or requested BUG-003–022; resolved/tested BUG-001 and BUG-002 warrant regressions, not reopening by assumption. Track defects and triage in BUG_LOG. Protect owner-painted textures, UV coordinates and editable Blockbench models. No unrelated systems, items or progression gates are authorized.

## New owner-requested feature — offhand Spelunker's Rope takes bow-fire priority

**Interaction:** Hold **Spelunker's Rope in the offhand** and a **vanilla bow in the main hand**. Drawing and releasing the bow launches the rope's anchor at the crosshair target **even when normal arrows are available**. Offhand rope takes priority over arrows. The bow becomes an improvised rope launcher, **not** a separate tool, arrow attack, grappling hook or player-pulling system.

- **Input priority:** **When Spelunker's Rope is equipped in the offhand, bow draw/release launches rope instead of arrows, regardless of arrow availability in the inventory.** Do not fire or consume an arrow during a rope shot. With no offhand Spelunker's Rope, preserve vanilla bow and arrow behavior (including normal arrowless use). Rope in the inventory/mainhand alone does not activate the mechanic; Infinity or other bow effects must not create limitless rope.
- **Charged distance and flight:** Short draws launch a short distance, full draws farther; a visible, understandable path/impact respects line of sight, real obstructions and loaded world bounds. **Owner-settled range (2026-10-19):** at comparable bow charge, the launched rope should have a **modestly shorter practical range than an ordinary arrow**, conveying the anchor/rope's weight without becoming useless for caves, ledges and ravines. Short draws still shoot shorter than full draws. Claude should compare charged rope and vanilla-arrow shots in the same unobstructed test setting, propose a sensible initial speed/arc and tune from actual playtesting; the owner did **not** specify a fixed distance in blocks. No prolonged ballistics investigation or new grappling behavior.
- **Existing M10 anchor rules:** A valid hit on a supporting solid underside/side face attaches the same M10 anchor, with a **vertically hanging climbable rope**; the ordinary 12-block funded rope extension, ground coil, climbing, retrieval, terrain refusal and removal rules remain intact. Shooting across a ravine does not create a horizontal traversable rope or teleport/pull anyone.
- **Conservation and refusal:** A miss, obstructed/invalid surface, unsupported block or unloaded destination must not create free rope or consume/duplicate resources. Successful use must follow normal manually placed Spelunker's Rope cost and ownership. No damaging arrow spawns alongside the anchor. Bow wear/charge tuning must be documented.
- **Verification:** Partial/full draw, valid underside and side hits, misses, physical obstructions; **offhand rope overriding stocked arrows without consuming them**, no-arrow rope shots, **normal arrow firing once rope is removed**; enchantments, charged limits, offhand stacks, placement denial, line/coil extension and recovery, save/reload, multiplayer and real client, damage/grappling negative controls, and no rope duplication.

Projectile speed/arc, practical distance and bow durability cost remain **implementation tuning values**, not further design blockers before READY. Record chosen defaults and actual-client results for owner review; don't depart from the slightly-shorter-than-an-arrow direction. Readiness **does not authorize implementation**.

## New owner-requested feature — nighttime bed rest heals at a hunger cost

**Owner direction — 2026-10-17; refinement of M01's no-sleep-skipping rule, not a reversal.** Beds currently function as spawn setters without sleep-time advancement. Make **lying in a bed at night** a useful, vulnerable recovery action: **restore health gradually while consuming actual hunger**, without skipping the night or changing the weather.

- **Night-only rest:** Players may enter and remain in a bed for the healing/rest interaction only **at night** under the ordinary safe bed conditions; no daytime healing by beds. Preserve bed respawn-point setting and normal leave-bed behavior. Respect M01's original **Nether/End bed behavior** and existing bed types (including straw beds); do not create safe beds in dimensions where vanilla beds are dangerous.
- **Vanilla nearby-monster restriction is retained:** If Minecraft would refuse sleeping with **“You may not rest now; there are monsters nearby”**, the player **cannot lie down or begin recovery**. Do not bypass or loosen the vanilla hostile-monster proximity check to enable healing. Refusal must cause **no bed healing and no hunger cost**, while otherwise preserving vanilla bed/respawn interaction behavior.
- **Never skip time:** Even if every player rests simultaneously, the Overworld clock, weather and hostile activity continue normally. Rest is neither a hidden time skip nor a time-speed multiplier. No phantom/insomnia penalty or mandatory daily sleep.
- **Exchange rate:** **Healing an entire empty health bar (10 hearts / 20 health points) consumes 2½ visible hunger drumsticks (5 food points)**, proportionally: each heart restored costs **¼ drumstick (½ food point)**. Use a fractional accumulator or other exact server-side accounting so partial healing cannot evade the hunger cost due to integer rounding. Healing must actually deplete the hunger meter; invisible saturation/exhaustion alone must not replace the specified visible cost.
- **Recovery pace:** Heal over time *while the player is actually resting*, not instantaneously on clicking a bed. **Initial playtest proposal:** 1 heart per 2 seconds, so 10 hearts take about 20 seconds; adjust only after owner feedback. Stop at full health, on leaving bed, on interruption/death or once available hunger cannot pay for more recovery. Never heal or consume additional hunger when rest is not active, and never create food or health through repeated enter/exit.
- **Food-pressure design:** Rest remains useful when ordinary natural regeneration would not trigger because hunger is low, **provided some hunger remains to spend**. Honor hunger availability for partial healing instead of offering free full recovery; keep existing M02 food/saturation UI, food rules and other healing methods otherwise intact.
- **Verification:** Day vs night; full, partial and zero health deficits; full, low and zero hunger; fractional cost and enter/exit interruption; multiplayer simultaneous resting with time/weather unchanged; valid spawn setting; hostile mobs continuing to act; **nearby monsters prevent lying down, healing and hunger expenditure** (including after a refused attempt); bed obstruction/leave/death; straw bed; Nether/End safety; dedicated server, save/reload, and no free-heal/food-accounting negative controls.

## New owner-requested feature — Shift + right-click glide to transfer items

**Owner direction — 2026-10-17.** While an inventory or transfer-capable container interface is open, **hold Shift and hold right mouse button, then glide the cursor across item slots** to **quick-move items out of each visited eligible occupied slot**. The player should not need to separately click each stack.

- On entering each **new eligible slot** during the held gesture, invoke the same **server-authoritative quick-move/shift-click stack transfer** that the menu supports. Transfer whole eligible stacks, **not** one item per slot and **not** vanilla right-drag item distribution. A slot should act at most once per continuous gesture; dragging over empty/blocked slots is harmless, and releasing Shift or right mouse button stops further transfers.
- Respect the screen's normal **source-to-destination rules** and existing M02 **hotbar-first shift-click priority**, M08 commodity admission, M10 Overalls slot availability/weight and capacity constraints, M11 protected/read-only cargo-inspection slot, and all locked, output, crafting, equipment, or otherwise restricted slots. Never bypass restrictions just because the player is dragging.
- Preserve unmodified right-click splitting/distribution, normal left-click, ordinary one-off Shift-click, and cursor-held-item behavior. If vanilla drag/quick-craft or another mouse gesture conflicts, this explicit **Shift + right-button drag** takes precedence **only when valid**; do not create a stuck drag state when the menu closes, focus is lost, or modifiers change.
- Each attempted move must be atomic, reconciled against the authoritative slot contents, and harmless if the destination is full or items are rejected. No lost/duplicated stacks, slot desync, unintended trashing or duplicate triggers on repeated mouse-over events.
- **Verification:** Multiple neighboring stacks transferred in one glide in player/container interfaces; slot re-entry once per gesture; empty/blocked/ineligible slots; hotbar-first priority; near-full container, incompatible stacks and weight caps; Overalls-equipped/un-equipped layouts; M11 disabled slot; vanilla right-drag/Shift-click unchanged; shift/release/focus loss mid-gesture; dedicated server, multiplayer and negative controls for duplication/slot bypass.

**Implementation defaults:** Start at **1 heart healed per 2 seconds** in bed and tune the pace from owner playtest without changing the fixed health-to-hunger cost. For menus without ordinary eligible quick-move actions, do nothing rather than invent a new transfer pathway; test and report unsupported cases. These are engineering details, **not pre-READY owner decisions**. Implementation still needs explicit authorization.

## New owner-requested feature — Sort button for chests

**Owner direction — 2026-10-17.** Add a visible, clickable **Sort** button to ordinary chest storage screens. Players should be able to tidy a chest in one click without moving its items into their personal inventory first.

- **Chest scope:** Support **single and double chests**, including their normal trapped-chest counterparts where they use the same chest inventory screen. This is **chest storage only**; no automatic sorting of barrels, hoppers, minecarts, custom machinery, or disabled/read-only inspection windows is authorized by this request.
- **One-click result:** Sort **only the chest's own storage slots** (27 or 54 as appropriate), *not* the player's inventory, hotbar, equipment, cursor stack or unrelated container. Leave the chest's accessible size, capacity, slots, item types and quantities unchanged. Sort on demand only, **not automatically** every time the chest opens or its contents change.
- **Match existing inventory sorting:** Reuse the M02 [InventorySort](../src/main/java/io/github/boxofdog/simpleblock/feature/InventorySort.java) semantics: safely merge only mutually compatible stacks with **identical item components**, respect per-item maximum stack sizes, then sort by item registry order with namespaced ID tie-breaks; fill rows **left to right from the top row** with empty slots at the end. Never combine enchanted, damaged, renamed or otherwise component-distinct stacks. The chest sort is a separate inventory target, **not** permission to invoke the player inventory's existing sort payload against a container.
- **Button/UI:** Show a small icon-style Sort control with a clear tooltip, visually consistent with the existing player-inventory button. Place it **without overlapping chest slots, player slots, the heavy-load bar, tooltips, screen bounds or other controls** at supported GUI scales, including double-chest screens. Avoid accidental activation when clicking slots.
- **Server authority and safety:** Request sorting through the actual open chest menu; server verifies the player's currently open, valid chest and operates atomically on its **current** inventory state. Reject stale/closed/incorrect screen requests and spectator/unauthorized actions. Support simultaneous viewers and double-chest halves without desync, dropping, duplication, race conditions or hidden storage. Respect ordinary access restrictions. Repeated clicks must be safe and idempotent.
- **Verification:** Real client single, double and trapped chests; occupied/mixed/empty/full inventories, stack consolidation, nonstackables and custom item components; repeated clicks; no player-inventory changes; opening/closing, concurrent viewers, client/server synchronization, save/reload, varying GUI scales, and negative tests for loss/dupes, stale-menu commands and invalid chest targets.

This addition **does not alter** M02's existing player-inventory sorting or M13's Shift + right-click item-transfer gesture. Implementation and test evidence require separate owner authorization; the current change is documentation only.

## Transferred playtesting changes from original milestones

Historical positive owner observations, previously implemented changes still awaiting owner confirmation, and unimplemented owner requests are all retained below with their original dates. The full bug diagnostic trail stays in [BUG_LOG](BUG_LOG.md).

### M07 — Ore skip, freight and cable

*Moved from [MILESTONE_07.md](MILESTONE_07.md); original dated owner notes and delivery/retest distinctions retained.*

**Owner feedback — 2026-10-13 (after M09 Build #37); not implementation verification or acceptance.** The ore skip's hauling and cargo handling work extremely well in playtesting. M07 remains PLAYTESTING; defect details live in the bug log, while new operating requirements live here.

**Open bugs:** See [BUG-003–005](BUG_LOG.md) for connecting-rod reach, incline travel and incline cable clipping, and [BUG-022](BUG_LOG.md) for Ore Skip Z-fighting. Symptoms, diagnostics, status and checks belong only in the canonical bug log.

**Owner-directed changes (not bug-registry duplicates):**

1. **Loading Rail stop and ore transfers (owner-approved M13/M15 boundary, 2026-10-19):** **automatically lower the hinged rail stop after a single freight cart's successful load/unload transfer completes**, so it cannot receive the same cargo again as a hoisted skip passes. Keep the train held during actual transfer and retain inventory, alignment, refusal and conservation checks; do not accidentally restart the consist when the individual stop lowers. **M13 fixes existing single-wagon transfer/release behavior and regresses existing coupled-cart holding safety only.** Automatic progression/redocking of successive wagons, simultaneous multi-bay servicing and a Cobblestone Engine's **Through/Load/Unload** selector belong to [M15](MILESTONE_15.md), which has separate unresolved station decisions. Do **not** change M06 winch/terminal safety or lower a stop on failed/refused transfers.
2. **Two-ended winch — implemented, pending owner playtest:** a straight, single cable joins two drums, with control at either end; the original one-drum return loop remains available. The last command wins, NEUTRAL stops, fuel stays at the powered home drum, and one coil covers the line. This is manually commanded hauling, **not** an automatic shuttle. See [Build #42 verification](MILESTONE_07.md#implementation-and-verification--2026-10-14-1).

3. **Heavy suspended-skip drop (owner-directed):** when a suspended ore skip **loses its winch/cable attachment** (e.g., drum or support removal, route disconnect), it should **fall rapidly and visibly under gravity**, like a heavy anchor falling, then play a satisfying metallic **CLANG** **when it actually lands or strikes a solid obstruction**. Do not play the clang at the instant of disconnection or while still falling. The present implementation already settles an unhooked skip downward at a fixed `skip.fall_speed` of **0.25 blocks/tick** but has no landing impact sound; implement appropriately weighty fall acceleration/terminal speed and an impact event that works across client/server. Preserve collision-safe landing and all 512 cargo items; **do not turn detachment into deletion, duplication or automatic cargo spillage**. A skip deliberately unhooked while safely supported stays at rest. Test suspended disconnects, short and long falls, intermediate obstructions, supported unhooking, reload and multiplayer sound synchronization.

**Change 2 — owner decisions (2026-10-14) and what was built:** last command wins; fuel only at the home end; keep both line types. One drum is the existing loop with its return wheel, worked from the drum. Two drums is one cable from drum to drum, worked from either end. Hand-cranked, both drums have identical cranks. With a coal winch, the second drum gets the lever logic but not the coal engine. The route stays straight, and either drum can unhook the cable (owner, 2026-10-14). Built as `cable/FarDrums`: holding the cable end, use an empty drum to make the end fast to it. There is no return strand or wheel. Each end's controls work relative to that end, and the levers show the same setting, mirrored. Coal goes only in the winch, and the second drum refuses coal, a furnace or a coil. Sneak-using either drum, or breaking either drum or an anchor, unhooks the cable and leaves an open route. Verification is in the appendix below.

**Exploratory design option — not yet approved:** for cable inclines, keep the anchor's mounting base fixed while shifting/tilting its roller toward the uphill corner. Any adopted geometry must agree across rendering, route clearance and connecting-rod grip. Investigate the actual defects under [BUG-004](BUG_LOG.md) and [BUG-005](BUG_LOG.md) before selecting an implementation; this candidate does not establish the cause.

**Confirmed working — terminal Loading Rail approach:** In an actual owner playtest, a single cable-hauled freight cart approached a terminal Loading Rail with the **last cable anchor directly above the rail** and slid smoothly onto the station. The transition was unobtrusive and worked without extra cable beyond the last support. **This specific straight/single-cart end-of-line approach is owner-verified and needs no correction.** Curve/slope approaches, coupled-train approaches, partial-chunk conditions and sequential wagon redocking remain separate, unverified/open cases.

#### Owner playtest follow-up — 2026-10-14 (unverified)

The owner reports additional issues and operating refinements in [BUG-006–008, BUG-013, BUG-018–021](BUG_LOG.md). Each bug's observed symptom and reproduction requirements remain in that single register; this section records the intended operating design, **not a completed implementation**.

- **Freight and controls (2026-10-15 lid addendum):** a correctly positioned cart should feed a skip from **either short end**. Make the skip's **top lid two separate doors opening outward to the left and right sides** of its receiving bin so neither front nor rear dumping path is obstructed; animate both doors opening for valid intake and closing on finish/interruption. Preserve the lower discharge-chute gate, conditional bridle, clearances and cargo conservation. Deliberate chain coupling should not accidentally place a chain above the rail, and the protruding winch lever must be targetable from useful oblique angles. Canonical detail and verification cases: [BUG-006](BUG_LOG.md#bug-006).
- **Coal winch inspection:** clicking the **engine box** should open a small coal-input inventory screen; the existing lever controls remain separate and reliable. Do not add a new fuel type or a second engine at the receiving drum.
- **Two-drum connection:** an empty receiving drum accepts a routed cable end whether it is grounded or hanging and regardless of receiver facing. One original coil suffices; **no coil is required on the receiver**, and attempting an attachment to an already coiled receiver must not create, lose, or spuriously eject cable materials. Maintain actual route obstruction, one-cable conservation and mirrored winch command semantics.
- **Selective routing and inclines:** using a held cable end on a chosen previously routed anchor should retract the appropriate existing section and leave a hanging end at that anchor, rather than forcing a full unroute. Investigate a **bounded**, physically credible lowering of an incline's receiving anchor/roller to clear local obstructions; the exact allowable extension remains to be tuned, and no solid-block bypass is authorized.

**Patch gate:** diagnose, implement, run focused client/server checks and obtain owner retesting. None of these observations changes M07's PLAYTESTING status or proves the behavior shipped.

**Implementation/acceptance gate:** These are fixes and a new owner-directed operating requirement, **not evidence they have been coded, tested or approved**. The earlier sequential coupled-wagon redocking issue remains open. Update verification with new build/test evidence after Claude implements a correction; **do not mark M07 SHIPPED** on the strength of the working skip alone.

---

### M08 — Blast Furnace

*Moved from [MILESTONE_08.md](MILESTONE_08.md); original dated owner notes and delivery/retest distinctions retained.*

**Owner feedback — 2026-10-13:** Furnace operation and commodity-weight mechanics worked well in actual play. The two refinements below were **subsequently implemented and CI-tested** (see 2026-10-14 appendix); owner acceptance and visual playtest remain pending.

#### Owner playtest naming correction — 2026-10-13

**Required name: `Blast Furnace`** instead of **`Pre-Industrial Blast Furnace`**, **across the entire mod, including internal identities**, not just the tooltip or documentation heading. Claude should audit/rename the Simple Block item/block display names, translated keys, item/block identifiers (currently `simple_block:pre_industrial_blast_furnace`), internal code and asset/model/texture/recipe identifiers and references, generated sources, tests, documentation and any other remaining project-owned usages. Preserve meaningful distinctions from the **retired vanilla blast furnace**; do not accidentally reactivate its crafting recipe or placement. Existing worlds using the prior mod block/BE/item identifiers must be protected through an explicit compatibility/migration strategy and tested with saved-world fixtures; don't silently make placed furnaces or inventory items disappear. Internal class names already using `BlastFurnace` need not be renamed merely to churn.

**Delivered:** renamed to **Blast Furnace** with old-ID compatibility tests in Build #39; an entire pre-rename world has not yet been owner-tested. M08 remains PLAYTESTING.

#### Owner playtest casting animation feedback — 2026-10-13

**Owner-clarified visual requirement — continuous, rhythmic casting, NOT a persistent inventory display:** **Fill the mold gradually with molten metal during the actual time needed to cook each storage block** (currently `blast_furnace.cast_ticks = 300`, or 15 seconds); at completion, let the full molten mass resolve into a visible, satisfying solid metal block. **Do not make that finished block instantly pop out of view when the next casting cycle starts.** It needs a perceptible visual moment, followed by a smooth transition into the next rising molten pour. **The furnace should keep cooking consecutive blocks automatically**, including while previously completed blocks remain in the mold's 64-item inventory. The visible block is **the current/just-completed cast**, *not* a permanent depiction of all blocks in storage.

- **Continuous loop:** progressively fill over one cast; show the completed solid block long enough to read it; on the next cast, briefly retain or gracefully transition/cover that solid appearance as the new molten fill begins, without a sudden empty-mold jump. Claude may tune the short visual hold/crossfade/occlusion against the existing cast-progress timeline. **No extra cooking delay, collection requirement or second production timer:** the next nine-ore batch progresses at the normal rate even during the solid-block visual hold, and the new fill catches up to its true progress.
- **Idle/blocked:** if no further batch can cook, the most recently completed block **may remain visibly in the mold while stored**. If the output is collected, it **may disappear immediately**—the owner explicitly does *not* require a removal fade, sink animation or delay. If a cast pauses midway (e.g., missing fuel or blocked output), its molten fill holds and resumes in sync with real progress; save/load and multiplayer must reproduce the correct stage.
- **Inventory versus visual:** retained output stacks do **not** lock the animation to an unmoving solid block; successive casts visually restart and finish even with earlier blocks accumulated in that same mold. Preserve existing displayed metal identity and handle first cast, repeated casts, storage full, interrupted casts and partial/total withdrawals. Never imply an extra collectible block or change the real stored count.
- **No gameplay changes:** preserve nine raw iron/copper/gold → one corresponding storage block, existing cast and fuel timings, 516 ore/64 finished-block capacities, safety/refusal logic and item conservation. Existing 30-tick **post-completion** pour effect alone is insufficient; move the primary fill animation into active processing and smoothly cycle into the next batch. Validate in a real client and dedicated/multiplayer connection.

**Delivered:** progressive pour and brief solid-block display passed Build #39 checks; its exact feel awaits owner playtest. M08 remains **PLAYTESTING**, not SHIPPED.

#### Owner visual playtest follow-up — 2026-10-14 (unverified)

[BUG-011, BUG-012 and BUG-015](BUG_LOG.md) record three new furnace observations/requests: active casting should visibly illuminate the **mold and front heat grates**, active filling should produce small, restrained **cinder particles** comparable to lava, and flickering/z-fighting at the **front metal plate and mold** needs investigation. Visual effects follow actual casting progress and interruption; neither changes the furnace's contents or timings. Preserve the owner's **hand-painted PNGs and UV mapping**—do not solve overlapping geometry by regenerating/repainting the asset. These are pending patch work and owner retest, not evidence of implementation.

---

### M09 — Hostile behavior

*Moved from [MILESTONE_09.md](MILESTONE_09.md); original dated owner notes and delivery/retest distinctions retained.*

**Owner feedback — 2026-10-13:**

- **Creeper retaliation:** owner reports it **works great** in actual play, with no correction requested at this stage.
- **Illuminated-area loitering and wooden-door breaking:** owner reports both **work great** and describes zombies frequently breaking doors; no correction requested at this stage.
- **Rare spider jockeys / ledge grabs:** **not yet observed by the owner**, consistent with intentionally rare recruitment; **not a reported failure and not owner-verified**. Keep the existing natural-jockey playtest gap open.

**M09 remains PLAYTESTING**, not SHIPPED; positive feedback on the observed behaviors does not count as blanket milestone acceptance.

---

### M10 — Workwear and rope

*Moved from [MILESTONE_10.md](MILESTONE_10.md); original dated owner notes and delivery/retest distinctions retained.*

#### Owner playtest follow-up — 2026-10-14 (unverified)

See [BUG-009, BUG-010, BUG-016 and BUG-017](BUG_LOG.md) for the canonical observations and acceptance checks.

- **Rope appearance:** make the surplus bundle look like actual coiled rope rather than a tube, with a thicker hanging strand and a sturdier anchor. Leave climbing, extension and item conservation intact.
- **Inventory layout:** the weighted-cargo bar must remain visible when the Overalls' extra inventory row is expanded, including the over-cap condition.
- **Trail Blazer (owner decision 2026-10-19):** attempt a genuine **object-following silhouette** rather than the existing highlight and **explicitly check for a performance drop**. Establish a repeatable before/after real-client benchmark at matching render settings, camera scene and representative entity counts; compare FPS/frame times (especially frame-time spikes) while the effect is active and inactive. Avoid unbounded per-frame scans, expensive repeated passes and client/server tick regressions. If true silhouettes cause a significant sustained frame-rate loss, flicker or renderer incompatibility, try **one bounded simpler alternative** and report findings to the owner rather than shipping a slow effect or silently removing the bonus. **Do not remove Trail Blazer mechanics or visual feedback without owner approval.**
- **Old Pro:** off-hand pickaxe mining is **a complete Headlamp + Overalls + Mining Boots set benefit**, not an unconditional offhand mechanic. First establish the intended controls and reproduce with the actual set active; only classify it as a functional regression after that check.

All four items await investigation, implementation where needed and owner retest. M10 remains PLAYTESTING.

---

### M11 — Cart inspection

*Moved from [MILESTONE_11.md](MILESTONE_11.md); original dated owner notes and delivery/retest distinctions retained.*

#### Owner cargo-inspection amendment — 2026-10-14 (unverified)

**Owner-approved interaction (2026-10-19), superseding only the historical M11 ban on a replacement GUI in section 5:** **Empty-hand right-click on a freight cart** opens a **small read-only freight-cart inventory window with exactly one disabled/grayed-out cargo slot** showing the actual current material and quantity. Do **not** display the player's own inventory or allow inserting, extracting, dragging, or changing cargo through this screen. Keep the no-floating-nameplate, no-hover-status and no-action-bar-narration rules, and keep cargo fills/gates visible. **Input priority:** Empty main-hand right-click opens inspection only when no higher-priority special interaction applies. Preserve held-cargo right-click loading, **Shift + right-click unloading**, cushion fitting, coupling/uncoupling and engine/connecting-rod actions. No unintended riding/mounting, transfers, editable slot, player-inventory panel or text overlay. The displayed item/count must reflect actual authoritative contents. Test empty/full carts, main/offhand precedence, crouching, client/server synchronization and no dupes. See [BUG-014](BUG_LOG.md) for the owner request and test focus.

**Patch gate:** the previous M11 implementation evidence documents behavior *before* this amendment. The GUI has not been implemented, tested or accepted. M11 stays PLAYTESTING.

## Implementation handoff — explicit stages and bounded investigations (owner-confirmed 2026-10-19)

**Only execute these stages after a separate owner authorization.** These are checkpoints **within M13**, not additional milestones or permission to invent new mechanics. Claude may sensibly group commits/tests, but should avoid a long unrelated rewrite or single gigantic unreviewable patch.

1. **Baseline/triage.** Read M13, affected source and canonical BUG_LOG entries only. Audit which earlier M07/M08 corrections are **already implemented** versus unverified owner requests. Reproduce actual failures with focused server/client checks before modifying them; don't redo delivered fixes.
2. **Industrial repairs.** Address reproducible cable/skip/winch/Blast Furnace/Loading Rail issues with narrow fixes, including M13's **existing single-wagon stop/release** behavior. Keep M15's three-state selector and sequential multi-wagon yards completely out of scope. Test item conservation, obstruction, refusal, stopped trains and regressions before moving on.
3. **Inventory and cargo UI.** Implement the empty-hand freight inspection screen, Shift + right-click drag-to-transfer, and single/double/trapped chest sorting. Test interaction precedence and ordinary quick-move, weight rules, network authority, component-preserving stacks and real-client layouts.
4. **Rest, rope and workwear.** Implement night-only bed healing with the vanilla nearby-monsters refusal, the shorter-than-arrow bow-launched offhand rope, and other scoped workwear/inventory corrections. Test normal bows, rope placement, food accounting, input conflicts and multiplayer.
5. **Render/visual and performance.** Fix remaining geometry/VFX, preserve owner-painted art/UVs and attempt Trail Blazer's **actual following silhouette**. **Measure before/after FPS and frame-time behavior in representative scenes**; do not ship a significant rendering regression or remove the bonus without asking the owner.
6. **Full verification/owner handoff.** Run the broad M01–M12 regression suite and focused negatives, actual client, dedicated server, save/reload and interruption tests. Update BUG_LOG with the real observed state, keep unsolved items OPEN, and append the single bounded milestone evidence section at the end of M13 with genuine CI, test and owner-playtest gaps. Do not infer SHIPPED from a successful build.

**Do not go on wild-goose chases:** For any stubborn issue, write down the reproduction, isolate the smallest relevant subsystem and try a focused fix. After **at most two credible failed approaches**, stop broadening the investigation; document what was tested, the blocker and a concrete next action or owner decision, then continue another independent M13 task. Escalate immediately before speculative architecture-wide rewrites, regenerating editable or painted assets, introducing a new framework, repeatedly trawling unrelated files, or changing the intended mechanics. An unresolved problem is **not** an invitation to silently defer, delete or mark it solved. Prefer minimal reversible patches, targeted test runs and genuine evidence over repeated full builds and report churn.

**Confirmed readiness decisions:** M13 repairs existing single-wagon Loading Rail behavior; M15 owns the new engine selector and advanced multi-cart station logic. Trail Blazer requires a genuine silhouette attempt **with performance measurement**; if unsuitable, request owner judgment. Bow-fired rope travels somewhat less far than an equivalent arrow but is still useful. **Empty-hand right-click inspects freight cargo**, while Shift + right-click still unloads. Remaining numerical tuning may be proposed and playtested rather than stalling READY.

## Patch verification and owner acceptance

1. Reproduce and triage each still-open or requested BUG entry; fix or record owner-approved deferral, with status and evidence in BUG_LOG. Do not treat a REQUESTED visual improvement as proof of a code defect.
2. Execute broad **M01–M12 regression**, especially cable/skip/cart/winch safety; metallurgical cargo/fuel state; commodity weights and workwear; rope; hostile AI and effigies; **nighttime rest, Shift + right-click drag-to-transfer and chest sorting**; user interfaces and rendering. Include negative controls that expose known regressions and server-authoritative no-duplication/refusal tests.
3. Validate changed UI, VFX and models in actual clients at multiple perspectives and GUI scales. Cover dedicated server, multiplayer, chunk/save/reload and interrupted transfers. Preserve hand-painted artwork and UVs.
4. Previously implemented M07/M08 follow-ups are not to be blindly reimplemented; obtain actual owner retest. **Include before/after Trail Blazer client performance evidence** and obtain owner approval before removing its silhouette/bonus or materially redefining the shorter-than-arrow rope launch.
5. After **separate, explicit implementation authorization**, place a dated `## Implementation and verification — YYYY-MM-DD` appendix at this file's **end** per [REPORTING_POLICY.json](REPORTING_POLICY.json), citing actual tested full SHA, CI artifact, test counts and gaps. No CI result alone makes M13 **SHIPPED**; only explicit owner signoff does.
