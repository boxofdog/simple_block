# Simple Block — Milestone 03: Freight Cart & Storage-Bypass Closure

**Status:** Implementation handoff prepared. **Owner intent:** ONE small, complete, testable capability in one Claude Code session.
**Authority:** This milestone describes the feature to build. The roadmap is reference context, **not** permission to implement anything else in it.

## 1. Purpose / core loop

M02 introduced meaningful mining deposits and a shared 64-ingot-equivalent personal carrying allowance. M03 adds the first *physical* answer: a rail-bound open-top cart that holds a much larger quantity of **one** mined resource at a time. The player must move ore *through the world*, instead of pocketing an entire operation.

M03 proves: **mine → load cart → move along existing rails → unload at a destination**. It also removes two already-rejected ways of avoiding physical freight: ender chests and vanilla chest minecarts. Pack animals stay as the slower, geographical alternative.

No full train or automatic unloading is necessary yet.

## 2. Owner-locked requirements

| ID | Requirement | Acceptance criterion |
|---|---|---|
| FRT-01 | One dedicated freight minecart entity/item | Can be obtained for testing, placed on ordinary rails, pushed, and moved along vanilla rail shapes with expected minecart behavior. The cart is physically distinct from a chest minecart (open-topped / no chest). |
| FRT-02 | Single-item cargo | A cart may hold **one item type** at a time. Attempts to add a different type to a partially filled cart are rejected safely. When completely emptied, it accepts a new type. No mixed multi-slot inventory. |
| FRT-03 | **256 items initial maximum** | A cart accepts up to **256 qualifying mined-resource items**, for example 256 raw iron. Partial stacks are supported; cargo never exceeds 256. This is the first balance target, not a final universal storage standard. |
| FRT-04 | Manual loading and unloading | Players can load from their inventory and withdraw cargo without dropping/duplicating it. Repeated transfers work with normal stack limits. The M02 personal weight limit still governs material returning **to the player's inventory**. |
| FRT-05 | Persistence | Cargo type and count survive world save/reload, chunk unload/load, entity tracking and normal server/client synchronization. No negative counts, silent loss or duplication. |
| FRT-06 | Not rideable | A player cannot mount the freight cart. Passenger cushions are a later milestone; do not introduce one now. |
| FRT-07 | Not portable bulk storage | Picking up/breaking/destroying a loaded cart must **never** grant a single inventory item that still contains 256 cargo. Resolve cargo into separate dropped stacks and an **empty** freight-cart item, with no duplication. Consider safe behavior for hazards and entity destruction. |
| FRT-08 | Simple cargo feedback | Player can ascertain cargo item and count/capacity in-game without debug commands. A small unobtrusive capacity readout is required; a visually polished in-world fill and custom Blockbench art are M04. |
| FRT-09 | Existing build and gameplay stay intact | No regressions in M01/M02, no project rescaﬀolding, no break to Fabric/Gradle CI or user's updater. Keep this change limited to the freight cart and the two agreed removals. |
| FRT-10 | **Ender chests removed from new-world gameplay** | No survival crafting/acquisition, placing, opening, storing/retrieving, hopper/automation transfer, or other normal use of ender chests. Suppress relevant loot/recipes and creative exposure where safely supported by the existing M01-style removal approach. Keep other End progression (including eyes of ender) intact. |
| FRT-11 | **Vanilla chest minecarts removed** | No survival crafting/acquisition, placement, inventory opening, hopper interactions or chest-minecart cargo transport. Existing vanilla registration may remain internally if needed for stability; actual gameplay routes must be blocked. **Keep ordinary minecarts** and introduce the new freight cart separately. |
| FRT-12 | **Pack animals preserved** | Donkey/mule/llama cargo inventory, mounting, and physical movement continue working as in vanilla, except for any already-existing mod behavior. Do not accidentally disable them with a broad storage-item ban. |

### Capacity semantics (avoid a hidden 9× bypass)

