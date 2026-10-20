# Simple Block

A Minecraft Java Edition overhaul about **hostile nights, meaningful mining, industrial freight and physical infrastructure**. The goal is to transform the vanilla resource loop, not pile on extra ores and machines. Read the [vision](docs/VISION.md) for the design philosophy.

**Project navigation:** [Roadmap and milestone status](docs/ROADMAP.md) · [Owner decisions and handoffs](docs/RULES.md) · [Open bugs](docs/BUG_LOG.md) · [UX feedback to replace](docs/UX_TEXT_LOG.md) · [Painting queue](docs/Joe's%20To-Do/README.md) · [Future ideas](docs/BACKLOG.md).

**Fresh worlds only:** The mod is not designed to be installed into existing vanilla saves. **Milestone status is authoritative in the milestone YAML header**, mirrored into the roadmap; passing CI never makes a milestone SHIPPED without owner acceptance.

## Gameplay at a glance

| Milestone | What changes | Detailed requirements and verification |
|---|---|---|
| M01 | No sleeping through the night, insomnia phantoms, usable elytra or shulker-box storage. | [M01](docs/MILESTONE_01.md) |
| M02 | Different ore and stone generation, iron-tier deepslate mining, smaller inventory, heavy-commodity limit, inventory sorting, hunger and lighting adjustments, more dangerous mobs. | [M02](docs/MILESTONE_02.md) |
| M03 | Ordinary minecarts carry up to **256 of one cargo type**; a fitted cushion converts a cart to passenger use. Retired portable-storage shortcuts stay unavailable. | [M03](docs/MILESTONE_03.md) |
| M04 | Cable drums, timber-mounted anchors, hand-laid cable and cart-mounted connecting rods. | [M04](docs/MILESTONE_04.md) |
| M05 | Moving cable loops and hand cranks; furnace-powered Cobblestone Engines and chain-coupled trains. | [M05](docs/MILESTONE_05.md) |
| M06 | Loading Rails hold and release trains; coal upgrades a drum to a powered winch; one coil supports an arbitrarily long **supported** line. Redstone rails retire and uncushioned carts resist player shoving. | [M06](docs/MILESTONE_06.md) |
| M07 | Upright **512-item ore skips**, physical cart-to-skip transfers, four opening cart gates, improved cable grips, and optional **two-drum, one-cable** lines controlled from either end. | [M07](docs/MILESTONE_07.md) |
| M08 | A large Blast Furnace turns raw metals into storage blocks, with visible casting, skip/cart intake and commodity-aware carrying rules. | [M08](docs/MILESTONE_08.md) |
| M09 | Hostile light loitering and breach response, creeper retaliation, rare naturally recruited skeleton–spider jockeys. | [M09](docs/MILESTONE_09.md) |
| M10 | Headlamp, inventory-expanding Overalls, two mining-boot paths and set bonuses, hanging rope, and retirement of gold/chainmail armor. | [M10](docs/MILESTONE_10.md) |
| M11 | Material-specific cart loads at four visible fill heights, gradual hand/hopper freight transfers, side-gate unloading, and removal of mod-authored floating/action-bar text. | [M11](docs/MILESTONE_11.md) |
| M12 | Four permanent **Monster Effigies** (zombie, skeleton, creeper, enderman) built from nine full stacks of a mob drop per block; placed through a confirmation screen, they send their own monster fleeing a 100-block forward cone. | [M12](docs/MILESTONE_12.md) |
| M13 | Planned bug-fixing and refinement patch: offhand rope bow launch, night-only bed healing paid from hunger (retaining vanilla nearby-monster refusal), Shift + right-click drag transfer, and one-click chest sorting. | [M13](docs/MILESTONE_13.md) |
| M14 | Planned Old Coot: rare 1¼-block-tall bearded, villager-nosed mineshaft miner spawning at any light level, excavating stone, swinging his pickaxe, and protecting ore and cave spiders. | [M14](docs/MILESTONE_14.md) |
| M15 | Planned **three-position physical lever on the back of the Cobblestone Engine**, cycled with Space while riding a coupled passenger cart; Through/Load/Unload mode controls when freight trains dock at Loading Rails. | [M15](docs/MILESTONE_15.md) |

**Additional visual work:** Ordinary vanilla rails now use [3D models](docs/BACKLOG.md#i-013--3d-ordinary-rails) matching the Loading Rail profile; the block's physics, IDs, drops and crafting are unchanged. See [editable Blockbench sources](art/blockbench/README.md).

**Where details belong:** The table above is an overview, **not a second specification**. Milestones own exact behavior and verification; [BUG_LOG](docs/BUG_LOG.md) owns bug diagnoses; [UX_TEXT_LOG](docs/UX_TEXT_LOG.md) tracks text removal and replacement feedback; [Joe's painting queue](docs/Joe's%20To-Do/BLOCKBENCH_PAINTING.md) tracks artwork awaiting approval.

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

`src/gametest` contains **server game tests** (mechanics, regressions and inventory/cargo conservation) and **client game tests** (real rendering, interaction and screenshots, including dedicated-server scenarios). See each milestone's dated verification appendix and [evidence](docs/evidence/INDEX.md) for scoped results and negative controls. Green tests are not a substitute for owner playtesting.

<!-- GENERATED SOURCE INVENTORY START -->
## Generated Java source inventory

Counts reflect source files only, not proof of implemented or tested behavior.

| Source set | First-level package | Java files |
|---|---|---:|
| client | client | 29 |
| gametest | gametest | 60 |
| main | (root) | 1 |
| main | cable | 18 |
| main | effigy | 9 |
| main | feature | 27 |
| main | metallurgy | 12 |
| main | mixin | 62 |
| main | network | 4 |
| main | rope | 7 |
| main | skip | 7 |
| main | train | 9 |
| main | tuning | 1 |
| main | workwear | 4 |
| main | worldgen | 7 |

Regenerate using tools/docs/automation.py readme --write
<!-- GENERATED SOURCE INVENTORY END -->
