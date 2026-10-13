# Milestone 01 — Survival Foundation

**Goal:** Implement a minimal, buildable, testable mod that removes four vanilla conveniences/distractions. Establish a reliable code/build/test workflow. **Do not implement** any future mechanics from the design register.

**World policy:** Only newly created worlds are supported. Do not build save migration, backwards compatibility, or legacy inventory recovery.

## Scope at a glance

| ID | Feature | Required result |
|---|---|---|
| SF-01 | Beds | Can set valid Overworld respawn, cannot skip time or thunderstorms by sleeping. |
| SF-02 | Phantoms | No naturally/ordinarily spawning phantoms during survival. |
| SF-03 | Elytra | No usable flight and no normal-survival acquisition. |
| SF-04 | Shulker boxes | No normal-survival acquisition or functional portable inventory; all variants. |

### Phase 0 — Baseline audit

1. Report the repository's actual Minecraft version, loader, mappings, Java/Gradle versions, mod ID, dependencies, Git state, and current source layout.
2. Run an unmodified baseline build if possible; record result.
3. Prefer current existing configuration. No forced migration to a new game version or loader.
4. If empty repo, select a supported, compatible **released** toolchain and present the selection before scaffolding. Do not rely on unverified previous chat claims of a specific version.
5. Record the working build/run commands; don't overwrite existing user modifications.

### SF-01 — Sleeping

- Server-authoritatively prevent beds from advancing the clock or clearing storms, including the case where all multiplayer players attempt to sleep.
- Preserve valid Overworld respawn-point setting via bed interaction; give clear ordinary feedback to player.
- Do not trap players in a sleeping state or break getting out of a bed.
- Leave Nether/End bed behavior unchanged. Avoid modifying respawn anchors or the global day/night speed.
- No new beds or assets needed.

### SF-02 — Phantoms

- Prevent phantoms from appearing as part of normal survival systems, including the vanilla insomnia-related spawning behavior.
- No penalty tied to not sleeping.
- Other hostile mobs should remain unaffected in Milestone 01.
- Preserve registrations and avoid changing unrelated loot/advancement systems unless necessary; explicit creative/command-spawn handling can be documented separately.

### SF-03 — Elytra

- Stop elytra flight/gliding **on the logical server** even if the item is injected via commands or creative mode for testing.
- Prevent obtaining elytra through normal survival progression, including naturally generated loot/display opportunities such as End ship item frames; choose reliable version-appropriate hooks.
- Keep vanilla registrations if required for engine/mod stability; normal survival should not expose working elytra.
- Don't disable ordinary jumping, falling, swimming, boats, or other movement.

### SF-04 — Shulker boxes

- Block acquisition in normal survival: vanilla recipes and other vanilla access paths as applicable. Cover uncolored and all dyed variants.
- Prevent using shulker boxes as portable storage, including placement/opening/interacting in the normal survival flow and any common bypasses feasible in scope.
- **Shulker mobs are not removed** by this requirement.
- Keep vanilla item/block registrations if required for technical stability, rather than trying to unregister vanilla internals. There is **no requirement** to support old worlds or recover contents of shulker boxes from preexisting saves.
- Be mindful of items originating from creative mode or commands: the server should still stop the portable-storage exploit. The milestone does not need to erase creative inventory menus.

## Cross-cutting behavior

- Server-authoritative restrictions for single-player/integrated and dedicated-server cases.
- Avoid introducing unnecessary config systems; internal constants or isolated features are fine.
- Keep the four restrictions separate, simple, documented, and individually reversible.
- Use loader-native APIs where supported. Document mixins/patches when needed.
- No API that only changes client visuals to imply behavior is disabled.
- No custom models, GeckoLib, freight carts, cables, skips, ore/worldgen, hunger, mob-light AI, or redstone changes now.

## Staged execution

**Step A:** Establish clean baseline and implement SF-01. Compile + verify bed interaction before proceeding.

