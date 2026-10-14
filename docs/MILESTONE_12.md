---
milestone: M12
status: PLANNED - READY
---

# M12 — Monster Effigies: permanent directional fear wards

**Owner-scoped 2026-10-14. Status: PLANNED - READY, not implemented.** This is the complete design handoff for four very expensive, permanent monster-repelling monuments. Adding this milestone to the repository is **not** an implementation or SHIPPED claim; Claude starts coding only on explicit owner authorization. Preserve M01–M11, especially [M09 hostile behavior](MILESTONE_09.md) and [M11's no-floating-text direction](UX_TEXT_LOG.md).

## Purpose and boundaries

Turn large quantities of **existing vanilla mob drops** into permanent, placeable territorial defenses. The effigies do **not** stop monsters from spawning, illuminate the world, harm monsters, or add new ores, progression currencies or factory systems. A matching monster can appear within the protected territory but must panic and leave. Each effigy is specific to its corresponding vanilla monster.

| Effigy | Compressed ingredient (576 items) | Required top ingredient | Repels |
|---|---|---|---|
| Zombie Effigy | 9 full stacks of **Rotten Flesh** → Zombie Block | Zombie Head | Zombies, including baby zombies |
| Skeleton Effigy | 9 full stacks of **Bones** → Skeleton Block | Skeleton Skull | Skeletons |
| Creeper Effigy | 9 full stacks of **Gunpowder** → Creeper Block | Creeper Head | Creepers |
| Enderman Effigy | 9 full stacks of **Ender Pearls** → Enderman Block | Eye of Ender | Endermen |

**Do not silently expand these target sets** to husks, drowned, stray, wither skeletons, cave spiders, piglins or other variants. The only explicitly included variant is the baby zombie. Other mobs are completely unaffected. Preserve ordinary creature-head and Eye of Ender acquisition, recipes and other uses.

## Recipes and item conservation

**Compression:** Put **nine separate full 64-item stacks** of the matching drop into all nine squares of a 3×3 crafting grid to make **one** matching monster block. This costs **576 actual items**, not nine individual items. A grid containing even one stack of 63 must not craft it. Vanilla crafting consumes one per occupied slot by default; implement a real stack-count-aware recipe/menu transaction rather than declaring an ordinary nine-ingredient recipe that only consumes nine items. Ensure preview, ordinary click, shift-click/repeated craft, inventory updates, recipe-book interaction and multiplayer all agree: no unintended duplication or free output. The four compressed block items should be distinct and recognizable; ordinary block placing/breaking need not share an effigy's irreversible behavior. Do not invent a decompression recipe or refund exploit.

**Effigy:** An ordinary shaped 3×3 recipe, **center column only**, yields one effigy:

```text
[empty] [mob head / Eye of Ender] [empty]
[empty] [matching monster block] [empty]
[empty] [matching monster block] [empty]
```

The top ingredient is the matching vanilla Zombie Head, Skeleton Skull or Creeper Head; use an **Eye of Ender** for the Enderman Effigy. All other slots must be empty. **Cost: 1,152 corresponding mob drops plus the specified head/eye**. The item must craft and carry its tooltip normally.

## Placement, model and permanence

- Each type is a visibly distinct **impaled monster monument**: head and torso/body mounted on a stake or spear, intentionally grim and recognizable at a distance. Provide editable Blockbench models, linked UVs and temporary textures; final painting belongs to Joe's [art queue](Joe's%20To-Do/BLOCKBENCH_PAINTING.md). Do not claim flat placeholders are final art.
- Place the effigy upright and **facing away from the placing player**, using their direction at placement. Its visible facing and invisible protection cone must be aligned. Preserve **arbitrary horizontal yaw/angles** rather than snapping the direction to only four block facings. Persistence and client/server orientation must agree.
- **Obsidian-equivalent mining hardness** and **immunity to explosion destruction**. Effigies do not obey gravity and **remain standing without a support block**; breaking the ground underneath does not break them.
- Once placed, an effigy can **never be recovered as an item** by breaking, Silk Touch or any normal survival harvesting path. Destroying it yields **no effigy or ingredient refund**. Prevent trivial item-duplication or displacement/drop bypasses; do not make the item unplaceable or the block unbreakable.
- Before placing or consuming the item, show a **real confirmation screen** (inventory/menu-style), with this exact warning:

  > Are you sure? Monster Effigies cannot be picked back up after being destroyed.

  Buttons: **Place** and **Cancel**. Cancel leaves the item and world untouched. Place commits the exact selected location/orientation and consumes **one item once**, only after fresh server-side checks that the player still holds the item and the space is valid; handle changed blocks, distance, disconnects, double clicks and multiplayer races safely. No accidental double placement and no irreversible consumption on failure. The same rule applies consistently in survival and creative placement where appropriate.
