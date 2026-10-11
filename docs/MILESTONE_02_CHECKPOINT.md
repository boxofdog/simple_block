# Milestone 02 checkpoint (continuation note)

Written at the end of a session that ran out of context. **Read this first when resuming Milestone 02**, then continue with "Next steps". Do not redo finished phases and do not widen the scope beyond `docs/MILESTONE_02.md`.

State at the checkpoint: `main` builds and passes all **48 server game tests** (`./gradlew build`). The real-client test passes. CI Build runs #4 (02A) and #5 (02B) published updater-installable artifacts; the checkpoint commit's run should be checked too.

## Done and verified

| Phase | Items | Verification | Evidence |
|---|---|---|---|
| 00 | Audit and baseline | Baseline `./gradlew clean build`: 22/22 tests, client smoke test passed | `docs/evidence/milestone-02/baseline-build.txt` |
| 02A | GEO-01, MINE-01, ORE-01, ORE-02 | Game tests (`GeologyGameTests`, `MiningGameTests`, `WorldgenGameTests`); negative control (8 expected failures); 8-seed normal-world sampling vs a worktree of `a3f53d3` | `ore-sampling/ore-sampling-summary.md`, `negative-controls.md` |
| 02B | INV-01..04 | Game tests (`InventoryGameTests`, `HeavyLoadGameTests`); negative control (10 expected failures); real-client step with screenshots (27 slots on screen, Sort via the packet, indicator agrees with the server, red at 64) | `inventory-and-weight/`, `gametest-evidence.md`, `negative-controls.md` |

Key measured results (02A): diamonds 0.354× vanilla (per seed 0.33–0.38). Ordinary stone iron 0.70× vanilla. Deepslate is 1.4–1.8× stone and deep-ocean deepslate is 2.0–2.7× ordinary deepslate on every seed. Removed rock: 0 (control 1.2 M per seed). Trial chambers convert 1:1 to deepslate bricks with all spawners and vaults intact. Ore veins keep their ore and raw blocks, minus the filler.

Also done: the `CLAUDE.md` scope was updated narrowly and the README has 02A/02B rows. The tuning table moved to an overrides-only `config/simple_block/tuning.json` plus a regenerated `tuning-defaults.json`. The night-test flakes were fixed in `GameTestTiming` (wait a tick; set midnight when the batch clock is not in effect). A MINE-01 bug was fixed: a refused break no longer jams vanilla's delayed destroy (`ServerPlayerGameModeMixin`).

## In progress: 02C (inactive code committed, NOT wired, NOT tested)

These files compile but do nothing yet: their mixins are **not** listed in `simple_block.mixins.json` and nothing calls them.

- `feature/LightLevels.java` + `mixin/light/BlockStateBaseMixin.java`: torch, wall torch, copper torch and copper wall torch emit 12. The lantern is already 15 in vanilla and is left alone.
- `feature/Entrancement.java` + `mixin/mobs/RandomStrollGoalMixin.java` (MOB-01):
  - Hooks the destination returned by `getPosition()` inside `RandomStrollGoal.canUse`.
  - Applies to `Enemy` mobs with no target, at Overworld night (clock 13000–23000).
  - At each wander decision it samples 24 positions within the locked 20-block radius where block light exceeds the dimension's monster-spawn limit, and remembers up to 8 lit spots.
  - It then picks one at random, walks towards it with `LandRandomPos.getPosTowards`, and has a 15% chance of losing interest.
- New tuning keys in `Tuning.java`: `mob.entrancement.{enabled,samples,memory,lose_interest_chance,step}` and `mob.creeper.evaporation_ticks` (10).

## Next steps

### 02C (finish)

1. **Wire LIGHT-01 and MOB-01.**
   - Add `"light.BlockStateBaseMixin"` and `"mobs.RandomStrollGoalMixin"` to `simple_block.mixins.json`.
   - List both in the `SimpleBlock` Javadoc and the README table.
