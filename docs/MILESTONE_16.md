---
milestone: M16
status: PLANNED - NOT READY
---

# M16 — Better Iron Golem village guardians

**Preliminary owner direction, 2026-10-20. PLANNED - NOT READY; this is not implementation authorization.**

Iron Golems need to be reliable guardians before villager wellbeing depends on their presence in [M17](MILESTONE_17.md). Review their behavior, not merely their health or damage.

## Broad goals

- **Respond to danger:** Notice and urgently defend villagers being attacked; choose threats intelligently rather than wandering past danger.
- **Protect the settlement:** Patrol populated streets, homes and workplaces; avoid holes, pathing failures and wandering away while residents are at risk.
- **Handle real combat:** Survive and effectively respond to groups of hostile mobs without becoming an invincible boss. Consider AI/pathfinding and combat reliability before raw stat buffs.
- **Recover after loss:** A community needs a workable path to restoring golem protection. Consider villages with multiple golems, absence of the last guardian, and the requirements for player-founded villages under M17.
- **Respect existing mechanics:** Do not inadvertently rewrite M09 monster behavior, golem drops, village spawning or unrelated systems.

## Still undecided

Patrol territory, precise target priorities, stat changes, spawn/replacement mechanics, how constructed golems count as guardians, and any model/animation changes remain **open for later design**. Do not implement speculative features.

**Milestone relationship:** M16 should establish credible guardian behavior before the protection-dependent parts of [M17](MILESTONE_17.md) are implemented.

**Lifecycle:** PLANNED - NOT READY. Define a bounded, owner-approved implementation and test plan before requesting a separate START. No M16 work is claimed delivered.
