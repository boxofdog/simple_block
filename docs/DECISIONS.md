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