- The approved starting capacity is **256 ordinary mined-resource items**, not 1,024 and not 256 inventory slots. **Count items**, one per unit for the M03 eligible set.
- **Do not allow metal storage blocks, nuggets, containers, equipment, bundles, shulker boxes or other packed/compound inventories as cargo in M03.** Those have different material equivalences or nested storage; eligibility/volume for those forms is a later decision.
- M03 supports simple unprocessed ores / ordinary bulk mine products, not arbitrary items or a general-purpose chest replacement.

## 3. Bounded implementation defaults (provisional, not a game-design commitment)

The owner has not selected a final recipe, UI/gesture, or exhaustive cargo whitelist. Use the following narrow defaults so M03 can ship without spawning additional systems. If a default conflicts with the actual live codebase, explain the conflict and use the smallest compatible alternative; report the exact implemented behavior.

### 3a. Allowed commodity set

Use one clearly centralized eligibility rule (e.g. an item tag or small whitelist if supported cleanly by the current version). Start with these existing resource items **where they exist in the installed version**:

- Iron, copper and gold ore blocks, their deepslate variants, and raw iron/copper/gold.
- Coal and mined stone/cobblestone/cobbled deepslate.
- Optionally other plain, stackable, directly mined ore drops (e.g. lapis/diamond), if they can be added without special-case data handling; explicitly document what qualifies.

No arbitrary tools, food, storage items, filled containers, finished metal blocks, or complex stack-component content. One **exact item identity** per cart; do not merge distinct ore variants or raw items by metal family. Preserve stack component data correctly or reject non-default component-bearing stacks rather than corrupting them.

### 3b. Loading / unloading interaction

Prefer **simple in-world interactions**, not a 27-slot chest-style GUI:

1. Interact while holding eligible cargo: insert as many from the held stack as fit; respect 256 cap and existing cargo identity. Never consume more from the player than inserted.
2. Interact with empty hand: withdraw up to one normal stack into the player's allowed inventory space, respecting **M02's heavy-material allowance** and existing container/slot rules. If no cargo can be accepted, leave it in the cart.
3. Provide an intelligible cargo readout (e.g. brief status text `Raw Iron — 180/256`) on interaction, including full / incompatible-cargo feedback. If a lightweight progress bar is safe and quick, add it, but do not invent a large UI subsystem merely for this. The bar's artistic treatment belongs to M04.
4. If vanilla interactions conflict with these gestures, adapt to a minimal consistent gesture and report how players use it.

**Important:** Loading should be possible using materials already in hand. Unloading must never force an entire 64-stack into a player who has space or weight for only a few. Overflow remains in the cart; failure is transactional, not lossy.

### 3c. Item/crafting availability

Make the cart available in dev/creative commands and normal survival gameplay. For initial survival crafting, **a simple provisional recipe using the existing vanilla minecart and a modest amount of existing material** is acceptable. Put the recipe in ordinary project data files; document that recipe as *provisional* in the M03 report. **Do not add new metals, custom components, smithing stations or elaborate manufacture to justify it.** If the repository already contains an owner-approved recipe, use that instead.

### 3d. Visual placeholder

The cart must look like an **open-top cart without a chest sticking out of it**, even if its appearance is currently a basic/vanilla-style placeholder. **Do not** build the finished visibly heaped ore contents, special cargo shaders, block textures, or Blockbench asset pipeline now. M04 owns those. Preserve any existing hand-authored art unchanged.

## 4. In-scope storage-bypass closure (small, required part of M03)

**Ender chests:** Disable the ender-chest block/item's gameplay availability and use in new worlds. Use the existing M01 shulker-box restriction patterns where appropriate, but verify ender-chest-specific crafting, acquisition, placement, interaction, item drops, and external access routes. A recipe-only ban is **not** enough if players can find/place/use one anyway. Do not delete vanilla registrations that other parts of the engine need; keep compatibility stubs if necessary, but block the exploit in ordinary play. No legacy save migration required.

