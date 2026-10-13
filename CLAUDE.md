# Instructions for Claude Code — Minecraft Overhaul

**OWNER STATUS UPDATE — 2026-10-12:** M05 is owner-accepted by explicit instruction (no separate personal playtest result asserted here). M06 is authorized and IN PROGRESS, not completed or owner-accepted. M07 is NOW EXPLICITLY AUTHORIZED for implementation under its existing brief; implement it against the real M06 interfaces as they become available, ideally after M06 code handoff and verification. Do not treat authorization as M06 completion, and do not mark M07 implemented or verified prematurely. M08–M10 remain unauthorized.

**Execution order:** Finish and report M06, then execute the already-authorized `docs/MILESTONE_07.md` using verified M06 artifacts/interfaces; no new owner authorization is needed for M07. **M07 additionally REQUIRES TWO visual-only machinery remodels: (1) Cobblestone Engine** substantial pre-industrial furnace and smokestack **inside the original, unmodified vanilla minecart**; preserve its vanilla minecart model/texture and all existing engine behavior. **(2) The M06 coal-powered winch's furnace motor must REPLACE the motor-side side stand/support, not attach as a little adjacent box; integrate it directly into the existing drum's frame/axle.** Manual state keeps the stand; powered state replaces it without duplicate pieces. Preserve winch controls/coal/drive, drum, cable, model clearance and the lever. Verify both ground/hanging orientations. Provide editable Blockbench art/UV and before/after client proof for both visual tasks. Do not implement these art changes in M06; see M07 §5A–5B. Keep separate M06 and M07 commits, tests, reports and acceptance gates. Neither milestone is accepted simply by being authorized.

Read `docs/VISION.md`, `docs/DESIGN_REGISTER.md`, `docs/DECISIONS.md`, and `docs/ROADMAP.md` before working. **ACTIVE OWNER-AUTHORIZED MILESTONE: M06**, approved **2026-10-12**. **Read and implement [`docs/MILESTONE_06.md`](docs/MILESTONE_06.md)** as the complete scope: loading-track station/whole-train stop and right-click release plus front-gate model (actual skip transfers M07); coal-powered furnace-on-existing-drum and three-state lever; unlimited reusable cable per installed coil with at most 12-block unsupported spans and revised shaped Cable/Coil recipes; direct freight-shoving prohibition; retirement of powered/detector/activator rails; and Creative inventory/search cleanup of already-retired items. Work incrementally, test, and hand off with a new M06 report/CI evidence. **Do not implement skips, smelter, armor, cargo-fill visual overhaul, or full freight automation.** Preserve M01–M05 functionality and the owner's established freight-cart identity: the original vanilla minecart. Editable models are in `art/blockbench/`.

**Important owner-approved exception:** **M05 code/report are committed, independently GitHub/CI verified, and explicitly owner-accepted 2026-10-12 (no separate personal playtest result asserted).** The owner explicitly authorized beginning M06 anyway, intending to give M05 final acceptance after returning home. **M05 owner acceptance has now been recorded in the roadmap; do not alter that receipt without owner direction**, and do not require owner approval of normal tuning/engineering choices before starting M06. M05's coal-burning locomotive stall is intentional; the suspected leading-cart clearance near a cable terminal is awaiting the owner's playtest and is NOT a confirmed defect.

**MANDATORY M06 technical concern — Claude owns the solution:** the existing `CableLoops.ticking` checks `isPositionEntityTicking` on **every anchor** before any loop motion. Since M06 deliberately makes cumulative cable length unlimited, this can render a real long railway unusable when faraway anchors/chunks are not loaded. **Design, implement, and demonstrate a safe solution** under `docs/MILESTONE_06.md` §7. A loaded working section must not require every remote anchor to tick. **No chunk force-loading, no simulating or teleporting unloaded carts, no hidden cable-length cap or fuel consumption when stopped.** Preserve installed route and cargo across chunk transitions and save/load; test actual faraway unloaded chunks. If the engine/vanilla ticking architecture imposes a limitation, investigate, choose the smallest safe engineering solution and record the exact limitation honestly rather than declaring it solved. The owner expressly delegated this to you.

**M06 execution instructions:** inspect/run the M05 baseline first. Then implement the authorized work packages in `docs/MILESTONE_06.md` §8, preserving negative controls and client proof. **Do not change the historical M05 report or overwrite its acceptance receipt** as part of M06 implementation. At M06 completion, commit code first, write `docs/MILESTONE_06_REPORT.md`, link the actual CI Build/artifact, and update the roadmap's new M06 handoff/receipt without overwriting the now-approved M05 owner-acceptance record. Report blockers, skipped tests and recommended playtest steps.

