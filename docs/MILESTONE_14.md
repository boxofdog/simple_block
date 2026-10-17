---
milestone: M14
status: PLANNED - NOT READY
---

# M14 — The Old Coot: a territorial abandoned-mineshaft miner

**Owner-directed design, 2026-10-17. Planning only: this milestone is NOT implementation authorization.** The Old Coot is a rare, semi-hostile, persistent underground NPC—not a new ore, tool tier, progression gate, villager profession, or ordinary hostile-mob replacement. He has lived in an abandoned mineshaft long enough to regard its ores and cave spiders as *his*.

## Appearance, identity and atmosphere

- **Name:** Old Coot (working/favored name; “Hermit Miner” was an earlier idea, not a second mob).
- **Silhouette:** Unmistakably **villager-like**, but **short, squat and broad like a dwarf** rather than a normal-height villager. **He MUST have a villager's prominent projecting nose**; do not omit or replace it with a generic humanoid/dwarf face. Keep Minecraft-style blocky proportions.
- **Distinctive art:** A **large bushy white beard**, battered old **miner's hat**, worn work clothing and an **iron pickaxe**. The nose, white beard, squat proportions and hat must all read clearly together from normal gameplay distance.
- **Behavioral atmosphere:** Mostly quiet and absorbed in his work; intermittent pickaxe strikes, footsteps and occasional grumbling establish his presence before he is seen. A clear angry response may accompany provocation. No forced trading, dialogue, quests or village affiliation are required.
- **Art ownership:** Claude supplies working entity geometry, animation, UVs, placeholder assets, and render/client tests only after implementation authorization; Joe owns final paint and approval. See [Joe's painting queue](Joe's%20To-Do/BLOCKBENCH_PAINTING.md). Do not overwrite owner art.

## Natural spawn and population rules

1. **Abandoned mineshafts only.** Natural spawn points must lie **inside a generated abandoned-mineshaft structure**, not in arbitrary caves, player-built tunnels, ordinary villages, or nearby open rock merely because a mineshaft is close.
2. **Lit areas only.** He is eligible to naturally spawn **only at sufficiently lit positions inside the mineshaft** (e.g., near existing or player-placed torches); do not use the usual darkness-only hostile-mob rule. The precise light threshold is an implementation tuning decision, not yet an owner-approved number.
3. **Maximum one living Old Coot per individual mineshaft**, **not one per entire world**. Distinct mineshafts may each have one. Keep an authoritative, persistent identity/occupancy record per generated mineshaft so chunk unload/reload, player movement, multiple players and server restarts cannot create duplicates. Do not rely only on nearby loaded-entity counts.
4. **Rare arrivals and respawns.** An eligible mineshaft does not need to have an Old Coot immediately. After a Coot dies, wait at least **7 Minecraft days (168,000 game ticks; approximately 2h 20m of actively advancing world time)** before another can spawn for that same mineshaft, and then use a **low-probability periodic spawn opportunity**, not a guaranteed spawn on expiry. Fine-tune probability and attempt interval without making him common. Being unloaded is **not** a death and must not start or reset the cooldown.
5. **Persistent residents.** Once spawned, he survives normal entity despawn conditions and retains his home-mineshaft association across save/reload. No global ticking of unloaded mineshafts, forced chunk loading, world-spanning structure rescans every tick, or offscreen autonomous mining. Maintain a bounded, reliable way to reconcile genuine death/removal versus temporary unloading.
6. Natural spawning must respect server authority, dimension/structure data and ordinary safety checks (valid space, ground, collisions). Spawn controls should not unintentionally block testing via explicitly summoned entities; natural-spawn cap is the gameplay requirement.

## Everyday behavior: the mine is his home

- **Neutral until provoked:** He can walk past a player without attacking; he does not object to walking, looting minecart/chests or breaking ordinary stone.
- **Actually excavates:** He wanders in and immediately around his home mineshaft, finds reachable **ordinary stone and deepslate only**, swings his iron pickaxe, and **really removes those blocks from the world**, leaving small, irregular tunnels/excavations. This is world-changing mining, **not** a looping sound or animation against untouched walls.
- **Strict block whitelist:** Never break coal/iron/gold/diamond/emerald/lapis/redstone ores, their deepslate variants, any other ore, rails, minecart tracks, mineshaft wood/supports, chests, spawners, torches, machines, player-built structure or other blocks. No mining cobblestone, cobbled deepslate or non-whitelisted stone variants by assumption. Avoid mining if it would undermine a protected structure or create severe unsafe damage; remain inside a bounded territory around the home mineshaft rather than tunneling indefinitely through the world.
- **Occasional real torches:** Every so often he places an actual torch in a valid safe position along his excavations, without overwriting blocks or destroying mineshaft infrastructure. The scattered pools of light and telltale tunnels are clues to his presence. Torch frequency, excavation radius/depth and exact material handling/drop pickup remain to tune; do not silently permit bulk ore deletion or unbounded item/entity accumulation.
- **Monster neutrality, both directions:** **All ordinary hostile mobs ignore the Old Coot, and he ignores them**; cave spiders are included. They neither proactively target nor attack each other. This is not a global pacification of hostile mobs toward the player.

