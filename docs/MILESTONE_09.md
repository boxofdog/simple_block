---
milestone: M09
status: PLAYTESTING
---

# M09 — Hostile attraction, opportunistic intrusion and rare spider jockeys

**Owner-approved design: 2026-10-13. Current status: PLAYTESTING (2026-10-14).** Implementation was owner-authorized and delivered; the verification appendix below documents code, CI, evidence and known gaps. **Not owner-accepted or SHIPPED.** The original behavioral constraints remain in force: no siege waves, new species, village-AI work or deferred iron gate.

## Core intent and existing M02 baseline

Hostile monsters should be **drawn to areas that look inhabited**: illuminated ground implies a greater chance of finding humans to attack. They are **not** intelligent base detectors or tactical siege engineers. The feeling should be a gradual, natural trickle and persistent loitering, not mass aggression.

Reuse M02's existing `Entrancement` behavior: it detects **illuminated areas/ground**, not individual torches or the brightest source; its current range is 20 blocks, active in the Overworld at night and in unskylit underground areas. Vanilla player acquisition/pursuit overrides light attraction. **Preserve these existing boundaries unless the owner separately changes them.** M09 improves persistence and path opportunities, not the basic attraction target.

## Settled M09 behavioral rules

### Light interest, perimeter loitering and entrances

- **Wall outside a lit area:** attracted monsters that cannot reach an interior illuminated area **linger/wander along the exterior wall**, rather than abruptly forgetting the location or inexplicably knowing an entrance on the far side of a building.
- **Player first:** detecting/pursuing an attackable player takes precedence over ambient light interest, loitering and door investigation. Do not interfere with normal combat goals.
- **Open doors matter most:** where an entrance provides a usable route toward an illuminated area, monsters are **particularly interested in doors that have remained open**. Exploit actual reachable openings; do not read a base layout through walls or teleport to an entrance.
- **Wooden door obstructing access — settled eligibility:** **ground-navigating hostile mobs participating in M02's illuminated-area attraction** may attempt to **break a closed wooden door** when they physically encounter/recognize it blocking their route toward the illuminated interior, **without needing to chase a player first**. This is **not limited to the existing zombie family** (zombies, husks, zombie villagers); implement appropriate approach/attack behavior for other eligible ground hostiles without changing their player-combat priority. No global permission for unrelated water/flying mobs, door detection through an entire opaque structure, or destruction of non-wood doors by ordinary loitering. A mob investigates/attacks a door as a result of reaching that obstacle, not omniscient knowledge of a settlement's entrance.
- **Lights extinguished:** an area that is no longer illuminated is **no longer an attraction target**. Invalidate stale remembered lit-area destinations; do not continue a light-driven siege of a permanently dark building.
- **Path viability:** if an opening, door break, creeper explosion or other change makes the route toward a remembered illuminated area shorter or more accessible, monsters **advance as far into the newly reachable space as they can and loiter there**. They don't need to reach the interior in a single breach. **Claude may choose reasonable path recalculation algorithms, rate limits and performance-safe tuning.** No owner decision on internal pathfinding cadence is needed.

The original M02 system already samples and remembers lit ground and sometimes ignores it for a wander decision. M09 should make interest *more persistent* without removing natural variability. Precise loiter-duration, open-door persistence thresholds and other numerical parameters may be proposed/tuned during design and owner playtest; do not change the rules above.

### Creeper retaliation: arrows from an unreachable attacker

