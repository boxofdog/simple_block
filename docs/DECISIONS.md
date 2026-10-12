# Simple Block — Decisions

**Purpose:** The current decision record, ordered by development stage. Read this alongside [ROADMAP.md](ROADMAP.md) (progress and priorities) and the relevant milestone specification/report (full implementation and tests).

**Authority:** An item here documents a decision; it does **not** authorize implementation. Only the owner's explicitly approved milestone brief does that. **Implemented** describes the existing code, not necessarily owner acceptance. **Direction** is an agreed goal whose detailed mechanics remain to be designed. **Provisional** means a working implementation choice, not an immutable design rule. **Open** means no settled answer. Superseded ideas are isolated at the end.

## 0. Project-wide rules

- **Addition by subtraction:** prefer useful extensions to existing Minecraft objects over redundant new items, chores or systems.
- **Physical logistics:** geography, mine locations, rail networks and permanent infrastructure should matter.
- **Localized machinery, not a generic power grid:** manual effort, coal and gravity can power purpose-built devices; do not introduce an electrical network or Create-style universal rotation system just because a machine has a crank.
- **Fresh worlds only:** no save migrations or old-world compatibility requirements.
- **Scope control:** implement only the current owner-authorized milestone, protect delivered behavior and report unapproved changes instead of making them silently.
- **Assets:** new models must have editable Blockbench sources and paintable UVs; placeholder UV templates are not the owner's finished textures.

## 1. M01 — Survival foundation (shipped)

### Owner requirements and delivered behavior

| Area | Current behavior |
|---|---|
| Nights and beds | Beds still allow lying down and setting spawn, but cannot advance time or weather. Applies across dimensions; vanilla Nether/End bed explosions are unaffected. |
| Phantoms | No normal natural phantom spawning. |
| Elytra | Gliding is blocked, including items with Minecraft's glider component. End ships do not generate the displayed elytra item frame. Creative flight remains possible. |
| Shulker boxes | Ordinary acquisition and use are disabled; failed use gives action-bar feedback. |
| Recipes | Removed vanilla recipes use Fabric data conditions, including their recipe-book advancements. |

### Implementation baseline

- Minecraft Java **26.3**, Fabric Loader **0.19.5**, Fabric API **0.162.0+26.3**, Loom **1.18.4**, Gradle **9.7.1**, JDK **25**; version details describe the original delivered toolchain, not a promise to avoid future upgrades.
- Mod ID `simple_block` and display name **Simple Block** originated as provisional project identifiers. Rename deliberately before release if desired.
- The existing bed screen uses the vanilla leave-bed interaction rather than silently forcing players out. See [MILESTONE_01_REPORT.md](MILESTONE_01_REPORT.md) for tests and exact handling.

### M01 consequences still awaiting a separate decision

- Phantom membranes and related advancements/potions may become unavailable or harder to obtain without naturally spawning phantoms; the cat-gift route has not been verified here.
- Shulker shells lose their normal survival purpose.
- The elytra advancement becomes inaccessible through ordinary survival.
- Some vanilla sleep/phantom game rules no longer affect the behaviors changed by Simple Block.

These are **known side effects**, not permission to add replacement recipes or alter advancements.

## 2. M02 — Geology, survival pressure and inventory (shipped)

### Owner decisions

