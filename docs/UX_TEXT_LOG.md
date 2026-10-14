# Removed player-facing text — UX log

**M11 owner decision (2026-10-14): no mod-authored floating labels or action-bar narration anywhere.** M11 removed those messages; ordinary vanilla notices, GUI labels and inventory tooltips remain. This file is the **single register** of deleted wording and places where silent refusal/inspection needs a physical response. It is **not authorization** to add new feedback.

The world should communicate normal actions through cargo fills, gates, cable movement and machine states. An invalid action may still need a restrained **sound, animation or model cue**. `feature/Feedback` retains the outcome server-side without displaying it. Below, **Yes** means meaningful feedback is absent, **Maybe** means visibility is partial, **No** means the machine already shows it. The owner chooses replacements in a separate authorized pass.

## Find a message

| ID | Area / original observation | Details |
|---|---|---|
| UX-001 | Cable put-down | [see UX-014](#ux-014) |
| UX-002 | Cable pickup guidance | [see UX-013](#ux-013) |
| UX-003 | Rod fitted | [see UX-012](#ux-012) |
| UX-004 | Cargo hover label | [see UX-010](#ux-010) |
| UX-005 | Cable-loop confirmation | [see UX-015](#ux-015) |
| UX-006 | Open-loop warning | [see UX-016](#ux-016) |
| UX-007 | Loop routing refusals | [see UX-015](#ux-015) |
| UX-008 | Engine and coupling refusals | [see UX-019](#ux-019) |
| UX-009 | Cargo click/status overlay | [see UX-010](#ux-010) |
| UX-010 | Freight cart | [Removed wording](#ux-010) |
| UX-011 | Cushions | [Removed wording](#ux-011) |
| UX-012 | Connecting rod | [Removed wording](#ux-012) |
| UX-013 | Cable hook | [Removed wording](#ux-013) |
| UX-014 | Routing and drums | [Removed wording](#ux-014) |
| UX-015 | Loops | [Removed wording](#ux-015) |
| UX-016 | Crank and hoist | [Removed wording](#ux-016) |
| UX-017 | Coal winch | [Removed wording](#ux-017) |
| UX-018 | Second drum | [Removed wording](#ux-018) |
| UX-019 | Engine and couplings | [Removed wording](#ux-019) |
| UX-020 | Loading Rail | [Removed wording](#ux-020) |
| UX-021 | Skip | [Removed wording](#ux-021) |
| UX-022 | Blast furnace | [Removed wording](#ux-022) |
| UX-023 | Rope | [Removed wording](#ux-023) |
| UX-024 | Retired storage | [Removed wording](#ux-024) |

## Earlier playtest sightings — UX-001–009

These were recorded **before** the complete M11 removal. Kept for historical exact wording, but their current feedback questions belong to UX-010–024 linked above; do not open duplicate replacement tasks.

### UX-001

**Area:** Cable put-down

**Original observation:** `Cable end put down`

**Original note:** Routine action narration can likely go

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-014](#ux-014) for replacement decisions.

### UX-002

**Area:** Cable pickup guidance

**Original observation:** `Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum...)`

**Original note:** Replace verbose instructions with restrained feedback/inspection

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-013](#ux-013) for replacement decisions.

### UX-003

**Area:** Rod fitted

**Original observation:** `Connecting rod fitted; it grips a cable the cart passes under`

**Original note:** Let visible rod communicate purpose; optional short confirmation

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-012](#ux-012) for replacement decisions.

### UX-004

**Area:** Cargo hover label

**Original observation:** Floating freight-cart cargo label, e.g. `Empty 0 / 256` or `Iron 128 / 256` when looking

**Original note:** **M11 REQUIRED:** remove auto-generated cargo/status name tags even when crosshair-targeted; render actual item-form-aware progressive fill levels instead. Keep vanilla owner-named cart names, without appended counts. No numeric hover substitute.

**Outcome:** **Removed in M11** (load drawn in the cart instead). Follow [UX-010](#ux-010) for replacement decisions.

### UX-005

**Area:** Cable-loop confirmation

**Original observation:** M05 action bar: `Loop closed: 28 blocks of return cable` / `Loop opened: the return cable is back on the drum`

**Original note:** Confirmation of a costly action; could shrink once the return strand and wheel are obviously visible

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-015](#ux-015) for replacement decisions.

### UX-006

**Area:** Open-loop warning

**Original observation:** M05 action bar, crank on an open route: `Close the loop to haul`

**Original note:** Kept short on purpose (brief: discoverable without lengthy text); review after playtest

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-016](#ux-016) for replacement decisions.

### UX-007

**Area:** Loop routing refusals

**Original observation:** M05 refusals: `A loop needs a straight route: the cable turns at x, y, z`, `The return cable would run through Stone at x, y, z`, `Not enough cable for the return: it needs 26 blocks, the drum has 9`

**Original note:** Refusal feedback; the coordinates may be more than a player needs

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-015](#ux-015) for replacement decisions.

### UX-008

**Area:** Engine and coupling refusals

**Original observation:** M05 engine and couplings: `Needs coal`, `The firebox is full`, `Empty the cart first`, `Too far apart to couple`, `Both ends of this cart are coupled`

**Original note:** Short refusals; probably fine; M11 removes **MinecartCargo-generated** click narration, not all train/coupling feedback

**Outcome:** Removed in M11 (owner, no floating text); listed again below. Follow [UX-019](#ux-019) for replacement decisions.

### UX-009

**Area:** Cargo click/status overlay

**Original observation:** Minecart click/status overlays: `MinecartCargo.interact` sends `status()`, `Full`, `No room`, `Too heavy`, `This cart carries...` and load/unload messages

**Original note:** **M11 REQUIRED:** remove freight-cart-originated status/flavor/actionbar text from all click cases, not only from empty-hand inspection. Gameplay actions and real safeguards remain; no replacement counters, GUI or hover prompts.

**Outcome:** **Removed in M11** (code deleted; see UX-010). Follow [UX-010](#ux-010) for replacement decisions.

## M11 removed messages — UX-010–024

Each entry preserves the original text templates. Placeholders such as `<item>`, `<n>` and `<pos>` are variable values, not proposed player-facing replacement text. All listed messages were removed in M11.

### UX-010

**Area:** Freight cart (`feature/MinecartCargo`, deleted)

**Removed text:** look-at label `<item> <n> / 256`, `Empty 0 / 256`; click: `Full: <status>`, `<item> isn't cargo: ores, raw metals, coal, gems and stone only`, `This cart carries <item>: empty it before loading <item>`, `Too heavy: you can't carry any more <item>`, `No room in your inventory`

**Replacement assessment:** Replace? **No** for amounts (the load shows); **Maybe** for refusals (wrong kind, full, too heavy): nothing happens visibly now, a soft refusal sound could do

### UX-011

**Area:** Cushions (`feature/MinecartCushions`)

**Removed text:** `Unload the cart before fitting a cushion`, `Cushion fitted: this cart carries a rider now, not cargo`, `Someone is sitting on the cushion`, `Cushion taken out: this cart carries cargo again`

**Replacement assessment:** **No** for fitted/taken out (the cushion shows); **Yes** for "unload first" and "someone is sitting" (silent refusals)

### UX-012

**Area:** Connecting rod (`cable/ConnectingRods`)

**Removed text:** `Connecting rod fitted: it grips a cable the cart passes under`, `Connecting rod taken off`, `This cart already has a connecting rod`

**Replacement assessment:** **No** (the rod shows; a second rod just stays in hand)

### UX-013

**Area:** Cable hook (`cable/CableHook`)

**Removed text:** `Cable end picked up: use an anchor to run the cable through it`, `You are already holding a cable end`, `No cable on the drum: use a cable coil on it`

**Replacement assessment:** **Maybe**: an empty drum giving nothing is a silent refusal

### UX-014

**Area:** Routing and drums (`cable/CableRoutes`)

**Removed text:** `Cable fitted: run it through as many anchors as you like`, `The drum already has its cable: one coil reaches as far as you route it`, `Cable end put down`, `Empty cable drum: use a cable coil on it`, `Cable drum: <n> blocks laid out through <n> anchors`, `Cable loop: <n> blocks through <n> anchors`, `Cable to the drum at <pos>: <n> blocks through <n> anchors, worked from both ends`, `Cable wound back in from <n> anchors`, `Cable coil taken off the drum`, `No cable runs through this anchor`, `Cable disconnected here: wound back from <n> anchors`, `This anchor carries the cable from the drum at <pos>`, `Free anchor: pick up a drum's cable end, then use the anchor to run the cable through it`, `The cable already runs through this anchor`, `This anchor already carries a cable`, `Too far: a span can be at most <n> blocks, this one would be <n>`, `The cable would run through <block> at <pos>`, `Cable run through the anchor: a <n>-block span, <n> blocks of route`

**Replacement assessment:** **Yes** for the refusals (too far, blocked, anchor taken, already routed): the end just stays in hand. **No** for the narration. Drum/anchor inspection (lengths, links) has no replacement

### UX-015

**Area:** Loops (`cable/CableLoops`)

**Removed text:** `Loop closed`, `Loop opened: the return cable is back on the drum`, `This route is already a loop`, `A skip hangs on this cable: take it off before closing a loop`, `A loop needs a straight route: the cable turns at <pos>`, `The return cable would run through <block> at <pos>`, `Cable taken off the second drum: the end is back at the last support`

**Replacement assessment:** **Yes** for the refusals (not straight, blocked, hoisting); **No** for closed/opened (the return strand shows)

### UX-016

**Area:** Crank and hoist (`cable/CableDrive`, `cable/CableHoists`)

**Removed text:** `Close the loop, or hang a skip on the cable end, to haul`, `This cable end has lost its drum`, `Run the cable through an anchor over the shaft first: the skip hangs under the last one`, `The skip must stand straight under the cable's last anchor, at <pos>`, `The skip must hang below the anchor`, `This cable already carries a skip`, `The cable would run through <block> at <pos>`, `Skip hung on the cable: pull to lift it, feed to lower it`

**Replacement assessment:** **Yes**: a crank that won't turn and a skip that won't hang are now silent; hung is visible (bridle)

### UX-017

**Area:** Coal winch (`cable/CoalWinch`)

**Removed text:** `Coal winch: put coal in, then right-click the lever to pull, left-click to feed`, `Lever: <state>`, `Coal: <n>`, `The winch holds <n> coal`, `Coal winch: <n> coal, <n> s alight, lever <state>`, `Close the loop, or hang a skip on the cable end, to haul`

**Replacement assessment:** **Yes** for the coal count and burn time (inspection) and for "holds <n> coal" (full); **No** for the lever (it moves)

### UX-018

**Area:** Second drum (`cable/FarDrums`)

**Removed text:** `Cable made fast: crank from either drum`, `Cable made fast: work the winch's lever from either drum`, `Second drum of the cable from <pos>: crank from either end`, `Second drum of the coal winch at <pos>: lever <state>; coal at the winch: <n>`, `This route is already a loop`, `A skip hangs on this cable: take it off first`, `The second drum must be empty: no cable and no furnace`, `Drum to drum, the cable must run straight`, `That span would be <n> blocks: at most <n>`, `The cable would run through <block> at <pos>`, `Coal goes in the coal winch at <pos>`, `This drum holds the end of the cable from <pos>: it needs no furnace`, `... it needs no coil`

**Replacement assessment:** **Yes** for the refusals and for "coal goes at the home end"

### UX-019

**Area:** Engine and couplings (`train/CobblestoneEngine`, `train/Couplings`)

**Removed text:** `Empty the cart first`, `The firebox is full`, `Held at a Loading Rail: use the rail to release it`, `Needs coal`, `Both ends of this cart are coupled`, `Too far apart to couple`

**Replacement assessment:** **Yes**: all are refusals; "held at a Loading Rail" and "needs coal" matter most

### UX-020

**Area:** Loading Rail (`train/LoadingTracks`)

**Removed text:** `No train is held here`, `Released: start the engine or the winch to move off`

**Replacement assessment:** **No** for released (the stop folds, a sound plays); **Maybe** for "no train"

### UX-021

**Area:** Skip (`skip/OreSkip`, `skip/SkipCargo`, `skip/SkipMenu`, `skip/SkipItem`)

**Removed text:** `<item> <n> / 512` and `Empty` after loading by hand, `Full: <status>`, `<item> isn't cargo: ores, raw metals, coal, gems and stone only`, `This skip carries <item>: empty it before loading <item>`, `Too heavy: you can't carry any more <item>`, `This skip already hangs on a cable`, `The cable would run through <block> at <pos>`, `Lower the skip onto something before taking it off the cable`, `Skip taken off the cable`, `No room: a skip needs four blocks of height, and a clear block on each side`

**Replacement assessment:** **Yes** for the refusals (other cargo, full, too heavy, no room to place, can't unhang); amounts are in the skip's screen

### UX-022

**Area:** Blast furnace (`metallurgy/BlastFurnaceMenu`, `metallurgy/BlastFurnaceItem`)

**Removed text:** `The furnace smelts raw iron, copper or gold`, `The furnace holds <ore>: it takes no other ore until that is used or taken out`, `The furnace is full: <n> / <n>`, `Too heavy: you can't carry any more <item>`, `The furnace needs a clear space 3 blocks wide, 3 deep and 4 high`

**Replacement assessment:** **Yes**: all refusals (the screen shows contents, not why a click did nothing)

### UX-023

**Area:** Rope (`rope/*`)

**Removed text:** `Rope hung: <n> blocks down (Rope lets it down 12 more)`, `Rope let down <n> blocks: <n> in all`, `Rope taken down`, `Fix it under a block or on its side`, `No room to hang a rope here`, `The rope already reaches the ground`

**Replacement assessment:** **Maybe** for the placement refusals; **No** for hung/lowered/taken down (the rope shows)

### UX-024

**Area:** Retired storage (`feature/EnderChests`, `feature/ShulkerBoxes`, `feature/ChestMinecarts`)

**Removed text:** `Ender chests don't work in this world`, `Shulker boxes don't work in this world`, `Chest minecarts don't work in this world: load a minecart instead`

**Replacement assessment:** **Yes**: the item simply does nothing now

When playtesting reveals a *new* intrusive message or silent refusal, add its exact text (if any), source and evidence under a new ID. Do not reintroduce automatic action-bar narration to compensate for missing physical feedback.
