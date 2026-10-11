# simple_block

A stripped-down Minecraft Java Edition overhaul built around hostile nights, territory, mining and physical transport infrastructure. See [`docs/VISION.md`](docs/VISION.md) for the design and [`CLAUDE.md`](CLAUDE.md) for the working rules.

**Status:** Milestone 01 (Survival Foundation) is implemented and tested. See [`docs/MILESTONE_01_REPORT.md`](docs/MILESTONE_01_REPORT.md) and [`docs/TEST_CHECKLIST_01.md`](docs/TEST_CHECKLIST_01.md).

**Fresh worlds only.** The mod is not meant to be added to existing saves.

## What Milestone 01 changes

| ID | Change | Where |
|---|---|---|
| SF-01 | Beds set your respawn point and you can lie down, but sleeping never skips the night or clears a storm. Players get vanilla's "No amount of rest can pass this night". Nether/End beds still explode. | `mixin/sleep/ServerLevelMixin` |
| SF-02 | Phantoms never spawn from insomnia. The phantom entity still exists for commands and spawn eggs. | `mixin/phantom/PhantomSpawnerMixin` |
| SF-03 | Nothing can glide, including command- or creative-given elytra. End ships no longer have the elytra item frame. | `feature/NoElytraFlight`, `mixin/elytra/EndCityPieceMixin` |
| SF-04 | All 17 shulker box variants can't be crafted, placed (by hand or dispenser), opened, or filled/emptied by hoppers. Shulker mobs are unchanged. | `feature/ShulkerBoxes`, `mixin/shulker/*`, `src/main/resources/data/minecraft/{recipe,advancement}` |

## What Milestone 02 changes

Milestone 02 (world, night, inventory) is in progress; see [`docs/MILESTONE_02.md`](docs/MILESTONE_02.md).

| ID | Change | Where |
|---|---|---|
| GEO-01 | Granite, diorite, andesite and tuff no longer generate. Their blobs are gone, large ore veins lose their tuff/granite filler, and structures get stone- or deepslate-family blocks of the same shape (trial chambers become deepslate brick). Their recipes, mason trades and chest loot are disabled. The blocks stay registered. | `feature/RemovedRocks`, `mixin/geology/*`, recipe/advancement overrides, `data/minecraft/tags/villager_trade` |
| MINE-01 | Natural deepslate and all deepslate ores can't be mined at all without an iron, diamond or netherite pickaxe. Crafted deepslate keeps vanilla rules. | `feature/DeepslateGate`, `mixin/mining/*`, `#simple_block:requires_iron_pickaxe` |
| ORE-01 | Iron: sparse even background in stone, richer in deepslate, richer again under deep ocean; banded iron in mountains, alluvial deposits at desert/badlands/river landforms, pockets beside natural lava. | `worldgen/*`, `data/simple_block/worldgen`, `data/simple_block/tags/worldgen/biome` |
| ORE-02 | Diamonds at about 35% of vanilla. | `data/minecraft/worldgen/placed_feature/ore_diamond*.json` |
| INV-01 | 27 usable inventory slots: the hotbar plus two storage rows. The top storage row is covered in every screen and can't hold items; anything put there by a command is moved out or dropped. | `feature/InventorySlots`, `mixin/inventory/SlotMixin`, `InventoryMixin` |
| INV-02 | A Sort button in the survival inventory merges stacks and orders the storage rows by item registry id, bottom row first. The hotbar is untouched. | `feature/InventorySort`, `network/SortInventoryPayload`, client `InventoryScreenAddons` |
| INV-03 | Shift-clicks fill empty hotbar slots from the left. | `mixin/inventory/AbstractContainerMenuMixin` |
| INV-04 | One shared carrying limit of 64 ingot-equivalents for iron, gold and copper (inventory, offhand, cursor, 2x2 grid, bundles; worn armor exempt). Excess stays where it was. Shown as "Heavy x / 64" in the inventory. | `feature/HeavyLoad`, `feature/HeavyTransfers`, `mixin/inventory/*`, `simple_block/heavy_weights.json` |

Provisional numbers (ore amounts, deposit shapes, the diamond share) are in one table, `tuning/Tuning.java`. To change one in a game, copy its line from `config/simple_block/tuning-defaults.json` into `config/simple_block/tuning.json` and restart; worldgen values affect newly generated chunks.

Each feature is self-contained. To revert one, remove its mixin entries from `src/main/resources/simple_block.mixins.json` and its files listed above (plus its `register()` call in `SimpleBlock`).

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
