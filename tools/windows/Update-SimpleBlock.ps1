<#
.SYNOPSIS
    Installs the latest successful Simple Block build from GitHub into your Minecraft game directory.

.DESCRIPTION
    Normally started by double-clicking Update-SimpleBlock.bat. It:
      1. finds the newest successful "Build" workflow run on the main branch, using your GitHub CLI login;
      2. downloads that run's "simple-block-mod" artifact and checks it is the genuine Simple Block jar
         (mod id, version, commit and SHA-256 must all match the build record);
      3. replaces only Simple Block jars in <GameDir>\mods. Other mods, configs and worlds are untouched;
      4. moves the previous jar to a backup first and puts it back if anything goes wrong.

    Settings are taken from parameters, then from Update-SimpleBlock.settings.ini next to this script,
    then from the built-in defaults. One-time setup: docs/PLAYTESTING_SETUP.md in the repository.

    Exit codes: 0 updated or already up to date, 1 failed, 2 no usable build on GitHub,
    3 Minecraft is using the mods folder, 4 setup problem (game directory, GitHub CLI or login).

.PARAMETER GameDir
    Minecraft game directory of the Simple Block installation (the folder that contains "mods").
    Environment variables such as %APPDATA% are expanded. Default: %APPDATA%\Minecraft-Instances\SimpleBlock

.PARAMETER OpenLauncher
    ask (default), always or never: whether to open the Minecraft Launcher after a successful run.

.PARAMETER Force
    Reinstall even if the installed jar is already the latest build.

.PARAMETER NonInteractive
    Never prompt (used by tests and scheduled runs).
#>
[CmdletBinding()]
param(
    [string]$GameDir,
    [string]$Repository,
    [string]$Branch,
    [string]$Workflow,
    [string]$ArtifactName,
    [ValidateSet('', 'ask', 'always', 'never')]
    [string]$OpenLauncher,
    [string]$LauncherPath,
    [string]$GhPath,
    [string]$SettingsPath,
    [string]$WorkDir,
    [switch]$Force,
    [switch]$NonInteractive
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$script:ModId = 'simple_block'
$script:SettingKeys = @('GameDir', 'Repository', 'Branch', 'Workflow', 'ArtifactName', 'OpenLauncher', 'LauncherPath', 'GhPath')
$script:Defaults = @{
    GameDir      = '%APPDATA%\Minecraft-Instances\SimpleBlock'
    Repository   = 'boxofdog/simple_block'
    Branch       = 'main'
    Workflow     = 'build.yml'
    ArtifactName = 'simple-block-mod'
    OpenLauncher = 'ask'
    LauncherPath = ''
    GhPath       = ''
}
$script:ExitOk = 0
$script:ExitFailed = 1
$script:ExitNoBuild = 2
$script:ExitMinecraftRunning = 3
$script:ExitSetup = 4
$script:BackupsToKeep = 3
$script:GhExe = $null

#region Output and small helpers

function Write-Info([string]$Text) { Write-Host "  $Text" }
function Write-Detail([string]$Text) { Write-Host "  $Text" -ForegroundColor DarkGray }
function Write-Warning2([string]$Text) { Write-Host "  ! $Text" -ForegroundColor Yellow }

function Test-IsWindows {
    return [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
}

function Test-Interactive([bool]$NonInteractiveSwitch) {
    if ($NonInteractiveSwitch) { return $false }
    try {
        return [Environment]::UserInteractive -and -not [Console]::IsInputRedirected
    } catch {
        return $false
    }
}

# Throws an error that carries the exit code to report. ChangedFiles marks the rare case where the
# mods folder could not be fully restored, so the closing "not changed" line must not be printed.
function Stop-Update([int]$Code, [string]$Message, [bool]$ChangedFiles = $false) {
    $exception = New-Object System.Exception $Message
    $exception.Data['ExitCode'] = $Code
    $exception.Data['ChangedFiles'] = $ChangedFiles
    throw $exception
}

function Join-PathIfSet([string]$Base, [string]$Child) {
    if ([string]::IsNullOrEmpty($Base)) { return $null }
    return Join-Path $Base $Child
}

function Read-YesNo([string]$Question, [bool]$DefaultYes) {
    $suffix = if ($DefaultYes) { '[Y/n]' } else { '[y/N]' }
    $answer = Read-Host "  $Question $suffix"
    if ([string]::IsNullOrWhiteSpace($answer)) { return $DefaultYes }
    return $answer.Trim().ToLowerInvariant().StartsWith('y')
}

function Get-ShortSha([string]$Sha) {
    if ($Sha.Length -gt 7) { return $Sha.Substring(0, 7) }
    return $Sha
}

# GitHub timestamps arrive as strings (Windows PowerShell) or DateTime (PowerShell 7).
function Format-Timestamp($Value) {
    if ($null -eq $Value) { return 'unknown time' }
    if ($Value -is [datetime]) {
        $date = $Value
    } else {
        $date = [datetime]::Parse([string]$Value, [System.Globalization.CultureInfo]::InvariantCulture, [System.Globalization.DateTimeStyles]::RoundtripKind)
    }
    return $date.ToLocalTime().ToString('yyyy-MM-dd HH:mm')
}

function Get-Sha256([string]$Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

#endregion

#region Settings

function Read-SettingsFile([string]$Path) {
    $settings = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $settings }

    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#') -or $trimmed.StartsWith(';') -or $trimmed.StartsWith('[')) { continue }
        $equals = $trimmed.IndexOf('=')
        if ($equals -lt 1) {
            Write-Warning2 "Ignoring unreadable line in ${Path}: $trimmed"
            continue
        }
        $key = $trimmed.Substring(0, $equals).Trim()
        $value = $trimmed.Substring($equals + 1).Trim()
        if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($script:SettingKeys -notcontains $key) {
            Write-Warning2 "Ignoring unknown setting '$key' in $Path"
            continue
        }
        $settings[$key] = $value
    }
    return $settings
}

function Set-SettingInFile([string]$Path, [string]$Key, [string]$Value) {
    $lines = New-Object System.Collections.Generic.List[string]
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $lines.AddRange([string[]][System.IO.File]::ReadAllLines($Path))
    }
    $replaced = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match ('^\s*' + [regex]::Escape($Key) + '\s*=')) {
            $lines[$i] = "$Key = $Value"
            $replaced = $true
            break
        }
    }
    if (-not $replaced) { $lines.Add("$Key = $Value") }
    [System.IO.File]::WriteAllLines($Path, $lines.ToArray())
}

