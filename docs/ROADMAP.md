# Simple Block — Roadmap

**Goal:** Make Minecraft genuinely about mining, crafting and transforming a dangerous world. Discover deposits, secure land, develop mines, and connect them to a permanent home with physical freight infrastructure. See [VISION.md](VISION.md).

## Current state

| Milestone | Feature | Status |
|---|---|---|
| M01 | Nights cannot be skipped; no natural phantoms, functional elytra or shulker boxes | **Shipped** |
| M02 | Geology, mining tiers, ore deposits, heavy inventory, hostile light attraction, hunger and QoL | **Shipped** |
| M03 | Existing minecarts become 256-item single-commodity freight carts; cushions enable riding; storage bypasses removed | **Shipped** |
| M04 | Cable coils, drum, underside copper anchors, routed overhead cable and minecart gripping rod | **Implemented / awaiting owner acceptance** |
| M05 | Reversible cable loops, default hand crank, Cobblestone Engine and drum-side rendering fix | **Authorized / specification prepared; not implemented** |

M04 has 95 passing server game tests, a passing real-client test and CI Build #16 according to its [report](MILESTONE_04_REPORT.md). Cable/grip **does not yet move carts**. Do not mark owner acceptance or full mining-loop playtesting complete without confirmation.

## M05 — authorized handoff (not yet implemented)

[**MILESTONE_05.md**](MILESTONE_05.md) defines four connected work packages: **(A)** close M04 cable routes into reversible circulating loops with a discreet side-return strand and terminal pulley; **(B)** always-visible hand crank and usable manual drive; **(C)** a bidirectional, coal-powered **Cobblestone Engine** made by applying a furnace to a vanilla cart, including minimal functional train coupling; **(D)** fix the observed drum rendering defect (BUG-001). Preserve all M04 interaction and visual foundations; implement in separable, tested stages.

M05 is a larger combined milestone specifically requested by the owner, **not** permission for unrelated systems. M04 owner acceptance still requires an explicit confirmation; do not silently mark it accepted.

## After M05 (suggested; not automatically authorized)

| Order | Independent future deliverable | Notes |
|---|---|---|
| Later | **Coal-powered winch attachment** | **Next high-priority design question:** upgrade the same manual drum with localized fuel power; determine attachment, fueling, actuation, stopping and player interaction. No generalized network. |
| Later | **Refined train coupling and operation** | Expand/adjust only after M05's minimum viable consist can push and pull. |
| Later | **Incline counterweights and upright mining skips** | Skip upright dimensions, recipe, 512 capacity, built-in chute and single-line hoist with triangular bridle now specified below. Vertical lifting, safe reset and freight-transfer implementation remain separate; connect to the mine-to-smelter flow. |
| Later | **Bulk ore transfer and large smelter** | Gravity-charge raw ore from a minecart or vertical skip into a substantial smelter's hopper. Remove the vanilla blast furnace and slow ordinary-furnace raw-ore processing to make bulk smelting worthwhile. Details below; do not start within M05. |
| Later | **Extreme-grade rails and downhill crashes** | Design three-block rises/drops and consequences separately. |
| Later | **Full mining/logistics playtest** | Once powered rail haulage and vertical transport make a representative loop possible. |

Small standalone improvements (such as faster ladders) may be scheduled between mechanical milestones. **No milestone after M05 starts until the owner authorizes it.** Milestone IDs after M05 are assigned when their briefs are approved; historical placeholders are not binding.

## Later workstreams

- **Villages:** terrain suitability, larger buildings, population and destinations, pubs, centralized stores, workshop services, real golem protection.
- **Character and equipment:** armor/loadout redesign, climbable ropes and faster ladders.
- **Geology/metallurgy:** bulk smelting and hopper-fed ore handling, alluvial iron sand/gravel, steel, useful long-lived anvil/smithing.
- **Rules and usability:** selective redstone-system redesign, guidebook, stepped freight-cart cargo visuals and player-facing text polish.

### High-priority scope review (design status; M05 still in progress)

