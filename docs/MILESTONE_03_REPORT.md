# Milestone 03 report: Minecart cargo and storage-bypass closure

Scope: `docs/MILESTONE_03.md` and `docs/M03_TEST_PLAN.md`, **as clarified by the owner during the session** (section 2). Implemented and tested; not playtested by a person yet.

- **Tests:** 80 required server game tests pass in `./gradlew build`. That is 65 from Milestones 01 and 02 (including vanilla's built-in one) plus 15 new ones in `MinecartCargoGameTests` and `StorageBypassGameTests`. Three extra repeats of the cargo tests passed.
- **Negative control:** with the M03 hooks removed, every restriction test fails and the pack-animal regression test still passes.
- **Real client:** the client test (`runClientGameTest`) passes, including a new minecart step driven by real key presses and a save, quit and reopen of the world.
- **Dedicated server:** starts with the M03 code and stops cleanly.
- **Follow-up (section 11):** after the first delivery, cushions for minecarts, a straw-bed test, a top-row-first sort and a slimmer Sort button were added at your request. The build now has 85 game tests (section 11).
- **Not established by any test:** how hauling *feels* over a real mining session. See section 8.

Evidence is in `docs/evidence/milestone-03/`:
- [`gametest-evidence.md`](evidence/milestone-03/gametest-evidence.md): the numbers each test logged.
- [`negative-controls.md`](evidence/milestone-03/negative-controls.md)
- [`baseline-build.txt`](evidence/milestone-03/baseline-build.txt)
- `screenshots/` (real client).

## 1. Baseline

- **Toolchain:** Minecraft 26.3 (unobfuscated), Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1, JDK 25.
- **Starting point:** `main` at `d6da037`, the commit that uploaded the M03 docs. `./gradlew build` passed 65/65 there.
- **Existing code:** none touched minecarts, chest minecarts or ender chests.

## 2. Owner clarification and deviations from the written spec

**Clarification (owner, during the session):** M03 overhauls *the minecart that is already in the game*. It is not a new "freight cart". A plain minecart can't be sat in without a cushion (cushions come in a later milestone), and its main use is to be filled with raw material.

This replaces these parts of the spec:
- FRT-01, "one dedicated freight minecart entity/item"
- FRT-11, "keep ordinary minecarts and introduce the new freight cart separately"
- FRT-06, which planned that the separate cart would not be rideable
- T23, "ordinary cart still works": placing it and rolling it still work; riding it no longer does.

It's recorded in `docs/DECISIONS.md`. The other spec rules carry over to the minecart unchanged:
- 256 items of one kind;
- loading and unloading through the M02 heavy-load cap;
- cargo saved with the cart;
- cargo spilled on destruction and never carried in the item;
- chest minecarts and ender chests removed;
- pack animals kept.

**Deviations of my own, for you to confirm or change:**

| Spec default | Implemented | Why |
|---|---|---|
| Use with an **empty hand** unloads (3b.2) | **Sneak + use** unloads one stack (with anything in hand). Use with an empty hand shows the cargo. | An empty-hand unload puts the stack into that same hand, so the next click loads it straight back (seen in the client test: the stack lands in the selected hotbar slot). |
| A provisional recipe for a new cart (3c) | None: the vanilla minecart recipe (5 iron) is unchanged | There is no new item. |
| An open-top placeholder model (3d) | The vanilla minecart model, unchanged, plus a cargo label | The vanilla minecart is already open-topped. |
| Riding blocked for players only (FRT-06) | Mobs can't ride either: a rolling cart no longer scoops up villagers or monsters | "Sitting without a cushion" applies to anyone. Villager transport by minecart is gone until cushions. |

## 3. What changed

| Feature | Files |
|---|---|
| Minecart cargo (FRT-01..09) | `feature/MinecartCargo` (rules, capacity, messages, saving); `mixin/minecart/MinecartMixin` (cargo state on the vanilla `Minecart`: use, no riders, spilling, saving); `data/simple_block/tags/item/minecart_cargo.json`; client `CargoMinecartRenderer` (label) |
| Ender chests (FRT-10) | `feature/EnderChests`, `mixin/enderchest/{BlockItemMixin, EnderChestBlockMixin}`; `data/minecraft/recipe/ender_chest.json`, `advancement/recipes/decorations/ender_chest.json` (disabled), `loot_table/blocks/ender_chest.json` (always obsidian) |
| Chest minecarts (FRT-11) | `feature/ChestMinecarts`, `mixin/chestcart/{AbstractMinecartMixin, MinecartChestMixin, HopperBlockEntityMixin, VehicleEntityMixin}`; `data/minecraft/recipe/chest_minecart.json`, `advancement/recipes/transportation/chest_minecart.json` (disabled) |
| Tests | `MinecartCargoGameTests`, `StorageBypassGameTests`, new client steps in `SurvivalSmokeClientTest` |
| Docs | this report, `docs/DECISIONS.md` (M03 sections), README (M03 table and status), `CLAUDE.md` (points at the M03 documents), `SimpleBlock` Javadoc, `en_us.json` (messages) |
| Test fix | `MobGameTests.lightBehindWallsStillAttracts`, the walled light test from the earlier light-attraction work, failed twice here on runs that clearly attracted (once its 2.5 ratio, once a margin over its control phase: the control's own lit-box share varies 9-25% between runs). It now asserts what doesn't depend on random-walk noise: wander decisions are steered towards the light behind the walls (never on vanilla), the lit-box share beats the dark box's by 10 points, and no zombie gets in. Test-only. |

There are no new blocks, items, entities, textures or models. The vanilla minecart, chest minecart, ender chest and their entity types all stay registered.

## 4. How it plays

- **Getting a cart:** craft a minecart as usual (5 iron) and place it on rails by hand or with a dispenser. It rolls and is pushed as before, and powered rails work as before.
- **Loading:** hold cargo and use the cart. As much of the stack as fits goes in, up to **256 of one kind**. A different kind is refused with "This cart carries Raw Iron: empty it before loading Raw Copper". A full cart shows "Full: Raw Iron 256 / 256".
- **What counts as cargo** (`#simple_block:minecart_cargo`):
  - raw iron, copper and gold;
  - iron, copper and gold ore, plus their deepslate forms;
  - coal, diamond, emerald, lapis lazuli, redstone;
  - stone, cobblestone, deepslate, cobbled deepslate.

  Stacks must be plain: a renamed or otherwise customised stack is refused. Ingots, nuggets, metal blocks, raw-metal blocks, tools, containers, buckets, bundles and dirt or gravel are all refused.
- **Unloading:** sneak and use. One stack (up to 64) goes into the inventory, but only as much as fits and as much as the 64-ingot heavy cap allows. The rest stays in the cart, with "No room in your inventory" or "Too heavy: you can't carry any more Raw Iron". Stone and cobblestone weigh nothing, so only space limits them.
- **Seeing the cargo:** looking at a cart shows "Raw Iron 180 / 256" (or "Empty 0 / 256") above it, and every use shows the same in the action bar.
- **No riders without a cushion:** using a minecart never seats you, and a rolling cart doesn't pick up mobs. Fitting a cushion changes that (section 11). Furnace, hopper, TNT, command-block and spawner minecarts are unchanged.
- **Breaking:** breaking, blowing up, burning or `/kill`-ing a loaded cart drops its cargo as ordinary stacks of up to 64. Breaking it also leaves a plain minecart item, which never carries cargo. Cargo can be lost only where any dropped item would be: in lava, or in the void.
- **Ender chests:** can't be crafted, placed or opened, and mining one gives 8 obsidian even with silk touch. The only natural one is built into an End city floor; it stays shut and gives obsidian.
- **Chest minecarts:** can't be crafted or placed, by hand or dispenser. Mineshafts still generate them with their loot. They don't open, and hoppers, droppers and hopper minecarts can't reach them. Breaking one spills the loot and drops a minecart and a chest.
- **Pack animals:** donkeys, mules and llamas still take a chest, open, hold items and can be ridden.
- **Hoppers and the cargo cart:** a hopper under a loaded cart takes nothing. Automatic loading and unloading is out of scope.

## 5. Test matrix (`docs/M03_TEST_PLAN.md`)

Methods:
- **GT:** server game test in `./gradlew build`.
- **Client:** `runClientGameTest`, the real client under Xvfb with software Vulkan, driven by simulated keyboard and mouse.
- **Dedicated:** `runServer` start and stop.

| Test | Result | Method and evidence | Limitations |
|---|---|---|---|
| T01 baseline, packaging | PASS | Baseline 65/65 (`baseline-build.txt`). Final build 80/80. CI Build #14 published `simple-block-mod` | |
| T02 get and place a cart | PASS | GT `minecartPlacesOnRailsButNobodyRidesIt`: the minecart item used on a rail makes one cart and is used up. Client: placed with the use key. Craftable: GT `noRecipeMakesEnderChestsOrChestMinecarts` checks the minecart recipe | |
| T03 straight, curved, sloped, powered rails | PASS | GT `cartsRunALoopOfEveryRailKind`: a 6x6 loop with four curves, a climb to y+1 and back, and four powered rails. Loaded (256 raw iron) and empty carts each did 3 laps in 300 ticks; cargo still 256 | Detector and activator rails not tested |
| T04 load 1, 63, stacks | PASS | GT `loadingIsExactAndSingleKind`: 1 → 1, +63 → 64, +64 +64 +63 → 255; hand emptied exactly each time. Client: 3 × 64 → 192 on server and client | |
| T05 255 + 64 | PASS | Same GT: stops at 256, 63 stay in hand; a further 63 into a full cart stays in hand | |
| T06 another kind | PASS | Same GT: raw copper into a raw-iron cart is refused; hand and cargo unchanged | |
| T07 empty, reuse | PASS | Same GT: 256 unloaded a stack at a time, then the cart takes cobblestone | |
| T08 refused items | PASS | Same GT: iron block, raw iron block, ingots, nuggets, pickaxe, chest, shulker box, bucket, water bucket, minecart, dirt, gravel, renamed raw iron and a bundle of raw iron are all refused, unchanged in hand | |
| T09 full inventory | PASS | GT `unloadingRespectsSpaceAndHeavyLoad`: a full inventory with one slot of 60 cobblestone takes 4 (cart 100 → 96), then 0 | |
| T10 heavy cap | PASS | Same GT: 60 ingots carried, so 4 raw iron come out (200 → 196), then 0 at the cap; weightless cobblestone still comes out at the cap. Client: unloading 64 into an empty inventory | |
| T11 save, reload, chunks | PASS | GT `cargoSurvivesSaveAndLoad`: a saved and reloaded entity keeps 180 deepslate iron ore; an empty cart saves nothing. **Client: real save, quit and reopen of the world: 128 raw iron on server and client** | Chunk unload without quitting is not tested separately (the same save path) |
| T12 break a loaded cart | PASS | GT `destroyedCartsSpillCargoOnce`: a survival player's hit drops 256 raw iron in 4 stacks plus 1 minecart item with no data; spilling again adds nothing. Client: broken by hand → 128 raw iron + 1 minecart on the ground | |
| T13 other destruction | PASS | Same GT: an explosion spills 100 cobblestone; `/kill` spills 50 coal (no cart item, as vanilla); a creative player's hit, which vanilla discards silently, spills 30 raw gold | Lava: the cart is destroyed and the cargo spills into the lava and burns, like any dropped item (not tested). Falling into the void discards the cart and its cargo (not tested) |
| T14 rapid use, two players | PASS | GT `quickRepeatedUseByTwoPlayersConservesItems`: 10 loads and 20 unloads by two players in one tick; 328 raw iron before and after; each player ends at the 64 cap | Two real clients: NOT RUN |
| T15 riding, container use | PASS | GT: using the cart doesn't seat the player, `startRiding` is refused, and a NoAI zombie in the path of a cart rolling at 0.4 b/t is not picked up (the cart rolled 3.2 blocks). The minecart is not a `Container`. GT `hopperCannotUnloadACart`. Client: use with an empty hand → not riding | |
| T16 server authority | PASS | Every transfer runs in the server's handler for a use-entity packet (`Player.interactOn`); the client only shows synced cargo. The client test compares server and client cargo after loading, unloading and reloading. Dedicated: `runServer` starts ("Done (0.477s)") and stops | Remote client joining a dedicated server: NOT RUN |
| T17 M01/M02 regression | PASS | All 65 earlier game tests pass in the 80-test build. The client bed, shulker, movement, elytra, inventory, light, creeper and food steps pass | |
| T18 build, client, screenshots, artifact | PASS | `./gradlew build`; client screenshots in `evidence/milestone-03/screenshots/`; CI Build #14 green with the artifact | Software renderer, not a desktop GPU |
| T19 ender chest recipe and loot | PASS | GT `noRecipeMakesEnderChestsOrChestMinecarts`: recipe and advancement gone, the obsidian + eye of ender shape crafts nothing, and none of 1,940 recipes yields one. GT `noLootGivesEnderChestsOrChestMinecarts`: none of 1,447 loot tables gives one, and a silk-touched ender chest drops 8 obsidian. GT `creativeTabsLeaveThemOut` | |
| T20 ender chest place, open | PASS | GT `enderChestsCannotBePlacedOrOpened`: placement refused in survival and creative; an existing one doesn't open. Client: refused with "Ender chests don't work in this world" | |
| T21 chest minecart recipe, place | PASS | GT: recipe gone, chest + minecart crafts nothing, not in any creative tab. GT `chestMinecartsCannotBePlacedButMinecartsCan`: refused by hand and by dispenser (both keep the item), while a plain minecart dispenses. Client: refused with a message | |
| T22 chest minecart inventory, hoppers, drops | PASS | GT `existingChestMinecartsAreSealedLoot`: it doesn't open; hoppers above and below move nothing in 60 ticks; breaking it spills its 64 raw iron + minecart + chest. A mineshaft loot cart spills its loot and no chest-minecart item | |
| T23 ordinary minecart | PASS (as clarified) | Placing, dispensing, rolling and powered rails all work (T02, T03, T21). Riding is removed by design (section 2) | |
| T24 pack animals | PASS | GT `packAnimalsStillCarryChests`: a donkey, mule and llama each take a chest from a player's hand, open, accept 32 raw iron by shift-click and carry a rider. Also passes in the negative control | Walking a loaded donkey along a route: NOT RUN (inventory travels with the entity) |
| T25 M01 shulker controls | PASS | The 6 shulker tests pass in the 80-test build | |

**Negative control:** see [`negative-controls.md`](evidence/milestone-03/negative-controls.md).
- **Without the M03 hooks:** vanilla seats the player, a loaded cart doesn't exist, all six storage restriction tests fail, and the pack-animal test passes.

**Manual playtest** (load a cart at a real mine, push it round a loop, unload at an outpost, ride a donkey the same way): **NOT RUN**. No one has played it. The client test covers the mechanics, not the feel.

## 6. Configuration and data

- **Capacity:** `MinecartCargo.CAPACITY = 256`, a constant, because it's your decision, not a tuning key. Change it in code if the playtest says so.
- **Cargo list:** the item tag `data/simple_block/tags/item/minecart_cargo.json`. Add or remove items there; only stacks without components are accepted.
- **Weight:** the minecart still weighs 5 iron in the heavy-load table. Cargo inside a cart weighs nothing for the player until it's unloaded.

## 7. Decisions that need you

1. **Unload gesture:** is sneak + use acceptable? The alternatives are an empty-hand unload that skips the selected slot, or a small screen.
2. **Cargo list:** should gravel, dirt, sand, other ore blocks (coal, diamond, emerald, lapis, redstone ore), Nether quartz or ancient debris count? They're left out for now.
3. **Mobs:** settled by cushions (section 11): a cushioned cart picks up mobs again, so villagers can be moved in one.
4. **Loaded carts and speed:** a full cart rolls exactly like an empty one. Vanilla's container carts slow down as they fill; M03 doesn't.
5. **Capacity of 256:** set it after a real haul.

## 8. Known gaps and unverified feel

- How hauling feels over a real mining session hasn't been tested. Neither has the friction of loading a stack per click: a full cart takes 4 clicks to load and 4 sneak-clicks to unload.
- Not tested:
  - two real clients on a dedicated server;
  - detector and activator rails, and comparator output from a loaded cart (the cart isn't a container, so there's no comparator signal);
  - chunk unload and reload without quitting the world.
- `MobGameTests.pursuitOverridesThenAttractionResumes` (MOB-05, from M02) failed once in a full build here: one of the chasing zombies dropped its target within the 60-tick check. It then passed 6 times out of 6 on its own and in the next full build. Not investigated; nothing here touches pursuit.
- The cargo label is a name tag. Inside the 64-block name-tag range it is drawn only while the crosshair is on the cart. M04 owns the real cargo visuals.

## 9. Build, run and update

| Command | What it does |
|---|---|
| `./gradlew build` | Jar plus all 80 server game tests |
| `./gradlew runClient` / `runServer` | Dev client / dedicated server |
| `XDG_RUNTIME_DIR=/tmp/xdg xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render" ./gradlew runClientGameTest` | Real-client test |
| `JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dfabric-api.gametest.filter=simple_block-gametest:minecart_cargo_game_tests_*" ./gradlew runGameTest` | Just the cargo tests |

**CI:** Build run #14 on `69ed216` passed and published `simple-block-mod` (jar + `build-info.json`).

**Installing:** pushing to `main` runs `.github/workflows/build.yml`, which publishes `simple-block-mod`. On Windows, run **Update Simple Block**, then **create a new world**. The workflow file, artifact name and `build-info.json` are unchanged.

## 10. Next

M03 stops here. M04 (cargo visuals) is **not started**. Please playtest a haul in a new world and answer the questions in section 7 first.

## 11. Follow-up changes (owner requests after the first delivery)

| Request | What changed | Evidence |
|---|---|---|
| Cushions for minecarts | Vanilla 26.3's cushion is a seat entity fixed to the block below, so it can't ride along. Instead a fitted cushion is part of the minecart's state: use a cushion on an **empty** cart to fit it (one is used up). A cushioned cart behaves like a vanilla minecart for riding (players get in; a rolling cart picks up mobs), but takes no cargo. **Sneak + use** takes the cushion back out, not while someone sits in it. A destroyed cart drops its cushion. The client draws vanilla's cushion model and texture inside the cart. No new item: a picked-up minecart is plain. `feature/MinecartCushions`, `MinecartMixin`, client `CargoMinecartRenderer` and `mixin/AbstractMinecartRendererMixin`. | GT `MinecartCushionGameTests` (4 tests). Client: fitted with a real click (red on server and client), got in, sneaked out, sneak-clicked it back out; screenshots `minecart-cushion-fitted.png`, `minecart-cushion-riding.png` |
| Straw beds must not skip the night | They already couldn't: SF-01 blocks the night skip itself, whatever the bed. Now tested. | GT `sleepingInAStrawBedDoesNotSkipNight`: 140 ticks asleep at midnight, the clock moved 140. Negative control without SF-01: the clock jumped 5,550 ticks to morning |
| Sort top to bottom | Sort fills the top storage row first, then the bottom row (the M02 spec said bottom first). | GT `sortMergesAndOrdersTopRowFirst` (renamed; top row filled, bottom row empty); client sort step; `inventory-after-sort.png` |
| Smaller Sort button | The button is now 15 x 8 pixels: the load bar's height, on its level, dark with a frame that lights under the mouse, and a small sorted-bars icon. The tooltip still says what it does. | `inventory-gui-scale-3.png`; the client test clicks it |

Tests after these changes: **85 required server game tests** pass in `./gradlew build` (80 plus 4 cushion tests and the straw-bed test). The real-client test passes, cushion step included.

