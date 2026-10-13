# Future work and unnumbered design packages

**Unnumbered proposals are not milestone statuses.** Unassigned design ideas in this file have no implementation authorization. Apply the five-state [milestone status key](STATUS_KEY.md) only when a proposal becomes a numbered milestone. Milestone-specific work belongs in the linked milestone handoffs, not here. This file holds **unassigned or unnumbered** design directions moved out of ROADMAP.md. Confirm scope and acceptance criteria in a dedicated brief before starting one. [BACKLOG.md](BACKLOG.md) contains additional speculative ideas and is never blanket authorization.

### Planned visual follow-up — 3D ordinary rails (NOT implementation-authorized)

**Owner request (2026-10-13):** Replace the flat appearance of **ordinary vanilla rails** with 3D rails/sleepers whose railhead height, gauge, thickness and joint profile match the Loading Rail, so regular rails are **flush and visually seamless** against it. Support straight, curved and ascending rail variants and all adjacent orientations; preserve ordinary rail gameplay, placement, cart physics, recipes, item identity and drops. Provide editable Blockbench sources, paintable UV/texture templates and real-client checks. **Roadmap only for now, separate from the authorized M06 corrective pass and M07; decide implementation scheduling later.**

### Future spelunker's rope (owner-approved direction; not authorized)

- **Rope shaped recipe:** three string items vertically in the center crafting column. Result quantity remains unspecified; do not assume a multiple-item yield.

  ```text
  [empty] [string] [empty]
  [empty] [string] [empty]
  [empty] [string] [empty]
  ```

- **Spelunker's Rope shaped recipe:** one existing **copper cable anchor** above one rope in the center column (reuses the current anchor item, not a new kind of anchor).

  ```text
  [empty] [empty]         [empty]
  [empty] [copper anchor] [empty]
  [empty] [rope]          [empty]
  ```

- Place the Spelunker's Rope on the **bottom or side face of a block**. Its anchor fixes at that block; the climbing rope **hangs vertically downward through open air** without needing intermediate attachments, support posts or guides.
- **Hold right-click with Rope in hand** to extend the anchored line downward in **12-block increments**, using the supplied rope resource for each increment. Once it reaches solid ground, any extra length **coils visibly into a bundle on the lowest block** rather than extending through the terrain. Details of quantity spent, coil retrieval and support removal are for the future rope milestone; avoid silently providing unlimited free rope.
- This is a **temporary spelunking/access tool**, not a moving mechanical winch cable. Preserve M04/M05 copper cable, anchors and hauling logic; climbing and retrieval controls and the speed of ordinary ladders remain separately tunable.

## Post-M10 — Witch's Tome and discovered enchanting (future concept; not authorized)

Place witch huts on remote mountain peaks and in deep deepslate caverns. Each features an enchanting table; breaking the table returns a diamond plus a **Witch's Tome**. A Witch's Tome is required to craft a replacement enchanting table, making enchanting knowledge something discovered through exploration rather than unlocked from ordinary mined materials alone. Final table recipe, hut generation density, repeat acquisition and loot/drop safeguards remain to be designed. Keep this beyond M10, alongside the separate post-M10 vanilla combat armor concepts. No implementation authorization implied. See [BACKLOG.md](BACKLOG.md) I-012.

## After M10 — existing combat armor gains full-set identities (owner-settled design; not authorized)

This is a **separate, unnumbered post-M10 workstream**, not additional M10 scope. Keep the original vanilla **iron, diamond and netherite armor items**; each of the following activates **only with all four pieces of its matching set**. M10's workwear bonuses **Trail Blazer** and **Old Pro** remain separate, and gold/chainmail retirement remains an M10 task.

| Complete vanilla armor set | Named bonus | Owner-settled behavior |
|---|---|---|
| **Iron** | **Cast Iron Crusader** | No knockback from zombie attacks or arrows; after hitting a mob, show its **remaining hitpoints**. |
| **Diamond** | **Iridescent Infantry** | Reflect incoming arrows back with **reduced travel distance but full damage/strength**; a zombie biting the wearer **breaks its teeth**, damaging it and **reducing its subsequent attack damage**. |
| **Netherite** | **Netherfire Knight** | **Piglins never fight back** against the wearer and offer **more favorable trade/barter outcomes**; any hostile mob that hits the wearer **ignites**. |

**Implementation timing:** settle numerical reductions, durations, reflected-arrow return/range, teeth-broken state behavior, UI feedback, piglin barter odds and exact triggering on a future implementation brief. Do **not** improvise extra systems, equipment tiers or automation. The settled design details above remain here until a dedicated post-M10 milestone is created. **Current milestone status is in [ROADMAP.md](ROADMAP.md): M04–M06 SHIPPED; M07 PLAYTESTING.**

## Other unnumbered workstreams and playtest follow-ups

- **Villages:** terrain suitability, larger buildings, population and destinations, pubs, centralized stores, workshop services, real golem protection.
- **Character and equipment:** retire gold/chain armor from normal gameplay/creative; define copper headlamp, low-protection backpack overalls covering chest/legs, and swappable spiked/mining utility boots (details below). Add climbable ropes and tune baseline ladders.
- **Geology/metallurgy:** bulk smelting and hopper-fed ore handling, alluvial iron sand/gravel, steel, useful long-lived anvil/smithing.
- **Rules and usability:** selective redstone-system redesign, guidebook, stepped freight-cart cargo visuals and player-facing text polish.

- **Coupled consist re-docking at a single side-unload station:** M07 playtesting decision, not a silently authorized M06 release-rule change. See [MILESTONE_07_REPORT.md §12](MILESTONE_07_REPORT.md).
- **Further train coupling/operation:** defer a redesign until its behavior is settled separately.
- **Extreme-grade rails and downhill crashes:** three-block inclines and crash consequences remain unnumbered design ideas; do not roll them into M07 or M08 by default.
- **Faster vanilla ladders:** still open independently of Spelunker's Rope.
- **Full mine-to-smelter logistics playtest:** a future integrated user test, not another system to implement automatically.

**Historical planning:** The old `After M05` staging table is preserved in the [roadmap archive](archive/ROADMAP_PRE_CONSOLIDATION_2026-10-13.md) because its M06/M07 'not started' claims are obsolete. This file carries forward only still-future work.