| Area | Current decision |
|---|---|
| Geology | Remove naturally occurring granite, diorite, andesite and tuff families where specified; retain stone/deepslate transitions and preserve useful world generation. |
| Mining | Naturally generated plain deepslate and deepslate ores require iron-tier-or-better pickaxes; crafted/decorative deepslate variants keep their own vanilla requirements. |
| Ore distribution | Some iron remains widely accessible; large rich deposits reward geography and exploration. Diamonds are substantially rarer. Exact generation settings and measurements are recorded in the M02 report. |
| Carrying capacity | Player inventory has **27 usable slots**. Heavy iron, copper, gold, diamond and netherite share a **64 ingot-equivalent unit** limit, including the applicable blocks, ores, tools and armor. |
| Diamond / netherite weight | A diamond counts like one ingot; a diamond block counts as nine. Netherite similarly counts by the diamond-equivalent weight of its gear rather than charging the gold/ingots used in fabrication. |
| Weightless bulk materials | Cobbled stone materials, gravel and sand **do not contribute to the player's heavy-load allowance**. They still occupy ordinary inventory slots. The heavy-load mechanic is for designated metals and other heavy resources, not every material that can be shipped by cart. |
| Weight UI | A material-colored, text-free bar communicates weight in the inventory; hover gives total out of 64 and a per-material breakdown. |
| Mob attraction | Hostiles are drawn toward nearby **lit ground**, not individual torches or the brightest source. The system applies at Overworld night and underground where sky light is absent, including underground daytime; not the Nether/End. Pursuing a player takes precedence. |
| Lighting and survival | Standard torches emit light level **12**; lanterns **15**. Zombies can threaten wooden doors while pursuing; creepers disappear in direct sunlight. Routine hunger exhaustion is reduced, and saturation is visibly shown. |
| Inventory interaction | Inventory sorting and hotbar-first shift-click behavior preserve the constrained-carrying design. |

### Later changes to the M02 implementation (delivered during M03)

- Inventory sort order is **top row first**, superseding the original bottom-row-first proposal.
- The Sort control is an icon in an **8-pixel-high** frame aligned with the weight bar.
- Straw beds, like other beds, cannot skip the night; this gained an explicit regression test.

For exact ore distributions, AI tests, hunger tuning, weight mappings, and performance evidence, use [MILESTONE_02_REPORT.md](MILESTONE_02_REPORT.md), not outdated working-phase handoffs.

## 3. M03 — Vanilla minecart overhaul (shipped)

### Owner decisions

| Area | Current decision |
|---|---|
| Freight vehicle | **The existing vanilla minecart is the freight cart.** No separate freight minecart item/entity or recipe. This supersedes the original M03 brief, retained only in Git history. |
| Capacity | Holds **256 of exactly one eligible bulk item type**, a playtestable starting value. |
| Bulk cargo expansion (approved, pending implementation) | **Cobbled stone materials, gravel and sand are freightable** in ordinary minecarts while remaining weightless for personal heavy-load calculations. Existing `cobblestone` and `cobbled_deepslate` already qualify; add `gravel` and `sand` to the eligible cargo tag **after the owner has playtested M05**. Other possible cobbled/sand variants are not silently assumed. |
| Passenger use | Install a **vanilla 26.3 cushion** into an empty cart to enable riding and mob pickup; a cart without a cushion cannot take riders. Cushion and bulk cargo are mutually exclusive. |
| Cargo appearance (future owner decision) | Keep the existing vanilla freight minecart. Display its cargo as a **surface that rises in discrete steps with item count**, cauldron/composter-style, from empty to its 256-item capacity; implement new editable, owner-paintable texture assets. Replace persistent floating item-count text with visual fullness while preserving an intentional interaction to inspect exact count/type. The exact number of steps and how different bulk materials look remain implementation details. **Not in M05.** |
| Storage bypasses | Ender chests and ordinary chest minecarts cannot serve as portable storage; donkeys, mules and llamas retain their cargo role. |
| Other variants | Furnace, hopper, TNT and other specialized cart variants were left unchanged by M03; future Cobblestone Engine work is a separate decision. |

### Delivered interaction details (implementation choices)

- Right-click with eligible cargo to load; empty-hand use inspects; **sneak + use** unloads one stack without bypassing inventory limits. `#simple_block:minecart_cargo` defines eligible material types.
- Cushions fit only empty carts, are visible, and return on removal or cart destruction. A cushioned cart carries passengers instead of bulk cargo.
- A broken cargo cart spills its contents into the world rather than storing them inside the dropped minecart item. Minecart storage survives world save/load.
- Mineshaft loot chest minecarts remain sealed: they cannot open or feed hoppers, and breaking them spills loot plus a minecart and chest. Ender chests drop obsidian rather than becoming usable containers.
- M03 introduced an on-look cargo readout and action-bar feedback. The owner has now decided that **visible stepped cargo-fill geometry replaces the intrusive floating on-look count**, while deliberate inspection can still provide exact material/count. This is a future presentation update, tracked in [UX_TEXT_LOG.md](UX_TEXT_LOG.md); other useful information must not simply disappear.
- Full **as-shipped** eligible-item list, edge cases and test evidence: [MILESTONE_03_REPORT.md](MILESTONE_03_REPORT.md). **Pending follow-up:** the new gravel/sand eligibility decision above is not yet implemented; the M03 report's former "gravel refused" tests describe the old behavior. Update the item tag and relevant tests only in an authorized implementation pass.

