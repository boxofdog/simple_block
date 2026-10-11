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

Each feature is self-contained. To revert one, remove its mixin entries from `src/main/resources/simple_block.mixins.json` and its files listed above (plus the `register()` call in `SimpleBlock` for SF-03).

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

Test runs set `eula = true` in `build.gradle`, which accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for the throwaway test servers.

On a machine without a display, run the client test under Xvfb: `xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGameTest`. Xvfb has no sRGB-capable GLX visuals, so Minecraft 26.3 falls back to Vulkan there. That needs Mesa's software Vulkan driver (`apt install mesa-vulkan-drivers`). A normal desktop uses OpenGL and needs none of this.

## Playtesting builds on Windows

Every push to `main` is built and tested by GitHub Actions (`.github/workflows/build.yml`), which publishes the mod jar as the `simple-block-mod` artifact. On Windows, double-clicking `tools/windows/Update-SimpleBlock.bat` installs the newest successful build into your Simple Block game directory. One-time setup: [`docs/PLAYTESTING_SETUP.md`](docs/PLAYTESTING_SETUP.md).

## Tests

`src/gametest` holds two kinds of tests:

- **Server game tests** (`*GameTests.java`) run each rule in a real server world with stand-in players. Examples: two players sleeping at midnight and in thunderstorms, 96,000 insomnia-spawner ticks, generating a real End ship, crafting and placing every shulker box colour, hoppers and dispensers.
- **Client smoke test** (`client/SurvivalSmokeClientTest.java`) drives the real game in a fresh survival world with simulated input: bed at midnight, shulker box, walking/jumping/falling, elytra jump.

Each test was also run once against a build with the restrictions removed, to show the tests detect vanilla behaviour. Results are in [`docs/evidence/milestone-01/`](docs/evidence/milestone-01/).
