# Milestone 02 checkpoint (continuation note)

Written at the end of a session that ran out of context. **Read this first when resuming Milestone 02**, then continue with "Next steps". Do not redo finished phases and do not widen the scope beyond `docs/MILESTONE_02.md`.

State: **Milestone 02 is complete.** `main` builds and passes all **63 server game tests** (`./gradlew clean build`), and the real-client test passes. The final report is `docs/MILESTONE_02_REPORT.md` (test matrix, configuration, decisions). CI Build runs #4 (02A), #5 (02B), #6 (checkpoint), #8 (02C) and #9 (02D and the report) published updater-installable artifacts; run #7 failed on a test-timing flake fixed in #8.

## Done and verified

| Phase | Items | Verification | Evidence |
|---|---|---|---|
| 00 | Audit and baseline | Baseline `./gradlew clean build`: 22/22 tests, client smoke test passed | `docs/evidence/milestone-02/baseline-build.txt` |
| 02A | GEO-01, MINE-01, ORE-01, ORE-02 | Game tests (`GeologyGameTests`, `MiningGameTests`, `WorldgenGameTests`); negative control (8 expected failures); 8-seed normal-world sampling vs a worktree of `a3f53d3` | `ore-sampling/ore-sampling-summary.md`, `negative-controls.md` |
| 02B | INV-01..04 | Game tests (`InventoryGameTests`, `HeavyLoadGameTests`); negative control (10 expected failures); real-client step with screenshots (27 slots on screen, Sort via the packet, indicator agrees with the server, red at 64) | `inventory-and-weight/`, `gametest-evidence.md`, `negative-controls.md` |

Key measured results (02A): diamonds 0.354× vanilla (per seed 0.33–0.38). Ordinary stone iron 0.70× vanilla. Deepslate is 1.4–1.8× stone and deep-ocean deepslate is 2.0–2.7× ordinary deepslate on every seed. Removed rock: 0 (control 1.2 M per seed). Trial chambers convert 1:1 to deepslate bricks with all spawners and vaults intact. Ore veins keep their ore and raw blocks, minus the filler.

Also done: the `CLAUDE.md` scope was updated narrowly and the README has rows for every finished item. The tuning table moved to an overrides-only `config/simple_block/tuning.json` plus a regenerated `tuning-defaults.json`. The night-test flakes were fixed in `GameTestTiming` (wait a tick; set midnight when the batch clock is not in effect). A MINE-01 bug was fixed: a refused break no longer jams vanilla's delayed destroy (`ServerPlayerGameModeMixin`).

## Done: 02C (light and hostile mobs)

| Items | Verification | Evidence |
|---|---|---|
| LIGHT-01, MOB-01, MOB-02, MOB-03 | 13 game tests (`MobGameTests`), all passing in the 61-test build; negative control (8 expected failures); real-client step: client light 12/15 and creeper poof screenshots | `gametest-evidence.md` (02C), `negative-controls.md` (02C), `mobs-and-light/` |

What was built:
- **LIGHT-01:** torches, wall torches and copper torches emit 12 (`mixin/light/BlockStateBaseMixin`). Lanterns are unchanged at 15.
- **MOB-01:** at Overworld night (clock 13000–23000), ground-navigating `Enemy` mobs with a `RandomStrollGoal` get their wander destination redirected towards remembered lit spots (block light > 0) within 20 blocks.
  - Memory drifts towards brighter spots; a random spot is chosen each time, so mobs spread out.
  - Within a step of a spot, the mob walks to the spot itself. Vanilla's own random positions favour darkness for monsters and kept mobs at the dim edge.
  - An unreachable spot (light behind walls) gives a partial path, so the mob loiters outside.
  - 15% lose-interest chance per wander decision. The audit list is in the `Entrancement` Javadoc.
- **MOB-02:** zombies, husks and zombie villagers. Vanilla `setCanBreakDoors` is forced off.
  - A `BreakDoorGoal` subclass (240 ticks, every difficulty except Peaceful) breaks only `#minecraft:wooden_doors`, and only while pursuing a player.
  - "Pursuing" means the target is a player now, or was within the last 300 ticks; vanilla forgets a player about 3 s after a door closes.
  - While breaking with no target, the zombie stays at the door.
  - Paths may open doors only while pursuing. A metal-door goal swings and bangs, and never damages the door.
