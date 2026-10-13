# Milestone 01 — QA Checklist

Mark each check **PASS**, **FAIL**, or **NOT RUN**; give the game version, method, and relevant evidence. Treat unknown as NOT RUN, never PASS.

| Test | Expected behavior | Status | Evidence / notes |
|---|---|---|---|
Results recorded 2026-10-11 on Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Temurin JDK 25.0.4.1 (Linux).

Methods: **GT** = automated server game test (`./gradlew runGameTest`, also part of `./gradlew build`) in a newly created world with real server players. **CT** = automated end-to-end test in the real game client (`./gradlew runClientGameTest`) in a newly created survival world, driven by simulated keyboard/mouse input. **NC** = negative control: the same GT suite run once against a copy with every restriction removed. A test that fails there is shown to detect the vanilla behaviour. Raw output and screenshots are in [`evidence/milestone-01/`](evidence/milestone-01/).

| Test | Expected behavior | Status | Evidence / notes |
|---|---|---|---|
| ENV-01 | Baseline repo audited; versions and Git status recorded | PASS | Repo had only `README.md` and `LICENSE` (MIT), one commit, clean tree. No existing loader, version or build, so a toolchain was selected (see `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` (historical)). |
| ENV-02 | Gradle build completes on documented JDK | PASS | `./gradlew clean build` on Temurin 25.0.4.1: BUILD SUCCESSFUL, `simple_block-0.1.0.jar` (22 KB). The build also runs the 22 server game tests. |
| ENV-03 | Dev client launches with mod enabled | PASS | CT: client started with `simple_block 0.1.0` loaded, created a fresh survival world and rendered it. Run headless under Xvfb with Mesa's software Vulkan driver; not tried on a desktop GPU. |
| ENV-04 | Dedicated server starts with mod; remote client joins (if available) | NOT RUN | Skipped by agreement. Partial evidence: the GT suite runs in the server environment (`Env=SERVER`, server jar, no client classes) with the mod loaded. To run: `./gradlew runServer` (set `eula=true` in `run/eula.txt`), then `./gradlew runClient` → Multiplayer → `localhost`. |
| BED-01 | Overworld bed sets respawn on valid interaction | PASS | GT `allPlayersSleepingDoNotSkipNight`: respawn = bed head position and dimension for both players. CT: "Respawn point set" shown, respawn at the bed. |
| BED-02 | Bed interaction at night does not advance day/night clock | PASS | GT: clock 18002 → 18142 after 140 ticks in bed. CT: player in bed at midnight, clock 18008 → 18170 after 160 ticks, "No amount of rest can pass this night" shown, Leave Bed works. NC: clock jumped ~6000 ticks to morning. |
| BED-03 | All players attempting to sleep cannot skip night | PASS | GT: 2 of 2 players asleep and past vanilla's 100-tick threshold (`isSleepingLongEnough`), not woken, clock advanced normally. NC: vanilla skipped the night. |
| BED-04 | Storm cannot be skipped by sleeping | PASS | GT, night thunderstorm and daytime thunderstorm: after 140 ticks asleep it is still raining and thundering, clock advanced normally. NC: vanilla skipped (clock +6004 and +17899). |
| BED-05 | Nether and End beds retain vanilla behavior | PASS | GT `netherBedStillExplodes`, `endBedStillExplodes`: bed is destroyed, no sleep, no respawn set. Passes with and without the mod. |
| BED-06 | Player respawns at properly established bed after death | PASS | GT `respawnAtBedAfterDeath`: after death and respawn the player is within 3 blocks of the bed. |
| PHA-01 | Repeated nights without sleeping do not create phantom spawns | PASS | GT: survival player awake for 10 days, open sky, midnight, hard difficulty, 96,000 phantom-spawner ticks (≈4 in-game days of attempts): 0 phantoms. NC: 63 phantoms. Simulated spawner ticks, not 4 real-time nights. |
| PHA-02 | Other hostile mob spawning still works | PASS | GT: vanilla natural spawner around a player at midnight produced 476 monsters (zombie, husk, parched, skeleton, spider, creeper, enderman, witch) and no phantoms. |
| ELY-01 | Elytra not obtainable through ordinary survival progression (End ship/source test) | PASS | GT: real End ship generated from the vanilla template (557 purpur blocks, Elytra data marker present): 0 elytra frames (NC: 1). GT audit: 1447 loot tables and 471 villager trades contain no elytra. A natural End city reached by playing was not visited. |
| ELY-02 | Command-injected or creative-equipped elytra cannot initiate/maintain gliding | PASS | GT: the start-gliding packet from survival and creative players wearing an elytra mid-air is refused; a forced gliding state is ended on the next server tick (NC: both glide). CT: elytra via `/item`, jump pressed 3× mid-air: never gliding. |
| ELY-03 | Other movement remains normal | PASS | CT: walked 4.06 blocks in 20 ticks, jumped 1.25 blocks, a 10-block fall did 7 damage (20 → 13 health), as in vanilla. Swimming and boats were not exercised. |
| SHU-01 | No uncolored shulker-box crafting in normal survival | PASS | GT: recipe `minecraft:shulker_box` absent; shell/chest/shell crafts nothing; recipe-book advancement absent. NC: recipe exists. |
| SHU-02 | No dyed shulker-box crafting/acquisition bypass | PASS | GT: all 16 dyed recipe ids absent; 272 box+dye combinations craft nothing; all 2025 loaded recipes scanned, none produce any shulker box; loot/trade audit finds none. |
| SHU-03 | Shulker-box item cannot be used as portable storage | PASS | GT: all 17 variants refused for survival and creative players (item kept); dispenser cannot place one (NC: placed). CT: right-click shows "Shulker boxes don't work in this world", nothing placed. |
| SHU-04 | Placed boxes cannot provide a functional survival storage loophole | PASS | GT: command-placed boxes of all 17 variants don't open; hoppers neither drain nor fill them (NC: opened; hopper drained 5 diamonds). |
| SHU-06 | Shulker mobs still behave normally | PASS | GT: shulker spawns, survives, takes damage; loot table still drops shells; End ship sentries (3) still spawn. |
| REG-01 | All tests use new disposable worlds; no migration or old-save tests required | PASS | Every GT and CT run creates a fresh world (run directory cleared). No migration or legacy-save code exists. |
| REG-02 | Unrelated standard recipes/items/entities remain usable | PASS | GT: chest, crafting table, bed, minecart and hopper still craft; elytra, shulker box items, phantom and shulker entity types still registered. |
| SCOPE-01 | No worldgen, train, cable, custom asset, or redstone work accidentally included | PASS | Diff review: main source is 9 Java files plus 34 data overrides for the four features. The only world-generation touch is skipping the End ship's elytra item frame (SF-03). No assets, no config system. |

