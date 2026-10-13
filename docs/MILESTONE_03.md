# M03 — Vanilla minecart cargo and storage-bypass closure (historical handoff)

**Lifecycle: SHIPPED.** The original M03 draft was superseded by the owner's decision to modify **vanilla minecarts**, not add a separate freight-cart entity. This concise decision baseline replaces the earlier central decision-ledger dependency; implementation and deviations are in [MILESTONE_03_REPORT.md](MILESTONE_03_REPORT.md).

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
- Full **as-shipped** eligible-item list, edge cases and test evidence: [MILESTONE_03_REPORT.md](MILESTONE_03_REPORT.md). **Pending follow-up:** the new gravel/sand eligibility decision above is not yet implemented; the M03 report's former "gravel refused" tests describe the old behavior. Update the item tag and relevant tests only in an authorized implementation pass.

**Important later override:** The M03-era note suggesting deliberate click/hover exact-number inspection is **superseded** by the owner's [M11 handoff](MILESTONE_11.md), which calls for removing **both** hover-generated labels and click-originated freight-cart flavor/status text, alongside progressively rendered hopper transfers. M07 already implemented the universal four-gate cart; M11 is **PLANNED - READY**, not started. For exact current implementation, use [the M03 report](MILESTONE_03_REPORT.md), [M07 report](MILESTONE_07_REPORT.md) and later authorized implementation notes.

**Freight eligibility:** A historical owner-approved gravel/sand expansion was marked pending after M05 playtesting; verify today's actual cargo tags before implementing any later eligibility changes. Do not assert it shipped solely because it was decided.
