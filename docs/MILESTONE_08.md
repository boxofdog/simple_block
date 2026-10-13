---
milestone: M08
status: PLANNED - NOT READY
---

# M08 — Standalone blast furnace + more forgiving heavy-load intake

**Owner scoped and refined:** 2026-10-13. **Status: PLANNED - NOT READY.** Not implementation-authorized. Most M08 gameplay, structure and transfer behavior is now owner-settled; the exact revised Bricks-block crafting-grid layout has not yet been recorded, so do not invent it. M07 corrections remain separate. No gameplay code changed by this document.

M08 contains **two scoped jobs** when eventually authorized:

1. **M08 metallurgy:** a single placed 3×3-footprint, four-block-high pre-industrial blast furnace, Bricks-block-based construction recipe (exact grid still to be settled), bulk ore charging/casting, tuned fuel/processing, slower ordinary-furnace raw-ore smelting, village structure replacement of vanilla blast furnaces, and retirement of the vanilla blast furnace. Detailed owner decisions are below.
2. **NEW M08 owner amendment:** revise the M02 personal heavy-load gate so **mining equipment/supplies keep their existing weight but can always be taken**, while remaining load capacity **only restricts additional ingots, material/storage blocks and ore**. This is meant to make hauling/setting up a mine less irritating, **not** to make carried equipment weightless or undo freight logistics.

## Weight is still real; restrictions become commodity-specific

**Keep accounting and intake permission separate.**

- **Weight accounting remains:** keep existing recipe-derived metal/material weights for **lanterns, rails, buckets, minecarts, tools, other weighted equipment/supplies and carried raw resources**, including materials inside bundles, cursor/offhand/crafting interfaces and the existing worn-armor exemption. Don't silently remove weight entries from `heavy_weights.json` to achieve this. The material-colored inventory load bar still represents **total carried weighted material**, not just ore.
- **Equipment does not get refused merely because it weighs something:** in Survival/Adventure-style enforcement, if there is room in the ordinary inventory, a player can pick up, craft into, retrieve, shift-click, equip/unequip or otherwise obtain **mining infrastructure and everyday equipment**, even when the total weighted burden is at/above the normal 64-ingot-equivalent allowance. **Example:** a player already carrying 64 weighted ingot-equivalents may still carry another rail, minecart, bucket or lantern. It adds to the total weight and reduces future bulk-resource headroom (possibly leaving the player over the limit).
- **The *admission cap* applies to bulk valuable mined/refined materials:** attempting to bring in additional **metal ingots, resource/storage blocks and ores** is limited by the **remaining shared weighted allowance**. That includes the item's own recipe-derived weight plus already-carried equipment weight. **Example:** if heavy equipment contributes 20 units of the existing 64-unit allowance, only 44 equivalent units remain for these restricted cargo forms. It is *not* a separate free 64 units of ore on top of equipment.
- **If equipment alone has exceeded 64**, do not drop it, destroy it, arbitrarily prohibit obtaining more supplies, or forcibly unload carried ore. Simply **refuse additional restricted raw-material intake** until weight is reduced or the player later has the M10 Mining Boots/Mule override. Keep all ordinary slot limits, genuine item acquisition restrictions and crafting/material conservation.
- **Commodity forms expressly settled by the owner:** metal ingots, storage/material blocks, mined raw ore pieces, **nuggets**, **loose diamonds**, and **Silk Touch-mined ore blocks** are cap-restricted commodities. Existing weight calculations still apply; do not grant a bypass through ingot ↔ nugget, raw ore ↔ storage block, or diamond ↔ diamond block conversions. Audit the exact item/category list against `heavy_weights.json`, including any other legitimately ambiguous gemstone or byproduct forms, without silently restricting tools/equipment. Preserve material counts through crafting and never destroy existing items at a category boundary.
- **The UI must remain truthful:** a player holding 70/64 units because of equipment should see an over-cap total, not a clamped/falsely empty meter. The red frame/tooltip can explain the burden but **must not imply that the player cannot pick up another shovel, rail or bucket**. No new required progress chores or arbitrary equipment slot restrictions.