- **Trigger — player-fired arrow only:** a creeper is hit by an **arrow attributed to a player** whom it **cannot readily reach by walking/pathing**. An arrow from another mob, a dispenser or any non-player source **does not trigger retaliation**. This is a special retaliation against the barrier separating the creeper from its attacker: in the owner's words, "fuck your walls, fuck your doors." This is not restricted to the deferred iron gate.
- **Explicit negative controls:** **sword/melee damage** does not activate the new behavior. **Arrow damage when the shooter is ordinarily reachable and the creeper can approach to explode** does not activate it; retain normal creeper chase/explosion behavior in both cases.
- **First target — doors:** an offended creeper looks for the **nearest valid door nearby**, including **wooden, iron, and copper doors and trapdoors**. It approaches the chosen door and detonates against that point of access/fortification. The door-selection search radius, validity checks (including state, obstruction and reachability), and tie-break behavior are not yet numerically defined; do not silently define the door as only wood, or require the deferred custom iron gate.
- **Fallback target — beneath the shooter:** if **no valid door is nearby**, the creeper approaches **the nearest point it can reach to the attacker's horizontal (X,Z) coordinates**, **ignoring vertical (Y) separation when choosing the destination**. It then detonates **even if the attacker is far above or below**: the point is to blow up whatever the attacker is standing on or behind, rather than abandoning the pursuit because the player is out of explosion range in three dimensions. This does not teleport the creeper or exempt it from normal world collision.
- **After the explosion:** the creeper has opened or weakened the barrier. Other hostile mobs already interested in the **illuminated area**, not the light source, should **reevaluate which positions are now reachable**, press forward to the nearest positions they can physically reach, and **fill/loiter in that newly accessible space**. This is emergent pathfinding, not a scripted coordinated siege or a guaranteed complete breach; if nothing becomes reachable, they keep ordinary perimeter behavior. Player pursuit continues to override this ambient interest.
- **Special-case, not global rule:** do not turn all creepers into random wall demolishers, give monsters abstract settlement knowledge, or alter ordinary creeper behavior without an arrow **from a player** the creeper cannot readily reach. **Claude implementation discretion:** choose bounded, performance-safe door-search radius, approachability/validity tests, shooter attribution, reachability threshold, fuse/abort and timeout behavior. Keep vanilla `mobGriefing` and ordinary explosion/block-damage rules unless the owner authorizes a separate change; do not invent a new gate, block-destruction subsystem or protected-block exceptions. Propose tunable defaults and verify the required observable behaviors.
- Keep existing M02 creeper sunlight evaporation and ordinary creeper behavior where this narrow trigger is not satisfied.

### Occasional skeleton–spider jockeys

**Owner-approved approach: opportunistic recruitment, not tactical recruitment.** Skeletons can occasionally decide a nearby spider would be a useful mount while wandering; they do not deliberately recruit mounts to overcome a wall or organize a siege.

- **Recruiter and eligible mount:** an **ordinary skeleton** initiates. It may select a **living, nearby, reachable ordinary spider** that is not already mounted or carrying another entity. Exclude cave spiders and other skeleton/spider variants from this new recruitment mechanic unless the owner later approves them.
- **Genuinely rare:** recruitment must be **exceptional**, not a routine response to sharing an area with a spider. Do not flood an illuminated perimeter with mounted skeletons. Propose and playtest an explicitly low recruitment probability with sensible opportunity/cooldown limits; the owner has **not** chosen an exact percentage. No artificial jockey population quotas or guaranteed formation. Preserve any existing vanilla-spawn behavior unless the owner separately asks to change it.
- **Initiation and interruption:** recruitment happens opportunistically during idle/wandering. A skeleton **actively detecting/pursuing a player abandons recruitment** in favor of combat. When recruitment succeeds, it **physically walks to the spider and mounts**; no instantaneous remote mounting/teleportation. If the target dies, is mounted, becomes unreachable or a player is spotted en route, cancel appropriately.
- **While mounted:** the **spider moves and climbs walls normally**; the skeleton rides it and **retains ranged attacks**. A jockey that catches the player's attention or notices/chases the player is not turned into a special tactical siege unit.
- **Overhang maneuver — specific visual and physical rule:** when a jockey is **climbing up a wall normally** and the **riding skeleton begins to suffocate against an overhead overhang/ledge**, the **skeleton raises an arm as if grabbing the lip of the ledge**. At the same time, it helps the **spider drift outward away from the wall and upward**, moving the mounted pair around the obstructing edge **until they clear the overhang**, after which ordinary spider climbing/pursuit resumes. The arm motion and spider movement must visibly correspond: it should look like the skeleton has hauled the spider around the ledge, **not** a random vertical boost or teleport. The maneuver is **reactive to actual skeleton headroom obstruction**, not a general-purpose spider-climbing upgrade or preplanned overhang search.
- **Boundaries and unfinished details:** the maneuver must respect solid-block collision, genuine free space and server-authoritative movement; do not phase the mount through stone. Exact activation hysteresis, motion speed/distance, animation implementation, fail/abort behavior when a lip cannot be cleared, and natural dismount/termination conditions need implementation proposals and acceptance tests. Those details may be tuned; the behavior above is settled.

