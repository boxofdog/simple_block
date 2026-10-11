# Decision Log and Supersessions

This record prevents older brainstorming ideas and assistant proposals from accidentally becoming requirements.

## Changes that supersede earlier ideas

| Earlier concept | Current direction |
|---|---|
| Iron exclusively in selected biomes | Small amounts broadly available underground; rich deposits biased to selected biomes. |
| Hostiles attracted to particular torches/lantern blocks | Hostiles attracted to illuminated **areas**. |
| Delete every item once associated with redstone | Remove circuitry; keep/rework useful mechanics such as hoppers and compasses. |
| Sideways skip that rotates upright after loading | Skip **always upright**, sitting below an elevated rail-dumping point. |
| Traction cable at rail level | **Overhead mechanical hauling cable** with cart connecting arm; not electricity. |
| Timber support required to avoid cave-ins | Timber support useful as mechanical infrastructure; no arbitrary collapse penalty. |
| Freight through chest minecarts | Open-top cargo carts show their actual load. |
| Begin with full freight cart prototype | **Begin with small vanilla survival-rule changes**; freight cart is later first major custom addition. |

## Accepted development decisions

- Keep a single consolidated register of design intent, with decided vs proposed labels.
- Start with changes that are isolated and testable.
- Read and respect the existing repository configuration rather than importing assumptions from brainstorming.
- Future models must be editable in Blockbench and textures paintable by the user.
- No significant future transport or worldgen implementation in Milestone 01.
- **Fresh-world-only scope:** No existing-save compatibility, save migration, or legacy inventory recovery is required.

## Important unresolved issues

- Actual Minecraft version, mod loader, mapping set, JDK: **discover from repo**.
- Mod name and ID: retain from repo if set; otherwise use an explicitly marked provisional choice.
- What a bed should do in specific edge conditions; first milestone chooses simple spawn-setting/no-skip behavior in Overworld.
- Whether to preserve phantom entities for commands/testing; normal survival must not spawn them.
- Most numeric balance and asset architecture choices for later milestones.

## Rejected as automatic requirements

Suggestions made during earlier discussion (hand-winch operation time, continuous loop rope design, particular cargo capacities, and specific locomotive stats) are not binding until explicitly decided.

## Milestone 01 implementation decisions (2026-10-11)

These answer the "discover from repo" items above. The repository was empty, so nothing could be discovered and a toolchain had to be chosen.

| Topic | Decision | Why / how to change |
|---|---|---|
| Game version | Minecraft Java **26.3**, the latest release at the time | Current release; the official Fabric template targets it. 26.x requires Java 25 and ships unobfuscated, so there is no mappings layer. |
| Loader | **Fabric** (Loader 0.19.5, API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1) | Lightweight; the API covers elytra events, resource conditions and game tests. Switching loaders later means rewriting the mixins and the test harness. |
| Mod ID / name | `simple_block` / "Simple Block": **provisional**, taken from the repository name | Rename in `fabric.mod.json`, `gradle.properties` and the Java package before any public release. |
| Bed behaviour (SF-01) | Players can still lie down; the night and storms never pass. They see vanilla's "No amount of rest can pass this night" and leave with the Leave Bed button (or are woken at morning). This is the same as vanilla with `players_sleeping_percentage` above 100, enforced in code. | Keeps vanilla stats, the "Sweet Dreams" advancement and the bed screen. **Alternative:** never let players lie down at all (clicking only sets spawn). That's a small change if you prefer it. |
| Night-skip scope | Disabled in every dimension, not only the Overworld | Nether/End beds explode before sleep is checked, so they are unaffected. Custom dimensions also can't skip nights. |
| Gliding (SF-03) | All gliding is blocked, not only the vanilla elytra item | Also covers items given the `minecraft:glider` component by commands. Creative players can't glide either (creative flight is unaffected). |
| End ship elytra | The item frame is not generated at all | An empty frame would only advertise a missing item. |
| Shulker feedback | Action-bar message "Shulker boxes don't work in this world" (translation key `message.simple_block.shulker_box_disabled`, English fallback only) | No language file was added. |
| Recipe removal | Data overrides using Fabric's `fabric:false` load condition | Removes the 17 vanilla recipes and their recipe-book advancements without code. |