# Parameters win over the settings file, which wins over the defaults.
function Resolve-UpdaterSettings([hashtable]$Bound) {
    $settingsPath = $Bound['SettingsPath']
    if ([string]::IsNullOrWhiteSpace($settingsPath)) {
        $settingsPath = Join-Path $PSScriptRoot 'Update-SimpleBlock.settings.ini'
    }
    $fromFile = Read-SettingsFile $settingsPath

    $resolved = @{ SettingsPath = $settingsPath; Sources = @{} }
    foreach ($key in $script:SettingKeys) {
        if ($Bound.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace([string]$Bound[$key])) {
            $resolved[$key] = [string]$Bound[$key]
            $resolved.Sources[$key] = 'parameter'
        } elseif ($fromFile.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace($fromFile[$key])) {
            $resolved[$key] = $fromFile[$key]
            $resolved.Sources[$key] = 'settings file'
        } else {
            $resolved[$key] = $script:Defaults[$key]
            $resolved.Sources[$key] = 'default'
        }
    }

    $resolved.OpenLauncher = $resolved.OpenLauncher.ToLowerInvariant()
    if (@('ask', 'always', 'never') -notcontains $resolved.OpenLauncher) {
        Stop-Update $script:ExitSetup "OpenLauncher must be ask, always or never (got '$($resolved.OpenLauncher)')."
    }

    $workDir = $Bound['WorkDir']
    if ([string]::IsNullOrWhiteSpace($workDir)) {
        $workDir = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'SimpleBlockUpdater'
    }
    $resolved.WorkDir = $workDir
    return $resolved
}

