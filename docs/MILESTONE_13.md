---
milestone: M13
status: PLANNED - NOT READY
---

# M13 — Widespread bug-fixing, playtesting and feature-refinement patch

**Owner scoped 2026-10-15; documentation change only, not implementation authorization.** M13 centralizes playtest-driven corrections, cross-milestone regressions and usability refinements from M07–M11, with a newly requested bow-launched Spelunker's Rope mechanic. It does **not** reimplement or change the lifecycle states of original M01–M12 features. Their original requirements, implementation appendices and CI evidence remain in their milestone files. [BUG_LOG.md](BUG_LOG.md) is the sole authority for bug symptoms, diagnoses, status and fix verification.

## Patch scope

| Area | M13 playtesting and refinement focus |
|---|---|
| M03–M07 and M11 industrial freight | Cart/skip intakes, split skip lids, winch lever, rail-stop automation, coil/two-drum connections, cable inclines and selective disconnect, skipped-drop gravity/CLANG, coupling, owner retests. [BUG-003–008](BUG_LOG.md), [BUG-013 and BUG-018–021](BUG_LOG.md). |
| M08 metallurgy and capacity | Heat-grate/mold light, cinder particles, z-fighting and owner retests of previously delivered rename and casting animation. [BUG-011/012/015](BUG_LOG.md). |
| M09/M12 mobs and defenses | Verify light attraction, doors, creeper retaliation, rare jockey occurrence and effigy interactions. **No M12 defect has been reported in the moved notes.** |
| M10 access and workwear | Rope/coil visual refinements, weighted load bar, Trail Blazer silhouettes, Old Pro offhand control and bow-launched rope. [BUG-009/010/016/017](BUG_LOG.md). |
| M11 cargo UX | A read-only, one-disabled-slot cargo inspection window, with no player inventory or return of hover/action-bar narration. [BUG-014](BUG_LOG.md). |
| M01–M12 overall | Broad regression of resource conservation, survival, weight/inventory, hostile AI, freight systems, ropes, art, client/server and saved-world behavior. |

The present repair/retest pool is open or requested BUG-003–021; resolved/tested BUG-001 and BUG-002 warrant regressions, not reopening by assumption. Track defects and triage in BUG_LOG. Protect owner-painted textures, UV coordinates and editable Blockbench models. No unrelated systems, items or progression gates are authorized.

## New owner-requested feature — launch Spelunker's Rope with a dry-fired bow

**Interaction:** Hold **Spelunker's Rope in the offhand** and a **vanilla bow in the main hand**. Draw and release the bow **when it has no usable arrow ammunition** to shoot the rope's anchor at the crosshair target. The bow becomes an improvised rope launcher, **not** a separate tool, arrow attack, grappling hook or player-pulling system.

- **Input priority:** Ordinary bows still shoot genuine arrows normally when ammunition is available. Without a valid arrow *and* offhand Spelunker's Rope, preserve ordinary dry-fire behavior. Rope in the inventory/mainhand alone does not activate the mechanic; Infinity or other bow effects must not create limitless rope.
- **Charged distance and flight:** Short draws launch a short distance, full draws farther; a visible, understandable path/impact respects line of sight, real obstructions and loaded world bounds. Specific maximum range, draw curve, trajectory and feedback remain to be tuned/tested rather than invented as owner-settled values.
- **Existing M10 anchor rules:** A valid hit on a supporting solid underside/side face attaches the same M10 anchor, with a **vertically hanging climbable rope**; the ordinary 12-block funded rope extension, ground coil, climbing, retrieval, terrain refusal and removal rules remain intact. Shooting across a ravine does not create a horizontal traversable rope or teleport/pull anyone.
- **Conservation and refusal:** A miss, obstructed/invalid surface, unsupported block or unloaded destination must not create free rope or consume/duplicate resources. Successful use must follow normal manually placed Spelunker's Rope cost and ownership. No damaging arrow spawns alongside the anchor. Bow wear/charge tuning must be documented.
- **Verification:** Partial/full draw, valid underside and side hits, misses, physical obstructions, normal arrows and no arrows, enchantments, charged limits, offhand stacks, placement denial, line/coil extension and recovery, save/reload, multiplayer and real client, damage/grappling negative controls, and no rope duplication.

Unresolved numeric/visual defaults require a documented proposal and owner review; adding this design to M13 is **not permission to implement**.

## Transferred playtesting changes from original milestones

