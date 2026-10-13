# simple_block

A stripped-down Minecraft Java Edition overhaul built around hostile nights, territory, mining and physical transport infrastructure. See [`docs/VISION.md`](docs/VISION.md) for the design and [`CLAUDE.md`](CLAUDE.md) for the working rules.

**Current milestone status and handoffs:** [`docs/ROADMAP.md`](docs/ROADMAP.md) (status cells synchronized from milestone YAML headers). Canonical lifecycle and handoff rules: [`docs/RULES.md`](docs/RULES.md). **PLANNED - READY is not implementation authorization.**

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

See the [M03 handoff](docs/MILESTONE_03.md) and [historical implementation report](docs/archive/milestone-reports/MILESTONE_03_REPORT.md) for the owner-approved vanilla-minecart change.

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

See the [M04 handoff](docs/MILESTONE_04.md) and [historical as-built report](docs/archive/milestone-reports/MILESTONE_04_REPORT.md). In M04 nothing moves: M05 (below) makes the cable haul carts. M06 (below) changes how much cable a drum holds and the cable recipes; the rows here describe M04 as built.

| Component | What it does | Where |
|---|---|---|
| Cable and cable coils | Cable (4 blocks of it per item; 3 iron nuggets + string make 2) combines into coils of 8 (32 blocks) and back. | `cable/CableContent`, `data/simple_block/recipe` |
| Cable drum | Placed like a block (recipe: empty top row; stick, iron, stick; plank, iron, plank). Use a coil on it to wind the cable on (up to 128 blocks): the barrel shows cable and the loose end with its hook hangs at its foot. | `cable/CableDrumBlock`, `CableDrumBlockEntity`, client `cable/CableDrumRenderer` |
| Copper cable anchor | Hangs only under wooden blocks (logs, planks, wooden slabs, stairs and fences, bamboo) that offer their underside, and drops when that block goes. Carries one cable on a roller; no collision. | `cable/CableAnchorBlock`, `#simple_block:cable_anchor_supports` |
| Laying cable | Use the hook to pick up the end; it follows your hand like a lead. Use anchors in turn to run the cable through them: each straight span (at most 12 blocks, clear of solid blocks) uses its length off the drum. Use the drum or the last anchor to put the end down. Wind on more coils and carry on to extend. Sneak + use an anchor to disconnect from there on, sneak + use the drum to wind everything back and then take the cable off as coils; breaking an anchor or the drum does the same. | `cable/CableRoutes`, `cable/CableHook`, client `cable/CableHookRenderer` |
| Copper connecting rod | Use it on a minecart to fit it (sneak + use takes it off once the cart is empty). Under a laid cable, 1.5 to 3 blocks above the rail, it reaches up and grips it, rolls past the anchors, and lets go where the cable ends. | `cable/ConnectingRods`, `mixin/minecart/MinecartMixin`, client `CargoMinecartRenderer` |
| Models | Editable Blockbench sources with UV-template textures for you to paint: `art/blockbench/` (see its README). | `art/blockbench/*.bbmodel`, `assets/simple_block/{models,textures,blockstates,items}` |

## What Milestone 05 adds

See the [M05 handoff](docs/MILESTONE_05.md) and [historical as-built report](docs/archive/milestone-reports/MILESTONE_05_REPORT.md) for choices and starting values. Every M04 part and gesture works as before.

