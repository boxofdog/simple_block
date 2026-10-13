# Instructions for Claude Code — Minecraft Overhaul

**Milestone status:** use the exact five labels in [docs/RULES.md](docs/RULES.md). Read the current [ROADMAP.md](docs/ROADMAP.md) board, not a copied status list here. The `milestone`/`status` YAML header in each `MILESTONE_XX.md` is the **single source of truth**. Change that header only after explicit owner direction, then run `python3 tools/docs/sync_milestone_statuses.py --write` and `python3 tools/docs/check_reporting.py`. GitHub Actions also synchronizes roadmap status cells on main. **Never edit roadmap status cells directly; `PLANNED - READY` does not authorize coding; green CI never grants `SHIPPED`.** Historical reports, the retired central decisions ledger and older handoffs are not active permission to implement.

**M07 owner playtest gate:** the main open decision is sequential unloading of a coupled train at one side-on Loading Rail after release; other untested cases are in [M07 report §12](docs/archive/milestone-reports/MILESTONE_07_REPORT.md). Do not reopen accepted M06 behavior or mark M07 owner-accepted without a fresh explicit instruction. The old minecart model restriction was superseded by the **shared universal four-gate cart**.

**Reporting contract (mandatory):** before every milestone implementation or documentation closeout, read and apply [docs/REPORTING_POLICY.json](docs/REPORTING_POLICY.json) and [docs/RULES.md](docs/RULES.md). On an authorized status transition, change only the milestone YAML status, run `python3 tools/docs/sync_milestone_statuses.py --write` and then `python3 tools/docs/check_reporting.py`. Verification goes in one dated `## Implementation and verification — YYYY-MM-DD` section appended to the end of the existing `docs/MILESTONE_XX.md` (never a new `MILESTONE_XX_REPORT.md`); run `python3 tools/docs/check_reporting.py` before committing documentation.

## Priority and scope

1. The user's current direct instructions take priority.
2. The **owner-authorized milestone brief** defines implementation scope; consult the live [ROADMAP.md](docs/ROADMAP.md) status column. Only explicit owner direction authorizes new implementation, regardless of `PLANNED - READY`. Never infer permission from backlog or CI.
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
- **M08 FUTURE OWNER-SCOPED WEIGHT CHANGE (2026-10-13; NOT CURRENT IMPLEMENTATION AUTHORIZATION):** [docs/MILESTONE_08.md](docs/MILESTONE_08.md) adds carry-rule relief to the planned standalone blast furnace milestone. **ALL equipment, rails, buckets, lanterns, minecarts and tools retain their metal-weight contribution but can be obtained even when carried weight is 64+**; the only cap-controlled intakes are **ingots, storage/material blocks and ore** (precise converted forms to be audited), and equipment weight reduces room for those cargo resources. Fix ALL enforcement paths, not only pickups: `HeavyLoad.allowedCount`, menu transactions/rollback, crafting, bundles, cursor and offhand; keep a truthful >64 weight meter and preserve items. Mining Boots, the Mule progressively slower overloaded-carrying benefit, and Old Pro remain **M10**, NOT M08; do not start either milestone merely because it is in this note.
- Do not overwrite user-authored art. When an approved milestone calls for custom models, provide editable Blockbench `.bbmodel` source and UV templates with replaceable textures, not unsolicited finished textures. **All owner-facing painting tasks and asset approval checkboxes belong in [docs/Joe's To-Do/](docs/Joe's%20To-Do/README.md)**; add ready-to-paint file paths there, not as separate owner to-do lists in milestone reports. **Planned M11** ([docs/MILESTONE_11.md](docs/MILESTONE_11.md)) requires Claude-written material/form/quantity-based freight fill rendering, **full vanilla hopper→cart top-loading (only docked on Loading Rail), cart→hopper through real short-end/long-side gates, item-conserving progressive loading/unloading including manual cart filling, and removal of all cart-originated hover/click flavor/status text**; also editable silhouettes and UV templates when separately authorized. Joe paints final assets. **Do not implement M11 during current M07 patches** without explicit owner authorization.
- Record any platform/version-specific limitations rather than pretending a behavior is implemented.

## Verification

- Keep the milestone's test matrix filled in with PASS, FAIL or NOT RUN and evidence. Old M01–M07 matrices remain in historical reports; new work gets concise results at the bottom of its milestone document, linking detailed evidence.
- A successful compile alone is not proof of in-game behavior.
- Report which checks were actually run (build, dev client, integrated server, dedicated server, manual checks), and which were not.
- If running Minecraft is unavailable, provide exact local testing instructions and honestly report incomplete validation.
- Don't call the milestone complete while a required acceptance test is known to fail.
- Preserve a clean, reviewable diff; do not overwrite user changes. Explain any unusual configuration edits.

## Handoff protocol

Follow **[docs/RULES.md — Handoff rules](docs/RULES.md#handoff-rules)**. After authorized work, append a **concise dated implementation and verification note to the bottom of that milestone's own `MILESTONE_XX.md`**; do not produce a standalone report file or a detailed report inside the roadmap. Preserve existing M01–M07 reports as historical evidence, and leave independent review/SHIPPED status to the owner.

**Documentation:** `docs/ROADMAP.md` = status, `docs/RULES.md` = lifecycle/workflow policy, `docs/BACKLOG.md` = ALL unnumbered future ideas, and `docs/MILESTONE_XX.md` = scope plus final handoff. The central decisions ledger is retired and remains an archive only. Do not recreate `docs/FUTURE_WORK.md`.

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
- **Results so far:** `docs/TEST_CHECKLIST_01.md`, `docs/archive/milestone-reports/MILESTONE_01_REPORT.md`, `docs/evidence/`.
