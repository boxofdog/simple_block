# Simple Block — Living Development Roadmap

**Project:** Simple Block (Minecraft Java Edition / Fabric)
**Roadmap version:** 0.7
**Updated:** 2026-10-11
**Owner:** Project creator
**Status:** Planning document. Not an authorization to implement all listed features.

> **North star:** Make Minecraft about mining, crafting, and physically transforming a dangerous world. Progress comes from creating secure territory and building useful infrastructure—not bypassing geography, darkness, or logistics.

## How to use this document

- This file records **what is done, what is currently authorized, and what might come later**. Detailed behavior belongs in milestone specifications and decision records, not in this roadmap.
- **Only the active handoff authorizes implementation.** A feature appearing in *Planned* or *Idea backlog* is **not** permission to build it.
- **Freeze active milestone scope.** Capture new ideas in the inbox. Only add them to an in-progress milestone if the project owner explicitly approves a scope amendment.
- Claude may update **factual progress, blockers, test results, and links** after work; Claude must **not silently promote ideas to requirements, change priorities, or mark unverified work as complete**.
- After each milestone, reconcile this document with the actual repository, tests, and user's acceptance. Do not infer completion from chat discussion or a partially completed code change.

**Status legend:** `DONE` = delivered and accepted; `ACTIVE` = authorized work under way; `HANDOFF READY` = a scoped assignment exists but implementation has not been verified; `QUEUED` = candidate next assignment; `PLANNED` = tentative future work; `DESIGN` = concept needs decisions; `PARKED` = consciously deferred.

## Current development snapshot

| Area | State | Notes |
|---|---|---|
| Milestone 01 — Survival foundation | **DONE (reported)** | Beds cannot skip nights; phantoms do not naturally spawn; elytra/gliding blocked; shulker-box use/acquisition disabled. Agent reported 22 PASS, 0 FAIL, 1 NOT RUN. |
| Local testing/deployment | **DONE (user confirmed)** | Dedicated Fabric instance and one-click updater are working. Keep this workflow intact. |
| Milestone 02 — World, mobs, inventory | **DONE (user confirmed shipped)** | Delivered and shipped per project owner. Formal QA totals/commit references can be linked from its repository closeout report; do not invent them here. |
| Milestone 03 — Freight cart and storage-bypass closure | **HANDOFF PREPARED** | One M03 handoff includes functional freight carts **plus** ender-chest and vanilla chest-minecart removal; pack animals remain. |
| Milestone 04 onward | **NOT AUTHORIZED** | Future work remains small, individually testable, and separately assigned. |

### Milestone 02 — Completed scope (historical record)

**Source of truth for details:** `docs/MILESTONE_02.md` and its test plan in the repository. This is a summary only; do not substitute this list for that specification.

- **Geology:** Remove granite, diorite, andesite, tuff from new-world generation; replace with stone above and deepslate below the appropriate transition; handle dependent ore features.
- **Mining:** Natural deepslate and deepslate ores require an iron-tier pickaxe; crafted deepslate variants retain vanilla harvesting requirements.
- **Ores:** Baseline iron scarcer in stone and somewhat richer in deepslate; further enrichment from mountain/peak banded formations, deep-ocean deepslate, arid alluvial deposits, and lava-adjacent pockets. Diamond target initially ~30–40% of vanilla. Test distributions rather than hardcoding unsupported claims.
- **Lighting and mobs:** Torches at light 12, lanterns at 15. At night, hostile mobs within 20 blocks become attracted to qualifying illuminated regions, including through walls; they mill around without automatically becoming aggressive. Normal player detection/pursuit takes priority.
- **Zombies and creepers:** Pursuing zombies break wooden doors at vanilla Hard-mode break speed; strike metal doors without damaging them. Creepers exposed to direct sunlight vanish in a brief smoke/ash poof without exploding.
- **Inventory/QOL:** 27 slots total (18 storage + 9 hotbar); sorting of main inventory by registry order, filling left-to-right and bottom-to-top without touching hotbar; shift-click hotbar filling prioritizes the leftmost available slot.
- **Bulk materials:** Shared 64-ingot-equivalent carrying allowance for iron, gold, and copper materials and agreed derivatives; implement now, not postponed until minecarts.
- **Hunger/UI:** Reduced exhaustion for routine activity; native saturation visualization.

