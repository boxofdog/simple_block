## REC-01..04
PASS: [3 nuggets middle: 2 cable; left/right: nothing; old nuggets+string: nothing; middle+string: nothing; 3 cables middle: coil; left col/top row/8-ring: nothing; coil -> 3 cable]
Negative control (CraftingMenu/Crafter/CrafterMenu mixins removed): FAIL "3 nuggets, left column should make nothing, made 2 simple_block:cable".
## LONG negative control
M05 gate (drum + every anchor entity-ticking) re-added in CableDrive.driveCart: long_line_works_where_it_is_loaded FAIL "fed east with the drum unloaded: x 8.5 -> 8.5 on tick 385".
## LT-01..04 PASS (05:42)
LT-01: rail + anchor: 1 loading_track; apart: 1; powered rail + anchor: nothing
LT-02: from the west and from the east: docked at x 15.5, front EAST both; cushioned cart at x 20.1 and engine at x 19.1 passed, dock and front saved and loaded
LT-03: docked: lead x 16.50 front west, engine off; 40 ticks docked: all three cars held, engine refused to start, no fuel burnt; released: nothing moved in 40 ticks; restarted: whole train passed the track (engine x 18.1) without docking; coming back west: engine passed over, first freight car (rear) docked, front west
LT-04: track replaced by plain rail: undocked; rolled east front trailing, round the curve south: front NORTH (still trailing)
Negative control (dock disabled): LT-02 "both freight carts docked", LT-03 "lead freight cart docked", LT-04 "docked" all FAIL.
## WIN-01..05 PASS (05:49)
WIN-01: furnace fitted on a routed drum: route of 3 anchors, 23 blocks and the coil kept; open route: lever stays NEUTRAL; lever: [NEUTRAL, PULL, FEED, NEUTRAL]; coal: 2 in, one per click; charcoal refused; empty hand on the body: reports, lever stays PULL; left-click does nothing; broken: 1 furnace, 2 coal, 1 coil
WIN-02: PULL: x 12.50 -> 9.35 in 30 ticks; FEED straight after: speed changed at most 0.010 a tick; FEED to x 21.95, lever NEUTRAL (end); top speed 0.150 (crank 0.060); burnt 144 ticks for 144 ticks moving; 60 ticks later: held, still NEUTRAL, burn unchanged at 1456
WIN-03: FEED onto loading track: docked at x 17.50, lever NEUTRAL (docked), burn 1549; 40 docked + 40 released: no movement, no burn, lever NEUTRAL; PULL: left the track to x 12.85 without docking again; rails ending at x 13: FEED stopped at x 12.70 on the rails, lever NEUTRAL (track_end)
WIN-04: hauled into a docked cart: stopped 0.98 blocks behind it, lever NEUTRAL (blocked) after 10 ticks still, 0 ticks burnt meanwhile
WIN-05: running FEED: 2 coal and 1565 of 1600 ticks left; saved and loaded (level lines codec + drum BE): 2 coal, 1565 ticks, lever NEUTRAL
Negative control (burn whenever lever runs): WIN-04 FAIL "burnt 9 ticks while blocked".
Bugs found by tests and fixed: drum BE backup burn lagged one tick (now saves line's exact values); stall detection used intra-tick motion (now progress along strand tick-to-tick).
## RET-01..03, PUSH-01 PASS (05:56-05:57)
RET-01: 4059 tab entries + search checked: none of the 59 retired items shown (elytra, 17 shulker boxes, ender_chest, chest_minecart, furnace_minecart, powered/detector/activator rail, 35 granite/diorite/andesite/tuff family items), all still registered; kept visible: rail, loading_track, furnace, minecart, chest, blast_furnace, golden_chestplate, chainmail_chestplate, stone, redstone, coal
RET-02: powered/detector/activator rail: no recipe; powered rail pattern: nothing; rail pattern: 16 rail
RET-03: 500 abandoned mineshaft chests: 3075 stacks, 585 of rails, 0 redstone rails
PUSH-01: 20 ticks shoving: freight 0.000, engine 0.000, cushioned 7.299 blocks; loaded freight not moved; freight cart on slope rolled from x 22.5 to 26.5
Negative controls (pushing mixins removed, RetiredItems unregistered, rail recipe overrides removed): RET-01 FAIL (granite etc. in Building Blocks), RET-02 FAIL "powered_rail recipe still loaded", RET-03 FAIL 463 redstone rail stacks, PUSH-01 FAIL "freight cart shoved by 7.16".
Fixture adaptation: CAB-15/16 and FRT-01 T03 now use test-only propulsion instead of powered rails.
## Former finite-capacity negative control
128-block drum cap restored in CableRoutes routing: long_line_works_where_it_is_loaded FAIL "Expected anchors routed on one coil to be 39: was 13".
## Dedicated server boot
runServer: "Cable: one coil per drum, spans of up to 12 blocks", "Done (0.412s)!"
## Repeated full builds
After the ENG-01 isolation fix and the long-line wait hardening: 3 consecutive `./gradlew build` runs, 129/129 each. Before them, of 5 runs, one LONG-01 timeout and one ENG-01 cross-test count failure (the reasons for the two fixes).
## Long-line timeout cause
Step logging in a full build: the wait for the forced section to become entity-ticking spent ticks 100..1350 within one wall-clock second (the test server ticks without pausing), so the 1,600-tick budget ran out while chunks were still loading asynchronously. Budget raised to 20,000 ticks; line moved to z -2000 from the origin and taken down at the end. Then: full builds 129/129, 129/129, and one failure of the pre-existing statistical M02 test lightBehindWallsStillAttracts.
