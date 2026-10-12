# Simple Block — Milestone 06: Freight Handling and Coal-Powered Winch (DRAFT)

**Status: Design scaffold — NOT authorized for implementation.** This is the owner's initial M06 direction and is intentionally incomplete. **M05 is still in progress**; finish, commit, verify and review M05 before starting M06. Refine this document together rather than asking Claude to execute it as-is.

## Purpose

Build the first physical **freight-transfer railway interface** and add a **coal-powered upgrade to the existing manual winch**, preparing the mine → skip → railway → smelter loop. Preserve the ordinary vanilla minecart as Simple Block's freight cart. The special loading track *is* the future transfer station: no compulsory secondary loading/unloading station block or item-conveyor system. The powered winch must improve the SAME M05 cable system, **not introduce a parallel machine network**. Actual upright mining skips and their transfer/hoisting gameplay are planned for **M07**; the large smelter is **M08** and the full armor overhaul **M09**, all requiring later authorization.

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

## 4. Coal-powered winch upgrade (M06 design requirement; mechanical details OPEN)

- **Owner has added the coal-powered winch to M06's intended scope.** The existing hand-operated M05 winch/drum gains a **localized coal-fueled drive**, usable for the same cable-hauling system and later M07 upright skip hoisting. This is **not** a request for a new power-grid, generic rotary-shaft network or unrelated factory tech tree.
- **Do not yet choose the construction mechanic.** The owner wants to discuss whether fuel power is applied directly to the existing drum, how its engine attachment is represented/crafted, or whether another compact localized mechanism makes more sense. Do not automatically assume a furnace item attachment merely because that is how the M05 Cobblestone Engine is made.
- **Preserve the M05 hand crank and cable-direction semantics:** empty hand, hold **left-click = feed/away** and **right-click = pull/toward** while using the crank. A closed cable loop still retains its installed length; powered reversal moves the strand, it does not wind extra loop length. Decide how powered activation/reversal and manual fallback coexist without conflicting click actions.
- **Still to design with owner:** attachment/recipe and visual identity; exact eligible fuel (coal vs coal blocks vs other fuels), loading and burn timing; which gesture/controls start, stop, reverse and optionally adjust motor speed; whether powered operation runs continuously or only while held; safe stopping/holding under load and blocked/colliding cable routes; comparative hand-vs-motor power/fuel/speed; break/retrieval/save-load behavior and shared use with M07 skip. These are **questions, not resolved specifications**.
- **M06 acceptance when authorized:** confirm the upgrade genuinely drives existing winch cable hauling without breaking manual operation, stops/reverses predictably and preserves cargo/fuel state, works on a dedicated server and in a real client, and has no unrelated machine-system dependencies. Test M07-ready vertical hoist attachment interfaces only as needed for future compatibility; **do not implement the skip itself in M06**.

## 5. M06 boundaries and work still to scope

**Excluded from M06 unless separately approved:** the actual M07 skip entity/doors/automatic docking/vertical-motion gameplay; M08 bulk smelter; M09 armor/workwear overhaul; full freight-transfer automation beyond the agreed track/cart interface; extreme-grade custom rails; general redstone redesign; complete station signalling; and redesign of the M05 Cobblestone Engine/coupling. **Coal-powered winch attachment is now IN M06's intended scope**, but its mechanism must be settled before Claude can implement it.

**Potential adjacent work, NOT included yet:** visible cargo level filling in steps like a cauldron/composter with new owner-paintable texture (already specified in ROADMAP/DECISIONS), UX text cleanup, gravel/sand cargo eligibility. These remain separate unless the owner explicitly includes them.

**Expected implementation discipline after the brief is finalized:** preserve editable Blockbench sources and painted owner assets; perform baseline checks against finished M05; test rail locking/opening/release, cart push prohibition vs legitimate train movement, powered AND manual winch movement, safe stopping/reversing, fuel and cable conservation, multiplayer/save-load behavior, structure/recipe consequences of rail removal, and a real-client demonstration. Document implementation SHA, CI and owner acceptance through the established roadmap handoff. Stop after M06.

**Next design question (priority):** How exactly should the **coal-powered winch upgrade** be built and controlled on the existing drum? Settle that with the owner before writing an executable M06 brief. Loading-track facing, cart unlock/depart and cargo transfer triggers also remain unresolved.
