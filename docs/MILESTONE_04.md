# Simple Block — Milestone 04: Cable Infrastructure

**Source:** the owner's handoff in the Claude Code session of 2026-10-12, recorded here verbatim (headings added) so later sessions have it. Historical as-built implementation and tests are in `docs/MILESTONE_04_REPORT.md`. For future authorized patches, append a short dated implementation/verification note at the end of THIS milestone document; do not create a new report.

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
