# Simple Block — Backlog

**The single list of unnumbered future work.** This combines the previous `BACKLOG.md` and `FUTURE_WORK.md`. Concepts here are **not implementation authorizations**. [ROADMAP.md](ROADMAP.md) owns priority and milestones, [RULES.md](RULES.md) owns workflow, and [MILESTONE_XX.md](MILESTONE_08.md) owns each numbered milestone's specification. Do not duplicate an entire milestone handoff here or treat an idea's inclusion as a go-ahead to code.

## Future-work index

| ID | Future work / idea | Standing |
|---|---|---|
| I-001 | Smithy tools made from forged parts at an anvil, assembled at crafting table | Concept; avoid blocking early iron tools behind a 31-ingot anvil |
| I-002 | Durable, effectively permanent anvils | Concept; tune cost and connect with I-001 |
| I-003 | Broader block/inventory-content cleanup | Parked; identify exact additions to the removed families |
| I-004 | Copper Headlamp, dual-slot Overalls, Spiked/Mining Boots, full workwear bonuses | **Delivered in [M10](MILESTONE_10.md)**; see the milestone for recipes and behavior |
| I-005 | Rope and hanging Spelunker's Rope | **Delivered in [M10](MILESTONE_10.md)**; see the milestone |
| I-006 | Faster normal ladders | Unnumbered balance question; consider I-005/M10 interactions |
| I-007 | Alluvial iron sand and iron gravel at riverbanks | Owner-directed concept; finite deposits, extraction, textures, iron scarcity |
| I-008 | Steel with a purposeful manufacturing chain | Owner-directed concept; decide role, production and smithy/anvil relationship |
| I-009 | In-world recipe/discovery guidebook | Owner-directed; avoid automatic new crafting/progression gates |
| I-010 | Named full-set effects for vanilla iron/diamond/netherite armor | **Owner-settled POST-M10, unnumbered**; detailed below |
| I-011 | Persistent hostile attraction, entrance behavior, rare spider jockeys and provoked creepers | **Delivered in [M09](MILESTONE_09.md)**; pending owner acceptance |
| I-012 | Witch's Tome and exploration-gated enchanting | Owner-directed POST-M10; detailed below |
| I-013 | Three-dimensional ordinary rails matching Loading Rail | **Implemented** as an owner-authorized visual task; details below |
| I-014 | Extreme grade rails, runaways, collision/cart damage | Design geometry, thresholds, safe cargo drops |
| I-015 | Train coupling/operation, sequential coupled-wagon unloading | Related to open **M07 playtest decision**; no silent M06 revision |
| I-016 | Full mine-to-smelter logistics playtest | Future integrated owner playtest, not a new mod system |
| I-017 | Selective redstone redesign while retaining useful devices | No blanket removal of worthwhile standalone mechanisms |
| I-018 | Village overhaul ideas (assigned to milestones) | Moved to [M16](MILESTONE_16.md) village design, [M17](MILESTONE_17.md) commerce/jobs, [M18](MILESTONE_18.md) golems and [M19](MILESTONE_19.md) wellbeing |
| I-019 | Large winch-operated iron defense gate | **Deferred from M09 by owner**; unnumbered future design, not authorized |

## I-019 — Deferred industrial iron gate

Owner deferred the large iron gate from M09 on 2026-10-13. Earlier concept: a substantial winch-operated gate with **two sections, each three blocks wide**, building on existing cable/winch mechanisms. No recipe, final dimensions, operation, durability or relationship with hostile behavior is settled. Do not implement or reconnect it to creeper retaliation. This is not M04–M07's moving hoisting cable.

## I-010 — Post-M10 combat armor identities

This is a **separate, unnumbered post-M10 workstream**, not additional M10 scope. Keep the original vanilla **iron, diamond and netherite armor items**; each of the following activates **only with all four pieces of its matching set**. M10's delivered workwear bonuses **Trail Blazer** and **Old Pro** remain separate; gold/chainmail retirement was completed in M10.

| Complete vanilla armor set | Named bonus | Owner-settled behavior |
|---|---|---|
| **Iron** | **Cast Iron Crusader** | No knockback from zombie attacks or arrows; after hitting a mob, show its **remaining hitpoints**. |
| **Diamond** | **Iridescent Infantry** | Reflect incoming arrows back with **reduced travel distance but full damage/strength**; a zombie biting the wearer **breaks its teeth**, damaging it and **reducing its subsequent attack damage**. |
| **Netherite** | **Netherfire Knight** | **Piglins never fight back** against the wearer and offer **more favorable trade/barter outcomes**; any hostile mob that hits the wearer **ignites**. |