**Step B:** Implement SF-02. Test repeated nights and confirm regular hostile mobs remain.

**Step C:** Implement SF-03. Validate both removal from survival access and no server-authorized gliding from an item injected by commands for testing.

**Step D:** Implement SF-04. Test each color family in a newly created disposable world, including command-injected boxes to check for interaction bypasses.

**Step E:** Clean build, dev-client run, dedicated-server verification where available, and final QA report.

## Acceptance criteria

- Build succeeds with documented exact command and runtime environment.
- Mod can be loaded in dev client; server also validated or marked NOT RUN with steps.
- SF-01 through SF-04 behave as specified in an actual world; limitations reported honestly.
- No legacy-save features or migration tools are added; required vanilla registrations remain stable.
- Each feature is isolated sufficiently to change later without rewriting the others.
- No out-of-scope design systems or speculative infrastructure added.
- Test report uses **PASS / FAIL / NOT RUN**, with concise evidence for each.

## When blocked

Report the smallest concrete blocker, explain any required deviation, and keep already-working changes intact. Do not claim untested behavior is working. Ask approval before destructive changes or a wholesale loader/version migration.

---

## Known M01 side-effects / deferred decisions (consolidated 2026-10-13)

### M01 consequences still awaiting a separate decision

- Phantom membranes and related advancements/potions may become unavailable or harder to obtain without naturally spawning phantoms; the cat-gift route has not been verified here.
- Shulker shells lose their normal survival purpose.
- The elytra advancement becomes inaccessible through ordinary survival.
- Some vanilla sleep/phantom game rules no longer affect the behaviors changed by Simple Block.

These are **known side effects**, not permission to add replacement recipes or alter advancements.

**Source:** These consequences were previously recorded in the retired central decisions ledger. This section does not authorize new recipes, advancements or drop changes.

## Implementation and verification — 2026-10-11

**Metadata:** M01 | SHIPPED | Survival restrictions: no sleep or storm skipping, no survival phantoms, no elytra gliding, no portable shulker-box storage.

**Implementation**
- Commits: `3e1e6615af0a12330a573ceddc9717d88700e95a` scaffold and SF-01..04; `55362ce17149a330cda73609df4a6479d12795ff` test-player fixes; `c9e6e53456889a69553d552fad486d726122afe8` client smoke test and QA docs
- Delivered: beds set spawn but never skip night or storms; no natural phantoms; no elytra gliding and no End-ship elytra; shulker boxes neither crafted, placed nor opened.
- Deviations: NONE recorded.

**CI**
- Build: #2 | Run ID: 38118083013 | PASS
- Source SHA: `ba59665a947f9691b1006d1f5fa16e6b526cf125` (first green run containing M01 code; M01 commits predate CI, Build #1 failed)
- URL: https://github.com/boxofdog/simple_block/actions/runs/38118083013
- Artifact: `simple-block-mod` id 11693732495, available (verified 2026-10-13).

**Tests**

| Category | Result | Passed/Total | Evidence / Notes |
|---|---|---|---|
| Server | PASS | 22/22 | [test output](evidence/milestone-01/test-output.txt); [checklist](TEST_CHECKLIST_01.md) |
| Client | PASS | 1/1 | `SurvivalSmokeClientTest`; [screenshots](evidence/milestone-01/) |
| Dedicated server | NOT RUN | 0/1 | ENV-04 (dedicated server with remote client) skipped by agreement |
| Negative controls | PASS | 12/12 | all restriction tests fail with the changes removed; [archived report](archive/milestone-reports/MILESTONE_01_REPORT.md) |

**Known gaps**
- GAP-01: MINOR — no dedicated-server session with a remote client was run for M01; next: join a dedicated server and repeat the bed, phantom, elytra and shulker checks.
- GAP-02: MINOR — a vanilla client may briefly see a ghost shulker box before the server removes it (untested); next: check during that session.

**Owner playtest**
- NONE
