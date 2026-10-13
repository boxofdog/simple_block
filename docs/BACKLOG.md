# Simple Block — Future Design Backlog

Ideas here are **not approved implementation tasks**. The short [roadmap](ROADMAP.md) owns order and progress. Confirmed design choices are in [DECISIONS.md](DECISIONS.md); implementation reports are in the milestone files.

## Additional ideas

| ID | Idea / question | Origin | Status | Next action |
|---|---|---|---|---|
| I-001 | Developed smithy: forge tool components at an anvil, assemble at crafting table; avoid making first iron tools depend on a 31-ingot anvil | Brainstorm | DESIGN | Separate future design conversation |
| I-002 | Anvils should be effectively permanent under ordinary use, with a cost appropriate to usefulness | Brainstorm | DESIGN | Decide when metalworking is scoped |
| I-003 | Broader inventory/world-content cleanup beyond the four confirmed rock variants | Brainstorm | PARKED | Specify exact block list before approving |
| I-004 | **Modular workwear redesign:** retire gold/chainmail; copper Headlamp, one-piece Overalls occupying chest AND legs (+9 slots, low protection). **Spiked Boots alone: Spelunker's Speed**, replaced in full outfit by **Trail Blazer** (climbing + yellow/blue outlines + skeleton aim reduction). **Mining Boots alone: Mule** (overweight carrying with slowdown), replaced in full outfit by **Old Pro** (retains carrying and adds offhand pickaxe mining plus bottom-edge warning ONLY for hostile monsters behind within 20 blocks and with direct eye-line to Steve's second-block-high neck). Prior +5% mining-speed idea superseded. | Owner-approved future direction; detailed tuning pending | PARTIALLY SCOPED — not implemented | Shaped recipes settled; determine headlamp lighting, output counts/stats, safe two-slot equip/backpack, offhand mining, rear-angle/neck ray detail, Trail Blazer and balance |
| I-005 | **Rope + Spelunker's Rope:** three vertical strings craft rope; rope under an existing copper cable anchor crafts a mountable Spelunker's Rope. Attach beneath or beside a block, hang vertically in open air, hold right-click with rope to extend by 12-block lengths, and coil extra length on ground. Climbable temporary cave access, not a moving haulage cable. | Owner-approved future feature | SCOPED — not implemented | Detail climb speed, exact consumption/yield, retrieval, clipping/support failure and coil behavior only when preparing its later milestone |
| I-006 | **Faster ladders**; ordinary vertical movement should not be so tedious | Owner-approved future improvement | DESIGN | Choose climb speed and verify interactions with ropes, shafts, and skips |
| I-007 | **Alluvial iron sand and iron gravel**, naturally occurring at appropriate riverbanks; require new original textures. This is a proposed additional deposit *form* beyond M02's existing alluvial enrichment, not an amendment to M02. | BTA-inspired prospecting discussion; owner-approved future direction | DESIGN | Design finite deposits and extraction/processing; reconcile surface iron availability with iron demand and huge mountain veins |
| I-008 | **Steel** as a crafted/manufactured material with a distinctive purpose, using a production system of our own design rather than BTA's | BTA inspiration; owner-approved future direction | DESIGN | Decide properties, production chain, and smithy/anvil integration |
| I-009 | **Recipe guide / guidebook** for learning about tools, materials, and machines as the player discovers them | BTA inspiration; owner-approved future feature | DESIGN | Decide discovery presentation, recipe navigation, and whether any unlock restrictions exist (none assumed) |
| I-010 | **Post-M10 full-set bonuses for existing iron, diamond and netherite armor:** **Cast Iron Crusader** (zombie/arrow knockback immunity; target mob's remaining HP after hitting), **Iridescent Infantry** (full-strength but shorter-range arrow reflection; zombie teeth break, hurting and weakening them), **Netherfire Knight** (piglins never fight back and give better barters; hostile attackers ignite) | Owner-settled post-M10 direction; NOT M10 scope | DESIGNED — not implementation-authorized | Future dedicated brief: set checks, numerical effects, UI, arrow physics, zombie weakening, piglin behavior, tests; no new armor items |

| I-012 | **Witch's Tome and exploration-gated enchanting:** witch huts on mountain peaks and in deep deepslate caverns contain an enchanting table; breaking it drops one diamond and a Witch's Tome, which is required to craft a new enchanting table. | Owner-proposed post-M10 direction | DESIGN — not implemented or authorized | Determine hut rarity/placement, table drops and crafting recipe, whether each new table requires a tome, and safeguards against bypass/farming |

### I-010 — Post-M10 vanilla combat armor roles (owner-settled, NOT authorized)

**Existing complete sets only (helmet + chestplate + leggings + boots; no new tiers):**
- **Iron / Cast Iron Crusader:** wearer cannot be knocked back by **zombies or arrows**; hitting a mob reveals its **remaining hitpoints**.
- **Diamond / Iridescent Infantry:** incoming **arrows reflect back** at **full strength but reduced distance**; a zombie that bites the wearer **breaks its teeth**, taking damage and dealing reduced damage afterward.
- **Netherite / Netherfire Knight:** **piglins never fight back** and offer the wearer **better trades/barters**; hostile mobs that hit the wearer **catch fire**.

Full-match set activation, numerical damage/slowdown/range, display style, durations, zombie-state persistence, piglin barter weights and test design belong to a **later standalone implementation brief**. Never silently add these to M10 or M06–M08. M10's **Trail Blazer/Old Pro** are separate workwear bonuses; gold/chainmail retirement still belongs to M10. Design details live in [DECISIONS.md §5.7](DECISIONS.md).

### I-004 — Modular armor roles and integrated overalls backpack

- **Head:** a copper **Headlamp**, protection just below iron head armor. Lighting behavior has not been decided.
- **Chest + legs:** **ONE Overalls item** simultaneously occupies/reserves both chest AND leggings slots; it blocks combining backpack capacity with either conventional chest or leg armor. It restores the missing nine slots (**27 → 36 total**) and offers at most the combined protection of leather chestplate and leggings. Dual-slot equipping is settled as a design rule, with implementation details pending.
- **Feet (choose one):** **Spiked Boots** grant **Spelunker's Speed** for faster rope/ladder climbing; **Mining Boots** grant **Mule**, allowing heavy loads past 64 units with progressive movement slowdown. Each works without other workwear; neither grants backpack inventory slots. Their displayed boots-only bonus is replaced by Trail Blazer or Old Pro respectively when the complete matching outfit is worn, preserving the base function without duplicate modifiers.

Both boots work with the same overalls, so users can change specialization with **one equipment swap** rather than replacing an entire outfit. **Spiked Boots alone display Spelunker's Speed; full Headlamp + Overalls + Spiked Boots replaces that displayed effect with one named bonus, Trail Blazer**, keeping faster climbing (no double multiplier), adding reduced skeleton firing accuracy and subtle through-wall outlines of nearby torches, ladders and ropes (**yellow** player-placed, **blue** world-generated). **Mining Boots alone display Mule** (heavy-item overload with progressive speed penalties); **full Headlamp + Overalls + Mining Boots replaces the displayed Mule effect with Old Pro**, retaining overweight carrying while enabling pickaxe block mining from the offhand as the main hand remains free for combat/chopping, and producing a **red bottom-screen-edge glow ONLY for a hostile monster behind the player within 20 blocks with a direct unobstructed eye-line to Steve's second-block-high upper body/neck**. Do not warn for monsters hidden behind solid walls. No free offhand combat, double mining rate or omnidirectional enemy radar. The previous tentative **+5% mining speed is superseded**. Full-set effects disappear as soon as any required piece is removed. Gold and chainmail disappear from normal creative/survival play with safe treatment of old registry IDs. Exact rear-angle/upper-body ray target height, offhand mining controls, skeleton penalty, visualization range/fade, provenance tracking/fallback, recipe output counts and safe overalls-backpack unequip are left to a dedicated future armor brief; M05/M06 remain untouched.

#### Workwear shaped crafting recipes — settled

#### Exact shaped workwear recipes (owner decision; not implemented)

All four recipes are **shaped 3×3 crafting layouts**. The specified positions and empty slots matter; do not convert these to shapeless crafting recipes. "Chest" means the **ordinary vanilla chest block/item**; "leather pants" means **vanilla leather leggings**. Reuse vanilla copper ingot, torch, string, leather armor, pointed dripstone and iron ingot; no additional intermediate ingredients.

**Headlamp** — 5 copper ingots and 1 torch:

```text
[copper ingot] [copper ingot] [copper ingot]
[copper ingot] [torch]        [copper ingot]
[empty]        [empty]        [empty]
```

**Overalls** — 2 string, 1 vanilla chest, 1 leather chestplate, 1 leather leggings:

```text
[string] [chest]              [string]
[empty]  [leather chestplate] [empty]
[empty]  [leather leggings]   [empty]
```

**Spiked Boots** — 1 leather boots, 1 pointed dripstone underneath:

```text
[empty] [empty]            [empty]
[empty] [leather boots]    [empty]
[empty] [pointed dripstone][empty]
```

**Mining Boots** — 1 leather boots, 1 iron ingot underneath:

```text
[empty] [empty]         [empty]
[empty] [leather boots] [empty]
[empty] [iron ingot]    [empty]
```

Recipe layouts and components are settled. The number of output items per craft is not explicitly set here; default crafting behavior may be proposed in the future milestone brief. Headlamp's torch ingredient does not itself decide illumination mechanics or fuel consumption, which remain open. Overalls still occupy BOTH chest and leggings slots simultaneously.

### I-005 — Spelunker's Rope recipe and placement direction

```text
Rope:                      Spelunker's Rope:
[empty][string][empty]     [empty][empty][empty]
[empty][string][empty]     [empty][copper anchor][empty]
[empty][string][empty]     [empty][rope][empty]
```

The anchor is the existing copper cable anchor item used as a crafting ingredient; the finished Spelunker's Rope may attach to a block's underside or side. Its hanging rope drops vertically with no intermediate blocks. Holding use with additional rope extends the line in 12-block increments; on reaching ground, excess rope coils visibly on the ground block. Avoid conflating this with the moving overhead haulage cable. Exact recipe output count, extension costs, climb/retrieval controls and block-obstruction behavior have not been chosen; they may be resolved in a separate implementation brief. No authority to implement during M05.

## I-011 — M09 settlement defense and emergent monster behavior (owner-settled direction; NOT authorized)

Develop the existing night/light-entrancement system into meaningful settlement defense without raids or new hostile mobs. **Owner playtest finding:** the slow trickle toward light looks natural and should remain, but monsters abandon their interest too quickly. Increase persistence of attraction/loitering without creating an unnatural synchronized rush; decide timing and test in M09. Nearby monsters outside walls already remember illuminated positions and loiter: when a gate, door or wall breach makes a previously unreachable lit destination reachable, they should re-evaluate navigation and temporarily favor the new route, then resume ordinary lit-area loitering or player pursuit. No magical attraction to individual lantern blocks, and no requirement that mobs conceptually understand 'bases'. Explore a modest chance that skeletons actively seek and mount nearby spiders while keeping jockey frequency limited; mounted skeletons may assist spiders over defensive overhangs with a brief grab/pull-back animation and interruption of bow use. Explore a large winch-operated iron gate composed of two three-block-wide sections, and narrowly conditioned retaliation by a creeper wounded by a sheltered player, potentially attacking that gate. Exact triggers, spawning, path refresh, gate shape, safety rules, griefing limits, animation, and tests require a standalone M09 brief. M09 is ahead of M10 workwear; vanilla armor bonuses remain after M10. This is design only, not an implementation authorization.

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

## Areas needing design before implementation

- Train coupling, reversible coal locomotive, fuel efficiency and gradients.
- Hand winch operation and mechanical cable traction; turning/guide pulleys, grip clearance, alternate track alignments.
- **M06 OWNER-AUTHORIZED WORK (2026-10-12; M05 owner acceptance deferred):** also **replaces the existing Cable and Cable Coil crafting recipes** with shaped center-column patterns: **three iron nuggets vertically → cable (no string)**, and **three cable items vertically → coil (no 8-cable ring)**. All other grid slots empty; see [MILESTONE_06.md](MILESTONE_06.md). **Owner-settled: ONE installed Cable Coil now provides UNLIMITED cumulative cable route length** — remove the M04/M05 **128-block drum cap**, 4-block-per-cable consumption, extension-coil requirement and return-loop strand charge. Preserve **physical anchors/supports, ≤12-block support spans**, clear routes and locomotive/winch behavior. A dismantled line returns **exactly its one installed coil**, not cable determined by length. **Update the existing coil→cable reverse craft to 3 cables (not 8) if retained**, avoiding duplication from the new three-cable input. **Implementation defaults:** Cable recipe outputs 2, three cable items craft 1 coil, coil uncrafting returns 3 cables; Claude updates weights and tests for duplication. Do not treat the M04/M05 historical 4-block-cable/32-block-coil numbers as new M06 requirements. Includes **retired-item Creative inventory and search cleanup**: audit already-disabled M01–M05 items (e.g. elytra, shulker boxes, retired rock families, ender chest/chest minecart and the vanilla furnace-minecart item), plus M06's retired redstone rail types. Hide them from Creative tabs/search, check existing acquisition restrictions, retain compatibility IDs and ordinary useful items. **Do not prematurely retire the M08 blast furnace or M10 gold/chainmail armor.** Also apply a **vanilla furnace to the free side of the existing winch drum**, converting crank to a **three-position, latching lever** (**PULL/backward, NEUTRAL/center, FEED/forward**). **Right-click lever cycles NEUTRAL → PULL → FEED → NEUTRAL**, including **direct but safe PULL→FEED reversal**, and stays at each setting: PULL/FEED **continue the haul without holding the mouse or remaining beside the machine**. **At a derailment/rail end/loading track or M07 skip station/shaft endpoint, the lever automatically resets to NEUTRAL**, ending motion and coal burn; it cannot restart a route autonomously. Right-click drum with **coal to insert**; right-click body empty-handed to **inspect stored coal**. Powered hauling is faster/more efficient; burn pauses when motion stops and preserves exactly the remaining burn time of partially used coal across stops/save/load. **Settled:** the complete three-state right-click sequence (NEUTRAL → PULL → FEED → NEUTRAL). **Loading track:** cart arrival docks/stops and holds its whole coupled consist, including safely shutting down running Cobblestone Engines; **right-click loading track releases** the consist without automatic restart. **M06 delivers the actual stopping rail and freight-cart front-gate/ramp model, M07 the upright skip, conditional gate motion and cart↔skip cargo transfers.** **Authorized defaults / Claude discretion:** dock every arriving ordinary uncushioned freight cart, right-click releases after a safe departure latch, 1 loading track per craft, 1 coal item inserted per right-click; choose fuel capacity, speed and precise facing, return to neutral when fuel empty. **Mandatory Claude-owned technical blocker:** existing `CableLoops.ticking` requires *all* anchors entity-ticking; resolve for **long lines with unloaded faraway anchors**, without forcing chunks, moving unloaded vehicles or burning coal while stopped. Test >128-block supported lines, local active travel, chunk unload/reload, fuel/cargo preservation and full regression coverage; report architectural limits honestly. No new power grid or autonomous recurring freight system; counterweights excluded. **M07 HANDOFF-READY (owner-settled, NOT IMPLEMENTATION-AUTHORIZED):** the complete [MILESTONE_07.md](MILESTONE_07.md) specification now contains staged work and a test matrix.  upright skip has **1×1×3 body collision**, **collidable bridle in the fourth block up**, and **one lateral block of visual clearance on each side** for its sideways-minecart silhouette. One commodity is held in **ONE inventory UI slot supporting a maximum stack of 512**, without changing normal stack limits. **Cargo direction uses aligned skip SECTION, never empty/full state or an empty frontage block:** cart beside the fourth-block bridle **feeds skip**; cart beside the first/bottom skip block **receives from skip's lower chute**. Physical collisions still stop unsafe operation. Claude chooses transfer timing and docking tolerances; safe station/shaft stops reset the powered lever to neutral. Actual skips, transfers and collisions require later M07 authorization, not M06. **Claude may make safe timing, docking, interaction and numeric decisions when M07 is explicitly authorized; first verify M06's actual delivered interfaces.** **M08 planned:** smelter; **M10 planned:** armor/workwear. **M06 only is authorized now**; M07, M08 and M10 remain separate future milestones requiring new owner authorization. Starting M06 does **not** mark M05 owner acceptance.
- Upright vertical mining skip, loading by gravity from elevated minecart tracks, and unloading at the surface.
- Freight cargo visuals are tracked in **[M11](MILESTONE_11.md)**: type-/shape-aware stepped fill rendering, editable UV assets built by Claude and final painting by Joe in [Joe's To-Do](Joe's%20To-Do/README.md). M11 is planned, not implementation-authorized. Preserve exact quantity inspection; related dev-text work lives in UX_TEXT_LOG.md.
- **Planned M08 weight-balance adjustment (owner, 2026-10-13):** equipment (rails, lanterns, buckets, minecarts, tools) still weighs the same but is not blocked by the heavy cap; **only ingot/block/ore intake is capped** after subtracting equipment weight. Mining Boots let players haul extra restricted resource load with progressive walking slowdown **when introduced by M10**. See [MILESTONE_08.md](MILESTONE_08.md); neither milestone is newly implementation-authorized.
- Metalworking, durable anvils, differentiated armor roles and steel.
- **M05 owner clarification:** the Cobblestone Engine **does consume coal under commanded power while stalled on a grade**; this is intentional engine work, not a defect. Separately, owner will check whether a leading coupled cart can get too close to a winch terminal in-game before any redesign. Do not conflate the engine fuel rule with M06 winch's motion-dependent burn cutoff.
- Redstone dependency audit: preserve worthwhile standalone mechanisms rather than automatically deleting associated devices.

Avoid introducing gangue, mandatory waste processing, seasons, painting systems or survival-maintenance chores as planned work. Reconsider only on a new explicit owner decision.