**Implementation timing:** settle numerical reductions, durations, reflected-arrow return/range, teeth-broken state behavior, UI feedback, piglin barter odds and exact triggering on a future implementation brief. Do **not** improvise extra systems, equipment tiers or automation. The settled design details above remain here until a dedicated post-M10 milestone is created. **Consult [ROADMAP.md](ROADMAP.md) for current status.**

## I-012 — Witch's Tome and discovered enchanting

Place witch huts on remote mountain peaks and in deep deepslate caverns. Each features an enchanting table; breaking the table returns a diamond plus a **Witch's Tome**. A Witch's Tome is required to craft a replacement enchanting table, making enchanting knowledge something discovered through exploration rather than unlocked from ordinary mined materials alone. Final table recipe, hut generation density, repeat acquisition and loot/drop safeguards remain to be designed. Keep this beyond M10, alongside the separate post-M10 vanilla combat armor concepts. No implementation authorization implied. This item is tracked as I-012 here.

## I-013 — 3D ordinary rails

**Implemented 2026-10-14 after explicit owner instruction; visual change only.** `minecraft:rail` uses 3D straight, curved and ascending models matching the Loading Rail's gauge, sleeper profile and shared `loading_track.png` texture. No vanilla rail block behavior, recipe, physics, placement, collision or drops were replaced. Editable sources: `art/blockbench/rail_*.bbmodel`; generator: `tools/models/generate_rail_models.py`. [Client screenshots](evidence/rails-3d/screenshots/), [commit](https://github.com/boxofdog/simple_block/commit/e49ba32616d0847047a02dbafb13e5f143b551bb), [Build #53](https://github.com/boxofdog/simple_block/actions/runs/38601099542) (215/215 server tests). Owner visual acceptance pending.

## I-014–I-016 — Later railway operation and testing

- **Extreme grades:** Investigate rails as steep as three-block rises/drops, dangerous downhill runaways, crash damage and item-conserving drops. These are not claims about existing vanilla rails.
- **Whole-train operation:** Investigate loops, coupling slack and grade transitions. [M11](MILESTONE_11.md) implemented side-gate, cart-by-cart hopper unloading; owner playtesting and the broader M07 station decision remain open. Do not change SHIPPED M06 behavior without authorization.
- **Integrated playtest:** Validate the now-implemented mine → hoist/skip → freight wagon → hopper/furnace route end-to-end. No extra autonomous system is implied.

## Future design backlog — inhabited villages

**This former unnumbered village workstream is now divided into four preliminary milestones, in dependency order:** [M16 — villages and architecture](MILESTONE_16.md), [M17 — commerce, jobs and daily routines](MILESTONE_17.md), [M18 — Iron Golem protection](MILESTONE_18.md), and [M19 — villager belonging and wellbeing](MILESTONE_19.md).

The original village ideas (terrain suitability, larger enterable houses/workshops, streets, population, pub, centralized general store, workplace services, responsive golems and settlement conditions) are now tracked in those briefs. **All four remain PLANNED - NOT READY; none authorizes implementation.** Their milestone specifications, not this historical backlog pointer, own further design.

## Explicitly rejected BTA-inspired directions

- **Seasons:** Not planned for Simple Block.
- **Painting / cosmetic coloring system:** Not planned for Simple Block.

These rejections do not prohibit ordinary visual polish or other environmental mechanics, but they should not re-enter the roadmap as features without a new owner decision.

## Guardrails for unnumbered ideas

- Iron sand/gravel, steel, durable anvils, smithing components and the recipe guide are **future possibilities**, not automatically included in M08 metallurgy.
- Do not add gangue, mandatory waste processing, seasons, a decorative painting/coloring progression system or routine maintenance chores without explicit owner direction.
- Full implementations of M08 metallurgy and weight rules, M09 settlement defense, M10 workwear and M11 fill-state/hopper functionality live in their milestone handoffs. The historical M06/M07 execution directions formerly found in BACKLOG were superseded by their SHIPPED/PLAYTESTING milestone records.