## 4. M04 — Cable infrastructure (implemented; owner acceptance pending)

### Owner-approved M04 scope

- Fit an existing minecart with a **copper connecting rod** that engages overhead cable; supports and the rod/grip must be shaped to pass each other.
- **Copper roller anchors** mount only beneath appropriate wooden blocks.
- Cable is a finite craftable item bundled into **coils**. A coil applied to a placeable drum creates a loaded winch structure and a loose hooked cable end.
- Pick up the end like a lead and route it from anchor to anchor; consume cable by length. Players can extend, disconnect and dismantle routes, including horizontal spans, turns and elevation changes.
- Produce editable `.bbmodel` assets and UVs, **not** finished textures.
- **M04 does not drive the cable, pull minecarts, implement engines or power, automate inclines, or build mining skips.**

The authoritative brief is [MILESTONE_04.md](MILESTONE_04.md); the as-built record and tests are [MILESTONE_04_REPORT.md](MILESTONE_04_REPORT.md).

### As-built implementation choices (provisional until playtested)

| Component | Implemented rule / starting value |
|---|---|
| Cable & coils | 1 cable item provides **4 blocks**; 8 cables form a **32-block coil**, reversible to cables. |
| Recipes | Cable: 3 iron nuggets + string → 2 cables. Coil: 8 cables in a ring. Anchor: 3 copper nuggets in a V → 2 anchors. Rod: copper nugget over 2 copper ingots. Drum: empty top row; stick / iron / stick; plank / iron / plank. |
| Anchor supports | Tag `#simple_block:cable_anchor_supports`: suitable logs, wood/stems, planks, slabs, stairs, fences, bamboo and mosaics. The underside must physically accept a centered mount; not top slabs, upside-down stairs, doors, trapdoors, etc. |
| Anchor shape | One cable per anchor; axle follows placement axis. Hanger is on one side, leaving space on the opposite side for the rod. No collision. |
| Drum capacity | **128 blocks** total, on the drum plus installed route; tuning key `cable.drum_capacity`. |
| Spans | Straight, unobstructed route segments of at most **12 blocks** (`cable.max_span`), charged in whole blocks rounded up. One unbranched, open route per drum. |
| Routing | Right-click loose hook to carry it; right-click each anchor to add a span; put the end down at the drum or last anchor. Sneak-use at anchor retracts that anchor and everything beyond; sneak-use at drum retracts route, then removes coils. Broken supports/anchors disconnect routes; a broken drum drops its cable. |
| Reach / persistence | Carrying the hook is bounded by available cable/maximum span plus interaction reach. Cable/routes persist; temporary held hooks are restored to a loose end after reload. Missing anchors trigger route cleanup. |
| Connecting rod | One per cart, independent of cushion and cargo. Reaches to a cable roughly **1.5–3 blocks above the rail** and within **0.5 blocks laterally** (tunable); engages automatically, releases at cable end, has no physical collision. Visual head/arm changes show engagement. |
| Models | Source in `art/blockbench/`; generated UV-template art is not final texture work. Cable spans are rendered procedurally. |

### Open M04 follow-ups — not implemented

