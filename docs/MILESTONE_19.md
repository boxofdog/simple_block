---
milestone: M19
status: PLANNED - NOT READY
---

# M19 — Villager belonging, new homes and settlement wellbeing

**Owner-directed early design, 2026-10-20. PLANNED - NOT READY; not implementation authorization.** Villagers should behave like people who belong to communities, not movable trading machines. Players can found **functional new villages** by providing genuine living conditions, rather than merely confining villagers near beds. Inspiration: **a more demanding Terraria-style NPC settlement**, implemented through Minecraft villager behavior.

This milestone addresses homesickness, settlement adoption, unhappy behavior and player-facing explanations. It **depends on** [M16](MILESTONE_16.md)'s larger navigable villages, [M17](MILESTONE_17.md)'s meaningful work/commerce and [M18](MILESTONE_18.md)'s reliable Iron Golem protection. Those foundational changes belong to their own milestones, not M19.

## 1. Original homes and homesickness

- On **birth or natural spawning**, a villager acquires a persistent **home-village identity**.
- If taken away from that village without finding a legitimate new one, they become **homesick**. Trapped or isolated villagers pace slowly, make distressed sounds and **refuse trading/services**; right-click remains available for a useful explanation instead of a trade window.
- If freedom and a plausible route permit it, a displaced villager **tries to return home**. Do not force-load chunks or attempt unbounded pathfinding across the world.
- A new home is earned through a **functional settlement**, not a special item, arbitrary trading reset or a timer alone.

## 2. Five necessary settlement conditions

**All five conditions** must be genuinely present before a villager adopts a settlement as home.

| Condition | Player-visible expectation |
|---|---|
| **Eligible homes** | Real usable and accessible housing/sleeping places. |
| **Eligible workstations** | Accessible legitimate work areas and professions as redesigned in [M17](MILESTONE_17.md); decorative or unreachable job blocks are insufficient. |
| **An Iron Golem** | A functioning local guardian provides security; see prerequisite [M18](MILESTONE_18.md). |
| **Another villager** | A real companion with opportunities to meet/socialize; a solitary captive is not settled. |
| **Freedom to path in open space** | Residents can actually traverse reasonable space between home, work and communal areas. A cramped prison with token beds, jobs and a golem must fail. |

These are observable conditions, **not** a hidden happiness score. Both naturally generated and **player-built settlements** can qualify. Exact housing/job validity, village boundaries, access/pathing checks, distances and frequency still need owner-approved design; avoid a global village-simulation engine.

## 3. Permanent adoption, temporary threats and prolonged unhappiness

- Once the five conditions hold consistently for a suitable period, the villager **adopts the present settlement as their home** and **forgets the original village**. It becomes their one home, not an additional address.
- **Belonging persists, wellbeing depends on conditions.** If one or more conditions disappear, villagers do **not** automatically lose their adopted home or run back to a forgotten original village.
- If conditions remain unsatisfied for a **prolonged period**, residents become **unhappy**: distressed behavior and refusal to trade or perform normal services. Short outages should not instantly cause chronic unhappiness or rapidly toggle state.
- **Guardian death has an immediate safety response:** When the village loses its effective Iron Golem protection, villagers retreat indoors and stop casually wandering outside. This happens before any prolonged-unhappiness timer expires. Losing one golem need not mean unprotected if another functioning guardian remains.
- Once conditions return, residents can resume their ordinary lives **in the same adopted village**, without a second resettlement ritual.

## 4. Diagnosing unhappy villagers

**Right-clicking an unhappy villager opens a simple dialogue/status box instead of the ordinary trade interface.** Show the status of **all five conditions**:

- Which conditions are currently **satisfied**.
- Which are **missing or failing**, in plain player-friendly terms.

This should let the player see why a villager is refusing trade and what needs attention. It is **not** a quest system, numeric happiness grind, hidden reputation economy or a general village-management GUI. Healthy villagers use their normal appropriate interaction. Future trading-loop changes require their own design.

## 5. Example behaviors to preserve

- A villager transported far away and locked alone in a tiny trading cell is homesick and refuses service.
- Two villagers with usable houses, workplaces, protection, genuine open movement and social contact can adopt a **new player-built village**; their original homes are then forgotten.
- A fake village that crams two villagers, beds, workstations and a golem inside a tiny enclosure **does not** satisfy freedom of movement.
- Killing the community's last guardian makes residents immediately shelter indoors; sustained insecurity eventually makes them unhappy. Restoring protection allows them to recover without changing home identity.
- An unhappy resident's right-click dialogue distinguishes **satisfied** conditions from **problems**, instead of forcing the player to guess.

## Prerequisite, limits, and unresolved decisions

**Prerequisite sequence:** [M16](MILESTONE_16.md) establishes credible housing and paths, [M17](MILESTONE_17.md) establishes work and commerce, and [M18](MILESTONE_18.md) establishes reliable Iron Golem protection **before M19** makes villager wellbeing depend on these systems. Respect the M17-approved job/trade behaviors and existing hostile-mob rules rather than silently replacing them. Run bounded checks; never force distant chunks to find homes or destinations.

**Not yet ready for implementation:** Agree on settlement identification and reassignment timing; what counts as an eligible home, job, guardian and open path; time thresholds for homesickness/unhappiness and recovery; behavior at destroyed or inaccessible original villages; multiplayer/save persistence and UI details. Real-client testing must cover escape/homecoming, genuine relocation, false-positive trading prisons, golem death, prolonged hardship, restored security, and informative dialogues.

**Lifecycle:** PLANNED - NOT READY. This records the approved *direction*, not final numerical rules, a completed feature, or permission for Claude Code to start. Separate owner approval is required to move to READY and later to IMPLEMENTING.
