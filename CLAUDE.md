# Instructions for Claude Code — Minecraft Overhaul

Read `docs/VISION.md`, `docs/DESIGN_REGISTER.md` and `docs/DECISIONS.md`, then the current milestone's documents: `docs/MILESTONE_04.md` (the owner's brief, verbatim; its test matrix is in `docs/MILESTONE_04_REPORT.md`), and `docs/ROADMAP.md` for context only (Milestones 01-03 are complete and must keep working; their reports are `docs/MILESTONE_01_REPORT.md`, `docs/MILESTONE_02_REPORT.md` and `docs/MILESTONE_03_REPORT.md`). The owner's Milestone 03 clarification in `docs/DECISIONS.md` (the vanilla minecart is the cargo cart; no separate freight cart) still stands. M04's provisional choices and open questions are in `docs/DECISIONS.md`; editable models are in `art/blockbench/` (see its README). If a `docs/MILESTONE_0N_CHECKPOINT.md` exists for the current milestone, read it first: it records what is done and the next steps.

## Priority and scope

1. The user's current direct instructions take priority.
2. The current milestone's documents define the **only authorized implementation scope**. Now that is Milestone 04: `docs/MILESTONE_04.md`. Milestones 01-03's rules stay in force.
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

- Work incrementally. Isolate the four features so they are independently modifiable or revertible.
- Prefer supported loader hooks/events and data/recipe overrides over invasive patches where practical. Use mixins/patching only when needed for the selected game version and document why.
- Put authoritative game-rule enforcement on the logical server, including integrated single-player. Do not rely on client UI restrictions alone.
- Prefer a small, understandable implementation over speculative frameworks for future trains, custom rendering, or worldgen.
- This overhaul targets **newly created worlds only**. Do not implement existing-world migration, legacy-data recovery, or compatibility layers.
- Keep vanilla block, item, and entity registrations where the game/loader requires them; disable targeted gameplay behavior through appropriate hooks, recipes, and acquisition paths instead. This is a technical constraint, not an old-save compatibility promise.
- Avoid adding broad dependencies or unrelated code cleanup.
- Do not modify user-authored art or generate custom models (Milestones 01 and 02 add no new block art). Future custom assets will use editable Blockbench `.bbmodel` source with replaceable textures.
- Record any platform/version-specific limitations rather than pretending a behavior is implemented.

## Verification

- Keep the milestone's test matrix (`docs/TEST_CHECKLIST_01.md`, and for Milestones 02-04 the matrix in that milestone's report) filled in with PASS, FAIL, or NOT RUN and evidence.
- A successful compile alone is not proof of in-game behavior.
- Report which checks were actually run (build, dev client, integrated server, dedicated server, manual checks), and which were not.
- If running Minecraft is unavailable, provide exact local testing instructions and honestly report incomplete validation.
- Don't call the milestone complete while a required acceptance test is known to fail.
- Preserve a clean, reviewable diff; do not overwrite user changes. Explain any unusual configuration edits.

## Session output

At the end, provide:
1. Baseline repository/runtime findings.
2. Files changed and why.
3. Implementation summary for sleeping, phantoms, elytra, and shulker boxes.
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
