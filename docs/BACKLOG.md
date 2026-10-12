# Simple Block — Future Design Backlog

Ideas here are **not approved implementation tasks**. The short [roadmap](ROADMAP.md) owns order and progress. Confirmed design choices are in [DECISIONS.md](DECISIONS.md); implementation reports are in the milestone files.

## Additional ideas

| ID | Idea / question | Origin | Status | Next action |
|---|---|---|---|---|
| I-001 | Developed smithy: forge tool components at an anvil, assemble at crafting table; avoid making first iron tools depend on a 31-ingot anvil | Brainstorm | DESIGN | Separate future design conversation |
| I-002 | Anvils should be effectively permanent under ordinary use, with a cost appropriate to usefulness | Brainstorm | DESIGN | Decide when metalworking is scoped |
| I-003 | Broader inventory/world-content cleanup beyond the four confirmed rock variants | Brainstorm | PARKED | Specify exact block list before approving |
| I-004 | **Modular workwear redesign:** retire gold/chainmail; copper Headlamp provides slightly less head protection than iron; chest-and-legs Overalls restore the third inventory row (+9 slots) with no more than combined leather chest/leggings protection; Spiked Boots speed rope/ladder climbing; Mining Boots permit overweight heavy pickups with a progressive speed penalty. Original chestplate-role idea superseded. | Owner-approved future direction | PARTIALLY SCOPED — not implemented | Define overalls equipping across slots, headlamp lighting, recipes/models/protection, overload curve, piglin behavior and safe occupied-backpack unequip |
| I-005 | **Rope + Spelunker's Rope:** three vertical strings craft rope; rope under an existing copper cable anchor crafts a mountable Spelunker's Rope. Attach beneath or beside a block, hang vertically in open air, hold right-click with rope to extend by 12-block lengths, and coil extra length on ground. Climbable temporary cave access, not a moving haulage cable. | Owner-approved future feature | SCOPED — not implemented | Detail climb speed, exact consumption/yield, retrieval, clipping/support failure and coil behavior only when preparing its later milestone |
| I-006 | **Faster ladders**; ordinary vertical movement should not be so tedious | Owner-approved future improvement | DESIGN | Choose climb speed and verify interactions with ropes, shafts, and skips |
| I-007 | **Alluvial iron sand and iron gravel**, naturally occurring at appropriate riverbanks; require new original textures. This is a proposed additional deposit *form* beyond M02's existing alluvial enrichment, not an amendment to M02. | BTA-inspired prospecting discussion; owner-approved future direction | DESIGN | Design finite deposits and extraction/processing; reconcile surface iron availability with iron demand and huge mountain veins |
| I-008 | **Steel** as a crafted/manufactured material with a distinctive purpose, using a production system of our own design rather than BTA's | BTA inspiration; owner-approved future direction | DESIGN | Decide properties, production chain, and smithy/anvil integration |
| I-009 | **Recipe guide / guidebook** for learning about tools, materials, and machines as the player discovers them | BTA inspiration; owner-approved future feature | DESIGN | Decide discovery presentation, recipe navigation, and whether any unlock restrictions exist (none assumed) |

### I-004 — Modular armor roles and integrated overalls backpack

- **Head:** a copper **Headlamp**, protection just below iron head armor. Lighting behavior has not been decided.
- **Chest + legs:** **Overalls** with an integrated backpack restore nine slots of M02's suppressed third row (**27 → 36 total**). Protection is capped at the combined equivalent of vanilla leather chest and legs, so this is practical workwear rather than serious combat armor. The exact item/equipment-slot implementation remains to be decided.
- **Feet (choose one):** **Spiked Boots** improve rope/ladder climbing; **Mining Boots** permit lifting heavy cargo past the normal 64-unit limit while slowing movement progressively with excess weight. The boots do not independently provide a backpack.

Both boots work with the same overalls, so users can change their specialization with **one equipment swap** rather than replacing an entire armor outfit. Gold and chainmail sets disappear from ordinary survival/creative play, with safe technical treatment of old registry identities. Decide recipes, protections, headlamp lighting, exact speed curves, and safe overalls unequip with occupied slots before implementation; M05/M06 remain untouched.

### I-005 — Spelunker's Rope recipe and placement direction

```text
Rope:                      Spelunker's Rope:
[empty][string][empty]     [empty][empty][empty]
[empty][string][empty]     [empty][copper anchor][empty]
[empty][string][empty]     [empty][rope][empty]
```

The anchor is the existing copper cable anchor item used as a crafting ingredient; the finished Spelunker's Rope may attach to a block's underside or side. Its hanging rope drops vertically with no intermediate blocks. Holding use with additional rope extends the line in 12-block increments; on reaching ground, excess rope coils visibly on the ground block. Avoid conflating this with the moving overhead haulage cable. Exact recipe output count, extension costs, climb/retrieval controls and block-obstruction behavior have not been chosen; they may be resolved in a separate implementation brief. No authority to implement during M05.