- **Cable motion topology (M05 direction now approved):** keep the M04 open-ended route for placement and add a closed circulating loop for powered travel, with the original strand retained for the rod and a discreet side-offset return strand. A terminal anchor becomes a return wheel, ideally by adding another copper anchor. Exact closing gesture, return clearances and drive mechanics are still to be validated in M05.
- **Grip versus rollers:** a moving cable needs a believable grip/guide geometry that passes anchor rollers. Present M04 engagement and no-collision behavior is not proof of future powered haulage.
- **Curves and multiple tracks:** tight curves can pull the grip out of alignment; two-track layouts, diagonal passages and bidirectional geometry need rules.
- **Tension, braking and stopping:** present cable renders taut without simulation; future powered motion must decide sag/tension, braking, release and route ends.
- **Drum alignment and support variants:** decide barrel/outlet facing, and whether to add support types beyond underside-mounted wood.
- **Balance:** cable and coil costs, 12-block spans, 128-block drum capacity and grip reach are implemented starting values, not validated long-term balance.

## 5. M05 and future transport / mechanical design

**M05 is owner-authorized for implementation but not yet built.** Its specification is [MILESTONE_05.md](MILESTONE_05.md): circulating cable loop, reversible manual drum crank, coal-powered bidirectional Cobblestone Engine with minimally functional coupling, and the drum-side render fix. The longer-term decisions below (coal winch motors, skips, extreme rails, counterweights) **are not automatically authorized** by M05.

### 5.1 Cobblestone Engine — coal-powered railway locomotive

- Apply an **ordinary furnace directly to an existing vanilla minecart** to convert it into a **Cobblestone Engine**. Do **not** use the vanilla crafting-table furnace-minecart recipe as the creation gesture.
- A new, distinct model with editable Blockbench source. Breaking the engine returns **one ordinary minecart and one regular furnace**, not a combined engine item.
- Coal-powered, **bidirectional**, able to **push or pull** coupled carts, including passenger carts, freight carts and mixed trains.
- **Minecarts are heavy before adding cargo.** Empty carts have meaningful baseline mass, so several empty carts returning uphill can still strain an engine. Cargo adds to that burden.
- Engine suitability depends on **cart base mass, cargo load, number of carts, gradient and available traction**; never disable movement simply because a train contains freight.
- A relatively level, well-engineered railway can rely on a Cobblestone Engine alone. Grades sharply limit propulsion; sustained steep climbs, including some empty-cart climbs, may be impractical.
- Specific speed, fuel use, traction equations, downhill braking and coupling rules are **open**.

### 5.2 Winches, dangerous gradients, and upright mining skips

- A **stationary winch** is a localized answer to a difficult rail incline, not an alternative general-purpose locomotive. Manual operation first; a coal-powered attachment is a potential later upgrade. Do not introduce a generalized mechanical-energy distribution network.
- **Rail-bound winch haul speed falls sharply as uphill gradient increases.** Very steep climbs with loaded carts should take an extremely long time, making track geometry a meaningful design tradeoff. The exact speed curve is **open**.
- Allow purpose-built rail geometry as extreme as a **three-block vertical rise/drop**. This states the design goal, **not** an approved track shape or a claim that existing vanilla rails support it.
- **Extreme downhill travel is dangerous.** An uncontrolled cart can accelerate and strike the bottom hard enough to **break the cart**. Braking, crash thresholds and preservation/dropping of cargo require explicit design; items must not silently vanish.
- **Skips remain upright** and lift ore through near-vertical shafts. They should be substantially more practical for steep vertical ore transfer than winching conventional minecarts up extreme-grade rails. A skip is the hoisted carrier; a winch provides lifting power.
- **Skip form (owner decision; not implemented):** a large upright carrier **one block wide and three blocks tall** (NOT three blocks long), requiring **four blocks of vertical clearance** for an upper loading door that unfolds toward an elevated loading track. Its horizontal depth is not yet specified. The upper **two blocks** form the ore storage compartment, with a **slide-out gate at its floor**. The bottom **one-block-tall section is an open-air angled discharge chute**, visibly open rather than a sealed compartment, routing ore sideways into an adjacent minecart. The skip itself carries the chute—do not require a separate station building or discharge structure.
- **Skip capacity and recipe (owner decision):** **512 units** of bulk ore, with the shaped crafting layout:

  ```text
  [empty] [cable]         [empty]
  [empty] [iron trapdoor] [empty]
  [iron]  [minecart]      [iron]
  ```

  The precise allowed cargo mix/type behavior and the construction orientation are still to be confirmed; do not assume additional materials or ingredients.
