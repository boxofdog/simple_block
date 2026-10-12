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

- **Cable motion topology:** implemented in M05 as a closed loop on straight routes (section 5.0).
- **Grip versus rollers:** a moving cable needs a believable grip/guide geometry that passes anchor rollers. M04 engagement and no-collision behavior is kept as is in M05; the grip still has no physical shape that would clear a roller.
- **Curves and multiple tracks:** tight curves can pull the grip out of alignment; two-track layouts, diagonal passages and bidirectional geometry need rules. M05 loops refuse routes that turn (section 5.0).
- **Tension, braking and stopping:** the cable still renders taut without simulation. M05 settles stopping for loops: carts are held when the crank stops and stop short of both ends (section 5.0).
- **Drum alignment and support variants:** M05 lines a looped drum up with its route and lets a drum hang under a wooden beam; other support types remain open.
- **Balance:** cable and coil costs, 12-block spans, 128-block drum capacity and grip reach are implemented starting values, not validated long-term balance.

## 5. M05 and future transport / mechanical design

**M05 is implemented; owner acceptance pending.** Its specification is [MILESTONE_05.md](MILESTONE_05.md): circulating cable loop, reversible manual drum crank, coal-powered bidirectional Cobblestone Engine with minimally functional coupling, and the drum-side render fix. The as-built record and tests are [MILESTONE_05_REPORT.md](MILESTONE_05_REPORT.md). The longer-term decisions below (coal winch motors, skips, extreme rails, counterweights) **are not automatically authorized** by M05.

### 5.0 M05 as built (implemented; owner acceptance pending)

**Owner decisions during M05:**