- **#1 Vertical mining skips/hoisting and #2 freight loading, unloading and transfer:** **design-scoped, not implemented**. Skip dimensions, construction recipe, built-in chute, track-driven transfer, bridle, winch lifting and basic automatic docking are documented below. Claude chooses predictable, local alignment tolerance when that later milestone is authorized; retain implementation-level edge cases for its brief.
- **#3 Coal-powered winch attachment:** **next design discussion**; the power-upgrade mechanics and controls still need a decision.
- **#4 Train stopping/braking/stations and #5 M05 locomotive/coupling/cable balancing:** **defer further design until M05 finishes and is playtested**; assess actual in-game behavior first.
- **#6 Gravel/sand freight eligibility:** **already owner-approved but not implemented**; user wants it scheduled only after M05 playtesting, with cargo-tag and test updates then. No additional scope discussion required.
- **#7 Minecart cargo visuals/dev text:** **design decided, implementation later**. Ordinary vanilla freight carts retain their models but show a **visible material surface rising in discrete fill steps** proportional to the 0–256 load, analogous to a cauldron or composter. Provide new owner-paintable texture/art assets with editable sources; remove the intrusive always-floating cargo-count label in favor of the fill visual while retaining deliberate exact-quantity inspection. Exact step count, mesh/render method and how to depict different commodities can be chosen during implementation; no extra cart type or UI machinery. Other noisy development text remains logged in [UX_TEXT_LOG.md](UX_TEXT_LOG.md).
- **#8 Spelunker's ropes and faster ladders:** **rope recipes and hanging/extension behavior now design-scoped; not implemented**. Faster ladder speed remains open; rope climbing, retrieval and placement edge cases can be settled in its implementation brief.

### Future spelunker's rope (owner-approved direction; not authorized)

- **Rope shaped recipe:** three string items vertically in the center crafting column. Result quantity remains unspecified; do not assume a multiple-item yield.

  ```text
  [empty] [string] [empty]
  [empty] [string] [empty]
  [empty] [string] [empty]
  ```

- **Spelunker's Rope shaped recipe:** one existing **copper cable anchor** above one rope in the center column (reuses the current anchor item, not a new kind of anchor).

  ```text
  [empty] [empty]         [empty]
  [empty] [copper anchor] [empty]
  [empty] [rope]          [empty]
  ```

- Place the Spelunker's Rope on the **bottom or side face of a block**. Its anchor fixes at that block; the climbing rope **hangs vertically downward through open air** without needing intermediate attachments, support posts or guides.
- **Hold right-click with Rope in hand** to extend the anchored line downward in **12-block increments**, using the supplied rope resource for each increment. Once it reaches solid ground, any extra length **coils visibly into a bundle on the lowest block** rather than extending through the terrain. Details of quantity spent, coil retrieval and support removal are for the future rope milestone; avoid silently providing unlimited free rope.
- This is a **temporary spelunking/access tool**, not a moving mechanical winch cable. Preserve M04/M05 copper cable, anchors and hauling logic; climbing and retrieval controls and the speed of ordinary ladders remain separately tunable.

### Future winch mounting and shaft layout (owner-approved direction)

- **Overhead cable drums should be the default installation orientation** for mine haulage: mount the drum into the wooden support/headframe so its cable runs at roughly the same level as underside-mounted copper rollers. This keeps the railway below unobstructed and avoids a floor-to-ceiling cable transition.
- Preserve the current M04 drum, hooks, anchors, routing gestures, connecting rod and models. **Do not roll back or replace the implemented cable infrastructure**; overhead mounting and powered motion are future additive work.
- Leave room beneath the overhead assembly for an elevated rail's minecart to stop and **dump bulk freight into an upright mining skip below**, which a shaft hoist can move vertically. Exact transfer mechanics and headframe geometry are still to be designed.
- A hand-operated overhead winch must have an accessible operating position; a later coal engine attachment should fit the same assembly. Other mounting configurations need not be prohibited merely because overhead is the normal layout.
- **M05 design direction:** preserve the open-ended construction route and add a reversible closed loop with an offset return strand and terminal pulley. Specific pulley conversion and cable-routing edge cases are detailed in the M05 brief; they must preserve existing anchors and the loose hook.

### Upright mining skip — specified physical form (owner-approved direction; not authorized)

- **Size and clearance:** skip body is **one block wide and three blocks tall** (NOT a 1×3 horizontal footprint); it needs **four blocks of vertical clearance** so its top loading door can unfold toward the loading track. Its horizontal depth and precise collision shape have not been finalized. The **upper two blocks** are the ore container, whose **floor is a sliding discharge gate**. The **bottom block is an exposed/open-air angled chute**, not an enclosed chamber; ore falls onto it after the gate slides open and travels sideways into a receiving cart. The chute is permanently built into the skip, with no extra station or chute block required.
- **Capacity:** **512 units** of ore. Exact mixed/single-material rules need confirmation.
- **Crafting recipe** (shaped):

  ```text
  [empty] [cable]         [empty]
  [empty] [iron trapdoor] [empty]
  [iron]  [minecart]      [iron]
  ```

