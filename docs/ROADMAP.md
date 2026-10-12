# Simple Block — Roadmap

**Goal:** Make Minecraft genuinely about mining, crafting and transforming a dangerous world. Discover deposits, secure land, develop mines, and connect them to a permanent home with physical freight infrastructure. See [VISION.md](VISION.md).

## Current state

| Milestone | Feature | Status |
|---|---|---|
| M01 | Nights cannot be skipped; no natural phantoms, functional elytra or shulker boxes | **Shipped** |
| M02 | Geology, mining tiers, ore deposits, heavy inventory, hostile light attraction, hunger and QoL | **Shipped** |
| M03 | Existing minecarts become 256-item single-commodity freight carts; cushions enable riding; storage bypasses removed | **Shipped** |
| M04 | Cable coils, drum, underside copper anchors, routed overhead cable and minecart gripping rod | **Implemented / awaiting owner acceptance** |

M04 has 95 passing server game tests, a passing real-client test and CI Build #16 according to its [report](MILESTONE_04_REPORT.md). Cable/grip **does not yet move carts**. Do not mark owner acceptance or full mining-loop playtesting complete without confirmation.

## Next steps (suggested, not automatically authorized)

| Order | Small, independent deliverable | Notes |
|---|---|---|
| Next | **Manual winch hauling** | Make the existing cable drum player-operated and able to pull a connected minecart. Resolve continuous moving cable vs winding line and grip/turn geometry before the handoff. |
| Then | **Coal-powered winch attachment** | Coal powers mechanical rotation locally; upgrade the existing hand winch, no electricity or generic power network. |
| Then | **Train coupling** | Reliable join/release of ordinary minecarts; test with curves/stops. |
| Then | **Coal locomotive** | Reversible push/pull and useful speed/fuel for coupled carts. |
| Later | **Incline counterweights and upright mining skips** | Separate lift, reset, and freight-transfer milestones. |
| Later | **Full mining/logistics playtest** | Evaluate only when functional hauling and vertical transport make a representative loop possible. |

Small standalone improvements (such as faster ladders) may be scheduled between mechanical milestones. **No next milestone starts until the owner authorizes it.** Milestone IDs after M04 are assigned when their briefs are approved; historical placeholder numbering is not binding.

## Later workstreams

- **Villages:** terrain suitability, larger buildings, population and destinations, pubs, centralized stores, workshop services, real golem protection.
- **Character and equipment:** armor/loadout redesign, climbable ropes and faster ladders.
- **Geology/metallurgy:** alluvial iron sand/gravel, steel, useful long-lived anvil/smithing.
- **Rules and usability:** selective redstone-system redesign, guidebook, cargo visuals and player-facing text polish.

Full design context lives in [BACKLOG.md](BACKLOG.md), confirmed implementation choices in [DECISIONS.md](DECISIONS.md), and the running visible-text cleanup list in [UX_TEXT_LOG.md](UX_TEXT_LOG.md).

## Milestone commit handoff

This is the **shared handoff between Claude Code, ChatGPT and the project owner**. At the end of **each authorized milestone**, Claude updates this section **in the repository**, not only in its chat. A checked box means that named party performed that check; it is not a substitute for the checks below. Claude can add brief notes and links. After independent review and owner acceptance, keep one compact receipt in the history table and reset this checklist for the next authorized milestone. Do not start another milestone automatically.

**Milestone being handed off:** _Next authorized implementation milestone — to be filled in by Claude._

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
