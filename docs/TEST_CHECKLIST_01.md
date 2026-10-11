# Milestone 01 — QA Checklist

Mark each check **PASS**, **FAIL**, or **NOT RUN**; give the game version, method, and relevant evidence. Treat unknown as NOT RUN, never PASS.

| Test | Expected behavior | Status | Evidence / notes |
|---|---|---|---|
| ENV-01 | Baseline repo audited; versions and Git status recorded | NOT RUN | |
| ENV-02 | Gradle build completes on documented JDK | NOT RUN | |
| ENV-03 | Dev client launches with mod enabled | NOT RUN | |
| ENV-04 | Dedicated server starts with mod; remote client joins (if available) | NOT RUN | |
| BED-01 | Overworld bed sets respawn on valid interaction | NOT RUN | |
| BED-02 | Bed interaction at night does not advance day/night clock | NOT RUN | |
| BED-03 | All players attempting to sleep cannot skip night | NOT RUN | |
| BED-04 | Storm cannot be skipped by sleeping | NOT RUN | |
| BED-05 | Nether and End beds retain vanilla behavior | NOT RUN | |
| BED-06 | Player respawns at properly established bed after death | NOT RUN | |
| PHA-01 | Repeated nights without sleeping do not create phantom spawns | NOT RUN | |
| PHA-02 | Other hostile mob spawning still works | NOT RUN | |
| ELY-01 | Elytra not obtainable through ordinary survival progression (End ship/source test) | NOT RUN | |
| ELY-02 | Command-injected or creative-equipped elytra cannot initiate/maintain gliding | NOT RUN | |
| ELY-03 | Other movement remains normal | NOT RUN | |
| SHU-01 | No uncolored shulker-box crafting in normal survival | NOT RUN | |
| SHU-02 | No dyed shulker-box crafting/acquisition bypass | NOT RUN | |
| SHU-03 | Shulker-box item cannot be used as portable storage | NOT RUN | |
| SHU-04 | Placed boxes cannot provide a functional survival storage loophole | NOT RUN | |
| SHU-06 | Shulker mobs still behave normally | NOT RUN | |
| REG-01 | All tests use new disposable worlds; no migration or old-save tests required | NOT RUN | |
| REG-02 | Unrelated standard recipes/items/entities remain usable | NOT RUN | |
| SCOPE-01 | No worldgen, train, cable, custom asset, or redstone work accidentally included | NOT RUN | |

## Suggested manual QA setup

- **World A:** newly created disposable survival world, preferably with commands enabled for time/weather verification.
- **World B:** newly created disposable test world for injecting elytra and all shulker-box variants through commands or creative mode. No pre-existing save required.
- **Server:** optional dedicated development server for checking that restrictions aren't client-only.
- Test elytra both through normal world content and by injecting an existing item. Test shulker boxes across all color variants, not just the default.

## Final implementation report template

**Build/runtime:** Minecraft __; loader __; JDK __; build command __; dev-client result __; dedicated-server result __.

**Implementation:** SF-01 __; SF-02 __; SF-03 __; SF-04 __.

**Tests:** __ PASS / __ FAIL / __ NOT RUN. Important gaps: __.

**Fresh-world-only confirmation:** __.

**Changed files:** __.

**How to reproduce:** __.

**Known issues / next action:** __.
