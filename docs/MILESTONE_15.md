---
milestone: M15
status: PLANNED - READY
---

# M15 — Cobblestone Engine selector and multi-wagon freight stations

**Owner-approved design revision, 2026-10-20. PLANNED - READY is NOT implementation authorization.** After a separate explicit owner START, implement a physical three-position control on the existing Cobblestone Engine, two-block-spaced Loading Rail stations that can service several coupled freight wagons together, and optional vanilla hopper-side lever/button controls. This revises M05–M07/M11 freight behavior; it does not introduce a new locomotive, a new item-routing system, or a cargo GUI.

## 1. The engine's physical three-position selector

Mount a **real three-detent mechanical lever on the BACK** of the existing Cobblestone Engine. Each position must be visibly different and actually move in its 3D model:

| Lever position | Name | Meaning |
|---|---|---|
| **LEFT** | **THROUGH** | Freight service bypass: pass Loading Rails without docking or transferring. Releases an existing station service hold; the player still operates the train normally. |
| **CENTER** | **NEUTRAL** | No freight loading or unloading. A train already docked remains held; this does not release the station stop. |
| **RIGHT** | **SERVICE** | Seek a station, dock/snap when appropriate, and let each rail-aligned wagon determine loading or unloading from its actual hopper connections. |

The selector controls freight service, **not throttle or propulsion**. It must not make stationary carts drive away on its own.

- **Space** cycles THROUGH → NEUTRAL → SERVICE → THROUGH, **once per distinct key press**, without repeating while held.
- **Left-click** on the actual engine lever steps one detent **left**, toward THROUGH; **right-click** steps one detent **right**, toward SERVICE. End positions stay at their ends. Both require ordinary physical interaction range.
- Seated **Space control** is allowed only while riding a **cushioned passenger cart** that is **directly coupled to the engine**, **or is the sole passenger cart in that engine's coupled train** (freight wagons between it and the engine do not disqualify the sole passenger cart). A nonadjacent passenger cart in a train with multiple passenger carts does **not** get Space control.
- Physical left/right-clicks never acquire remote range merely because the rider is seated. Ordinary interaction with the engine remains possible within reach.
- The selected state belongs to the **engine**, not the seat/cart; synchronize server-authoritatively to clients and persist over save/reload. No accidental selector copying when consists split. Competing player inputs are serialized on the server. For multiple engines, remote Space control must fail safely unless the intended engine is unambiguous; a direct physical click affects only the clicked engine.
- Keep Shift dismount, normal cart controls, coal/engine movement, and other Space uses unaffected outside an eligible seat. Test Minecraft 26.3 input handling in an actual client before settling hooks.
- Use a restrained mechanical click and the moving 3D lever for feedback, **not floating text/HUD labels**. Preserve and update the editable engine Blockbench source at [art/blockbench/cobblestone_engine.bbmodel](../art/blockbench/cobblestone_engine.bbmodel); the owner retains painting/visual approval. Do not overwrite hand-painted assets. A conservative **THROUGH default** for a newly created engine is an implementation safety default to verify at playtest, not an additional operating mode.

## 2. Straight station geometry, lookahead, and snapping

