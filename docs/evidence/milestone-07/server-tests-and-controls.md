## Server tests (SkipGameTests, 8) all PASS (11:00)
SKIP-01 owner's layout makes 1 ore_skip; 9 wrong layouts make nothing
SKIP-02 refused with a block at side cell (13,2,5), side cell bridle level (11,4,5), bridle cell (12,4,5); placed with front/back blocks: box 1x4x1, facing north, side cells along x
SKIP-03 hand: 576 offered, 512 taken; coal/sticks refused; screen buttons: L skip 448 cursor 64; L 512/0; R 480/32; R 481/31; Shift 448/31 inv 33 (heavy cap); quick-move back to 512; cart 256; saved/loaded; broken: 8x64 raw iron + 1 skip
SKIP-04 hung at 1.0, one main cable 3 points, no loose hook; crank pull 40t 1.0->2.44; crank feed 20t ->1.12; winch PULL stop at head y 7.45 apex 11.45 under roller 11.75 (end); sneak refused while hanging; next click FEED (PULL skipped); side block stop at 4.000 (blocked); coal burnt 30 ticks only while moving; floor (blocked); unhung on floor; hook back
SKIP-05 head anchor disconnected while lifted: let go, falls to floor exactly, cargo 300 coal kept, coil kept, route 1
SKIP-06 PULL stopped exactly y 6 (docked), cart 200->88 skip 400->512 via ramp (lid open, discharge gate never); FEED down, skip 512->256 into empty bottom cart (front shut, discharge gate open); total 600
SKIP-07 80 ticks: empty skip/loaded cart at chute 0/100; full skip/empty cart at bridle 512/0; body 0/100; rail facing away 0/100; coal at iron skip 10/100
SKIP-08 released after 24 of 256: skip 24 + cart 232; partial: cart 200->256, skip 300->244
## Negative controls (each restored)
lateral side-cell check removed: SKIP-02 FAIL (placed with block in side cell), SKIP-04 FAIL (went past side block to 1.0)
height rule changed (LOAD at +1): SKIP-07 FAIL "cart beside the body: skip 100, cart 0"
stack caps removed (withdraw/allowed/menu): SKIP-03 FAIL "button 1: skip 448, cursor 64"
station stop disabled: SKIP-06 FAIL "stopped exactly at the top station: 7.45"
reversal near-zero speed (skip treated a 1e-9 drive speed during PULL->FEED reversal as a stop): found by the real client test; fixed (|dy| > 1e-6); SKIP-05 regression (3-tick PULL then FEED); negative control (dy != 0.0): SKIP-05 FAIL "Expected the lever after reversing through zero (blocked, skip blocked) to be FEED: was NEUTRAL"
## Client SkipClientTest PASS (11:16)
CLIENT SKIP: placed by key facing north; 64 raw iron loaded by hand 'Raw Iron 512 / 512'; screen shows 512; clicks: left skip 448 cursor 64; left again 512/0; shift 448, inventory 64; totals 512
CLIENT SKIP: cable routed by hand through 2 anchors; end used on the skip: 'Skip hung on the cable...', hung true, client draws its cable true
CLIENT SKIP UNLOADED: PULL, 400 blocks away: left the loaded world after 2 ticks at y 0.06 (not entity-ticking); back after 100 ticks y 0.06; lever NEUTRAL (idle); coal burn 6397 -> 6397
CLIENT SKIP STATION: PULL stopped at y 5.00 (docked); client saw ramp down and lid open; skip 300->500, top cart 200->0
CLIENT SKIP STATION: FEED down to y 0.00; client saw discharge gate open, cart front shut; skip 500->244, bottom cart 0->256
CLIENT SKIP REOPEN: Snapshot y 0, count 244, hung, line hoist, burn 6249 identical after save/reopen; PULL 40 ticks 0.00 -> 2.20
