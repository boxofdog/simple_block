# M09 — Hostile mobs and defended settlements

**Status as of 2026-10-13: OWNER-SCOPED DESIGN DIRECTION ONLY; NOT IMPLEMENTATION-AUTHORIZED.** This is the milestone-specific home for the formerly inlined roadmap concept, **not** a finalized executable brief. Its behavior/rates and test matrix still require owner approval. M07 is playtesting; M08–M11 are not implementation-authorized.

## Owner design direction

Build on existing light entrancement and physical defenses without raids, invasion waves, or new mob species. **Current playtest observation:** monsters trickle toward illuminated areas at a pleasing, natural pace, but lose interest too quickly. Preserve the gradual approach while making their interest in nearby lit areas more persistent; tune and test disengagement/loitering rather than accelerating or mass-aggroing every mob. Monsters should opportunistically exploit newly reachable illuminated areas when a wall, gate, or door opens or is breached: use their existing memory of lit destinations, reevaluate paths after access changes, and temporarily favor reachable interior light before resuming normal loitering or player pursuit. Do not require monsters to identify abstract 'bases' or give them omniscient knowledge of entrances. Explore occasional skeletons seeking spiders to ride (without making jockeys routine), including a readable skeleton-assisted overhang maneuver while mounted. Explore a substantial iron gate using existing winch mechanics: two three-block-wide sections form a full gate, and a creeper damaged by a sheltered player may retaliate by targeting the gate under carefully constrained conditions. Balance rarity, defense readability, pathfinding performance, non-griefing frustration, and tests in a later dedicated brief; no precise rules or models authorized yet.

**No implementation authorization for M09.**

## Before authorization

Settle mob interest/loiter timings, entrance breach/pathing boundaries, jockey rarity and readable climbing maneuver, iron-gate dimensions/controls, creeper retaliation constraints, negative controls, actual-client/dedicated-server evidence, and performance limits. No raids, invasion-wave subsystem or new monster type by assumption.
