# Playtesting on Windows: one-click updates

After a change is pushed to `main`, GitHub builds and tests the mod. Double-clicking **Update Simple Block** on your desktop then installs that build into your Simple Block game directory. No manual downloading, renaming or copying.

```
push to main ─► GitHub Actions "Build" (build + game tests, ~5 min) ─► download "simple-block-mod"
                                                                            │
double-click "Update Simple Block" ◄────────────────────────────────────────┘
   └─► checks the download, swaps only the Simple Block jar in <GameDir>\mods ─► open launcher ─► Play
```

## One-time setup

### 1. A Fabric installation with its own Game Directory

You only need this once; skip any part you have already done.

1. Download the Fabric installer for Windows from <https://fabricmc.net/use/installer/> and run it.
2. On the **Client** tab choose Minecraft **26.3** and the newest Loader (0.19.5 or later). Leave **Install location** at its default (your `.minecraft` folder) and keep **Create profile** ticked. Click **Install**.
3. Open the Minecraft Launcher → **Installations** → the new `fabric-loader-…-26.3` entry → **Edit**:
   - **Name:** `Simple Block`
   - **Game directory:** Browse to your Simple Block folder, e.g. `C:\Users\<you>\AppData\Minecraft-Instances\SimpleBlock`. This keeps its mods, settings and worlds separate from normal Minecraft.
   - **Save**.