function Expand-UserPath([string]$Path) {
    $expanded = [Environment]::ExpandEnvironmentVariables($Path)
    if ($expanded.StartsWith('~')) {
        $expanded = Join-Path ([Environment]::GetFolderPath('UserProfile')) $expanded.Substring(1).TrimStart('\', '/')
    }
    return [System.IO.Path]::GetFullPath($expanded)
}

#endregion

#region Game directory and installed jars

# Other places a "Minecraft-Instances\SimpleBlock" folder is commonly created by hand.
function Get-GameDirCandidates {
    $candidates = @()
    if (Test-IsWindows) {
        if ($env:USERPROFILE) { $candidates += Join-Path $env:USERPROFILE 'AppData\Minecraft-Instances\SimpleBlock' }
        if ($env:LOCALAPPDATA) { $candidates += Join-Path $env:LOCALAPPDATA 'Minecraft-Instances\SimpleBlock' }
        if ($env:USERPROFILE) { $candidates += Join-Path $env:USERPROFILE 'Minecraft-Instances\SimpleBlock' }
    }
    return @($candidates | Where-Object { Test-Path -LiteralPath $_ -PathType Container })
}

function Resolve-GameDirectory($Settings, [bool]$Interactive) {
    $gameDir = Expand-UserPath $Settings.GameDir
    if (Test-Path -LiteralPath $gameDir -PathType Container) { return $gameDir }

    $message = "Game directory not found: $gameDir"
    if ($Settings.Sources.GameDir -eq 'default') {
        $found = @(Get-GameDirCandidates)
        if ($found.Count -gt 0) {
            $candidate = $found[0]
            Write-Warning2 $message
            Write-Info "Found a Simple Block game directory at $candidate"
            if ($Interactive -and (Read-YesNo 'Use that folder and remember it?' $true)) {
                Set-SettingInFile $Settings.SettingsPath 'GameDir' $candidate
                Write-Detail "Saved GameDir to $($Settings.SettingsPath)"
                return $candidate
            }
            Stop-Update $script:ExitSetup "$message. If your Simple Block installation uses $candidate, add this line to $($Settings.SettingsPath):`n    GameDir = $candidate"
        }
    }
    Stop-Update $script:ExitSetup "$message. Set GameDir in $($Settings.SettingsPath) to the Game Directory of your Simple Block installation (Minecraft Launcher > Installations > Edit)."
}

# Reads id and version from a jar's fabric.mod.json. Returns $null for anything that isn't a readable Fabric mod.
function Get-JarModInfo([string]$Path) {
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            $entry = $zip.GetEntry('fabric.mod.json')
            if ($null -eq $entry) { return $null }
            $reader = New-Object System.IO.StreamReader($entry.Open())
            try { $json = $reader.ReadToEnd() } finally { $reader.Dispose() }
        } finally {
            $zip.Dispose()
        }
        $info = $json | ConvertFrom-Json
        return [pscustomobject]@{ Id = [string]$info.id; Version = [string]$info.version }
    } catch {
        return $null
    }
}

function Test-IsAuxiliaryJar([string]$Name) {
    return $Name -match '-(sources|dev|javadoc)\.jar$'
}

# Every jar directly in mods whose fabric.mod.json says it is Simple Block, whatever the file is called.
function Get-InstalledSimpleBlockJars([string]$ModsDir) {
    $jars = @()
    foreach ($file in @(Get-ChildItem -LiteralPath $ModsDir -File | Where-Object { $_.Name -like '*.jar' })) {
        $mod = Get-JarModInfo $file.FullName
        if ($null -ne $mod -and $mod.Id -eq $script:ModId) {
            $jars += [pscustomobject]@{ File = $file.FullName; Name = $file.Name; Version = $mod.Version }
        }
    }
    return $jars
}

function Remove-StalePartialFiles([string]$ModsDir) {
    foreach ($file in @(Get-ChildItem -LiteralPath $ModsDir -File -Force | Where-Object { $_.Name -like '.simple_block-update-*.partial' })) {
        Remove-Item -LiteralPath $file.FullName -Force
    }
}

#endregion

#region Minecraft running checks

