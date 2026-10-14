---
milestone: M12
status: PLAYTESTING
---

# M12 — Monster Effigies: permanent directional fear wards

**Owner-scoped 2026-10-14. Status: PLANNED - READY, not implemented.** This is the complete design handoff for four very expensive, permanent monster-repelling monuments. Adding this milestone to the repository is **not** an implementation or SHIPPED claim; Claude starts coding only on explicit owner authorization. Preserve M01–M11, especially [M09 hostile behavior](MILESTONE_09.md) and [M11's no-floating-text direction](UX_TEXT_LOG.md).

**Owner decision (2026-10-14, with the implementation authorization):** Ender Pearls stack to 16, so the Enderman Block takes **nine full stacks of 16 (144 pearls)**; the Enderman Effigy costs 288 pearls plus the Eye of Ender. Pearl stacking stays vanilla. The compression rule for all four is "every grid slot holds one full stack", consuming a full stack per slot.

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
- Once placed, an effigy can **never be recovered as an item** by breaking, Silk Touch or any normal survival harvesting path. Destroying it yields **no effigy or ingredient refund**. **Pistons may not push/pull it; fluids, broken support, or falling blocks may not dislodge it as a dropped item.** Explosions cannot destroy it. Admin/creative commands can remain deliberate exceptions without permitting survival retrieval. Prevent duplication or displacement/drop bypasses; do not make the item unplaceable or the block unbreakable.
- Before placing or consuming the item, show a **real confirmation screen** (inventory/menu-style), with this exact warning:

  > Are you sure? Monster Effigies cannot be picked back up after being destroyed.

  Buttons: **Place** and **Cancel**. Cancel leaves the item and world untouched. Place commits the exact selected location/orientation and consumes **one item once**, only after fresh server-side checks that the player still holds the item and the space is valid; handle changed blocks, distance, disconnects, double clicks and multiplayer races safely. No accidental double placement and no irreversible consumption on failure. The same rule applies consistently in survival and creative placement where appropriate.
- Item tooltip: **"Scares away [monster] in a 100-block conical radius"**, substituting the matching monster name. The confirmation screen is an intentional high-consequence action, **not a return to M11's action-bar narration**.

## Directional fear behavior

- **A fully three-dimensional, forward-facing cone, not a flat ground footprint or an omnidirectional sphere.** The influence reaches **100 blocks forward, upward and downward within forward-facing directions**, widening toward both sides and vertically (roughly up to 100 blocks to either side at full forward reach). The vertical coverage is **intentional**: caves, towers and terrain above/below are affected where they fall within the forward cone; the region **behind** the effigy is not. Claude must choose and document one deterministic 3D cone geometry, its boundary rules and horizontal facing for all four types, and test the upper/lower, side and forward limits. **No visible magical barrier, light source, damage or permanent status effect.**
- **Natural spawning remains completely unchanged**, including inside the cone. Do not use spawn-cancellation, altered spawn weights, light-level changes or spawn-proof blocks. A matching mob that spawns inside must become frightened **as soon as it is in the world**. The same applies to already-present matching mobs when an effigy is placed, and to mobs spawned by spawners, reinforcements, portals or commands: check fear upon their first normally ticking state, not merely on crossing the boundary.
- Matching mobs crossing into the influence **immediately turn away and actively flee** until they leave it. Add a **small temporary movement-speed increase while fleeing** and **subtle foot-level dust particles like a sprinting player's** when moving; no potion effect, dramatic magic particles or unrelated changes to normal speed. The behavior is *not* merely clearing a target or resuming a random stroll inside. Select **reachable** escape paths outward, respecting real terrain/collision. If trapped, remain afraid, stop attacking and retry pathfinding at a bounded rate; never walk endlessly into a wall, clip through it or invent a new block-breaking escape power.
- **Fear outranks player pursuit and attacks, M09 light attraction/loitering, doorway interest, opportunistic intrusion and other hostile goals** **only when the monster itself is inside the influence.** A frightened mob abandons its attack/target while escaping. **A skeleton outside the cone can still shoot into it**; projectiles, attacks and other behavior originating outside are not blocked or nullified. This is an **AI influence, not a magical wall or player-protection shield**. For Endermen, preserve legitimate movement/teleport safety constraints without making teleportation mandatory.
- **Mounted matching mobs dismount and run away.** A skeleton on a spider leaves the spider behind; a baby zombie riding a chicken dismounts. The unaffected mount retains its own behavior. **Creepers cancel an active pre-explosion fuse/ignition countdown upon becoming frightened**, then flee; an explosion already created is not rewound. Neither a mount nor an active fuse should bypass fear.
- Avoid oscillation at the boundary: maintain a short, bounded retreat/hysteresis state so a mob cannot repeatedly step back into the cone or resume a chase while still exposed. With multiple relevant effigies, seek a viable exit from **all currently affecting cones** rather than ping-ponging between them. **Accumulation at the boundary is acceptable**, but mobs must retain ordinary despawning and mob-cap behavior; they do not become persistent. Fear ends after safe escape; unaffected monster species, other AI and normal spawning retain vanilla/M09 behavior.
- **No extra chunk-loading or persistent world-scale effigy registry.** Fear is evaluated for **mobs that are actually loaded/ticking** using ordinary available world/chunk state. Do not force-load effigy chunks or parts of their influence, scan or simulate unloaded mobs, or maintain offscreen fear. Naturally unloaded areas need no fear processing; once the relevant world and mobs are ticking again, ordinary fear priority applies. Persist the placed effigy's identity and orientation normally for save/reload. Keep 100-block checks bounded and efficient, without expensive global entity/block scans.

## Implementation ownership and verification gate

Claude supplies four items/blocks and four compressed blocks, the real count-consuming compression transaction, shaped effigy recipes, GUI confirmation, facing/persistence, models/rendering/placeholder art, localized fear AI, dedicated-server networking and tests. Joe owns final painting; add exact art paths to the existing painting queue **after assets exist**. Do not create a second report or an unrelated milestone.

Required acceptance coverage:
1. All four **576-item** compression recipes: 64 in every slot succeeds, 63/empty/wrong-material fails; one craft uses exactly 576, and repeat/shift-click/multiplayer crafting cannot consume nine while awarding a block.
2. Each **head/eye + two blocks** shaped recipe, correct output, incorrect top ingredient or arrangement refused; tooltip and all item identities.
3. Four effigy orientations, including non-cardinal yaw; preview/cancel/confirm, item held changed, blocked target, disconnect, rapid repeated clicking, multiplayer synchronization, no loss/duping and saved orientation.
4. Obsidian-level resistance, explosion immunity, no gravity/support requirement, piston/fluid/structure interaction safety, and **no dropped effigy** with or without Silk Touch or after demolition.
5. **Three-dimensional** cone boundaries, including 100-block forward, upward/downward and lateral cases, near/far edges and **behind** the monument; mobs entering, spawning *inside* (including spawner/portal/reinforcements), or already present at placement must turn and **actually leave**. **Spawn rates remain unchanged**. Unloaded areas are not ticked or force-loaded.
6. Fear beats player pursuit and M09 light/door behavior while **the mob itself** is inside; attacks/projectiles from **outside** still function. Test dismounting jockey skeletons and chicken-riding baby zombies; creeper fuse cancellation; trapped-path recovery; modest temporary sprint speed/dust; boundary mob accumulation without disabling despawning; and overlapping-effigy behavior. Nonmatching mobs behave normally.
7. Full regression suite, negative controls that deliberately disable key guards/behaviors and make the respective tests fail, real-client GUI/model/fear screenshots and dedicated-server/client proof. Record actual PASS/FAIL/NOT RUN numbers and CI artifact evidence, never infer them.

**Owner playtest (after delivery):** Build one of each; cancel and confirm placement; try to reclaim one; check vertical/horizontal cone reach, dismounting, a fleeing creeper and the sprinting dust effect. Watch an attacking or newly spawned mob flee while projectiles from monsters outside the cone can still enter. **Keep M12 at PLAYTESTING after implementation until the owner explicitly accepts it.** Append the required concise dated [verification section](RULES.md#handoff-rules) at the bottom, not a standalone report.

## Implementation and verification — 2026-10-14

**Metadata:** M12 | PLAYTESTING | Monster Effigies, full-stack compression, confirmed placement, directional fear cone.

**Implementation**
- Commits: `dc2c5e6ae0bd44ec224a89f6ad99d2044b28d081` code; `43537769aa06c0d2fa63db6734c3e81d8787e198` assets, tests, crafter fix; `3c7da6715936c3cf8d1046ffb06adb154dbcca9f` client test; `7ab8d6e7bd4e8481ee1cfbf2783bd75ba8f06e39` piston test
- Delivered: four compressed blocks (nine full stacks: 576, pearls 144), whole stacks taken by grid, shift-click and crafter; middle-column effigy recipes; tooltips; Place/Cancel screen with server rechecks; any-angle facing, saved and synced; obsidian hardness, blast-proof, no drops, immovable; cone: apex mid-monument, level axis, 45° half-angle, 100 long; priority fear goal, no target, dismount, fuse out, vanilla sprint, 4-block margin then 5 s retreat.
- Deviations: crafter's count-blind recipe cache rechecked (closed a 575-item craft). Loaded effigies only; a cone whose effigy chunk is unloaded is inactive.

**CI**
- Build: #57 | Run ID: 38637606159 | PASS
- Source SHA: `7ab8d6e7bd4e8481ee1cfbf2783bd75ba8f06e39`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38637606159
- Artifact: `simple-block-mod` id 11929181475, available. Builds #54–#55 failed on intermediate commits (test fixes since).

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 227/227 | local; 12 M12 tests (EFF-01–10); [log](evidence/milestone-12/m12-verification-2026-10-14.txt) |
| Client | PASS | 17/17 | full suite; screen, placement, figures, fleeing; [screenshots](evidence/milestone-12/screenshots/) |
| Dedicated server | PASS | 1/1 | confirm, place, angle sync, fear over a dedicated server |
| Negative controls | PASS | 10/11 | 8 alone, 2 with second guard off; pistons: vanilla also refuses; [log](evidence/milestone-12/m12-verification-2026-10-14.txt) |

**Known gaps**
- GAP-01: MINOR — eight placeholder textures and figures; next: Joe paints (To-Do).
- GAP-02: MINOR — wooden-door interest not tested separately; covered by goal priority check.
- GAP-03: MINOR — spawner/portal spawns tested as spawner-reason and command spawns only.
- GAP-04: MINOR — "no room" refusal is silent (no-text direction); next: owner feedback choice.
- GAP-05: MINOR — retreat 5 s and margin 4 provisional; next: playtest.

**Owner playtest**
- Craft one effigy; try a stack of 63.
- Cancel, then place at an angle.
- Break it with Silk Touch.
- Watch mobs flee; a skeleton outside still shoots.
- Check jockeys, a lit creeper, sprint dust.
