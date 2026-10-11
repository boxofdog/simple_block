# Milestone 02 negative controls

Per CLAUDE.md, each new restriction's tests were run once against a copy of the tree with that change removed, to show the tests detect vanilla behaviour.

## 02A (geology, deepslate gate, iron/diamond wiring)

Removed: `geology.*` and `mining.*` mixins, `RemovedRocks.register()`, the biome modifications in `IronWorldgen`, the recipe/advancement overrides for the removed rocks, the `needs_iron_tool` and mason trade tag overrides, and the diamond placed-feature overrides. Result: **8 failures**, all expected:

```
phantom_game_tests_insomnia_never_spawns_phantoms failed at 8851881, 4, 9014965! waiting for the night sky (overworld clock 25742, sky darkness 0) on tick 42
geology_game_tests_no_recipe_makes_removed_rocks failed at 8851881, 4, 9014999! recipe minecraft:diorite still exists on tick 0
geology_game_tests_chest_loot_never_gives_removed_rocks failed at 8851896, 4, 9014999! loot gave removed rocks: [chests/trial_chambers/corridor -> 8 minecraft:tuff, chests/trial_chambers/corridor -> 18 minecraft:tuff, chests/trial
geology_game_tests_no_villager_trade_involves_removed_rocks failed at 8851911, 4, 9014999! Expected mason level 3 trades to be [minecraft:mason/3/emerald_dripstone_block]: was [minecraft:mason/3/andesite_emerald, minecraft:mason/3
mining_game_tests_deepslate_and_deepslate_ores_need_iron_pickaxe failed at 8851836, 4, 9015016! deepslate with air: progress 6.666667E-4, drops false; deepslate with wooden_pickaxe: progress 0.0044444446, drops true; deepslate wit
worldgen_game_tests_overworld_biomes_use_simple_block_iron failed at 8851896, 4, 9015016! minecraft:badlands still has minecraft:ore_granite_upper; minecraft:badlands still has minecraft:ore_granite_lower; minecraft:badlands still
worldgen_game_tests_diamond_placements_are_thinned failed at 8851911, 4, 9015016! ore_diamond has no keep-chance filter: [CountPlacement[count=7], net.minecraft.world.level.levelgen.placement.InSquarePlacement@1e2d3377, HeightRang
mining_game_tests_server_rejects_breaking_deepslate_with_stone_pickaxe failed at 8851851, 4, 9015016! Expected block Deepslate: got Air at 8851854, 7, 9015019 (relative: 2, 1, 1) on tick 120
```

The `phantom_game_tests_insomnia_never_spawns_phantoms` line is the night-timing flake that led to `GameTestTiming.ensureNight` (fixed afterwards; the test is not part of this control). The preservation tests (crafted deepslate, stone-ore tiers, creative breaking, substitution table) passed without the change, as they should.

## 02B (inventory and heavy load)

Removed: all `inventory.*` mixins and `InventorySlots.register()`. Result: **10 failures**, every restriction test:

```
inventory_game_tests_menus_offer_exactly27slots failed at 1320646, 4, 9682621! Expected chest usable inventory slots to be 27: was 36 on tick 0
inventory_game_tests_pickups_never_use_the_disabled_row failed at 1320661, 4, 9682621! disabled slot 9 received 1 minecraft:wooden_sword on tick 0
inventory_game_tests_items_in_the_disabled_row_are_moved_out failed at 1320676, 4, 9682621! Expected emeralds dropped at the player's feet to be 7: was 0 on tick 2
inventory_game_tests_disabled_row_refuses_clicks failed at 1320691, 4, 9682621! disabled slot accepted the cursor stack on tick 0
inventory_game_tests_shift_click_fills_hotbar_from_the_left failed at 1320706, 4, 9682621! first transfer should land in hotbar slot 1 on tick 0
heavy_load_game_tests_full_load_refuses_any_more_metal failed at 1320631, 4, 9682638! Expected iron ingots picked up at 64 to be 0: was 1 on tick 0
heavy_load_game_tests_exact_integer_accounting failed at 1320646, 4, 9682638! Expected iron block picked up at 63 to be 0: was 1 on tick 0
heavy_load_game_tests_offhand_cursor_count_worn_armor_does_not failed at 1320691, 4, 9682638! Expected nugget picked up with a full offhand load to be 0: was 1 on tick 0
heavy_load_game_tests_transfers_leave_the_remainder_in_the_source failed at 1320706, 4, 9682638! Expected chest keeps what did not fit to be 40: was 0 on tick 0
heavy_load_game_tests_bundles_creative_and_mode_changes failed at 1320601, 4, 9682655! Expected no new heavy intake while over the cap to be 0: was 1 on tick 0
```

