---
milestone: M11
status: PLAYTESTING
---

# M11 — Type-aware minecart cargo fill states, hopper freight transfer and text cleanup

**Owner-scoped 2026-10-13; implementation authorized and delivered 2026-10-14 (see appendix). Status: PLAYTESTING, not SHIPPED.** A standalone **freight-cart rendering, progressive transfer and hopper integration** milestone, separate from the active M07 corrections and planned M08–M10. Number is for tracking and **does not require** completing M08–M10 first. Claude starts only upon specific owner authorization.

**Responsibilities:** Claude codes real cargo-type-aware rendering, **hopper-to-cart and cart-to-hopper inventory integration, incremental transfer, open-gate geometry and text cleanup**, fill transitions, reusable shapes/models, linked UV and placeholder PNG templates, synchronization and tests. Joe paints final textures later, exclusively tracked in [Joe's To-Do](Joe's%20To-Do/BLOCKBENCH_PAINTING.md). Passing CI does not mean finished paintwork.

**Latest owner additions (2026-10-13):** Full hopper support and **progressive filling/draining that actually advances the visible fill steps**, not instant dump; remove **all freight-cart cargo/flavor status text when looked at AND when clicked**. This explicitly overrides the former instruction to preserve on-click exact-number text, and the former "no automatic unloading" exclusion in this brief. All work is still **M11 only when separately authorized**; it is not silently part of M07.

**Owner decisions (2026-10-14, with the implementation authorization):** (1) Skips and minecarts should both **unload more slowly** than the M07/M08 4 items per tick. (2) Cargo eligibility: **add the 17 identities** of §1 (ingots, storage blocks, sand, gravel, dirt) to cart and skip cargo, **keep every item already eligible**, and give each current cargo item (raw metals, deepslate ores, coal, gems, stone, deepslate, redstone) its own look as well.

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

Retain one-item-type, **256 capacity** and the current item inventory. **The fill levels reflect the actual count during each gradual insertion/extraction batch, not a cosmetic animation over an inventory that was already instantly filled or emptied.** The milestone baseline is **EMPTY + four nonempty bands**: EMPTY = 0, LOW = 1–64, MEDIUM = 65–128, HIGH = 129–192, FULL = 193–256. Exact thresholds and appearance remain owner-reviewable before art freeze. A cart with a single item is visibly nonempty; a full one reads full, and every band has materially different 3D height/coverage. All material families advance appropriately (ingot stacks accumulate bars; blocks accumulate cubes; gems/ore/rubble form progressively deeper piles). No pile sticks out of the functional interior or blocks gate movement.

**17 materials x 4 nonempty bands = 68 logical render combinations (+ shared empty).** This does **not** mean Joe must paint 68 independent PNGs. Favor shared reusable height meshes, silhouette groups and carefully assigned atlas regions/material surfaces. Claude documents the actual number of owner-painted files needed. Use truthful distinct placeholders before Joe paints, never unrelated/falsely colored cargo.

## 3. Rendering integration and constraints

- Inspect the **final, shared four-gate minecart** from M07 before writing geometry: permanent four corner posts, two bottom-hinged short end gates and top-hinged long sides. When open, genuine openings must remain visible; cargo meshes must not clip through either ramp/door, cover the loading interaction, or float outside the cart. Render with curves, slopes, motion, docked trains and cart orientation.
- Derive form and fill band **only from the actual synchronized freight cargo type/count**. Changes during loading, unloading, transfers and moving between materials after emptying must update visually; save/reload and dedicated-server clients must agree. Do not maintain a second independently persisted item count.
- Passenger/cushioned and Cobblestone Engine carts show **no freight fill**. Preserve the base four-gate cart's appearance in all uses, and all current cart mechanics/capacity, freight item eligibility, cable movement, docking, fuel and item conservation.
- Unknown but eligible cargo receives an honest generic fallback *rather than an invisible load or depiction of a different named material*. No new cart type, commodity, item, machine, recipe, GUI, sound or VFX. **Hopper-fed loading and gate-to-hopper unloading are expressly required in M11.**
- **Superseding text rule:** Remove freight-cart cargo/status/flavor-text from **crosshair looking** and **click interactions**, including on-click exact quantity/type readouts. Do not add a replacement hover label, action-bar counter, click tutorial or new GUI. The authoritative exact count remains available to game code and tests; the owner prefers readable physical cargo visuals rather than textual narration. Leave player-assigned vanilla custom names alone and do not suppress unrelated winch/Loading Rail or genuine non-cart safety feedback.