- A multi-wagon station is a **straight run of Loading Rails spaced exactly two track blocks apart**, with ordinary rail between consecutive Loading Rails. From a Loading Rail, look **two blocks forward along the track in the train's direction of travel**: if another Loading Rail continues the run, keep rolling. **The forward-most/terminal Loading Rail is the docking reference**; do not stop at the rear of the station.
- On ordinary SERVICE-mode approach, dock only for a viable train/station freight opportunity; assess the **actual freight wagons and their positions across the consist**, not merely whether the lead engine, passenger cart, or first wagon contains cargo. The terminal reference determines alignment, not an arbitrary permission to unload the leading wagon.
- **Manual docking exception:** A player may position the train over the station and select SERVICE even if **every station hopper is capped**. That docks/snaps the consist for manual button/lever operation. Do not require a currently open or stocked hopper just to align it manually.
- Coupled carts normally have approximately **1.6-block center spacing** on the railway. **Only while docked**, temporarily align them at **2-block centers** to match the station's Loading Rails; restore the normal coupling spacing on release. Keep the couplings connected, within their actual permitted stretch, and prevent collision, rail departure, teleporting through obstacles, or invalid snaps. Test both engine-at-front and engine-at-rear approaches.
- Each rail-aligned freight cart may use **its own** physically aligned source/receiver and transfer **simultaneously** with other wagons. Engine/passenger carts occupying a berth do no freight transfer. If the consist has **more wagons than rail berths**, the remaining caboose/extra wagons are still shoved into the temporary snapped formation but **perform no transfer without a Loading Rail underneath**. More rails than wagons simply leave unused berths.
- If a station cannot be safely aligned (curve, insufficient track, obstruction, chunk edge), do not fabricate a remote transfer or forcibly move carts through blocks. No autonomous train shunting, automatic multi-stop indexing, or world-scale track search. Two-block lookahead and station scans must remain bounded and loaded-chunk aware.

## 3. Automatic freight decision — PER WAGON

In SERVICE, each **properly rail-aligned freight cart** independently decides what is physically possible, in this order. A transfer requires a real, currently permitted source/destination, acceptable item type, room, correct geometry and gates; proximity alone is insufficient.

1. **TRY LOAD FIRST:** A hopper **above** the freight cart, with **1–3 unobstructed blocks of vertical clearance** between its outlet and the cart, can supply items directly to the wagon: no intervening chest required. This is deliberately beyond normal vanilla cart/hopper reach. Accept only a commodity compatible with the cart's existing type and capacity; a **partly filled** wagon may be topped up with more of the same commodity. A full wagon, empty hopper, incompatible cargo, obstruction, or **capped** connection is not a valid load.
2. **OTHERWISE TRY UNLOAD:** If no eligible loading exists, look for a **valid adjacent receiving hopper** at the rail/wagon's unloading interface. If it is **open** and can accept the wagon's cargo, unload. An empty wagon, full/blocked receiver, wrong-facing or capped hopper, or incompatible inventory is not eligible.
3. **NEITHER POSSIBLE:** This particular wagon does nothing; leave its cargo untouched. On ordinary approach, a station with no eligible wagons is passed rather than treated as an automatic transaction. Manual docking remains possible under §2.

A station may **load some wagons, unload others, and leave others untouched at the same time**. Hopper positions and their individual controls decide; the engine's SERVICE position does **not** force all wagons to perform the same direction.

**Latch the direction of each wagon's transaction.** Once loading starts, it must not flip to unloading during that same station visit merely because the supplying hopper empties. Once unloading starts, it must not flip to loading. Reevaluate and choose anew only upon a **newly requested** SERVICE operation after completion or cancellation; never automatically cycle cargo through the same station. Preserve M11's one-commodity/256-item limit, gradual hopper throughput, physical gates, exact conservation and Ore Skip/cart transfers with their established directional/position requirements.

## 4. Ordinary hopper levers, buttons, and physical caps

Use **ordinary Minecraft levers or buttons on a block ADJACENT TO the relevant hopper**. They are **not** modeled as attachments to the hopper and do not require multiple new Hopper Blockbench states, a special block or a trackside GUI. Resolve which nearby control governs **which individual hopper's freight-facing opening** deterministically; never let one lever accidentally control a whole station.

| Hopper's local control | Freight-facing opening | Cart transaction behavior |
|---|---|---|
| **No lever/button** | Open | Participates automatically in §3. |
| **Lever ON** | Open | Stays eligible for automatic freight transfers. |
| **Lever OFF** | **Capped** | Never participates in an automatic cart transaction. |
| **Button nearby** | **Normally capped** | A press temporarily opens this specific connection and requests **one complete eligible transaction** for its aligned wagon, then the cap closes again. |

A button means **one full loading or unloading operation, not one item**. A button press with no aligned SERVICE-mode cart or no valid transfer does not extract/insert anything. Button control allows deliberate per-wagon freight handling; a lever is the persistent alternative.

