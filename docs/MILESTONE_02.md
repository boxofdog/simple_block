---
milestone: M02
status: SHIPPED
---

# M02 — Geology, survival pressure and personal inventory (historical handoff)

**Lifecycle: SHIPPED.** This restores a **compact owner decision and scope reference** because the original M02 handoff was removed after delivery. It does not reopen completed work or supersede code as shipped. The full implementation, measurements, final tuning and test evidence are in [MILESTONE_02_REPORT.md](archive/milestone-reports/MILESTONE_02_REPORT.md). Old [DECISIONS.md](archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md) is archive-only.

## Scope and owner decisions



| Area | Current decision |
|---|---|
| Geology | Remove naturally occurring granite, diorite, andesite and tuff families where specified; retain stone/deepslate transitions and preserve useful world generation. |
| Mining | Naturally generated plain deepslate and deepslate ores require iron-tier-or-better pickaxes; crafted/decorative deepslate variants keep their own vanilla requirements. |
| Ore distribution | Some iron remains widely accessible; large rich deposits reward geography and exploration. Diamonds are substantially rarer. Exact generation settings and measurements are recorded in the M02 report. |
| Carrying capacity — M02 as built; later revised | M02 delivered **27 usable slots** and a **64 ingot-equivalent** weight system; its original blanket intake cap was later changed by [M08](MILESTONE_08.md) to restrict only incoming commodities, still counting equipment weight. [M10 Overalls](MILESTONE_10.md) can open the fourth row; Mining Boots change the commodity cap. This row records the M02 baseline, **not** the complete current equipment behavior. |
| Diamond / netherite weight | A diamond counts like one ingot; a diamond block counts as nine. Netherite similarly counts by the diamond-equivalent weight of its gear rather than charging the gold/ingots used in fabrication. |
| Weightless bulk materials | Cobbled stone materials, gravel and sand **do not contribute to the player's heavy-load allowance**. They still occupy ordinary inventory slots. The heavy-load mechanic is for designated metals and other heavy resources, not every material that can be shipped by cart. |
| Weight UI | A material-colored, text-free bar communicates weight in the inventory; hover gives total out of 64 and a per-material breakdown. |
| Mob attraction | Hostiles are drawn toward nearby **lit ground**, not individual torches or the brightest source. The system applies at Overworld night and underground where sky light is absent, including underground daytime; not the Nether/End. Pursuing a player takes precedence. |
| Lighting and survival | Standard torches emit light level **12**; lanterns **15**. Zombies can threaten wooden doors while pursuing; creepers disappear in direct sunlight. Routine hunger exhaustion is reduced, and saturation is visibly shown. |
| Inventory interaction | Inventory sorting and hotbar-first shift-click behavior preserve the constrained-carrying design. |

## M03-era refinements to the M02 mechanics

- Inventory sort order is **top row first**, superseding the original bottom-row-first proposal.
- The Sort control is an icon in an **8-pixel-high** frame aligned with the weight bar.
- Straw beds, like other beds, cannot skip the night; this gained an explicit regression test.

For exact ore distributions, AI tests, hunger tuning, weight mappings, and performance evidence, use [MILESTONE_02_REPORT.md](archive/milestone-reports/MILESTONE_02_REPORT.md), not outdated working-phase handoffs.

**Later delivered overrides:** [M08](MILESTONE_08.md) changed commodity admission while keeping equipment weight; [M10](MILESTONE_10.md) added Mining Boots/Mule and Overalls. Both are **PLAYTESTING**, not SHIPPED. The archived M02 report remains evidence of **M02 at delivery**, not a description of current enforcement.

**Implementation and tests:** [MILESTONE_02_REPORT.md](archive/milestone-reports/MILESTONE_02_REPORT.md) and [M02 evidence](evidence/milestone-02/).

## Implementation and verification — 2026-10-11

**Metadata:** M02 | SHIPPED | Geology and ore rework, deepslate gate, 27-slot inventory with 64-ingot heavy load, torch light and hostile mobs, halved exhaustion.

**Implementation**
- Commits: `9d177181cf7309b7ae1f469be996c39a7638a80c` 02A geology; `0d003d3f2167cbdd1666c61ac71762df572dd32d` 02B inventory and heavy load; `192cae9700e49594dba00ab483e022ba5c742d88` 02C light and mobs; `45fa8383549e6f400b522214b4749ceb3307904d` 02D hunger and report; later refinements `85ee63e65783b96aa93dbc756787c0ecfe1af439` (diamond/netherite weight, load bar), `c45d9015be7b52b7977ecd4189c9186dbe399eed` (weight tooltip), `16d04709fb185a47bc9514d3c7cf720a79c6428f` (lit-area attraction), `2c112b7092e0851fcf0a8fa6f9faf9598c786b1b` (sort order)
- Delivered: removed rock families and gated deepslate; reworked iron and diamonds; 27 slots, sort, left-first hotbar, shared 64-ingot cap; torches at 12, night light attraction, pursuit-only zombie door breaking, creepers dissolve in sunlight; activity exhaustion halved with saturation display.
- Deviations: Refinements above shipped after the 02D handoff build.

**CI**
- Build: #9 | Run ID: 38144733388 | PASS
- Source SHA: `45fa8383549e6f400b522214b4749ceb3307904d`
- URL: https://github.com/boxofdog/simple_block/actions/runs/38144733388
- Artifact: `simple-block-mod` id 11703859176, available (verified 2026-10-13). Refinements green in Builds #11 and #13; #7, #10 and #12 failed on test timing, fixed in the next run.

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 63/63 | [archived report](archive/milestone-reports/MILESTONE_02_REPORT.md); [game-test evidence](evidence/milestone-02/gametest-evidence.md) |
| Client | PASS | 1/1 | `SurvivalSmokeClientTest` M02 steps; [screenshots](evidence/milestone-02/) |
| Dedicated server | PASS | 1/1 | `runServer` start and stop only; no remote client |
| Negative controls | PASS | 4/4 | one per phase 02A–02D; [negative controls](evidence/milestone-02/negative-controls.md) |

**Known gaps**
- GAP-01: MINOR — statistical mob tests (`lightBehindWallsStillAttracts`, zombie door tests) fail occasionally; next: make them deterministic or widen their margins.
- GAP-02: MINOR — all balance values are provisional tuning keys; next: tune from owner playtests.

**Owner playtest**
- NONE
