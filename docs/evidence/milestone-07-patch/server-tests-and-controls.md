# M07 owner playtest patch and cart redesign: server tests and negative controls (2026-10-13)

`./gradlew build`: **All 152 required tests passed** (the full server game-test suite, fresh world).

## Logged results of the patch tests

```
SKIP-02: [refused with a block at BlockPos{x=13, y=2, z=5}, refused with a block at BlockPos{x=11, y=3, z=5}, refused with a block at BlockPos{x=12, y=3, z=5}]; placed with blocks in front, behind and over the body: box AABB[1469122.0, 7.0, 4777357.0] -> [1469123.0, 10.0, 4777358.0] (no bridle), facing north, side cells along x
SKIP-03: [hand: 576 offered, 512 taken, 64 left in hand, coal and sticks refused, raw iron 100 kept, screen: [button 0: skip 448, cursor 64, inventory 0, button 0: skip 512, cursor 0, inventory 0, button 1: skip 480, cursor 32, inventory 0, button 1: skip 481, cursor 31, inventory 0, button 2: skip 448, cursor 31, inventory 33]]; saved {Air:300s,Fire:0s,Invulnerable:0b,Motion:[0.0d,0.0d,0.0d],OnGround:0b,PortalCooldown:0,Pos:[1469153.5d,7.0d,4777357.5d],Rotation:[180.0f,0.0f],UUID:[I;-1439987627,1384664353,-1807950608,329932100],cargo:"minecraft:raw_iron",count:512,facing:"north",fall_distance:0.0d}; broken: [64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 64 minecraft:raw_iron, 1 simple_block:ore_skip]
SKIP-04: [hung at y 1.0 under the head anchor; one main cable through 3 points, crank pull 40 ticks: y 1.0 -> 2.44, crank feed 20 ticks: down to 1.12, coal winch PULL: stopped at the head (y 7.20, apex 11.20 under the roller at 11.75), lever NEUTRAL (end), FEED: stopped by a block in a side cell, bottom at 4.000 (blocked); coal burnt 29 ticks, FEED to the floor (blocked), then taken off the cable]
SKIP-05: lifted, head anchor disconnected: let go, down on the floor with OreSkip[minecraft:coal x300 at (1469019.5, 7.0, 4777378.5)]; route [BlockPos{x=1469014, y=12, z=4777378}]
SKIP-06: [PULL: stopped at y 6.0 (docked), cart 200 -> 88 into the skip 400 -> 512 via the ramp, FEED: down to the unloading station (bottom 1 + 11/16, lip on the rim), skip 512 -> 256 into the empty bottom cart (front shut, discharge gate open); total 600 kept]
SKIP-07: [unloading place: no station for an unhung skip, a station for the hung one, same level as the chute: skip 300, cart 0, empty skip, loaded cart below the chute: skip 0, cart 100, full skip, empty cart at the bridle: skip 512, cart 0, loaded cart beside the body: skip 0, cart 100, rail at the bridle beside the skip, not in front: skip 0, cart 100, coal cart at an iron skip's bridle: skip 10, cart 100, a block over the cart in the chute's way: skip 300, cart 0]
SKIP-08: released mid-way: skip 24 + cart 232 = 256, nothing more moved, doors closed; partial at the unloading place: cart 200 -> 256, skip 300 -> 244
SKIP-09: [block in the fourth cell: refused, end still held, anchor ordinary, hooked on: bridle (1 x 4), head anchor a pulley facing east; a saved copy loads with its bridle, taken off: no bridle, anchor ordinary, end hanging at y 5.00 (the apex), not at the top, route cut before the head anchor: skip let go without its bridle, anchor ordinary]
SKIP-10: [crank feed 40 ticks: hook 11.25 -> 8.73, anchor ordinary, crank pull 20 ticks: up to 10.05, coal winch FEED: hook down on the floor (eye 1.50, blocked), coal winch PULL: back under the anchor (end); coal only while moving; anchor ordinary throughout]
SKIP-11: [14 blocks across: taut, the hook 12.00 out, still held, 17 blocks across: put down, hanging under its support again, picked up again; a 13-block span to a new anchor refused]
SKIP-12: [north: 100 in at the bridle, 100 out through the chute, east: 100 in at the bridle, 100 out through the chute, south: 100 in at the bridle, 100 out through the chute, west: 100 in at the bridle, 100 out through the chute]
SKIP-13: [bridle, side-on: docked car 100 -> 0 through its south side gate (south), other car 50 untouched, still coupled, below the chute, side-on: docked car 0 -> 100 through its rim, gates shut, other car untouched, still coupled]
WIN-07 Loading Rail 1 before the last support: cart x 22.500 (block 22), stop 'docked', docked true, grip along 21.000 of 22.000, cable end (return wheel) x 23.50, rail x 22, track to x 29
WIN-07 Loading Rail 2 before the last support: cart x 21.500 (block 21), stop 'docked', docked true, grip along 20.000 of 22.000, cable end (return wheel) x 23.50, rail x 21, track to x 29
WIN-07 Loading Rail 2 from the drum: cart x 3.500 (block 3), stop 'idle', docked true, grip along - of -, cable end (return wheel) x 23.50, rail x 3, track to x 29
WIN-07 Loading Rail 3 before the last support: cart x 20.500 (block 20), stop 'docked', docked true, grip along 19.000 of 22.000, cable end (return wheel) x 23.50, rail x 20, track to x 29
WIN-07 Loading Rail under the last support: cart x 23.500 (block 23), stop 'docked', docked true, grip along 22.000 of 22.000, cable end (return wheel) x 23.50, rail x 23, track to x 29
WIN-07 no Loading Rail, track ending under the support: cart x 21.950 (block 21), stop 'end', docked false, grip along 20.450 of 22.000, cable end (return wheel) x 23.50, rail none, track to x 23
WIN-07 no Loading Rail, track past the support: cart x 21.950 (block 21), stop 'end', docked false, grip along 20.450 of 22.000, cable end (return wheel) x 23.50, rail none, track to x 29
WIN-07 terminal Loading Rail under the last support: cart x 23.500 (block 23), stop 'docked', docked true, grip along 22.000 of 22.000, cable end (return wheel) x 23.50, rail x 23, track to x 23
```

