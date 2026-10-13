# M08 — Standalone blast furnace + more forgiving heavy-load intake

**Owner scoped:** 2026-10-13. **PLANNED, NOT IMPLEMENTATION-AUTHORIZED.** This is a design addendum / handoff note, not a claim that M08 has begun or that its remaining furnace design decisions are finished. M07 corrections remain separate. No gameplay code changed by this document.

M08 contains **two scoped jobs** when eventually authorized:

1. **Existing M08 metallurgy:** the substantial standalone pre-industrial blast furnace, its owner-settled horizontally mirrorable shaped recipe, industrial fuel/charging/casting, normal furnace raw-ore slowdown, and vanilla blast-furnace retirement. **The detailed furnace requirements and recipe remain authoritative in [ROADMAP.md, "Future bulk ore smelting and freight transfer"](ROADMAP.md)**; do not alter or prematurely implement them as part of this weight note.
2. **NEW M08 owner amendment:** revise the M02 personal heavy-load gate so **mining equipment/supplies keep their existing weight but can always be taken**, while remaining load capacity **only restricts additional ingots, material/storage blocks and ore**. This is meant to make hauling/setting up a mine less irritating, **not** to make carried equipment weightless or undo freight logistics.

## Weight is still real; restrictions become commodity-specific

**Keep accounting and intake permission separate.**

- **Weight accounting remains:** keep existing recipe-derived metal/material weights for **lanterns, rails, buckets, minecarts, tools, other weighted equipment/supplies and carried raw resources**, including materials inside bundles, cursor/offhand/crafting interfaces and the existing worn-armor exemption. Don't silently remove weight entries from `heavy_weights.json` to achieve this. The material-colored inventory load bar still represents **total carried weighted material**, not just ore.
- **Equipment does not get refused merely because it weighs something:** in Survival/Adventure-style enforcement, if there is room in the ordinary inventory, a player can pick up, craft into, retrieve, shift-click, equip/unequip or otherwise obtain **mining infrastructure and everyday equipment**, even when the total weighted burden is at/above the normal 64-ingot-equivalent allowance. **Example:** a player already carrying 64 weighted ingot-equivalents may still carry another rail, minecart, bucket or lantern. It adds to the total weight and reduces future bulk-resource headroom (possibly leaving the player over the limit).
- **The *admission cap* applies to bulk valuable mined/refined materials:** attempting to bring in additional **metal ingots, resource/storage blocks and ores** is limited by the **remaining shared weighted allowance**. That includes the item's own recipe-derived weight plus already-carried equipment weight. **Example:** if heavy equipment contributes 20 units of the existing 64-unit allowance, only 44 equivalent units remain for these restricted cargo forms. It is *not* a separate free 64 units of ore on top of equipment.
- **If equipment alone has exceeded 64**, do not drop it, destroy it, arbitrarily prohibit obtaining more supplies, or forcibly unload carried ore. Simply **refuse additional restricted raw-material intake** until weight is reduced or the player later has the M10 Mining Boots/Mule override. Keep all ordinary slot limits, genuine item acquisition restrictions and crafting/material conservation.
- **Keep the restricted-class definition narrow and auditable.** Iron/copper/gold/netherite ingots, storage/material blocks, ore blocks and mined raw ore drops are obvious candidates; diamond blocks are material blocks. **Nuggets, loose diamonds/gems and conversion byproducts need an explicit consistency decision before implementation** so a player cannot trivialize the intent by cycling ingot ⇄ nugget or raw item ⇄ block and back. This is **an implementation classification question**, not permission to silently cap all tools/gear again. Build an explicit item/category list, document any exceptions and get the owner to settle genuinely ambiguous forms. Preserve weight-neutral crafting/repackaging and never destroy existing items at a category boundary.
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
- **Recipes and conversions:** block ⇄ ingots ⇄ nuggets / raw forms and crafting results must conserve weight and not create a restriction bypass or strand carried materials in inventory. Check bundles, cursor, offhand, crafting grid and drag/shift-click/container rollback. The general `AbstractContainerMenuMixin` safety rollback currently rejects **any** net weight increase beyond the cap; update this coherently instead of fixing only `HeavyLoad.allowedCount()`.
- **Workwear compatibility:** regression placeholder/tests document M10 boots overload granting access to more bulk commodity and progressive walking slowdown, complete-set Old Pro inheriting it, and safe boots removal; do not falsely report tests for a boots item that does not exist yet as PASS in M08.
- **Physical freight remains valuable:** the normal 64-equivalent allowance and one-way resource-intake restriction stay meaningful; this change must not increase cart/skip capacity, remove cargo item restrictions, globally make metals weightless, or alter furnace throughput.
- Test integrated-client load bar at **under, exactly at, and above** 64, real inventory/container item movement, dedicated server as relevant; required server game tests should fail on the old blanket-blocking behavior. Separate M08 code/CI/artifact/report only after implementation authorization; preserve all historical M02 tests/reports as history and update current baselines rather than pretending old enforcement rules still hold.

**Scope status:** The owner has **approved this design direction**, not started M08 implementation or moved the M10 workwear milestone into M08. M08 furnace design remains as previously settled, and M10 still owns Mining Boots/Mule/Old Pro.

---

## Furnace requirements moved here from ROADMAP.md (2026-10-13 consolidation)

**Authoritative M08 furnace design baseline — owner decisions recorded before this consolidation, NOT implementation authorization.** The earlier roadmap's furnace/recipe details now live in this handoff alongside the existing M08 carrying-cap adjustment above. Historical stage labels and old planning status in the source are subordinate to the current status statement at the top of this file. **The owner is currently DISCUSSING a possible 3×3 footprint / four-block height and a brick-based recipe; neither the new dimensions nor the revised recipe have been finalized or authorized for code yet.** The prior recipe below is the last fully documented design, subject to explicit replacement after that conversation. Do NOT implement either alternative without a settled final brief.

