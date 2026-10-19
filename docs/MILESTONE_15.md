---
milestone: M15
status: PLANNED - NOT READY
---

# M15 — Cobblestone Engine three-position freight selector

**Owner-directed design, 2026-10-19. Planning only; this document does not authorize implementation.** Give the Cobblestone Engine an **actual, rear-mounted, three-position mechanical lever** that a player operates with **Space while riding in a coupled cushioned passenger cart**. It decides whether the engine's train passes Loading Rails or performs a valid loading/unloading transaction. This is an improvement to the *existing* M05–M07/M11 freight and station system, not a new locomotive, cargo cart, inventory GUI, or automation tech tree.

## 1. Physical engine model — non-negotiable

- **Location:** Remodel the **BACK of the existing Cobblestone Engine** to include a substantial, visually identifiable **three-position mechanical selector/lever**. It belongs on the engine's actual 3D model, in the existing recognizable minecart-sized engine body; do not put it on the front, on a floating sign, or only in the rider's UI.
- **Three real detents:** THROUGH, LOAD and UNLOAD correspond to **three visually different lever positions/angles**. The control **physically moves between positions** on a client-visible state change; its geometry and placement make the selected position readable without words. It is **not a flat texture change**, recolored pixel, static decal, floating text, nameplate, action-bar message, or substitute HUD icon. A small non-textual detail may complement the lever, but must not replace it.
- **Editability/art ownership:** Preserve and update the editable source at [`art/blockbench/cobblestone_engine.bbmodel`](../art/blockbench/cobblestone_engine.bbmodel), and the existing engine's renderer/UV mapping as necessary. Claude supplies functional model geometry, mechanical movement, animation/state sync and appropriate editable sources after authorization; Joe retains ownership of final painting/visual approval in [Joe's queue](Joe's%20To-Do/BLOCKBENCH_PAINTING.md). **Never overwrite hand-painted engine art or flatten the new geometry into a texture trick.** No new model or artwork is claimed to exist yet.
- **Feedback:** A restrained mechanical click on each successful change, plus the changed **physical lever position**, should communicate state. **No floating or written status feedback**. The lever must render consistently in singleplayer, multiplayer, first/third person where visible, and after chunk reload.

## 2. Seated controls and selector state

- **Space cycles exactly once per distinct key press:** THROUGH → LOAD → UNLOAD → THROUGH (order is the proposed intuitive cycle; confirm during owner playtest). Holding Space must **not auto-repeat**. Normal Shift dismount, forward travel and braking controls continue functioning; do not create unintended jumps or dismounts. Verify actual Minecraft 26.3 passenger-cart key behavior in a real client before choosing input hooks.
- **Where it works:** A rider seated in **any cushioned passenger minecart coupled into the same train as a Cobblestone Engine** operates that engine's rear-mounted selector remotely. The player does not leave the cart, click the engine, or open a menu. Space outside an eligible seated coupled consist is unchanged.
- **State belongs to the engine, not the passenger cart**; it is server-authoritative, persisted over save/reload and synchronized to every observing client/model. Disconnecting the passenger car must not silently copy the setting to another vehicle. The selected mode is an engine setting, not a new item.
- **Default selection, multi-engine consists, competing riders/inputs, and whether Space should cycle while the train is held at a station** need explicit conflict-safe design/owner review. Do not let two passengers race into unintended repeated changes or redirect a different train.

## 3. Three functional modes: permission, not forced stopping

A Loading Rail docks an engine-hauled freight train **only when the selected mode matches a genuinely possible physical transfer for the freight cart arriving there**. The source/receiver must meet all established M07/M11 geometry, commodity, capacity, obstruction, docking and ticking requirements. A hopper's *existence alone* is not a transaction.

| Selected mode | Eligible source/receiver at the arriving freight wagon | Behavior |
|---|---|---|
| **THROUGH** | Anything | Train passes the Loading Rail without docking, opening a freight gate or transferring cargo. |
| **LOAD** | A valid, currently available **source** (e.g. a supplying hopper above or a correctly aligned ore skip) that can put actual accepted cargo into this cart | Dock and load incrementally. If only unloading is possible, or no actual compatible source/item/room exists, pass through. |
| **UNLOAD** | A valid **receiver** (e.g. appropriately positioned receiving hopper or ore skip) that can accept actual cargo from this cart | Dock and unload incrementally. If only loading is possible, or no actual acceptable destination/cargo exists, pass through. |

- The mode is a **filter for direction and eligibility**. It does not mean stop at every Loading Rail or perform the wrong-direction transaction. In particular, **LOAD at an unload-only station is THROUGH behavior**, and **UNLOAD at an intake-only station is THROUGH behavior**.
- A rail with no available cargo, mismatched item, full freight cart in LOAD, empty freight cart in UNLOAD, full/blocked receiver, obstructed gate, wrong geometry, or incompatible source/destination is **passed without docking** when first encountered. Preserve the M11 constraints: one commodity per 256-item freight cart, gradual transfers, physical gates, exact item conservation and actual hopper cooldown/locking semantics. Do not infer a transfer from cargo-type labels or add invented item teleportation.
- **Once a valid transfer has begun and a train is docked**, later exhaustion, backpressure, a briefly full hopper or loss of throughput must **not silently send the train away or switch it into a different direction**. Hold safely and resume when the original exchange is again valid, or allow an explicit, safe operator release/cancel. Neither materials nor coal may duplicate or vanish. Distinguish an initially ineligible station (pass) from an already-engaged station (hold).
- A docked cart must not begin loading and unloading simultaneously. Preserve end/side gate alignment, the M11 above-hopper intake versus side/end-hopper discharge distinction, and established skip/cart directional geometry.
- A passenger-only consist with no eligible freight cart passes Loading Rails in all modes. A freight cart coupled to the engine may be *ahead or behind* the engine: evaluate the actual arriving wagon and the complete consist, not only the locomotive's position.

## 4. Coupled trains, rail yards and existing machinery

- **One setting governs the whole Cobblestone Engine consist**, but each freight wagon has its own cargo and must be assessed against the Loading Rail and receiver/source where *that wagon* is located. Do not transfer a distant wagon's inventory through another cart or require uncoupling merely to use its long-side gate.
- Respect M06's train-wide hold, gates and hinged rail stops when an exchange actually docks. The existing implementation switches off an engine at any dock; **in-seat operation must not strand the rider with no way to resume service**. Design a safe rider-operated hold/release/resume behavior compatible with the selector and physical stop before declaring M15 implementation complete. Do not let a parked train roll away unexpectedly.
- **Unresolved, explicitly NOT claimed solved by the selector:** automatic progression of multiple coupled wagons through a *single* Loading Rail; simultaneous servicing of several wagons at several Loading Rails; choice/prioritization of multiple competing hoppers; precise release/restart controls after a partial or finished exchange. The M07 sequential-wagon GAP-01 and M13 safety/stop requirements remain relevant. Propose a minimal coherent rule with the owner before implementation, test coupled consists and preserve conservation. **Do not silently add autonomous whole-train shunting or arbitrary cargo routing.**
- **Keep winches and cable-hauled carts working as before.** A cart hauled by a connecting rod/cable without Cobblestone Engine remains governed by the Loading Rail's existing physical docking and winch-stop behavior; a free/single ordinary freight cart is not granted an engine selector. Vertical skips retain their existing winch controls and station geometry. A train involving both a cable grip and an engine must have an explicitly defined control priority; do not rewrite winching accidentally.
- Through-running is an **engine-controlled exception** to existing automatic Loading Rail docking, **not a global toggle that causes established winch terminal stops to fail**. No new trackside lever, station GUI, signal circuitry, special hopper, extra intermediate machine, junction/turnout system or lift hill is included in M15.

## 5. Implementation acceptance after separate owner authorization

1. Real-client inspection of the **BACK** of the engine shows a **genuine modeled three-detent lever**, visibly moving for each state, including viewing from behind and appropriate rider perspectives. Texture-only, floating text, hovering icons and static lever substitutes fail acceptance. Edited `.bbmodel` remains valid; Joe's existing painted files are preserved.
2. From a cushioned cart anywhere in the same coupled consist, three discrete Space presses visit the three positions in order; holding Space produces only one change. Remote clients see the same physical lever, including after save/reload. Shift still dismounts and existing driving/braking inputs still work; ineligible passengers/other carts don't control it.
3. THROUGH passes otherwise-valid intake and discharge rails with zero docking, cargo transfer, stop raising or engine shutdown. Freight carts without a coupled Cobblestone Engine retain their established Loading Rail behavior.
4. LOAD docks and accepts real hopper→cart or valid skip→cart cargo. It passes an unload-only station, empty/incompatible source and full cart **without stopping**. UNLOAD docks and discharges to a valid physical hopper or skip, but passes load-only stations, empty carts and unusable receivers.
5. A once-valid transaction that stalls does **not** auto-bypass or reroute to another hopper. Verify gating, partial transfers, reactivation, manual cancellation/release, resource conservation and no new unattended fuel drain or stuck stop.
6. Mixed passenger/freight trains, engine at either end, multiple freight carts, adjacent/competing hoppers and sequential Loading Rails receive dedicated server and actual-client tests. Cover interactions with M07 GAP-01 and M13 stop/release changes; never claim autonomous multi-wagon servicing without a settled design and passing tests.
7. Winched single carts, winched coupled carts, and vertical ore skips retain their existing safety, transfer, start/stop and dock behavior. Test simultaneous/ambiguous engine-plus-cable control and unloaded chunk boundaries without forced loading.

**Open owner decisions before READY:** mode default on a newly converted engine; operator arbitration if several riders/engines share a train; rider release/resume/cancel input and whether the mode changes while docked; sequential wagon advancement versus manual indexing; competing hopper outlet selection; shared cable/engine driving priority. These are **real implementation/UX decisions**, not permission to invent a new fully autonomous station system.

**Lifecycle:** M15 is **PLANNED - NOT READY**. Nothing has been coded, modeled, painted, tested or accepted for M15. Implementation requires separate explicit owner authorization and later evidence under [RULES.md](RULES.md).