**Negative controls:** spiders without riders keep ordinary climbing; a jockey on an unobstructed wall does not perform the arm/sideways maneuver; an active combat target interrupts recruitment; no distant or ineligible spider is recruited; a genuinely impassable overhang is not defeated by clipping.

## Explicitly out of M09

- **Large winch-operated iron gate:** owner deferred it. Its former idea (two three-block-wide gate sections) is parked in [BACKLOG.md](BACKLOG.md), **not** part of this milestone. Do not confuse this with existing vanilla wooden/iron doors or rewrite those items by assumption.
- No raid/wave subsystem, omniscient breach planning, new monsters, settlement architecture overhaul, villager profession/trade/scheduling work, or golem-AI redesign. Village-specific work remains in [BACKLOG.md](BACKLOG.md).

## Original implementation handoff — settled design constraints

**The owner settled the four areas above before authorizing implementation.** These constraints informed M09's delivery and remain its acceptance criteria. Within the behavior and exclusions documented here:

1. **Door attacks:** use M02-attracted, ground-navigating hostile eligibility as above; give eligible mobs a readable, physical door attack when a wooden door blocks their approach. Choose animations/attack cadence and path selection without treating ambient wandering as player pursuit or giving mobs omniscient base knowledge.
2. **Creeper retaliation:** verify **arrow damage attributed to a player** and an attacker the creeper cannot readily path to; select the nearest nearby valid wood/iron/copper door or trapdoor first, otherwise the closest reachable position under the attacker's **X/Z** regardless of **Y**, and explode there. Set reasonable proximity, reachability, fuse/timeout and collision-safe fallback defaults. Retain ordinary explosion and `mobGriefing` semantics.
3. **Jockeys:** keep ordinary skeleton → ordinary spider opportunistic recruitment **genuinely rare** and interruptible by player combat; tune low chance/cooldown and collision-safe reactive ledge-grab, arm animation, abort and dismount behavior. Do not introduce tactical recruitment or extra variants.
4. **Performance and acceptance:** propose tunables and run focused server GameTests and actual-client checks for loitering, door use, extinction of lit areas, player combat priority, breach-driven redistribution, creeper target order, rare recruitment and the headroom-triggered jockey maneuver. Rate-limit path checks where sensible; measure or bound crowded-mob overhead. Do not turn test tuning into new gameplay scope.

**Implementation authorization was subsequently granted and M09 was delivered for owner playtesting.** Future changes remain subject to the owner's playtest feedback and approval.

**Required negative controls when authorized:** arrow damage from skeletons, dispensers or other non-player sources does not provoke creeper retaliation; player pursuit wins; darkened areas release ambient interest; closed inaccessible interior does not cause omniscient door navigation; valid wooden-door access behaves as approved; ordinary sword hits and reachable arrow hits do not provoke special creeper retaliation; **a provoked creeper prioritizes a nearby valid door/trapdoor over the X/Z fallback; absent a door it approaches the closest reachable horizontal position even when the shooter is vertically distant; post-explosion mobs occupy only genuinely reachable space**; **recruitment is exceptional, combat interrupts it, unmounted spiders climb normally, and no ledge-grab animation/boost happens without an obstructing overhang**; no out-of-scope custom iron gate or village changes. Check real client and dedicated-server behavior, alongside regression tests against M02's entrancement, zombie-door breaking and creeper evaporation.