## Negative controls

Each control removes one rule from the source (`negctl2.py`: edit, run the named tests, restore). Every control made its test(s) fail; the source was restored after each and the suite passes again.

| Control (rule removed) | Result |
|---|---|
| §1: unload to a cart beside the chute at its own level (old M07 place) | FAIL as expected: `skip_never_exchanges_the_wrong_way` — same level as the chute: skip 188, cart 112 on tick 44 |
| §3: bridle (4th block) always there | FAIL as expected: `skip_needs_its_column_and_side_cells` — Expected skips placed with blocks in front, behind and over the body to be 1: was 0 on tick 0 (+1 more) |
| §§1–2: stations for an unhung skip | FAIL as expected: `skip_never_exchanges_the_wrong_way` — unloading place: unhung skip station true, hung true on tick 4 |
| §4: no pulley on hooking a skip | FAIL as expected: `skip_bridle_and_pulley_come_with_the_cable` — Expected head anchor once a skip hangs from it to be PULLEY: was NONE on tick 2 |
| §4: pulley not reverted on taking the skip off | FAIL as expected: `skip_bridle_and_pulley_come_with_the_cable` — Expected head anchor after taking the skip off to be NONE: was PULLEY on tick 2 |
| §5A: drive does not wind the free hook | FAIL as expected: `free_hook_is_wound_up_and_down` — the crank's feed lowers the free hook: 11.25 -> 11.25 on tick 54 |
| §5A: no put-down distance (unlimited horizontal carry) | FAIL as expected: `cable_end_leash_is_measured_across_the_ground` — 17 out: put down on tick 2 |
| §5A: old 15-block straight-line leash (12 + 3 slack, height counted) | FAIL as expected: `cable_end_leash_is_measured_across_the_ground` — 14 out: taut, held, hook 0.0 out on tick 2 |
| redesign: only end-on rails (no side-on stations) | FAIL as expected: `skip_exchanges_side_on_with_acoupled_train` — side-on at the bridle: skip 0, docked car 100, other car 50 on tick 82 |
| M07: bridle station at the wrong height | FAIL as expected: `skip_exchanges_facing_every_way` — north: loaded at the bridle: skip 0, cart 100 on tick 62 (+4 more) |
| M07: no stop at station heights | FAIL as expected: `skip_exchanges_by_height_at_the_loading_rail` — stopped exactly at the top station: 7.199999999999999 on tick 264 |
| M07: near-zero speed reversal | FAIL as expected: `skip_let_go_when_the_route_changes` — Expected the lever after reversing through zero (blocked, skip blocked) to be FEED: was NEUTRAL on tick 57 |
| M07: side cells not checked | FAIL as expected: `skip_needs_its_column_and_side_cells` — Expected skips placed with a block at BlockPos{x=13, y=2, z=5} to be 0: was 1 on tick 0 (+1 more) |
| §10: no station-aware final approach (the original end guard) | FAIL as expected: `feed_docks_on_aterminal_loading_rail` — docked on the Loading Rail, centred: terminal Loading Rail under the last support: cart x 21.950 (block 21), stop 'end', docked false, grip along 2 (+3 more) |
| redesign (client): vanilla 5-box cart body | FAIL as expected (real client): Can't find part gate_end_plus |
| §9 (client): open-route cable texture not moving | FAIL as expected (real client): the cable moves while lifting and stands still when stopped: 10.767635997529663 -> 10.767635997529663, 10.767635997529663 -> 10.767635997529663 |