## Milestone 02 decisions by the user (2026-10-11)

| Topic | Decision |
|---|---|
| Heavy load materials | Diamond and netherite count towards the 64-ingot allowance, alongside iron, gold and copper. One diamond weighs like an ingot (ore 1, block 9, gear by diamonds used). Netherite weighs like diamond: an ingot 1, a block 9, netherite gear what its diamond version weighs, not the 4 gold an ingot is made from. |
| Light attraction (MOB-01) | Mobs head for the nearest lit area, then wander about it preferring lit ground, with no pull towards light sources or the brightest block. Active at Overworld night and, at any time of day, wherever the mob gets no sky light (caves at any depth, no fixed y). Not in the Nether or End, which have no sky light anywhere. |
| Heavy load display | A bar without text in the inventory, filled per material in that material's colour (netherite has its own colour), so the player sees what the weight is made of. Hovering it shows the numbers: total out of 64 and each material's exact weight. |

## Milestone 03 decisions by the user (2026-10-11)

| Topic | Decision |
|---|---|
| Freight cart = the minecart | **Supersedes the separate "freight cart" of `docs/MILESTONE_03.md`** (FRT-01, FRT-11 "keep ordinary minecarts and introduce the new freight cart separately", T23). M03 overhauls the vanilla minecart itself: nobody sits in one without a cushion (a later milestone), and its job is to be filled with raw material: 256 of one kind. There is no new cart item, entity or recipe; the vanilla minecart recipe is unchanged. |
| Capacity | 256 items of one kind per cart (starting playtest value). |
| Storage bypasses | Ender chests and vanilla chest minecarts are removed from ordinary play; donkeys, mules and llamas keep their chests. |

## Milestone 03 implementation choices (provisional, 2026-10-11)

| Topic | Choice | Why / how to change |
|---|---|---|
| Riders | No player or mob gets into a plain minecart: using it loads/unloads instead, and a rolling cart no longer scoops up mobs. Furnace, hopper, TNT, command-block and spawner minecarts are unchanged. | "Sitting without a cushion" covers mobs too (villager transport by minecart is gone until cushions). |
| Gesture | Use with cargo in hand loads it; use with an empty hand shows the cargo; **sneak + use unloads one stack**, whatever is in hand. | The spec's default (an empty hand unloads) puts the unloaded stack into that same empty hand, so the next click loads it straight back. |
| Cargo list | `#simple_block:minecart_cargo`: raw iron/copper/gold, iron/copper/gold ores and their deepslate forms, coal, diamond, emerald, lapis lazuli, redstone, stone, cobblestone, deepslate, cobbled deepslate. Only component-free stacks. | One data tag; add or remove items there. Not included: gravel, dirt, sand, other ore blocks, Nether materials, blocks of metal, nuggets, ingots. |
| Readout | A label over the cart while the player looks at it ("Raw Iron 180 / 256", or "Empty 0 / 256"), plus an action-bar message on every use. | M04 replaces the look; the label is a renderer override with no state of its own. |
| Ender chest drops | Mining one always gives 8 obsidian, silk touch or not | The only natural ender chest is built into one End city floor template; this keeps it from becoming an item. |
| Mineshaft chest minecarts | Kept as sealed loot: they don't open and hoppers can't reach them; breaking one spills the loot and drops a minecart and a chest. | Keeps mineshaft loot without changing world generation. |

### Consequences to decide on later (not changed in Milestone 01)

- **Phantom membranes** lose their main source. The only other one is a tamed cat's occasional "morning gift" after the owner sleeps; whether that still triggers without night skipping is untested. That makes **Potions of Slow Falling** effectively unavailable. The advancements **Monsters Hunted** and **Two Birds, One Arrow** can't be completed.
- **Shulker shells** still drop but have no use, since the box was their only recipe.
- **Sky's the Limit** (obtain an elytra) can't be completed in survival.
- The `players_sleeping_percentage` and `spawn_phantoms` game rules no longer do anything.