**M06 OWNER CORRECTION PASS — explicitly authorized (2026-10-13):** Before implementing the already-authorized M07, complete BUG-002 plus **Loading Rail** rename and **hinged triangular mechanical stop** per [MILESTONE_06.md](docs/MILESTONE_06.md) §0. Folded low on the *inside edge* when not holding a cart, flipped UP **behind** the rear of a docked cart; remove the old tall side post, preserve a reachable right-click release with the cart covering the rail, all M06 docking/anti-relock/train behavior and unchanged internal `simple_block:loading_track` ID. Display name must search as "Loading Rail" / "rail" in Creative. Preserve old Build #19 evidence; add follow-up tests/CI/report before M06 owner signoff. **Do NOT implement the separate roadmapped 3D ordinary-rail models yet.**

**M06 OWNER PLAYTEST BUG-002 (2026-10-13; BLOCKS OWNER ACCEPTANCE):** See [ROADMAP.md](docs/ROADMAP.md) bug log. A cart at the winch-side end of a track makes PULL physically impossible; the lever auto-NEUTRALs immediately after a PULL attempt, so every subsequent click selects PULL again and FEED cannot be reached without destroying the cart. Fix M06's control/endpoint interaction so the player can deliberately select FEED and move away from the endpoint, while retaining safe stops, coal conservation, no derailment, and normal three-position functionality. Add near- and far-end regression and actual-client checks. Do not mark M06 owner accepted merely because CI #19 passed; address this discovered defect before M07 integration. Keep unrelated M07 changes separate.

**URGENT M07 OWNER PLAYTEST PATCH (2026-10-13) — AUTHORIZED CORRECTIVE WORK:** Read and implement [docs/MILESTONE_07_PLAYTEST_PATCH.md](docs/MILESTONE_07_PLAYTEST_PATCH.md) against M07 as built. Fix **skip → cart** unloading ONLY when the cart is **horizontally adjacent AND one rail level LOWER** (diagonally below the skip's bottom chute), so the **existing chute lip touches the minecart rim**; **NOT** when directly side-by-side at the same height. **DO NOT reshape the ore skip** to fix station logic. Place skips **without** any bridle or fourth-block collision; cable attachment visibly creates the collidable bridle and cable detachment removes it. For **cart → skip**, actually visually lower the minecart front gate/ramp into the intake. A ceiling anchor whose cable turns vertically downward to a hung skip must visually become a ceiling-mounted pulley. Geometry, actual gate/ramp model, client rendering and obstacle checks must agree: inventory state must not select transfer direction. Add negative tests, real-client screenshots, report amendment and new passing CI before declaring M07 complete. M07 owner acceptance is still pending; do not start M08. **ALSO EXPLICITLY REMOVE 'YOUR HAND SLIPS':** The player may carry an installed cable's loose hook ANY distance (including vertically down a deep shaft) without auto-dropping after 12+3 blocks. Remove the `CableHook.tick` distance-triggered release and old message; keep 12-block max for a completed support-to-support span in `CableRoutes.routeThrough`. Fix the existing drum-unload-dependent hook disappearance so long-distance carrying, hook ownership and deep-shaft attachment do not break when the drum chunk is unloaded; no force-loading, duplication or no-clip cabling. Test >15/32/100 blocks, deep-shaft attachment, unloaded drum, deliberate release and fixed-span rejection. Full requirements in patch §5A. **This patch supersedes incompatible assumptions in original M07 brief/report; historical Build #22 remains historical.**

## Priority and scope

1. The user's current direct instructions take priority.
2. The **owner-authorized milestone brief** defines the only authorized implementation scope. **M06 is expressly authorized and in progress; M05 was subsequently owner-accepted on 2026-10-12**; this exception is scoped to starting M06, not claiming completion of M06 or M07. Do not assume other milestones are authorized merely because a roadmap mentions them.
3. `docs/DESIGN_REGISTER.md` provides future context and decision status, **not permission to implement future systems**.
4. `docs/VISION.md` is a design guide, not a technical specification.
5. If documents conflict, flag the conflict and prefer the narrowest interpretation that satisfies the current milestone in a fresh world.

## First inspect, then edit

Before changing code:
- Review repository tree, Git status, build tooling, gradle wrapper, mod metadata, dependencies, existing source, tests, and any existing instructions.
- Identify the actual Minecraft version, loader, mappings, Java version, and run tasks.
- Try the existing build and report the baseline result. Don't treat pre-existing errors as new regressions.
- Do not assume a particular loader or upgrade an existing version just to use a preferred API.
- If this is an empty repository, propose a minimal supported Java Edition mod project based on currently available, mutually compatible stable tooling; state selections explicitly before scaffolding.

## Engineering boundaries

- Work incrementally. Keep the authorized milestone small, testable, and independently modifiable.
- Prefer supported loader hooks/events and data/recipe overrides over invasive patches where practical. Use mixins/patching only when needed for the selected game version and document why.
- Put authoritative game-rule enforcement on the logical server, including integrated single-player. Do not rely on client UI restrictions alone.
- Prefer a small, understandable implementation over speculative frameworks for future trains, custom rendering, or worldgen.
- This overhaul targets **newly created worlds only**. Do not implement existing-world migration, legacy-data recovery, or compatibility layers.
- Keep vanilla block, item, and entity registrations where the game/loader requires them; disable targeted gameplay behavior through appropriate hooks, recipes, and acquisition paths instead. This is a technical constraint, not an old-save compatibility promise.
- Avoid adding broad dependencies or unrelated code cleanup.
- Do not overwrite user-authored art. When an approved milestone calls for custom models, provide editable Blockbench `.bbmodel` source and UV templates with replaceable textures, not unsolicited finished textures.
- Record any platform/version-specific limitations rather than pretending a behavior is implemented.

## Verification

- Keep the milestone's test matrix (`docs/TEST_CHECKLIST_01.md`, and for Milestones 02-04 the matrix in that milestone's report) filled in with PASS, FAIL, or NOT RUN and evidence.
- A successful compile alone is not proof of in-game behavior.
- Report which checks were actually run (build, dev client, integrated server, dedicated server, manual checks), and which were not.
- If running Minecraft is unavailable, provide exact local testing instructions and honestly report incomplete validation.
- Don't call the milestone complete while a required acceptance test is known to fail.
- Preserve a clean, reviewable diff; do not overwrite user changes. Explain any unusual configuration edits.

