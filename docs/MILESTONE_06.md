---
milestone: M06
status: SHIPPED
---

# M06 — Loading Rail, coal winch and supported cable infrastructure

**Status: SHIPPED (owner accepted).** This is the **as-built M06 baseline**, not a new implementation handoff. Later [M07](MILESTONE_07.md) adds ore skips, four cart gates and two-drum control; [M08](MILESTONE_08.md) adds metallurgy; [M10](MILESTONE_10.md) adds workwear; [M11](MILESTONE_11.md) adds hopper handling. Historical implementation and tests remain in the appendix below. See [BUG-002](BUG_LOG.md) for the endpoint-reversal defect and correction, without duplicating its diagnosis here.

## Loading Rail and freight handling

- The **Loading Rail** is one ordinary rail plus one copper cable anchor in any crafting-grid positions; the registered ID remains `simple_block:loading_track`. It is a genuine rail section, not a separate unloading station or conveyor.
- An arriving **uncushioned freight cart** docks and the **entire coupled consist is held**. The track, not arrival direction, determines the cart's front. The hinged triangular iron stop rises **behind** the docked cart; it folds low while empty or released. The stop is visually attached to the inside rail edge, not a tall signalling post.
- **Right-click the Loading Rail** (even when a cart covers it) to release the waiting train. Stop any running Cobblestone Engine or coal winch on docking; release alone must never restart either machine or instantly re-dock the consist. A cushioned passenger cart is not a freight docking target.
- The M06 station supplied the stop and gate-ready interface; **skip-sensitive ramp/chute movement and freight exchange are M07**, not retroactive M06 features. Later M11 adds real hopper transfers. Ordinary nonpassenger carts and Cobblestone Engines cannot be shoved by walking into them; gravity, cable traction, powered engines and couplings still move them.

## Modular manual and coal winch

- Apply **one vanilla furnace to an existing cable drum** to convert its manual crank into a visibly mounted furnace and **three-position lever**, preserving the installed cable, route and anchors. The manual drum retains its held-input crank: **right-click pulls**, **left-click feeds**.
- Each right-click on the powered lever cycles **NEUTRAL → PULL → FEED → NEUTRAL**. PULL draws toward the drum, FEED sends away; reversal needs no intermediary fourth position. The lever stays latched without holding the mouse. At a physical endpoint, a Loading Rail, no fuel, an obstruction or unsafe stop, movement halts and the lever returns to NEUTRAL. The near-end direction trap was corrected under [BUG-002](BUG_LOG.md).
- Add ordinary coal at the **winch body** without moving the lever; no generator, generic shaft network or additional fuel machine. Fuel burns **only while freight actually moves**. Keep unspent coal and the exact remainder of a partly burning unit across stops and save/load. Never resume motion after loading a world or releasing a dock without a fresh deliberate command. The powered drive is faster than manual operation; numeric rates are tunable.
- M07 later permitted a second, empty drum at the far end. **Do not interpret this historical M06 description as forbidding M07's delivered two-drum behavior.**

## Cable recipes, lengths and safe long routes

- **Cable:** three iron nuggets in the **center column only** yield **two** cables; no string. **Coil:** three cables in the center column yield **one** coil; reversing a coil yields exactly **three** cables. Old shapeless/ring recipes and material-length accounting were retired; cargo/heavy weights use the revised cost.
- **One coil installs one reusable cable** on a drum, regardless of total route distance or return-strand length. Extensions and loop closure use no additional items. Correctly dismantling a line yields its single installed coil, never extra cable proportional to route length.
- Unlimited *total* length does not waive **12-block maximum individual spans**, required supports, unobstructed cable paths, correct straight-loop geometry, rod grip or track safety. No unsupported gaps or routing through terrain.
- Lines and their geometry are kept at the level, allowing the **loaded, locally active section** to operate without every distant anchor or the home drum being entity-ticking. An unloaded cart/rail/connection required for the current motion must **stop safely**, preserving route, cargo and fuel. No global force-loading, simulated travel of unloaded carts, unbounded world scans or automatic restart.

## Retired rails and Creative cleanup

- Powered, detector and activator rails are unavailable from normal acquisition, crafting and Creative/search; **ordinary rails remain**. Keep registered legacy IDs where necessary for saves and structures.
- Audit and hide items already retired by M01–M05 (e.g., shulker boxes, elytra, specified rock families, ender chests and chest minecarts, retired furnace-minecart item) without removing useful ordinary objects, mob eggs or unrelated recipes. Creative hiding and survival retirement must agree.
- The original M06 did **not** authorize subsequent ore skips, furnace metallurgy, clothing or more elaborate factories; those have their own milestone records and authorizations.

## Owner-corrected post-playtest details (2026-10-13)

The owner requested and accepted three focused corrections: [BUG-002's lever reversal](BUG_LOG.md); the **Loading Rail** display/search name (retaining its registry ID); and replacing the signal-like side post with a low hinged triangular stop on the rail's inside edge. Client tests covered docking directions, the stop's appearance and release targeting. The verification appendix records implementation SHAs, negative controls and CI.

## Implementation and verification — 2026-10-13

**Metadata:** M06 | SHIPPED | Loading Rail station, coal-powered winch, unlimited supported cable, redstone-rail retirement, no freight shoving, Creative cleanup and owner corrections.

**Implementation**
- Commits: `c38973c90d4532bca1f2cc4f617213b45615d56c` cable overhaul and winch core; `e0314631a47daaa3a0c2582cc49a26bdc52d11cb` Loading Rail; `85ad4fc981bc928b41c3d3cb5fab353ffab1deac` rails, Creative, shoving; `6114a118aa5641ad8b6a064e4c13a0904b3fb469` client proof; `e042fe96ec5eb6d6c2f1e1969767bf3c15ada7cc` long-line test isolation; `fc8f99a529225230f62742837fae505212c6468e` BUG-002, Loading Rail name, hinged stop
- Delivered: whole-train docking and release; furnace winch with coal burnt only while moving; one coil for any supported route, lines working where loaded; powered, detector and activator rails retired; freight carts cannot be shoved.
- Deviations: Winch controlled only at its drum. Lever cycle later replaced in M07; freight-gate model replaced by the M07 four-gate cart.

**CI**
- Build: #20 | Run ID: 38382953782 | PASS
- Source SHA: `fc8f99a529225230f62742837fae505212c6468e`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38382953782
- Artifact: `simple-block-mod` id 11803902813, available (verified 2026-10-13). Build #19 (`e042fe9`) PASS; #18 failed on an M02 door flake.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 131/131 | 129/129 before corrections; [archived report](archive/milestone-reports/MILESTONE_06_REPORT.md); [server log](evidence/milestone-06/server-game-tests.txt) |
| Client | PASS | 5/5 | [client log](evidence/milestone-06-corrections/client-game-tests.txt) |
| Dedicated server | PASS | 1/1 | `DedicatedServerClientTest`: real client over the network |
| Negative controls | PASS | 11/11 | 8 initial, 3 corrections; [controls](evidence/milestone-06/negative-controls-and-runs.md) |

**Known gaps**
- GAP-01: MINOR — a released train's later cars do not re-dock at the same Loading Rail (accepted M06 behaviour); next: owner decision tracked under M07.

**Owner playtest**
- NONE
