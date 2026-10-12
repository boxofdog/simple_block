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
| Later | **Coal-powered winch attachment** | Upgrade the existing hand winch with localized fuel power; no generalized network. |
| Later | **Refined train coupling and operation** | Expand/adjust only after M05's minimum viable consist can push and pull. |
| Later | **Incline counterweights and upright mining skips** | Separate vertical lifting, safe reset and freight-transfer milestones; design unloading into a raised receiving hopper as part of the mine-to-smelter flow. |
| Later | **Bulk ore transfer and large smelter** | Gravity-charge raw ore from a minecart or vertical skip into a substantial smelter's hopper. Remove the vanilla blast furnace and slow ordinary-furnace raw-ore processing to make bulk smelting worthwhile. Details below; do not start within M05. |
| Later | **Extreme-grade rails and downhill crashes** | Design three-block rises/drops and consequences separately. |
| Later | **Full mining/logistics playtest** | Once powered rail haulage and vertical transport make a representative loop possible. |

Small standalone improvements (such as faster ladders) may be scheduled between mechanical milestones. **No milestone after M05 starts until the owner authorizes it.** Milestone IDs after M05 are assigned when their briefs are approved; historical placeholders are not binding.

## Later workstreams

- **Villages:** terrain suitability, larger buildings, population and destinations, pubs, centralized stores, workshop services, real golem protection.
- **Character and equipment:** armor/loadout redesign, climbable ropes and faster ladders.
- **Geology/metallurgy:** bulk smelting and hopper-fed ore handling, alluvial iron sand/gravel, steel, useful long-lived anvil/smithing.
- **Rules and usability:** selective redstone-system redesign, guidebook, cargo visuals and player-facing text polish.

### Future winch mounting and shaft layout (owner-approved direction)

- **Overhead cable drums should be the default installation orientation** for mine haulage: mount the drum into the wooden support/headframe so its cable runs at roughly the same level as underside-mounted copper rollers. This keeps the railway below unobstructed and avoids a floor-to-ceiling cable transition.
- Preserve the current M04 drum, hooks, anchors, routing gestures, connecting rod and models. **Do not roll back or replace the implemented cable infrastructure**; overhead mounting and powered motion are future additive work.
- Leave room beneath the overhead assembly for an elevated rail's minecart to stop and **dump bulk freight into an upright mining skip below**, which a shaft hoist can move vertically. Exact transfer mechanics and headframe geometry are still to be designed.
- A hand-operated overhead winch must have an accessible operating position; a later coal engine attachment should fit the same assembly. Other mounting configurations need not be prohibited merely because overhead is the normal layout.
- **M05 design direction:** preserve the open-ended construction route and add a reversible closed loop with an offset return strand and terminal pulley. Specific pulley conversion and cable-routing edge cases are detailed in the M05 brief; they must preserve existing anchors and the loose hook.

### Future bulk ore smelting and freight transfer (owner-approved direction; not authorized)

- **Batch-based raw-material handling, not Create-style ore conveyors.** The practical mine-to-smelter loop should move bulk ore in minecarts or upright mining skips, then discharge it **by gravity into a raised receiving hopper** on a substantial, constructed smelter. No requirement to transport every individual ore item through exposed processing belts.
- **Two processing scales:** retain the ordinary furnace for early/small-scale iron and other familiar uses, but **slow its raw-ore smelting** so it does not compete with bulk processing. **Remove/disable the vanilla blast furnace** in a future authorized metallurgy milestone to avoid a redundant intermediate metal-processing shortcut.
- **Large smelter:** a visually substantial piece of settlement infrastructure with a hopper/charging point, a fuel demand, and a deliberate output or tapping point. Its functional reward is handling large quantities of ore efficiently, making rail elevation, terminal placement, vertical skips and a permanent mine-to-settlement supply line valuable.
- **Physical delivery:** design unloading from elevated rails and skips alongside the receiving hopper, not as independent item-teleportation or general automatic-factory systems. Material should never disappear or duplicate when an unloading attempt fails or the recipient is full.
- **Open design questions before a milestone brief:** smelter structure/footprint, whether it takes one load or multiple stored loads, which ores/materials it processes, furnace slowdown amount, fuel and smelting throughput, hopper capacity/behavior, player-controlled vs passive dumping, casting/tapping/output collection and integration with later steelmaking. A larger internal reservoir is a **suggestion**, not yet approved. Do not add mandatory crushing, waste handling, conveyor networks or industrial process simulation by assumption.

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
