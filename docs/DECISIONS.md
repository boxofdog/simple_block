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
| Freight cart = the minecart | **Supersedes the separate "freight cart" in the original M03 handoff (retained in Git history)** (FRT-01, FRT-11 "keep ordinary minecarts and introduce the new freight cart separately", T23). M03 overhauls the vanilla minecart itself: nobody sits in one without a cushion (a later milestone), and its job is to be filled with raw material: 256 of one kind. There is no new cart item, entity or recipe; the vanilla minecart recipe is unchanged. |
| Capacity | 256 items of one kind per cart (starting playtest value). |
| Storage bypasses | Ender chests and vanilla chest minecarts are removed from ordinary play; donkeys, mules and llamas keep their chests. |
| Cushions (after the first M03 delivery) | Vanilla 26.3 cushions make a minecart a passenger cart. A cushion is a seat entity fixed to the block under it and can't ride along, so it becomes part of the minecart's own state: fitted by using a cushion on an empty cart, drawn inside it, taken out with sneak + use. This brings forward the roadmap's "passenger cushions" item. |
| Straw beds | Like other beds, sleeping in one never skips the night. (Already true through SF-01's skip block, which is bed-type independent; now covered by a test.) |
| Sort order | Sort fills the **top** storage row first, then the bottom row (supersedes the M02 spec's bottom-row-first). |
| Sort button | Same profile and level as the heavy-load bar: an 8-pixel-high framed button at the right end of the covered row. |

## Milestone 03 implementation choices (provisional, 2026-10-11)

| Topic | Choice | Why / how to change |
|---|---|---|
| Riders | No player or mob gets into a minecart without a cushion: using it loads/unloads instead, and a rolling cart doesn't scoop up mobs. With a cushion fitted, riding (players, and mobs picked up by a rolling cart) works as in vanilla. Furnace, hopper, TNT, command-block and spawner minecarts are unchanged. | "Sitting without a cushion" covers mobs too; villager transport works again with a cushion. |
| Cushioned carts | Fit: use a cushion on an empty cart (one is used up; a cart with cargo refuses it). A cushioned cart carries a rider, not cargo. Remove: sneak + use, not while someone sits in it; the cushion goes to the inventory or the player's feet. A destroyed cart drops its cushion. The cart stores only the colour (a custom name on the cushion item is not kept). | No new item: the minecart item stays plain, so a picked-up cart is always a cargo cart until a cushion is fitted again. |
| Sort button look | No text: three rising bars as an icon, dark like the load bar, with a frame that lights up under the mouse; the tooltip says what it does. | Text does not fit in 8 pixels at the vanilla font size. |
| Gesture | Use with cargo in hand loads it; use with an empty hand shows the cargo; **sneak + use unloads one stack**, whatever is in hand. | The spec's default (an empty hand unloads) puts the unloaded stack into that same empty hand, so the next click loads it straight back. |
| Cargo list | `#simple_block:minecart_cargo`: raw iron/copper/gold, iron/copper/gold ores and their deepslate forms, coal, diamond, emerald, lapis lazuli, redstone, stone, cobblestone, deepslate, cobbled deepslate. Only component-free stacks. | One data tag; add or remove items there. Not included: gravel, dirt, sand, other ore blocks, Nether materials, blocks of metal, nuggets, ingots. |
| Readout | A label over the cart while the player looks at it ("Raw Iron 180 / 256", or "Empty 0 / 256"), plus an action-bar message on every use. | M04 replaces the look; the label is a renderer override with no state of its own. |
| Ender chest drops | Mining one always gives 8 obsidian, silk touch or not | The only natural ender chest is built into one End city floor template; this keeps it from becoming an item. |
| Mineshaft chest minecarts | Kept as sealed loot: they don't open and hoppers can't reach them; breaking one spills the loot and drops a minecart and a chest. | Keeps mineshaft loot without changing world generation. |

## Cobblestone Engine design decisions (owner-approved direction; future milestone)

- **Furnace installation:** Applying a normal furnace to an existing vanilla minecart converts it into a **Cobblestone Engine**. This is not a separate `minecart + furnace` crafting-table recipe and should not be presented as a vanilla furnace minecart.
- **Appearance:** The Cobblestone Engine receives its own new model; retain an editable Blockbench source model and UV layout for owner-painted textures.
- **Movement:** The engine uses coal and can move **bidirectionally**, either pulling or pushing a coupled train. Passenger carts, loaded freight carts and mixed consists are all valid; there is no categorical ban on hauling freight.
- **Disassembly:** Breaking a Cobblestone Engine returns **one ordinary minecart and one ordinary furnace**, not a combined engine item.
- **Traction niche:** The engine is suited to level track and gentler gradients; steep grade and heavy total train load severely limit traction. Stationary cable winches are intended for difficult incline sections, not general rail travel. Numerical physics and winch integration remain to be designed.
- **Scope:** These are future design decisions, **not an instruction to add a Cobblestone Engine in M04**. M04's shipped cable-infrastructure behavior stays unchanged until a separately authorized milestone.

### Minecart mass, rail grades, winch speed, and skips (owner-approved direction)

- **Minecarts are intrinsically heavy.** An *empty* iron minecart has meaningful mass and should already be difficult to haul up sustained grades; freight adds to that load. A multi-cart empty return train may still need an incline winch. Do not treat only cargo as weight.
- **Cobblestone Engine performance:** A locomotive can haul substantial loads on well-engineered level lines, but grade sharply reduces what it can push or pull. Short ascents may be possible; sustained steep climbs, even with empty carts, become impractical. Model resistance using **cart base mass + carried cargo + number of carts + gradient** (and the engine's traction), not special freight-only movement bans.
- **Winch speed versus gradient:** For *rail-bound minecart haulage*, steeper uphill gradients greatly reduce hauling speed; at extreme grades, moving loaded carts by winch should take a very long time. Do not make cable winches an effortless universal substitute for laying a sensible rail route. Exact speed curve, mass tuning and hand-versus-coal drive rates remain open.
- **Extreme steep rails:** Allow purposefully built rail sections with slopes as extreme as a **three-block vertical rise/drop** (the owner's “three-high rail” idea). Precise rail-piece geometry and support rules need later design, rather than assuming vanilla 1-up/1-across rails suffice. Such routes are *possible*, not necessarily efficient or safe.
- **Downhill danger:** Uncontrolled carts on those extremely steep descending rails accelerate to dangerous speeds; a sufficiently violent arrival/impact at the bottom **breaks the cart**. Safe descent requires controlling speed/braking. The exact velocity thresholds, collision behavior and cargo-drop treatment require a later specification (do not silently delete cargo).
- **Why build skips:** Purpose-built **upright vertical mining skips** should be an attractive, substantially more practical way of lifting ore through near-vertical shafts than dragging ordinary minecarts up extreme-grade rails. Treat skip hoisting as its own use of winch machinery; don't blindly apply the minecart-on-rails grade penalty to skips and accidentally erase their advantage.
- **Scope:** These are future design and balancing commitments, *not* authorization to modify M04 or introduce the rail, crash, engine, or skip mechanics yet.


## Milestone 04 decisions by the user (2026-10-12)

The owner's M04 brief is recorded verbatim in `docs/MILESTONE_04.md`. In short: a copper connecting rod on the vanilla minecart that grips an overhead cable and passes its supports; copper cable anchors only under wooden blocks, guiding the cable on rollers; cable as a finite, craftable item combined into coils; a cable drum (recipe given) that becomes a winch with a coil; the loose end picked up and dragged like a lead and routed anchor to anchor, consumed by length; routes extendable, disconnectable, dismantlable, with runs, turns and climbs. Editable `.bbmodel` models and UV layouts, no finished textures. No engines, counterweights, inclined-lift automation, skips or locomotives.

## Milestone 04 implementation choices (provisional, 2026-10-12)

| Topic | Choice | Why / how to change |
|---|---|---|
| Nothing moves yet | The drum winds nothing and the cable never moves; a gripping rod does not pull or brake its cart. M04 is placement, routing and the grip state only. | Moving cable and cart traction need a power source (the roadmap's winch and engine work), which the brief excludes. |
| Units | 1 cable = 4 blocks of cable; 1 coil = 8 cables = 32 blocks. Coils go back into 8 cables. | Constants in `CableContent`. |
| Recipes | Cable: 3 iron nuggets + 1 string, shapeless, makes 2. Coil: 8 cables in a ring. Anchor: 3 copper nuggets in a V, makes 2. Rod: a copper nugget over 2 copper ingots. Drum: as the brief (empty top row; stick, iron ingot, stick; plank, iron ingot, plank; any planks). | The brief gave only the drum's. Weights for the heavy-load cap follow from the recipes (cable 1/6 iron, coil 4/3, drum 2; anchor 1/6 copper, rod 19/9). |
| What counts as wood for anchors | `#simple_block:cable_anchor_supports`: all logs, wood and stems (incl. stripped), planks, wooden slabs, stairs and fences, bamboo blocks, bamboo mosaic (and its slab and stairs). The block must also offer the middle of its underside, like a hanging lantern needs: a bottom slab, upright stairs and fence posts work, a top slab or upside-down stairs don't. | One data tag. Not included: doors, trapdoors, fence gates, signs, shelves (no usable underside or not structural). |
| Anchor | No collision. It carries one cable. Its roller axle is set by the direction the player faces when placing it (meant: face along the cable); only the axis counts. The hanger arm is on one side: west of a north-south cable, north of an east-west one. | The rod's stem passes on the other side; see "unresolved" below for two-way traffic. |
| Drum capacity | 128 blocks (4 coils) of cable per drum, wound plus laid out. | Tuning key `cable.drum_capacity`. |
| Spans | A span is a straight line between two supports (the drum's barrel top, or an anchor's roller), at most 12 blocks (tuning `cable.max_span`), and must not pass through a block with collision. It ignores the wooden blocks the two anchors hang from, so steep spans don't catch the beam's edge. Each span uses its length rounded up to a whole block. Routes are one line per drum: no branches, loops or joins. | Elevation changes are any slope the 12-block limit and clear line allow. |
| Picking up and dragging the end | Use the hook to pick it up (not when the drum has no cable left, not while holding another end). It follows the hand like a lead. It slips out of the hand if the player gets more than (cable reach + 3) blocks from the last support, where the reach is the smaller of 12 and the cable left on the drum; also on death, leaving the game or changing dimension. | The extra 3 blocks is a player's reach, so the end can be carried to an anchor that is itself just within reach. |
| Routing gestures | Holding the end, use an anchor to run the cable through it; the end stays in hand for the next one. Using the drum or the last anchor puts the end down. Sneak + use an anchor on a route (both hands empty): the cable comes off it and everything after it and winds back onto the drum. Sneak + use the drum: first winds the whole route back in, then takes the cable off as coils. Using an anchor or drum with an empty hand otherwise reports its state. | Extending is winding on more coils and carrying on from the last anchor. |
| Breaking things | Breaking an anchor (by hand, explosion, or removing its wooden support) is the same as disconnecting there. Breaking a drum drops all its cable, wound and laid out, as coils, and frees its anchors. | Cable is never lost or created: a drum's cable is always whole coils. |
| After loading | A drum checks its route once its anchors' chunks are loaded and winds back from the first anchor that is missing or no longer records it (e.g. broken while the drum was unloaded). The loose end is not saved: the drum puts it back when loaded. | |
| Rod | Fitted by using a rod on a minecart (one per cart; independent of cushion and cargo). Sneak + use takes off the cushion first, then unloads cargo, then takes the rod. Dropped when the cart is destroyed. It works like a tram's trolley pole, sprung upwards: it grips any route cable that runs within 0.5 blocks sideways of the cart's middle and 1.5 to 3 blocks above the rail (tuning `cable.rod_reach_min`, `cable.rod_reach_max`; an anchor hung two blocks above the rail puts the cable 2.75 above it), stays gripped across anchors, and lets go where the cable ends. Gripping is computed by the server every tick and shown on the cart: the rod stretches to the cable and the head closes. | "Auto-engages" needed a rule for where a cable counts as "above". The trolley pole is the nearest real-world mechanism. |
| Rod past anchors | The head grips from above and the side away from the anchors' hanger arms (east of a north-south track, south of an east-west one); the drawn rod is always on that side. The rod has no collision with anything, so it cannot snag in the game; the models are shaped so it would not snag in reality either (the head's top bar passes between the cable and the anchor's mounting plate). | See "unresolved". |
| Models | One-off generated geometry (`tools/models/generate_models.py`), now owned by the `.bbmodel` files in `art/blockbench/`. The textures are UV templates (flat colours per face), not artwork. The cable between supports is drawn by code with `textures/entity/cable.png`. | `art/blockbench/README.md`. |

### Milestone 04 design questions left open (for the owner)

- **How the grip holds the cable past a roller.** The head grips from above only (a top roller pressing on the cable and a side guide), because a jaw closed *under* the cable would hit each anchor's roller. A real haulage grip would need either a jaw that opens at every support (cams or guides on the anchors) or supports shaped around the grip. This matters once the cable pulls carts.
- **Two-way traffic and the arm side.** The hanger arm is always on one fixed side, so the rod's stem must pass on the other. Carts coming the other way on the same track are fine (the rod is drawn on the fixed side whichever way the cart runs), but a second track beside the first under the same cable, or a cable running diagonally, has no rule yet.
- **Turns.** A cart's rod follows the cable only where the cable runs within 0.5 blocks of the track. At a turn anchor the cable cuts the corner in a straight line while the track curves; on tight curves the grip lets go and takes the cable again after the turn. Real systems use curve pulleys; whether turns need a dedicated "curve anchor" is undecided.
- **Slack, tension and sag.** Cable is drawn taut and straight. There is no slack to take up, no sag, no tension limit.
- **Drum facing and which end is "the winch".** The drum pays out to its front, but the first span may leave it in any direction; the drum does not turn to face its cable.
- **Lengths and costs.** 4 blocks per cable, 32 per coil, 128 per drum, 12 per span, and the recipes are starting values with no playtest behind them.
- **What anchors accept.** Whether non-wooden supports (iron bars, chains from stone) should ever carry anchors, and whether an anchor should be allowed on a wall bracket.

### Consequences to decide on later (not changed in Milestone 01)

- **Phantom membranes** lose their main source. The only other one is a tamed cat's occasional "morning gift" after the owner sleeps; whether that still triggers without night skipping is untested. That makes **Potions of Slow Falling** effectively unavailable. The advancements **Monsters Hunted** and **Two Birds, One Arrow** can't be completed.
- **Shulker shells** still drop but have no use, since the box was their only recipe.
- **Sky's the Limit** (obtain an elytra) can't be completed in survival.
- The `players_sleeping_percentage` and `spawn_phantoms` game rules no longer do anything.
