# Milestone 03 game-test evidence

Lines logged by the tests (`simple_block-evidence` logger). Server game tests from the final `./gradlew build` ("All 80 required tests passed"); client lines from `runClientGameTest`.

## Server game tests

```
FRT-02/03: loaded 1+63+64+64+63=255, +64 stopped at 256 with 63 left in hand; raw copper refused while raw iron was in; unloaded 256, then reused for cobblestone; refused: minecraft:iron_block, minecraft:raw_iron_block, minecraft:iron_ingot, minecraft:iron_nugget, minecraft:iron_pickaxe, minecraft:chest, minecraft:shulker_box, minecraft:bucket, minecraft:water_bucket, minecraft:minecart, minecraft:dirt, minecraft:gravel, minecraft:raw_iron (renamed), minecraft:bundle
FRT-04: full inventory + one slot of 60 cobblestone took 4 (cart 100 -> 96), then 0; with 60 ingots carried took 4 raw iron (cart 200 -> 196), then 0 at the cap; at the cap a full 64 cobblestone (weightless) still came out
FRT-05: saved "minecraft:deepslate_iron_ore" -> loaded minecraft:deepslate_iron_ore x180
FRT-07: survival break: 256 raw iron in 4 stacks + 1 empty minecart; explosion: 100 cobblestone + 1 minecart; kill: 50 coal + 0 minecart; creative hit: 30 raw gold + 0 minecart
FRT-04 (T14): two players, 10 loads then 20 unloads in one tick: 328 raw iron before, 328 after loading, 328 after unloading (cart 200, each player 64 = heavy cap)
FRT-06: placed on a rail by item; player and a zombie in its path stayed out (cart rolled 3.2 blocks)
FRT-10/11 recipes: ender chest and chest minecart recipes and advancements absent, both shapes craft nothing, 1940 recipes scanned, none yield either; the minecart still crafts
FRT-10/11 loot: 1447 loot tables scanned, none give an ender chest or chest minecart; silk-touched ender chest drops [8 minecraft:obsidian]
FRT-10: survival and creative placement refused; an existing ender chest stayed shut (menu: net.minecraft.world.inventory.InventoryMenu@46f47447)
FRT-12: donkey: chest equipped by hand, opened, 32 raw iron shift-clicked in, ridden; mule: chest equipped by hand, opened, 32 raw iron shift-clicked in, ridden; llama: chest equipped by hand, opened, 32 raw iron shift-clicked in, ridden
FRT-11: chest minecart refused by hand and by dispenser (kept in both); plain minecart dispensed onto its rail
FRT-11: chest minecart stayed shut; hoppers above/below moved nothing in 60 ticks; broken: [17 minecraft:raw_iron, 28 minecraft:raw_iron, 19 minecraft:raw_iron, 1 minecraft:minecart, 1 minecraft:chest]; mineshaft loot cart broken: [1 minecraft:melon_seeds, 1 minecraft:melon_seeds, 2 minecraft:rail, 3 minecraft:rail, 4 minecraft:rail, 2 minecraft:coal, 1 minecraft:coal, 1 minecraft:golden_apple, 1 minecraft:activator_rail, 1 minecraft:rail, 2 minecraft:rail, 2 minecraft:rail, 1 minecraft:melon_seeds, 1 minecraft:activator_rail, 2 minecraft:coal, 1 minecraft:minecart, 1 minecraft:chest]
FRT-01 (T03): loaded cart (256 raw iron): 3 laps in 300 ticks, cargo after 256; empty cart: 3 laps in 300 ticks, cargo after 0
```

## Real client (`SurvivalSmokeClientTest`)

```
CLIENT MINECART: after three right-clicks with 64 raw iron: server [192 minecraft:raw_iron], client [192 minecraft:raw_iron], raw iron in hand 0, message 'Raw Iron 192 / 256'
CLIENT MINECART: riding after use: false; sneak-use unloaded 64 raw iron, cart now [128 minecraft:raw_iron], message 'Raw Iron 128 / 256'
CLIENT MINECART: after save, quit and reopen: server [128 minecraft:raw_iron], client [128 minecraft:raw_iron]
CLIENT MINECART: broken by hand: carts left [], raw iron on the ground 128 + picked up 0, minecart items 1 + picked up 0
CLIENT STORAGE: ender chest placed: false, message 'Ender chests don't work in this world'; chest minecart placed: false, still held: 1, message 'Chest minecarts don't work in this world: load a minecart instead'
```

Screenshots of these steps are in `screenshots/`: the cart with its label after loading, after unloading a stack, after reopening the world, broken (cargo and minecart on the rails), and the ender chest and chest minecart refusals.

## Repeats

`MinecartCargoGameTests` was run three more times on its own after the first full build: 8/8 each time.

## Walled light test (MobGameTests)

Changed to compare with its own control run (see the report, section 3). Final build:

```
MOB-01 walled: two closed boxes, one lit inside, 32 zombies; share of zombie-samples round the lit box / round the dark box: control 12% / 5%, attraction on 30% / 5% (6720 samples); any zombie inside a box at the end: false
```
