# Simple Block — Milestone 06: Loading Track and Freight-Cart Handling (DRAFT)

**Status: Design scaffold — NOT authorized for implementation.** This is the owner's initial M06 direction and is intentionally incomplete. **M05 is still in progress**; finish, commit, verify and review M05 before starting M06. Refine this document together rather than asking Claude to execute it as-is.

## Purpose

Build the first physical **freight-transfer railway interface** for the future mine → skip → railway → smelter loop. Preserve the ordinary vanilla minecart as Simple Block's freight cart. The special loading track *is* the future transfer station: no compulsory secondary loading/unloading station block or item-conveyor system.

## 1. Loading track (owner-specified direction)

- Introduce a **loading track**: a rail section that **locks a freight minecart in position** for cargo handling, rather than letting it drift or be shoved away while a transfer is attempted.
- The **loading track itself determines the orientation of the freight cart's front**. The cart's approach direction does **not** choose which end opens. It locks in place and selects a cargo-transfer state from its **relative position to the skip**:
  - **Skip below the track (cart → skip):** when the space in front of the oriented cart/track is clear for deployment, the cart's **front panel unfolds into a downward ramp** that directs bulk ore into the skip's open upper compartment. This is the **loading-the-skip** state. Retract the ramp when this mode ends or the cart is released.
  - **Skip beside the track (skip → cart):** the cart's front **stays closed**. The aligned skip's built-in lower chute and automatic discharge gate feed ore into the stationary cart. The track/cart **accepts freight without opening its panel**.
  - Do not open the cart or discharge items merely because an empty space exists in front of the rail. The mode requires a compatible positional relationship with the skip. A blocked ramp must not clip through other blocks; do not lose or duplicate cargo.
- Keep the **same vanilla minecart and its 256-item, single-type inventory**; these are state-dependent animations/handling rules, not a new freight-cart item/entity.
- **Shapeless crafting recipe:** **one vanilla rail + one existing copper cable anchor → Loading Track**. They can occupy **any two crafting-grid positions**; the number of loading-track items produced per craft is **not yet specified**. Reuse the anchor item as an ingredient; do not create a new anchor variant.
- This rail is the intended physical interface for **cart ↔ upright skip** transfers and, later, mine-to-smelter logistics. The cart's ramp into a skip **below** and the skip's built-in chute into a cart **beside** are distinct modes of the *same rail*, not distinct station machines. The full skip and bulk-smelter implementations remain outside initial M06 scope unless subsequently authorized. **No separate compulsory unloading station or conveyor belt.**
- **SETTLED:** one rail and one copper anchor in a shapeless recipe; the rail orients the cart front; cart-to-skip-below deploys a downward ramp if unobstructed; skip-beside-to-cart keeps the cart closed; the rail itself defines the transfer station.
- **NOT YET SETTLED:** recipe output count; exact rail facing/configuration gesture and in-world model; lock/release controls; connection to an active train; transfer triggers/timing and validation; compatibility with future visible cargo fill, rod and cushioning; exact world clearance and relative-position tolerances. Keep the ramp/cargo transfer safe and explicit rather than assuming opening means instant lossless unloading.

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

**Next design question:** How does the player choose the loading track's **facing** when placing it, and how does a freight cart **unlock/depart** after its transfer? The cart's front direction is now fixed by track orientation.