Passing without the change, as expected: the weight-table and recipe audits (they check data), crafting at the cap (a "no false refusal" check), sorting (calls the sort directly) and movement speed.

## 02C (light and hostile mobs)

Removed: the `light.BlockStateBaseMixin`, `mobs.RandomStrollGoalMixin`, `mobs.ZombieMixin` and `mobs.CreeperMixin` mixins. The test-only `mobs.MobAccessor` stays, because the door tests use it to read the goal selector. Ran `MobGameTests`. Result: **8 failures**:

```
mob_game_tests_torch_and_lantern_light failed: Block{minecraft:torch} emits 14, expected 12; Block{minecraft:wall_torch} emits 14, expected 12; Block{minecraft:copper_torch} emits 14, expected 12; Block{minecraft:copper_wall_torch} emits 14, expected 12
mob_game_tests_zombies_drift_to_lit_ground_at_night failed: lit ground should be far busier than the dark area   (control 38% / 21%, "on" 41% / 28%)
mob_game_tests_light_behind_walls_still_attracts failed: zombies should gather round the lit box more than the dark one   (control 20% / 23%, "on" 20% / 12%)
mob_game_tests_pursuing_zombie_breaks_oak_door failed: waiting for the oak_door to be broken on tick 6902
mob_game_tests_pursuing_zombie_breaks_crimson_door failed: waiting for the crimson_door to be broken on tick 6902
mob_game_tests_metal_doors_are_attacked_but_not_damaged failed: zombie should attack the iron door
mob_game_tests_creeper_dissolves_in_sunlight failed: waiting for the creeper to dissolve on tick 6702
mob_game_tests_attraction_is_cheap failed: zombies should have made wander decisions: 0
```

All but the last are expected:
- Vanilla torches give 14.
- Vanilla mobs show no lit/dark difference.
- Vanilla zombies on Normal never break doors.
- Vanilla has no metal-door attack and no evaporation.

`attraction_is_cheap` fails only because its decision counter lives in the removed hook. It measures cost; it is not a restriction test.

Passing without the change, as they should, because each checks that vanilla behaviour is kept:
- `unaware_zombies_leave_doors_alone`
- `creepers_in_shade_or_torchlight_stay`
- `creepers_at_night_stay`
- `no_attraction_in_daytime`: husks 18% / 47%.
- `pursuit_overrides_then_attraction_resumes`: the zombies start in the lit area and vanilla wandering leaves them there 74% of the time, so this test shows that combat still comes first, not the attraction itself.

## 02D (hunger)

Removed: the `food.ServerPlayerMixin` and `food.BlockMixin` mixins. Ran `FoodGameTests`. Result: `activity_exhaustion_is_halved` **fails**; every activity costs exactly vanilla's exhaustion:

```
FOOD-01 activities: sprint 10 blocks 1.0000 (vanilla 1.0000, x1.00); walk 10 blocks 0.0000 (vanilla 0.0000, x0.00); swim 10 blocks 0.1000 (vanilla 0.1000, x1.00); 10 jumps 0.5000 (vanilla 0.5000, x1.00); 10 sprint-jumps 2.0000 (vanilla 2.0000, x1.00); break 10 dirt 0.0500 (vanilla 0.0500, x1.00)
food_game_tests_activity_exhaustion_is_halved failed: walk 10 blocks under water: 0.099999994 instead of 0.049999997 (vanilla 0.099999994)
```

`combat_regeneration_and_hunger_effect_unchanged` passes without the change, as it should: it checks that those costs stay vanilla.
