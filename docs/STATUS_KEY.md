# Milestone status key — canonical vocabulary

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

**As of 2026-10-13:** M01–M06 **SHIPPED**; M07 **PLAYTESTING**; M08, M09 and M10 **PLANNED - NOT READY**; M11 **PLANNED - READY** (detailed rendering/hopper/test handoff is prepared, but still awaits an explicit owner go-ahead). For current status, consult [ROADMAP.md](ROADMAP.md); this file defines the vocabulary, not a separate competing status board.

**Documentation routing:** The [roadmap](ROADMAP.md) holds one exact lifecycle label per milestone. Technical scope and owner decisions live in `MILESTONE_XX.md`; actual commits, CI and remaining gaps for **new work** are summarized in a concise dated section appended to the end of the same `MILESTONE_XX.md`. M01–M07's separate `*_REPORT.md` files are historical only. Don't reintroduce verbose milestone history into the roadmap.
