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
