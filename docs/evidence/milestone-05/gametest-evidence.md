# Milestone 05 test evidence

What each test logged (`simple_block-evidence` lines), 2026-10-12, on the final code (`16b1098`). Server: `./gradlew build`, **114/114** required game tests passed (95 from M01-M04, 11 `CableLoopGameTests`, 8 `TrainGameTests`). Positions in server lines are relative to the 30 x 10 x 10 `cable_yard` test structure unless they are absolute. Client: `./gradlew runClientGameTest` in a fresh superflat survival world (ground y -60), all three client tests passed: `SurvivalSmokeClientTest` (M01-M03), `CableClientTest` (M04, plus the BUG-001 close-ups) and `HaulageClientTest` (M05).

## Server game tests (M05)

```
COUP-01: [10 blocks apart: refused; 1.6 apart: coupled with one chain, shears: uncoupled, chain dropped, both ends taken: refused, middle cart broken: 2 chains dropped, partners free]
CRANK-01: [coil in hand, right-click on the crank: crank only; sneak on the crank: no dismantling; open route: locked, drum body sneak-use still dismantles; empty drum spins at 0.06]
CRANK-02: hanging drum placed under a plank, cable exit at 11.0/16, coil wound on, cranked; plank removed: drum and coil dropped
ENG-01: [loaded cart and cushioned cart: refused, furnace kept, empty cart: became an engine in place, coupling kept, broken: [1 minecraft:iron_chain, 1 minecraft:minecart, 1 minecraft:furnace], exploded: [1 minecraft:minecart, 1 minecraft:furnace]]
ENG-02: furnace_minecart recipe and advancement absent; furnace over minecart crafts nothing
ENG-03: [no fuel: won't start; oak log: refused; coal and charcoal: 7200 ticks of fuel, ran east x 10.50 -> 16.31 in 40 ticks; stopped at x 17.41; no fuel burnt while stopped, started from the east: ran west x 17.41 -> 11.60]
ENG-04: [forward looking east: x 10.50 -> 14.58; looking west: -> 9.62; back: stopped at 8.52 and held]
ENG-05: [east 80 ticks: freight 8.00 -> 16.48, rod cart 12.80 -> 21.28; west 80 ticks: freight -> 8.99, rod cart -> 13.79]
ENG-06: [20 ticks on the level: alone 1.81 blocks, with 3 loaded carts 0.26; climb: alone reached x 23.28 (top 20), loaded stalled at x 15.27 and held]
ENG-07: engine saved and loaded: fuel 5000, running true, heading (-1.0, 0.0, 0.0), speed 0.125; couplings kept on both carts
LOOP-01/02: [closed: 23 laid out, return 26 (41 -> 15 on the drum), opened by sneak-use: route kept, 41 on the drum, wheel anchor back, closed again, wheel anchor broken: loop open, route back to the second anchor, 2 anchors dropped]
LOOP-03: [short of cable: refused with 9 on the drum, anchor kept, stone in the return path: refused, route turning at the first anchor: refused]
LOOP-04..06: [start x 8.50, hauled to x 22.00 (wheel at 23.5), held, back to x 4.24 (drum at 1.5), held]
LOOP-07: [ticks per block: level 17.7, empty climb 44.0, loaded climb (256 cobblestone) 136.0]
LOOP-08: anchor broken while hauling at x 13.24: loop opened, cart stopped (moved 0.000 in 60 ticks), cable kept
LOOP-09: loop saved and loaded: closed, 26 return, 23 laid out, 15 wound, no loose end
LOOP-10: 36 spans indexed, none on the return strand; a rod under the return strand grips the working strand
LOOP-11: coupled train (empty, rod cart, 128 raw copper): out x 6.90/8.50/10.10 -> 16.62/18.22/19.82, held, back -> 6.90/8.50/10.10
LOOP-12: looped drum broken: 64 blocks of cable dropped (2 coils, 0 cables), wheel anchor dropped, anchors free
```

## Real client

