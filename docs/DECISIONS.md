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
| Heavy load display | A bar without text in the inventory, filled per material in that material's colour (netherite has its own colour), so the player sees what the weight is made of. Hovering it shows the numbers: total out of 64 and each material's exact weight. |

### Consequences to decide on later (not changed in Milestone 01)

- **Phantom membranes** lose their main source. The only other one is a tamed cat's occasional "morning gift" after the owner sleeps; whether that still triggers without night skipping is untested. That makes **Potions of Slow Falling** effectively unavailable. The advancements **Monsters Hunted** and **Two Birds, One Arrow** can't be completed.
- **Shulker shells** still drop but have no use, since the box was their only recipe.
- **Sky's the Limit** (obtain an elytra) can't be completed in survival.
- The `players_sleeping_percentage` and `spawn_phantoms` game rules no longer do anything.
