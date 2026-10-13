# Simple Block — Rules

## Milestone status terminology

**Single source of truth for milestone lifecycle names.** Both ChatGPT and Claude Code MUST use **exactly these five labels**, spelled/capitalized as shown, in the active [roadmap](ROADMAP.md), current milestone headers and status updates. Do not invent synonyms such as `PASSED`, `ACCEPTED`, `IN PROGRESS`, `AUTHORIZED`, `DESIGN DIRECTION` or `PENDING` as additional milestone states.

| Status | Meaning |
|---|---|
| **PLANNED - NOT READY** | A milestone is proposed or being designed; decisions/specifications needed before implementation handoff remain unresolved. |
| **PLANNED - READY** | Scope and implementation handoff are sufficiently defined to start **once the owner explicitly authorizes it**; no implementation has begun. |
| **IMPLEMENTING** | The owner has explicitly authorized work and the implementation is underway; tests, CI and owner playtest are not yet the final acceptance gate. |
| **PLAYTESTING** | Code is handed off for owner inspection/playtesting; regressions or fixes may still be needed. **This does not mean accepted or shipped.** |
| **SHIPPED** | The owner has explicitly signed off on the milestone's delivered work. This replaces the former milestone label **PASSED**. |

**Normal progression:** `PLANNED - NOT READY` → `PLANNED - READY` → `IMPLEMENTING` → `PLAYTESTING` → `SHIPPED`. A status can move backward if discoveries require more design or rework, but only with a dated explanation. No skipped owner authorization: **PLANNED - READY is not approval to start**. Claude must not independently promote a milestone to `SHIPPED` on the strength of green CI, a report or automated tests; owner signoff is required.

**Translation rule:** The old owner-approved `PASSED` label maps to `SHIPPED`, without inventing additional tests or playtest sessions. Old documentary statuses (`owner accepted`, `implemented`, `not authorized`, `PASS` test results, and design-register `DECIDED/DIRECTION/EXPLORATORY/OPEN`) may remain in historical narrative and test matrices; they are **not** milestone lifecycle labels. Authorization is a separate explicit decision recorded in the handoff; readiness alone grants no permission to code.

**Current statuses:** Each `MILESTONE_XX.md` file's two-field YAML front matter is authoritative (`milestone: MXX` and `status: <one of the five labels>`). The [ROADMAP.md](ROADMAP.md) status column is generated from those headers, not separately maintained. This file defines the vocabulary and rules, not another status list.

## Where documentation belongs

| Question | Canonical document |
|---|---|
| What is next? What is shipped? | [ROADMAP.md](ROADMAP.md), generated milestone status column and next decisions |
| What do the five states mean? | **This document**, the canonical cross-LLM lifecycle key |
| What exactly must Claude build? | **[MILESTONE_XX.md](MILESTONE_08.md)** or scoped corrective patch, owner-authorized before coding |
| Where do implementation commits, CI, tests and gaps go? | A **concise dated section at the end of the same `MILESTONE_XX.md`** for all new work. M01–M07 existing reports remain historical records. |
| Where do design decisions live? | The appropriate [milestone handoff](MILESTONE_08.md), or [DESIGN_REGISTER.md](DESIGN_REGISTER.md) for unnumbered ideas. [Retired decision archive](archive/DECISIONS_PRE_RETIREMENT_2026-10-13.md) is historical only. |
| What bug was seen, reproduced and fixed? | [BUG_LOG.md](BUG_LOG.md) (not an automatic task authorization) |
| What's an idea without a milestone? | [BACKLOG.md](BACKLOG.md) |
| What does Joe need to paint? | [Joe's To-Do](Joe's%20To-Do/README.md) |
| Which messages/labels need cleanup? | [UX_TEXT_LOG.md](UX_TEXT_LOG.md) |
| How do I install a test build? | [PLAYTESTING_SETUP.md](PLAYTESTING_SETUP.md) |

## Handoff rules

**Canonical reporting contract: [REPORTING_POLICY.json](REPORTING_POLICY.json).** Its report schema, mandatory Markdown template, 350-word limit, missing-value markers (`UNAVAILABLE`, `NOT RUN`), lifecycle enum and validation rules govern every milestone verification section. Where this file and the JSON differ on reporting, the JSON wins; this section only summarizes it. Mechanically checkable rules are verified by `python3 tools/docs/check_reporting.py`.

**Automated status sync:** To change a milestone's lifecycle state, change **only the `status` field in that milestone's YAML header**, after the required explicit owner instruction. Do not hand-edit the roadmap Status column. Run `python3 tools/docs/sync_milestone_statuses.py --write`; the [Documentation status sync workflow](../.github/workflows/docs-status.yml) does the same on a push to `main`, committing **only generated status cells and bounded README/evidence inventories**. `python3 tools/docs/sync_milestone_statuses.py --check` and `python3 tools/docs/check_reporting.py` validate consistency. The [reporting-policy JSON](REPORTING_POLICY.json) defines the exact five values. The tool **copies labels; it does not infer readiness, implementation or acceptance from CI**. `SHIPPED` and implementation authorization still require an explicit owner decision. Free-text roadmap notes and milestone narrative are **not automatically rewritten**; update them intentionally when facts change.

**CLAUDE: DO NOT CREATE NEW STANDALONE MILESTONE REPORT FILES.** Following an explicitly owner-authorized implementation, **append a concise, dated `## Implementation and verification — YYYY-MM-DD` section at the very end of that milestone's existing `MILESTONE_XX.md`**. Include only what was implemented, code commit SHA(s), the exact GitHub CI build and published `simple-block-mod` artifact, PASS/FAIL/NOT RUN server/client tests, negative controls where relevant, material deviations, remaining issues and the owner's next playtest. Link fuller logs/screenshots under `docs/evidence/` instead of producing a second lengthy document. Corrective patches also report at the end of the **main milestone file**, with a link to any patch brief. **Do not paste this report into the roadmap.**

The former standalone `MILESTONE_01_REPORT.md` through `MILESTONE_07_REPORT.md` were consolidated on 2026-10-13 into one verification section at the end of each milestone document and moved to [archive/milestone-reports/](archive/milestone-reports/) as historical evidence, not templates for future work. No new `MILESTONE_XX_REPORT.md` files. After writing the dated milestone appendix, change the authorized milestone's YAML status and let the script regenerate its roadmap status cell; edit non-generated roadmap links deliberately when needed, and use only the [canonical status labels](#milestone-status-terminology). Claude must not claim independent ChatGPT verification or owner acceptance. **PLANNED - READY is not implementation authorization; only the owner authorizes starting and marks SHIPPED.**

## Documentation maintenance boundaries

- `ROADMAP.md` is a milestone index and current next-decision board. Never paste long scope briefs, code history, CI tables or report narratives into it.
- `BACKLOG.md` is **the only** unnumbered future-work list. Never recreate `FUTURE_WORK.md` or scatter independent brainstorm ledgers throughout docs.
- Milestone-specific decisions and detailed plans belong in `MILESTONE_XX.md`. When Claude implements owner-authorized work, it appends its **short dated verification handoff to the end of the SAME milestone document**.
- The archived M01–M07 reports ([archive/milestone-reports/](archive/milestone-reports/)) and archived decisions/roadmap are historical evidence, not an instruction to resume producing separate reports or to implement old superseded proposals.
- Owner-facing painting tasks go in [Joe's To-Do](Joe's%20To-Do/README.md); UX text issues go in [UX_TEXT_LOG.md](UX_TEXT_LOG.md). No document by itself authorizes coding.
