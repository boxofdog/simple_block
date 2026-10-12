# Milestone 04 test evidence

What each test logged (`simple_block-evidence` lines), 2026-10-12. Server tests: `CableGameTests` in `./gradlew build` (95/95 passed) and in a filtered run (10/10). Positions in the server lines are absolute coordinates of the test structure, which the runner places far from spawn. Client: `CableClientTest` in `./gradlew runClientGameTest`, which also ran `SurvivalSmokeClientTest` (all its M01-M03 steps passed); the world is a fresh superflat survival world with the ground at y -60.

## Server game tests

```
CAB-01: cable x2, coil, coil -> 8 cable, drum, anchor x2 and connecting rod all craft from their recipes
CAB-02: [under Oak Planks: hung, under Spruce Log: hung, under Stripped Birch Log: hung, under Crimson Stem: hung, under Oak Slab: hung, under Dark Oak Stairs: hung, under Jungle Fence: hung, under Block of Bamboo: hung, under Bamboo Mosaic: hung, under Block{minecraft:stone}: refused, under Block{minecraft:iron_block}: refused, under Block{minecraft:bricks}: refused, under Block{minecraft:glass}: refused, under Block{minecraft:oak_leaves}[distance=7,persistent=false,waterlogged=false]: refused, under Block{minecraft:oak_slab}[type=top,waterlogged=false]: refused, under Block{minecraft:oak_stairs}[facing=north,half=top,shape=straight,waterlogged=false]: refused, plank removed: anchor dropped]
CAB-03: drum wound with 4 coils = 128 blocks (one more refused); loose end hangs at (-2918655.5, 7.02, -1.047160012E7)
CAB-04..07: [drum->B 16.5 blocks: refused, too far, drum->A through stone: refused, drum->A: 8 blocks, A->B straight: 10, B->C turn and climb 2: 8, C->D: 4, D->E needs 6 with 2 left: refused]; laid out 30 of 32, 2 left
CAB-08: anchor kept the first drum's cable; second drum refused; second end not picked up while holding one
CAB-09..11: [one coil: A-D laid (30), E refused with 2 left, second coil: E laid (39 out, 25 left), sneak-use C: 23 blocks wound back, B broken: route back to A, sneak-use drum: rewound 64, then 2 coils out, drum broken with A laid: dropped 2 coils and the drum, A freed]
CAB-12: [7 blocks out: held, 0.25 from the hand, 18 blocks out: slipped back to the drum, holder left: put down]
CAB-13: [saved [[I;-2918487,10,-10471600],[I;-2918481,10,-10471600],[I;-2918475,10,-10471600]] with 13 wound, B missing on load: wound back to A (7 out, 25 wound), C freed]
CAB-14: rod fitted (second refused), cargo unloaded before the rod came off, saved 1b, dropped [1 simple_block:copper_connecting_rod, 1 minecraft:minecart]
CAB-15/16: [40 ticks under the cable from A to C, gripping on every one (reach 2.69-2.69), let go past C; past x 26 on tick 60, in step with the control cart on every tick]
```

## Real client

```
CLIENT CABLE: drum placed: Block{simple_block:cable_drum}[facing=east,wound=false]; coil wound on: 32 blocks, wound barrel true, hooks seen by the client 1, message 'Coil wound on: 32 blocks of cable on the drum'
CLIENT CABLE: anchors under planks [true, true, true]; under stone false; anchors left in hand 1
CLIENT CABLE: picked up ('Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum)'); routed through [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}] (client sees [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}]), 25 blocks laid out, 7 left; last message 'Cable run through the anchor: 8 blocks used, 7 left on the drum'; end held after using the last anchor again: false
CLIENT ROD: fitted ('Connecting rod fitted: it grips a cable the cart passes under'); between the first two anchors the rod grips at 2.6875 (client sees 2.6875); past the last anchor: true, still gripping false
CLIENT CABLE: after save and reopen: route [BlockPos{x=28, y=-58, z=6}, BlockPos{x=36, y=-58, z=6}, BlockPos{x=44, y=-58, z=6}], end hangs at (44.5, -58.25, 6.5) (client (44.5, -58.25, 6.5))
CLIENT CABLE: sneak-use second anchor: route [BlockPos{x=28, y=-58, z=6}] ('Cable disconnected here: 16 blocks wound back onto the drum'); sneak-use drum: 'Cable wound back in: 32 blocks on the drum', then 'Cable taken off the drum: 32 blocks'; drum has cable false, wound false, coils in inventory 1
```

Screenshots (`screenshots/`), in the order taken:

| File | Shows |
|---|---|
| `cable-drum-placed.png` | The drum placed with the use key, bare barrel (template texture) |
| `cable-drum-wound-hook.png` | After a coil: wound barrel, the loose end at its foot, the action-bar message |
| `cable-anchors-hung.png` | An anchor under a plank; nothing under the stone block to the right |
| `cable-end-in-hand.png` | The end picked up: the loose cable runs from the drum to the hand |
| `cable-end-dragged-to-last-anchor.png` | After routing through three anchors, at the last one |
| `cable-route-laid.png` | The route from the side: spans over the powered track, through the anchors |
| `cable-route-from-the-drum.png` | The same route from behind the drum |
| `minecart-rod-fitted.png` | A rod fitted to a minecart, folded down with the head open |
| `minecart-rod-gripping.png` | The cart frozen between the first two anchors: the rod stretched up to the cable, head closed |
| `minecart-rod-gripping-close.png` | The same, close up |
| `cable-route-after-reload.png` | After saving, quitting and reopening: route and loose end back |
| `cable-disconnected-at-second-anchor.png` | Sneak-use of the second anchor: only the first span is left |
| `cable-dismantled.png` | Sneak-use of the drum twice: rewound, then taken off as a coil |
