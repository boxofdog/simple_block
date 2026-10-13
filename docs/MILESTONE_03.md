# M03 — Vanilla minecart cargo and storage-bypass closure (historical handoff)

**Lifecycle: SHIPPED.** The original M03 draft was superseded by the owner's decision to modify **vanilla minecarts**, not add a separate freight-cart entity. This concise decision baseline replaces the earlier central decision-ledger dependency; implementation and deviations are in [MILESTONE_03_REPORT.md](archive/milestone-reports/MILESTONE_03_REPORT.md).

## Owner decisions (M03, with later amendments labeled)



| Area | Current decision |
|---|---|
| Freight vehicle | **The existing vanilla minecart is the freight cart.** No separate freight minecart item/entity or recipe. This supersedes the original M03 brief, retained only in Git history. |
| Capacity | Holds **256 of exactly one eligible bulk item type**, a playtestable starting value. |
| Bulk cargo expansion (approved, pending implementation) | **Cobbled stone materials, gravel and sand are freightable** in ordinary minecarts while remaining weightless for personal heavy-load calculations. Existing `cobblestone` and `cobbled_deepslate` already qualify; add `gravel` and `sand` to the eligible cargo tag **after the owner has playtested M05**. Other possible cobbled/sand variants are not silently assumed. |
| Passenger use | Install a **vanilla 26.3 cushion** into an empty cart to enable riding and mob pickup; a cart without a cushion cannot take riders. Cushion and bulk cargo are mutually exclusive. |
| Cargo appearance, hopper transfer and UX (future M11, planned) | The **same freight minecart item/entity** in the four-gate body shows its real cargo material/form and graduated 0–256 fullness: ingots, loose diamonds, storage blocks, mined ore and bulk rubble. **Full vanilla hopper support:** hopper directly above feeds an open-top **docked cart on a Loading Rail**; cart empties into a receiving hopper through a real **end or long-side gate**, including coupled wagon side unloading. Gradual, item-conserving, hopper-rate-aware transfers **change actual inventory and visible fill tiers together**, including progressive hand loading rather than instant bulk click deposit; respect one-commodity/256 capacity, powered/locked hopper, docking and physical obstruction. **No cart-produced look-at cargo text or click action-bar/status/flavor text**; player-assigned custom names and other systems' feedback survive. This explicitly supersedes earlier preserved on-click exact-number display. [M11](MILESTONE_11.md) defines Claude's code/assets/tests; Joe paints via [Joe's To-Do](Joe's%20To-Do/README.md). Planned, not yet authorized for implementation. |
| Storage bypasses | Ender chests and ordinary chest minecarts cannot serve as portable storage; donkeys, mules and llamas retain their cargo role. |
| Other variants | Furnace, hopper, TNT and other specialized cart variants were left unchanged by M03; future Cobblestone Engine work is a separate decision. |

## As-built interaction baseline (M03; later milestones override)

- Right-click with eligible cargo to load; empty-hand use inspects; **sneak + use** unloads one stack without bypassing inventory limits. `#simple_block:minecart_cargo` defines eligible material types.
- Cushions fit only empty carts, are visible, and return on removal or cart destruction. A cushioned cart carries passengers instead of bulk cargo.
- A broken cargo cart spills its contents into the world rather than storing them inside the dropped minecart item. Minecart storage survives world save/load.
- Mineshaft loot chest minecarts remain sealed: they cannot open or feed hoppers, and breaking them spills loot plus a minecart and chest. Ender chests drop obsidian rather than becoming usable containers.
- M03 introduced an on-look cargo readout and action-bar feedback. The owner has now decided that **visible stepped cargo-fill geometry replaces the intrusive floating on-look count**, while deliberate inspection can still provide exact material/count. This is a future presentation update, tracked in [UX_TEXT_LOG.md](UX_TEXT_LOG.md); other useful information must not simply disappear.
- Full **as-shipped** eligible-item list, edge cases and test evidence: [MILESTONE_03_REPORT.md](archive/milestone-reports/MILESTONE_03_REPORT.md). **Pending follow-up:** the new gravel/sand eligibility decision above is not yet implemented; the M03 report's former "gravel refused" tests describe the old behavior. Update the item tag and relevant tests only in an authorized implementation pass.

**Important later override:** The M03-era note suggesting deliberate click/hover exact-number inspection is **superseded** by the owner's [M11 handoff](MILESTONE_11.md), which calls for removing **both** hover-generated labels and click-originated freight-cart flavor/status text, alongside progressively rendered hopper transfers. M07 already implemented the universal four-gate cart; M11 is **PLANNED - READY**, not started. For exact current implementation, use [the M03 report](archive/milestone-reports/MILESTONE_03_REPORT.md), [M07 report](archive/milestone-reports/MILESTONE_07_REPORT.md) and later authorized implementation notes.

**Freight eligibility:** A historical owner-approved gravel/sand expansion was marked pending after M05 playtesting; verify today's actual cargo tags before implementing any later eligibility changes. Do not assert it shipped solely because it was decided.

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