- **Loading/unloading track (owner direction):** a dedicated load/unload rail itself defines the transfer point; a cart **above** a skip can dump into its top compartment, while a receiving cart **beside** the skip on the designated rail **automatically triggers the upper ore compartment's sliding floor gate to open** when the skip is stationary and correctly aligned. Ore then travels down the skip's exposed lower chute and into the cart. No additional docking structure, unloading machinery or manual opening should be needed. Transfer must respect capacity and avoid item loss; precise detection, closing and partial-transfer behavior remain open.
- **Hoisting hookup (owner direction):** using the existing loose cable **hook on a skip** attaches it; this generates a **triangular two-leg connecting bridle**: two attachment points flank the skip's ore-intake opening and converge at **a single apex above the skip**. **One main hoisting cable** then continues upward from that apex, over an already established overhead guide wheel, to a higher winch. The bridle is not a pair of independently routed lines all the way to the wheel. Cable/bridle material accounting and clearance remain engineering details to settle before implementation.
- **Hoisting motion (owner decision):** this is Minecraft, not a pendulum simulator: the attached skip stays upright and moves **only vertically beneath its guide wheel** using the **existing winch's ordinary drive power**, whether operated manually or later fitted with a coal attachment. Do **not** require timber guides, special vertical rails, counter-sway mechanics, or a separate skip motor. Keep ordinary world clearance and obstruction checks.
- **Bare-hand crank controls (owner decision, ALL winch uses):** aim at the installed winch's crank with an **empty hand**; **hold left-click to FEED cable away from the winch**, **hold right-click to PULL it toward the winch**. This same directional convention governs **lateral rail hauling** and vertical skip hoisting (**left lowers, right raises**). Release stops the drive. For a **closed circulating loop**, feeding/pulling means moving the working strand away from/toward the winch, **not winding/unwinding extra cable**; total installed loop length stays fixed. Left-click at the crank must override vanilla block-breaking during use; intentional break/dismantle elsewhere remains possible. M05 is underway; implement this as a shared crank-control refinement, **not** as permission to add skips to M05.
- **Skip track stopping (owner decision):** when **carrying freight**, the moving skip stops at a compatible **unloading rail** as soon as its **bottom chute** is level with that rail. A properly positioned receiving minecart triggers the slide-out discharge gate and receives as much compatible cargo as it can hold. When **empty and descending**, releasing left-click or reaching the bottom of the shaft makes the skip check for the **nearest loading rail** and unfold its **top door** toward that track. If a suitable loading position is absent, or the skip is otherwise idle, it hangs at rest with doors shut. The load/unload rail itself serves as the station; no separate station machine. **Owner delegates search range, alignment tolerance and any small positioning adjustment to Claude's implementation discretion**, provided the behavior is predictable and safe, with no lost/duplicated ore or unintended long-distance jumps.
- Counterweights, coal-powered winch attachments, automated handoff/reset, skips and new rail shapes are **separate future designs**, not implicit M04 work.

### 5.3 Intended logistics tradeoff

**Flat or carefully graded routes reward engineering with easy locomotive transport. Difficult slopes invite fixed cable haulage. Nearly vertical shafts reward purpose-built skips.** The tradeoff must emerge from mass, gravity, gradient and machinery, **not** arbitrary freight/passenger restrictions.

### 5.4 Future spelunking ropes (owner-approved design; not implemented)

- **Ordinary Rope recipe:** three strings vertically in the center crafting column. Recipe output count is not yet specified.

  ```text
  [empty] [string] [empty]
  [empty] [string] [empty]
  [empty] [string] [empty]
  ```

- **Spelunker's Rope recipe:** craft with one existing **copper cable anchor** placed above one ordinary Rope in the center column; do not invent a second specialized anchor item.

  ```text
  [empty] [empty]         [empty]
  [empty] [copper anchor] [empty]
  [empty] [rope]          [empty]
  ```