# True when a java command line launches Minecraft with --gameDir pointing at GameDir.
function Test-CommandLineUsesGameDir([string]$CommandLine, [string]$GameDir) {
    if ([string]::IsNullOrEmpty($CommandLine) -or [string]::IsNullOrEmpty($GameDir)) { return $false }
    $dir = $GameDir.TrimEnd('\', '/')
    $pattern = '--gameDir\s+"?' + [regex]::Escape($dir) + '[\\/]?(?:"|\s|$)'
    return [regex]::IsMatch($CommandLine, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

function Get-MinecraftProcess([string]$GameDir) {
    if (-not (Test-IsWindows)) { return @() }
    try {
        $processes = Get-CimInstance -ClassName Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" -ErrorAction Stop
    } catch {
        return @()
    }
    return @($processes | Where-Object { Test-CommandLineUsesGameDir $_.CommandLine $GameDir })
}

function Assert-MinecraftNotRunning([string]$GameDir) {
    $running = @(Get-MinecraftProcess $GameDir)
    if ($running.Count -gt 0) {
        Stop-Update $script:ExitMinecraftRunning "Minecraft is running from $GameDir (process $(@($running | ForEach-Object { $_.ProcessId }) -join ', ')). Quit Minecraft, then run the updater again."
    }
}

# A running game keeps its mod jars open, which blocks replacing them.
function Test-FileLocked([string]$Path) {
    try {
        $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $stream.Dispose()
        return $false
    } catch [System.IO.IOException] {
        return $true
    }
}

#endregion

#region GitHub CLI

function ConvertTo-CommandLineArgument([string]$Value) {
    if ($Value -eq '') { return '""' }
    if ($Value -notmatch '[\s"]') { return $Value }
    $escaped = [regex]::Replace($Value, '(\\*)"', { param($m) $m.Groups[1].Value + $m.Groups[1].Value + '\"' })
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}

# Runs a program and captures its output without PowerShell's native-command stderr quirks.
function Invoke-Native([string]$FilePath, [string[]]$Arguments) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = (@($Arguments | ForEach-Object { ConvertTo-CommandLineArgument $_ }) -join ' ')
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $startInfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8
    $startInfo.EnvironmentVariables['GH_PROMPT_DISABLED'] = '1'
    $startInfo.EnvironmentVariables['NO_COLOR'] = '1'

    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    return [pscustomobject]@{ ExitCode = $process.ExitCode; StdOut = $stdout.Result; StdErr = $stderr.Result }
}

function Invoke-Gh([string[]]$Arguments) {
    return Invoke-Native $script:GhExe $Arguments
}

function Find-GitHubCli([string]$Configured) {
    if (-not [string]::IsNullOrWhiteSpace($Configured)) {
        $path = Expand-UserPath $Configured
        if (Test-Path -LiteralPath $path -PathType Leaf) { return $path }
        Stop-Update $script:ExitSetup "GhPath is set to $path, but that file doesn't exist."
    }
    $command = Get-Command gh -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $command) { return $command.Path }
    foreach ($candidate in @(
            (Join-PathIfSet $env:ProgramFiles 'GitHub CLI\gh.exe'),
            (Join-PathIfSet ${env:ProgramFiles(x86)} 'GitHub CLI\gh.exe'),
            (Join-PathIfSet $env:LOCALAPPDATA 'Programs\GitHub CLI\gh.exe'))) {
        if ($null -ne $candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) { return $candidate }
    }
    Stop-Update $script:ExitSetup "GitHub CLI (gh) is not installed. Install it once with:`n    winget install --id GitHub.cli`nthen sign in with:  gh auth login"
}

function Assert-GitHubLogin([bool]$Interactive) {
    $status = Invoke-Gh @('auth', 'status', '--hostname', 'github.com')
    if ($status.ExitCode -eq 0) { return }

    Write-Warning2 'GitHub CLI is not signed in to github.com.'
    if ($Interactive -and (Read-YesNo 'Sign in now? (opens your browser)' $true)) {
        # Runs attached to this console so gh can show its one-time code and questions.
        Start-Process -FilePath $script:GhExe -ArgumentList @('auth', 'login', '--hostname', 'github.com', '--git-protocol', 'https', '--web') -NoNewWindow -Wait
        $status = Invoke-Gh @('auth', 'status', '--hostname', 'github.com')
        if ($status.ExitCode -eq 0) { return }
    }
    Stop-Update $script:ExitSetup "Sign in once with:  gh auth login   (choose GitHub.com, HTTPS, login with a web browser). Use an account that can see the repository."
}

function Get-GhJson([string[]]$Arguments, [string]$What) {
    $result = Invoke-Gh $Arguments
    if ($result.ExitCode -ne 0) {
        $detail = ($result.StdErr + ' ' + $result.StdOut).Trim()
        Stop-Update $script:ExitNoBuild "Couldn't get $What from GitHub: $detail"
    }
    try {
        return $result.StdOut | ConvertFrom-Json
    } catch {
        Stop-Update $script:ExitNoBuild "GitHub returned something unexpected for ${What}: $($_.Exception.Message)"
    }
}

# Newest run of the build workflow on the branch (any state) and the newest successful one.
function Get-BuildRuns($Settings) {
    $data = Get-GhJson @('api', '-X', 'GET', "repos/$($Settings.Repository)/actions/workflows/$($Settings.Workflow)/runs",
        '-f', "branch=$($Settings.Branch)", '-f', 'per_page=20') "the build list for $($Settings.Repository)"
    $runs = @($data.workflow_runs | Where-Object {
            $_.head_branch -eq $Settings.Branch -and ($_.event -eq 'push' -or $_.event -eq 'workflow_dispatch')
        })
    $success = @($runs | Where-Object { $_.status -eq 'completed' -and $_.conclusion -eq 'success' })
    $newest = $null
    if ($runs.Count -gt 0) { $newest = $runs[0] }
    $latestSuccess = $null
    if ($success.Count -gt 0) { $latestSuccess = $success[0] }
    return [pscustomobject]@{ Newest = $newest; LatestSuccess = $latestSuccess }
}

function Get-RunArtifact($Settings, $Run) {
    $data = Get-GhJson @('api', '-X', 'GET', "repos/$($Settings.Repository)/actions/runs/$($Run.id)/artifacts", '-f', 'per_page=100') "the files of build #$($Run.run_number)"
    $artifact = @($data.artifacts | Where-Object { $_.name -eq $Settings.ArtifactName }) | Select-Object -First 1
    if ($null -eq $artifact) {
        Stop-Update $script:ExitNoBuild "Build #$($Run.run_number) has no '$($Settings.ArtifactName)' download."
    }
    if ($artifact.expired) {
        Stop-Update $script:ExitNoBuild "The download for build #$($Run.run_number) has expired (GitHub keeps them for 90 days). Push a commit or re-run the Build workflow, then try again."
    }
    return $artifact
}

function Save-RunArtifact($Settings, $Run, [string]$Destination) {
    if (Test-Path -LiteralPath $Destination) { Remove-Item -LiteralPath $Destination -Recurse -Force }
    $null = New-Item -ItemType Directory -Path $Destination -Force
    $result = Invoke-Gh @('run', 'download', [string]$Run.id, '-R', $Settings.Repository, '-n', $Settings.ArtifactName, '-D', $Destination)
    if ($result.ExitCode -ne 0) {
        Stop-Update $script:ExitFailed "Downloading build #$($Run.run_number) failed: $(($result.StdErr + ' ' + $result.StdOut).Trim())"
    }
}

#endregion

#region Validation and install

# Picks the mod jar named in build-info.json and proves it is the Simple Block build for this exact commit.
function Get-VerifiedArtifactJar([string]$Directory, [string]$ExpectedCommit) {
    $infoFile = @(Get-ChildItem -LiteralPath $Directory -Recurse -File -Filter 'build-info.json') | Select-Object -First 1
    if ($null -eq $infoFile) {
        Stop-Update $script:ExitFailed 'The download has no build-info.json, so it is not a Simple Block build.'
    }
    try {
        $info = [System.IO.File]::ReadAllText($infoFile.FullName) | ConvertFrom-Json
        $declared = @{ ModId = [string]$info.modId; Version = [string]$info.version; Jar = [string]$info.jar; Sha256 = [string]$info.sha256; Commit = [string]$info.commit }
    } catch {
        Stop-Update $script:ExitFailed "The download's build-info.json is unreadable: $($_.Exception.Message)."
    }

    $folder = $infoFile.DirectoryName
    foreach ($jar in @(Get-ChildItem -LiteralPath $folder -File | Where-Object { $_.Name -like '*.jar' -and $_.Name -ne $declared.Jar })) {
        Write-Detail "Ignoring $($jar.Name) (not the mod jar named in build-info.json)"
    }
    if ((Test-IsAuxiliaryJar $declared.Jar) -or [string]::IsNullOrWhiteSpace($declared.Jar)) {
        Stop-Update $script:ExitFailed "build-info.json points at '$($declared.Jar)', which is not a mod jar."
    }
    $jarPath = Join-Path $folder $declared.Jar
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        Stop-Update $script:ExitFailed "The download is missing $($declared.Jar)."
    }

    $problems = @()
    if ($declared.ModId -ne $script:ModId) { $problems += "build-info.json is for '$($declared.ModId)'" }
    if ($declared.Commit -ne $ExpectedCommit) { $problems += "it was built from commit $(Get-ShortSha $declared.Commit), not $(Get-ShortSha $ExpectedCommit)" }
    $sha256 = Get-Sha256 $jarPath
    if ($sha256 -ne $declared.Sha256.ToLowerInvariant()) { $problems += 'its SHA-256 does not match build-info.json' }
    $mod = Get-JarModInfo $jarPath
    if ($null -eq $mod) {
        $problems += 'it is not a readable Fabric mod'
    } else {
        if ($mod.Id -ne $script:ModId) { $problems += "its mod id is '$($mod.Id)'" }
        if ($mod.Version -ne $declared.Version) { $problems += "its version is '$($mod.Version)', expected '$($declared.Version)'" }
    }
    if ($problems.Count -gt 0) {
        Stop-Update $script:ExitFailed "Refusing to install $($declared.Jar): $($problems -join '; ')."
    }

    return [pscustomobject]@{ Path = $jarPath; Name = $declared.Jar; Version = $declared.Version; Sha256 = $sha256; Info = $info }
}