4. Put **Fabric API** for 26.3 (from <https://modrinth.com/mod/fabric-api>) in that folder's `mods` folder. The updater never touches Fabric API or any other mod.

### 2. GitHub CLI (signs the updater in to the private repository)

1. Open **Terminal** or **PowerShell** and run:
   ```
   winget install --id GitHub.cli
   ```
   (or use the installer from <https://cli.github.com>).
2. Close that window, open a new one, and run:
   ```
   gh auth login
   ```
   Choose **GitHub.com** → **HTTPS** → **Y** (authenticate Git) → **Login with a web browser**. Copy the one-time code, approve in the browser, and use the GitHub account that can see `boxofdog/simple_block`.
3. Check with `gh auth status`. It should say "Logged in to github.com".

If you skip this, the updater offers to start the sign-in for you.

### 3. Get the updater

1. On GitHub, open the repository → **Code** → **Download ZIP**. You need to be signed in, since the repository is private.
2. Before extracting, right-click the ZIP → **Properties** → tick **Unblock** → **OK**. This prevents "Windows protected your PC" warnings.
3. Copy the `tools\windows` folder from the ZIP to a permanent place, for example `C:\Users\<you>\SimpleBlockUpdater`. It holds `Update-SimpleBlock.bat`, `Update-SimpleBlock.ps1` and `Update-SimpleBlock.settings.example.ini`. The `tests` subfolder is optional.

You only need to repeat this if the updater itself changes. Your settings file is not part of the ZIP, so it won't be overwritten.

### 4. Tell it where your game directory is

The default is `%APPDATA%\Minecraft-Instances\SimpleBlock`, which is `C:\Users\<you>\AppData\Roaming\Minecraft-Instances\SimpleBlock` (note the **Roaming**). If your folder is somewhere else, set it:

1. In the updater folder, copy `Update-SimpleBlock.settings.example.ini` to `Update-SimpleBlock.settings.ini`.
2. Open it with Notepad and change the `GameDir` line to the same folder as the launcher's **Game directory**. For a folder directly under `AppData` (no `Roaming`), that is:
   ```
   GameDir = %USERPROFILE%\AppData\Minecraft-Instances\SimpleBlock
   ```
   Plain paths work too. No quotes or doubled backslashes needed.

If you skip this step and the default folder doesn't exist, the updater looks in a few usual places. If it finds a `Minecraft-Instances\SimpleBlock` folder, it asks before using it and remembers your answer.

### 5. Desktop shortcut

Right-click `Update-SimpleBlock.bat` → **Show more options** → **Send to** → **Desktop (create shortcut)**. Rename the shortcut to **Update Simple Block**. Keep the `.bat`, `.ps1` and settings file together in the updater folder; the shortcut points at them.

## Everyday use

1. After a change is pushed, wait for the build to finish (about 5 minutes; you can watch it on GitHub under **Actions**).
2. Quit Minecraft. The updater refuses to swap the jar while the Simple Block installation is running.
3. Double-click **Update Simple Block** and read the window:
   - `Installed Simple Block 0.1.0+build.12.abc1234 (commit abc1234).`: first install.
   - `Updated Simple Block: 0.1.0+build.11.def5678 -> 0.1.0+build.12.abc1234`: an update happened.
   - `Simple Block is already up to date: …`: nothing to do; nothing was touched.
   - A yellow `!` line about a newer build still running or failed: you got the newest *working* build. Run it again later, or report the failure.
4. Answer **Y** to open the Minecraft Launcher. **Then pick the Simple Block installation** in the drop-down next to Play; the launcher doesn't switch to it by itself.

The version string tells you exactly what you are playing: `0.1.0+build.12.abc1234` is build #12 of commit `abc1234`. It also appears in Minecraft's log and in mod lists.

## When something goes wrong

Whenever the updater stops early, it says so and leaves your mods folder exactly as it was.

| Message | What to do |
|---|---|
| GitHub CLI (gh) is not installed | Do step 2. |
| GitHub CLI is not signed in | Run `gh auth login` (step 2), or answer Y when the updater offers. |
| Couldn't get the build list … (HTTP 404) | The signed-in GitHub account can't see the repository. Check `gh auth status`. |
| No successful build of main found | Check GitHub → Actions; the recent builds failed. |
| The download … has expired | GitHub keeps builds for 90 days. Push a change, or GitHub → Actions → Build → **Run workflow**, then retry. |
| Minecraft is running from … / … is in use | Quit Minecraft completely, then run the updater again. |
| Game directory not found | Do step 4. |
| Refusing to install … | The download failed a safety check (wrong mod, wrong commit or a corrupted file). Nothing was installed. Report it. |
| "Windows protected your PC" | Unblock as in step 3, or click **More info → Run anyway**. |

**Going back to the previous build:** the last three replaced jars are kept in `%LOCALAPPDATA%\SimpleBlockUpdater\backups`. Quit Minecraft, delete the Simple Block jar from `mods`, and copy the older jar in. The next updater run will install the newest build again.

## Settings reference

`Update-SimpleBlock.settings.ini` (all optional; `#` starts a comment):

| Key | Default | Meaning |
|---|---|---|
| `GameDir` | `%APPDATA%\Minecraft-Instances\SimpleBlock` | Game directory of the Simple Block installation. Only its `mods` folder is changed. |
| `OpenLauncher` | `ask` | `ask`, `always` or `never` open the Minecraft Launcher after a successful run. |
| `LauncherPath` | (auto) | Full path to `MinecraftLauncher.exe`, if it isn't found automatically. |
| `GhPath` | (auto) | Full path to `gh.exe`, if it isn't on PATH. |
| `Repository`, `Branch`, `Workflow`, `ArtifactName` | `boxofdog/simple_block`, `main`, `build.yml`, `simple-block-mod` | Where builds come from. |

The same names work as parameters, e.g. `Update-SimpleBlock.bat -Force` reinstalls the current build. Exit codes are `0` (updated or up to date), `1` (failed), `2` (no usable build), `3` (Minecraft running) and `4` (setup problem).

## What has and hasn't been verified

Verified automatically:

- **CI:** the GitHub "Build" workflow builds the jar, runs the game tests and publishes it with `build-info.json`.
- **Production jar:** the CI-style jar loads in a stock Fabric 26.3 install alongside Fabric API.
- **Updater logic:** 29 scenario tests with GitHub faked, on PowerShell 7 (Linux). They cover install, up to date, replacement with backup, jars found by mod id, sources/dev jars ignored, six kinds of bad downloads refused, unavailable/expired/failed builds, failed downloads, rollback after a failed swap, a missing game directory, settings, and command-line matching. Each safety check was also broken on purpose to confirm a test catches it.
- **Real Windows:** the "Windows updater" workflow runs the same tests on Windows in **Windows PowerShell 5.1** (what the `.bat` uses) and PowerShell 7. That includes Windows file locking, and a real Java process that holds the jar open with `--gameDir` on its command line, like Minecraft. It also checks that the `.bat` passes arguments and exit codes through.
- **Static analysis:** PSScriptAnalyzer reports no Windows PowerShell 5.1 compatibility problems.

Not verifiable outside your PC (please watch for these on first use):

- The real `gh` download from the private repository with your login. The test suite fakes GitHub's answers, using the formats the GitHub API documents.
- Finding and opening the Minecraft Launcher, both the classic `MinecraftLauncher.exe` and the Microsoft Store/Xbox app version.
- Double-clicking from Explorer, SmartScreen/Unblock prompts and the desktop shortcut.
- Your Fabric installation and Game Directory, and the real launcher's `javaw.exe` command line. The running-Minecraft check matches the `--gameDir` argument the launcher passes, and the jar-in-use check backs it up.