## Shared milestone handoff (required on every completion)

After implementing and pushing an **owner-authorized milestone**, update the **Milestone commit handoff** section in `docs/ROADMAP.md`:

1. Record the milestone ID, **implementation/code commit SHA(s)**, the milestone report link, a concise change summary, deviations and unresolved issues.
2. Check **Claude — code committed and pushed** only after confirming the implementation commits are on `main`. Commit the code **before** recording the SHA in the roadmap; the later documentation commit is a separate commit.
3. Check **Claude — tests and closeout report recorded** only after recording actual results, including skipped/flaky checks.
4. Check **Claude — CI verified** only after checking the specific GitHub Actions run for the implementation SHA and confirming the `simple-block-mod` artifact. Otherwise leave unchecked with a note.
5. Commit and push the updated roadmap so ChatGPT and the owner can independently inspect the **same repository record**. Include the roadmap/handoff commit SHA in your final reply.
6. **Never check ChatGPT's independent verification or the owner's acceptance boxes.** Those are separate checks; do not mark a milestone accepted based on tests alone.
7. After verification and acceptance, keep a short line in the receipts table and reset the live checklist for the **next explicitly authorized** milestone. Don't auto-start it.

This is a factual bookkeeping workflow, **not permission to change scope, reprioritize work, or start another milestone**.

## Session output

At the end, provide:
1. Baseline repository/runtime findings.
2. Files changed and why.
3. Implementation summary for the **current milestone** (including any deviations from the authorized brief).
4. Test results and limitations.
5. How to run/build locally.
6. Remaining issues and next recommended milestone.

If making commits is appropriate for the current repository and workflow, use clear, scoped commits; never discard existing uncommitted user work.

## Project facts

- **Toolchain:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Gradle 9.7.1 wrapper, JDK 25. Minecraft 26.x is unobfuscated: code uses Mojang names, with no mappings. Decompiled sources come from `./gradlew genSources`; read them instead of guessing APIs.
- **Mod ID:** `simple_block` (provisional). Java package `io.github.boxofdog.simpleblock`. Each feature keeps its own `mixin/<feature>` package and is listed in `SimpleBlock`'s Javadoc and the README table.
- **Git:** work directly on `main`; don't create feature branches.
- **Tests:** `./gradlew build` runs the server game tests in `src/gametest` (fresh world each run). `./gradlew runClientGameTest` runs the real-client smoke test. Headless machines need `xvfb-run` plus `mesa-vulkan-drivers`, because Xvfb lacks the sRGB GLX visual 26.3's OpenGL backend wants and the client falls back to Vulkan.
- **Test quality bar:** for each new restriction, add a game test that fails on vanilla behaviour, and check it once against a build with the change removed (negative control).
- **Tuning (Milestone 02):** provisional balance numbers live only in `tuning/Tuning.java`. Locked design values are constants in their features, not tuning keys. Players override keys in `config/simple_block/tuning.json`; `tuning-defaults.json` next to it is rewritten each start with every key.
- **Worldgen measurement:** game-test worlds are superflat, so ore and rock changes are measured with `./gradlew runOreSampler -PoreSamplerLabel=<label>` (gametest source set, `OreSampler`): a dedicated server on the normal world whose `level-seed` is in `build/run/oreSampler/server.properties`, writing CSVs and slices to `build/ore-sampler/`. Compare against the same sampler in a `git worktree` of the pre-change commit.
- **Playtest deployment:** pushing to `main` runs `.github/workflows/build.yml`, which publishes the `simple-block-mod` artifact (the jar plus `build-info.json`). The user installs it with `tools/windows/Update-SimpleBlock.bat` (see `docs/PLAYTESTING_SETUP.md`). Keep the workflow file name, artifact name and `build-info.json` fields in step with the updater and `tools/windows/tests/Test-Updater.ps1`. After pushing, check the Build run succeeded so the user's next update actually gets the change.
- **Results so far:** `docs/TEST_CHECKLIST_01.md`, `docs/MILESTONE_01_REPORT.md`, `docs/evidence/`.