function Move-JarFile([string]$From, [string]$To) {
    [System.IO.File]::Move($From, $To)
}

# Swaps the jars so that a failure at any step leaves the previous installation in place.
function Install-SimpleBlockJar($Jar, [object[]]$Installed, [string]$ModsDir, [string]$BackupRoot) {
    $target = Join-Path $ModsDir $Jar.Name
    $targetIsOld = @($Installed | Where-Object { $_.File -eq $target }).Count -gt 0
    if ((Test-Path -LiteralPath $target) -and -not $targetIsOld) {
        Stop-Update $script:ExitFailed "$target already exists and is not a Simple Block jar. Move it away and try again."
    }
    foreach ($old in $Installed) {
        if (Test-FileLocked $old.File) {
            Stop-Update $script:ExitMinecraftRunning "$($old.Name) is in use, probably by Minecraft. Quit Minecraft, then run the updater again."
        }
    }

    # Copy next to the target under a name Fabric ignores, so the final step is a same-folder rename.
    $partial = Join-Path $ModsDir ('.simple_block-update-' + [guid]::NewGuid().ToString('N') + '.partial')
    Copy-Item -LiteralPath $Jar.Path -Destination $partial
    if ((Get-Sha256 $partial) -ne $Jar.Sha256) {
        Remove-Item -LiteralPath $partial -Force
        Stop-Update $script:ExitFailed 'The copied jar does not match the download (disk problem?).'
    }

    $backupDir = $null
    $moved = New-Object System.Collections.Generic.List[object]
    try {
        if (@($Installed).Count -gt 0) {
            $backupDir = Join-Path $BackupRoot (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
            $null = New-Item -ItemType Directory -Path $backupDir -Force
            foreach ($old in $Installed) {
                $backupPath = Join-Path $backupDir $old.Name
                Move-JarFile $old.File $backupPath
                $moved.Add([pscustomobject]@{ From = $old.File; To = $backupPath })
            }
        }
        Move-JarFile $partial $target
    } catch {
        $reason = $_.Exception.Message
        if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue }
        $restoreFailures = @()
        for ($i = $moved.Count - 1; $i -ge 0; $i--) {
            try {
                Move-JarFile $moved[$i].To $moved[$i].From
            } catch {
                $restoreFailures += $moved[$i].To
            }
        }
        if ($restoreFailures.Count -gt 0) {
            Stop-Update $script:ExitFailed "Install failed ($reason) and the previous jar could not be put back automatically. Copy it from $($restoreFailures -join ', ') into $ModsDir." $true
        }
        Stop-Update $script:ExitFailed "Install failed ($reason). Your previous Simple Block jar was put back."
    }
    return $backupDir
}