- Item tooltip: **"Scares away [monster] in a 100-block conical radius"**, substituting the matching monster name. The confirmation screen is an intentional high-consequence action, **not a return to M11's action-bar narration**.

## Directional fear behavior

- **Directional conical influence, not a 100-block sphere.** The protected footprint projects **100 blocks forward from the effigy's facing**, widening to its left and right, with the owner's intended broad reach of roughly **100 blocks to either side at the far end**. Use one explicit, deterministic geometric definition for all four types and document how boundary points, heights and orientation are handled; use that definition for tests and any debug-only visualization. **No visible magical barrier, light source, damage or permanent status effect.**
- **Natural spawning remains completely unchanged**, including inside the cone. Do not use spawn-cancellation, altered spawn weights, light-level changes or spawn-proof blocks. A matching mob that spawns inside must become frightened **as soon as it is in the world**.
- Matching mobs crossing into the influence **immediately turn away and actively flee** until they leave it. The behavior is *not* merely clearing a target or resuming a random stroll inside. Select reachable escape paths that move outward from the dangerous region, obey real terrain/collision and fail safely if temporarily trapped.
- **Fear outranks player pursuit and attacks, M09 light attraction/loitering, doorway interest, opportunistic intrusion and other hostile goals.** A frightened mob abandons its current attack/target while escaping. For Endermen, preserve their legitimate movement/teleport safety constraints without treating teleportation as mandatory.
- Avoid oscillation at the boundary: maintain a short, bounded retreat/hysteresis state so a mob cannot repeatedly step back into the cone or resume a chase while still exposed. With multiple relevant effigies, seek a viable exit from **all currently affecting cones** rather than ping-ponging between them. Fear ends after safe escape; unaffected monster species, other AI and normal spawning retain vanilla/M09 behavior.
- Implement efficiently for a potential **100-block reach**, without expensive all-mob/all-block global scans each tick, forcing chunks to load, simulating unloaded mobs, or breaking dedicated-server performance. Stored effigies must work after save/reload and while relevant areas naturally load and unload.

## Implementation ownership and verification gate

Claude supplies four items/blocks and four compressed blocks, the real count-consuming compression transaction, shaped effigy recipes, GUI confirmation, facing/persistence, models/rendering/placeholder art, localized fear AI, dedicated-server networking and tests. Joe owns final painting; add exact art paths to the existing painting queue **after assets exist**. Do not create a second report or an unrelated milestone.

Required acceptance coverage:
1. All four **576-item** compression recipes: 64 in every slot succeeds, 63/empty/wrong-material fails; one craft uses exactly 576, and repeat/shift-click/multiplayer crafting cannot consume nine while awarding a block.
2. Each **head/eye + two blocks** shaped recipe, correct output, incorrect top ingredient or arrangement refused; tooltip and all item identities.
3. Four effigy orientations, including non-cardinal yaw; preview/cancel/confirm, item held changed, blocked target, disconnect, rapid repeated clicking, multiplayer synchronization, no loss/duping and saved orientation.
4. Obsidian-level resistance, explosion immunity, no gravity/support requirement, and **no dropped effigy** with or without Silk Touch or after demolition.
5. Cone boundaries and orientation at near/far sides and different heights; matching mobs enter, turn and **actually leave**, or spawn *inside* and flee immediately. **Spawn rates remain unchanged**.
6. Fear beats player pursuit and M09 light/door behavior, breaks attacks, avoids boundary jitter and overlapping-effigy loops; nonmatching mobs behave normally.
7. Full regression suite, negative controls that deliberately disable key guards/behaviors and make the respective tests fail, real-client GUI/model/fear screenshots and dedicated-server/client proof. Record actual PASS/FAIL/NOT RUN numbers and CI artifact evidence, never infer them.

**Owner playtest (after delivery):** Build one of each, cancel then confirm placement, try to reclaim one, orient an effigy toward a monster approach and watch an attacking and a freshly spawned matching mob flee while other mobs continue to behave normally. **Keep M12 at PLAYTESTING after implementation until the owner explicitly accepts it.** Append the required concise dated [verification section](RULES.md#handoff-rules) at the bottom, not a standalone report.