## Future design backlog — inhabited villages

**Status: OWNER-APPROVED DIRECTION / NOT IMPLEMENTATION-AUTHORIZED.** These are desired outcomes, not permission to modify village generation or AI before a dedicated milestone.

### Settlement geography and architecture

- **Restrict bad sites:** Strongly discourage or reject village generation when the planned village footprint contains extreme elevation variation, cliffs, or terrain that would create disconnected buildings, sheer paths, or dirt pedestals. Favor contiguous, reasonably level land; mountain plateaus and manageable slopes remain possible.
- **Larger, convincing structures:** Houses should feel like homes, with enough room to enter and inhabit. The same applies to professional workshops, general stores, and communal buildings; avoid the tiny playset-like interiors of many vanilla structures.
- **Real layouts:** Construct connected, traversable streets, coherent building placement, an intelligible village center, and accessible public spaces. Building interiors and doors must support villager and player navigation.
- **Population density:** Villages should feel actually populated. Resident counts ought to relate sensibly to available homes, workplaces, and village scale, rather than leave large settlements nearly deserted.

### Daily life and communal destinations

- **Purposeful schedules:** Villagers should prioritize identifiable destinations beyond their homes during the day: workplaces, shops, public areas, and social spaces. They should visibly inhabit and use the buildings that define their village.
- **Pub / tavern:** Include a suitably large communal pub where **most villagers gather and mingle during the evening/night**. It must support multiple villagers inside at once instead of operating as a decorative empty building. Later return-home/sleep schedules need design, not assumption.
- **Profession presence:** An existing smithy should reliably have a smith at work; other workplaces likewise should have their associated professionals present and visibly working when appropriate.

### Commerce, skilled work, and services

- **General store:** Ordinary buying and selling occurs at a centralized general store, with a shopkeeper stationed **behind a counter** during trading hours.
- **Workplace services:** Professionals offer specialized **services at their workplaces**, rather than every wandering worker simply carrying a personal trade menu. Smithing, repairing, mapmaking, and similar functions are examples to design, not currently finalized mechanics.
- **Avoid economic shortcuts:** Village services must not make the mod's mining, crafting, or freight economy obsolete. Shared store inventory versus consolidated trades, stock limits, replenishment, and pricing are still open decisions.

### Real protection and sensible nighttime survival

- **Iron golems must actually defend villagers:** Golems should notice and urgently respond when a monster attacks a villager, with reliable target prioritization and meaningful patrols near populated or vulnerable parts of the village.
- **Monsters should threaten villagers appropriately:** Existing hostile behaviors and new light-entrancement rules must interact coherently with village safety, rather than golems ignoring attacks or villages being routinely depopulated.
- **Safe gathering:** A pub that draws villagers together at night must have plausible protection. Avoid designing social routines that simply turn villagers into helpless victims.

### Candidate village milestones — explicitly not scheduled

1. Village footprint terrain-suitability checks.
2. Larger, enterable buildings and coherent navigable village layout.
3. Population scaling and workplace occupancy.
4. Social-destination scheduling and pub.
5. Centralized general-store trading.
6. Profession-specific services.
7. Responsive golem defense and nighttime safety.

**Out of scope until discussed:** Complex autonomous economies, village simulation/management, town growth systems, and automatic industrial production.

## Explicitly rejected BTA-inspired directions

- **Seasons:** Not planned for Simple Block.
- **Painting / cosmetic coloring system:** Not planned for Simple Block.

These rejections do not prohibit ordinary visual polish or other environmental mechanics, but they should not re-enter the roadmap as features without a new owner decision.

## Areas needing design before implementation

- Train coupling, reversible coal locomotive, fuel efficiency and gradients.
- Hand winch operation and mechanical cable traction; turning/guide pulleys, grip clearance, alternate track alignments.
- Coal-fired **mechanical** winch attachment and counterweight incline reset; no electrical power grid or Create-style power network.
- Upright vertical mining skip, loading by gravity from elevated minecart tracks, and unloading at the surface.
- Cargo visibility is **design-scoped**: keep the original freight minecart and render a cauldron/composter-like stepped fill surface proportional to cargo count with new owner-paintable texture assets; defer implementation until after M05. Exact quantity remains inspectable and floating dev text is addressed in `UX_TEXT_LOG.md`.
- Metalworking, durable anvils, differentiated armor roles and steel.
- Redstone dependency audit: preserve worthwhile standalone mechanisms rather than automatically deleting associated devices.

Avoid introducing gangue, mandatory waste processing, seasons, painting systems or survival-maintenance chores as planned work. Reconsider only on a new explicit owner decision.