## 4. Full hopper compatibility — physical loading and unloading

**M11 is an actual freight-handling milestone, not merely visual skins.** Reuse ordinary vanilla **hopper BLOCKS** and existing standard freight minecarts and **Loading Rails**; no special cart, hopper, chute item or new transport inventory. Respect Minecraft hopper direction, power/locking state, stack limits, capacity, and normal input/output acceptance rather than creating a parallel item-teleportation system.

### A. Hopper → cart, from directly above

- A conventional hopper **immediately above the open top of a freight cart** may feed items down into the cart **only when that freight cart is held/docked at a real Loading Rail**. Rail and hopper must actually line up with the cart's top opening; no transfer through obstructing blocks or from a merely passing cart.
- The hopper inserts **only eligible freight commodities**, one actual item type per cart, until 256 units; a cart with another type rejects the offered item without silently swapping/voiding it. Full carts block gracefully. An existing hopper inventory remains the source of truth and can only transfer what it holds.
- The top is already physically open; **do not open a random end/side gate for top-fed loading**. Hoppers feeding from a conveyor chain or normal upstream hopper network continue to behave as vanilla supplies allow. Locked/redstone-disabled or unloaded hoppers must not feed.

### B. Cart → hopper, through an end or long-side gate

- A freight cart **docked on a Loading Rail** can discharge toward an actual receiving hopper, **from either short end OR either long side** depending on the hopper's world position relative to the four-gate cart. This is the same geometrical idea as cart-to-skip unloading, except the **receiver is the vanilla hopper mouth**. The hopper must be positioned at/below the **gate's real discharge opening** so the cargo has a credible physical path *into the top of the hopper*, not sideways through an apparently sealed container.
- Correct geometry opens **only the matching gate**: **bottom-hinged short end** drops as a ramp if the receiver is in front of that end; **top-hinged long side** swings up/out from its upper hinge if the receiver is alongside the long face. Keep the other three gates closed. On stoppage, loss of docking, blocked receiver, release or interruption, close the gate safely. A coupled train must be able to unload **individual freight carts sideways without decoupling** as each arrives at its Loading Rail; no end gate can require passing through an adjacent coupled wagon.
- Keep the physically distinct **upper/top-loading** and **end/side unloading** rules; a hopper above the cart is an **input**, not an output, and a gate next to a hopper is an **output**, not an input. Do not choose direction from which inventory happens to be full/empty. If more than one eligible receiver/source is present, use a deterministic conflict rule (document and test it; never duplicate, consume twice, or open incompatible gates simultaneously). Skip-to-cart/cart-to-skip docking and direction rules remain intact; no invented skip→hopper mechanic is implied.
- Check actual vanilla hopper collision/mouth, minecart/rail elevations and model pivots in the real client. **Do not declare geometry valid from block adjacency alone.** The top of the hopper must truly be reachable from the gate lip without a solid block/rail/second cart blocking the path. Never bypass protected/locked hopper behavior. If ordinary below-rail hopper extraction would be physically valid, explicitly verify its vanilla behavior and either safely support it through the same inventory contract or document why it is not supported; **the required output paths are end and side gates**.

### C. Real progressive item movement — not instant dumping

