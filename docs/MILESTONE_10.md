---
milestone: M10
status: PLANNED - READY
---

# M10 — Modular workwear, spelunking rope, armor retirement and set effects

**Status: PLANNED - READY (2026-10-13).** Owner-approved behavior and recipes are sufficient for an implementation handoff; technical details and balancing belong to Claude's implementation discretion, within the constraints below. **Not implementation-authorized:** do not begin coding M10 until the owner explicitly authorizes it.

## Owner-settled scope, behavior and safeguards

- **Remove gold and chainmail armor sets entirely from player-facing Simple Block:** no craftable, lootable, tradeable or creative-listed items or naturally equipped examples. Retire them with world/registry compatibility precautions; gold as a material stays. Replacement/drop mappings and mob gear tables need an audit.
- **Head: Headlamp / Copper Mining Helmet.** A **copper mining helmet with a vanilla-style lantern physically mounted upright on top**, inspired by an old-fashioned hard hat with a prominent beacon. **Replace the lantern's hanging hook with a copper cap/housing above the lantern**, visibly attaching and finishing its upper end. Preserve the recognizable protruding lantern-on-helmet silhouette in the wearable model and item presentation; do not redesign it as a small forehead spotlight. Worn in the head slot, it provides head protection **just below iron armor** and **steady, usable light whenever worn**, including without the rest of the workwear set. **No fuel, consumables, recharging, or conical/directional beam requirement.** Use ordinary omnidirectional Minecraft-style light behavior (not a flashlight cone); Claude may choose a compatible dynamic-light implementation that works in multiplayer and does not leave permanent artificial light behind when the player moves, unequips or disconnects. Exact protection, durability, light intensity, model construction and rendering are engineering/tuning decisions, not open game-design gates.
- **Chest and legs: Overalls (settled).** **One garment/item occupies both equipment slots at once**, excluding normal chestplates AND leggings. This prevents combining the +9-slot backpack (M02's **27 → 36 including hotbar**) with high-value armor from either of those slots. Its combined protection is at most that of a vanilla leather chestplate plus leggings. Claude must implement safe, atomic dual-slot equip/unequip; this design is **not** two independently equippable pieces.
- **Feet: Spiked Boots** give the named effect **Spelunker's Speed** when worn alone: faster rope/ladder climbing. When worn as part of the complete spelunking outfit, the visible effect label **changes to Trail Blazer**, retaining the same climbing speed without a second multiplier. Recipe and ingredient are specified below; climb multiplier, armor protection and durability are delegated tuning.
- **Feet: Mining Boots** grant the named boots-only bonus **Mule**: when added in **M10**, permit extra **ingots, storage/material blocks and ore** over M08's regular **64-ingot-equivalent weighted allowance** with a **progressively increasing movement-speed penalty** as weighted overload rises. **M08 separately changes the ordinary limit so equipment (rails, lanterns, buckets, minecarts, tools) remains weighted but is never refused merely for exceeding the cap** ([M08 note](MILESTONE_08.md)); boots and slow-walk behavior remain M10 implementation. On equipping the complete mining outfit, **Old Pro replaces Mule in the displayed bonus list while retaining this carrying effect**, just as Trail Blazer replaces Spelunker's Speed. Exact penalty and any maximum remain open; freight weight exemptions stay intact.
- **Roles are modular:** same headlamp/overalls for work, with a **boots choice** between fast climbing and carrying overweight. There are **not separate spelunking/mining chestplates**. Neither boots variant adds slots by itself. Conventional combat mechanics (including jump crits) are unaffected by this decision.
- **Spelunking access equipment included in M10:** craftable **Rope** and anchored **Spelunker's Rope** are part of this milestone, not a separate future update. They give Spiked Boots' climbing bonus an actual supported hanging-rope use case. Detailed owner-settled recipes, support placement and extension behavior are below; cable/winch hauling systems from M04–M07 remain separate.
- **Complete-outfit bonuses (owner direction; tuning open):** require **Headlamp + two-slot Overalls + matching boots**. **Spiked Boots alone:** **Spelunker's Speed** improves ladder/rope climbing. **Full spelunking outfit:** the displayed Spelunker's Speed effect is **replaced by the single named set bonus “Trail Blazer,”** which retains faster climbing, reveals faint ghosted nearby **torches, ladders and spelunking ropes** through walls (**yellow** player-placed by any player; **blue** world-generated) and reduces skeleton archery accuracy. No second stacked climbing multiplier or separate Shiny/Trail Sense buffs. **Full mining outfit (Headlamp + Overalls + Mining Boots): “Old Pro”**, replacing the displayed **Mule** effect without losing its overweight-carrying bonus, granting **intentional offhand pickaxe mining** while the main hand can hold/use a sword, axe or other item, and showing a **red glow along the bottom edge of the screen ONLY when a hostile monster is behind the player, within 20 blocks and has a clear, unobstructed eye-line to Steve's upper body/neck in his SECOND block of height**. Wall-obscured monsters never trigger it; line of sight matters, not whether mob pathfinding can reach the player. Old Pro **replaces** the previously proposed +5% mining-speed perk; Mining Boots alone still allow overweight carrying with increasing slowdown. Offhand block-breaking controls must preserve mainhand combat/chopping, avoid unintended blocks or double-speed mining, and not add offhand combat attacks. Rear alert is directional, not a mob radar: unobstructed monster-eye-to-upper-player line of sight is mandatory; exact rear angle, neck-target ray position and visual intensity are open for tuning. Trail Blazer range, fade, accuracy effect and old-world placement provenance remain open; Claude should persist player placement and use a documented unknown-origin fallback. Not an unbounded through-wall ore scan.
- **Exact shaped crafting recipes for all four workwear items are owner-settled**, with 3×3 layouts defined in **the recipe section below**. Headlamp: five copper ingots + center vanilla lantern; Overalls: two top-corner strings, top-center vanilla chest, center leather chestplate, bottom-center leather leggings; Spiked Boots: leather boots over pointed dripstone; Mining Boots: leather boots over iron ingot. Preserve empty cells and the specified positions. No new materials or ingredients implied.
- **Implementation responsibilities, not readiness blockers:** safely handle reserved chest/leg slots and backpack-row items on Overalls equip/unequip/replacement; preserve sorting, shift-click, deaths, persistence and no-duplication. Claude may set sensible default crafting output counts, armor stats/durability, **Headlamp light level and compatibility-safe moving illumination**, **Trail Blazer** skeleton-aim tuning and infrastructure rendering/provenance, **Old Pro** offhand-use and rear-warning implementation, and gold-armor removal/piglin behavior without undermining the settled design. Document meaningful choices and verify them in tests and owner playtesting. **No implementation authorization yet.**

## Related milestone boundary

M08 makes weighted mining supplies obtainable even above 64 ingot-equivalents, but only metal/raw-resource intake remains capped; **M10** adds Mining Boots, their **Mule** over-limit carrying/slowdown effect, and full-outfit **Old Pro**. Do not move this equipment implementation into M08 without explicit owner approval. The separate iron/diamond/netherite combat armor bonuses remain **post-M10 only**, in [BACKLOG.md](BACKLOG.md), not M10.

---

## Exact shaped workwear recipes — owner-settled, not implemented

All four recipes are **shaped 3×3 crafting layouts**. The specified positions and empty slots matter; do not convert these to shapeless crafting recipes. "Chest" means the **ordinary vanilla chest block/item**; "leather pants" means **vanilla leather leggings**. Reuse vanilla copper ingot, lantern, string, leather armor, pointed dripstone and iron ingot; no additional intermediate ingredients.

**Headlamp** — 5 copper ingots and 1 vanilla lantern:

```text
[copper ingot] [copper ingot] [copper ingot]
[copper ingot] [lantern]      [copper ingot]
[empty]        [empty]        [empty]
```

**Overalls** — 2 string, 1 vanilla chest, 1 leather chestplate, 1 leather leggings:

```text
[string] [chest]              [string]
[empty]  [leather chestplate] [empty]
[empty]  [leather leggings]   [empty]
```

**Spiked Boots** — 1 leather boots, 1 pointed dripstone underneath:

```text
[empty] [empty]            [empty]
[empty] [leather boots]    [empty]
[empty] [pointed dripstone][empty]
```

**Mining Boots** — 1 leather boots, 1 iron ingot underneath:

```text
[empty] [empty]         [empty]
[empty] [leather boots] [empty]
[empty] [iron ingot]    [empty]
```

Recipe layouts and components are settled. Claude may use ordinary single-item crafting output unless an established recipe convention requires otherwise. **Headlamp illumination is settled: on while worn, no fuel, no conical beam, independent of any full-outfit bonus.** The vanilla lantern ingredient is physically represented by the top-mounted lantern in its model; the hanging hook is replaced by a copper cap/housing. Overalls still occupy BOTH chest and leggings slots simultaneously.

**Recipe provenance:** The four exact grids were owner-settled and moved here from the retired decisions ledger; the owner subsequently replaced the Headlamp's original center torch with a **vanilla lantern** (2026-10-13). This revision also settles Headlamp lighting and appearance. Output quantity and technical tuning are delegated to Claude. **PLANNED - READY does not authorize implementation.**

## Rope and Spelunker's Rope — M10 owner-settled access equipment

- **Rope shaped recipe:** three string items vertically in the center crafting column. Result quantity remains unspecified; do not assume a multiple-item yield.

  ```text
  [empty] [string] [empty]
  [empty] [string] [empty]
  [empty] [string] [empty]
  ```

- **Spelunker's Rope shaped recipe:** one existing **copper cable anchor** above one rope in the center column (reuses the current anchor item, not a new kind of anchor).

  ```text
  [empty] [empty]         [empty]
  [empty] [copper anchor] [empty]
  [empty] [rope]          [empty]
  ```

- Place the Spelunker's Rope on the **bottom or side face of a block**. Its anchor fixes at that block; the climbing rope **hangs vertically downward through open air** without needing intermediate attachments, support posts or guides.
- **Hold right-click with Rope in hand** to extend the anchored line downward in **12-block increments**, using the supplied rope resource for each increment. Once it reaches solid ground, any extra length **coils visibly into a bundle on the lowest block** rather than extending through the terrain. Claude may decide exact consumption, coil retrieval and support-removal behavior for M10; every extension must use a real Rope resource and must never grant unlimited free rope.
- This is a **temporary spelunking/access tool**, not a moving mechanical winch cable. Preserve M04/M05 copper cable, anchors and hauling logic; climbing and retrieval controls and the speed of ordinary ladders remain separately tunable.

**Delegated to Claude for M10 (not readiness blockers):** recipe output quantities, resource cost for each 12-block extension, line/coil retrieval, support breakage and drops, climbing controls, collision/obstruction checks and visual implementation. Preserve the shaped recipes, 12-block increments, resource consumption, visible ground coil and stationary anchor. Make placement, extension and removal server-authoritative; prevent free rope, item duplication, passing through terrain or interference with M04–M07's moving cable systems. The Spiked Boots / Trail Blazer climbing bonus must work on Spelunker's Rope as well as ordinary ladders. Provide editable models/UVs and document the chosen defaults for owner playtest.


## M10 implementation and acceptance handoff

**Claude's discretion:** select numerical tuning (durability, armor points, movement/overload and climbing curves, detection range/fade where unspecified), compatible client/server implementation for moving Headlamp light, item/equipment rendering, recipe outputs and animation details; for Rope/Spelunker's Rope choose resource cost, yield, pickup/drop/retrieval and collision-safe control details. Report chosen defaults so the owner can playtest. Do **not** remove or silently redefine the owner-specified effects, precise recipe grids, dual-slot Overalls semantics, Trail Blazer provenance colors, Old Pro's line-of-sight condition, or Rope's fixed hanging-anchor/12-block resource-funded extension mechanic. Do not create a flashlight/cone lighting engine.

**Required test/evidence focus:** Headlamp emits light while equipped without fuel, independently of full workwear; stops emitting when removed and leaves no stranded light after movement, logout or death; wearable model clearly shows the upright lantern with copper cap and no hanging hook. Verify all four shaped recipes (including wrong placements); retired gold/chain armor acquisition and piglin compatibility; Overalls atomically reserve both slots and preserve inventory without duplication; correct Spiked/Mining Boots names and effects alone versus sets; intentional Old Pro offhand mining without interfering with combat; rear-warning eye-line obstruction and behind-only checks; Trail Blazer ghost-color/provenance and skeleton-accuracy tuning; equipment weights and overload behavior remain consistent with M08. Verify both Rope shaped crafting grids and their negative cases; place anchors on block bottoms/sides, lower the line only through open air in resource-funded 12-block increments, coil spare length visibly on solid ground, conserve resources on retrieval/breakage, avoid terrain penetration or unsupported infinite rope, and verify Spiked Boots / Trail Blazer rope climbing. Include dedicated-server and real-client/multiplayer checks where relevant.

**Status: PLANNED - READY. Owner has not authorized implementation.**