2. **MOB-02 zombie doors** (design settled, verified against the 26.3 sources).
   - Family: `Zombie`, `Husk` and `ZombieVillager` share `Zombie.addBehaviourGoals`. Exclude `Drowned` (amphibious navigation) and `ZombifiedPiglin` (neutral Nether mob); both override `addBehaviourGoals`. Document the exclusions.
   - Mixin `Zombie.setCanBreakDoors` to always pass `false`, so vanilla's random Hard-mode door breakers (passive vandalism through `MoveThroughVillageGoal`) are gone.
   - Inject at TAIL of `Zombie.addBehaviourGoals` to add a `BreakDoorGoal` subclass at priority 1:
     - predicate: difficulty is not peaceful; break time is vanilla's `DEFAULT_DOOR_BREAK_TIME` of 240;
     - `canUse`/`canContinueToUse` also require `getTarget() instanceof Player`;
     - only `#minecraft:wooden_doors`. Vanilla `DoorBlock.isWoodenDoor` also accepts copper doors because they open by hand.
   - Each tick, set `navigation.setCanOpenDoors(target is Player)`.
   - Add a metal-door goal: while pursuing and within about 2 blocks of a closed non-wooden `DoorBlock`, swing and play `levelEvent(1020)` (`SOUND_ZOMBIE_IRON_DOOR`) every ~20–40 ticks, never damaging the door.
   - `BreakDoorGoal` already respects the `mob_griefing` game rule.
3. **MOB-03 creepers.**
   - In a `Creeper.tick` mixin (server side), add a `@Unique` countdown.
   - Start it under the same conditions as `Mob.isSunBurnTick`: environment attribute `MONSTERS_BURN` at the position, `getLightLevelDependentMagicValue() > 0.5`, the random onset `nextFloat()*30 < (br-0.4)*2`, not in water/rain/powder snow, and `canSeeSky(eye pos)`.
   - When it starts: `setSwellDir(-1)`, stop navigation, and freeze the AI.
   - For `mob.creeper.evaporation_ticks` ticks, send smoke/ash/poof particles with `ServerLevel.sendParticles` and a soft fizz sound.
   - Then `discard()`: no explosion, no loot, no death event.
4. **Tests** (new `MobGameTests`, own test environments so each runs alone in its batch; force-load the chunks used with `setChunkForced`; clean up mobs and blocks at the end).
   - Torch emits 12 and lantern 15 (`getLightEmission` and real block light next to a placed torch).
   - Statistical MOB-01:
     - about 20 persistent zombies midway between a torch-lit zone and an equal dark zone at night, 600 ticks, compare time spent within 6 blocks of each;
     - a walled lit enclosure vs a walled dark one (presence along the outside perimeter);
     - daytime with husks, which don't burn: no difference;
     - spread of the final positions;
     - pursuit: a survival player is targeted, and when they switch to creative the zombie loses the target and drifts back to the light;
     - tick time with 40 mobs, on vs off.
   - Use a creative player within 32 blocks, or wandering stops (vanilla's `noActionTime`).
   - Negative control: `Tuning.set("mob.entrancement.enabled", false)`.
   - Doors on Normal difficulty: pursuing zombie breaks oak/spruce doors in about 240 ticks; an iron door gets attacked but stays; an unaware zombie by a closed door with light behind it leaves it intact.
   - Creepers: direct sun → removed after the effect with no explosion or drops; roofed, torch-lit cave and night → survive. Client screenshot of the poof.
5. Run the negative control for the 02C restriction tests, build, commit, push and check CI.

### 02D

1. **FOOD-01.**
   - Halve sprint, jump, swim and mining exhaustion (locked 0.5, a constant) with `@ModifyArg` on the `causeFoodExhaustion` calls in `Player` (sprint, jump, swim) and `Block.playerDestroy`.
   - Leave combat (attack/hurt) and regeneration exhaustion unchanged.
2. **Saturation HUD.** Draw a saturation outline over the food bar with the Fabric HUD API (client), on by default.
3. **Tests.**
   - Measure exhaustion for scripted sprint/jump/swim/mine against the vanilla constants.
   - Check combat and regeneration costs are unchanged.
   - Client screenshots at common GUI scales.
4. **Final pass.**
   - Full regression (M01 tests and the client smoke test).
   - Write `docs/MILESTONE_02_REPORT.md`: the TEST_PLAN matrix with PASS/FAIL/NOT RUN for every row, the config keys and defaults (`tuning-defaults.json`), the decisions-needed list below, and launch and update commands.
   - Push, then confirm the CI Build is green and its artifact has the new `build-info.json`.

## Decisions flagged for the user (go in the report)

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
