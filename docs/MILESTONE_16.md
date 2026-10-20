---
milestone: M16
status: PLANNED - NOT READY
---

# M16 — Village generation and settlement-scale overhaul

**Owner-directed scope revision, 2026-10-20.** M16 is a **fundamental redesign of how Simple Block generates Minecraft villages**, not simply a larger set of vanilla houses or an attempt to stretch the existing jigsaw configuration. The goal is for villages to resemble functioning settlements with appropriately sized homes, streets, public buildings and surrounding agriculture, using the owner's handcrafted architecture.

This is the **physical and world-generation foundation** for [M17 — commerce and work](MILESTONE_17.md), [M18 — Iron Golem guardians](MILESTONE_18.md) and [M19 — villager belonging and wellbeing](MILESTONE_19.md). Its immediate design task is to understand the scope and choose an appropriate generation architecture.

## 1. Core purpose: rethink village assembly

Minecraft's existing villages are assembled from small, largely fixed structure pieces with substantial spread and terrain-fitting constraints. M16 should examine **replacing or substantially reworking the village assembly logic** so the resulting settlement is planned as a coherent whole, rather than constrained by vanilla's jigsaw radius or patched together from unrelated district centers.

The intended world-generation system should be able to:

- **Plan a whole settlement:** Choose suitable land and organize a town center, streets, residential lots, workshops, public buildings and outskirts as parts of one village.
- **Support substantially larger villages:** Scale village footprints, building count and population to the settlement being designed, rather than reducing the architecture to fit vanilla generation limits. Exact sizes and distribution need investigation and owner direction.
- **Fit real terrain:** Evaluate slopes and usable land, favor sensible sites and lots, and accommodate gradual elevation changes through roads and placement rules. Avoid the familiar cliffs, dirt pedestals, buried doors, disconnected streets and floating buildings.
- **Generate meaningful outskirts:** Give plains villages room for proper cultivated fields, pastures and farms instead of a few token crop rows or tiny animal pens. Agricultural scale and arrangement should suit the biome and town.
- **Populate livestock appropriately:** Investigate how farm animals are initially spawned or placed in generated pens/pastures, how they stay associated with those areas, and how to avoid empty farm structures or uncontrolled population growth. Distinguish initial world-generation population from later ordinary animal spawning/breeding.
- **Define coherent settlement identity and extents:** Decide how a large generated village is recognized as **one settlement**, including its homes, workplaces, outskirts and connected areas. Provide usable boundaries/points of interest for villager home/work recognition (M17/M19) and Iron Golem guardian detection/patrols (M18), without assuming vanilla's village radius still matches the new footprint.
- **Place handcrafted structures faithfully:** Preserve their actual dimensions, entrances, interiors and visual design; select and position them using a clear structure catalog, including compatible orientation and street connections.
- **Remain dependable world generation:** Account for deterministic placement, chunk boundaries and generation order, collision/overlap prevention, seed reproducibility, reasonable resource use and compatibility with surrounding terrain/features.

**Architecture is an open engineering investigation.** Compare the actual capabilities and constraints of Minecraft 26.3's existing structure/jigsaw machinery against a dedicated settlement planner or other bounded generation design. Multi-district organization may be useful *inside* a town plan, but several independently generated villages accidentally touching is not the desired model. No particular implementation technique or numerical vanilla limit should be assumed to be the final answer without inspecting the code.

## 2. Owner-built architectural library and reference villages

The owner expects to handcraft village assets in Minecraft, probably in a dedicated creative superflat reference world, and export individual buildings/tiles with **WorldEdit schematics or a comparable structure format**. The owner will provide the look and proportions of the buildings, roads, squares, farms and other connecting pieces. The intended five regional sets are plains, desert, savanna, taiga and snowy.

Each piece should be individually identifiable and accompanied by whatever **minimal metadata** the generator truly needs: for example, intended role, footprint, entrance/road connection and placement constraints. The exact labeling, connector and export conventions are part of the M16 technical investigation; the owner should not need to document block coordinates and dimensions by hand if tooling can extract them.

**Example complete villages** should communicate intended settlement scale, spacing, visual rhythm, roads and overall quality. These are design references and tests for the generator, not single giant structures to stamp into the world. A **small plains kit and example layout first** can validate the conversion, catalog and generation pipeline before the owner spends the time creating all five regional libraries.

The **owner controls building aesthetics, size and final architectural approval**; Claude handles ingestion/conversion, placement, generation and related code architecture.

## 3. What this enables downstream

- **M17:** Proper stores, workplaces and gathering places that can support purposeful jobs, trading and daily schedules.
- **M18:** Streets, public spaces, homes and working districts worth defending, with usable patrol geography for Iron Golems.
- **M19:** Observable, navigable homes, work locations and communal space for settlement adoption and villager wellbeing, including player-built settlements.

The M16 generator establishes **physical settlements and a coherent settlement-identification foundation**; the subsequent milestones give their inhabitants economic activity, protection and social needs. M19 must also support player-founded settlements, whose boundaries cannot depend solely on generated-village markers.

## 4. Questions for the technical architecture assessment

Determine the real Minecraft 26.3 and current-repository constraints, and recommend a feasible route for:

1. Planning expansive, **single coherent villages** beyond the useful footprint of vanilla's default village assembly, without disproportionately increasing ordinary chunk-generation cost.
2. Assessing terrain and reserving lots, laying out streets, and placing many templates consistently across independently generated chunks.
3. Importing and cataloging the owner's WorldEdit assets, validating footprints and connection points, and turning a plains pilot into a reliable repeatable pipeline for five biome styles.
4. Defining village identity and usable boundaries for villagers, work locations and Iron Golem protection across an expansive settlement, including how these interact with vanilla village/point-of-interest detection and later player-founded settlements.
5. Populating generated farms/pastures with appropriate animals, handling biome transitions and village population distribution, and preserving the owner's supplied architecture and placement requirements.
6. Defining a small, observable proof-of-concept and performance/regression criteria **before** the full architectural library is built.

**Lifecycle:** PLANNED - NOT READY. This brief describes the intended magnitude and purpose of the overhaul. Technical architecture, asset-handoff conventions, exact generation limits and final building designs are still to be settled with the owner.
