# Simple Block — Milestone 06: Loading Track and Freight-Cart Handling (DRAFT)

**Status: Design scaffold — NOT authorized for implementation.** This is the owner's initial M06 direction and is intentionally incomplete. **M05 is still in progress**; finish, commit, verify and review M05 before starting M06. Refine this document together rather than asking Claude to execute it as-is.

## Purpose

Build the first physical **freight-transfer railway interface** for the future mine → skip → railway → smelter loop. Preserve the ordinary vanilla minecart as Simple Block's freight cart. The special loading track *is* the future transfer station: no compulsory secondary loading/unloading station block or item-conveyor system.

## 1. Loading track (owner-specified direction)

- Introduce a **loading track**: a rail section that **locks a freight minecart in position** for cargo handling, rather than letting it drift or be shoved away while a transfer is attempted.
- While properly locked onto this rail, the **front of a non-cushioned minecart opens**, visibly giving access to the cart's cargo. The front should shut again when the cart leaves/unlocks. Keep the existing minecart and its inventory (256 of one supported bulk material), not a new minecart item/entity.
- This rail is the intended future interface for **cart ↔ upright skip** transfers and for mine-to-smelter loading/unloading. The adjacent skip's top opening or lower discharge chute determines the transfer direction, as already scoped in ROADMAP/DECISIONS. **Do not preemptively invent a station machine, conveyor belt, or item teleportation.**
- **NOT YET SETTLED:** loading-track recipe/model, whether it handles both directions immediately, how the cart's *front* is determined on symmetric vanilla minecarts (approach direction, placed orientation, track state or other), exact locking/release gesture, collision when in a coupled train, compatibility with freight fill visuals and connecting rods, transfer range, receiver-required behavior and automatic vs deliberate transfer triggers. Do not infer that opening the front instantly dumps cargo.

## 2. Retire redstone rails (owner-specified direction)

- Remove **redstone-dependent rail variants specifically**, provisionally meaning **powered rails, detector rails and activator rails**. **Ordinary vanilla rails remain**; do not remove useful non-rail redstone-adjacent blocks as collateral.
- No redstone-based rail propulsion/control should be necessary: Cobblestone Engines and cable winches provide motive power, and the loading track provides physical stopping/loading infrastructure.
- Before implementation, audit recipes, loot/structure generation, item acquisition, placed-block behavior, existing M05 train tests, rail switches and cart variants to decide precisely how each redstone rail is retired (availability, behavior and compatibility). **The three-variant interpretation and removal mechanics should be confirmed by the owner before code work.**
- Do not silently replace redstone rail signals with an elaborate mechanical/electrical automation network.

## 3. Hand-pushing freight carts (owner-specified direction)

- **A minecart that cannot carry a passenger is not manually pushable by a player.** This includes the ordinary uncushioned freight cart: player-body contact/shoving should not impart cart motion. Do not substitute another hidden hand-push gesture.
- **Cushioned passenger carts remain manually movable** (retain practical player interaction). Freight carts must still move under **Cobblestone Engine traction, coupling, cable haulage and gravity** as designed; this is not an instruction to immobilize freight or block ordinary world physics.
- Clarify whether the powered Cobblestone Engine is subject to exactly the same no-player-push rule, and ensure protections do not break M05 movement, loading into tracks, or train physics.

## 4. M06 boundaries and work still to scope

**Do not fold the following into the initial scaffold without further owner decisions:** actual skip entity/hoisting/door logic; bulk smelter; coal-powered winch attachment; full cargo-transfer automation; extreme-grade custom rails; general redstone redesign; complete station signalling; overhaul of M05's coupling or locomotive controls. A future M06 draft can add deliberate, independent work packages once the owner approves their requirements.

**Potential adjacent work, NOT included yet:** visible cargo level filling in steps like a cauldron/composter with new owner-paintable texture (already specified in ROADMAP/DECISIONS), UX text cleanup, gravel/sand cargo eligibility. These remain separate unless the owner explicitly includes them.

**Expected implementation discipline after the brief is finalized:** preserve editable Blockbench sources and painted owner assets; perform baseline checks against finished M05; test rail locking/opening/release, cart push prohibition vs legitimate train movement, cargo conservation, multiplayer/save/load behavior, structure/recipe consequences of rail removal, and a real-client demonstration. Document implementation SHA, CI and owner acceptance through the established roadmap handoff. Stop after M06.

**Next design question:** How does the loading track define the freight cart's *front* and allow it to be released after loading/unloading?
