# simple_block

A stripped-down Minecraft Java Edition overhaul built around hostile nights, territory, mining and physical transport infrastructure. See [`docs/VISION.md`](docs/VISION.md) for the design and [`CLAUDE.md`](CLAUDE.md) for the working rules.

**Status:** Milestones 01 (Survival Foundation) and 02 (World, Night, Inventory) are done: see [`docs/MILESTONE_01_REPORT.md`](docs/MILESTONE_01_REPORT.md) and [`docs/MILESTONE_02_REPORT.md`](docs/MILESTONE_02_REPORT.md). Milestone 03 (minecart cargo and storage-bypass closure) is reported in [`docs/MILESTONE_03_REPORT.md`](docs/MILESTONE_03_REPORT.md). Milestone 04 (cable infrastructure) is reported in [`docs/MILESTONE_04_REPORT.md`](docs/MILESTONE_04_REPORT.md). Milestone 05 (cable haulage, the Cobblestone Engine and train couplings) is reported in [`docs/MILESTONE_05_REPORT.md`](docs/MILESTONE_05_REPORT.md).

**Fresh worlds only.** The mod is not meant to be added to existing saves.

## What Milestone 01 changes

| ID | Change | Where |
|---|---|---|
| SF-01 | Beds set your respawn point and you can lie down, but sleeping never skips the night or clears a storm (straw beds included). Players get vanilla's "No amount of rest can pass this night". Nether/End beds still explode. | `mixin/sleep/ServerLevelMixin` |
| SF-02 | Phantoms never spawn from insomnia. The phantom entity still exists for commands and spawn eggs. | `mixin/phantom/PhantomSpawnerMixin` |
| SF-03 | Nothing can glide, including command- or creative-given elytra. End ships no longer have the elytra item frame. | `feature/NoElytraFlight`, `mixin/elytra/EndCityPieceMixin` |
| SF-04 | All 17 shulker box variants can't be crafted, placed (by hand or dispenser), opened, or filled/emptied by hoppers. Shulker mobs are unchanged. | `feature/ShulkerBoxes`, `mixin/shulker/*`, `src/main/resources/data/minecraft/{recipe,advancement}` |

## What Milestone 02 changes

Milestone 02 (world, night, inventory); see [`docs/MILESTONE_02.md`](docs/MILESTONE_02.md).