## What makes him angry

- **Ore theft:** When a player successfully mines/breaks **coal, iron, diamond, lapis lazuli, redstone, gold or emerald ore**, including corresponding deepslate ore variants, **within the Old Coot's sensible local detection range**, he targets that player. He may notice mining through nearby walls; do not make this a dimension-wide omniscient alert. Breaking stone or deepslate, or merely carrying/looking at ore, does not trigger him.
- **Defending cave spiders:** If the Old Coot has **direct line of sight to a player who attacks a cave spider**, he attacks that player. The trigger is **seeing the player's attack on the cave spider**, not merely a cave spider taking unexplained damage, the player defending against another monster, or the spider existing nearby. Line-of-sight and short-range tests matter.
- **Self-defense:** A player directly attacking the Old Coot also provokes him, even if no ore or cave spider was involved.
- **Hostile response:** He **stops mining**, chases that player and fights with his pickaxe, with genuine damage, **until he or the player dies**, unless the player **escapes beyond his aggro/chase range and he loses track of them**.
- **Calm again after escape:** If the player gets sufficiently far away and is no longer being tracked, the Coot drops aggression and **returns to his normal neutral mining/torch-placing routine**. He should not permanently blacklist the player, demand a peace offering, flee/surrender, or remain hostile forever simply because of an old encounter. Decide reasonable detection/chase distances and loss-of-target hysteresis during tuning; avoid instant flickering between states.
- Other hostile mobs remain neutral with him during the chase. Do not let player-versus-Coot combat or cave-spider protection create unintended persistent monster-to-Coot aggro.

## Death drops and economy

- **A handful of emeralds** on death; the exact small integer range needs tuning/owner review, rather than inventing an approved drop count.
- **Exactly one iron pickaxe**, with randomized **5–50% durability REMAINING** (i.e., 50–95% used), not 5–50% damage taken and not an undamaged pickaxe. Clamp rounding to a valid non-broken item, and verify tooltip/durability-bar display.
- Death starts the home mineshaft's **7-day minimum respawn cooldown**. Kill-and-reload, chunk boundaries and multiple attackers must not duplicate the reward or the living resident.

## Boundaries, verification and owner acceptance

Implementation should be server-authoritative and perform bounded, chunk-aware AI, structure checks, pathfinding, real block modifications, torch placement and combat. No additional resource families, alternate ore deposits, cave-spider taming, villager trades, mining automation systems or M09/M12 monster-rule rewrites are in scope. The Old Coot's neutrality is specific to him; ordinary monster aggression toward players stays intact.

Required server, negative-control and real-client coverage after authorization:
1. Natural spawn **only inside mineshaft structure AND in adequate light**; darkness, unlit neighboring caves and unrelated lit caves reject. Independent mineshafts count separately.
2. Population never exceeds **one living Coot per mineshaft** under simultaneous players, chunk unloading/loading, saves/restarts or race conditions; the same individual stays persistent. Respawn blocked before seven Minecraft days; after cooldown, low-probability attempts (not instant guaranteed replacement) and valid lit positions required.
3. Genuine breaking of stone/deepslate makes an observable tunnel; verify **no ores, rare blocks, mineshaft supports, rails, containers, placed fixtures or non-whitelisted rock** are mined. Validate bounded excavation, safe torch placement, and no force-loaded chunks.
4. Ordinary walking/looting/stone mining is peaceful. Mining each named ore and deepslate variant nearby provokes; mining out of range does not. Cave-spider attack **seen** by Coot provokes; unseen attack does not. Direct player attack provokes.
5. While angry he attacks persistently, stops working and pursues the provoking player until death or genuine lost-range escape; if escaped, he returns to neutral work. Death yields the right emerald/pickaxe drops once, with **5–50% remaining durability**.
6. Zombies, skeletons, creepers, spiders **and cave spiders** neither target nor are targeted by him, including during player fights. Existing hostile/player AI must not regress.
7. Real-client appearance/screenshots confirm **short squat villager-like body, unmistakable villager nose, large white beard, hat**, pickaxe swing, mining, torches, neutral/angry cues and iron pickaxe durability UI; dedicated-server multiplayer and reload validation.
8. Meaningful negative controls should fail if the mineshaft cap, light gate, block whitelist, spider sight gate, monster neutrality, cooldown or loot-durability range is deliberately broken.

**Open tuning / not yet owner-settled:** exact light threshold, natural spawn chance/check interval, territory radius, mining and torch cadence, path safety, material pickup/drops, combat stats, detection/chase ranges, emerald quantity and precise model dimensions/animations. Preserve the owner-fixed behavior above; propose values for owner review rather than silently making them canon.

**Lifecycle:** M14 is **PLANNED - NOT READY**; no code, art, tests, CI, acceptance or release is claimed. Implementation begins only on fresh explicit owner authorization. Once implemented, append a concise dated verification section **at the end of this file** following [RULES.md](RULES.md), and wait for owner playtest/acceptance before SHIPPED.