| Component | What it does | Where |
|---|---|---|
| Cable loops | Use a copper anchor on the last anchor of a straight route to close it into a loop: that anchor becomes a return wheel and the return cable runs back beside the working one, off the drum (refused, with nothing used, if the drum is short of cable, the return path is blocked or the route turns). Sneak + use an anchor or the drum (empty hands) to open it again; breaking a part opens it too. No cable is ever lost. | `cable/CableLoops`, `cable/CableAnchorBlock` (`return` state), `CableAnchorBlockEntity`, client `cable/CableDrumRenderer` |
| Hand crank and hauling | Every drum has a crank on its axle end. Hold left-click on it to feed the cable away from the drum, hold right-click to pull it back (left-clicking the crank never breaks the drum). On a loop the cable moves, and a cart whose rod grips the working strand (never the return) is hauled out or back with the carts coupled to it, slower uphill the heavier the train, stopping short of either end; let go and it is held. An open route's drum is locked; other uses of the drum are as in M04. | `cable/CableDrive`, `network/CrankPayload`, client `mixin/MultiPlayerGameModeMixin`, `cable/CableIndex`, `cable/ConnectingRods`, `cable/CableDrumBlock`, `train/TrainPhysics` |
| Overhead drum | Place a drum against the underside of a wooden beam and it hangs there, its cable at roller height. Standing drums work as before. | `cable/CableDrumBlock` (`hanging` state) |
| Cobblestone Engine | Use a furnace on an empty minecart: it becomes a Cobblestone Engine. Coal or charcoal fuels it; with an empty hand, use it to start it away from you or to stop it. A player sitting in a cushioned cart coupled to it drives: forward powers it towards where you look along the track, back brakes. It pushes and pulls its train both ways; weight and climbs slow it, and a sustained climb stalls a train. Breaking it gives back the minecart and the furnace. M07: drawn as a heavy furnace engine with a tall smokestack seated inside the unchanged vanilla minecart. The furnace-minecart recipe is gone. | `train/CobblestoneEngine`, `mixin/train/MinecartFurnaceMixin`, `mixin/train/VehicleEntityMixin`, recipe/advancement overrides, client `train/CobblestoneEngineRenderer` |
| Couplings | Use an iron chain on a cart, then on the next (within 3 blocks): they are coupled. Shears take a coupling off and give the chain back; a broken cart drops its chains. | `train/Couplings`, `mixin/train/AbstractMinecartMixin`, client `train/CouplingChains` |
| Models | New editable sources: anchor return guide and wheel, the drum's frame, spool and crank, the engine. The drum's side ghosting (BUG-001) is fixed in the model generator. | `art/blockbench/`, `tools/models/generate_models.py` |

## What Milestone 06 adds

See [`docs/MILESTONE_06.md`](docs/MILESTONE_06.md) and [`docs/MILESTONE_06_REPORT.md`](docs/archive/milestone-reports/MILESTONE_06_REPORT.md). M01–M05 work as before except where this table says otherwise.

| Component | What it does | Where |
|---|---|---|
| Cable recipes and length | Shaped, middle column only: three iron nuggets make 2 cable; three cable make a coil; a coil goes back into 3 cable. One coil on a drum lays any length of route (spans still at most 12 blocks); closing a loop costs nothing more; taking it down gives back exactly the one coil. | `data/simple_block/recipe/cable*.json`, `cable/CenterColumnRecipes` and `mixin/cable/*` (the middle column), `cable/CableRoutes`, `simple_block/heavy_weights.json` |
| Long lines | Every installed line is kept with the level, not in its drum, and driven every tick: a cart hauled where it is loaded moves even when the drum or the far anchors are not. A train stops (and a coal winch's lever goes to NEUTRAL) at the edge of the entity-ticking world; nothing loads chunks or moves unloaded carts. An anchor broken while its drum is unloaded cuts the line at once. | `cable/CableLines`, `cable/CableDrive`, `cable/CableIndex`, `cable/CableAnchorBlockEntity` |
| Coal winch | Use a furnace on a drum: the furnace motor takes the place of the drum's motor-side stand (M07 model) and the crank becomes a lever (cable and route kept). Right-click the lever for PULL (leans back, hauls towards the drum), left-click for FEED (leans forward); the same click again sets NEUTRAL, the other switches straight over (owner's controls, 2026-10-13). It stays where set and the winch runs without you. Use coal on the drum to put one in (regular coal only); an empty hand on the drum tells you the coal, the burn time left and the lever. Faster than the crank; coal burns only while the load moves. After a stop against an end, the other click takes the load back out (BUG-002). Docking, the end of the line or track, a derailment, a blocked cart, the edge of the loaded world or running out of coal put the lever back to NEUTRAL. Partly burnt coal is kept exactly; a loaded world never resumes hauling. Breaking the drum gives back the furnace and the coal. | `cable/CoalWinch`, `cable/CableDrive`, `cable/CableDrumBlock`, `winch.*` in `tuning/Tuning`, client `cable/CableDrumRenderer` |
| Loading Rail | One rail + one cable anchor, shapeless (id `simple_block:loading_track`). A straight rail facing the way you face when placing it. A freight cart (a plain minecart without a cushion) rolling onto it docks in the middle with its front (its folding gate) the rail's way, and its whole train is held: an engine in it is switched off, a hauling winch goes to NEUTRAL. A hinged triangular iron stop on the inside edge of the rail folds flat under passing carts and flips up behind a docked cart (M06 correction). Use the rail, or the raised stop from behind the cart, with an empty hand to release; the stop folds, nothing starts by itself, and the released cars pass over it once before it catches them again. M07: the gate folds down as a ramp into a skip at bridle height. | `train/LoadingTrackBlock`, `train/LoadingTracks`, `train/Couplings`, `mixin/train/AbstractMinecartMixin`, client `CargoMinecartRenderer` |
| Redstone rails retired | Powered, detector and activator rails can't be crafted and no longer come in mineshaft chests (no structure places them). Placed ones still work; ordinary rails are unchanged. | `feature/RetiredItems`, recipe/advancement overrides |
| Creative menu | No retired item shows in a creative tab or the creative search: elytra, shulker boxes, the granite/diorite/andesite/tuff families, ender chest, chest minecart, furnace minecart, the three redstone rails. They stay registered. | `feature/RetiredItems` |
| No shoving | Walking into a minecart that can't carry a passenger (freight carts, engines, other vanilla carts) doesn't move it; cushioned carts are pushed as before. Engines, cables, couplings and slopes move carts as before. | `feature/CartPushing`, `mixin/pushing/*` |
| Models | New editable sources: the drum's furnace and lever, the Loading Rail (stop folded and raised), the freight gate (closed and open). | `art/blockbench/`, `tools/models/generate_models.py` |

