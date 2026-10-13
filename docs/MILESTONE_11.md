# M11 — Type-aware minecart cargo fill states

**Owner-scoped 2026-10-13. Status: PLANNED; NOT YET IMPLEMENTATION-AUTHORIZED.** A standalone renderer-and-art-template milestone, separate from the active M07 corrections and planned M08–M10. Number is for tracking and **does not require** completing M08–M10 first. Claude starts only upon specific owner authorization.

**Responsibilities:** Claude codes real cargo-type-aware rendering, fill transitions, reusable shapes/models, linked UV and placeholder PNG templates, synchronization and tests. Joe paints final textures later, exclusively tracked in [Joe's To-Do](Joe's%20To-Do/BLOCKBENCH_PAINTING.md). Passing CI does not mean finished paintwork.

## 1. Exact visual inventory: material FORM matters

| Different visible SHAPES | Cargo identities to support |
|---|---|
| Bar/ingot piles | **Gold, copper, iron, netherite ingots** |
| Individual loose faceted gems | **Diamonds (items)** |
| Ordered stacks of squared storage blocks | **Gold, copper, iron, diamond, netherite BLOCKS** |
| Rough mined ore rocks (not smooth refined metal cubes) | **Gold, copper, iron ORE** |
| Uneven loose soil/rubble heaps | **Sand, gravel, cobblestone, dirt** |

This is **17 material identities in five distinct silhouette families**. The geometry and texture must both communicate the actual form: diamonds ≠ diamond blocks; iron ingots ≠ iron ore ≠ iron blocks; rubble ≠ sand ≠ gravel ≠ dirt. Colors alone applied to one universal flat fill plane are **insufficient**. Work with the game's **actual item IDs and cargo-eligibility system**: ore blocks vs raw ores/deepslate variants must not be quietly conflated; document which item variants are actually eligible and ask the owner about unsupported variants instead of silently changing cargo rules.

## 2. Sequential physical fullness

Retain one-item-type, **256 capacity** and the current item inventory. The milestone baseline is **EMPTY + four nonempty bands**: EMPTY = 0, LOW = 1–64, MEDIUM = 65–128, HIGH = 129–192, FULL = 193–256. Exact thresholds and appearance remain owner-reviewable before art freeze. A cart with a single item is visibly nonempty; a full one reads full, and every band has materially different 3D height/coverage. All material families advance appropriately (ingot stacks accumulate bars; blocks accumulate cubes; gems/ore/rubble form progressively deeper piles). No pile sticks out of the functional interior or blocks gate movement.

**17 materials x 4 nonempty bands = 68 logical render combinations (+ shared empty).** This does **not** mean Joe must paint 68 independent PNGs. Favor shared reusable height meshes, silhouette groups and carefully assigned atlas regions/material surfaces. Claude documents the actual number of owner-painted files needed. Use truthful distinct placeholders before Joe paints, never unrelated/falsely colored cargo.

## 3. Rendering integration and constraints

- Inspect the **final, shared four-gate minecart** from M07 before writing geometry: permanent four corner posts, two bottom-hinged short end gates and top-hinged long sides. When open, genuine openings must remain visible; cargo meshes must not clip through either ramp/door, cover the loading interaction, or float outside the cart. Render with curves, slopes, motion, docked trains and cart orientation.
- Derive form and fill band **only from the actual synchronized freight cargo type/count**. Changes during loading, unloading, transfers and moving between materials after emptying must update visually; save/reload and dedicated-server clients must agree. Do not maintain a second independently persisted item count.
- Passenger/cushioned and Cobblestone Engine carts show **no freight fill**. Preserve the base four-gate cart's appearance in all uses, and all current cart mechanics/capacity, freight item eligibility, cable movement, docking, fuel and item conservation.
- Unknown but eligible cargo receives an honest generic fallback *rather than an invisible load or depiction of a different named material*. No new cart type, commodity, item, machine, recipe, GUI, automatic unloading, sound or VFX.
- Preserve intentional exact cargo amount/type inspection. Any removal of floating developer item-count labels is a separately testable UX decision; don't accidentally remove inspection.

## 4. Claude deliverables and tests

1. Maintainable cargo item → identity/form mapping and quantity → step logic (not 17 scattered ad-hoc renderer checks), with graceful fallback; avoid redundant network or inventory state.
2. Model/silhouette variants for **ingots, loose gems, refined block cubes, ore rock, bulk heaps**, in four rising volumes; textures selected **by actual material**.
3. **Editable Blockbench .bbmodel sources** where modeled, exported runtime assets, shared UV maps, named placeholder texture(s), direct in-game examples and asset-reuse documentation. Never overwrite user-painted art. Add actual asset paths and Joe's painting tasks to [the owner checklist](Joe's%20To-Do/BLOCKBENCH_PAINTING.md).
4. Tests for all transitions 0/1/64/65/128/129/192/193/256, all 17 identity mappings (or explicitly documented ineligible types), subtype changes after emptying, safe fallback, no fill on cushion/engine carts, 256-capacity/item conservation, station transfers and coupled carts.
5. **Actual-client visual proof** of all five shapes and step differences, at least two materials per multi-material family, gate open/closed and different travel orientations; multiplayer and save/reload proof. Negative controls showing the tests fail with a deliberately wrong mapping or constant fill height. Only after owner separately authorizes implementation: full server/client regression, green CI artifact and report with PASS/FAIL/NOT RUN. Artwork remains pending Joe's approval.

This brief is **not** an instruction for Claude to paint all those cargo piles. It *is* a commitment that Claude will supply the correctly differentiated shapes and fill-state selection system once the owner authorizes M11.
