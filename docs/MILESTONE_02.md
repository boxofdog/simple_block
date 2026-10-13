# M02 — Geology, survival pressure and personal inventory (historical handoff)

**Lifecycle: SHIPPED.** This restores a **compact owner decision and scope reference** because the original M02 handoff was removed after delivery. It does not reopen completed work or supersede code as shipped. The full implementation, measurements, final tuning and test evidence are in [MILESTONE_02_REPORT.md](MILESTONE_02_REPORT.md). Old [DECISIONS.md](archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md) is archive-only.

## Scope and owner decisions



| Area | Current decision |
|---|---|
| Geology | Remove naturally occurring granite, diorite, andesite and tuff families where specified; retain stone/deepslate transitions and preserve useful world generation. |
| Mining | Naturally generated plain deepslate and deepslate ores require iron-tier-or-better pickaxes; crafted/decorative deepslate variants keep their own vanilla requirements. |
| Ore distribution | Some iron remains widely accessible; large rich deposits reward geography and exploration. Diamonds are substantially rarer. Exact generation settings and measurements are recorded in the M02 report. |
| Carrying capacity — M02 as built, M08 revision planned | Player inventory has **27 usable slots**. M02's **64 ingot-equivalent** heavy-load system counts applicable metal/diamond/netherite materials, including weighted blocks, ores and equipment; it currently rejects additional weight indiscriminately. **NEW owner M08 direction (not yet implemented):** retain all equipment weight but only **restrict intake of ingots, resource/storage blocks and ore** at the cap; equipment (rails, lanterns, buckets, minecarts, tools) is always collectible when an ordinary inventory slot permits. Detailed classification, edge cases and tests in [MILESTONE_08.md](MILESTONE_08.md). |
| Diamond / netherite weight | A diamond counts like one ingot; a diamond block counts as nine. Netherite similarly counts by the diamond-equivalent weight of its gear rather than charging the gold/ingots used in fabrication. |
| Weightless bulk materials | Cobbled stone materials, gravel and sand **do not contribute to the player's heavy-load allowance**. They still occupy ordinary inventory slots. The heavy-load mechanic is for designated metals and other heavy resources, not every material that can be shipped by cart. |
| Weight UI | A material-colored, text-free bar communicates weight in the inventory; hover gives total out of 64 and a per-material breakdown. |
| Mob attraction | Hostiles are drawn toward nearby **lit ground**, not individual torches or the brightest source. The system applies at Overworld night and underground where sky light is absent, including underground daytime; not the Nether/End. Pursuing a player takes precedence. |
| Lighting and survival | Standard torches emit light level **12**; lanterns **15**. Zombies can threaten wooden doors while pursuing; creepers disappear in direct sunlight. Routine hunger exhaustion is reduced, and saturation is visibly shown. |
| Inventory interaction | Inventory sorting and hotbar-first shift-click behavior preserve the constrained-carrying design. |

## M03-era refinements to the M02 mechanics

- Inventory sort order is **top row first**, superseding the original bottom-row-first proposal.
- The Sort control is an icon in an **8-pixel-high** frame aligned with the weight bar.
- Straw beds, like other beds, cannot skip the night; this gained an explicit regression test.

For exact ore distributions, AI tests, hunger tuning, weight mappings, and performance evidence, use [MILESTONE_02_REPORT.md](MILESTONE_02_REPORT.md), not outdated working-phase handoffs.

**Later requirement overrides:** M08's [revised heavy-load admission design](MILESTONE_08.md) is **PLANNED - NOT READY**, and M10's Mining Boots/Mule carrying benefit is likewise **not implemented**. The M02 report describes the currently shipped blanket weight gate; never claim the future M08/M10 changes are already present.

**Implementation and tests:** [MILESTONE_02_REPORT.md](MILESTONE_02_REPORT.md) and [M02 evidence](evidence/milestone-02/).
