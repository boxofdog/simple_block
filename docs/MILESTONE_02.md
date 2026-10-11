# Milestone 02 — World, Night, Inventory

**Authority:** These Milestone-02 documents contain the user's agreed functional behavior. The separate tuning document identifies values that are merely starting targets. Existing repo facts and tools take precedence over guessed technical details.

## Scope and acceptance summary

| ID | System | Required result |
|---|---|---|
| GEO-01 | Geological simplification | Remove *natural generation and normal-survival access* to granite, diorite, andesite, and tuff families; substitute stone above the deepslate transition and deepslate below it. |
| MINE-01 | Deepslate access | Natural deepslate and all deepslate ore blocks require iron-tier-or-better **pickaxes**. Player-crafted cobbled/polished/decorative deepslate variants retain vanilla mining requirements. |
| ORE-01 | Iron | Ordinary iron obtainable across all ordinary Overworld biomes; deepslate baseline richer than stone; distinct large deposits in mountain/peak BIF-like strata, deep-ocean deepslate, arid rivers/canyons, and selected lava-adjacent regions. |
| ORE-02 | Diamond | Target ~30–40% of vanilla diamond ore generation, measured over comparable new-world samples. |
| LIGHT-01 | Torches / lanterns | Torch emits 12 block-light; lantern emits 15. Maintain spawn exclusion in sufficiently lit areas. |
| MOB-01 | Entrancement | **Overworld nighttime only**, eligible hostile mobs within 20 blocks of a sufficiently spawn-proof artificially illuminated **block** favor illuminated regions, even through walls. Natural wandering/loitering, no intelligence about settlements. Pursuit of a visible/detected player overrides the attraction. |
| MOB-02 | Doors | Zombies actively pursuing a player break all wood door variants at Hard-mode door-breaking speed on applicable non-Peaceful difficulties; also attack iron/metal doors without damage. No passive door vandalism. |
| MOB-03 | Creepers | In direct sunlight, briefly dissolve in a subtle smoke/ash *poof*; no explosion or terrain damage. Underground/shaded creepers remain. |
| INV-01 | Capacity | Exactly 27 usable player inventory slots: 18 main slots + 9 hotbar slots. No hidden accessible extras. |
| INV-02 | Sort | Inventory Sort button; merge compatible stacks and order by item registry order/ID, left-to-right **bottom row first, then upward**; never touch hotbar. |
| INV-03 | Hotbar transfers | When an interaction transfers items into hotbar and must select a free slot, select from the **left**, beginning at hotbar slot 1. Preserve sensible existing stack merges. |
| INV-04 | Heavy freight | One **shared** allowance of 64 ingot-equivalent units across iron, gold, and copper material/forms. Implement in this milestone, enabled by default, using all applicable acquisition paths. No speed debuff; excess not acquired. |
| FOOD-01 | Hunger | Reduce ordinary activity exhaustion by ~50%; leave combat and healing costs unchanged. Display saturation by default with an AppleSkin-like overlay without requiring AppleSkin. |

## Non-negotiable development rules

1. **First audit the existing repository.** It is no longer empty; Milestone 01 was reportedly completed with Fabric, an automated QA suite, real-client testing, and an updater. Verify those claims against files. Do not resaffold or replace build tooling.
2. **Milestone 01 regression-free:** beds cannot skip night/storms, phantoms absent in normal play, elytra flight and acquisition blocked, shulker boxes disabled. Keep existing tests green.
3. **New worlds only.** No migration, preservation, or recovery code for preexisting vanilla saves. Do not indiscriminately unregister vanilla blocks/items when dependent structures, registries, or recipes require them.
4. **Server authority.** Worldgen, harvesting, pickup limits, inventory slot restrictions, zombies, and time-dependent mob logic must work in the integrated server and be designed for dedicated server. GUI/hud purely client-side; server validates actions.
5. **No invented game systems.** No new geological items, ores, stamina bars, automation, crafting stages, structural-collapse rules, or custom minecarts now.
6. **Complete requested scope.** Do not defer heavy items or inventory shrinking to later freight-cart work. Use in-game and automated tests to evaluate these systems now.
7. **No speculative architecture.** Prefer version-compatible events, tags, data-driven worldgen, and clean isolated mixins where required. Avoid performance-heavy per-tick scans and chunk rewrites.
8. **Keep the existing one-click updater working.** CI artifacts must remain properly built and installable.

## Staged implementation; verify before proceeding

**00 — Audit and plan.** Report the exact codebase state (MC, Fabric, Java, mixins, tests, updater, Git cleanliness). Run baseline build/tests. Identify any test harness opportunities and major affected components. Report a concise plan before coding.

**02A — Mining and geology.** Implement generated rock substitutions and mining-tier rules first, then iron/diamond worldgen. Measure ore density over multiple identical seeds versus vanilla. Ensure large iron veins formerly associated with tuff and copper features associated with granite still behave sensibly after removal.

**02B — Inventory.** Implement true 27-slot capacity across screens/transactions, slot order rules and sort UI. Then implement the shared metal weight system. Verify no bypass by crafting, moving stacks, dropping/picking up, cursor/offhand/equipment swaps, containers, or logged-out/login states.

**02C — Night and hostiles.** Implement torch/lantern values, illumination-region attraction, zombie door behavior, and creeper evaporation. Test the light attraction **statistically** against vanilla/control, including walls and player acquisition/forgetting.

**02D — Hunger and HUD.** Change activity exhaustion, add clear saturation display, and test combat/healing unchanged. Final full regression/visual/client worldgen checks and a release artifact suitable for the existing updater.

Phases are individually commit-able if the existing repo workflow permits. Do not interpret phase separation as permission to ship only a subset of this milestone.

## Required final report

Write `docs/MILESTONE_02_REPORT.md` with: baseline findings; phase-by-phase changes and files; all config keys/defaults; test matrix with PASS/FAIL/NOT RUN and evidence; statistical iron/diamond comparisons; performance/AI observations; screenshots under `docs/evidence/milestone-02/`; known gaps; client/server coverage; actual build & launch commands; updater/CI verification; and any cases that require user balancing feedback. Distinguish between **tested mechanics** and **unverified gameplay feel**.