- **MOB-03:** a creeper meeting vanilla's sun-burn test stops (fuse out), smokes for `mob.creeper.evaporation_ticks` (10) and is discarded: no explosion, no drops. Rain protects it, as it protects zombies.

## Done: 02D (hunger and saturation display)

| Items | Verification | Evidence |
|---|---|---|
| FOOD-01, saturation display | 2 game tests (`FoodGameTests`: activities ×0.50, combat/regeneration/Hunger ×1.00); negative control; real-client step (saturation sync after eating/exertion/regeneration, a real sprint at 0.049 per block, screenshots at GUI scales 1–3, F1, creative) | `gametest-evidence.md` (02D), `negative-controls.md` (02D), `food-and-hud/` |

## Next steps

Milestone 02 needs nothing more from the agent. Next:
1. The user playtests in a new world.
2. The user settles the decisions in the report (section 6).
3. The user authorises the next milestone.

Do not start Milestone 03 work without that.

## Decisions flagged for the user (go in the report)

02C additions:

- **Copper doors:** they count as metal doors (attacked, never broken), although vanilla's "wooden door" check (`DoorBlock.isWoodenDoor` = opens by hand) includes them. Wooden means `#minecraft:wooden_doors`.
- **Pursuit memory:** a zombie counts as chasing for 300 ticks after it last had a player target, so a door shut in its face still gets broken (vanilla forgets the player after about 3 s). Paths through doors (opening them) are also only allowed during a chase.
- **Excluded from the attraction:**
  - Flying and water-bound mobs (the Wither, guardians), because land destinations would break their movement.
  - Mobs whose AI has no wander goal (slimes, phantoms, ghasts, wardens, piglins and others).
  - Drowned *are* attracted.
- **Rain:** rain and thunderstorms protect creepers from evaporating, exactly as they protect zombies from burning.
- **Exhaustion factor:** `M02_HUNGER_HUD.md` asks for a config key for it, but `M02_DECISIONS_AND_TUNING.md` locks 50% for the first balance pass. It will be a constant (the CLAUDE.md rule for locked values); turning it into a tuning key is a one-line change.

02A/02B:

1. Heavy accounting is in 1/216 ingot (rails 3/8, hanging signs 11/27) instead of ninths. The capacity is the same 64.
2. Any spawn-preventing block light counts for attraction, including lava, glow lichen and so on.
3. Crafted rock variants in structures keep one palette per structure: tuff → deepslate family, granite/diorite/andesite → stone family. Raw rock follows depth.
4. The copper torch gets light 12, like the ordinary torch.
5. Without an iron-or-better pickaxe, deepslate and deepslate ores can't be broken at all, rather than breaking with no drops. `infested_deepslate` is not gated.
6. The mason's level-3 trades shrink to the dripstone trade only.
7. The `notes` in `heavy_weights.json` need decisions: loot-only gear weighs 1/9, netherite counts as its 4 gold, golden food counts its gold, maps count their compass, statues count a copper block. Silk-touched ore weighs 1 per the spec, but Fortune can turn that into more.
8. Ocean-floor stone iron is 0.45–0.5× vanilla (vanilla concentrates iron around y 16, under the ocean floor). The ordinary-biome stone target of 0.70× is met.

## Environment notes (this cloud container)

- JDK: `/opt/jdk/jdk-25.0.4.1+1`.
- Client tests: `XDG_RUNTIME_DIR=/tmp/claude-0/xdg xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest`.
- Ore sampling: `tools/ore-sampling/` (run_sampler.sh, analyze.py, composite.py). The control is `git worktree add --detach <dir> a3f53d3`, plus the sampler file, the gametest `fabric.mod.json` main entrypoint and the `oreSampler` run block in `build.gradle`.
- The decompiled 26.3 sources come from `./gradlew genSources`; the previous session's scratch copies are gone with the container.