## Mining Boots: M10 implements the benefit; M08 prepares the rules

**Important milestone boundary:** The owner says that **wearing Mining Boots means you can carry *more ingots, blocks and ore*, but walk progressively slower**. That effect is already designed as **Mule** in **M10**, with the full outfit replacing the displayed name with **Old Pro** while retaining Mule's carry/slowdown behavior.

- M08 changes what the ordinary cap **blocks**, and should give M10 a clean means to distinguish **counted equipment weight** versus **cap-controlled commodity intake**. M08 does **NOT** introduce the boots item, recipe, crafting, workwear bonus or slow-walk effect early unless the owner separately authorizes changing that milestone boundary.
- When M10 is authorized, **Mining Boots/Mule must let the wearer bring in extra ingots/blocks/ore over the normal allowance**, with a **progressively stronger movement-speed penalty** as the total weighted overload grows. Equipment still contributes to the burden and therefore affects how much additional cargo pushes the wearer into slowdown. Boots never add inventory slots. **Old Pro** retains exactly that carrying/slowdown behavior, not a second stack of it.
- No-boot players cannot take further restricted cargo above the limit even though they can keep acquiring equipment. Taking boots off while already over the ordinary threshold must **not delete or auto-drop anything**; prevent only new restricted material pickups until back under allowance. Protect all multiplayer, death, save/load and shift-click transitions from loss/duplication.
- Exact Mule slowdown curve, whether there is a hard boots-assisted upper bound, and how it interacts with other movement modifiers remain **M10 tuning/open questions**. Don't invent numerical penalties in M08.

## Required implementation checks (when M08 is authorized)

- **At the cap:** fill inventory weight to exactly 64 ingot-equivalents; iron ingots/ore/blocks are refused as before, **but rail, bucket, lantern, minecart and pickaxe pickup/shift-click/crafting succeed** wherever ordinary item rules allow. Their weights correctly raise the bar above 64.
- **Already over capacity by equipment:** keep picking up normal mining equipment without artificial rejection; verify adding a qualifying ingot/ore/metal block is rejected until sufficient capacity is freed. Partial stacks of restricted commodities accept only as many as fit.
- **Resource-first, equipment-second:** pick up 64 ingots, then a weighted equipment item. Both are retained, count rises above the usual cap, but new ore intake is refused. No automatic dropping, undoing legitimate equipment clicks, or out-of-thin-air creation.
- **Recipes and conversions:** explicitly test nuggets, loose diamonds, Silk Touch ore blocks, block ⇄ ingot ⇄ nugget and other material conversions; crafting results must conserve weight and not create a restriction bypass or strand carried materials in inventory. Check bundles, cursor, offhand, crafting grid and drag/shift-click/container rollback. The general `AbstractContainerMenuMixin` safety rollback currently rejects **any** net weight increase beyond the cap; update this coherently instead of fixing only `HeavyLoad.allowedCount()`.
- **Workwear compatibility:** regression placeholder/tests document M10 boots overload granting access to more bulk commodity and progressive walking slowdown, complete-set Old Pro inheriting it, and safe boots removal; do not falsely report tests for a boots item that does not exist yet as PASS in M08.
- **Physical freight remains valuable:** the normal 64-equivalent allowance and one-way resource-intake restriction stay meaningful; this change must not increase cart/skip capacity, remove cargo item restrictions, globally make metals weightless, or alter furnace throughput.
- Test integrated-client load bar at **under, exactly at, and above** 64, real inventory/container item movement, dedicated server as relevant; required server game tests should fail on the old blanket-blocking behavior. Separate M08 code/CI/artifact/report only after implementation authorization; preserve all historical M02 tests/reports as history and update current baselines rather than pretending old enforcement rules still hold.