Historical positive owner observations, previously implemented changes still awaiting owner confirmation, and unimplemented owner requests are all retained below with their original dates. The full bug diagnostic trail stays in [BUG_LOG](BUG_LOG.md).

### M07 — Ore skip, freight and cable

*Moved from [MILESTONE_07.md](MILESTONE_07.md); original dated owner notes and delivery/retest distinctions retained.*

**Owner feedback — 2026-10-13 (after M09 Build #37); not implementation verification or acceptance.** The ore skip's hauling and cargo handling work extremely well in playtesting. M07 remains PLAYTESTING; defect details live in the bug log, while new operating requirements live here.

**Open bugs:** See [BUG-003–005 in the canonical bug log](BUG_LOG.md) for connecting-rod reach, incline travel and incline cable clipping, including symptoms, diagnostics, status and required tests. Do not duplicate their details here.

**Owner-directed changes (not bug-registry duplicates):**

1. **Loading Rail stop and ore transfers (owner-requested change):** **automatically lower the hinged rail stop after a minecart load/unload transfer completes**, so a still-docked receiving cart cannot inadvertently receive the same ore again when the hoisted skip is raised past it. Keep the cart held during the actual transfer and maintain all inventory/alignment/refusal safeguards. Clarify in implementation how this interacts with coupled carts and the separately open sequential-wagon redocking problem; do **not** undo M06's already-shipped safety rules or lower the stop on unsuccessful/refused transfer attempts.
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
- **Trail Blazer:** explore an actual object-following outline instead of the current unconvincing highlight. **Do not silently remove the effect**: the owner offered removal as a fallback if accurate silhouettes are impractical; confirm the choice before deleting visual feedback or related mechanics.
- **Old Pro:** off-hand pickaxe mining is **a complete Headlamp + Overalls + Mining Boots set benefit**, not an unconditional offhand mechanic. First establish the intended controls and reproduce with the actual set active; only classify it as a functional regression after that check.

All four items await investigation, implementation where needed and owner retest. M10 remains PLAYTESTING.

---

### M11 — Cart inspection

*Moved from [MILESTONE_11.md](MILESTONE_11.md); original dated owner notes and delivery/retest distinctions retained.*

#### Owner cargo-inspection amendment — 2026-10-14 (unverified)

**Newer owner direction supersedes only the historical M11 ban on a replacement GUI in section 5.** Provide a deliberately opened, **small read-only freight-cart inventory window with exactly one disabled/grayed-out cargo slot** showing the actual current material and quantity. Do **not** display the player's own inventory or allow inserting, extracting, dragging, or changing cargo through this screen. Keep the no-floating-nameplate, no-hover-status and no-action-bar-narration rules, and keep cargo fills/gates visible. Choose a clear inspection interaction that does not interfere with normal hand loading/unloading, cushion fitting, coupling or engine/rod actions. See [BUG-014](BUG_LOG.md) for the owner request and test focus.

**Patch gate:** the previous M11 implementation evidence documents behavior *before* this amendment. The GUI has not been implemented, tested or accepted. M11 stays PLAYTESTING.

## Patch verification and owner acceptance

1. Reproduce and triage each still-open or requested BUG entry; fix or record owner-approved deferral, with status and evidence in BUG_LOG. Do not treat a REQUESTED visual improvement as proof of a code defect.
2. Execute broad **M01–M12 regression**, especially cable/skip/cart/winch safety; metallurgical cargo/fuel state; commodity weights and workwear; rope; hostile AI and effigies; user interfaces and rendering. Include negative controls that expose known regressions and server-authoritative no-duplication/refusal tests.
3. Validate changed UI, VFX and models in actual clients at multiple perspectives and GUI scales. Cover dedicated server, multiplayer, chunk/save/reload and interrupted transfers. Preserve hand-painted artwork and UVs.
4. Previously implemented M07/M08 follow-ups are not to be blindly reimplemented; obtain actual owner retest. Obtain the owner's decision before dropping Trail Blazer's ghost outline or materially redefining the new rope launch.
5. After **separate, explicit implementation authorization**, place a dated `## Implementation and verification — YYYY-MM-DD` appendix at this file's **end** per [REPORTING_POLICY.json](REPORTING_POLICY.json), citing actual tested full SHA, CI artifact, test counts and gaps. No CI result alone makes M13 **SHIPPED**; only explicit owner signoff does.
