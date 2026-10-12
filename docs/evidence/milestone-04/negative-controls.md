# Milestone 04 negative controls

Per CLAUDE.md, the restriction tests were run once against copies of the tree with a rule removed, to show they detect its absence. M04 adds new content, so "vanilla behaviour" here means the same content without the rule. The test sources were the final ones. Run with the configuration cache off (a copied `.gradle` cache otherwise runs the original tree), filter `simple_block-gametest:cable_game_tests_*`.

The multi-step tests skip their later steps once one has failed (`CableGameTests.step`): the vanilla runner keeps only the last failure, which would otherwise hide the cause.

## Control A: four rules removed

- `CableAnchorBlock.canHangFrom`: only the "offers the middle of its underside" check, not the wooden-support tag;
- `CableRoutes.routeThrough`: no 12-block span limit, and an anchor that already carries a cable accepts another;
- `ConnectingRods.tick`: does nothing (the rod never grips).

Result: **4 failures**, one per rule; the other 6 cable tests pass.

```
simple_block-gametest:cable_game_tests_anchor_hangs_only_under_wood failed at -9083852, 4, -12391301! Did not expect block to be Copper Cable Anchor at -9083851, 10, -12391293 (relative: 0, 4, 6) on tick 0
simple_block-gametest:cable_game_tests_cable_routes_through_anchors_and_uses_its_length failed at -9083822, 4, -12391301! a 16.5-block span should be refused (longest 12) on tick 2
simple_block-gametest:cable_game_tests_anchor_carries_one_cable failed at -9083785, 4, -12391301! the second cable should not run through an anchor that carries the first on tick 2
simple_block-gametest:cable_game_tests_rod_grips_the_cable_and_passes_anchors failed at -9083852, 4, -12391282! the rod let go of the cable between the first and the last anchor on tick 122
4 required tests failed :(
```

## Control B: no obstacle check

- `CableRoutes.obstacle`: always "clear".

Result: **1 failure**, the routing test at the span through a stone block; the other 9 pass.

```
simple_block-gametest:cable_game_tests_cable_routes_through_anchors_and_uses_its_length failed at -7433113, 4, 2121673! a span through the stone at (5, 3, 4) should be refused on tick 2
1 required tests failed :(
```
