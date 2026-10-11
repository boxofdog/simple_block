# Milestone 02 game-test evidence (checkpoint build, 2026-10-11)

`./gradlew build` at the checkpoint: 48 required server game tests passed. Evidence lines logged by the tests:

```
ELY-01/SHU-02 audit: 1447 loot tables and 471 villager trades scanned, problems: []
GEO-04 recipes: 1942 recipes scanned, 0 make a removed rock []
GEO-01 substitution: 2729 block states checked, problems 0; transition y1 87% deepslate, y7 12% deepslate
MINE-01 creative: empty-handed creative player removed deepslate diamond ore instantly
MINE-01/02: 9 gated blocks x 6 weak tools blocked, x 3 iron+ pickaxes at vanilla speed; problems []
MINE-03: 19 crafted deepslate blocks mined by a wooden pickaxe at vanilla speed with drops; problems []
MINE-02: 11 stone-depth ore/tool pairs keep vanilla tiers and speed; problems []
SHU-01/02: 17 recipe ids and advancements absent, 272 dye combinations rejected, 1942 recipes scanned, none yield a shulker box
GEO-04 trades: mason level 3 = [minecraft:mason/3/emerald_dripstone_block]; 452 offerable trades scanned, 0 involve removed rocks
GEO-04 loot: 2 chest tables list a removed rock; 800 rolls gave 2328 stacks, 0 of them removed rocks
ORE-01 wiring: 56 Overworld biomes; deep-ocean iron in 4, banded iron in 10, placer iron in 6; problems []
INV-01 usable player slots per menu: {chest=27, furnace=27, inventory=27}
INV-01 pickups: 30 swords offered, 27 picked up, disabled row empty
INV-01 clicks: cursor placement and shift-click both refused by the disabled row
INV-03 hotbar after shift-clicking shovel, axe, hoe, cobblestone from a chest: [iron_shovel, wooden_axe, stone_hoe, -, -, -, -, -, -]; slot 30 cobblestone 30
INV-02 sorted storage (bottom row first): [27:dirtx5, 28:cobblestonex5(named), 29:cobblestonex64, 30:cobblestonex16, 31:applex3, 32:diamond_pickaxex1(damaged), 33:diamond_pickaxex1, 34:-, 35:-, 18:-, 19:-, 20:-, 21:-, 22:-, 23:-, 24:-, 25:-, 26:-]
ORE-02 wiring: all 4 vanilla diamond placements carry the ore.diamond.keep_chance filter
HVY-04 weight table: 272 items weigh something; 31 design values checked; problems []
HVY-01/02: at 64 iron or 32 iron + 32 gold, iron/copper/raw gold pickups refused; 64 cobblestone still picked up
HVY-03/04: 63+block refused, 63+10 nuggets took 9, 55+block filled to exactly 64, 61+16 rails took 8; carried 64 / 64
HVY-05 recipe audit: 1620 recipes checked, 0 make more metal than their cheapest inputs []; not checkable from ingredients: [black_banner_duplicate, blue_banner_duplicate, bolt_armor_trim_smithing_template_smithing_trim, book_cloning, brewing/lingering_potion_awkward_blaze_powder, brewing/lingering_potion_awkward_breeze_rod, brewing/lingering_potion_awkward_cobweb, brewing/lingering_potion_awkward_ghast_tear, brewing/lingering_potion_awkward_glistering_melon_slice, brewing/lingering_potion_awkward_golden_carrot, brewing/lingering_potion_awkward_magma_cream, brewing/lingering_potion_awkward_pha
HVY-05 crafting at the cap: 9 ingots -> iron block in a table, 1 ingot -> 9 nuggets in the 2x2 grid; load stays 64
HVY-06: offhand and cursor counted; worn chestplate exempt, but shift-click unequip and equip-swap into overload refused
HVY-07: chest shift-click moved 24 of 64; number-key swap undone; furnace output gave 4 of 20; crafting result refused at 60 with ingredients kept; closing the table returned 4 ingots and dropped 5
HVY-06/07: bundle contents counted; creative ignores the cap; survival keeps a creative overload and blocks intake until under 64
HVY-09: movement speed 0.10000000149011612 empty and at 64/64
INV-01 eviction: 5 diamonds moved to usable slots; with a full inventory 7 emeralds dropped, none lost
MINE-01 server break: stone pickaxe 100 ticks -> deepslate refused and still present; next early-reported stone break still completes; iron pickaxe -> deepslate broken, cobbled deepslate dropped
PHA-02: natural spawner produced 564 monsters: [entity.minecraft.creeper, entity.minecraft.enderman, entity.minecraft.husk, entity.minecraft.parched, entity.minecraft.skeleton, entity.minecraft.spider, entity.minecraft.zombie] (skyDarken=11)
BED-01..03: 2 players slept 140 ticks at midnight; clock 18003 -> 18143, still sleeping: true
BED-04: slept 140 ticks in a thunderstorm; clock 7945 -> 8085, thundering: true, still sleeping: true
ELY-01: End ship placed (557 purpur blocks), item frames: 0, frames with elytra: 0, shulker sentries: 3
BED-04: slept 140 ticks in a thunderstorm; clock 24810 -> 24950, thundering: true, still sleeping: true
PHA-01: 96000 insomnia spawner ticks at midnight on hard, player sleepless for 10 days: 0 phantoms
```