### Future bulk ore smelting and freight transfer (planned M08 blast furnace; not authorized)

- **Batch-based raw-material handling, not Create-style ore conveyors.** The practical mine-to-blast furnace loop should move bulk ore in minecarts or upright mining skips, then discharge it **by gravity into a raised receiving hopper** on a substantial, constructed blast furnace. No requirement to transport every individual ore item through exposed processing belts.
- **Two processing scales:** retain the ordinary furnace for early/small-scale iron and other familiar uses, but **slow its raw-ore smelting** so it does not compete with bulk processing. **Remove/disable the vanilla blast furnace** in a future authorized metallurgy milestone to avoid a redundant intermediate metal-processing shortcut.
- **Large blast furnace:** a visually substantial piece of settlement infrastructure with a hopper/charging point, a fuel demand, and a deliberate output or tapping point. Its functional reward is handling large quantities of ore efficiently, making rail elevation, terminal placement, vertical skips and a permanent mine-to-settlement supply line valuable.
- **Physical delivery:** design unloading from elevated rails and skips alongside the receiving hopper, not as independent item-teleportation or general automatic-factory systems. Material should never disappear or duplicate when an unloading attempt fails or the recipient is full.
- **M08 pre-industrial blast furnace recipe (OWNER-SETTLED, not yet implemented):** craft the **new standalone pre-industrial blast furnace** with the following exact **shaped 3×3 layout**, also accepting the **horizontal mirror** of the whole arrangement. All explicitly empty slots must remain empty; it is **not shapeless** and may **not rotate** by 90°/180°. `Iron block` means a normal vanilla **Block of Iron**; `iron ingot` means an ordinary vanilla **Iron Ingot**; `hopper`, `cauldron` and `furnace` are the existing vanilla items. No extra intermediates.

  ```text
  [empty]    [hopper]     [empty]
  [empty]    [iron block] [iron ingot]
  [cauldron] [furnace]    [iron ingot]
  ```

  **Accepted horizontal mirror:**

  ```text
  [empty]      [hopper]     [empty]
  [iron ingot] [iron block] [empty]
  [iron ingot] [furnace]    [cauldron]
  ```

  **Owner has settled ingredients/placement and mirroring, not the output count.** Use one blast furnace item per craft as a *proposed* conventional default when finalizing M08, unless owner specifies otherwise. Future M08 tests should accept either pictured arrangement and reject mirrored-by-rotation, misplaced ingredients and extraneous items. This is **M08 only**, not permission to implement it with M06.
- **Fuel and casting direction:** the pre-industrial blast furnace accepts **blocks of coal, charcoal, or dried kelp** as its only planned fuels. Coal blocks and dried kelp blocks are vanilla; a charcoal block would require a new item/recipe, to be scoped separately. Smelt raw iron, copper or gold into the corresponding **metal blocks**, preserving vanilla block-to-ingot crafting reversibility. No mandatory ore crushing or refinement stages.
- **Animated channel, practical mold inventory (owner decision):** show molten metal pouring down the front casting channel into a mold as an **animation only**, not a simulated fluid or a block the player must repeatedly break. **Interact with the mold to open/collect its output inventory; it stores at most one stack (64) of a single iron, gold or copper block type.** Finished blocks accumulate there automatically without requiring a click after every casting. No mixed block types in the same mold at once; when full or containing a different type, further output must wait safely rather than vanish. Preserve partial progress when raw-item quantities are not divisible by nine.
- **Open design questions before a milestone brief:** blast furnace structure/footprint, raw-input reservoir and hopper capacity, exact charcoal-block crafting rule, furnace slowdown, blast furnace fuel and processing rates, whether and how users switch between metal types, manual vs passive freight dumping, and later steelmaking integration. No conveyor networks or industrial process simulation by assumption.

Full design context lives in [BACKLOG.md](BACKLOG.md), confirmed implementation choices in [DECISIONS.md](DECISIONS.md), and the running visible-text cleanup list in [UX_TEXT_LOG.md](UX_TEXT_LOG.md).

### Related M08 heavy-load integration decision (from previous roadmap)

**OWNER AMENDMENT (2026-10-13):** Alongside the existing blast furnace, **M08 changes M02's heavy-load admission rule, NOT its weight accounting**. Lanterns, rails, buckets, minecarts, tools and other mining equipment still contribute their existing recipe-derived material weight. But **equipment can be picked up/crafted/transferred irrespective of reaching/exceeding 64 ingot-equivalents**, if ordinary inventory room exists. **Only taking in additional ingots, resource/storage blocks and ore is capacity-gated** by the remaining allowance, including the weight of all that equipment. This is not a new separate materials allowance: 20 weighted equipment units leave 44 for restricted raw materials; 64 ingots + a rail remains in inventory but prevents taking further ore. No automatic item dropping or global suppression of carry weights. Restricted material form edge cases (nuggets, mined raw pieces, loose gems) require a documented item/conversion audit so the rule cannot be bypassed through recipes. Preserve a truthful inventory load display above the cap and server-side checks across pickups/crafting/containers/bundles.

**M10 COMPATIBILITY:** Mining Boots/Mule, **introduced in M10 rather than M08**, let the wearer carry **more of the cap-restricted ingots/blocks/ore** in exchange for **progressively slower walking** as weighted overload increases; equipment still counts toward burden. Full-set Old Pro keeps Mule's overload benefit without stacking it twice. M08 supplies a compatible classification/eligibility rule but **does not preemptively add the boots, recipe or movement modifier**. No change to freight carts/skips or M08's furnace design. [Detailed M08 design note and future test matrix](MILESTONE_08.md).