**M09: PLAYTESTING. Implemented and verified as documented below; owner acceptance is still pending.**

## Playtesting Changes

**Owner feedback — 2026-10-13:**

- **Creeper retaliation:** owner reports it **works great** in actual play, with no correction requested at this stage.
- **Illuminated-area loitering and wooden-door breaking:** owner reports both **work great** and describes zombies frequently breaking doors; no correction requested at this stage.
- **Rare spider jockeys / ledge grabs:** **not yet observed by the owner**, consistent with intentionally rare recruitment; **not a reported failure and not owner-verified**. Keep the existing natural-jockey playtest gap open.

**M09 remains PLAYTESTING**, not SHIPPED; positive feedback on the observed behaviors does not count as blanket milestone acceptance.

## Implementation and verification — 2026-10-14

**Metadata:** M09 | PLAYTESTING | Persistent light interest, open doors, light-drawn wooden-door breaking, breach redistribution, creeper retaliation, rare jockeys with ledge grab.

**Implementation**
- Commits: `817a49de598dba930acfa56b5d71bb469ffa87c7` M09 behaviour, server tests, client test
- Delivered: lit spots remembered to 28 blocks, forgotten when dark; walled-off mobs loiter, use reachable open doors, break a wooden door within 6 path steps; breaches trigger immediate re-routing; unreachable-archer creepers blow the nearest door or the spot under the shooter; rare skeleton recruitment; collision-checked ledge haul with raised-arms pose.
- Deviations: ZOMB-03 (M02) now uses a dark room. Under an impassable cap, vanilla climbing still pushes the rider's head into the ceiling after the haul is abandoned.

**CI**
- Build: #37 | Run ID: 38501374609 | PASS
- Source SHA: `817a49de598dba930acfa56b5d71bb469ffa87c7`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38501374609
- Artifact: `simple-block-mod` id 11866715185, available. Build #37 log: all 188 required server tests passed.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 188/188 | local and Build #37; 20 M09 tests; M02 entrancement, zombie-door, creeper-evaporation regressions pass; [log](evidence/milestone-09/server-test-log.txt) |
| Client | PASS | 11/11 | full suite; client sees the grab pose and the creeper's blast under the shooter; [log](evidence/milestone-09/client-test-log.txt), [screenshots](evidence/milestone-09/screenshots/) |
| Dedicated server | PASS | 1/1 | ledge grab pose synced to a remote client |
| Negative controls | PASS | 16/16 | 8 features disabled, 16 expected failures; [log](evidence/milestone-09/negative-controls.txt) |

**Known gaps**
- GAP-01: MINOR — light behind walls is found by M02's random sampling, so door breaking can take minutes; next: playtest pace.
- GAP-02: MINOR — rates provisional (memory 28, door notice 6 steps, breach 24, creeper radius 10/reach 32/timeout 400, recruit 1%/200 ticks); next: playtest.
- GAP-03: MINOR — `pursuing_zombie_breaks_oak_door` (M02) flaked once locally (M08 GAP-01).
- GAP-04: MINOR — creeper screenshots hide the creeper; assertions cover it.
- GAP-05: MINOR — ledge grab tested with a no-AI rider pushed up a wall; next: watch natural jockeys.

**Owner playtest**
- At night, light a walled room; watch mobs loiter, then try an open and a closed wooden door.
- Put the lights out; confirm interest fades.
- Shoot a creeper from a tower and from behind a door; sword one; shoot one in the open.
- Leave skeletons and spiders near light; look for a rare jockey.
- Watch a jockey climb under an overhang.