- **Coupling:** an **iron chain**. Use a chain on one cart, then on the next; shears uncouple and give the chain back.
- **Crank controls** (owner's clarification during M05): with an empty hand, hold left-click on the crank to feed cable away from the winch, hold right-click to pull it toward the winch; on a closed loop that is the direction the working strand moves.
- **Engine controls:** **driver seat plus ground toggle**. A rider in any cushioned cart coupled to the engine holds forward to power the train towards where they look along the track, and back to brake. From the ground, an empty-hand use starts the engine away from you, or stops it if it is running. (Chosen so a train can be driven from either end without getting out.)

**As-built implementation choices (provisional until playtested):**

| Component | Implemented rule / starting value |
|---|---|
| Closing a loop | Use a copper anchor on the **last anchor** of an open route: it becomes the return wheel (the anchor used goes into it), the anchors in between get return guides, and the drum and anchors turn to line up with the route. The return strand runs **4.75 pixels beside** the working strand, on the anchors' arm side, at the same height. |
| Loop rules | **Straight routes only**: drum and anchors in one line along a world axis, climbing or falling freely but not turning; a turning route is refused, naming the anchor. The return strand must be clear of solid blocks. It costs the return spans plus **3 blocks** for the turns, taken off the drum, and refused if the drum is short. |
| Opening a loop | Sneak-use (empty hands) any anchor of the loop or the drum: the return cable winds back and the wheel's anchor comes back. Breaking an anchor or the drum also opens it; nothing is lost. Opening never dismantles the route; a further sneak-use does, as in M04. |
| Crank | On every drum, on the axle end to the right of the cable side. **Owner's controls:** aiming at it, **hold left-click to feed** the cable away from the drum and **hold right-click to pull** it toward the drum (a held button repeats every 4 ticks; the crank keeps turning 6 ticks after each). Left-click on the crank never starts breaking the drum (the client sends a small crank packet instead, which the server checks); left-click elsewhere on the drum breaks it as before. Any held item works the crank the same way, so a coil in hand is never wound on from the crank. Other uses of the drum are as in M04. A drum with an **open route is locked** (short message: "Close the loop to haul"); an empty drum just spins. |
| Hauling | On a loop the drum is a capstan: feeding moves the working strand away from the drum and pulling moves it back, at `cable.crank_speed` **0.06 blocks/tick** (1.2 blocks/s), and the cable never piles up on the spool. A cart whose rod grips the **working strand only** moves with it, with its coupled carts. Crank stopped: the carts are held. Carts stop **1.5 blocks** (`cable.end_clearance`) short of the drum and the wheel, or where the cable would leave the rod's reach. |
| Hauling uphill | Speed divided by 1 + `cable.grade_load` (1.0) x mass of the gripping trains on a climbing rail. Lowering is not faster. |
| Overhead drum | Place a drum against the underside of a wooden beam (same supports as anchors): it hangs upside down and its cable leaves at about the anchors' roller height. Ground placement is unchanged. A hanging drum drops when its beam goes. |
| Mass | In empty-minecart units: cart **1.0** (`rail.cart_mass`), a full load of 256 adds **2.0** (`rail.cargo_mass`, pro rata), a rider **0.5**, the engine **2.0**. Nothing depends on what the cargo is. |
| Cobblestone Engine | Vanilla's **furnace minecart entity**, renamed and redrawn. Made by using a furnace on an **empty** plain minecart (no cargo, cushion, rod or rider); it keeps the cart's couplings. Breaking it drops **one minecart and one furnace**. The furnace-minecart recipe and its advancement are disabled. |
| Engine fuel | Coal or charcoal (`#minecraft:furnace_minecart_fuel`), 3,600 ticks of driving each, up to 32,000. **OWNER CLARIFICATION after M05 review:** consumes coal while commanded to apply power **even if the train stalls on a grade**; the engine is still working against the load. Engine stopped/braking normally does not consume fuel. This is distinct from M06 powered-winch fuel stopping when cable motion stops. |
| Engine traction | Force `train.engine_force` **0.02**; each car on a climbing rail pulls back `train.grade_force` **0.0078** per unit of mass; rolling resistance **0.0005** per unit; brake **0.02** blocks/tick per tick; top speed **0.2 blocks/tick** (4 blocks/s). An engine alone climbs a 45-degree rail slowly; any cart behind it on a sustained climb stalls it (momentum carries short humps). A stopped or stalled train holds where it is. |
| Couplings | Iron chain; carts within **3 blocks**; held **1.6 blocks** apart; at most two per cart; snaps (dropping its chain) past **4 blocks**. Plain minecarts (freight, cushioned, with a rod) and engines couple. A destroyed cart drops its chains. A train with a coupled cart in an unloaded chunk is held still. |
| Models | New editable sources: anchor return guide and wheel, the drum split into frame, spool and crank, and the Cobblestone Engine; UV templates, not artwork. |

**Open M05 follow-ups — not implemented:**

- **Loops on curves:** refused for now. A loop round a corner needs curve pulleys and a rule for the return strand's side.
- **Engine and loop in one train:** if a cable and an engine both drive one train in the same tick, whichever is handled first wins; an engine that is stopped never holds a train the cable is hauling. Not designed further.
- **Fine control:** a rider has power and brake only; there is no throttle, reverser lever or speed display (deliberately: "no speed-control dashboard").
- **Downhill:** the grade helps a running engine, but never past its top speed, and a stopped engine brakes; a cable-hauled cart goes no faster downhill than the crank turns. Runaways and crash damage are left to the extreme-rail milestone.
- **Hopper, TNT and other special minecarts** do not couple.
- **Balance:** every number above is a starting value with no playtest behind it.
- **M05 code-review follow-up (owner decision):** locomotive fuel spent when working against a stalled train is **intentional**, not a defect; no change requested. Coupled carts possibly reaching a cable terminal ahead of the gripping cart are **not yet reproduced**; owner will inspect in-game before deciding whether further clearance logic or a regression test is needed.

### 5.1 Cobblestone Engine — coal-powered railway locomotive

- Apply an **ordinary furnace directly to an existing vanilla minecart** to convert it into a **Cobblestone Engine**. Do **not** use the vanilla crafting-table furnace-minecart recipe as the creation gesture.
- A new, distinct model with editable Blockbench source. Breaking the engine returns **one ordinary minecart and one regular furnace**, not a combined engine item.
- Coal-powered, **bidirectional**, able to **push or pull** coupled carts, including passenger carts, freight carts and mixed trains.
- **Minecarts are heavy before adding cargo.** Empty carts have meaningful baseline mass, so several empty carts returning uphill can still strain an engine. Cargo adds to that burden.
- Engine suitability depends on **cart base mass, cargo load, number of carts, gradient and available traction**; never disable movement simply because a train contains freight.
- A relatively level, well-engineered railway can rely on a Cobblestone Engine alone. Grades sharply limit propulsion; sustained steep climbs, including some empty-cart climbs, may be impractical.
- Speed, fuel use, traction, braking and coupling have **provisional M05 values** (section 5.0); downhill behaviour and crash consequences remain **open**.

### 5.2 Winches, dangerous gradients, and upright mining skips

- A **stationary winch** is a localized answer to a difficult rail incline, not an alternative general-purpose locomotive. M05 builds the manually driven winch; **M06 upgrades that very same existing drum by applying a vanilla furnace directly to it**. This is a **player-started, mechanically driven winch**, not a generalized mechanical-energy network or full automatic freight logistics. The M06 lever can run after a right-click, but must stop itself at physical endpoints.
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
- **Hoisting motion (owner decision):** this is Minecraft, not a pendulum simulator: the attached skip stays upright and moves **only vertically beneath its guide wheel** using the **existing winch's ordinary drive power**, manually operated or upgraded in M06 by applying a furnace directly to its drum. Do **not** require timber guides, special vertical rails, counter-sway mechanics, or a separate skip motor. Keep ordinary world clearance and obstruction checks.
- **Manual crank (M05) versus powered lever (M06 owner decision):** The **unupgraded** drum retains an empty-hand crank: **hold left-click = FEED/away, hold right-click = PULL/toward; releasing stops**. In M06, applying a furnace **replaces the crank with a three-position lever**: **BACKWARD = PULL, CENTER = NEUTRAL/OFF, FORWARD = FEED**. **Right-click on the LEVER cycles one state per click and latches there**; PULL/FEED continue motion until the player selects neutral or the physical hauling path ends, even if Steve releases the mouse or walks away. **At a cart derailment/route end/loading track or M07 skip shaft endpoint/transfer stop, the lever automatically flips to NEUTRAL and stays off**, requiring a new right-click to restart. This supersedes the old assumption of continuous hand-holding; no self-restarting freight cycles. **From NEUTRAL, the first lever right-click selects PULL by default.** The remaining repeating click transitions are open. Closed circulating cable retains fixed installed length. M05 left-click interception must avoid unintentional crank break while allowing deliberate dismantling.
- **Skip track stopping (owner decision; M07):** a skip **carrying freight** can stop **in the middle of a lift** at a compatible loading track with a **not-full receiving freight cart stopped on it**; align the built-in **lower chute** beside the cart, open the gate and transfer only cargo that fits. An **empty skip** encountering an appropriately aligned freight cart on a loading track instead **assumes the minecart is a source intending to load the skip from above**; open/align its **top loading door** rather than try to discharge an empty skip into the cart. When **empty and descending**, releasing the manual crank, selecting powered-lever neutral, or reaching shaft bottom also checks for the nearest compatible loading position and opens the top door when the cart/track geometry is valid. Otherwise a stopped skip hangs with doors shut. **Any skip stop—intermediate station, shaft top/bottom, or operator setting neutral—ends cable motion and the M06 furnace-powered winch's active coal burn; physical stops also AUTO-FLIP the lever to NEUTRAL**. The player must select a direction again to resume; the furnace power may run between stops without continuous player input. The track itself serves as the station with no secondary machine. **Owner delegates search range, alignment tolerance and any small positioning adjustment to Claude**, provided docking is predictable, conserves cargo and never teleports a skip across long distances.
- Counterweights, automated handoff/reset and new extreme rail shapes remain separate future designs, not implicit M04 work. **Coal furnace-on-drum is now assigned to M06, and upright skips with safe docking/transfers to M07.** M06 permits a clicked lever to power one continuous haul without holding the control, but not autonomous repeating transports or restarting after a station/shaft stop.

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

### 5.5 M06 freight railway interface AND coal-powered winch (owner direction; draft only)

- **Expanded M06 scaffold:** [MILESTONE_06.md](MILESTONE_06.md), **not yet authorized for implementation**. M05 is committed/CI verified and awaits owner acceptance. M06 establishes the cart ↔ skip freight-transfer rail interface, **upgrades the existing winch/drum to coal power**, **AND cleans retired items out of the Creative inventory/search**, in preparation for M07 skips. It does NOT complete the entire mine-to-smelter loop.
- **Loading track (owner decision):** a special rail section **locks an ordinary uncushioned freight cart in place** and **determines its front-facing direction** rather than taking the direction from the cart's approach. The front **only unfolds into a downward loading ramp** when a compatible **skip is below** and the oriented frontage is unobstructed; this guides cart cargo into the skip's upper opening. If the skip is **beside** the track, the cart's front **remains closed** and receives freight from the skip's built-in chute and gate. An empty block in front is clearance for the ramp, **not by itself permission to unload or open**. No new freight-cart variant, independent loading station, or conveyor system.
- **Loading-track shapedness/ingredients (owner decision):** **shapeless**: **one ordinary vanilla rail + one existing copper cable anchor** anywhere in the crafting matrix. Exact craft output count remains open. Reuse the established copper anchor item. Rail-facing selection, locking/release controls and transfer timing require subsequent scoping.
- **Redstone rail removal:** remove **redstone-dependent rail variants specifically** while preserving **ordinary vanilla rails**. Powered, detector and activator rails are the provisional affected set, subject to explicit confirmation and dependency/structure/recipe testing before implementation. This is not approval to remove useful standalone non-rail redstone mechanisms or invent a replacement power grid.
- **M06 retired-item/Creative cleanup (owner-settled scope):** audit **already-disabled items and block-items from M01–M05** and remove surviving catalog entries from **every normal Creative tab and Creative search**, plus remove M06-retired powered/detector/activator rail entries. **Audit candidates:** elytra, all shulker boxes, only the retired granite/diorite/andesite/tuff families and variants, ender chest, chest minecart, and **vanilla furnace-minecart item** still showing in Creative as Cobblestone Engine despite the intended **furnace-on-ordinary-cart** construction. Some candidates may already be hidden; verify first. Prevent accidental ordinary recipe/loot/acquisition leaks where features already promised retirement. **Do not delete registry IDs, break structure/save compatibility, or hide useful ordinary items.** Blast furnace is reserved for M08; gold and chainmail armor for M09. Creative-item audit and real-client Creative search/tab verification are M06 tasks, **not immediate code authorization**.
- **No player pushing for non-passenger carts:** players cannot shove **uncushioned ordinary freight minecarts** into motion through direct physical contact; cushioned passenger carts remain manually pushable. This must **not** block legitimate engine, coupling, cable, gravity or other ordinary movement. Confirm any special rules for the Cobblestone Engine before implementation.
- **Coal-powered winch upgrade (M06 owner-settled; tuning still open):** Apply a **vanilla furnace to the free side of the existing winch drum**, retaining cable and anchor installations but **replacing the manual crank with a latching three-position lever**. Lever **BACKWARD = PULL**, **CENTER = NEUTRAL/OFF**, **FORWARD = FEED**. **Right-click on the lever advances exactly one setting in a three-state repeating cycle**; no mouse holding is required. In PULL or FEED it continues to drive until the lever is changed to neutral or the moving cart/skip reaches a physical stop. **A physical stop auto-flips the lever visibly to NEUTRAL**, so it cannot restart unexpectedly. Steve may initiate the haul then walk away, but the winch does not autonomously choose another route or initiate another trip. This **supersedes the earlier 'must remain at and hold the lever' interpretation**, while remaining simple mechanically powered, player-initiated haulage rather than a generalized automation network. **Right-click the upgraded drum/furnace body with coal in hand to add fuel; empty-hand right-click body inspects stored coal**, distinct from right-clicking the lever. Speed and coal efficiency exceed manual winching. Regular coal is explicitly supported; other fuels, capacity and the remaining lever transitions after **NEUTRAL → PULL** remain open. No additional generator or power grid.
- **Coal burn ends when motion ends (M06 owner decision):** combustion does not continue while the cable-driven load is motionless. Besides manually setting the lever to NEUTRAL, examples include a **minecart derailment**, a minecart **stopped/locked at a loading track**, a later M07 skip **arriving at shaft top/bottom**, and a skip **stopping at an intermediate compatible loading track**, whether the loaded skip is offloading into a non-full receiving cart or an empty skip is positioning to receive freight from a cart. A physical path/transfer stop is a real stop of drive and fuel burn, and **forces the lever to NEUTRAL**; it is **not** a command to continue traveling automatically after transfer. **Preserve the exact remaining burn time of an active partly consumed fuel unit when motion stops, however it stops; after the player right-clicks the lever back into a running direction and the load actually moves, resume combustion from that remainder.** Stopping a flame leaves the unburnt portion of the coal usable: no passive decay while stopped, and no discard, reset/refund to a fresh whole item or duplication of consumed fuel. Persist this remainder through save/load and synchronized multiplayer. **Open:** coal storage capacity/addition quantity, exact fuel-progress serialization, precise speed/efficiency/strength, remaining lever click-state transitions after **NEUTRAL → PULL**, upgraded drum appearance, motor-empty manual behavior and blocked-route safety. Implementation belongs to M06 only **after** owner approves the completed brief.
- **M06 owner-settled cable recipe replacement (NOT implemented yet):** Existing **Cable** becomes a **shaped 3×3 recipe with exactly three iron nuggets vertically down the CENTER column**, every other slot empty; **string is no longer required**. Existing **Cable Coil** becomes a **shaped 3×3 recipe with exactly three cable items vertically down the CENTER column**, every other slot empty; **the old eight-cable ring is retired**. No new cable/coil item variants. See [MILESTONE_06.md](MILESTONE_06.md) for both literal layouts.
- **M06 OWNER DECISION — Unlimited installed cable length (supersedes M04/M05 material-length accounting):** apply **ONE Cable Coil** to an existing drum to equip it with a **reusable cable installation of effectively unlimited total routed length**. Eliminate the M04/M05 **128-block drum capacity**, per-cable **four-block consumable length**, the need to add further coils while extending, and the return-loop length charge. A closed loop can return beside the outgoing strand without a material cost; opening, re-routing or dismantling cannot generate cable items from the apparent distance. **Return one installed coil when the drum is properly stripped/dismantled, regardless of route length**, protecting against loss or duplication. Copper anchors and wood supports, currently **12-block maximum *between consecutive supports***, clear routing, sensible cable geometries, train load/grade and actual winch limits still apply. Unlimited route length does **not** permit unsupported infinite spans or routing through walls. The M05 straight-loop-only constraint remains until separately designed; extend this no-material-length rule to later M07 skip hoists. Do not add an arbitrary new cap or make long routes force-load chunks.
- **Craft outputs, reverse crafting and item weight (M06 follow-up):** ingredient layouts are settled; exact Cable yield was not set by the new recipe. One coil is made from **three cable items**; the existing coil→cable uncrafting path must return **three**, NOT eight, if kept, preventing item duplication. New recipe-derived item weights, exact output counts, safe installed-coil retrieval and route/state serialization must be finalized during the M06 brief/implementation without reintroducing a length budget. Historical M04/M05 implementation descriptions (two cables per old craft, 4-block cables, 8-cable/32-block coils, 128-block drum) remain **historical fact**; M06 changes the behavior prospectively.

- **Future presentation work:** stepped physical cargo-fill levels and replacement of floating debug labels are already agreed separately; not implicitly part of this M06 scaffold.

### 5.6 Armor and modular workwear (owner-approved future direction; not implemented)

- **Remove gold and chainmail armor completely from the player-facing game:** retire all four pieces of each set rather than repurposing them as lesser combat tiers. Remove ordinary crafting/acquisition, creative inventory/search entries, relevant loot/trades and mob equipment as appropriate, with safe old-world/registry compatibility. Gold as a resource remains.
- **Head — Headlamp:** a copper-made wearable helmet/headlamp, offering armor protection **just below iron-tier head armor**. The exact defense stat, recipe, lighting behavior, fuel/operation (if any), and rendering are not yet specified; do not assume dynamic lighting or an extra upkeep system merely from the name.
- **Chest and legs — Overalls (owner-settled):** **ONE equippable overall garment occupies and reserves BOTH chestplate and leggings slots simultaneously**; wearing a separate chestplate or leggings alongside it is prohibited. This is intentional: players must sacrifice *both* normal armor slots to gain the **integrated backpack** restoring **nine inventory slots** (the suppressed third row; M02's 27 → 36 slots including hotbar). Its **total** protection across the two slots is **no higher than leather chestplate + leather leggings combined**. The physical slot-occupancy representation, atomic equip/unequip and compatibility with existing saves need engineering, but the *one item/two slots* rule is not optional. The backpack comes from overalls, not boots.
- **Feet — Spiked Boots:** grant the named bonus **Spelunker's Speed**, improving climbing on **both ladders and hanging Spelunker's Ropes**. This bonus works **with the boots alone**, without Headlamp or Overalls, and grants no extra inventory slots. Exact climb multiplier and armor/crafting details are open. When the complete spelunking outfit is equipped, the displayed bonus **Spelunker's Speed is REPLACED by Trail Blazer**, which retains the same faster climbing; never stack a second climbing modifier.
- **Feet — Mining Boots:** grant the named boots-only effect **Mule**: permit collecting/carrying designated heavy materials **above the M02 64-unit limit**, with an **increasing movement-speed penalty as over-limit weight rises** rather than instantly rejecting pickups. The boots do **not** grant backpack inventory slots. **Mule appears while wearing Mining Boots without the complete mining outfit; when the full outfit is worn, the displayed Mule effect is replaced by Old Pro**, which preserves Mule's overloaded-carrying behavior without a second stacking modifier. Exact penalty curve, whether there is an absolute maximum, and safety/movement interactions remain open. Cobbled stone, gravel and sand keep their established weight rules.
- **Mix-and-match principle:** wear the **same overalls/headlamp** and select either **Spiked Boots** for mobility or **Mining Boots** for overload carrying. These are two alternatives in the **boots slot**, not mutually exclusive chestplate sets. Armor and combat rules outside this design (including jump criticals) are not automatically changed; minecarts/skips remain useful for serious bulk freight.
- **Full-workwear set bonuses (owner decisions; tuning open):** A complete outfit means **copper Headlamp + single two-slot Overalls + the matching pair of boots**. With **Spiked Boots**, the single set bonus **Trail Blazer REPLACES the displayed Spelunker's Speed bonus** and contains its faster climbing **without any extra stacking multiplier**; it also (a) shows ghosted nearby **torches, ladders and hanging ropes through walls**, **yellow** for **player-placed** infrastructure (any player) and **blue** for **world-generated** infrastructure, and (b) reduces **skeleton archery accuracy** against the wearer. Trail Blazer requires the full outfit; its effects are not independent buffs named Shiny or Trail Sense.
- **Mining set bonus — Old Pro (owner decision):** only the full **Headlamp + Overalls + Mining Boots** outfit grants **Old Pro**, which **replaces the displayed boots-only Mule bonus while retaining the same overweight-carrying behavior**. Old Pro additionally provides (a) **functional offhand pickaxe block mining**, letting the player keep a sword, axe or other item in the main hand without putting away the pickaxe; and (b) a **subtle red glow along the bottom screen edge ONLY when a hostile monster is behind the player, within 20 blocks, AND has a direct unobstructed sightline to Steve's upper body/neck in his SECOND block of height** (an eye-level-to-neck/head line-of-sight check, not a check to the feet). **Monsters behind walls, in sealed caves or otherwise without that clear sightline do not trigger the alert**, even if in range. 'Clear path' here means **unobstructed line of sight**, not a monster-navigation/pathfinding route. Behind is determined relative to the player's facing; the precise rear angular threshold and neck-point height are implementation tuning. This is a directional threat cue, NOT a general mob radar or full-screen overlay. The pickaxe remains a mining tool: no new offhand combat attack, doubled mining speed, or change to jump criticals. Exact offhand-mining gesture/priority versus main-hand attack or chopping is open. **Old Pro supersedes the tentative +5% mining-speed set perk; do not retain or stack it.**
- **Trail Blazer implementation boundaries:** reveal nearby qualifying blocks regardless of who placed them; **yellow = player placed, blue = originally world generated**. Track placement provenance safely with appropriate save/load and multiplayer behavior; do not presume vanilla stores that provenance for existing blocks. For pre-existing worlds / unclassifiable blocks, pick a conservative documented fallback rather than inventing origin. Keep outlines subtle and range-bounded; do not highlight ore, mobs or arbitrary blocks through walls. The skeleton inaccuracy effect is part of this same named set bonus.
- **Inventory safety before implementation:** if overalls are unequipped or replaced when their third-row slots contain items, those items must not disappear, duplicate, become permanently inaccessible, or permit swapping exploits. Preserve save/load, death, sorting and shift-click semantics; the behavior for occupied slots is open.
- **Still open:** technical two-slot-equipment handling for one-piece overalls, headlamp illumination, recipe output count/models/exact protection and durability, combat armor tiers, piglin behavior without gold armor, Mule overload/climb tuning, **Trail Blazer range/provenance fallback and skeleton accuracy penalty**, plus **Old Pro offhand-mining input/priority and the exact rear-angle, second-block neck-target ray and red-edge intensity**. **All four crafting ingredient layouts are now settled below.** The requirement for unobstructed monster-eye-to-upper-player line of sight within 20 blocks is settled; the tentative +5% mining-speed set perk remains replaced. This is a **future armor milestone**, not authorization for M05 or M06.

#### Exact shaped workwear recipes (owner decision; not implemented)

All four recipes are **shaped 3×3 crafting layouts**. The specified positions and empty slots matter; do not convert these to shapeless crafting recipes. "Chest" means the **ordinary vanilla chest block/item**; "leather pants" means **vanilla leather leggings**. Reuse vanilla copper ingot, torch, string, leather armor, pointed dripstone and iron ingot; no additional intermediate ingredients.

**Headlamp** — 5 copper ingots and 1 torch:

```text
[copper ingot] [copper ingot] [copper ingot]
[copper ingot] [torch]        [copper ingot]
[empty]        [empty]        [empty]
```

**Overalls** — 2 string, 1 vanilla chest, 1 leather chestplate, 1 leather leggings:

```text
[string] [chest]              [string]
[empty]  [leather chestplate] [empty]
[empty]  [leather leggings]   [empty]
```

**Spiked Boots** — 1 leather boots, 1 pointed dripstone underneath:

```text
[empty] [empty]            [empty]
[empty] [leather boots]    [empty]
[empty] [pointed dripstone][empty]
```

**Mining Boots** — 1 leather boots, 1 iron ingot underneath:

```text
[empty] [empty]         [empty]
[empty] [leather boots] [empty]
[empty] [iron ingot]    [empty]
```

Recipe layouts and components are settled. The number of output items per craft is not explicitly set here; default crafting behavior may be proposed in the future milestone brief. Headlamp's torch ingredient does not itself decide illumination mechanics or fuel consumption, which remain open. Overalls still occupy BOTH chest and leggings slots simultaneously.



### 5.7 Planned M06–M09 milestone order (owner planning; NOT implementation authorization)

- **M06 — loading track/freight-cart handling, coal-powered winch, unlimited cable and Creative cleanup:** the freight interface and rail cleanup already drafted, now plus furnace-on-drum power, **replacement shaped cable/coil recipes (three iron nuggets center column → cable; three cables center column → coil), and ONE coil installing UNLIMITED routed cable without the 128-block drum limit, strand-length cost or extra extension coils**. Revise reverse crafting to prevent item duplication and **retain physical anchor/span/collision requirements**. Also audit/hide existing retired items from Creative tabs/search. **Furnace mounts on drum's free side, replacing crank with a latching lever (back = pull, center = neutral, forward = feed), advanced one setting per right-click.** Coal right-click loads fuel, empty-hand right-click on winch body reports stored coal. PULL/FEED keep running without held input; physical path/transfer stops automatically reset lever to NEUTRAL. Motion-only burning and **preserved partial burn time** across stops are settled. **Open:** fuel capacity/addition quantity, exact lever state order, numerical tuning; see [MILESTONE_06.md](MILESTONE_06.md). Not yet authorized.
- **M07 — upright mining skips:** implement the existing 512-unit vertical skip concept, triangular bridle, one main hoisting cable, door/chute and automatic loading-track transfer and safe stops using the M05/M06 winch systems. No smelter included; docking and cargo details need a dedicated brief.
- **M08 — standalone industrial smelter:** build the large raw iron/copper/gold smelter with its hopper, industrial block fuel and safe output mold; retire blast furnace and slow normal-furnace raw-ore processing as specified in [ROADMAP.md](ROADMAP.md). Skips were scheduled earlier in M07 and should not be reimplemented here; no steelmaking or factory network by default.
- **M09 — armor and full modular workwear implementation:** implement agreed armor removals, exact recipes, Headlamp, two-slot Overalls/backpack, Spiked Boots/**Spelunker's Speed**, Mining Boots/**Mule**, plus complete-set **Trail Blazer** and **Old Pro** with ALL defined functions. Open lighting, controls, tuning and compatibility require a dedicated finalized brief.
- The milestone order is the **owner's planned sequence**, not permission to code. M05 must finish, M06 is still a draft, and M07–M09 each need explicit final specifications and authorization.

## 6. Open decisions requiring a future milestone brief

Only record settled answers in sections 1–5 after the owner decides them.

| System | Unsettled details |
|---|---|
| Cable motion | **M05 loop direction settled** (outgoing grip strand and side-offset return); **M05 manual** bare-hand winch input is **hold left = feed away**, **hold right = pull toward**, including future manual vertical hoists. **M06 coal upgrade replaces the crank with a latched right-click-cycle lever**, PULL/back, NEUTRAL/center, FEED/forward, and auto-neutral on physical stops. Circulating loops reverse direction without changing wound length. Input interception, terminal gesture, guide clearance, braking and safe release still need engineering validation. **M05 as built (provisional):** straight closed loops only, anchor-on-last-anchor gesture, end clearance, hold on stop (section 5.0); loops round curves and tension/sag remain open. |
| Cobblestone Engine | **M05 provisional:** mass/force/grade numbers, coal per tick of driving, seat and ground controls, hold on stall (section 5.0). Open: balance after playtesting, downhill behaviour. |
| Coupling | **M05 provisional:** iron chain, shears, fixed 1.6-block spacing. Open: behaviour on tight curves and grade transitions beyond what M05 tested, slack. |
| Rails | Geometry and construction of extended extreme slopes; whether steep routes require special rail pieces; speed and crash damage. |
| M06 freight track, coal winch, redstone rails and retired-item Creative cleanup | **Settled:** shapeless loading track = vanilla rail + copper anchor; track determines cart front and skip-relative loading modes; retire redstone rails safely and disallow manual freight shoving. **Powered winch:** apply furnace on free side of drum, replacing crank with **three-position latching lever (PULL/back, NEUTRAL/center, FEED/forward)**; **right-click lever advances one state** and PULL/FEED run without continuous input. **At derailment/path end, cart dock or M07 skip dock/shaft end, auto-flip to NEUTRAL**, stopping and pausing burn until player starts a new segment. Right-click coal on drum to load; bare-hand right-click body inspects supply. Remaining partly used coal burn survives stop and save/load. **Settled:** first click from neutral selects PULL. **Open:** subsequent cycle transitions, fuel capacity, speed/fallback, loading track facing/release and transfer details. M06 not authorized. |
| Armor and utility gear | **Settled:** retire gold/chainmail; copper Headlamp; one-piece Overalls occupy BOTH chest/legs (+9 inventory slots, low protection). **Spiked Boots alone: Spelunker's Speed**, replaced in full outfit by **Trail Blazer** (climbing, nearby yellow/blue infrastructure outlines and skeleton inaccuracy). **Mining Boots alone: Mule** (overweight carrying with progressive slowdown), replaced in full outfit by **Old Pro** (retains Mule and adds offhand pickaxe mining and a red bottom-edge warning). Warning triggers ONLY for hostile monsters **behind, within 20 blocks, with unobstructed line of sight to Steve's second-block-high upper body/neck**; not through walls. Old proposed +5% mining speed retired. **Recipes settled:** exact shaped layouts for Headlamp, Overalls, Spiked Boots, Mining Boots below; output count/design models still open. **Open:** rear angle and ray target precision, offhand controls, provenance, balance and equipment safety. Not implementation-authorized. |
| Incline winches | M05 unupgraded crank requires held left/feed or right/pull. **M06 upgraded winch** has **PULL/back, NEUTRAL/center, FEED/forward** lever; right-click cycles one latched state, which drives until neutral is selected or a physical path endpoint automatically sends it to neutral. No need to hold lever or remain nearby between stops. Coal inserted by hand-right-clicking drum; empty-hand body interaction inspects stored coal. Burn only during motion, preserving remaining burn time through stops/save-load. **Settled:** first neutral click selects PULL. **Open:** subsequent cycle transitions, coal capacity, haulage/speed and fallback/safety. No generalized network or auto-restart. |
| Mining skips (planned M07) | **Settled:** one block wide × three blocks tall, four-block clearance for top door; recipe, 512 capacity, exposed lower chute; triangular bridle and single-line upright hoisting by existing drum. **Loaded skip may stop mid-lift at a not-full receiving freight cart on a loading track**, discharging into it; an **empty skip instead treats the waiting cart as its incoming cargo source**, opening top door in valid geometry. Shaft top/bottom, docking and manual lever neutral **stop motion and coal burn and auto-return the powered winch lever to NEUTRAL at physical stops**; no automatic restart. Claude may select local alignment range/tolerance, conserving cargo. Still open: horizontal depth, cable/bridle accounting, transfer safety and collision rules; no mandatory extra station/guides. |
| M04/M06 cable overhaul | M04/M05 historically used 4-block cable items, 32-block coils and 128-block drum capacity. **M06 settled: one installed coil allows UNLIMITED cumulative routed cable, with no length charge on outgoing or return strand**; *per-span* spacing and routing clearance still matter. Open: final cable craft output, reversible coil→cable ratio update, adjusted heavy weights and safe installed-coil retrieval. No M06 implementation authorization yet. |

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
