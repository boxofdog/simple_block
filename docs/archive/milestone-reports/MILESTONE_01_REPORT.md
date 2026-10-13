# Milestone 01 report: Survival Foundation

**HISTORICAL REPORT (M01–M07 format).** This record is preserved for its original implementation and tests. References to the retired decisions ledger now point to the dated archive, **not active implementation authority**. Under the current [roadmap rule](../../ROADMAP.md), new authorized work appends only a **concise dated implementation/verification note at the bottom of the main milestone document**, not a new standalone report.

Completed 2026-10-11. Test status: 22 PASS / 0 FAIL / 1 NOT RUN (see [`TEST_CHECKLIST_01.md`](../../TEST_CHECKLIST_01.md)).

## 1. Baseline findings

- The repository was empty apart from `README.md` and an MIT `LICENSE`: one commit, clean tree, no build, loader, version or source. There was nothing to build as a baseline.
- So a toolchain was selected rather than discovered: **Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25**. Reasons are in [`docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` (historical)](../DECISIONS_PRE_RETIREMENT_2026-10-13.md).
- First build of the unmodified scaffold: success.

## 2. Files and why

| Path | Purpose |
|---|---|
| `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew*`, `gradle/wrapper/` | Fabric build with split client/common sources (server code can't reference client classes) and the game-test source set. |
| `src/main/resources/fabric.mod.json`, `simple_block.mixins.json` | Mod metadata; one mixin config listing each feature's mixins. |
| `src/main/java/.../SimpleBlock.java` | Entrypoint; registers the elytra listener and documents the feature map. |
| `.../mixin/sleep/ServerLevelMixin.java` | SF-01 |
| `.../mixin/phantom/PhantomSpawnerMixin.java` | SF-02 |
| `.../feature/NoElytraFlight.java`, `.../mixin/elytra/EndCityPieceMixin.java` | SF-03 |
| `.../feature/ShulkerBoxes.java`, `.../mixin/shulker/{BlockItem,ShulkerBoxBlock,ShulkerBoxBlockEntity}Mixin.java` | SF-04 code |
| `src/main/resources/data/minecraft/recipe/*shulker_box.json` (17), `.../advancement/recipes/decorations/*shulker_box.json` (17) | SF-04 recipe and recipe-book removal |
| `src/gametest/**` | Server game tests (one class per feature plus a data audit and regression checks), the client smoke test, test environments (night, thunder, etc.) |
| `CLAUDE.md`, `docs/*` | The handoff package, the filled-in checklist, decisions, this report, evidence |

## 3. Implementation summary

- **Sleeping (SF-01).** `ServerLevel.canSleepThroughNights()` always returns false, and the tick-time skip is gated on it. This is the code path vanilla already uses when `players_sleeping_percentage` is above 100. Beds still set spawn; players can lie down, see "No amount of rest can pass this night", and leave with the Leave Bed button or at morning. Time and weather are never skipped. Nether/End beds are untouched (they explode before this code is reached).
- **Phantoms (SF-02).** `PhantomSpawner.tick` is cancelled. That spawner is the only survival source of phantoms. The entity stays registered.
- **Elytra (SF-03).** Fabric's `EntityElytraEvents.ALLOW` returns false. The server checks it when a client asks to start gliding and on every gliding tick, so injected or creative elytra can't glide. End ship generation skips the "Elytra" data marker, so no item frame is created. No loot table or trade contains elytra (audited).
- **Shulker boxes (SF-04).** The 17 recipes and their recipe-book advancements are removed with Fabric `fabric:false` load conditions. `BlockItem.place` refuses any `ShulkerBoxBlock`, which covers players and dispensers. Existing boxes never open (`useWithoutItem`) and expose no slots to hoppers (`getSlotsForFace`). Players get an action-bar message. Shulker mobs and shells are untouched.

Mixins are used where Fabric API has no hook. Each mixin's Javadoc explains why.

## 4. Tests and limitations

- **Server game tests:** 22 required tests pass (21 project tests + vanilla's built-in one) in `./gradlew build`.
- **Negative control:** the same suite against a copy with all restrictions removed. All 12 restriction tests fail on vanilla behaviour, e.g. 63 phantoms spawn, the night skips about 6,000 ticks, an elytra frame generates, and boxes craft, place, open and work with hoppers. This shows the tests detect what they claim to.
- **Client smoke test:** passes in the real client with a fresh survival world and screenshots. Bed at midnight, shulker box refused, normal walk/jump/fall damage, and no gliding with a command-given elytra.
- **Not run:**
  - ENV-04: dedicated server with a remote client.
  - Visiting a naturally generated End city by playing.
  - Swimming and boats.
  - A desktop-GPU client session (the automated client run used software Vulkan under Xvfb).
- **Raw output:** [`evidence/milestone-01/test-output.txt`](../../evidence/milestone-01/test-output.txt) plus screenshots.

## 5. Build and run

See the [README](../../../README.md#commands). In short:

- `./gradlew build`: jar plus server game tests.
- `./gradlew runClient`: play.
- `./gradlew runServer`: dedicated server.
- `./gradlew runClientGameTest`: client smoke test.

## 6. Remaining issues

- **Side effects to decide on** (listed in `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` (historical)): Slow Falling potions, two phantom advancements and "Sky's the Limit" become unobtainable, and shulker shells become useless.
- **Bed UX:** a player who lies down sees the dimmed sleep screen until they press Leave Bed or morning comes (vanilla's behaviour in this mode). The no-lie-down alternative is a small change.
- **Gliding:** blocking applies to every glider, including any a future mod adds.
- **Vanilla clients:** a vanilla client on a modded server may briefly see a "ghost" shulker box before the server removes it. Untested, since ENV-04 was not run.
- **Dev warning:** the empty client source set makes the dev launcher warn about missing `build/classes/java/client`. It's harmless and goes away once client code exists.
- **Mod ID:** `simple_block` is provisional.

## 7. Recommended next milestone

Keep the same small-and-isolated workflow for one more round before the first custom content (the freight cart, per `docs/archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md` (historical)). Candidates are **DECIDED** entries that need no assets:

- **S-06:** creepers vanish in direct sunlight.
- **G-02:** deepslate needs an iron pickaxe or better.
- **U-02:** shift-transfer fills hotbar slots 1→2→3.
- **S-05:** torches dimmer, lanterns brighter. Needs exact light values decided first.

S-04 (monsters drawn to lit areas) and the freight cart are bigger designs and should each get their own milestone.
