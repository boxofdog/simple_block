# Milestone 03 negative controls

Per CLAUDE.md, the new restriction tests were run once against a copy of the tree with the Milestone 03 changes removed, to show they detect vanilla behaviour.

Removed: the `chestcart.*`, `enderchest.*` and `minecart.*` mixins, `EnderChests.register()` and `ChestMinecarts.register()` (creative tabs), and the ender chest / chest minecart recipe, advancement and loot overrides. The test sources were the final ones. Run with the configuration cache off (a copied `.gradle` cache otherwise runs the original tree).

## Minecart cargo (`MinecartCargoGameTests`)

Result: the no-riding test fails (vanilla seats the player), loading does nothing, and the next test that reads cargo state crashes the batch: without the mixin a minecart has no cargo at all. The other cargo tests test the new feature, not a restriction, so they have no vanilla behaviour to compare.

```
minecart_cargo_game_tests_minecart_places_on_rails_but_nobody_rides_it failed at 11791264, 4, -5752159! using a minecart should not seat the player on tick 0
minecart_cargo_game_tests_loading_is_exact_and_single_kind failed at 11791279, 4, -5752159! Expected left in hand after loading 1 to be 0: was 1 on tick 0
minecart_cargo_game_tests_unloading_respects_space_and_heavy_load failed at 11791294, 4, -5752159! class net.minecraft.world.entity.vehicle.minecart.Minecart cannot be cast to class io.github.boxofdog.simpleblock.feature.MinecartCargo$Cart (net.minecraft.world
```

## Ender chests, chest minecarts, pack animals (`StorageBypassGameTests`)

Result: **6 failures**, every restriction test; the pack-animal test (FRT-12, a regression test) passes, as it should:

```
simple_block-gametest:storage_bypass_game_tests_no_recipe_makes_ender_chests_or_chest_minecarts failed at 11482568, 4, 1769449! recipe ender_chest still exists on tick 0
simple_block-gametest:storage_bypass_game_tests_no_loot_gives_ender_chests_or_chest_minecarts failed at 11482583, 4, 1769449! loot tables still giving an ender chest or chest minecart: [minecraft:blocks/ender_chest] on tick 0
simple_block-gametest:storage_bypass_game_tests_creative_tabs_leave_them_out failed at 11482598, 4, 1769449! 1 minecraft:ender_chest is still in a creative tab on tick 0
simple_block-gametest:storage_bypass_game_tests_ender_chests_cannot_be_placed_or_opened failed at 11482613, 4, 1769449! Expected block Air: got Ender Chest at 11482616, 8, 1769453 (relative: 2, 2, 2) on tick 0
simple_block-gametest:storage_bypass_game_tests_chest_minecarts_cannot_be_placed_but_minecarts_can failed at 11482628, 4, 1769449! Expected chest minecart items after use on a rail to be 1: was 0 on tick 0
simple_block-gametest:storage_bypass_game_tests_existing_chest_minecarts_are_sealed_loot failed at 11482643, 4, 1769449! the chest minecart opened: net.minecraft.world.inventory.ChestMenu@5a1e0c86 on tick 0
6 required tests failed :(
```
