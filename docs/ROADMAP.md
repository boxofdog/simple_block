# Simple Block — Roadmap

**Purpose:** A short milestone status and evidence index—not a second specification or bug log. Each milestone contains its requirements and dated verification appendices; older reports are [archived](archive/milestone-reports/). See the [project vision](VISION.md), [bug log](BUG_LOG.md) and [backlog](BACKLOG.md) for their respective subjects.

**Status column is generated:** The `milestone`/`status` YAML header of each `MILESTONE_XX.md` is authoritative. [RULES.md](RULES.md) defines the five labels; [status synchronization](../tools/docs/sync_milestone_statuses.py) changes only the status cells in the table below. **Do not edit the status cells manually.** Notes and pending decisions below remain editorial and do not change automatically.

## Milestone status

| ID | Deliverable | Status | Spec / handoff | Results |
|---|---|---|---|---|
| M01 | Remove sleep skipping, phantoms, elytra and shulker bypasses | **SHIPPED** | [Brief](MILESTONE_01.md) | [Verification](MILESTONE_01.md#implementation-and-verification--2026-10-11) |
| M02 | Geology, heavy inventory, hostile light, hunger and inventory QoL | **SHIPPED** | [Handoff](MILESTONE_02.md) | [Verification](MILESTONE_02.md#implementation-and-verification--2026-10-11) |
| M03 | 256-item freight minecarts, cushions, storage restrictions | **SHIPPED** | [Handoff](MILESTONE_03.md) | [Verification](MILESTONE_03.md#implementation-and-verification--2026-10-11) |
| M04 | Cable drums, anchors, routes, cable grips | **SHIPPED** | [Brief](MILESTONE_04.md) | [Verification](MILESTONE_04.md#implementation-and-verification--2026-10-12) · [CI #16](https://github.com/boxofdog/simple_block/actions/runs/38197364425) |
| M05 | Reversible cable loops, manual cranks, Cobblestone Engine, coupling | **SHIPPED** | [Brief](MILESTONE_05.md) | [Verification](MILESTONE_05.md#implementation-and-verification--2026-10-12) · [CI #17](https://github.com/boxofdog/simple_block/actions/runs/38306694723) |
| M06 | Loading Rail, powered winch, unlimited supported cable, related corrections | **SHIPPED** | [Brief](MILESTONE_06.md) | [Verification](MILESTONE_06.md#implementation-and-verification--2026-10-13) · [CI #20](https://github.com/boxofdog/simple_block/actions/runs/38382953782) |
| M07 | Vertical ore skip, conditional bridle/pulley, four-gate minecart, visual and station fixes | **PLAYTESTING** | [Unified M07 handoff](MILESTONE_07.md) | [Latest verification](MILESTONE_07.md#implementation-and-verification--2026-10-14-1) · [CI #42](https://github.com/boxofdog/simple_block/actions/runs/38526791742) |
| M08 | Bulk Blast Furnace + commodity-only carrying cap | **PLAYTESTING** | [M08 package](MILESTONE_08.md) | [Latest verification](MILESTONE_08.md#implementation-and-verification--2026-10-14) · [CI #39](https://github.com/boxofdog/simple_block/actions/runs/38522399370) |
| M09 | Hostile loitering, opportunistic doors, creeper retaliation, rare jockeys | **PLAYTESTING** | [M09 package](MILESTONE_09.md) | [Verification](MILESTONE_09.md#implementation-and-verification--2026-10-14) · [CI #37](https://github.com/boxofdog/simple_block/actions/runs/38501374609) |
| M10 | Headlamp/Overalls/boots, workwear bonuses, Spelunker's Rope, armor retirement | **PLAYTESTING** | [M10 package](MILESTONE_10.md) | [Verification](MILESTONE_10.md#implementation-and-verification--2026-10-14) · [CI #46](https://github.com/boxofdog/simple_block/actions/runs/38535509454) |
| M11 | Material-/shape-specific cart fills, progressive hopper transfer, cart text removal | **PLAYTESTING** | [M11 package](MILESTONE_11.md) | [Verification](MILESTONE_11.md#implementation-and-verification--2026-10-14) · [CI #52](https://github.com/boxofdog/simple_block/actions/runs/38597022406) |
| M12 | Permanent four-monster effigies, nine-full-stack compression and directional fear | **PLAYTESTING** | [M12 handoff](MILESTONE_12.md) | [Verification](MILESTONE_12.md#implementation-and-verification--2026-10-14) · [CI Build #57](https://github.com/boxofdog/simple_block/actions/runs/38637606159); owner playtest pending |
| M13 | Integration fixes, rope bow launch, bed recovery, chest sorting and inventory QoL | **IMPLEMENTING** | [M13 patch package](MILESTONE_13.md) | Handoff ready; awaiting explicit implementation authorization; no implementation or verification |
| M14 | Old Coot: abandoned-mineshaft miner and spider guardian | **PLANNED - NOT READY** | [M14 brief](MILESTONE_14.md) | Not implemented; owner authorization required |
| M15 | Cobblestone Engine rear three-position Through/Load/Unload lever and conditional Loading Rail service | **PLANNED - NOT READY** | [M15 brief](MILESTONE_15.md) | Planning only; no implementation or verification |

**Unnumbered work:** ordinary 3D rails have been implemented (owner visual review pending). Revised villages, extreme grades, discovered enchanting and iron/diamond/netherite armor bonuses remain future [backlog](BACKLOG.md) ideas, **not implementation authorizations**.

## Consolidated playtesting and feature refinement

[M13](MILESTONE_13.md) is the **single worklist** for M07–M11 owner playtesting amendments, cross-milestone bug fixes and broad M01–M12 regression, including Spelunker's Rope bow launching (offhand rope overrides arrows), night-only hunger-funded bed healing (with vanilla nearby-monster refusal), Shift + right-click drag transfers, a one-click Sort button for chests, and [BUG-022](BUG_LOG.md) Ore Skip Z-fighting triage. [BUG_LOG.md](BUG_LOG.md) retains canonical defect records. Existing milestone specifications, dated evidence and status headers remain intact. M13 is **PLANNED - READY** following 2026-10-19 design decisions and a staged, bounded implementation handoff; this **does not authorize coding**. Its existing single-cart Loading Rail fixes are distinct from [M15](MILESTONE_15.md)'s proposed three-state Cobblestone Engine and advanced multi-wagon freight stations.

See each milestone for requirements, verification and acceptance status. Documentation conventions: [RULES.md](RULES.md).
