# M02 Decisions, Boundaries, and Initial Tuning

## Locked design decisions (do not reinterpret as proposals)

- Approved removed rocks: **granite, diorite, andesite, tuff**; swap by stone-vs-deepslate geology.
- Iron-only gate applies to plain deepslate and deepslate ores; not fabricated decorative/cobbled variants.
- Overworld **nighttime** hostile ambient attraction to qualifying **lit areas**, through walls, **20-block range from an illuminated block**. Monsters loiter, do not automatically raid; player pursuit supersedes attraction.
- Zombie door-breaking only in active pursuit, all wooden types, current-version vanilla Hard breaking pace; attempt on iron doors with no damage.
- Brief daylight creeper smoke/ash poof, no explosion.
- 27 total usable player inventory slots = 18 + 9.
- Sorting: registry item order; bottom-to-top, left-to-right; hotbar excluded.
- Hotbar free-slot transfers from slot one onward, left-to-right.
- Shared **64 ingot-equivalent** player carry limit for iron, gold, and copper, enabled and tested **now**.
- Torch emission **12**, lantern **15**. Artificial block light counts, not sun/sky light.
- Ordinary physical activity exhaustion **half**, combat/regeneration unchanged; saturation visible by default.
- Background iron in all normal Overworld biomes; deepslate baseline > stone baseline; deep-ocean deepslate > ordinary deepslate; mountain BIF, arid river/canyon alluvium, geothermal deposits. Diamonds 30–40% of vanilla starting target.
- New-world-only development; preserve Milestone 01 behavior and CI updater.

## Implementation-tunable starting values (not further design decisions)

| Parameter | Initial value / target | Locked? |
|---|---|---|
| Torch block emission | 12 | **Yes** |
| Lantern block emission | 15 | **Yes** |
| Entrancement detection radius | 20 blocks from candidate illuminated block | **Yes** |
| Entrancement AI re-evaluation cadence / preference weight | Implementer chooses performant starting values and documents them | No |
| Daylight creeper fade | Roughly 0.3–0.6 seconds | No (quick visible poof is locked) |
| Zombie door-breaking duration | Use the actual current-version vanilla Hard setting | **Yes** |
| Player slots | 18 + 9 | **Yes** |
| Heavy weight budget | 576 ninths = 64 ingot-equivalents | **Yes** |
| Heavy items | Iron, gold, copper material/forms | **Yes** (individual crafted-form weights audited) |
| Routine activity exhaustion | 50% vanilla | **Yes, first balance pass** |
| Diamond density | Aim 35% vanilla; acceptable first test ~30–40% | **Yes, first balance pass** |
| Background stone-layer iron | Relatively sparse; possible first-pass 60–75% vanilla | No absolute ratio agreed |
| Background deepslate iron | Moderately richer than stone per rock volume | Relative ordering locked; ratio tunable |
| Deep-ocean deepslate iron | Richer than ordinary deepslate | Relative ordering locked; ratio tunable |
| BIF/alluvial/geothermal geometry and rates | Tunable after representative generation tests | Shape/identity locked; values tunable |

## Scope exclusions

Do NOT implement at this milestone:

- Redstone/circuit deletion or redesign; preserve it in the meantime as a functioning vanilla subsystem.
- Freight minecarts or visible cargo; padded passenger carts; locomotives, cable hooks, pulleys, hand winches, mining skips, dumping tracks.
- Smaller stack size for normal materials; new ore types or geological inventories.
- Cave-in physics, mandatory tunnel shoring, RPG skill trees, thirst/temperature/food spoilage.
- Organized raids, mobs smashing arbitrary walls, mob pathfinding that intentionally targets settlement weak spots.
- Existing vanilla-world compatibility or save-migration tools.

## Reporting policy

- If an **implementation detail** can be tuned without changing the above behavior, choose a reasonable starting value, explain it, and test.
- If a **new gameplay rule** or contradiction is discovered, flag it and ask instead of inventing one. Preserve already working phases.
- Present visual changes and measured numerical outcomes separately. “Feels right” cannot be claimed from automated tests; the user will judge gameplay feel in their own instance.
