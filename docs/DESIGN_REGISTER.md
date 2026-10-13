# Consolidated Design Register

**Decision-record labels (not milestone lifecycle states):** **DECIDED** = user explicitly established the direction; **DIRECTION** = strong preference, needs specifics; **EXPLORATORY** = floated idea or assistant suggestion, not approved; **OPEN** = unspecified. The five allowed **milestone** status labels live in [STATUS_KEY.md](STATUS_KEY.md). This file is design context, not permission to code.

## A. Survival, monsters, lighting

| ID | Status | Requirement or direction |
|---|---|---|
| S-01 | DECIDED | Do not skip nights using beds. |
| S-02 | DECIDED | No phantoms in ordinary gameplay. |
| S-03 | DECIDED | Nighttime should be dangerous enough that enclosing and defending land matters. |
| S-04 | DECIDED | Monsters gravitate toward **illuminated areas**, not individual light-source blocks; they do not spawn in adequately illuminated locations. |
| S-05 | DECIDED | Torches illuminate substantially less; lanterns illuminate substantially more. Exact values TBD. |
| S-06 | DECIDED | Creepers instantly disappear in direct sunlight (no explosion); shelter from sunlight still permits survival. |
| S-07 | DECIDED | Hunger is more forgiving than vanilla; do not make it a resource-management sim. |
| S-08 | DIRECTION | Secure territory by physical walls and illumination, not arbitrary land claims. How monsters behave near walls remains open. |

## B. Mining, geology, materials

| ID | Status | Requirement or direction |
|---|---|---|
| G-01 | DECIDED | Progression stone → iron → diamond should not happen routinely within minutes. |
| G-02 | DECIDED | Deepslate requires iron or better to mine. |
| G-03 | DECIDED | Diamonds are genuinely scarce. |
| G-04 | DECIDED | Iron is accessible in small/scarce underground deposits across ordinary Overworld biomes; **larger/richer concentrated deposits** are more common in selected biomes. This supersedes iron-exclusive-biomes. |
| G-05 | DECIDED | Remove diorite, granite, andesite, and tuff and associated unwanted variants/generation (future work; audit vanilla generation dependencies first). |
| G-06 | OPEN | Exact ore distributions, rich biomes, vein sizes, diamond generation, and resource clue systems. |

## C. Mobility and freight

| ID | Status | Requirement or direction |
|---|---|---|
| T-01 | DECIDED | Minecarts must become excellent for travel **and** hauling, without redstone dependency. |
| T-02 | DECIDED | No elytra and no shulker boxes in normal gameplay. |
| T-03 | DECIDED | Freight cart is open-topped with **visible cargo inside itself**, rather than a chest cart. |
| T-04 | DIRECTION | Riding a standard cart requires installing a visible cushion; baseline cart is for freight. |
| T-05 | DIRECTION | Coal-powered locomotive should push and pull coupled carts, reverse direction in place, and be far more useful/fuel-efficient than vanilla furnace carts. |
| T-06 | DECIDED | Multiple minecarts can couple into a train. |
| T-07 | DECIDED | Underground powered hauling uses a **moving overhead mechanical cable**, not electricity. A cart-mounted rod/grip connects to it. |
| T-08 | DECIDED | Cable anchors attach to ordinary wooden blocks, letting players build timber sets or surface posts. Proposed cheap recipe: two vertical iron nuggets; yield TBD. |
| T-09 | DIRECTION | A hand winch may power early cable hauling. One cable serving one route is preferred; independent branches probably need their own cables. |
| T-10 | DECIDED | Vertical freight uses upright mining skips in shafts; an **elevated unloading rail** tips cart cargo down into the skip. No sideways skip model. |
| T-11 | DIRECTION | A counterweight locked at top, a release lever, wire, pulley, and hoist raise loaded skips. Resetting the counterweight is open. |
| T-12 | DECIDED | Do not force timbering using collapse mechanics; supports should be useful because they carry haulage/lighting hardware. |
| T-13 | OPEN | Actual cart/skip capacities, speeds, fuel usage, coupling, routes, turns, slopes, stations, animations, and precise balancing. |

## D. Mechanical systems and automation

| ID | Status | Requirement or direction |
|---|---|---|
| M-01 | DECIDED | Remove **redstone circuitry as a system**, not all useful redstone-associated objects. |
| M-02 | DECIDED | Re-evaluate and preserve useful things such as hoppers, dispensers, observers, compasses; replace their recipes/operation as appropriate. |
| M-03 | DECIDED | Severe limits on autonomous resource-generating automation; manual operation and physical mechanisms are acceptable. Exact allowed mechanisms open. |
| M-04 | DECIDED | Literal wires/cables connect meaningful mechanical parts. Avoid simply reproducing abstract redstone logic. |

## E. Inventory and UX

| ID | Status | Requirement or direction |
|---|---|---|
| U-01 | DECIDED | Add inventory/container sorting. Exact scope and sort criteria TBD. |
| U-02 | DECIDED | Shift-transfers to the hotbar prioritize slot **1, then 2, then 3** rather than the last slot. |
| U-03 | EXPLORATORY | Slightly smaller personal inventory; may be unnecessary if heavy-material limits solve the concern. |
| U-04 | EXPLORATORY | Heavy materials, especially iron in any form, might be limited to one stack-equivalent across the player's inventory. Must prevent conversion into iron blocks from bypassing limit. |
| U-05 | EXPLORATORY | Single-click bulk unloading of heavy materials into dedicated freight storage. |

## F. Asset production

| ID | Status | Requirement or direction |
|---|---|---|
| A-01 | DECIDED | Joe paints/refines final art; Claude Code builds functioning models, UV layouts, placeholders, exports, animations and renderer mapping. Central owner painting queue: [Joe's To-Do](Joe's%20To-Do/README.md); future commodity fill visuals: [M11](MILESTONE_11.md). |
| A-02 | DECIDED | Keep editable Blockbench `.bbmodel` files for custom assets, plus replaceable PNG textures. |
| A-03 | DECIDED | Do not overwrite user-painted art or destroy editable source files during later code work. |
| A-04 | OPEN | Loader-specific model export approach/animation libraries; choose only when needed for an actual custom asset milestone. |

## Critical distinction

The exact numeric proposals made during discussion (e.g., a 27-slot inventory, 64 heavy units, 10–12 blocks/sec locomotives, 10–15 minutes/coal) are **not decisions**. They are potential test baselines requiring player approval or playtesting.

## Current authorized implementation

**Current milestone lifecycle/authorization is controlled by [ROADMAP.md](ROADMAP.md) and [STATUS_KEY.md](STATUS_KEY.md), not this historical design register.** As of 2026-10-13, M01–M06 are SHIPPED, M07 is PLAYTESTING, and no M08–M11 implementation is authorized.