## What Milestone 07 adds

See [`docs/MILESTONE_07.md`](docs/MILESTONE_07.md) and [`docs/MILESTONE_07_REPORT.md`](docs/archive/milestone-reports/MILESTONE_07_REPORT.md). M01–M06 work as before except where this table says otherwise.

| Component | What it does | Where |
|---|---|---|
| Ore Skip | Shaped recipe: cable top middle, iron trapdoor in the middle, iron ingot / minecart / iron ingot along the bottom; one skip. Used on the top of a block it stands there, front (its chute's mouth) towards you. One block wide and deep, three high; hung on a cable it gains its bridle as a fourth, colliding block on top (attaching is refused, using nothing, if that block's space is taken), and loses it when taken off. Its picture is wider than its block, so the block on each side of its shaft must be clear (it is refused, or stops, otherwise). Breaking it drops the skip and its cargo as ordinary stacks. | `skip/OreSkip`, `skip/SkipItem`, `skip/SkipContent`, recipe, client `skip/OreSkipRenderer` |
| One 512 slot | One commodity, up to 512 (freight-cart cargo only: ores, raw metals, coal, gems, stone). Cargo in hand loads it; an empty hand opens its screen, one slot showing the item and "count / 512". Left-click takes a stack (or puts in what you hold), right-click half a stack (or one), shift-click a stack into your inventory; shift-click cargo in your inventory to put it in. Nothing leaves it bigger than a normal stack, and the heavy-load cap applies. | `skip/SkipCargo`, `skip/SkipMenu`, client `skip/SkipScreen` |
| Hoisting | Route a drum's cable to an anchor over the shaft. The loose end hangs under that anchor, and the crank or the coal winch lowers it (FEED) and raises it (PULL) straight down the shaft, to any depth, before anything is hooked on; it stops on what is under it and keeps its height through saves. Carrying the end across the ground, it goes taut 12 blocks from its support and is put down beyond about 16 (height doesn't count, so you can pick it up at the foot of a deep shaft). Use it on the skip standing under that anchor: it hangs on one cable (bridle apex to the anchor, which turns into a ceiling pulley while a skip hangs from it, to the drum; the one coil). The hand crank (right-click pulls = up, held left-click feeds = down) and the coal winch's lever (PULL up, FEED down) move it straight up and down. It stops at the head of the shaft (apex under the roller), on what is under it, at anything in its column or side cells, and at stations; the lever goes to NEUTRAL, coal burns only while it moves. It never moves while its chunk isn't loaded. Sneak-use with an empty hand, while it rests on something, takes it off the cable; changing the route lets it go. | `cable/CableHoists`, `cable/CableDrive`, `cable/CoalWinch`, `skip/OreSkip` |
| Stations by height | A Loading Rail in front of a hung skip (either way: end-on, or side-on so each car of a coupled train can be worked) with a freight cart docked: a cart level with the skip's bridle tips its load in (its gate on the skip's side opens, the skip's lid opens); a cart one level below the skip's chute, kitty-corner, with the chute's lip on its rim, is filled (the skip's discharge gate slides open, the cart's gates stay shut). A cart level with the chute, side by side, is never filled. Direction depends only on the geometry, never on who holds cargo; as much moves as the receiver takes; another commodity, another height, a rail beside the skip or an unhung skip does nothing. A skip wound past a station height stops there; the train stays held; nothing restarts by itself. | `skip/SkipStations`, `mixin/train/AbstractMinecartMixin`, client `CargoMinecartRenderer` |
| Engine and winch models | The Cobblestone Engine is a heavy furnace engine with a tall smokestack inside the minecart body (below); the coal winch's furnace motor replaces the drum's motor-side stand (hung under a beam its flue rises beside the beam). Visual only. | client `train/CobblestoneEngineRenderer`, `cable/CableDrumBlock#POWERED`, `tools/models/generate_models.py` |
| Minecart body | Every minecart (freight, cushioned, the Cobblestone Engine and the rest) has the same body, in vanilla's proportions and texture: vanilla's floor, four corner columns, a fixed rim along the top of each long side, and four hinged gates. The end gates fold outwards and down into ramps; the wider side gates swing up and out from under their rims. Closed, it looks like vanilla's cart. Only a freight cart tipping into a skip opens a gate (the one facing the skip); the others stay shut. | client `train/GatedMinecartModel`, `mixin/AbstractMinecartRendererModelMixin`, `CargoMinecartRenderer`, `mixin/train/AbstractMinecartMixin` |
| Line ends | A winched cart runs on onto a Loading Rail it will dock at even within the line's end clearance (1.5 blocks of the drum or the return wheel), so a station under or just before the last support is reached with no cable past it. Without a station the cart still stops at the clearance, on the rails. | `cable/CableDrive`, `train/LoadingTracks` |
| Moving cable | The hoist cable (and the free end's) moves along its whole length, routed and down the shaft, with the drum while the skip or hook actually moves; it stands still when they stop. | `cable/CableLines#shownSpeed`, client `cable/CableDrumRenderer`, `skip/OreSkipRenderer`, `cable/CableHookRenderer` |
| Models | New editable sources: the skip's chute, bin, discharge gate (closed/open), lid (closed/open), bridle and item; the engine; the coal winch's powered frames; the ceiling pulley; the minecart body (`gated_minecart`, an entity model on vanilla's minecart texture). | `art/blockbench/`, `tools/models/generate_models.py`, `tools/models/generate_minecart_bbmodel.py` |

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

Every push that changes mod/build inputs on `main` is built and tested by GitHub Actions (`.github/workflows/build.yml`), which publishes the mod jar as the `simple-block-mod` artifact. On Windows, double-clicking `tools/windows/Update-SimpleBlock.bat` installs the newest successful build into your Simple Block game directory. One-time setup: [`docs/PLAYTESTING_SETUP.md`](docs/PLAYTESTING_SETUP.md).

## Tests

`src/gametest` holds two kinds of tests:

- **Server game tests** (`*GameTests.java`) run each rule in a real server world with stand-in players. Examples: two players sleeping at midnight and in thunderstorms, 96,000 insomnia-spawner ticks, generating a real End ship, crafting and placing every shulker box colour, hoppers and dispensers.
- **Client smoke test** (`client/SurvivalSmokeClientTest.java`) drives the real game in a fresh survival world with simulated input: bed at midnight, shulker box, walking/jumping/falling, elytra jump.

Each test was also run once against a build with the restrictions removed, to show the tests detect vanilla behaviour. Results are in [`docs/evidence/milestone-01/`](docs/evidence/milestone-01/).

<!-- GENERATED SOURCE INVENTORY START -->
## Generated Java source inventory

Counts reflect source files only, not proof of implemented or tested behavior.

| Source set | First-level package | Java files |
|---|---|---:|
| client | client | 18 |
| gametest | gametest | 39 |
| main | (root) | 1 |
| main | cable | 17 |
| main | feature | 20 |
| main | mixin | 38 |
| main | network | 3 |
| main | skip | 7 |
| main | train | 6 |
| main | tuning | 1 |
| main | worldgen | 7 |

Regenerate using tools/docs/automation.py readme --write
<!-- GENERATED SOURCE INVENTORY END -->