function Remove-OldBackups([string]$BackupRoot) {
    if (-not (Test-Path -LiteralPath $BackupRoot -PathType Container)) { return }
    $folders = @(Get-ChildItem -LiteralPath $BackupRoot -Directory | Sort-Object Name -Descending)
    foreach ($folder in @($folders | Select-Object -Skip $script:BackupsToKeep)) {
        Remove-Item -LiteralPath $folder.FullName -Recurse -Force -ErrorAction SilentlyContinue
    }
}

#endregion

#region Minecraft Launcher

function Find-MinecraftLauncher([string]$Configured) {
    if (-not [string]::IsNullOrWhiteSpace($Configured)) {
        $path = Expand-UserPath $Configured
        if (Test-Path -LiteralPath $path -PathType Leaf) { return [pscustomobject]@{ Kind = 'exe'; Target = $path } }
        Write-Warning2 "LauncherPath is set to $path, but that file doesn't exist."
    }
    if (-not (Test-IsWindows)) { return $null }

    foreach ($path in @(
            (Join-PathIfSet ${env:ProgramFiles(x86)} 'Minecraft Launcher\MinecraftLauncher.exe'),
            (Join-PathIfSet $env:ProgramFiles 'Minecraft Launcher\MinecraftLauncher.exe'))) {
        if ($null -ne $path -and (Test-Path -LiteralPath $path -PathType Leaf)) { return [pscustomobject]@{ Kind = 'exe'; Target = $path } }
    }

    # Microsoft Store / Xbox app version of the launcher.
    try {
        $package = Get-AppxPackage -Name 'Microsoft.4297127D64EC6' -ErrorAction Stop | Select-Object -First 1
        if ($null -ne $package) {
            $appId = @((Get-AppxPackageManifest -Package $package).Package.Applications.Application)[0].Id
            return [pscustomobject]@{ Kind = 'appx'; Target = "shell:AppsFolder\$($package.PackageFamilyName)!$appId" }
        }
    } catch {
        Write-Detail "Couldn't look up the Store version of the launcher: $($_.Exception.Message)"
    }
    return $null
}