**Closeout:** The project owner confirms Milestone 02 is completed and shipped. The milestone specification and test report remain the authoritative technical record. Add a commit/hash and QA totals only after checking the repository's actual closeout report; do not infer them from this roadmap.

**No retroactive scope expansion:** Any remaining polish or follow-up bugs should be tracked separately rather than silently added to the closed milestone.

## Milestone sizing rule (effective after M02)

**One milestone = one cohesive, demonstrable feature or narrowly connected capability, with its own acceptance tests.** Milestone 02 combined several substantial subsystems and took too much of a single Claude Code session. Avoid repeating that.

- Aim for a session-sized implementation, not a multi-system overhaul. If a milestone approaches the session limit, checkpoint it and continue without silently broadening the task.
- Split **foundation, visual polish, operation, and integration** when each can be tested independently.
- A first implementation may use working placeholders, but custom-model source and UV workflows must ultimately remain editable in Blockbench.
- Complete a build, automated verification, and actual-client checks where useful, then commit/document before beginning another milestone.
- Never have Claude automatically start the next milestone because the previous one passed. Owner authorization is required each time.
- Every proposed milestone ID below is tentative until reconciled with the actual repository roadmap and approved handoff.

## Proposed next milestones (small-scope planning, not authorization)

| ID | Working name | Narrow deliverable | Status | Notes |
|---|---|---|---|---|
| M03 | Freight cart and bypass closure | Open-topped, rail-compatible freight cart holding one bulk item type, 256 items max, manually loadable/unloadable with safe persistence; **disable ender chests and vanilla chest minecarts** | HANDOFF READY | Includes explicit negative tests for storage bypasses. **Keep donkeys, mules and llamas.** Finished cargo art, coupling, engines and cables remain future work. |
| M04 | Freight cart visuals | Editable Blockbench model/UVs and visibly changing cargo fill; preserve manually painted assets | PLANNED | Separate entity/rendering work from freight logic. |
| M05 | Cart coupling | Stable coupling/uncoupling of carts on basic rail routes without an engine | DESIGN | Define coupling rules and test corners/stops. |
| M06 | Coal locomotive | Reversible, fuel-powered engine that pulls **and** pushes a coupled consist | DESIGN | Specify fuel, speed and controls; powered rails not required. |
| M07 | Passenger cushions | Installing/removing a cushion enables/disables riding in an otherwise freight-only cart | PLANNED | Visible cushion and predictable cart interaction. |
| M08 | Copper cable anchors | Cheap **copper** anchors on wooden supports plus demonstrable cable placement/support geometry | DESIGN | Replace the earlier iron-nugget anchor concept. Overhead wire is a moving mechanical cable, not electrical redstone. |
| M09 | Winch and cable movement | One player-operated hand winch moves a connected overhead cable along a simple route | DESIGN | Distinct test of operating cable, before cart traction. |
| M10 | Copper-rod cable freight | Cart-mounted **copper connecting/gripping rod** engages overhead cable and hauls a cart along a supported route | DESIGN | Begin with one simple line; curves/branches separately if needed. |
| M11 | Vertical skip | Upright cargo skip and counterweighted hoist can move a load up/down a shaft | DESIGN | Reset/energy input and safe stops require design. |
| M12 | Dumping and transfer | Elevated dumping rail visibly transfers minecart cargo into an upright skip below | DESIGN | Separate from skip lift and engine mechanics. |
| M13 | Integrated logistics playtest | Verify that mines, freight, rail routes, and skips make sustained extraction more useful than hand hauling | PLANNED | Balance based on playtests, not new chores. |
| M14 | Redstone dependency audit | Decide keep/remove/rework for the existing circuitry-adjacent items, recipes and features | DESIGN | **Design/research milestone only.** No broad removal on the first pass. |
| M15+ | Subsequent small assignments | Redstone changes, armor/loadouts, metalworking, guidebook, village overhaul, etc. | DESIGN | Split each larger theme into individual milestones only after separate design approval. |

### Village overhaul — future workstream, not active

Village redesign is an **owner-approved design direction** but remains unscheduled. It must **not** be inserted into M03 or implemented as one giant "village overhaul" milestone.

