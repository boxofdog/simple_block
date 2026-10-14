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

**Automated status sync:** After explicit owner instruction, change **only the `status` field in that milestone's YAML header**; never manually edit a roadmap status cell. Run `python3 tools/docs/sync_milestone_statuses.py --write` or let the [status workflow](../.github/workflows/docs-status.yml) mirror it on a push to `main`. The workflow can also refresh bounded README/evidence inventories.

Validate with `python3 tools/docs/sync_milestone_statuses.py --check` and `python3 tools/docs/check_reporting.py`. The [reporting policy](REPORTING_POLICY.json) controls allowed labels. Automation **copies status labels; it never infers authorization, readiness, acceptance or SHIPPED from CI**. Roadmap commentary and milestone prose are editorial; update them separately when they become stale.

**CLAUDE: DO NOT CREATE NEW STANDALONE MILESTONE REPORT FILES.** After an authorized implementation, append a dated `## Implementation and verification — YYYY-MM-DD` section **at the very end of the existing `MILESTONE_XX.md`**, limited to 350 words.

Record delivered behavior, full commit SHA(s), the exact CI build/tested SHA, `simple-block-mod` artifact availability, PASS/FAIL/NOT RUN test counts, relevant negative controls, deviations, gaps and up to five owner playtest steps. Link full logs/screenshots under `docs/evidence/`. Corrective patches follow the **same milestone appendix rule**, linking any patch brief. **Never paste the report into the roadmap.**

Former M01–M07 standalone reports are [archived historical evidence](archive/milestone-reports/), not templates. After writing an appendix, update the milestone YAML state **only if its real lifecycle changed**, then regenerate the roadmap's status cell and adjust editorial links as needed.

Never claim independent ChatGPT verification or owner acceptance. **PLANNED - READY is not coding authorization; only the owner authorizes implementation and marks work SHIPPED.**

## Documentation maintenance boundaries

- `ROADMAP.md` is a milestone status and priority index. Below the milestone table, give **one brief line per milestone with current owner playtest feedback**, linking to its exact `## Playtesting Changes` heading in `MILESTONE_XX.md`. Do not repeat detailed requirements, evidence, bug diagnoses or lengthy next-step lists in the roadmap.
- `BACKLOG.md` is **the only** unnumbered future-work list. Never recreate `FUTURE_WORK.md` or scatter independent brainstorm ledgers throughout docs.
- Milestone-specific decisions and detailed plans belong in `MILESTONE_XX.md`. Put owner playtest refinements and observations under **one `## Playtesting Changes` section per milestone**, adding concise subsections as needed; link bug IDs instead of duplicating bug descriptions. When Claude implements owner-authorized work, it appends its **short dated verification handoff to the end of the SAME milestone document**.
- **DRY bug tracking:** `BUG_LOG.md` is the **only canonical record** of each bug's symptom, reproduction, suspected cause, fix, verification and open/resolved status. Milestone documents may link a `BUG-###` ID but must not copy its diagnostic description or maintain a second bug entry; retain genuinely new owner design requirements and milestone acceptance/CI evidence in the milestone. `ROADMAP.md` only links to these records and describes priority/next decisions, without repeating bug details.
- The archived M01–M07 reports ([archive/milestone-reports/](archive/milestone-reports/)) and archived decisions/roadmap are historical evidence, not an instruction to resume producing separate reports or to implement old superseded proposals.
- Owner-facing painting tasks go in [Joe's To-Do](Joe's%20To-Do/README.md); UX text issues go in [UX_TEXT_LOG.md](UX_TEXT_LOG.md). No document by itself authorizes coding.
