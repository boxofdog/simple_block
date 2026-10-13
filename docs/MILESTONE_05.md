---
milestone: M05
status: SHIPPED
---

# Simple Block — Milestone 05: Working Haulage and Cobblestone Engine

**Status:** Owner-requested implementation handoff; **not implemented**. Read the [M04 historical report](archive/milestone-reports/MILESTONE_04_REPORT.md), [M05 brief](MILESTONE_05.md) and [current roadmap](ROADMAP.md) first. The former decisions ledger is archived.

**Goal:** Make the existing M04 overhead cable infrastructure move minecarts reversibly, add an operable crank, introduce a bidirectional coal-powered Cobblestone Engine, and fix the known drum render artifact. **Preserve all existing M04 parts, placement gestures, models and save behavior.** Build additively; do not scrap or reimagine the system.

This is a **larger-than-usual milestone** comprising independently testable work packages. Finish each without unrelated systems, preserve regression coverage, and ask the owner before creating any new user-facing item or major mechanic beyond these requirements.

## A. Reversible overhead cable haulage

1. **Leave M04 construction intact.** The player still loads coils onto the existing cable drum, picks up the loose hooked cable end like a lead, and runs a single visible hauling cable through underside-mounted copper anchors. Open-ended routing, extension and dismantling must remain usable. Existing ground-mounted drums should continue to work.
2. **Add an optional closed-loop configuration** to that route, not a replacement. The original cable remains the **hauling strand in its existing location** above the railway. A **secondary return strand** runs discretely *beside* it on the anchor's mounting side, away from the minecart rod; do not vertically stack a strand into the support blocks.
3. **Terminal return wheel:** at the last anchor, add a 180-degree return pulley assembly when the route is deliberately closed. **Preferred minimum-ingredient interaction:** apply another existing copper anchor to the terminal anchor to convert/augment it into a return wheel. This specific activation gesture is a provisional UX choice: if it conflicts with M04 interactions, propose a minimally invasive equivalent before changing behavior. Do not add a new pulley item by default.
4. **Intermediate supports:** while looped, existing anchors visually accommodate the secondary strand with compact return guides. Keep their original roller and mounted support intact. Return guides/wheel can be extra model parts/states; preserve editable Blockbench source and UV layouts, without painting finished textures.
5. **Conservation and validity:** closing the loop requires enough *actual cable* to form the return strand and curved ends. Consume it from the existing drum/coils (subject to current capacity) and refund it on dismantling. Reject closure clearly if insufficient length, obstructed return path, invalid turns, or an unsafe geometry. Never silently generate free cable, clip through wood, or retract the working cable off supports during operation.
6. **Drive and reverse:** the existing drum acts as a reversible **drive wheel/capstan** while a loop is closed, circulating the cable without accumulating it on the spool. Its opposite mode of winding/releasing a *loose* line remains available for current M04 construction/dismantling and eventual vertical hoists. Reverse motion changes the hauling strand's direction, not which strand the cart grips.
7. **Minecart rod:** the existing rod **only grips the original hauling strand**, regardless of direction; it never attaches to the return strand. The minecart travels along the supported rail while that strand moves, can move uphill/downhill as feasible, and releases safely before reaching either terminal assembly. Supports and rod must not snag. If a corner cannot be traversed safely, explicitly refuse/stop rather than teleporting or cutting through supports.
8. **Safety:** controlled stopping must halt or hold the cart instead of letting the winch run it through an end wheel; disconnected/broken routes stop driving safely. Reversal must not dismantle the installed route. Preserve state across saves and unloaded chunks without duplication or runaway carts.

## B. A crank on every cable drum

- The existing drum **has a visible physical hand crank by default**, even before a cable coil is installed; no extra crank item or recipe.
- Holding interaction on the **crank**, rather than repeatedly clicking the drum, operates it; direction must be reversible. **Owner's later universal control clarification (while M05 implementation was underway): with an empty hand, hold left-click on the crank to FEED cable away from the winch and hold right-click to PULL cable toward the winch. This applies to lateral railway haulage as well as future skip hoisting (left lowers, right raises). For M05's closed-loop rail cable, this describes the direction of the moving hauling strand—not physical winding/payout of cable; the installed loop length remains fixed.** Vanilla left-click normally damages/breaks blocks, so reserve the crank hit zone for interaction and preserve intentional drum breaking/dismantling outside that zone. Do not collide with M04 coil loading, hook pickup or sneak-use retraction, and make crank interaction discoverable without lengthy action-bar dev text. **This clarifies M05 crank inputs only; do not implement skips or stations in M05.**
- Animate the drum/crank and actual cable travel together. Manual motion should be useful but modest; do not require frantic repeated clicks or exaggerated chore mechanics.
- The preferred physical installation is **overhead on wood supports**, aligned with the copper rollers and leaving rail clearance. Add overhead compatibility where reasonably possible **without removing floor/ground placement**, changing the user's current drum model wholesale, or requiring an inaccessible operating position. Keep exact attachment geometry minimal.
- For a rail-bound winch, uphill hauling slows as **train mass and grade** increase; very steep heavy hauls should be slow. Existing minecarts already have significant empty mass. Do not apply the same rail-only penalty to future vertical skips (not part of M05). Select conservative initial tuning and report the values rather than asserting a final physics simulation.

## C. Cobblestone Engine