| ID | Change | Where |
|---|---|---|
| GEO-01 | Granite, diorite, andesite and tuff no longer generate. Their blobs are gone, large ore veins lose their tuff/granite filler, and structures get stone- or deepslate-family blocks of the same shape (trial chambers become deepslate brick). Their recipes, mason trades and chest loot are disabled. The blocks stay registered. | `feature/RemovedRocks`, `mixin/geology/*`, recipe/advancement overrides, `data/minecraft/tags/villager_trade` |
| MINE-01 | Natural deepslate and all deepslate ores can't be mined at all without an iron, diamond or netherite pickaxe. Crafted deepslate keeps vanilla rules. | `feature/DeepslateGate`, `mixin/mining/*`, `#simple_block:requires_iron_pickaxe` |
| ORE-01 | Iron: sparse even background in stone, richer in deepslate, richer again under deep ocean; banded iron in mountains, alluvial deposits at desert/badlands/river landforms, pockets beside natural lava. | `worldgen/*`, `data/simple_block/worldgen`, `data/simple_block/tags/worldgen/biome` |
| ORE-02 | Diamonds at about 35% of vanilla. | `data/minecraft/worldgen/placed_feature/ore_diamond*.json` |
| INV-01 | 27 usable inventory slots: the hotbar plus two storage rows. The top storage row is covered in every screen and can't hold items; anything put there by a command is moved out or dropped. | `feature/InventorySlots`, `mixin/inventory/SlotMixin`, `InventoryMixin` |
| INV-02 | A small Sort button (the load bar's height, at its level) in the survival inventory merges stacks and orders the storage rows by item registry id, top row first. The hotbar is untouched. | `feature/InventorySort`, `network/SortInventoryPayload`, client `InventoryScreenAddons` |
| INV-03 | Shift-clicks fill empty hotbar slots from the left. | `mixin/inventory/AbstractContainerMenuMixin` |
| INV-04 | One shared carrying limit of 64 ingot-equivalents for iron, gold, copper, diamond (one diamond weighs like an ingot) and netherite (weighs like diamond: an ingot 1, gear what its diamond version weighs). Counts the inventory, offhand, cursor, 2x2 grid and bundles; worn armor is exempt. Excess stays where it was. Shown in the inventory as a bar without text, filled in one colour per material and framed red when full; hovering it shows the total out of 64 and each material's exact share. | `feature/HeavyLoad`, `feature/HeavyTransfers`, `mixin/inventory/*`, `simple_block/heavy_weights.json` |
| LIGHT-01 | Torches (wall and copper torches too) give light 12 instead of 14. Lanterns stay 15; other light sources are unchanged. Any block light still stops monster spawning in the Overworld. | `feature/LightLevels`, `mixin/light/BlockStateBaseMixin` |
| MOB-01 | Hostile mobs that wander drift towards the nearest area with block light within 20 blocks and loiter there, wandering about it rather than homing in on the light source. Active at Overworld night, and at any time of day wherever the mob gets no sky light (caves at any depth, under cover). Walls don't hide the light but do stop the mobs, so they hang about outside. A player in sight still comes first. | `feature/Entrancement`, `mixin/mobs/RandomStrollGoalMixin` |
| MOB-02 | Zombies, husks and zombie villagers break wooden doors only while chasing a player, on every difficulty except Peaceful, at the vanilla Hard pace (12 s). Iron and copper doors get attacked but never damaged. No door breaking without a chase. | `feature/ZombieDoors`, `mixin/mobs/ZombieMixin` |
| MOB-03 | Creepers in direct sunlight smoke for half a second and are gone: no explosion, no drops. Shade, caves, rain and night keep them. | `feature/CreeperEvaporation`, `mixin/mobs/CreeperMixin` |
| FOOD-01 | Sprinting, jumping, swimming and breaking blocks cost half the vanilla hunger exhaustion; fighting, taking damage, healing and the Hunger effect are unchanged. Saturation is shown on the food bar: the food icons it covers are drawn in gold and underlined. | `feature/Exhaustion`, `mixin/food/*`, `feature/SaturationSync`, client `SaturationHud` |

## What Milestone 03 changes

See [`docs/MILESTONE_03.md`](docs/MILESTONE_03.md) and, for how it differs from that spec, [`docs/DECISIONS.md`](docs/DECISIONS.md).

| ID | Change | Where |
|---|---|---|
| FRT-01..09 | The plain minecart is a cargo cart. Nobody gets in (players or mobs) unless a cushion is fitted. It holds up to 256 of one kind of mined resource: raw iron/copper/gold, those ores, coal, diamond, emerald, lapis, redstone, stone, cobblestone, deepslate, cobbled deepslate. Use while holding cargo to load it; sneak + use to take out a stack, as much as fits in your inventory and under the heavy-load cap; looking at a cart shows its cargo. Breaking or blowing up a cart spills the cargo as ordinary stacks and leaves a plain minecart. Furnace, hopper and TNT minecarts are unchanged. | `feature/MinecartCargo`, `mixin/minecart/MinecartMixin`, `#simple_block:minecart_cargo`, client `CargoMinecartRenderer` |
| CUSH-01 | Use a cushion on an empty minecart to fit it: the cushion shows in the cart and the cart works like a vanilla minecart for riding (players get in, a rolling cart picks up mobs), but carries no cargo. Sneak + use takes the cushion back out. | `feature/MinecartCushions`, `mixin/minecart/MinecartMixin`, client `CargoMinecartRenderer` and `mixin/AbstractMinecartRendererMixin` |
| FRT-10 | Ender chests can't be crafted, placed or opened, and mining one gives 8 obsidian even with silk touch. Not in the creative tabs. | `feature/EnderChests`, `mixin/enderchest/*`, recipe/advancement/loot overrides |
| FRT-11 | Chest minecarts can't be crafted or placed (by hand or dispenser). The ones mineshafts generate are sealed loot: they don't open, hoppers can't reach them, and breaking one spills the loot plus a minecart and a chest. Not in the creative tabs. | `feature/ChestMinecarts`, `mixin/chestcart/*`, recipe/advancement overrides |
| FRT-12 | Donkeys, mules and llamas still carry chests. | (unchanged) |

Provisional numbers (ore amounts, deposit shapes, the diamond share) are in one table, `tuning/Tuning.java`. To change one in a game, copy its line from `config/simple_block/tuning-defaults.json` into `config/simple_block/tuning.json` and restart; worldgen values affect newly generated chunks.

Each feature is self-contained. To revert one, remove its mixin entries from `src/main/resources/simple_block.mixins.json` and its files listed above (plus its `register()` call in `SimpleBlock`).

## What Milestone 04 adds

See [`docs/MILESTONE_04.md`](docs/MILESTONE_04.md) and, for the choices made where the brief left room, [`docs/DECISIONS.md`](docs/DECISIONS.md). In M04 nothing moves: M05 (below) makes the cable haul carts. M06 (below) changes how much cable a drum holds and the cable recipes; the rows here describe M04 as built.

| Component | What it does | Where |
|---|---|---|
| Cable and cable coils | Cable (4 blocks of it per item; 3 iron nuggets + string make 2) combines into coils of 8 (32 blocks) and back. | `cable/CableContent`, `data/simple_block/recipe` |
| Cable drum | Placed like a block (recipe: empty top row; stick, iron, stick; plank, iron, plank). Use a coil on it to wind the cable on (up to 128 blocks): the barrel shows cable and the loose end with its hook hangs at its foot. | `cable/CableDrumBlock`, `CableDrumBlockEntity`, client `cable/CableDrumRenderer` |
| Copper cable anchor | Hangs only under wooden blocks (logs, planks, wooden slabs, stairs and fences, bamboo) that offer their underside, and drops when that block goes. Carries one cable on a roller; no collision. | `cable/CableAnchorBlock`, `#simple_block:cable_anchor_supports` |
| Laying cable | Use the hook to pick up the end; it follows your hand like a lead. Use anchors in turn to run the cable through them: each straight span (at most 12 blocks, clear of solid blocks) uses its length off the drum. Use the drum or the last anchor to put the end down. Wind on more coils and carry on to extend. Sneak + use an anchor to disconnect from there on, sneak + use the drum to wind everything back and then take the cable off as coils; breaking an anchor or the drum does the same. | `cable/CableRoutes`, `cable/CableHook`, client `cable/CableHookRenderer` |
| Copper connecting rod | Use it on a minecart to fit it (sneak + use takes it off once the cart is empty). Under a laid cable, 1.5 to 3 blocks above the rail, it reaches up and grips it, rolls past the anchors, and lets go where the cable ends. | `cable/ConnectingRods`, `mixin/minecart/MinecartMixin`, client `CargoMinecartRenderer` |
| Models | Editable Blockbench sources with UV-template textures for you to paint: `art/blockbench/` (see its README). | `art/blockbench/*.bbmodel`, `assets/simple_block/{models,textures,blockstates,items}` |

## What Milestone 05 adds

See [`docs/MILESTONE_05.md`](docs/MILESTONE_05.md), and section 5.0 of [`docs/DECISIONS.md`](docs/DECISIONS.md) for the choices and starting numbers. Every M04 part and gesture works as before.

| Component | What it does | Where |
|---|---|---|
| Cable loops | Use a copper anchor on the last anchor of a straight route to close it into a loop: that anchor becomes a return wheel and the return cable runs back beside the working one, off the drum (refused, with nothing used, if the drum is short of cable, the return path is blocked or the route turns). Sneak + use an anchor or the drum (empty hands) to open it again; breaking a part opens it too. No cable is ever lost. | `cable/CableLoops`, `cable/CableAnchorBlock` (`return` state), `CableAnchorBlockEntity`, client `cable/CableDrumRenderer` |
| Hand crank and hauling | Every drum has a crank on its axle end. Hold left-click on it to feed the cable away from the drum, hold right-click to pull it back (left-clicking the crank never breaks the drum). On a loop the cable moves, and a cart whose rod grips the working strand (never the return) is hauled out or back with the carts coupled to it, slower uphill the heavier the train, stopping short of either end; let go and it is held. An open route's drum is locked; other uses of the drum are as in M04. | `cable/CableDrive`, `network/CrankPayload`, client `mixin/MultiPlayerGameModeMixin`, `cable/CableIndex`, `cable/ConnectingRods`, `cable/CableDrumBlock`, `train/TrainPhysics` |
| Overhead drum | Place a drum against the underside of a wooden beam and it hangs there, its cable at roller height. Standing drums work as before. | `cable/CableDrumBlock` (`hanging` state) |
| Cobblestone Engine | Use a furnace on an empty minecart: it becomes a Cobblestone Engine. Coal or charcoal fuels it; with an empty hand, use it to start it away from you or to stop it. A player sitting in a cushioned cart coupled to it drives: forward powers it towards where you look along the track, back brakes. It pushes and pulls its train both ways; weight and climbs slow it, and a sustained climb stalls a train. Breaking it gives back the minecart and the furnace. M07: drawn as a heavy furnace engine with a tall smokestack seated inside the unchanged vanilla minecart. The furnace-minecart recipe is gone. | `train/CobblestoneEngine`, `mixin/train/MinecartFurnaceMixin`, `mixin/train/VehicleEntityMixin`, recipe/advancement overrides, client `train/CobblestoneEngineRenderer` |
| Couplings | Use an iron chain on a cart, then on the next (within 3 blocks): they are coupled. Shears take a coupling off and give the chain back; a broken cart drops its chains. | `train/Couplings`, `mixin/train/AbstractMinecartMixin`, client `train/CouplingChains` |
| Models | New editable sources: anchor return guide and wheel, the drum's frame, spool and crank, the engine. The drum's side ghosting (BUG-001) is fixed in the model generator. | `art/blockbench/`, `tools/models/generate_models.py` |

## What Milestone 06 adds

See [`docs/MILESTONE_06.md`](docs/MILESTONE_06.md) and [`docs/MILESTONE_06_REPORT.md`](docs/MILESTONE_06_REPORT.md). M01–M05 work as before except where this table says otherwise.

| Component | What it does | Where |
|---|---|---|
| Cable recipes and length | Shaped, middle column only: three iron nuggets make 2 cable; three cable make a coil; a coil goes back into 3 cable. One coil on a drum lays any length of route (spans still at most 12 blocks); closing a loop costs nothing more; taking it down gives back exactly the one coil. | `data/simple_block/recipe/cable*.json`, `cable/CenterColumnRecipes` and `mixin/cable/*` (the middle column), `cable/CableRoutes`, `simple_block/heavy_weights.json` |
| Long lines | Every installed line is kept with the level, not in its drum, and driven every tick: a cart hauled where it is loaded moves even when the drum or the far anchors are not. A train stops (and a coal winch's lever goes to NEUTRAL) at the edge of the entity-ticking world; nothing loads chunks or moves unloaded carts. An anchor broken while its drum is unloaded cuts the line at once. | `cable/CableLines`, `cable/CableDrive`, `cable/CableIndex`, `cable/CableAnchorBlockEntity` |
| Coal winch | Use a furnace on a drum: the furnace motor takes the place of the drum's motor-side stand (M07 model) and the crank becomes a lever (cable and route kept). Right-click the lever: NEUTRAL, PULL (leans back, hauls towards the drum), FEED (leans forward), NEUTRAL; it stays where set and the winch runs without you. Use coal on the drum to put one in (regular coal only); an empty hand on the drum tells you the coal, the burn time left and the lever. Faster than the crank; coal burns only while the load moves. After a stop against an end (or anything else in the way it was going), the next click skips that direction, so the way out can always be chosen (BUG-002). Docking, the end of the line or track, a derailment, a blocked cart, the edge of the loaded world or running out of coal put the lever back to NEUTRAL. Partly burnt coal is kept exactly; a loaded world never resumes hauling. Breaking the drum gives back the furnace and the coal. | `cable/CoalWinch`, `cable/CableDrive`, `cable/CableDrumBlock`, `winch.*` in `tuning/Tuning`, client `cable/CableDrumRenderer` |
| Loading Rail | One rail + one cable anchor, shapeless (id `simple_block:loading_track`). A straight rail facing the way you face when placing it. A freight cart (a plain minecart without a cushion) rolling onto it docks in the middle with its front (its folding gate) the rail's way, and its whole train is held: an engine in it is switched off, a hauling winch goes to NEUTRAL. A hinged triangular iron stop on the inside edge of the rail folds flat under passing carts and flips up behind a docked cart (M06 correction). Use the rail, or the raised stop from behind the cart, with an empty hand to release; the stop folds, nothing starts by itself, and the released cars pass over it once before it catches them again. The gate stays closed: opening it onto a skip is M07. | `train/LoadingTrackBlock`, `train/LoadingTracks`, `train/Couplings`, `mixin/train/AbstractMinecartMixin`, client `CargoMinecartRenderer` |
| Redstone rails retired | Powered, detector and activator rails can't be crafted and no longer come in mineshaft chests (no structure places them). Placed ones still work; ordinary rails are unchanged. | `feature/RetiredItems`, recipe/advancement overrides |
| Creative menu | No retired item shows in a creative tab or the creative search: elytra, shulker boxes, the granite/diorite/andesite/tuff families, ender chest, chest minecart, furnace minecart, the three redstone rails. They stay registered. | `feature/RetiredItems` |
| No shoving | Walking into a minecart that can't carry a passenger (freight carts, engines, other vanilla carts) doesn't move it; cushioned carts are pushed as before. Engines, cables, couplings and slopes move carts as before. | `feature/CartPushing`, `mixin/pushing/*` |
| Models | New editable sources: the drum's furnace and lever, the Loading Rail (stop folded and raised), the freight gate (closed and open). | `art/blockbench/`, `tools/models/generate_models.py` |

## Toolchain

| | |
|---|---|
| Minecraft | 26.3 (Java Edition, unobfuscated, so there's no mappings layer) |
| Loader | Fabric Loader 0.19.5 + Fabric API 0.162.0+26.3 |
| Build | Gradle 9.7.1 (wrapper), Fabric Loom 1.18.4 |
| Java | JDK 25 (Minecraft 26.x requires it) |
| Mod ID | `simple_block` (provisional, taken from the repository name) |

## Commands

All commands run from the repository root with `JAVA_HOME` pointing at a JDK 25.

| Task | Command |
|---|---|
| Build the mod jar (also runs the server game tests) | `./gradlew build` → `build/libs/simple_block-<version>.jar` |
| Play in a dev client | `./gradlew runClient` |
| Start a dev dedicated server | `./gradlew runServer` (first run: set `eula=true` in `run/eula.txt`) |
| Server game tests only | `./gradlew runGameTest` |
| End-to-end client test (real client, screenshots in `build/run/clientGameTest/screenshots`) | `./gradlew runClientGameTest` |
| Measure ore and rock generation on a normal world (seed from `build/run/oreSampler/server.properties`) | `./gradlew runOreSampler -PoreSamplerLabel=mod` → `build/ore-sampler/` |

Test runs set `eula = true` in `build.gradle`, which accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for the throwaway test servers.

On a machine without a display, run the client test under Xvfb: `xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGameTest`. Xvfb has no sRGB-capable GLX visuals, so Minecraft 26.3 falls back to Vulkan there. That needs Mesa's software Vulkan driver (`apt install mesa-vulkan-drivers`). A normal desktop uses OpenGL and needs none of this.

## Playtesting builds on Windows

Every push to `main` is built and tested by GitHub Actions (`.github/workflows/build.yml`), which publishes the mod jar as the `simple-block-mod` artifact. On Windows, double-clicking `tools/windows/Update-SimpleBlock.bat` installs the newest successful build into your Simple Block game directory. One-time setup: [`docs/PLAYTESTING_SETUP.md`](docs/PLAYTESTING_SETUP.md).

## Tests

`src/gametest` holds two kinds of tests:

- **Server game tests** (`*GameTests.java`) run each rule in a real server world with stand-in players. Examples: two players sleeping at midnight and in thunderstorms, 96,000 insomnia-spawner ticks, generating a real End ship, crafting and placing every shulker box colour, hoppers and dispensers.
- **Client smoke test** (`client/SurvivalSmokeClientTest.java`) drives the real game in a fresh survival world with simulated input: bed at midnight, shulker box, walking/jumping/falling, elytra jump.

Each test was also run once against a build with the restrictions removed, to show the tests detect vanilla behaviour. Results are in [`docs/evidence/milestone-01/`](docs/evidence/milestone-01/).
