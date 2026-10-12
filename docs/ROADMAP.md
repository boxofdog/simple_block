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

## Claude Code commit verification

- [ ] **Verify documentation cleanup on `main`** (commit `fe9a73a5fe069c10f68819320aa558051ed16c1e`): confirm the duplicate `docs/docs/` files are gone, `ROADMAP.md`, `BACKLOG.md`, and `UX_TEXT_LOG.md` are present and linked correctly, and existing milestone specifications/reports remain intact. Check this box only after inspecting the repository and record the verified commit SHA here: `________`. This is a verification task, **not** permission to start the next milestone.

## Working rules

- **Addition by subtraction:** prefer making existing Minecraft objects useful over adding redundant variants.
- **Useful infrastructure, not chores:** machinery should permanently improve what players can do.
- **One small milestone at a time:** focused scope, build/actual-client verification where appropriate, closeout report, then stop.
- **Editable assets:** preserve source Blockbench models and owner-authored textures.
- **Single source of truth:** milestone specs and reports hold implementation detail; this roadmap only tracks priorities and progress. Never silently promote backlog suggestions to requirements.