**Scope status:** These M08 decisions are owner-settled design, **not** permission to implement. The revised furnace crafting-grid pattern remains the one concrete outstanding recipe detail. M10 still owns Mining Boots/Mule/Old Pro.

---

## M08 furnace — owner-settled construction and freight handoff

**Not implementation-authorized.** This section supersedes the earlier hopper/iron-block/cauldron crafting pattern previously recorded for M08. That superseded pattern remains available in the archived pre-consolidation roadmap; **do not implement it** as the final recipe.

### Structure, recipe and village integration

- **Single constructed structure:** a newly crafted furnace item places a single **3×3 footprint × four-block-high** pre-industrial blast furnace, rather than requiring the player to assemble individual structure blocks by hand. The upper receiving hopper and front casting channel/mold are functional parts of that structure; their exact presentation/collision/interaction geometry should support readable, accessible freight and mold access.
- **Recipe material:** use actual vanilla **Bricks blocks**, not individual Brick items. The owner confirmed the new brick-block direction and structure dimensions. **The literal 3×3 crafting grid and any retained old ingredients have not yet been specified; obtain the exact owner-approved layout before implementation.** One furnace item per craft is the conventional proposed yield pending that final grid. Preserve any horizontal mirroring only if confirmed for the revised recipe; do not assume the superseded layout applies.
- **Vanilla blast furnace retirement:** disable its normal crafting/acquisition path and replace vanilla blast-furnace blocks **in newly generated village structures** with ordinary vanilla furnaces. Do not remove registered block IDs or corrupt existing worlds. **Do not modify villager professions, AI, job-site mappings, trades or workstation assignment in M08**; any Armorer or other profession consequences belong to the inhabited-villages section of [BACKLOG.md](BACKLOG.md).
- **Ordinary furnace:** remains available and retains its other uses. Raw-ore processing becomes deliberately slower than industrial smelting. Claude may choose initial timing constants for playtesting.

### Ore, fuel and processing