**Totals:** 22 PASS / 0 FAIL / 1 NOT RUN (ENV-04).

## Suggested manual QA setup

- **World A:** newly created disposable survival world, preferably with commands enabled for time/weather verification.
- **World B:** newly created disposable test world for injecting elytra and all shulker-box variants through commands or creative mode. No pre-existing save required.
- **Server:** optional dedicated development server for checking that restrictions aren't client-only.
- Test elytra both through normal world content and by injecting an existing item. Test shulker boxes across all color variants, not just the default.

## Final implementation report template

**Build/runtime:** Minecraft 26.3; loader Fabric Loader 0.19.5 + Fabric API 0.162.0+26.3; JDK Temurin 25.0.4.1; build command `./gradlew build`; dev-client result PASS (headless, Vulkan/lavapipe); dedicated-server result NOT RUN (server environment exercised by the game tests).

**Implementation:** SF-01 night/storm skip disabled through vanilla's "nobody can sleep through the night" path, beds still set spawn; SF-02 phantom insomnia spawner disabled; SF-03 gliding refused server-side, End ship elytra frame removed; SF-04 17 recipes removed, placement/opening/hopper access refused.

**Tests:** 22 PASS / 0 FAIL / 1 NOT RUN. Important gaps: no dedicated server + remote client session; no natural End city visited by playing; swimming and boats not exercised.

**Fresh-world-only confirmation:** Yes. No migration, compatibility or recovery code. All tests used newly created worlds.

**Changed files:** see [`MILESTONE_01_REPORT.md`](archive/milestone-reports/MILESTONE_01_REPORT.md).

**How to reproduce:** `./gradlew build` (server game tests) and `./gradlew runClientGameTest` (client test; under Xvfb on a headless machine). See `README.md`.

**Known issues / next action:** see [`MILESTONE_01_REPORT.md`](archive/milestone-reports/MILESTONE_01_REPORT.md).
