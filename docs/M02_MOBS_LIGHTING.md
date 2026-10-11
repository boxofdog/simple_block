# M02 Light and Hostile-Mob Behavior

## LIGHT-01: Torch / lantern power

- Ordinary torch emitted **block light level 12** (formerly 14). Torch variants that conceptually behave like ordinary torches should be explicitly reviewed; do not silently alter unrelated light sources. **Standard lantern** emits **15**. Existing other light-source behavior should remain vanilla unless a necessary exception is documented.
- Preserve vanilla-style natural spawn prevention in sufficiently lit regions. For the usual Overworld hostile monsters, any positive block light should prevent ordinary natural spawning; handle per-mob exceptions according to actual game rules, and report any notable exceptions rather than claiming one global threshold covers everything.
- The radius for the new attraction mechanic is **not** the radius of a torch light source; it is the radius around individual *lit blocks*. Daylight/skylight does not cause attraction.

## MOB-01: Entrancement by illuminated areas

**Fantasy:** Hostile mobs remain aimless wilderness creatures. At night, when close enough to genuinely illuminated terrain, they feel inexplicably drawn to it; they wander into it and mill about like loiterers. Walls protect people inside but do not conceal the attractive illumination.

**Rules:**

1. Only active **during Overworld nighttime**. During ordinary daytime, do not enter this AI state; restore normal mob AI. Timeless Nether/End dimensions do not have a day/night entrancement mode for this milestone. This can be adjusted in a future design decision, but don't invent permanent Nether attraction.
2. An eligible hostile mob detects **blocks with artificial block light high enough to prevent ordinary hostile spawning**, up to a **20-block radius measured from the mob to candidate illuminated blocks**. It does **not** seek specific torches or lantern entities, and ordinary skylight does not count.
3. Detection **passes through solid walls**; no line-of-sight requirement for *attraction*. This is intentional supernatural flavor. Physical pathfinding does **not** pass through solid walls, and no block-breaking or structure-sensing goals are introduced merely by attraction.
4. When entranced, preferentially choose feasible wandering destinations in illuminated areas and favor remaining/loitering around them. **Do not converge all mobs on one maximum-light coordinate**, and do not pathfind endlessly into unreachable walls. If interior light is inaccessible, exterior perimeter loitering is an acceptable outcome.
5. Keep wandering recognizably aimless and ambient: roam, spread out, mill about, sometimes lose interest/leave. No settlement raids, synchronized marches, door attacks, or attacks on light fixtures caused by light alone.
6. On **player detection**, normal aggression/pathfinding takes priority. Upon losing the player after the game's normal pursuit/forgetting behavior, the mob can resume wandering/entrancement. Do not allow light attraction to override hostile combat AI.
7. Preserve special movement/behavior for hostile types (ground mobs, flying mobs, water mobs, etc.) wherever practical. The rule is intended for hostile mobs generally in the Overworld; audit exclusions/limitations rather than arbitrarily limiting it to zombies without explanation. Avoid altering bosses or non-hostile creatures under the guise of “all mobs”.

### Technical constraints

- Tune movement-selection preference and reevaluation cadence **without changing the 20-block detection range**. Use limited sampling/caching or event hooks; no full 41x41x41 expensive scans per monster every tick.
- The presence of light behind a wall must produce a *measurable* increase in mob presence near that region relative to dark control areas. It need not imply monsters know the safest route to the light.
- Prioritize natural-looking movement and distribution over artificial perfect pathfinding. Don’t rewrite all existing mob navigation.

## MOB-02: Zombie door behavior

- While **actively pursuing a detected player**, zombies attack any **wooden door type**, on applicable survival difficulty settings, using the duration/speed of vanilla **Hard-mode** zombie door-breaking behavior for the game's actual version. This behavior should not be restricted to Hard difficulty. Peaceful retains its normal hostile-mob absence.
- Zombies should **also attack iron/metal doors** under the same pursuit circumstances, with a visible/recognizable attack attempt, but **must never damage or destroy those doors**. This should not inadvertently damage adjacent blocks.
- Zombies **never attack doors solely because an illuminated area is behind them**. No passive vandalism, no breaching stone walls, no unexplained settlement detection.
- Define the supported ordinary zombie family by auditing the current game's zombie/variant AI classes (at minimum ordinary zombies; include vanilla zombie variants where consistent with shared door-breaking behavior). Document exclusions if the base game treats some variants differently.
- Keep vanilla pathfinding/path memory as the foundation; confirm that closing a door during an active chase still permits a meaningful break attempt.

## MOB-03: Sunlight creeper evaporation

- Creepers exposed to **direct sunlight** in a sunlit Overworld daytime state dissolve in a quick, subtle ash/smoke **poof**. Target animation lifetime about **0.3–0.6 seconds**, as an initial art/implementation value; this is not a new death/loot subsystem.
- No explosions, environmental damage, knockback, lingering cloud, or normal death loot from this evaporation. Unlike a command `/kill`, this is a visually legible daylight removal effect.
- Creepers in caves, under an opaque shelter, or during night remain normally dangerous. Artificial block light or lava alone must not trigger evaporation. Evaluate rainfall/thunderstorms and partial cover using the version's normal notion of direct sunlight; document the precise condition used.
- Server authoritatively removes the creeper; clients see the particle effect. Avoid double particles or an instant teleport/pop before the effect can be seen.
- Other monsters keep their usual daylight behavior unless already changed in Milestone 01.

## Core QA scenarios

**Controlled light experiment:** In a test world at night, set up one broad artificially illuminated courtyard, one equally sized dark control region, and a solid wall isolating the lit interior. Spawn multiple eligible hostiles at equal distance. Observe/time/sample mob positions and quantify extra loitering near the lit perimeter. Repeat with a fully reachable open lit region and during daylight. The behavior must be robust to moving a player out of sight.

**Player priority:** Acquire a player target while mob is entranced: it should switch to pursuit, then plausibly return to loitering/wandering when the target is lost.

**Door chase:** A wooden door must eventually break during active pursuit, including on Normal difficulty; an iron door must receive attempted attacks but remain intact. A wood door behind which no player has been detected must remain intact despite high light behind it.

**Creeper:** Direct sun → short visible effect, no explosion/loot; opaque roof → survives; torchlit cave → survives; night → survives. Capture client screenshots/video or equivalent visual evidence of the effect; numerical entity-despawn tests alone cannot prove the presentation.