```
CLIENT BED: message 'No amount of rest can pass this night', clock 18008 -> 18170 after 160 ticks in bed, still in bed: true, respawn BlockPos{x=0, y=-60, z=3}
CLIENT SHULKER: placed: false, still holding: 1, message 'Shulker boxes don't work in this world'
CLIENT MOVEMENT: walked 4.06 blocks in 20 ticks, jump height 1.25, health after 10-block fall 13.0
CLIENT ELYTRA: wearing elytra, pressed jump 3 times mid-air, gliding at any point: false
CLIENT INVENTORY: 27 usable inventory slots on screen; heavy load client 9720 / server 9720 (units of 1/216 ingot), by material (iron/gold/copper/diamond/netherite) client [6480, 1296, 864, 648, 432] / server [6480, 1296, 864, 648, 432]; load bar tooltip with 4 gold nuggets added, then after they were taken away again (still hovering): [Heavy load: 45.45 / 64, ■ Iron: 30, ■ Copper: 4, ■ Gold: 6.44, ■ Diamond: 3, ■ Netherite: 2, |, Heavy load: 45 / 64, ■ Iron: 30, ■ Copper: 4, ■ Gold: 6, ■ Diamond: 3, ■ Netherite: 2]; after Sort (top row first): [dirtx5, cobblestonex64, cobblestonex16, applex3, iron_ingotx30, copper_ingotx4, gold_ingotx6, netherite_ingotx2, diamond_pickaxex1, -, -, -, -, -, -, -, -, -]; hotbar torch kept: true; after /give 30 more iron: client 64 / 64 / server 64 / 64
CLIENT LIGHT: client block light at a torch 12, at a lantern 15
CLIENT CREEPER: creeper in the noon sun started smoking: true; afterwards creepers on server 0 / client 0, item drops 0, player health 20.0 -> 20.0
CLIENT FOOD: saturation server/client after eating a steak 12.8 / 12.8, after 4.5 exhaustion 11.8 / 11.8, after regeneration 8.8 / 8.8 (health 14 -> 16.0); sprinted 11.19 blocks for 0.5440 exhaustion = 0.0486 per block (vanilla 0.1)
CLIENT MINECART: after three right-clicks with 64 raw iron: server [192 minecraft:raw_iron], client [192 minecraft:raw_iron], raw iron in hand 0, message 'Raw Iron 192 / 256'
CLIENT MINECART: riding after use: false; sneak-use unloaded 64 raw iron, cart now [128 minecraft:raw_iron], message 'Raw Iron 128 / 256'
CLIENT MINECART: after save, quit and reopen: server [128 minecraft:raw_iron], client [128 minecraft:raw_iron]
CLIENT MINECART: broken by hand: carts left [], raw iron on the ground 128 + picked up 0, minecart items 1 + picked up 0
CLIENT CUSHION: fitted server [red] / client [red], message 'Cushion fitted: this cart carries a rider now, not cargo'; riding client true / server true; after sneaking riding: false; sneak-click: cushion server [null] / client [null], red cushions back 1, message 'Cushion taken out: this cart carries cargo again'
CLIENT STORAGE: ender chest placed: false, message 'Ender chests don't work in this world'; chest minecart placed: false, still held: 1, message 'Chest minecarts don't work in this world: load a minecart instead'
CLIENT CABLE: drum placed: Block{simple_block:cable_drum}[facing=east,hanging=false,wound=false]; coil wound on: 32 blocks, wound barrel true, hooks seen by the client 1, message 'Coil wound on: 32 blocks of cable on the drum'
CLIENT CABLE: anchors under planks [true, true, true]; under stone false; anchors left in hand 1
CLIENT CABLE: picked up ('Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum)'); routed through [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}] (client sees [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}]), 25 blocks laid out, 7 left; last message 'Cable run through the anchor: 8 blocks used, 7 left on the drum'; end held after using the last anchor again: false
CLIENT ROD: fitted ('Connecting rod fitted: it grips a cable the cart passes under'); between the first two anchors the rod grips at 2.6875 (client sees 2.6875); past the last anchor: true, still gripping false
CLIENT CABLE: after save and reopen: route [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}], end hangs at (44.5, -58.25, 6.5) (client (44.5, -58.25, 6.5))
CLIENT CABLE: sneak-use second anchor: route [BlockPos{x=28, y=-58, z=6}] ('Cable disconnected here: 16 blocks wound back onto the drum'); sneak-use drum: 'Cable wound back in: 32 blocks on the drum', then 'Cable taken off the drum: 32 blocks'; drum has cable false, wound false, coils in inventory 1
CLIENT LOOP: anchor used on the last anchor: 'Loop closed: 28 blocks of return cable'; looped true (client true), 28 blocks of return cable
CLIENT CRANK: left-click held on the crank 160 ticks: cart x 25.50 -> 35.46 (client spool speed 0.06, travel 0.0 -> 3.4199977), drum still there true; let go: x 35.46; right-click held 120 ticks: back to x 28.14; still looped true, route 3 anchors
CLIENT CRANK: drum placed against a plank's underside: Block{simple_block:cable_drum}[facing=west,hanging=true,wound=false]
CLIENT ENGINE: furnace on a minecart: engine (client sees it true); coal: 3600 ticks of fuel; chain: coupled true (1 chain left, client sees it true); empty-hand use from the west: running true, x 30.50 -> 31.15 in 15 ticks; used again: stopped at x 31.68 (speed 0.0, running false)
CLIENT ENGINE: in the cushioned cart, forward looking west 40 ticks: engine x 31.68 -> 27.40; back 25 ticks: speed 0.0, x 26.10; 20 ticks later x 26.10
CLIENT UNLOAD: yard chunks unloaded after 20 ticks away; before Snapshot[looped=true, returnLength=28, stored=11, carts=2, engines=1, rodCartX=28.1399999409914, gripping=true, engineX=26.095880952380952, seatX=27.732609977239107, fuel=3541, coupled=true]; after Snapshot[looped=true, returnLength=28, stored=11, carts=2, engines=1, rodCartX=28.1399999409914, gripping=true, engineX=26.095880952380952, seatX=27.732609977239107, fuel=3541, coupled=true]
CLIENT REOPEN: after save and reopen Snapshot[looped=true, returnLength=28, stored=11, carts=2, engines=1, rodCartX=28.1399999409914, gripping=true, engineX=26.095880952380952, seatX=27.732609977239107, fuel=3541, coupled=true]; cranked 60 ticks: rod cart x 28.14 -> 31.86
```

Run totals: `All 114 required tests passed :)` (build); client `BUILD SUCCESSFUL` with all three client tests.
