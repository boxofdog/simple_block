# Simple Block — Roadmap

**Purpose:** A compact **milestone status and priority index**. This is **not** a specification, implementation handoff, historical conversation log, or bug tracker. For functional requirements, open the milestone brief; for M01–M07 code/CI evidence, open the **Implementation and verification** section at the end of each milestone document (detailed historical reports are archived in [archive/milestone-reports/](archive/milestone-reports/)). For future work, the implementation/verification summary goes at the END of its milestone document; settled requirements live there too. See the [project vision](VISION.md).

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
| M07 | Vertical ore skip, conditional bridle/pulley, four-gate minecart, visual and station fixes | **PLAYTESTING** | [Unified M07 handoff](MILESTONE_07.md) | [Verification](MILESTONE_07.md#implementation-and-verification--2026-10-13) · [CI #24](https://github.com/boxofdog/simple_block/actions/runs/38443850502) |
| M08 | Bulk Blast Furnace + commodity-only carrying cap | **PLAYTESTING** | [M08 package](MILESTONE_08.md) | [Verification](MILESTONE_08.md#implementation-and-verification--2026-10-13) · [CI #36](https://github.com/boxofdog/simple_block/actions/runs/38471351760) |
| M09 | Hostile loitering, opportunistic doors, creeper retaliation, rare jockeys | **PLAYTESTING** | [M09 package](MILESTONE_09.md) | [Verification](MILESTONE_09.md#implementation-and-verification--2026-10-14) · [CI #37](https://github.com/boxofdog/simple_block/actions/runs/38501374609) |
| M10 | Headlamp/Overalls/boots, workwear bonuses, Spelunker's Rope, armor retirement | **PLAYTESTING** | [M10 package](MILESTONE_10.md) | Implementation authorized; verification pending |
| M11 | Material-/shape-specific cart fills, progressive hopper transfer, cart text removal | **PLANNED - READY** | [M11 package](MILESTONE_11.md) | Not implemented |

**Post-M10 and unnumbered plans:** ordinary 3D rails, revised villages, extreme-grade railway behavior, the Witch's Tome/discovered enchanting, and separate iron/diamond/netherite armor set bonuses live in [BACKLOG.md](BACKLOG.md). They are not an authorization or an extension of M08–M11.

## Playtesting Changes

- **M07:** [Playtesting Changes](MILESTONE_07.md#playtesting-changes) — freight operating refinements; [open bugs](BUG_LOG.md).
- **M08:** [Playtesting Changes](MILESTONE_08.md#playtesting-changes) — furnace naming and casting-animation polish.
- **M09:** [Playtesting Changes](MILESTONE_09.md#playtesting-changes) — owner feedback on hostile behavior; rare jockeys still unobserved.

See each milestone for requirements, verification and acceptance status. Documentation conventions: [RULES.md](RULES.md).