- **Physical freight transfer:** a specially designated load/unload track is itself the interface. A cart above the skip empties downward through the top opening. **When the skip is stationary and aligned with a receiving minecart on the special load/unload rail, its upper compartment's sliding floor gate automatically opens**, discharging ore down the exposed lower chute and into that cart. No separate docking structure, interaction or external chute should be required. Transfers must respect receiving capacity and cargo type; gate safety and partial loads need implementation design.
- **Cable attachment:** right-click the skip with the existing cable's loose hook to attach it; the connection creates a **triangular two-leg suspension bridle**, anchored to either side of the skip's ore intake and converging to **one attachment point above the skip**. From that apex, **one main hoisting cable** rises to the existing overhead guide wheel and onward to a higher winch. The visible bridle appears as part of the hook-attachment gesture; its material accounting and clearance still need design. Do not assume two independent cables running to the wheel or invent unaccounted-for free cable.
- **Vertical motion (owner decision):** the skip moves **straight up or down directly beneath its hoist wheel**, using the **normal power/speed of its connected winch** through the single hoisting cable and bridle. It remains upright, with **no swinging, pendulum physics, or mandatory guide rails/timber-lined shaft system**. Ordinary clearance/collision with the surrounding world still matters; do not add a separate skip-only drive or new guidance machinery.
- **Manual controls (owner decision; universal winch convention):** while targeting the built-in crank **with an empty hand**, **hold left-click to FEED cable out/away from the winch**, and **hold right-click to PULL cable toward the winch**. Release to stop/hold. This applies to **lateral railway hauling as well as vertical hoisting**: left lowers a skip, right raises it. For a **closed circulating railway loop**, feed/pull denotes **direction of movement along the hauling strand**, not an actual increase/decrease in cable stored on the drum; the loop's total cable length stays fixed. Override vanilla block breaking while left-clicking the crank without sacrificing intentional drum dismantling. This is an M05 crank-control clarification, not authorization to implement skips in M05.
- **Automatic skip stops and doors (owner decision):** a skip **carrying freight** stops at the first compatible unloading-track position encountered, aligning its **bottom chute** beside that track; when a receiving freight cart is correctly aligned, the sliding discharge gate opens to transfer items within capacity. An **empty skip descending** pauses when the player releases left-click or when it reaches the shaft bottom, then searches for the **nearest compatible loading-track position** and opens its **top loading door** toward that rail. Otherwise the skip hangs at its stopped height with both doors shut. The transfer rail is the station; no extra station or manually operated docking equipment.
- **Alignment behavior delegated to Claude:** choose the exact nearest-track search range, detection tolerances and any small vertical docking adjustment during the later skip implementation. The owner does **not** require a specific numeric range or snapping algorithm. Keep alignment local and predictable; never make cargo disappear/duplicate or unload into open air just because a rail is nearby.
- **Not M05 work.** The later skip brief should leave exact stop/docking tolerance to Claude while preserving predictable behavior and safe freight transfer. Horizontal depth, collision envelope and unfolded-door clearance still need definition. Universal left-feed/right-pull input applies to the M05 winch; skip operation/automatic station logic remains later work. No extra guided-shaft mechanic is required.

### Future bulk ore smelting and freight transfer (owner-approved direction; not authorized)

- **Batch-based raw-material handling, not Create-style ore conveyors.** The practical mine-to-smelter loop should move bulk ore in minecarts or upright mining skips, then discharge it **by gravity into a raised receiving hopper** on a substantial, constructed smelter. No requirement to transport every individual ore item through exposed processing belts.
- **Two processing scales:** retain the ordinary furnace for early/small-scale iron and other familiar uses, but **slow its raw-ore smelting** so it does not compete with bulk processing. **Remove/disable the vanilla blast furnace** in a future authorized metallurgy milestone to avoid a redundant intermediate metal-processing shortcut.
- **Large smelter:** a visually substantial piece of settlement infrastructure with a hopper/charging point, a fuel demand, and a deliberate output or tapping point. Its functional reward is handling large quantities of ore efficiently, making rail elevation, terminal placement, vertical skips and a permanent mine-to-settlement supply line valuable.
- **Physical delivery:** design unloading from elevated rails and skips alongside the receiving hopper, not as independent item-teleportation or general automatic-factory systems. Material should never disappear or duplicate when an unloading attempt fails or the recipient is full.
- **Fuel and casting direction:** the industrial smelter accepts **blocks of coal, charcoal, or dried kelp** as its only planned fuels. Coal blocks and dried kelp blocks are vanilla; a charcoal block would require a new item/recipe, to be scoped separately. Smelt raw iron, copper or gold into the corresponding **metal blocks**, preserving vanilla block-to-ingot crafting reversibility. No mandatory ore crushing or refinement stages.
- **Animated channel, practical mold inventory (owner decision):** show molten metal pouring down the front casting channel into a mold as an **animation only**, not a simulated fluid or a block the player must repeatedly break. **Interact with the mold to open/collect its output inventory; it stores at most one stack (64) of a single iron, gold or copper block type.** Finished blocks accumulate there automatically without requiring a click after every casting. No mixed block types in the same mold at once; when full or containing a different type, further output must wait safely rather than vanish. Preserve partial progress when raw-item quantities are not divisible by nine.
- **Open design questions before a milestone brief:** smelter structure/footprint, raw-input reservoir and hopper capacity, exact charcoal-block crafting rule, furnace slowdown, smelter fuel and processing rates, whether and how users switch between metal types, manual vs passive freight dumping, and later steelmaking integration. No conveyor networks or industrial process simulation by assumption.

