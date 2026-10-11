# M02 Inventory, Sorting, Hotbar, and Heavy Freight

## INV-01: Exactly 27 usable slots

- Player inventory: **18 regular storage slots arranged as two rows of nine**, plus **the normal nine-slot hotbar**: exactly **27** usable carrying slots. Armor, offhand, and crafting input/output continue to exist under ordinary vanilla rules and are not counted as additional general storage rows.
- This must be more than hiding nine UI squares. The removed row must not be usable for item pickup, chest transfers, hopper-related player inventory operations if applicable, command/creative-to-survival transitions, death drops, crafting returns, or inventory interactions. No unreachable ghost inventory with secretly stored items.
- Adapt the **player inventory screen and any container/merchant/furnace/crafting interfaces that show the player's inventory**, including shift-click mappings, menu indexes, collision/hitboxes, and screen layout. Keep modded screens out of compatibility scope unless the project already includes them.
- Make slot mapping easy to inspect and test. No duplication, voiding, unexpected mouse pickup, or hidden inaccessible slots.
- **Fresh worlds only**: no inventory migration from prior saves. If a player uses creative/commands and changes back to survival, handle excess inventory non-destructively; the normal survival constraints should apply to subsequent acquisition.

## INV-02: Sort inventory button

- Add a clearly labeled/iconified **Sort** button in the **survival inventory**; it operates on the **18 general storage slots only**. **Never alter the hotbar**, armor, offhand, or crafting slots.
- On click, **consolidate compatible stacks** subject to max stack sizes and component/data equality. Do not merge incompatible durability/enchantment/custom data/contents.
- Sort by **the item's registry order / numerical item ID in the target game's active registry**, independent of display language or translated item names. Document the actual ordering API used; in games without a stable persisted numeric registry ID, derive the deterministic *current-session registry order*, with a namespaced identifier tie-break. Do not sort alphabetically by item display name.
- Place sorted stacks **left to right in the bottom of the two storage rows, then left to right in the upper storage row**. Empty slots go at the end of that traversal (the upper/right-hand end).
- Invoke a server-authoritative inventory transaction and sync the UI. Prevent quick sequential clicks from duping or losing items.
- Sorting a partially full inventory should not reorganize the hotbar or secretly spill items into unavailable slots.

## INV-03: Hotbar auto-fill order

- Whenever a vanilla quick-move/shift-transfer operation chooses an **empty hotbar destination**, choose **left to right**, i.e. first empty position beginning at slot **1**, instead of the rightmost slot.
- Preserve existing-stack merges where vanilla reasonably prioritizes merging; this is a **free-slot ordering** requirement, not an instruction to break all shift-click/container semantics.
- Scope to relevant player-side inventory transfers; do not reverse the placement order in chests or unrelated container inventories.

## INV-04: Heavy cargo, enabled in Milestone 02

**Design goal:** Carry small quantities of raw materials personally, but cannot carry *industrial freight quantities* without future physical transportation. Avoid stamina/encumbrance meters, gradual speed penalties, and inventory micromanagement beyond a simple hard cap.

### Capacity and material families

- **One combined pool of 64 metal-ingot-equivalent units** for **iron + gold + copper** across the player's carried inventory. NOT 64 of each metal independently.
- Normal inventory slots are still available for non-heavy objects. No slower walking/sprinting or hunger penalty when close to the cap.
- When a pickup or inventory action would exceed the allowance, only the amount that actually fits may transfer; **remainder stays safely in the original container, crafting output, cursor location, or on the ground as appropriate**. Do not delete the excess or accept it and silently apply a movement debuff.
- **Use exact integer accounting in ninths of an ingot** (capacity = 576 ninths). No repeated floating-point rounding exploits.

### Core weight equivalences

| Carried form | Weight in ingot-equivalents per item |
|---|---:|
| Iron / gold / copper ingot | 1 |
| Raw iron / raw gold / raw copper item | 1 |
| Iron / gold / copper nugget (where item exists) | 1/9 |
| Silk-Touched metal ore block, stone or deepslate variant (and raw resource-containing ore equivalents) | 1 |
| Storage block of iron, gold, copper, or raw iron/gold/copper | 9 |
| Iron pickaxe | 3 |
| Iron bucket | 3 |
| Other metal tools, armor items in inventory, minecarts, anvils, rails, iron doors/hoppers etc. | Actual metal content using an audited explicit mapping, respecting recipe output count |

