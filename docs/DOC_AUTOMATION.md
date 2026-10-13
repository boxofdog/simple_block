# Simple Block — documentation automation

All commands run from the repository root using Python 3 (standard library only).
Automation handles factual indexes and mechanical consistency; it **never**
authorizes implementation, alters an owner's design, claims test success, or
accepts a milestone on the owner's behalf.

## Single source of truth

| Subject | Owner | Automated behavior |
|---|---|---|
| Lifecycle and permitted status words | docs/RULES.md, docs/REPORTING_POLICY.json | Enforce exact vocabulary |
| A milestone's current status | YAML header in docs/MILESTONE_XX.md | Sync only ROADMAP status cells |
| Scope and accepted requirements | Relevant milestone file | Never generated or inferred |
| Implementation evidence and known gaps | Dated appendix at bottom of milestone file | Structural validation only |
| CI run, tested SHA and artifact availability | Actual GitHub Actions Build | Read and present verified REST API records |
| Client, server, negative control and owner verification | Executed test records and owner | Not inferred from green CI |
| Design, bugs, UX, future ideas | DESIGN_REGISTER, BUG_LOG, UX_TEXT_LOG, BACKLOG | Duplicate IDs and broken references flagged |
| Evidence files | docs/evidence | File counts only; no test-status inference |
| Java source package inventory | src/*/java | One bounded generated section in README |

## Commands Claude should use

| Purpose | Command |
|---|---|
| Mirror approved status changes | python3 tools/docs/sync_milestone_statuses.py --write |
| Verify milestone statuses | python3 tools/docs/sync_milestone_statuses.py --check |
| Check dated appendices and links | python3 tools/docs/check_reporting.py |
| Check cross-doc identifiers and existence | python3 tools/docs/automation.py drift |
| Get known Build facts | python3 tools/docs/automation.py ci --run-id 38443850502 |
| Get latest successful Build on main | python3 tools/docs/automation.py ci --latest-successful |
| Get structured verified Build facts | python3 tools/docs/automation.py ci --run-id 38443850502 --format json |
| Regenerate README package counts | python3 tools/docs/automation.py readme --write |
| Check README inventory freshness | python3 tools/docs/automation.py readme --check |
| Regenerate file evidence inventory | python3 tools/docs/automation.py evidence --write |
| Check evidence inventory freshness | python3 tools/docs/automation.py evidence --check |
| Suggest docs affected by code changes | python3 tools/docs/automation.py impact --base HEAD^ --head HEAD |
| Preview milestone scaffold | python3 tools/docs/automation.py new M12 "Owner supplied title" |
| Create requested milestone and roadmap row | python3 tools/docs/automation.py new M12 "Owner supplied title" --write |
| Run documentation regression tests | python3 -m unittest discover -s tools/docs -p 'test_*.py' -v |

The milestone scaffold always starts PLANNED - NOT READY and never starts coding.
Only use it when the owner explicitly asks to create a new milestone.

### CI evidence and missing tests

The CI command uses GH_TOKEN or GITHUB_TOKEN when present, otherwise read-only
GitHub endpoints. It verifies the selected run really belongs to the Build workflow,
copies its tested commit SHA and result, and checks actual artifact entries for
expiration. It **does not** convert CI success to client, remote-server, negative
control or owner playtest PASS. Test counts remain UNAVAILABLE by default.

For an optional separate test record, use the --test-evidence FILE argument
with a JSON record using these keys:

    {
      "head_sha": "<full 40-character SHA for the Build>",
      "categories": {
        "Server": {"result": "PASS", "passed": 10, "total": 10},
        "Client": {"result": "NOT RUN", "passed": 0, "total": 0},
        "Dedicated server": {"result": "NOT RUN", "passed": 0, "total": 0},
        "Negative controls": {"result": "NOT RUN", "passed": 0, "total": 0}
      }
    }

The tool checks that test counts and statuses have valid shapes and that the
SHA matches the CI Build. That does not verify the truth of manually supplied
counts: inspect primary execution logs before making a PASS claim.

## GitHub workflows and token savings

The lightweight Documentation Status Sync workflow runs on relevant pushes and
manual dispatch. It synchronizes ROADMAP status cells, the README Java package
inventory and docs/evidence/INDEX.md, checks reporting and links, runs offline
regression tests, and displays advisory source-to-doc review hints.

Only these generated locations may be automatically committed: ROADMAP status cells,
the marked README section, and the evidence INDEX. After fetching latest main,
the workflow recomputes generated files before pushing so it does not overwrite
other edits. Non-generated prose remains owner/Claude maintained.

The more expensive Java Build workflow only runs on relevant Java/build inputs
or deliberate manual dispatch, not Python, README or documentation-only changes.
Do not run an extra Java build just to reformat documentation.

## Deliberate limitations

- Heuristic code-impact hints search for exact class-name references. They do
  not prove that a specification needs to change; manual semantic review remains.
- Structural drift checks detect duplicate ledger IDs, unrepresented milestones
  and missing milestone-file references. They cannot decide whether competing
  prose expresses a true design conflict.
- The evidence inventory counts files; it does not assert that any pictured test
  passed or that a developer ran it.
- The generated README inventory counts source files, not completed features.
- Only the owner can authorize an implementation and sign off SHIPPED.