Real-client test (`runClientGameTest`, Xvfb + Vulkan), 02B step:

```
CLIENT INVENTORY: 27 usable inventory slots on screen; heavy load client 8640 / server 8640 (units of 1/216 ingot); after Sort (bottom row first): [dirtx5, cobblestonex64, cobblestonex16, applex3, iron_ingotx40, diamond_pickaxex1, -, -, -, -, -, -, -, -, -, -, -, -]; hotbar torch kept: true; after /give 30 more iron: client 64 / 64 / server 64 / 64
```

Screenshots: `inventory-and-weight/`.

### INV-04 follow-up: diamond and netherite weight, per-material load bar

Diamonds and netherite now count, and the "Heavy x / 64" text became a bar filled per material. `HeavyLoadGameTests` (10 tests) pass:

```
HVY-04 weight table: 290 items weigh something; 42 design values and 6 materials checked; problems []
HVY-10 materials: 10 iron + 5 copper + 4 gold nuggets + diamond pickaxe + netherite chestplate in a bundle = [2160, 96, 1080, 648, 1728] (units per iron/gold/copper/diamond/netherite, 1/216 ingot), total 5712; at 64 iron picked up 0 of 3 diamonds and 0 netherite ingots; at 61 iron a diamond pickaxe (3) fits: 1
HVY-05 recipe audit: 1620 recipes checked, 0 make more metal than their cheapest inputs []
```

The HVY-05 audit includes the netherite smithing upgrades: a diamond item (its diamonds) plus a netherite ingot (1) gives netherite gear weighing what the diamond item did.

Real-client test, inventory step:

```
CLIENT INVENTORY: 27 usable inventory slots on screen; heavy load client 9720 / server 9720 (units of 1/216 ingot), by material (iron/gold/copper/diamond/netherite) client [6480, 1296, 864, 648, 432] / server [6480, 1296, 864, 648, 432]; after Sort (bottom row first): [dirtx5, cobblestonex64, cobblestonex16, applex3, iron_ingotx30, copper_ingotx4, gold_ingotx6, netherite_ingotx2, diamond_pickaxex1, -, ...]; hotbar torch kept: true; after /give 30 more iron: client 64 / 64 / server 64 / 64
```

Screenshots in `inventory-and-weight/`:
- `0004_inventory-load-bar-mixed.png`: 30 iron, 4 copper, 6 gold, a diamond pickaxe and 2 netherite ingots.
- `0006_inventory-heavy-full.png`: full, with the red frame.
- `food-and-hud/inventory-gui-scale-3.png`: an empty bar at GUI scale 3.
- `inventory-load-bar-tooltip.png`: the tooltip when hovering the bar with the real mouse.

Tooltip check in the client test: with 4 gold nuggets added, the tooltip read `[Heavy load: 45.45 / 64, ■ Iron: 30, ■ Copper: 4, ■ Gold: 6.44, ■ Diamond: 3, ■ Netherite: 2]`. After the nuggets were taken away while the mouse stayed on the bar, it read `[Heavy load: 45 / 64, …, ■ Gold: 6, …]`. The total is rounded up, so it never shows room that is not there; each material is rounded to 0.01.

## 02C (light and hostile mobs)

`./gradlew build` after 02C: **61 required server game tests passed** (the 48 above plus 13 in `MobGameTests`). Evidence lines:

```
LIGHT-01: torch block light 12 (block above 11), lantern 15; Overworld monster spawn block-light limit 0; monster spawn check passed 0 of 50 times beside the torch (block light 10, sky light 0) and 50 of 50 in an unlit closed box; emission problems []
MOB-01 open ground, 24 zombies, night: share of zombie-samples on lit ground / on the dark mirror area: control (attraction off) 34% / 32%, attraction on 83% / 2% (2160 samples each); at the end 22 zombies on lit ground on 22 different blocks
MOB-01 walled: two closed boxes, one lit inside, 24 zombies; share of zombie-samples round the lit box / round the dark box: control 11% / 20%, attraction on 43% / 10% (2160 samples); any zombie inside a box at the end: false
MOB-04 daytime, 16 husks: share of husk-samples on lit ground / on the dark mirror area 27% / 20% (960 samples)
MOB-05: 4 entranced zombies spent 100% of 300 ticks on lit ground; 3 targeted the survival player by tick 313 and closed from 25.8 to 20.6 blocks on average in 60 ticks; all dropped the target by tick 373 when the player went creative; afterwards 100% of samples (ticks 300-1200 after) on lit ground
MOB-07: 40 zombies among torches at night; 400 ticks took 661 ms with the attraction off and 621 ms with it on (1.65 vs 1.55 ms per tick); 92 wander decisions with the attraction cost 4.72 ms in total, 51.3 us each
ZOMB-01 oak_door on normal: door shut in the chasing zombie's face at tick 5, zombie started breaking at tick 61, door broken at tick 300 (239 ticks of breaking; vanilla Hard 240)
ZOMB-01 crimson_door on normal: door shut in the chasing zombie's face at tick 16, zombie started breaking at tick 60, door broken at tick 299 (239 ticks of breaking; vanilla Hard 240)
ZOMB-02 metal doors: pursuing zombie spent 202 ticks attacking the iron door and 260 ticks the copper door (of 300 each); both doors and the blocks round them unchanged
ZOMB-03 unaware: 4 zombies outside a closed oak door with a torch-lit room behind it, 900 ticks at night: door intact, door-breaking goal ran 0 ticks, 17% of zombie-samples within 4 blocks of the door
CREEP-01 sun: creeper started smoking at tick 39, gone at tick 48 (9 ticks of effect, 0.45 s); drops 0, floor intact true, removal reason DISCARDED, swell -1
CREEP-02 shade: 600 ticks at noon, creeper under a roof still there true, creeper in a torch-lit closed room still there true, any smoking false
CREEP-02 night: 600 ticks in the open at midnight, creeper still there true, smoking false
```

How the mob experiments work (`MobGameTests`):

- Each test builds its own walled arena 512 blocks north of the test grid, in a region no other test uses. Arenas next to the grid overlapped the neighbouring test positions. The runner unforces every forced chunk at the end of each batch, and an arena built on chunks that were still unloading never got its light computed.
- Mobs are spawned only once the light engine and the arena's entity sections are ready (`Arena.whenLit`). Game-test ticks take about 1 ms, while light and entity loading run on other threads.
- MOB-01 compares two phases with the same zombies on the same start line midway between the lit area and its dark mirror image: attraction off (vanilla wandering, the control), then on.
  - "Lit ground" is the attraction's own definition: block light above 0. Torch light reaches about 11 blocks past the torches.
  - Presence is sampled every 10 ticks after the first 300 ticks of each 1200-tick phase.

Follow-up (commit `9c5a486`, after CI run 7 failed on a night-timing flake):
- The walled experiment now uses 32 zombies and 2400-tick phases. With the mod it measured 40–58% round the lit box vs 4–8% (6 runs plus the 63-test build); without the mixins it failed every time (3 runs).
- The open-ground test asserts a lit-minus-dark gap (> 30 points and 25 more than the control) instead of a 4× ratio.
- Build of 02D: open ground 89% / 6% (control 35% / 24%); walled 58% / 6% (control 13% / 16%).

Stability: the 13 mob tests were run repeatedly while the experiments were tuned. The last five runs all passed: two before and two after the final pursuit-test change (at least three of four zombies need to notice the player), plus the full build.
- Open ground: lit ground 67–83% vs dark 0–6% (control 24–39% / 21–32%).
- Walled boxes: 43–59% round the lit box vs 2–10% round the dark one.

Real-client test, 02C steps (`runClientGameTest`):

```
CLIENT LIGHT: client block light at a torch 12, at a lantern 15
CLIENT CREEPER: creeper in the noon sun started smoking: true; afterwards creepers on server 0 / client 0, item drops 0, player health 20.0 -> 20.0
```

Screenshots in `mobs-and-light/`:
- `creeper-smoking-in-sunlight.png`: smoke and ash on the creeper, three ticks into the effect.
- `creeper-gone.png`: the final poof, about one second later, with the creeper gone.

The creeper has no AI so it stands still in front of the camera; evaporation does not depend on the AI.

### MOB-01 refinement (after the Milestone 02 report)