Suggested separable future assignments: (1) site suitability / elevation filtering; (2) larger and connected structure templates; (3) population and meaningful daily destinations; (4) nighttime pub congregation; (5) centralized general-store trading; (6) profession-specific workplace services; (7) responsive iron-golem defense. Each will need a focused handoff, gameplay tests, and design decisions before authorization.

**Note:** The milestone IDs above are organizational placeholders; they do not override any IDs already in the repository.

## Feature decisions and constraints to preserve

- **Minecraft first, complexity second:** Avoid survival chores that do not create a meaningful reason to build.
- **Physical infrastructure over magical convenience:** Minecarts, skips, rails, walls, workshops and lighting should solve actual problems.
- **Freight cart initial capacity:** Start playtesting at **256 bulk cargo units per cart**, holding **one item type at a time**. This is an initial balance target, not a confirmed final capacity. The cart should represent heavy physical freight, not an ordinary multi-slot chest; loaded carts should not trivially become portable bulk storage.
- **Transport loopholes (owner decisions):** **Remove ender chests** and **remove vanilla chest minecarts in M03**, including ordinary survival acquisition and use routes; **retain donkeys, mules and llamas** as slower, physical cargo options. Do not remove regular empty/passenger minecarts as part of this change.
- **Mechanization, not unattended resource creation:** Transport and handling machinery is welcome; industrial farming/automation should be severely limited. Exact exceptions remain under design.
- **Visual models are editable assets:** Source `.bbmodel` projects, UVs and replaceable textures belong in the repository; user paints art; avoid overwriting hand-authored textures.
- **Distinct uses for copper:** Overhead hauling cable **anchors and minecart connecting/gripping rods use copper** rather than iron. Their exact recipes are undecided; cheap anchors remain the economic goal. The material of the moving high-tension hauling cable itself is **not** decided. Keep this for its dedicated future milestones, not M03.
- **New-world focus:** Legacy-world migration/compatibility is not a project requirement; retain internal vanilla registrations where necessary for technical stability.
- **Test deliberately:** Use Minecraft client runs, screenshots, scripted in-world tests, comparative controls, and Gradle tests where they actually demonstrate required behavior. Describe what was NOT tested.

## Decision queue (not permission to implement)

- Freight cart cargo: **256 units and one bulk item type per cart** are initial owner decisions; decide allowed bulk commodities, whether quantities differ for compacted/processed materials, the loading/unloading interaction and final playtested capacity.
- Ender-chest/chest-minecart removal: choose a minimal standalone implementation pass and verify all acquisition, storage, and loot paths; deliberately preserve slower pack-animal carrying.
- Coal locomotive controls, effective speed/fuel baseline, train maximum length, coupling rules.
- Timber/cable route geometry, **copper** anchor recipe/crafting yield, actual hauling-cable material and cost, curve handling, winch behavior.
- Copper connecting-rod recipe, whether a hinge/grip needs additional components, and how the rod connects/releases from the moving overhead cable.
- Skip capacity, counterweight reset, station layout and handling of multiple carts per skip.
- Redstone-dependent utility-by-utility keep/remove/replace decisions.
- Scope and economics of anvil/smithing redesign; ordinary-use anvil degradation should not remain the default long-term direction.
- Any balancing problems exposed by Milestone 02 playtests; record actual data before changes.
- Future equipment system: define distinct spelunking, raiding/combat, and mining loadouts; conventional vanilla/BTA armor progression is **not** the intended model.
- Alluvial iron economy: decide whether added shallow iron sand/gravel requires less surface iron elsewhere, more iron demand, or a combination. Decide extraction/processing mechanics before implementation.
- Steel manufacturing: decide purpose, production steps, and interaction with future smithing; do not simply copy BTA recipes.
- Guidebook: decide discovery and presentation rules; informational reference, not arbitrary recipe gating unless explicitly approved.
- Villages: decide valid terrain-relief thresholds, structure footprint/layout standards, housing-to-population relationship, daily destinations, pub schedule/safety, general-store stock model, and which professions provide which services.
- Village defense: establish iron-golem attack-response range, prioritization, patrol patterns, and interaction with light-attracted nighttime mobs; do not assume existing vanilla behavior is sufficient.

