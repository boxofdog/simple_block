# Milestone 05 negative controls

Per CLAUDE.md, each new rule's test was run once against a copy of the tree with that rule removed, to show the test detects its absence. Copies exclude `build`, `.git`, `.gradle` and `run`, and run with `--no-configuration-cache`; each used the matching game-test filter. The mutations are in the table; the script is reproducible from it (one string replacement or file removal each). The final test sources were used; the crank's left-click handling was added afterwards, and its own control (the last row) was run on the final code.

| Control | Rule removed | Test run | Result |
|---|---|---|---|
| recipe | `data/minecraft/recipe/furnace_minecart.json` and its advancement override deleted | `furnaceMinecartIsNotCraftable` | FAIL: "recipe furnace_minecart still exists" |
| drops | `train.VehicleEntityMixin` taken out of the mixin config | `furnaceTurnsAnEmptyMinecartIntoAnEngine` | FAIL: broken engine dropped `[iron_chain, furnace_minecart]`, no minecart |
| convert_any | `CobblestoneEngine.convert`: no "empty cart" check | `furnaceTurnsAnEmptyMinecartIntoAnEngine` | FAIL: "a loaded cart should not become an engine" |
| grip_return | `CableIndex.update` also indexes the loop's return points | `returnStrandIsNeverGripped` | FAIL: "a return strand point is in the grip index" |
| loop_checks | `CableLoops.close`: no "enough cable" check | `loopClosingIsRefusedWhenItCannotWork` | FAIL: closed with -17 blocks left on the drum |
| end_stops | `CableDrive.canMove`: no end clearance | `crankHaulsACartAlongTheLoopAndBack` | FAIL: hauled to x 23.5, onto the wheel |
| crank_first | `CableDrumBlock.useItemOn`: no crank check before the coil | `crankLeavesTheOtherGesturesAlone` | FAIL: a coil was wound on (64 instead of 32) |
| coupling_reach | `Couplings.chain`: no 3-block reach check | `chainsCoupleAndShearsUncouple` | FAIL: "carts 10 blocks apart should not couple" |
| engine_grade | `CobblestoneEngine.tick`: no grade force | `sustainedClimbStallsALoadedTrain` | FAIL: the loaded train ran to the end of the line (x 29.5) |
| cable_grade | `CableDrive.driveCart`: no grade load | `haulingUphillIsSlowerForHeavierLoads` | FAIL: an empty cart climbed at 19.0 ticks a block vs 17.7 on the level |
| left_click (client) | `MultiPlayerGameModeMixin` taken out of the client mixin config | `HaulageClientTest` | FAIL: holding left-click on the crank broke the drum ("no cable drum at (20, -60, 6)") |

Every control failed only the test aimed at it, at the expected assertion.

## Raw output

```
== recipe (simple_block-gametest:train_game_tests_furnace_minecart*)
(Minecraft) simple_block-gametest:train_game_tests_furnace_minecart_is_not_craftable failed at -3527198, 4, -6222970! recipe furnace_minecart still exists on tick 0
(Minecraft) 1 required tests failed :(
== drops (simple_block-gametest:train_game_tests_furnace_turns*)
(Minecraft) simple_block-gametest:train_game_tests_furnace_turns_an_empty_minecart_into_an_engine failed at 6939501, 4, -2401855! Expected minecarts dropped [1 minecraft:iron_chain, 1 minecraft:furnace_minecart] to be 1: was 0 on tick 4
(Minecraft) 1 required tests failed :(
== convert_any (simple_block-gametest:train_game_tests_furnace_turns*)
(Minecraft) simple_block-gametest:train_game_tests_furnace_turns_an_empty_minecart_into_an_engine failed at 14188581, 4, -13387313! a loaded cart should not become an engine on tick 2
(Minecraft) 1 required tests failed :(
== grip_return (simple_block-gametest:cable_loop_game_tests_return*)
(Minecraft) simple_block-gametest:cable_loop_game_tests_return_strand_is_never_gripped failed at 10305443, 4, 11146034! a return strand point is in the grip index: (1.03054675E7, 9.75, 1.1146040203125E7) on tick 2
(Minecraft) 1 required tests failed :(
== loop_checks (simple_block-gametest:cable_loop_game_tests_loop_closing*)
(Minecraft) simple_block-gametest:cable_loop_game_tests_loop_closing_is_refused_when_it_cannot_work failed at 4423199, 4, 1846196! one coil leaves -17 blocks: too little for the return on tick 2
(Minecraft) 1 required tests failed :(
== end_stops (simple_block-gametest:cable_loop_game_tests_crank_hauls*)
(Minecraft) simple_block-gametest:cable_loop_game_tests_crank_hauls_acart_along_the_loop_and_back failed at -5489896, 4, -9090453! hauled to the end, short of the wheel at x 23.5: at x 23.499999664723873 on tick 407
(Minecraft) 1 required tests failed :(
== crank_first (simple_block-gametest:cable_loop_game_tests_crank_leaves*)
(Minecraft) simple_block-gametest:cable_loop_game_tests_crank_leaves_the_other_gestures_alone failed at 5773542, 4, -3567306! Expected cable after cranking with a coil in hand to be 32: was 64 on tick 2
(Minecraft) 1 required tests failed :(
== coupling_reach (simple_block-gametest:train_game_tests_chains*)
(Minecraft) simple_block-gametest:train_game_tests_chains_couple_and_shears_uncouple failed at -13508302, 4, 8424868! carts 10 blocks apart should not couple on tick 2
(Minecraft) 1 required tests failed :(
== engine_grade (simple_block-gametest:train_game_tests_sustained*)
(Minecraft) simple_block-gametest:train_game_tests_sustained_climb_stalls_aloaded_train failed at -10597528, 4, -8701864! the loaded train should stall on the climb (x 12-20): reached x 29.509999990463257 on tick 422
(Minecraft) 1 required tests failed :(
== cable_grade (simple_block-gametest:cable_loop_game_tests_hauling_uphill*)
(Minecraft) simple_block-gametest:cable_loop_game_tests_hauling_uphill_is_slower_for_heavier_loads failed at -5826278, 4, 14398129! an empty cart should climb clearly slower than it runs on the level: 19.0 vs 17.666666666666668 ticks a block on tick 2407
(Minecraft) 1 required tests failed :(
== left_click (HaulageClientTest)
java.lang.AssertionError: no cable drum at BlockPos{x=20, y=-60, z=6}
```