- Model a **simple visible lid/cap covering only the hopper opening that interfaces with the Loading Rail**. Above-cart supplying hoppers close their downward freight outlet; adjacent receiving hoppers close their cart-facing intake. Where appropriate, a free-hanging freight outlet is capped, **not** a legitimate chest connection. Avoid generating dozens of 3D model variants.
- **Closing the freight opening does NOT lock the whole hopper.** Example: chest → hopper above rail still fills the hopper while its freight outlet is capped; items already in a side hopper can still flow into its attached chest while its cart-facing entrance is capped.
- For hoppers **actually associated with a Loading Rail freight interface**, bypass vanilla redstone **hopper-locking** effects on that hopper: the nearby lever/button governs the **freight lid and eligibility**, not an unintended global inventory freeze. Retain vanilla item movement at the hopper's other inputs/outputs and all ordinary hopper behavior **outside** a Loading Rail installation. Do not globally disable redstone locking or introduce remote item teleportation.
- For ambiguous/multiple neighboring controls, use a documented, fail-closed deterministic binding rule; never silently enable transfer through an explicitly closed lid.
- Changing a hopper's own lever to OFF during service must shut **that opening** without moving any additional items; it must not cancel transfers at other wagons. Button openings close on completion or cancellation.

## 5. Docking, completion, grace periods and release

**A transaction moves items incrementally, not in one burst.** Keep the physical loading/unloading gates and existing hopper/skip transfer pacing.

A wagon's **LOAD** transaction completes when:
- It reaches full 256-item capacity; **or**
- Its supplying hopper runs dry and **stays dry for a short grace period** (a few ticks, long enough to respect actual hopper transfer cadence).

A wagon's **UNLOAD** transaction completes when:
- Its cargo becomes empty; **or**
- Its receiving hopper remains **full/unable to accept cargo for the same grace period**, indicating the connected storage is backed up.

For either direction, **resumed legitimate material flow resets the grace countdown**. A short interruption is not completion; an expired grace period permits the wagon to finish **with remaining capacity or cargo**. No leftovers vanish or move into an impossible inventory. Tune and test the exact short delay against real hopper transfer cadence and network latency.

- **All-wagon coordination:** The engine returns automatically from SERVICE to **NEUTRAL only when at least one transaction has actually begun and all currently participating wagon transactions have completed**. One wagon finishing cannot interrupt another's active exchange.
- **Waiting for a manual command:** If the player manually docks in SERVICE with every hopper capped, **stay docked and armed in SERVICE**, even with zero active transactions, so nearby button presses or lever openings can initiate work. Do not instantly return to NEUTRAL for "nothing to do." A button only triggers freight while the train is actually docked in SERVICE; it cannot bypass NEUTRAL or THROUGH.
- **Explicit train-wide override:** Any **player-commanded switch away from SERVICE to NEUTRAL or THROUGH** immediately stops **every** active wagon transaction, including button-initiated ones. Grace periods are ignored; any button-controlled openings return to capped. Already moved items stay put, untransferred items stay where they were. Interrupted transactions must **not silently resume** without a new SERVICE request. Lever settings are not reset.
- **NEUTRAL retains the physical station hold and prevents all further transfers** (including newly pressed buttons). **THROUGH** cancels service, releases the station hold, restores normal coupling spacing and allows player-operated forward movement/bypass. Neither state drives a stationary train automatically.
- Release or cancellation must preserve cargo and coal, correct train stops, gates, and synchronization across clients. An engine switched to NEUTRAL while not docked does not invent a new permanent railway brake; these modes govern station service/holds, not general propulsion.

## 6. Compatibility and implementation boundaries