Full design context lives in [BACKLOG.md](BACKLOG.md), confirmed implementation choices in [DECISIONS.md](DECISIONS.md), and the running visible-text cleanup list in [UX_TEXT_LOG.md](UX_TEXT_LOG.md).

## Bug log (observed; not automatically scheduled)

Record reproducible defects here as they are noticed. This list is **not** permission to change active milestone scope. Use a new `BUG-###` ID for each distinct issue; keep the symptom, location, reproduction/evidence, status, and eventual fix/verification concise. Maintain player-facing *wording* cleanup separately in [UX_TEXT_LOG.md](UX_TEXT_LOG.md).

| ID | Area | Observed problem | Evidence / suggested check | Status |
|---|---|---|---|---|
| BUG-001 | M04 cable drum — side geometry | **Ghosting / flickering stripe-like texture artifacts** on the side of the drum/frame, particularly around the angled side panels. | Close-up screenshot shared by the owner in chat. Inspect the in-game model and source Blockbench geometry at several angles for overlapping or nearly coplanar faces (**possible z-fighting; unconfirmed**) and for UV/rendering overlap. Verify visually in the actual client before closing. | **OPEN — targeted for M05; cause unconfirmed** |

Keep bugs open until the fix is observed in game. Do not conflate completion of tests with confirmation that a visual defect is gone.

## Milestone commit handoff

This is the **shared handoff between Claude Code, ChatGPT and the project owner**. At the end of **each authorized milestone**, Claude updates this section **in the repository**, not only in its chat. A checked box means that named party performed that check; it is not a substitute for the checks below. Claude can add brief notes and links. After independent review and owner acceptance, keep one compact receipt in the history table and reset this checklist for the next authorized milestone. Do not start another milestone automatically.

**Milestone being handed off:** **M05 — reversible cable haulage, Cobblestone Engine, hand crank and BUG-001** ([brief](MILESTONE_05.md)); implementation not yet started/verified.

- [ ] **Claude — code committed and pushed to `main`.** Implementation commit SHA(s): `—` (link to commits; use **code commit**, not a subsequent roadmap-only commit).
- [ ] **Claude — tests and closeout report recorded.** Report: `—` · Tests run/results: `—` · Not tested/known issues: `—`.
- [ ] **Claude — CI verified for the implementation SHA.** Workflow run URL: `—` · Result: `—` · `simple-block-mod` artifact: `—`. If unavailable, leave unchecked and explain.
- [ ] **ChatGPT — independently verified on GitHub.** Confirm commit is reachable from `main`, inspect changed files/report and corresponding CI result. Verification notes/date: `—`. **Claude must not check this box.**
- [ ] **Owner — accepted/playtested.** Acceptance note/date: `—`. **Only the owner can authorize acceptance; Claude and ChatGPT must not infer it from tests.**

**Claude's handoff notes:** _What changed, what remains open, and any deviations from the brief._

**Verified milestone receipts** (one row per accepted milestone; older details remain in milestone reports):

| Milestone | Implementation commit(s) | CI / artifact | Verified by ChatGPT | Owner accepted |
|---|---|---|---|---|
| M04 — cable infrastructure | `215a24a` (per [report](MILESTONE_04_REPORT.md); not independently checked here) | Build #16, 95 tests (per report) | Pending | Pending |

## Working rules

- **Addition by subtraction:** prefer making existing Minecraft objects useful over adding redundant variants.
- **Useful infrastructure, not chores:** machinery should permanently improve what players can do.
- **One small milestone at a time:** focused scope, build/actual-client verification where appropriate, closeout report, then stop.
- **Editable assets:** preserve source Blockbench models and owner-authored textures.
- **Single source of truth:** milestone specs and reports hold implementation detail; this roadmap only tracks priorities and progress. Never silently promote backlog suggestions to requirements.