**Vanilla chest minecarts:** Disable recipes, obtaining, placement, usable entity inventory, and hopper-assisted storage/transport in ordinary gameplay. **Do not** remove the normal minecart item/entity or vanilla track mechanics. Minecraft internals may retain the chest minecart type when essential for runtime stability, but it must not provide player-usable cargo transport. Avoid loose cargo spawning/duplication from blocked entities. Keep the new freight cart as the legitimate bulk-hauling alternative.

**Pack animals:** Keep donkey, mule, and llama carried storage in place; being slow and requiring physical travel is their accepted logistical tradeoff. Include a regression playtest.

**Removal regression principle:** Once these restrictions ship, a player must not be able to move 256 items instantly between a mine and home using an ender chest, or use a vanilla chest minecart as a substitute for the new freight system. Test representative crafting/loot/creative/automation paths without growing M03 into a sweeping all-storage ban.

## 5. Explicitly out of scope
- Coupling, train AI, locomotive/furnace-minecart replacement, push/pull trains, passenger cushion, faster rail physics.
- Copper cable anchors, copper rods, overhead winches/cables, pulley systems, vertical mining skips, dumping rails, automatic loading/unloading, hoppers/factories.
- New ore processing chains, materials, cave physics, redstone replacement, new village systems.
- Custom rendered cargo fill. M04 adds it; M03 needs only a correct cargo state and minimal player feedback.
- Cross-version migration or preservation of existing saves; this project targets new worlds.

## 6. Engineering guidance and scope protection

1. **Audit before editing:** versions, mappings, entity registration/rendering, minecart mechanics, player weight validation, tests, CI/updater, Git status. Run baseline tests/build.
2. **Choose the smallest stable Minecraft 26.3 Fabric implementation.** Follow current minecart/version APIs, avoid legacy tutorials or guessed Yarn names. Entity state changes and interaction validation must be server-authoritative; client-only visuals cannot author cargo changes.
3. **Do not bypass the M02 carrying allowance.** Withdrawal paths, shift actions if used, inventories and entity destruction must never secretly place excess heavy resources directly in personal storage. World-item drops on destruction are fine; they remain in the world and are subject to normal pickup constraints.
4. **Duplication safety matters more than polish.** Treat cargo moves and entity destruction transactionally. Ensure break/removal only pays out once. Avoid trusting client-provided item counts or player inventory snapshots.
5. **Use no unloaded chunk search or per-tick scanning for cargo.** Save only the amount of state needed: precise item identifier/components and quantity; keep in-sync appropriately.
6. **Avoid irreversible user-art changes.** If M03 uses an existing minecart model, document how M04 can replace its visuals without rewriting storage logic.
7. **No silent scope expansion:** if the cart depends on a bigger minecart-physics rewrite or an unsolved API issue, implement the narrowest demonstrable core, document the blocker, and ask the owner rather than beginning a second milestone.
8. Work against current `main` if that's the repository convention. Check for uncommitted changes and never force-push or discard work. Keep the existing updater/release artifact workflow intact.

## 7. Finish line

M03 is complete when a user can **place an empty freight cart on rails, load up to 256 of one eligible mined resource, move it physically along rails, withdraw it in safe quantities, reload the world and retain the cargo, empty it and use it for a second commodity, and break a loaded cart without duplicating or pocketing its contents**; **ender chests and vanilla chest minecarts cannot be used to bypass this physical hauling loop**; and **pack animals remain available**. It must build, pass dedicated tests, work in a real Minecraft client, and leave prior milestones working.

Produce `docs/MILESTONE_03_REPORT.md` documenting exact recipe, accepted cargo list, interaction gesture, changes/files, test PASS/FAIL/NOT RUN, commands, actual screenshots/evidence, limitations and any deviations from these defaults. Do **not** claim successful in-client tests if they were not run. After delivering a verified build and summary, **stop**. Do not start M04 automatically.