- **Hopper transfer is incremental and server-authoritative**: move a small bounded number of items on actual transfer ticks, observing vanilla hopper cadence/cooldown where applicable. **Do not immediately transfer 256 items or a whole cartful and merely play a fake visual filling animation afterward.**
- While the cart fills, its **actual cargo count** increases in batches; while it unloads, the count decreases in batches. At **0 / 1–64 / 65–128 / 129–192 / 193–256**, the **correct visible material-specific fill step** changes promptly on the client, and reverses during unloading. The hopper inventories change by exactly the opposite amount at the **same server-authoritative exchange**.
- **Manual loading also currently dumps an entire held stack on one click** (`MinecartCargo.load`). M11 must replace that instant bulk deposit with **genuinely progressive hand-to-cart filling** (bounded individual/batch transfer over time while the deliberate transfer remains valid), so successive fill thresholds are honestly visible. Do not pre-deduct an entire stack from the player's hand then cosmetically animate it, silently drain a player who walked away, or permit double-use/duping. Preserve purposeful hand unloading and heavy-load restrictions; when hand unloading is changed to gradual, it too must conserve items and obey inventory space.
- A transfer **stalls without burning resources** on full receiver, empty source, mismatched cargo, powered-off hopper, obstruction, station release, removal, world unload or missing required ticking chunks; no forced loading. Resume safely when valid again only if the normal station/transfer rules permit it; never keep a zombie transfer moving items between unloaded objects.
- Preserve conservation through save/reopen mid-fill, breaking either endpoint mid-transfer, switching offered items, competing hopper sources/targets, coupled carts, and simultaneous skip operation. No dropped-world item intermediates unless there is a deliberate real-world spill event; ordinary successful transfer should deposit straight into the receiver's actual inventory without loss.

## 5. No minecart hover/click flavor text

The prior M03 behavior (see `CargoMinecartRenderer.shouldShowName/getNameTag` and `MinecartCargo.interact`) shows **"Empty 0 / 256" / "Iron 128 / 256"** when crosshair-looking at a cart, and emits **status/load/unload/error overlays** on click. **Owner explicitly wants both removed in M11.**

- Looking at a freight cart shows **no automatically generated cargo nameplate, debug count or floating fill status**. Leave vanilla **player-assigned custom names** intact, but never append a cargo counter to a name tag.
- Clicking, right-clicking, feeding a held item, sneak-unloading or using an empty hand must **not produce cart-originated cargo text, status strings, tutorial narration or on-click action-bar overlays**. Do not replace them with a new GUI/counter/tooltip. Visual material and fill depth, visibly opening gates and actual cargo movement become the player's normal feedback.
- **Interactions still work**: cargo transfers, cushions, engines, rods, coupling and special deliberate behaviors continue to function according to their existing rules. Do not remove important unrelated **Loading Rail or winch** status/failure feedback by broadly muting all game overlays.
- This **overrides M11's earlier demand for exact on-click count inspection**, and [UX-004](UX_TEXT_LOG.md). Exact quantities remain internal/server/queryable in tests; no replacement numerical cart text is required. Make sure all former automatically displayed `MinecartCargo.status()` usages are audited, not merely hidden for one camera angle.

## 6. Claude deliverables and tests

1. Maintainable cargo item → identity/form mapping and quantity → step logic (not 17 scattered ad-hoc renderer checks), with graceful fallback; avoid redundant network or inventory state. **Hopper input/output and staged hand loading must update this same authoritative count, not mirror it.**
2. Model/silhouette variants for **ingots, loose gems, refined block cubes, ore rock, bulk heaps**, in four rising volumes; textures selected **by actual material**.
3. **Editable Blockbench .bbmodel sources** where modeled, exported runtime assets, shared UV maps, named placeholder texture(s), direct in-game examples and asset-reuse documentation. Never overwrite user-painted art. Add actual asset paths and Joe's painting tasks to [the owner checklist](Joe's%20To-Do/BLOCKBENCH_PAINTING.md).
4. Tests for all transitions 0/1/64/65/128/129/192/193/256, all 17 identity mappings (or explicitly documented ineligible types), subtype changes after emptying, safe fallback, no fill on cushion/engine carts, 256-capacity/item conservation, station transfers and coupled carts.
5. **Hopper tests:** hopper ABOVE a docked freight cart loads gradually; no Loading Rail, moving cart, blocked top, locked hopper, incompatible item or full cart must not load. Hopper AT A PHYSICALLY VALID END and hopper AT A PHYSICALLY VALID LONG SIDE receive material incrementally with the correct gate opening and closing. Check all four approaches, both gate hinge styles, orientation/rail geometry, full/blocked/locked hopper, other cargo, multiple competing inputs/outputs, train coupling, save/reload mid-transfer, item conservation and negative controls that old instant/bypassing behaviors would fail.
6. **Hand transfer/text tests:** deliberate player loading shows real multi-tick fill progress and correct held-stack decrement; interrupted/failed attempts lose nothing, and intentional unloading honors heavy-capacity/inventory limits. **No custom status nameplate while looking**; **no cart-originated action-bar/status/flavor text on clicking**, whether empty, occupied, loading, unloading or rejected. Preserve player-given vanilla custom names, unrelated rail/winch messages and cart interaction behaviors.
7. **Actual-client visual proof** of all five shapes and step differences, at least two materials per multi-material family, gate open/closed and different travel orientations; multiplayer and save/reload proof. Negative controls showing the tests fail with a deliberately wrong mapping or constant fill height. Only after owner separately authorizes implementation: full server/client regression, green CI artifact and report with PASS/FAIL/NOT RUN. Artwork remains pending Joe's approval.