What changed in the audit:
- **Detection:** a 20-block sphere instead of a ±6-block slab, with 48 samples per wander decision instead of 24.
- **No brightness bias:** remembered lit spots are a random sample; previously the memory drifted towards brighter blocks.
- **Off lit ground:** mobs head for the nearest lit area (any remembered spot within 4 blocks of the nearest).
- **On lit ground:** mobs wander like vanilla but prefer lit destinations.
- **When:** active at Overworld night, or wherever the mob gets no sky light.

Three consecutive runs of `MobGameTests` (14 tests) all passed:

```
MOB-01 open ground, 24 zombies, night: control (attraction off) 24% / 29%, attraction on 75% / 7%; ... of the samples on lit ground with the attraction on, 15% were within 1.5 blocks of a torch (those blocks are about 10% of the lit ground)
MOB-01 underground at noon (roofed arena, sky light 0), 24 zombies: control (attraction off) 25% / 44%, attraction on 74% / 10%; overworld clock 6000, attraction active under the roof: true
MOB-01 walled: two closed boxes, one lit inside, 32 zombies; control 13% / 17%, attraction on 36% / 10%; any zombie inside a box at the end: false
MOB-04 daytime on the surface, 16 husks: 59 wander decisions, 0 of them steered towards light; attraction active under the open sky at noon: false
MOB-05: ... afterwards 100% of samples (ticks 300-1200 after) on lit ground
```

Ranges over the 4 runs on the final code:

| Measure | Range |
|---|---|
| Open ground, lit / dark presence | 56–75% / 5–15% |
| Time near a torch | 5–16% |
| Underground at noon, lit / dark | 59–74% / 8–17% |
| Walled, round lit box / round dark box | 36–44% / 5–10% |
| Steered decisions at surface noon | 0 every run |

## 02D (hunger and saturation display)

`./gradlew build` after 02D: **63 required server game tests passed** (adds 2 in `FoodGameTests`). Evidence lines (exhaustion added; vanilla 26.3 value; ratio):

```
FOOD-01 activities: sprint 10 blocks 0.5000 (vanilla 1.0000, x0.50); walk 10 blocks 0.0000 (vanilla 0.0000, x0.00); swim 10 blocks 0.0500 (vanilla 0.1000, x0.50); 10 jumps 0.2500 (vanilla 0.5000, x0.50); 10 sprint-jumps 1.0000 (vanilla 2.0000, x0.50); break 10 dirt 0.0250 (vanilla 0.0500, x0.50)
FOOD-01 activities: walk 10 blocks under water 0.0500 (vanilla 0.1000, x0.50)
FOOD-02 unchanged costs: attack a zombie 0.1000 (vanilla 0.1000, x1.00); take a zombie hit 0.1000 (vanilla 0.1000, x1.00); regenerate from saturation (10 food ticks) 3.0000 (vanilla 3.0000, x1.00); regenerate from food (80 food ticks) 6.0000 (vanilla 6.0000, x1.00); Hunger effect, 20 ticks 0.1000 (vanilla 0.1000, x1.00)
```

The activities run through the methods vanilla itself calls:
- `ServerPlayer.checkMovementStatistics`, which handles a player's movement packets.
- `ServerPlayer.jumpFromGround`.
- `ServerPlayerGameMode.destroyBlock`.
- `Player.attack` and `hurtServer`.
- `FoodData.tick`.
- The Hunger effect's tick.

Real-client test, 02D step:

```
CLIENT FOOD: saturation server/client after eating a steak 12.8 / 12.8, after 4.5 exhaustion 11.8 / 11.8, after regeneration 8.8 / 8.8 (health 14 -> 16.0); sprinted 11.19 blocks for 0.5440 exhaustion = 0.0486 per block (vanilla 0.1)
```

- The exertion step changes saturation without changing food or health, which vanilla does not sync to the client. The client still matched the server, through `SaturationPayload`.
- The sprint was real keyboard input in the real client, charged by the server from the movement packets: 0.049 exhaustion per block against vanilla's 0.1 (the first ticks of acceleration are not yet sprinting).

Screenshots in `food-and-hud/`:
- `hud-saturation-after-eating.png`: saturation 12.8, so 6 gold, underlined icons over 9 food icons; 854×480, auto scale 2.
- `hud-saturation-gui-scale-1/2/3.png`: saturation 7, so 3.5 icons; 1280×720.
- `inventory-gui-scale-3.png`: the 27-slot inventory at the largest scale.
- `hud-hidden-f1.png`: F1, nothing drawn.
- `hud-creative-no-food-bar.png`: creative, no food bar and no overlay.