function Open-MinecraftLauncherIfWanted($Settings, [bool]$Interactive, [string]$GameDir) {
    $open = $false
    if ($Settings.OpenLauncher -eq 'always') { $open = $true }
    if ($Settings.OpenLauncher -eq 'ask' -and $Interactive) { $open = Read-YesNo 'Open the Minecraft Launcher now?' $true }
    if (-not $open) { return }

    $launcher = Find-MinecraftLauncher $Settings.LauncherPath
    if ($null -eq $launcher) {
        Write-Warning2 "Couldn't find the Minecraft Launcher. Open it from the Start menu (or set LauncherPath in $($Settings.SettingsPath))."
        return
    }
    if ($launcher.Kind -eq 'exe') {
        Start-Process -FilePath $launcher.Target
    } else {
        Start-Process -FilePath 'explorer.exe' -ArgumentList $launcher.Target
    }
    Write-Info 'Opened the Minecraft Launcher.'
    Write-Warning2 "Before pressing Play, pick your Simple Block installation (Game Directory $GameDir) in the launcher. It does not switch to it by itself."
}

# Opening the launcher is a convenience; a problem there must not turn a good update into a failure.
function Open-LauncherSafely($Settings, [bool]$Interactive, [string]$GameDir) {
    try {
        Open-MinecraftLauncherIfWanted $Settings $Interactive $GameDir
    } catch {
        Write-Warning2 "Couldn't open the Minecraft Launcher: $($_.Exception.Message)"
    }
}

#endregion