This brief is **not** an instruction for Claude to paint all those cargo piles. It **does** commit Claude to implement real hopper logistics, progressive cargo movement, correctly differentiated fill-state selection, and removal of cart flavor text **once the owner authorizes M11**. No new hopper textures are required: vanilla hoppers remain vanilla; Joe only paints material fills.

## Implementation and verification — 2026-10-14

**Metadata:** M11 | PLAYTESTING | Cargo looks by form and fill, gradual hand loading, vanilla hopper loading/unloading, slower skips, no floating text.

**Implementation**
- Commits: `8b7f2664f41e6e4b72ae2b48b5724d8665988d17` implementation; `dc2898e6a8e02d3f00e5796133bb8086d31e3fd7`, `e2f1c73a0304e7cf7de7bcc77b3525a5f806a476` test fixes
- Delivered: 30 cargo items (owner: brief's 17 added, rest kept), each its own look in five forms plus covered fallback, four bands; hand use moves 4 items, 1 per 4 ticks; down-pointing hopper over a docked cart feeds it at hopper pace; docked cart tips through matching gate into a hopper diagonally below, 1/tick, order right/left/front/back; skips and furnace 1/tick; all mod action-bar text and cart label removed (UX-010–024).
- Deviations: a locked receiving hopper still accepts (vanilla). Hoppers under the rail get nothing (closed floor). Emptied or filled carts are let go, like skip exchanges.

**CI**
- Build: #52 | Run ID: 38597022406 | PASS
- Source SHA: `e2f1c73a0304e7cf7de7bcc77b3525a5f806a476`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38597022406
- Artifact: `simple-block-mod` id 11909209800, available. Build #52 log: all 215 required server tests passed.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 215/215 | local and CI; 7 M11 tests; [log](evidence/milestone-11/m11-verification-2026-10-14.txt) |
| Client | PASS | 15/15 | full suite; looks and bands match synced cargo; gates open; held-key loading; [screenshots](evidence/milestone-11/screenshots/) |
| Dedicated server | PASS | 1/1 | same M11 client checks over a dedicated server |
| Negative controls | PASS | 9/9 | 9 rules broken, each failed its tests; [log](evidence/milestone-11/m11-verification-2026-10-14.txt) |

**Known gaps**
- GAP-01: MINOR — 31 placeholder material textures; next: Joe paints (To-Do).
- GAP-02: MINOR — rates and band heights provisional; next: playtest.
- GAP-03: MAJOR — refusals now silent (UX-010–024); next: owner picks replacement feedback.
- GAP-04: MINOR — Build #47 hit two unrelated flakes (long line, light behind walls); #48–#52 pass.
- GAP-05: MINOR — skip hand loading still instant (brief covers carts).

**Owner playtest**
- Hold use on a cart with raw iron; watch the load rise.
- Hopper over a Loading Rail; dock a cart; let it fill.
- Hopper beside and below the rail; dock a loaded cart; watch the gate.
- Check nothing prints anywhere.
