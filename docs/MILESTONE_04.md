# Simple Block — Milestone 04: Cable Infrastructure

**Source:** the owner's handoff in the Claude Code session of 2026-10-12, recorded here verbatim (headings added) so later sessions have it. Historical as-built implementation and tests are in `docs/archive/milestone-reports/MILESTONE_04_REPORT.md` (summary: the verification section at the end of this file). For future authorized patches, append a short dated implementation/verification note at the end of THIS milestone document; do not create a new report.

Implement the foundation for mechanical cable haulage using existing vanilla minecarts. Keep this milestone narrowly scoped.

## Components

- **Copper connecting rod:** Attaches to an existing minecart. When the minecart passes beneath an installed cable, the rod automatically engages it through a grip/roller assembly. The connection must be designed to pass cable supports without snagging.
- **Copper cable anchor:** Attaches exclusively to the underside of wooden blocks, including all wooden block variants where physically appropriate. Anchors guide and support cables using rollers; they do not permanently fasten the moving cable in place.
- **Cable:** A separate craftable item, which can be combined into coils. Cable length is a finite resource consumed when constructing a route.
- **Cable drum:** A placeable block that becomes a winch when a cable coil is applied.

## Cable drum recipe

```
[ Empty ] [ Empty ] [ Empty ]
[ Stick ] [ Iron  ] [ Stick ]
[ Plank ] [ Iron  ] [ Plank ]
```

## Cable placement

- Applying a cable coil to a drum creates a visible loose cable end with a hook.
- Right-click the hook to pick it up and drag it behind you, like a lead.
- Right-click an installed copper anchor to route the cable through it.
- Continue dragging the cable to subsequent anchors, creating a connected route.
- Cable is consumed according to the length installed, not simply the number of anchors.
- Allow players to extend, disconnect, and dismantle cable routes without rebuilding everything.
- Routes must support horizontal runs, turns, and changes in elevation.

## Assets

Do not create finished textures. Create editable 3D models and UV layouts that I can open in Blockbench (`.bbmodel` files). I will paint the textures myself.

Include working in-game geometry and rendering so I can test the components before applying final artwork. Preserve the source models in the repository.

## Scope

M04 establishes cable placement, routing, anchors, the winch structure, and minecart cable engagement.

Do not implement coal engines, counterweights, inclined-lift automation, skips, or locomotives in this milestone.

Build and test each component in an actual Minecraft client. Update documentation and report any unresolved design decisions rather than silently inventing new mechanics.

## Implementation and verification — 2026-10-12

**Metadata:** M04 | SHIPPED | Cable, coils, wood-supported anchors, cable drum, routed spans and the minecart connecting rod that grips the cable.

**Implementation**
- Commits: `02b93e7d63fbc702635dd8d781459bb6015175d2` content and routing; `f7cc3d5f47dbd6b8a626a017b2831d12c3f67291` models and rendering; `215a24af0b9d4eea068fb2b155ad1bfc5f935393` tests and docs
- Delivered: six recipes; anchors only under wooden supports; drum with coils and a loose end; routing, extending, disconnecting and dismantling; obstacle and 12-block span checks; rod grips the cable past anchors.
- Deviations: Cable-length consumption was later superseded by M06 (one coil, any supported length).

**CI**
- Build: #16 | Run ID: 38197364425 | PASS
- Source SHA: `215a24af0b9d4eea068fb2b155ad1bfc5f935393`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38197364425
- Artifact: `simple-block-mod` id 11723758660, available (verified 2026-10-13).

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 95/95 | [archived report](archive/milestone-reports/MILESTONE_04_REPORT.md); [evidence](evidence/milestone-04/gametest-evidence.md) |
| Client | PASS | 2/2 | `SurvivalSmokeClientTest`, `CableClientTest`; [screenshots](evidence/milestone-04/screenshots/) |
| Dedicated server | PASS | 1/1 | `runServer` start only; no remote client |
| Negative controls | PASS | 2/2 | control A (4 rules) and B (obstacle); [negative controls](evidence/milestone-04/negative-controls.md) |

**Known gaps**
- NONE

**Owner playtest**
- NONE