- **Creation:** applying an ordinary **furnace directly to an existing vanilla minecart** converts it into a **Cobblestone Engine**, in world. Do not make it by crafting a vanilla furnace-minecart item or by inventing another recipe/vehicle family.
- **Breaking/disassembly:** drops **one regular minecart and one regular furnace**, never a combined engine item. Persist engine state safely; guard against duplication on damage, explosions and reloads. Address the old furnace-minecart recipe/item so it cannot bypass the intended construction flow.
- **Look:** a distinct new 3D locomotive model, with editable `.bbmodel` + UVs for owner-painted textures; do not generate finished art. Existing freight minecart/cushion appearance and behavior remain intact.
- **Function:** consumes coal while propelling itself; can travel **either direction** and push or pull connected freight, passenger or mixed carts. Do not arbitrarily prohibit loaded freight. A **minimal reliable coupling** for at least a few linked carts is required to demonstrate *pulling*; prefer a simple vanilla-material/interaction solution over an elaborate new train system. **Before implementing the coupling gesture, obtain owner confirmation if a new item, non-obvious mechanic, or surprising interaction would be needed.**
- **Traction:** an empty minecart is already heavy; cargo adds weight. Total cart mass, train length and grade significantly limit locomotive uphill pulling/pushing capacity. Level routes should reward good track design. Some sustained steep rises should stall an engine even when hauling empty carts. No hardcoded freight-vs-passenger mobility restriction.
- **No extra systems:** no locomotive tech tree, electric network, speed-control dashboard, automatic rail routing, new three-block-high rail shapes, vertical skips, coal winch motor, or counterweight engineering.

## D. BUG-001 — drum-side rendering artifact

Investigate and correct the side-panel ghosting/striping seen on the M04 drum. Possible overlapping faces/z-fighting or UV/render overlap; **cause not yet established**. Fix the model source **and** the runtime-rendered result; keep Blockbench editable. Compare close-up in-game screenshots at multiple angles. In [ROADMAP](ROADMAP.md), mark resolved **only after verification**, not because the code compiled.

## Acceptance checks

- Current M01–M04 tests remain green; run `./gradlew build` and the actual-client test pipeline. Add server/game tests for all new critical behaviors and a real-client test exercising both mechanical systems.
- Existing loose-ended cable construction, reel additions, routing/retraction, rod fitting, cargo and cushions **still work**. Breaking an installed loop restores cable/material correctly; closing without sufficient cable fails without item loss.
- One assembled reversible cable loop draws a cart along the **working strand**, stops at the end and brings it back on reverse, including on an incline. No grip on the returning strand. Validate geometry through at least a straight run and a supported grade; test turn behavior, refusal or operation explicitly.
- Drum has a visible, animated, usable crank; hand operation does not trigger dismantling or coil loading by accident. Ground-mounted placement remains functional; overhead positioning is demonstrated if implemented.
- Furnace-on-minecart conversion produces a Cobblestone Engine; breaking returns exactly its furnace and minecart; coal powers **bidirectional** movement. At least one mixed coupled consist can be moved both ways. Show that a sustained climb stresses/stalls the locomotive compared with an equivalent flat run.
- BUG-001 shows no side ghosting in multiple in-client views; retain a before/after screenshot and note the underlying cause.
- Save/reload and chunk-unload safety tests; negative controls where feasible. **For future authorized corrections only**, append a concise dated report at the very END of `docs/MILESTONE_05.md` with tests, limitations, evidence links, SHAs and CI; old `*_REPORT.md` remains historical. **Stop after M05.**

## Deliberately left for later

Upright vertical skips and dumping into them, counterweights, coal-powered winch attachments, extreme three-block rails/crash physics, sophisticated locomotive controls, and full train-network logistics. These remain roadmap work, **not implied tasks**. Gravel/sand cargo eligibility is a separate approved but unimplemented follow-up unless the owner explicitly folds it into this milestone.

## Implementation and verification — 2026-10-12

**Metadata:** M05 | SHIPPED | Reversible cable loops with a manual crank, overhead drums, Cobblestone Engine, chain coupling and the BUG-001 drum render fix.

**Implementation**
- Commits: `e662ec21724868336d67ac6dd64d6eb1385f1ee1` work packages A–D; `16b10988d80643741f9e729531cb52c7eff00067` crank controls per owner clarification
- Delivered: closed loops haul and hold rod carts both ways; held left-click feeds, right-click pulls; drums hang under beams; furnace-on-minecart engine with seat driving; iron-chain couplings; drum flange ghosting fixed.
- Deviations: NONE recorded. Engine visuals were later changed in M07.

**CI**
- Build: #17 | Run ID: 38306694723 | PASS
- Source SHA: `16b10988d80643741f9e729531cb52c7eff00067`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38306694723
- Artifact: `simple-block-mod` id 11772702932, available (verified 2026-10-13).

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 114/114 | [archived report](archive/milestone-reports/MILESTONE_05_REPORT.md); [evidence](evidence/milestone-05/gametest-evidence.md) |
| Client | PASS | 3/3 | `SurvivalSmokeClientTest`, `CableClientTest`, `HaulageClientTest`; [screenshots](evidence/milestone-05/screenshots/) |
| Dedicated server | PASS | 1/1 | `runServer` start only; `stop` over piped stdin returned a command error |
| Negative controls | PASS | 11/11 | 10 server, 1 client; [negative controls](evidence/milestone-05/negative-controls.md) |

**Known gaps**
- NONE

**Owner playtest**
- NONE