## Idea inbox (capture first; discuss and prioritize later)

| ID | Idea / question | Origin | Status | Next action |
|---|---|---|---|---|
| I-001 | Developed smithy: forge tool components at an anvil, assemble at crafting table; avoid making first iron tools depend on a 31-ingot anvil | Brainstorm | DESIGN | Separate future design conversation |
| I-002 | Anvils should be effectively permanent under ordinary use, with a cost appropriate to usefulness | Brainstorm | DESIGN | Decide when metalworking is scoped |
| I-003 | Broader inventory/world-content cleanup beyond the four confirmed rock variants | Brainstorm | PARKED | Specify exact block list before approving |
| I-004 | **Redesign armor from first principles:** make conventional armor progression effectively inaccessible; create genuinely distinct preparation/loadouts for spelunking, raiding/combat, and mining. Do not assume vanilla or BTA armor rebalance. | BTA comparison; owner-approved future direction | DESIGN | Define protective roles, crafting/acquisition, weight and equipment interactions in a separate design pass |
| I-005 | **Climbable ropes** for early exploration, cave access, and temporary descent; complement, rather than replace, developed shafts | BTA inspiration; owner-approved future feature | DESIGN | Specify placement, retrieval, traversal, and crafting |
| I-006 | **Faster ladders**; ordinary vertical movement should not be so tedious | Owner-approved future improvement | DESIGN | Choose climb speed and verify interactions with ropes, shafts, and skips |
| I-007 | **Alluvial iron sand and iron gravel**, naturally occurring at appropriate riverbanks; require new original textures. This is a proposed additional deposit *form* beyond M02's existing alluvial enrichment, not an amendment to M02. | BTA-inspired prospecting discussion; owner-approved future direction | DESIGN | Design finite deposits and extraction/processing; reconcile surface iron availability with iron demand and huge mountain veins |
| I-008 | **Steel** as a crafted/manufactured material with a distinctive purpose, using a production system of our own design rather than BTA's | BTA inspiration; owner-approved future direction | DESIGN | Decide properties, production chain, and smithy/anvil integration |
| I-009 | **Recipe guide / guidebook** for learning about tools, materials, and machines as the player discovers them | BTA inspiration; owner-approved future feature | DESIGN | Decide discovery presentation, recipe navigation, and whether any unlock restrictions exist (none assumed) |

## Future design backlog — inhabited villages

**Status: OWNER-APPROVED DIRECTION / NOT IMPLEMENTATION-AUTHORIZED.** These are desired outcomes, not permission to modify village generation or AI before a dedicated milestone.

### Settlement geography and architecture

- **Restrict bad sites:** Strongly discourage or reject village generation when the planned village footprint contains extreme elevation variation, cliffs, or terrain that would create disconnected buildings, sheer paths, or dirt pedestals. Favor contiguous, reasonably level land; mountain plateaus and manageable slopes remain possible.
- **Larger, convincing structures:** Houses should feel like homes, with enough room to enter and inhabit. The same applies to professional workshops, general stores, and communal buildings; avoid the tiny playset-like interiors of many vanilla structures.
- **Real layouts:** Construct connected, traversable streets, coherent building placement, an intelligible village center, and accessible public spaces. Building interiors and doors must support villager and player navigation.
- **Population density:** Villages should feel actually populated. Resident counts ought to relate sensibly to available homes, workplaces, and village scale, rather than leave large settlements nearly deserted.

### Daily life and communal destinations

- **Purposeful schedules:** Villagers should prioritize identifiable destinations beyond their homes during the day: workplaces, shops, public areas, and social spaces. They should visibly inhabit and use the buildings that define their village.
- **Pub / tavern:** Include a suitably large communal pub where **most villagers gather and mingle during the evening/night**. It must support multiple villagers inside at once instead of operating as a decorative empty building. Later return-home/sleep schedules need design, not assumption.
- **Profession presence:** An existing smithy should reliably have a smith at work; other workplaces likewise should have their associated professionals present and visibly working when appropriate.

### Commerce, skilled work, and services

