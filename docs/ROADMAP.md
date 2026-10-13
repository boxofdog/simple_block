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
| M08 | Pre-industrial bulk blast furnace + commodity-only carrying cap | **PLAYTESTING** | [M08 package](MILESTONE_08.md) | Not implemented |
| M09 | Hostile behavior, persistent light interest and defensive infrastructure | **PLANNED - NOT READY** | [M09 package](MILESTONE_09.md) | Not implemented |
| M10 | Modular headlamp/overalls/boots, workwear bonuses, armor retirement | **PLANNED - NOT READY** | [M10 package](MILESTONE_10.md) | Not implemented |
| M11 | Material-/shape-specific cart fills, progressive hopper transfer, cart text removal | **PLANNED - READY** | [M11 package](MILESTONE_11.md) | Not implemented |

**Post-M10 and unnumbered plans:** ordinary 3D rails, Spelunker's Rope, revised villages, extreme-grade railway behavior, the Witch's Tome/discovered enchanting, and separate iron/diamond/netherite armor set bonuses live in [BACKLOG.md](BACKLOG.md). They are not an authorization or an extension of M08–M11.

## Current handoff and next decisions

**M07 — owner playtesting.** The source patch is [`49d7add`](https://github.com/boxofdog/simple_block/commit/49d7add4a3793b1ac4bb61e1f7f54156eb4d5cbc), report update [`dbda956`](https://github.com/boxofdog/simple_block/commit/dbda9560b5ccb9901eead06dc02c77f03837c936). **Build #23 passed 152/152 required server game tests** and published `simple-block-mod`; committed evidence reports 9/9 client tests and 17/17 negative controls. ChatGPT has reviewed source/CI/evidence, **not rerun those client tests**.

- **Decision to make:** a side-on coupled train can empty its currently docked wagon, but M06's intentional station release rule lets the following wagons pass without docking at the same Loading Rail. Decide how a whole train should advance and re-dock one wagon at a time. Do not change already-shipped M06 semantics without the owner's direction.
- **Still untested:** terminal station approach on curves/slopes, with coupled trains or partial chunk unloading; opening the generated cart model in Blockbench; further human/desktop-GPU and multiuser checks. The near-drum `idle` versus `docked` stop reason is a minor diagnostic to inspect.
- **Acceptance:** M07's owner checkbox stays **unchecked** until the owner finishes playtesting and expressly accepts it. Details and negative controls: [M07 verification](MILESTONE_07.md#implementation-and-verification--2026-10-13); history in the [archived M07 report](archive/milestone-reports/MILESTONE_07_REPORT.md).

**M08 — owner playtesting.** Implemented after the owner's 2026-10-13 authorization: the commodity-only carrying cap, the pre-industrial blast furnace (redesigned to the owner's iron-fronted brick furnace with a back-middle hopper, an outside channel and a whole-block mold), cart and skip intake, the Block of Charcoal, slower raw-ore smelting and the retired vanilla blast furnace. Results, gaps and the playtest list: [M08 verification](MILESTONE_08.md#implementation-and-verification--2026-10-13). Not owner-accepted; villager professions remain the [village backlog](BACKLOG.md).

**M09–M11:** packages are indexed above. M09–M10 still require design work; M11's implementation handoff is ready, but **none has been authorized to start**. Mining Boots/Mule/Old Pro remain **M10**, not M08; the heavy-freight fill rendering and hopper mechanics remain **M11**, not M07.

Documentation and handoff conventions (including the five canonical statuses): [RULES.md](RULES.md).

**Provenance:** [Retired decisions ledger](archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md) and [archived pre-consolidation roadmap (2026-10-13)](archive/ROADMAP_PRE_CONSOLIDATION_2026-10-13.md) preserves every original line, including older and now-superseded handoff instructions and acceptance snapshots. It is **not** authoritative for current status or implementation.