- **Placement and growth:** attach the Spelunker's Rope to the **underside or side of a block**. Rope falls/hangs vertically in open air. **Hold right-click with Rope in hand** to add downward length in **12-block increments**. When the line reaches the ground, additional rope visibly **coils into a bundle on the bottom block**, not through solid terrain. This is a climbable exploration rope, not the moving hauling cable used by M04/M05 winches.
- **Deferred details:** precise recipe yields and material consumption per 12 blocks, where the player aims during extension, climbing speed/control, coiled excess behavior, retrieval, and what happens if the mounting block disappears. Do not implement with M05.

## 6. Open decisions requiring a future milestone brief

Only record settled answers in sections 1–5 after the owner decides them.

| System | Unsettled details |
|---|---|
| Cable motion | **M05 loop direction settled** (outgoing grip strand and side-offset return); universal bare-hand winch input is **left = feed away**, **right = pull toward**, including rail haulage and future vertical hoists. Circulating loops reverse direction without changing wound length. Input interception, terminal gesture, guide clearance, braking and safe release still need engineering validation. |
| Cobblestone Engine | Mass and traction model, practical grade limits, how coal is spent, interaction/controls and what happens when trains stall or reverse. |
| Coupling | How carts attach/detach, how tension propagates through a mixed train, and behavior on curves/grade transitions. |
| Rails | Geometry and construction of extended extreme slopes; whether steep routes require special rail pieces; speed and crash damage. |
| Incline winches | Grade-versus-speed relationship, winch load limits, controlled lowering and automated return/reset. |
| Mining skips | **Settled:** one block wide × three blocks tall, four-block clearance for top door; recipe, 512 capacity, exposed lower chute and automatic discharge to aligned minecarts; triangular bridle and single-line, sway-free winch hoisting; left-feed/lower and right-pull/raise; loaded stop at unloading rail and empty-descending top-door check upon release/bottom-out. **Claude may select the local alignment range/tolerance and docking details.** Still open: horizontal depth, cable/bridle accounting, cargo-type and partial-transfer rules, collision and safety. No extra guide/station machinery. |
| M04 balance | Final recipes, route spans and cable capacity after actual owner playtesting. |

## 7. Superseded ideas (historical only)

These entries explain changes in direction. **Never treat the old side as an active instruction.** Original deleted handoffs remain recoverable through Git history.

| Earlier concept | Current decision |
|---|---|
| Start by building a freight-cart mod | Begin with isolated survival rules (M01), then geology/inventory (M02), then minecart logistics (M03). |
| Separate new freight-cart entity/item | M03 modifies the **vanilla minecart** itself. |
| Freight in chest minecarts | Minecart cargo is accessible directly, not hidden in chest storage; chest-minecart storage bypasses are closed. |
| Cushions deferred to a later milestone | Cushion passenger functionality shipped as an M03 follow-up. |
| Sort bottom row first | Top row first was accepted and shipped in M03. |
| Iron only in selected biomes | Small deposits broadly available; rich geographically situated deposits. |
| Hostiles drawn to specific torch/lantern blocks | Hostiles drawn to lit **areas**, including caves where appropriate. |
| Delete every item formerly linked to redstone | Remove/rethink circuits while preserving worthwhile stand-alone mechanics; decide specifics later. |
| Sideways mining skip that tips upright | Skip is **always upright**, with ore dropped in from elevated rails. |
| Cable at rail height | **Overhead** hauling cable, supported by copper rollers and reached by cart rods. |
| Timber supports required to prevent cave-ins | Support structures earn their place by holding useful mechanical equipment, not compulsory cave-in simulation. |
| Unapproved design numbers or suggested power mechanisms | Numbers from brainstorming are not binding; distinguish delivered provisional values from owner-approved future behavior. |

**Document maintenance:** Modify the relevant milestone section in place when a decision changes. Move rejected ideas to section 7. Do not append new sections in arbitrary locations or duplicate a topic with contradictory versions.