- **General store:** Ordinary buying and selling occurs at a centralized general store, with a shopkeeper stationed **behind a counter** during trading hours.
- **Workplace services:** Professionals offer specialized **services at their workplaces**, rather than every wandering worker simply carrying a personal trade menu. Smithing, repairing, mapmaking, and similar functions are examples to design, not currently finalized mechanics.
- **Avoid economic shortcuts:** Village services must not make the mod's mining, crafting, or freight economy obsolete. Shared store inventory versus consolidated trades, stock limits, replenishment, and pricing are still open decisions.

### Real protection and sensible nighttime survival

- **Iron golems must actually defend villagers:** Golems should notice and urgently respond when a monster attacks a villager, with reliable target prioritization and meaningful patrols near populated or vulnerable parts of the village.
- **Monsters should threaten villagers appropriately:** Existing hostile behaviors and new light-entrancement rules must interact coherently with village safety, rather than golems ignoring attacks or villages being routinely depopulated.
- **Safe gathering:** A pub that draws villagers together at night must have plausible protection. Avoid designing social routines that simply turn villagers into helpless victims.

### Candidate village milestones — explicitly not scheduled

1. Village footprint terrain-suitability checks.
2. Larger, enterable buildings and coherent navigable village layout.
3. Population scaling and workplace occupancy.
4. Social-destination scheduling and pub.
5. Centralized general-store trading.
6. Profession-specific services.
7. Responsive golem defense and nighttime safety.

**Out of scope until discussed:** Complex autonomous economies, village simulation/management, town growth systems, and automatic industrial production.

## Explicitly rejected BTA-inspired directions

- **Seasons:** Not planned for Simple Block.
- **Painting / cosmetic coloring system:** Not planned for Simple Block.

These rejections do not prohibit ordinary visual polish or other environmental mechanics, but they should not re-enter the roadmap as features without a new owner decision.

## Change log

| Date | Change | Approval / evidence |
|---|---|---|
| 2026-10-11 | Roadmap initialized; M01 and deployment recorded as reported/confirmed; M02 frozen; future logistics and crafting organized as proposals | Based on project discussion; M02 status awaits actual code/QA report |
| 2026-10-11 | v0.2: Added nontraditional armor redesign, ropes, faster ladders, alluvial iron sand/gravel, steel, and guidebook to future design backlog. Rejected seasons and painting. No change to active M02 scope. | Explicit project-owner decisions in BTA discussion |
| 2026-10-11 | v0.3: Marked M02 shipped; split oversized proposed milestones into narrow, session-sized deliverables; added detailed future village redesign (terrain, buildings, population, pub, store, workplaces, golems). | Owner confirmed M02 shipment and explicitly requested village roadmap update / smaller milestones |
| 2026-10-11 | v0.4: Specified **copper** for overhead cable anchors and the cart-mounted connecting/gripping rod; preserved moving cable material and recipes as open decisions. | Explicit owner choice; no implementation authorized yet |
| 2026-10-11 | v0.5: Set freight-cart playtest capacity to **256** and **one item type per cart**; recorded ender-chest/chest-minecart removal and pack-animal retention. | Explicit project-owner logistics decisions |
| 2026-10-11 | v0.6: Updated future priorities and created an initial narrowly scoped M03 handoff. | Owner decided existing depletion and bulk-hauling economics provide sufficient incentive; authorized M03 handoff preparation. |
| 2026-10-11 | v0.7: M03 now **includes ender-chest and vanilla chest-minecart removal**, while preserving pack animals, alongside the 256-item single-commodity freight cart. No separate bypass task. | Owner explicitly requested both removal changes in the M03 ship. |

## Milestone closeout template

For every milestone, append or link a short report recording:

1. **Delivered:** actual implemented behaviors and relevant commit/hash.
2. **Verified:** test totals, in-game evidence, and notable controls.
3. **Not verified:** explicit gaps and reasons.
4. **Changed from spec:** deviations with owner approval, not assumptions.
5. **Design feedback:** what felt good, what needs rebalancing.
6. **Next handoff:** precisely one authorized scope, with acceptance criteria.

**Owner's rule:** Nothing moves from `DESIGN`/`PLANNED`/`QUEUED` to `ACTIVE` merely because it appears in this roadmap. The project owner authorizes the next milestone explicitly. Prefer one tested capability per Claude Code session.
