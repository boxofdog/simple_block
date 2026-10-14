---
milestone: M03
status: SHIPPED
---

# M03 — Vanilla minecart cargo and storage-bypass closure (historical handoff)

**Lifecycle: SHIPPED.** The original M03 draft was superseded by the owner's decision to modify **vanilla minecarts**, not add a separate freight-cart entity. This concise decision baseline replaces the earlier central decision-ledger dependency; implementation and deviations are in [MILESTONE_03_REPORT.md](archive/milestone-reports/MILESTONE_03_REPORT.md).

## Owner decisions (M03, with later amendments labeled)



| Area | Current decision |
|---|---|
| Freight vehicle | **The existing vanilla minecart is the freight cart.** No separate freight minecart item/entity or recipe. This supersedes the original M03 brief, retained only in Git history. |
| Capacity | Holds **256 of exactly one eligible bulk item type**, a playtestable starting value. |
| Bulk cargo expansion — delivered later | M03 established cobblestone and cobbled deepslate freight. [M11](MILESTONE_11.md) subsequently added **gravel, sand**, ingots, metal/storage blocks and other cargo types. Gravel and sand remain outside the personal commodity weight limit. The precise present eligibility belongs to M11 and the cargo tag, not this original M03 baseline. |
| Passenger use | Install a **vanilla 26.3 cushion** into an empty cart to enable riding and mob pickup; a cart without a cushion cannot take riders. Cushion and bulk cargo are mutually exclusive. |
| Cargo display, hoppers and UX — delivered later | [M07](MILESTONE_07.md) supplied the shared four-gate cart; [M11](MILESTONE_11.md) supplied material-specific, four-height visible loads, gradual hand and hopper transfers, and removal of all mod-authored floating/action-bar text. This is **PLAYTESTING**, not an unfinished M03 task. See [UX decisions](UX_TEXT_LOG.md) and [painting queue](Joe's%20To-Do/README.md). |
| Storage bypasses | Ender chests and ordinary chest minecarts cannot serve as portable storage; donkeys, mules and llamas retain their cargo role. |
| Other variants | Furnace, hopper, TNT and other specialized cart variants were left unchanged by M03; future Cobblestone Engine work is a separate decision. |

## As-built interaction baseline (M03; later milestones override)

- Right-click with eligible cargo to load; empty-hand use inspects; **sneak + use** unloads one stack without bypassing inventory limits. `#simple_block:minecart_cargo` defines eligible material types.
- Cushions fit only empty carts, are visible, and return on removal or cart destruction. A cushioned cart carries passengers instead of bulk cargo.
- A broken cargo cart spills its contents into the world rather than storing them inside the dropped minecart item. Minecart storage survives world save/load.
- Mineshaft loot chest minecarts remain sealed: they cannot open or feed hoppers, and breaking them spills loot plus a minecart and chest. Ender chests drop obsidian rather than becoming usable containers.
- M03 originally used on-look cargo and action-bar status text. [M11](MILESTONE_11.md) removed this; [UX_TEXT_LOG.md](UX_TEXT_LOG.md) inventories wording and silent refusals needing owner-selected feedback.
- Full **original M03** eligible-item list, edge cases and test evidence: [archived report](archive/milestone-reports/MILESTONE_03_REPORT.md). Its former "gravel refused" tests document a previous rule, superseded by M11.

**Current behavior:** Use the [M07](MILESTONE_07.md) and [M11](MILESTONE_11.md) appendices for later gate, cargo, hopper and presentation behavior. Keep M03's original report as historical evidence, not a competing current specification.

## Implementation and verification — 2026-10-11

**Metadata:** M03 | SHIPPED | Vanilla minecarts carry 256 of one bulk item without riders; cushions make passenger carts; ender chests and chest minecarts removed.

**Implementation**
- Commits: `69ed2168e8aab1017190fd5a7df3a1827fe8b696` minecart cargo, ender chest and chest minecart removal; `6373fbae55e6b192c04ef8e91c330c668ad87885` straw-bed and light test fixes; `a36d9eb318940d1826bcfd7c7b4901055011d533` cushions
- Delivered: one-commodity 256-item cargo with load, unload, save and spill; no riding without a cushion; cushions fit and remove; ender chests and chest minecarts unobtainable and unusable.
- Deviations: Owner chose to overhaul the vanilla minecart instead of a separate freight cart.

**CI**
- Build: #14 | Run ID: 38167949507 | PASS
- Source SHA: `69ed2168e8aab1017190fd5a7df3a1827fe8b696`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38167949507
- Artifact: `simple-block-mod` id 11712444451, available (verified 2026-10-13). Cushions: Build #15 (run 38170211925, `a36d9eb`) PASS.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 80/80 | 85/85 after cushions; [archived report](archive/milestone-reports/MILESTONE_03_REPORT.md); [evidence](evidence/milestone-03/gametest-evidence.md) |
| Client | PASS | 1/1 | `SurvivalSmokeClientTest` cargo, save/reopen and cushion steps; [screenshots](evidence/milestone-03/screenshots/) |
| Dedicated server | PASS | 1/1 | `runServer` start and stop only; no remote client |
| Negative controls | PASS | 3/3 | [negative controls](evidence/milestone-03/negative-controls.md) |

**Known gaps**
- GAP-01: MINOR — two real clients using one cart at once (T14) NOT RUN; next: check in a multiplayer playtest.

**Owner playtest**
- NONE