- Maintain **an auditable, data-driven mapping** of vanilla metal-containing items and their individual ingot-equivalent weights. Account for nonmetal recipe ingredients and recipe output quantities rather than assuming every crafted item weighs one ingot. Representative examples: 16 rails from 6 iron = 3/8 ingot per rail; 3 iron doors from 6 iron = 2 per door; a hopper involves 5 iron. Extend the catalog for any metal-containing items in the actual target version (including newer copper items). No silent bypass by converting ingots into raw blocks, tools, rails, or other high-metal-content recipes.
- Custom or mixed recipes and reversibility may complicate naive weight inference. **Do not require runtime recursive recipe parsing or invent a general economic simulation**; use well-documented explicit weights and regression tests against known crafted forms. If a questionable edge-case item can't sensibly be classified, add it to the report with an explicit decision needed instead of assigning a nonsensical default.
- **Equipped armor is exempt while actually worn** (so protection is not punished), but the same armor item **in a carried inventory slot** counts. Offhand items and cursor-carried items count to prevent obvious bypasses. Ensure equip/unequip does not delete items or silently violate capacity.
- Nonmetal blocks, regular stone, cobblestone, wood, food, and generic construction materials do **not** participate in the heavy limit this milestone. Do not add diamond to the shared metal pool without user approval.
- **Creative and spectator may bypass the intake restriction for QA**, but their carried overcapacity must not be silently erased on switching to survival. In survival, excess may temporarily exist only through a documented non-destructive transition state; block new heavy intake until back under cap.

### Integration paths to protect

Server-side authoritative coverage should include:

- Ground pickup and partial-stack pickup, inventory auto-stacking.
- Shift-click moves between player/containers and container-to-player; ordinary click/drag movements; cursor holding.
- Crafting result acquisition (including shift-crafting) and recipe-produced tools/storage blocks.
- Furnace/smoker/blast furnace output acquisition and smithing/stonecutting as applicable.
- Inventory slot reductions, offhand moves, armor equip/unequip, item drops, respawn, save/reload, and reconnect.
- Item creation through legitimate survival loot/mechanics or commands/testing where appropriate. Repeated move/craft/uncraft paths cannot create free carrying capacity.
- **No grief by silent loss:** excess remains in its source or drops safely at an appropriate source location if preserving it in source is impossible; log/document any unusual behavior.

### HUD / UI

- Show a clear, unobtrusive, readable **current carried heavy weight / 64** indication where relevant (inventory GUI is sufficient for the initial milestone). Avoid a new complex survival meter.
- Do not require physical minecarts, skips, or coal locomotives to exist before enforcing this rule. These come later; heavy cargo is implemented **now**.

## Important QA cases

1. Carry 64 iron ingots. Another iron ingot cannot be picked up.
2. Carry 32 iron + 32 gold; additional copper can't be picked up.
3. Carry 63 ingots, then attempt to pick up an iron block (9): leave it behind unchanged; no fractional/item destruction.
4. Carry 55 ingots, pick up one iron block (9): exactly fills capacity.
5. Carry 9 nuggets = 1 unit; capacity checked in ninths without rounding errors.
6. Replace 9 ingots with an iron block through crafting; no extra apparent capacity. Then craft other metal-containing items and retry.
7. With 64 carried ingots, adding a new iron pickaxe or bucket is rejected if it is carried; wearing an existing iron armor piece is exempt but removing it into inventory is capacity-checked.
8. Offhand and cursor are included in carried total and cannot each hide another 64 units.
9. Transfer from chest/furnace/crafting and via quick-move; blocked remainder remains in source, not deleted/duplicated.
10. Open all relevant screens: exactly 18 + 9 slots usable; never store items in a ghost row.
11. Sort stacks of the same item with different data (e.g. durability/enchants) without merging them improperly; bottom-row left-to-right order; untouched hotbar.
12. Shift-click into free hotbar slots fills 1, 2, 3 in sequence, not 9, 8, 7.
13. Save/reload and death/respawn preserve inventories subject to standard game loss rules; no unintended vanish or duplication.
14. Existing Milestone-01 shulker restrictions remain fully enforced and are not bypassed by inventory refactoring.

**Implementation priority:** correctness and clean interactions are more important than clever weight-calculation abstractions. Confirm all significant weight-bearing item mappings in the final report.