- Preserve non-engine Loading Rail and winch/cable behavior for free carts, cable-hauled carts and vertical Ore Skips. THROUGH is **only** the Cobblestone Engine's station-bypass instruction, not permission to bypass an existing cable winch's safety/terminal stop.
- Preserve existing ore-skip interfaces, M06 hinged-stop and train-wide hold rules, M07 sequential-wagon and M13 station safety fixes where applicable. Engine-plus-cable combinations must behave safely and deterministically: do not let the engine selector defeat winch safety constraints or move a held consist unexpectedly.
- Server decides selector permission, station ownership/alignment, hopper binding/lids, transaction eligibility, direction, transfer count, stop/release and completion; clients render synced state. Do not scan unloaded track, minecart entities or inventories without bounds or force chunk loads to find more stations.
- No new locomotive, freight variant, special hopper item, cargo GUI, floating text, station control network, full redstone redesign, automated train dispatch or artificial item teleportation. Preserve Joe's engine textures/paint; the physical lever and simple hopper caps require functional placeholder geometry only until approved.

## 7. Acceptance tests after explicit implementation authorization

1. **Controls/model:** Real rear-engine three-position lever visibly changes THROUGH / NEUTRAL / SERVICE in singleplayer and dedicated multiplayer and after reload. Space cycles once per key press in the **directly coupled OR sole passenger** cushioned cart, not a distant cart when multiple passenger carts exist. Physical left/right clicks step in the correct direction only within reach. Normal movement/dismount still works.
2. **Two-block station:** Intermediate rails pass; forward-most/terminal rail anchors docking. On SERVICE, coupled carts snap from 1.6 to 2.0 spacing, transfer on each occupied eligible Loading Rail **simultaneously**, and return to regular spacing on THROUGH. Test engine at either end, mixed passenger/freight consists, surplus caboose wagons, extra rails, obstructions and unloaded edges.
3. **Manual docking:** With ALL hoppers capped, a player can position a train, choose SERVICE, snap/hold it without a live transaction, then initiate a valid transfer using a button or by opening a lever. It must not auto-neutral before a transaction begins.
4. **Direction and clearance:** Load from a real overhead hopper with 1, 2 and 3 clear blocks; reject blocked/out-of-range sources. Empty and partly filled carts load compatible items, mismatches are rejected. With no eligible overhead cargo, an eligible side hopper unloads; load-first wins when both are actually viable. No wagon changes direction within the same service visit.
5. **Controls and lid:** No-control automatic, lever ON/open, lever OFF/capped, button normally capped/one full transaction. Adjacent-block placement chooses the intended hopper only. Physical lid closes **only the freight opening**; normal chest ↔ hopper flow continues. Test ordinary redstone-locking outside stations remains vanilla and station controls do not inadvertently lock the hopper itself.
6. **End/interrupt:** Loading completes at capacity or after dry-source grace; unloading completes at empty or after full-receiver grace. Brief restoration resets grace. Different wagons may finish separately; the last active finisher returns the engine to NEUTRAL. Player switching to NEUTRAL or THROUGH cancels **all** transfers immediately, keeps exact inventory conservation, and caps button ports; THROUGH releases the hold without automatic acceleration.
7. **Regression:** Verify item conservation under partial loads, full hoppers, simultaneous transfers and rapid lever/Space inputs; no duplicate items/XP, no stranded engine. Test hoppers, skips, gates, single-cart and winched operations, engine/cable overlap, chunk reload/multiplayer, and M07/M11/M13 integration. Run scoped game tests, real-client tests and CI; include negative controls that fail if caps, direction latching, clearance, snap, or cancellation are broken.

## Lifecycle and handoff

**PLANNED - READY (owner design agreed 2026-10-20).** This document replaces M15's earlier separate LOAD versus UNLOAD modes and earlier "any seated cart controls the engine" proposal. All requirements above are ready for a bounded Claude Code implementation **only after the owner separately authorizes START**. Unspecified numerical tuning (e.g. exact few-tick grace) and safe fail-closed ambiguity handling are implementation choices, not permission to add features; escalate any unavoidable tradeoff that changes player-visible behavior. When implemented, record commits, exact tested SHA, CI, test counts, gaps and owner playtest steps in one dated appendix to this file per [RULES.md](RULES.md). Do not promote to SHIPPED without explicit owner acceptance.