function Invoke-SimpleBlockUpdate {
    [CmdletBinding()]
    param(
        [string]$GameDir,
        [string]$Repository,
        [string]$Branch,
        [string]$Workflow,
        [string]$ArtifactName,
        [string]$OpenLauncher,
        [string]$LauncherPath,
        [string]$GhPath,
        [string]$SettingsPath,
        [string]$WorkDir,
        [switch]$Force,
        [switch]$NonInteractive
    )

    $interactive = Test-Interactive $NonInteractive.IsPresent
    $stagingDir = $null
    $changedFiles = $false
    try {
        Write-Host ''
        Write-Host 'Simple Block updater' -ForegroundColor Cyan
        $settings = Resolve-UpdaterSettings $PSBoundParameters

        $gameDirPath = Resolve-GameDirectory $settings $interactive
        $modsDir = Join-Path $gameDirPath 'mods'
        Write-Info "Game directory: $gameDirPath ($($settings.Sources.GameDir))"
        if (-not (Test-Path -LiteralPath $modsDir -PathType Container)) {
            $null = New-Item -ItemType Directory -Path $modsDir
            Write-Detail "Created $modsDir"
        }
        Remove-StalePartialFiles $modsDir

        $script:GhExe = Find-GitHubCli $settings.GhPath
        Assert-GitHubLogin $interactive
        Assert-MinecraftNotRunning $gameDirPath

        $installed = @(Get-InstalledSimpleBlockJars $modsDir)
        if ($installed.Count -eq 0) {
            Write-Info 'Installed now: no Simple Block jar'
        } else {
            foreach ($jar in $installed) { Write-Info "Installed now: $($jar.Name) (version $($jar.Version))" }
        }

        Write-Info "Looking for the latest successful build of $($settings.Repository) ($($settings.Branch))..."
        $runs = Get-BuildRuns $settings
        if ($null -eq $runs.LatestSuccess) {
            Stop-Update $script:ExitNoBuild "No successful build of $($settings.Branch) found in the last 20 runs of $($settings.Workflow)."
        }
        $run = $runs.LatestSuccess
        $shortSha = Get-ShortSha ([string]$run.head_sha)
        Write-Info "Latest successful build: #$($run.run_number), commit $shortSha `"$($run.display_title)`" ($(Format-Timestamp $run.updated_at))"

        $newest = $runs.Newest
        if ($newest.id -ne $run.id) {
            $newestSha = Get-ShortSha ([string]$newest.head_sha)
            if ($newest.status -ne 'completed') {
                Write-Warning2 "A newer build (#$($newest.run_number), commit $newestSha) is still running. Run the updater again in a few minutes to get it."
            } else {
                Write-Warning2 "The newest build (#$($newest.run_number), commit $newestSha) ended '$($newest.conclusion)', so it is not installed."
            }
        }

        $null = Get-RunArtifact $settings $run
        $stagingDir = Join-Path $settings.WorkDir ("download-" + $run.id)
        Save-RunArtifact $settings $run $stagingDir
        $jar = Get-VerifiedArtifactJar $stagingDir ([string]$run.head_sha)
        $minecraft = [string]$jar.Info.minecraft
        $loader = [string]$jar.Info.fabricLoader
        Write-Detail "Downloaded and verified $($jar.Name) (for Minecraft $minecraft, Fabric Loader $loader or newer)"

        $current = @($installed | Where-Object { $_.Name -eq $jar.Name -and (Get-Sha256 $_.File) -eq $jar.Sha256 })
        if (-not $Force -and $installed.Count -eq 1 -and $current.Count -eq 1) {
            Write-Host ''
            Write-Host "Simple Block is already up to date: $($jar.Version) (commit $shortSha). Nothing was changed." -ForegroundColor Green
            Open-LauncherSafely $settings $interactive $gameDirPath
            return $script:ExitOk
        }

        Assert-MinecraftNotRunning $gameDirPath
        $backupDir = Install-SimpleBlockJar $jar $installed $modsDir (Join-Path $settings.WorkDir 'backups')
        $changedFiles = $true
        Remove-OldBackups (Join-Path $settings.WorkDir 'backups')

        Write-Host ''
        if ($installed.Count -eq 0) {
            Write-Host "Installed Simple Block $($jar.Version) (commit $shortSha)." -ForegroundColor Green
        } else {
            $before = (@($installed | ForEach-Object { $_.Version }) -join ', ')
            Write-Host "Updated Simple Block: $before -> $($jar.Version) (commit $shortSha)." -ForegroundColor Green
        }
        Write-Info "Mod file: $(Join-Path $modsDir $jar.Name)"
        if ($null -ne $backupDir) { Write-Detail "Previous jar saved in $backupDir" }
        Write-Detail "Play with the Fabric $minecraft installation whose Game Directory is $gameDirPath."
        Open-LauncherSafely $settings $interactive $gameDirPath
        return $script:ExitOk
    } catch {
        $code = $_.Exception.Data['ExitCode']
        Write-Host ''
        if ($null -eq $code) {
            $code = $script:ExitFailed
            Write-Host "Update failed: $($_.Exception.Message)" -ForegroundColor Red
        } else {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
        if (-not $changedFiles -and $_.Exception.Data['ChangedFiles'] -ne $true) {
            Write-Host 'Your Simple Block installation was not changed.'
        }
        return [int]$code
    } finally {
        if ($null -ne $stagingDir -and (Test-Path -LiteralPath $stagingDir)) {
            Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

# Run unless dot-sourced (the tests dot-source this file to reach the functions).
if ($MyInvocation.InvocationName -ne '.') {
    exit (Invoke-SimpleBlockUpdate @PSBoundParameters)
}