- **Ore reservoir:** exactly **516 ore items**, in **one inventory slot that deliberately exceeds vanilla 64-stack limits** (analogous to M07's ore skip storage, but not the same capacity). Store one commodity/metal type at a time; safely preserve fractional nine-ore batches and inventory state over save/load, block breaking, interruptions and chunk unload/reload. Do not duplicate or lose contents.
- **Allowed furnace fuel:** **Block of Coal, newly introduced Block of Charcoal, and Dried Kelp Block only**; loose coal/charcoal/kelp and other furnace fuels are not accepted. The new charcoal block has an exact shaped crafting recipe of **nine charcoal in the 3×3 grid → one charcoal block**. No reverse recipe is specified or implied.
- **Fuel storage:** a **single ordinary 64-stack-limited fuel slot**, separate from the 516-ore reservoir and output mold. Fuel burn duration, efficiency and relative throughput are **Claude's tunable implementation choices** until owner playtesting confirms/changes them. Choose clear initial constants, document them, and ensure conservation and sensible large-furnace advantage.
- **Casting:** for raw iron/copper/gold input, use **nine units of raw metal to make one block of the corresponding metal**. The front molten pour is a visual animation, not a fluid simulation or individually broken casting block. The separate front **mold is one normal stack-limited slot (64 blocks)** holding one matching finished metal type. Open/interact with the mold to retrieve its inventory. Completed cast blocks accumulate automatically; if the mold is full or holds a different metal, processing waits safely, preserving contents and progress.

### Physical freight transfers and refusal rules

- **Cart → furnace:** an M07 four-gate cargo minecart on an elevated rail **dumps diagonally downward** into the furnace's large top hopper, following M07's real cart-to-skip unloading principle, with a visible mechanically meaningful gate/receiving alignment.
- **Skip → furnace:** M07's upright ore skip can **deposit directly into the same receiving hopper from its bottom chute**; routing through a minecart is optional, not required.
- **No material obstruction:** if the furnace is holding a **different material**, has no remaining input space, or the receiving path/physical transfer is blocked, the minecart or skip **does not dump** into it. Do not mix ore types in the single reservoir, auto-eject occupants, drop items into the world, or replace the existing cargo. Transfer only genuinely compatible amounts with safe partial transfers; refusal must leave the source unchanged for the refused amount.
- **Safe operation:** integrate with existing M06/M07 mechanics instead of reimplementing them or adding invisible teleports, belts, automatic factories or new freight systems. Never lose or duplicate input, fuel or output during rejected transfers, shutdown, save/reload, breakage or chunk transitions.

### Acceptance and delegated tuning

- Check the complete placed 3×3×4 shape, collision/access, input and fuel slots, 516th accepted / 517th refused raw item, single-type input, interrupted nine-to-one casting, mold capacity/type protection, and charcoal-block recipe and refusal of invalid fuels.
- Exercise diagonal cart→hopper and direct skip-chute→hopper delivery under actual positioning, partial capacity, mixed metal, full/blocked input, save/reload and source recovery; preserve cart/skip inventories on refusal and only accept correctly aligned cargo.
- Confirm new village structures substitute regular furnaces for vanilla blast furnaces while villager professions, job AI and trades remain unchanged by **M08**. Verify vanilla blast-furnace recipe/acquisition retirement without destructive registry changes.
- Exercise regular-furnace raw-ore slowdown and compare large-batch industrial throughput. Claude chooses and reports initial testable processing/fuel constants; playtesting determines final balance.
- No steelmaking, new metal refining stages or charcoal-block reverse recipe are part of M08. Future steel and village-profession redesign remain backlog work.

**One outstanding owner input for an executable M08 brief:** the revised Bricks-block furnace crafting layout (specific ingredients in all nine crafting-grid positions, and any mirror rule). Do not invent it. This unresolved recipe is why the milestone remains PLANNED - NOT READY.

Full historical design context is archived in [ROADMAP_PRE_CONSOLIDATION_2026-10-13.md](archive/ROADMAP_PRE_CONSOLIDATION_2026-10-13.md); the active requirements are above. Other future ideas remain in [BACKLOG.md](BACKLOG.md).

### Related M08 heavy-load integration decision (from previous roadmap)

**OWNER AMENDMENT (2026-10-13):** Alongside the existing blast furnace, **M08 changes M02's heavy-load admission rule, NOT its weight accounting**. Lanterns, rails, buckets, minecarts, tools and other mining equipment still contribute their existing recipe-derived material weight. But **equipment can be picked up/crafted/transferred irrespective of reaching/exceeding 64 ingot-equivalents**, if ordinary inventory room exists. **Only taking in additional ingots, resource/storage blocks and ore is capacity-gated** by the remaining allowance, including the weight of all that equipment. This is not a new separate materials allowance: 20 weighted equipment units leave 44 for restricted raw materials; 64 ingots + a rail remains in inventory but prevents taking further ore. No automatic item dropping or global suppression of carry weights. Nuggets, loose diamonds, mined raw ore pieces and Silk Touch ore blocks are cap-restricted commodities; audit remaining material conversions to prevent category bypasses. Preserve a truthful inventory load display above the cap and server-side checks across pickups/crafting/containers/bundles.

**M10 COMPATIBILITY:** Mining Boots/Mule, **introduced in M10 rather than M08**, let the wearer carry **more of the cap-restricted ingots/blocks/ore** in exchange for **progressively slower walking** as weighted overload increases; equipment still counts toward burden. Full-set Old Pro keeps Mule's overload benefit without stacking it twice. M08 supplies a compatible classification/eligibility rule but **does not preemptively add the boots, recipe or movement modifier**. No change to freight carts/skips or M08's furnace design. [Detailed M08 design note and future test matrix](MILESTONE_08.md).
