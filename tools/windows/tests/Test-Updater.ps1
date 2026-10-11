<#
.SYNOPSIS
    Scenario tests for Update-SimpleBlock.ps1.

.DESCRIPTION
    GitHub (the gh CLI) and the "is Minecraft running" process lookup are faked. Everything else is
    real: reading jars, hashing, moving files, backups, rollback and, on Windows, file locks.
    Runs on Windows PowerShell 5.1 and PowerShell 7 (Windows or Linux):

        powershell -NoProfile -ExecutionPolicy Bypass -File tools\windows\tests\Test-Updater.ps1

    Set SB_TEST_VERBOSE=1 to see the updater's own output for every scenario.
#>
Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$updaterPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'Update-SimpleBlock.ps1'
. $updaterPath
# Kept for the real-process test below, before the fake replaces it.
$script:RealGetMinecraftProcess = ${function:Get-MinecraftProcess}

$script:TestRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('sb-updater-tests-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
$null = New-Item -ItemType Directory -Path $script:TestRoot
$script:Passed = 0
$script:Skipped = 0
$script:Failed = New-Object System.Collections.Generic.List[string]
$script:Fake = $null
$script:FailMoveTo = $null

#region Fakes (these replace the updater's functions of the same name)

function Reset-Fake {
    $script:Fake = @{
        LoggedIn         = $true
        Runs             = @()
        Artifacts        = @{}
        DownloadFrom     = @{}
        DownloadFails    = $false
        RunningMinecraft = @()
    }
    $script:FailMoveTo = $null
}

function Find-GitHubCli([string]$Configured) { return 'fake-gh' }

function Get-MinecraftProcess([string]$GameDir) { return @($script:Fake.RunningMinecraft) }

function Move-JarFile([string]$From, [string]$To) {
    if ($null -ne $script:FailMoveTo -and $To.EndsWith($script:FailMoveTo)) { throw 'simulated disk error' }
    [System.IO.File]::Move($From, $To)
}

function Invoke-Gh([string[]]$Arguments) {
    $joined = $Arguments -join ' '
    if ($Arguments[0] -eq 'auth') {
        if ($script:Fake.LoggedIn) { return New-GhResult 0 '' }
        return New-GhResult 1 '' 'You are not logged into any GitHub hosts. To log in, run: gh auth login'
    }
    if ($Arguments[0] -eq 'api' -and $joined -match 'actions/workflows/build\.yml/runs') {
        return New-GhResult 0 (ConvertTo-Json -Depth 6 -InputObject @{ workflow_runs = @($script:Fake.Runs) })
    }
    if ($Arguments[0] -eq 'api' -and $joined -match 'actions/runs/(\d+)/artifacts') {
        $artifacts = $script:Fake.Artifacts[$Matches[1]]
        if ($null -eq $artifacts) { $artifacts = @() }
        return New-GhResult 0 (ConvertTo-Json -Depth 6 -InputObject @{ artifacts = @($artifacts) })
    }
    if ($Arguments[0] -eq 'run' -and $Arguments[1] -eq 'download') {
        if ($script:Fake.DownloadFails) { return New-GhResult 1 '' 'error downloading simple-block-mod: HTTP 500' }
        $destination = $Arguments[[array]::IndexOf($Arguments, '-D') + 1]
        Copy-Item -Path (Join-Path $script:Fake.DownloadFrom[$Arguments[2]] '*') -Destination $destination -Recurse
        return New-GhResult 0 ''
    }
    throw "fake gh: unexpected call: gh $joined"
}

function New-GhResult([int]$ExitCode, [string]$StdOut, [string]$StdErr = '') {
    return [pscustomobject]@{ ExitCode = $ExitCode; StdOut = $StdOut; StdErr = $StdErr }
}

#endregion

#region Fixtures

function New-FakeJar([string]$Path, [string]$Id, [string]$Version, [string]$Content = '') {
    $zip = [System.IO.Compression.ZipFile]::Open($Path, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $writer = New-Object System.IO.StreamWriter($zip.CreateEntry('fabric.mod.json').Open())
        try { $writer.Write((ConvertTo-Json -InputObject ([ordered]@{ schemaVersion = 1; id = $Id; version = $Version }))) } finally { $writer.Dispose() }
        $writer = New-Object System.IO.StreamWriter($zip.CreateEntry('content.txt').Open())
        try { $writer.Write($Content) } finally { $writer.Dispose() }
    } finally {
        $zip.Dispose()
    }
}

function New-Run([long]$Id, [int]$Number, [string]$Sha, [string]$Status, [string]$Conclusion) {
    return [pscustomobject]@{
        id = $Id; run_number = $Number; head_sha = $Sha; head_branch = 'main'; event = 'push'
        status = $Status; conclusion = $Conclusion; display_title = "Commit number $Number"
        created_at = '2026-10-11T05:00:00Z'; updated_at = '2026-10-11T05:04:00Z'
    }
}

# Adds a build to the fake GitHub as the newest run. Options tweak it into the broken variants.
function Publish-Build([int]$Number, [hashtable]$Options = @{}) {
    $sha = ('{0:x7}' -f $Number) + ('a' * 33)
    $id = 5000 + $Number
    $version = "0.1.0+build.$Number.$($sha.Substring(0, 7))"
    $jarName = "simple_block-$version.jar"
    $dir = Join-Path $script:TestRoot ("artifact-" + [guid]::NewGuid().ToString('N').Substring(0, 8))
    $null = New-Item -ItemType Directory -Path $dir

    $modId = 'simple_block'
    if ($Options.ContainsKey('JarModId')) { $modId = $Options.JarModId }
    New-FakeJar (Join-Path $dir $jarName) $modId $version "build $Number"
    if ($Options.ContainsKey('SourcesJar')) { New-FakeJar (Join-Path $dir "simple_block-$version-sources.jar") 'simple_block' $version 'sources' }
    if ($Options.ContainsKey('DevJar')) { New-FakeJar (Join-Path $dir "simple_block-$version-dev.jar") 'simple_block' $version 'dev' }

    $info = [ordered]@{
        modId = 'simple_block'; version = $version; jar = $jarName; sha256 = (Get-Sha256 (Join-Path $dir $jarName))
        commit = $sha; branch = 'main'; runId = $id; runNumber = $Number; builtAt = '2026-10-11T05:04:00Z'
        minecraft = '26.3'; fabricLoader = '0.19.5'
    }
    if ($Options.ContainsKey('Info')) { foreach ($key in $Options.Info.Keys) { $info[$key] = $Options.Info[$key] } }
    if (-not $Options.ContainsKey('NoBuildInfo')) {
        [System.IO.File]::WriteAllText((Join-Path $dir 'build-info.json'), (ConvertTo-Json -InputObject $info))
    }

    $status = 'completed'; $conclusion = 'success'
    if ($Options.ContainsKey('Status')) { $status = $Options.Status }
    if ($Options.ContainsKey('Conclusion')) { $conclusion = $Options.Conclusion }
    $script:Fake.Runs = @(New-Run $id $Number $sha $status $conclusion) + @($script:Fake.Runs)
    $script:Fake.Artifacts["$id"] = @([pscustomobject]@{ id = $id * 10; name = 'simple-block-mod'; expired = $Options.ContainsKey('Expired') })
    $script:Fake.DownloadFrom["$id"] = $dir
    return [pscustomobject]@{ Id = $id; Sha = $sha; Version = $version; JarName = $jarName }
}

# A game directory with Fabric API, settings and a world, like a real Simple Block instance.
function New-Sandbox {
    $root = Join-Path $script:TestRoot ([guid]::NewGuid().ToString('N').Substring(0, 8))
    $game = Join-Path $root 'Minecraft Instances\SimpleBlock'
    foreach ($dir in @('mods', 'config', 'saves\Test World')) { $null = New-Item -ItemType Directory -Path (Join-Path $game $dir) -Force }
    New-FakeJar (Join-Path $game 'mods\fabric-api-0.162.0+26.3.jar') 'fabric-api' '0.162.0+26.3' 'api'
    [System.IO.File]::WriteAllText((Join-Path $game 'options.txt'), 'fov:1.0')
    [System.IO.File]::WriteAllText((Join-Path $game 'config\other-mod.json'), '{}')
    [System.IO.File]::WriteAllText((Join-Path $game 'saves\Test World\level.dat'), 'world data')
    return [pscustomobject]@{
        Root = $root; GameDir = $game; Mods = (Join-Path $game 'mods')
        WorkDir = (Join-Path $root 'work'); Settings = (Join-Path $root 'settings.ini')
    }
}

function Get-Snapshot([string]$Dir) {
    $map = @{}
    foreach ($file in @(Get-ChildItem -LiteralPath $Dir -Recurse -File -Force)) {
        $map[$file.FullName.Substring($Dir.Length)] = Get-Sha256 $file.FullName
    }
    return $map
}

function Assert-SnapshotUnchanged($Before, [string]$Dir) {
    $after = Get-Snapshot $Dir
    $diff = @()
    foreach ($key in $Before.Keys) {
        if (-not $after.ContainsKey($key)) { $diff += "removed $key" } elseif ($after[$key] -ne $Before[$key]) { $diff += "changed $key" }
    }
    foreach ($key in $after.Keys) { if (-not $Before.ContainsKey($key)) { $diff += "added $key" } }
    if ($diff.Count -gt 0) { throw "game directory changed: $($diff -join '; ')" }
}

function Invoke-Updater($Box, [hashtable]$Extra = @{}) {
    $params = @{ GameDir = $Box.GameDir; WorkDir = $Box.WorkDir; SettingsPath = $Box.Settings; OpenLauncher = 'never'; NonInteractive = $true }
    foreach ($key in $Extra.Keys) { $params[$key] = $Extra[$key] }
    $output = @(Invoke-SimpleBlockUpdate @params 6>&1)
    $codes = @($output | Where-Object { $_ -is [int] })
    $leaked = @($output | Where-Object { $_ -isnot [int] -and $_ -isnot [System.Management.Automation.InformationRecord] })
    $text = (@($output | Where-Object { $_ -is [System.Management.Automation.InformationRecord] } | ForEach-Object { [string]$_ }) -join "`n")
    if ($env:SB_TEST_VERBOSE) { Write-Host $text -ForegroundColor DarkGray }
    if ($leaked.Count -gt 0) { throw "updater leaked pipeline output: $($leaked -join ' | ')" }
    if ($codes.Count -ne 1) { throw "expected exactly one exit code, got $($codes.Count). Output:`n$text" }
    return [pscustomobject]@{ Code = $codes[0]; Text = $text }
}

function Get-SimpleBlockJarNames($Box) {
    return @(Get-InstalledSimpleBlockJars $Box.Mods | ForEach-Object { $_.Name })
}

#endregion

#region Assertions and runner

function Assert-True([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }

function Assert-Equal($Expected, $Actual, [string]$What) {
    if ($Expected -ne $Actual) { throw "${What}: expected '$Expected' but got '$Actual'" }
}

function Assert-Contains([string]$Text, [string]$Fragment) {
    if ($Text.IndexOf($Fragment, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) { throw "output does not mention '$Fragment'. Output:`n$Text" }
}

function Assert-Installed($Box, $Build) {
    $names = @(Get-SimpleBlockJarNames $Box)
    Assert-Equal $Build.JarName ($names -join ', ') 'installed Simple Block jars'
    Assert-True (Test-Path -LiteralPath (Join-Path $Box.Mods 'fabric-api-0.162.0+26.3.jar')) 'Fabric API jar should still be there'
    Assert-Equal 0 @(Get-ChildItem -LiteralPath $Box.Mods -Force | Where-Object { $_.Name -like '*.partial' }).Count 'leftover .partial files'
}

function Test-Case([string]$Name, [scriptblock]$Body) {
    Reset-Fake
    try {
        & $Body
        $script:Passed++
        Write-Host "PASS  $Name" -ForegroundColor Green
    } catch {
        $script:Failed.Add($Name)
        Write-Host "FAIL  $Name" -ForegroundColor Red
        Write-Host "      $($_.Exception.Message)"
        Write-Host "      at $($_.InvocationInfo.ScriptLineNumber): $($_.InvocationInfo.Line.Trim())"
    }
}

function Skip-Case([string]$Name, [string]$Reason) {
    $script:Skipped++
    Write-Host "SKIP  $Name ($Reason)" -ForegroundColor Yellow
}

#endregion

Write-Host "PowerShell $($PSVersionTable.PSVersion) ($($PSVersionTable.PSEdition)) on $([System.Environment]::OSVersion.VersionString)"

Test-Case 'fresh install adds only the Simple Block jar' {
    $box = New-Sandbox
    $build = Publish-Build 1
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text "Installed Simple Block $($build.Version)"
    Assert-Installed $box $build
    $after = Get-Snapshot $box.GameDir
    foreach ($key in $before.Keys) { Assert-Equal $before[$key] $after[$key] "hash of $key" }
    Assert-Equal ($before.Count + 1) $after.Count 'number of files in the game directory'
}

Test-Case 'second run says up to date and changes nothing' {
    $box = New-Sandbox
    $build = Publish-Build 1
    $null = Invoke-Updater $box
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text "already up to date: $($build.Version)"
    Assert-SnapshotUnchanged $before $box.GameDir
}

Test-Case 'new build replaces the old jar and keeps a backup' {
    $box = New-Sandbox
    $first = Publish-Build 1
    $null = Invoke-Updater $box
    $second = Publish-Build 2
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text "Updated Simple Block: $($first.Version) -> $($second.Version)"
    Assert-Installed $box $second
    $backups = @(Get-ChildItem -LiteralPath (Join-Path $box.WorkDir 'backups') -Recurse -File | ForEach-Object { $_.Name })
    Assert-True ($backups -contains $first.JarName) "previous jar should be in the backups folder (found: $($backups -join ', '))"
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $box.WorkDir "download-$($second.Id)"))) 'download folder should be cleaned up'
}

Test-Case 'old Simple Block jars are found by mod id, not file name' {
    $box = New-Sandbox
    New-FakeJar (Join-Path $box.Mods 'my renamed copy.jar') 'simple_block' '0.0.1' 'renamed'
    New-FakeJar (Join-Path $box.Mods 'simple_block-addon.jar') 'some_other_mod' '1.0' 'other'
    [System.IO.File]::WriteAllText((Join-Path $box.Mods 'readme.txt'), 'not a mod')
    $build = Publish-Build 1
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Installed $box $build
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $box.Mods 'my renamed copy.jar'))) 'renamed Simple Block jar should be replaced'
    Assert-True (Test-Path -LiteralPath (Join-Path $box.Mods 'simple_block-addon.jar')) 'a different mod with a similar name must stay'
    Assert-True (Test-Path -LiteralPath (Join-Path $box.Mods 'readme.txt')) 'non-jar files must stay'
}

Test-Case 'duplicate Simple Block jars are cleaned up' {
    $box = New-Sandbox
    New-FakeJar (Join-Path $box.Mods 'simple_block-old-a.jar') 'simple_block' '0.0.1' 'a'
    New-FakeJar (Join-Path $box.Mods 'simple_block-old-b.jar') 'simple_block' '0.0.2' 'b'
    $build = Publish-Build 1
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Installed $box $build
}

Test-Case 'sources and dev jars in the download are never installed' {
    $box = New-Sandbox
    $build = Publish-Build 1 @{ SourcesJar = $true; DevJar = $true }
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Installed $box $build
    Assert-Equal 0 @(Get-ChildItem -LiteralPath $box.Mods | Where-Object { $_.Name -match '-(sources|dev)\.jar$' }).Count 'sources/dev jars in mods'
}

$brokenArtifacts = @(
    @{ Name = 'jar that is not Simple Block'; Options = @{ JarModId = 'not_simple_block' }; Message = "mod id is 'not_simple_block'" },
    @{ Name = 'checksum mismatch'; Options = @{ Info = @{ sha256 = ('0' * 64) } }; Message = 'SHA-256 does not match' },
    @{ Name = 'build from a different commit'; Options = @{ Info = @{ commit = ('f' * 40) } }; Message = 'built from commit fffffff' },
    @{ Name = 'version mismatch'; Options = @{ Info = @{ version = '9.9.9' } }; Message = "expected '9.9.9'" },
    @{ Name = 'build-info points at a sources jar'; Options = @{ SourcesJar = $true; Info = @{ jar = 'simple_block-0.1.0-sources.jar' } }; Message = 'not a mod jar' },
    @{ Name = 'no build-info.json'; Options = @{ NoBuildInfo = $true }; Message = 'no build-info.json' }
)
foreach ($case in $brokenArtifacts) {
    Test-Case "refuses a bad download: $($case.Name)" {
        $box = New-Sandbox
        $good = Publish-Build 1
        $null = Invoke-Updater $box
        $before = Get-Snapshot $box.GameDir
        $null = Publish-Build 2 $case.Options
        $result = Invoke-Updater $box
        Assert-Equal 1 $result.Code 'exit code'
        Assert-Contains $result.Text $case.Message
        Assert-Contains $result.Text 'was not changed'
        Assert-SnapshotUnchanged $before $box.GameDir
        Assert-Installed $box $good
    }
}

Test-Case 'no successful build: reports it and changes nothing' {
    $box = New-Sandbox
    $null = Publish-Build 1 @{ Conclusion = 'failure' }
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 2 $result.Code 'exit code'
    Assert-Contains $result.Text 'No successful build'
    Assert-SnapshotUnchanged $before $box.GameDir
}

Test-Case 'newer build still running: installs the latest finished one and says so' {
    $box = New-Sandbox
    $finished = Publish-Build 1
    $null = Publish-Build 2 @{ Status = 'in_progress'; Conclusion = '' }
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text 'is still running'
    Assert-Installed $box $finished
}

Test-Case 'newest build failed: installs the last good one and says so' {
    $box = New-Sandbox
    $good = Publish-Build 1
    $null = Publish-Build 2 @{ Conclusion = 'failure' }
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text "ended 'failure'"
    Assert-Installed $box $good
}

Test-Case 'expired download: reports it and changes nothing' {
    $box = New-Sandbox
    $null = Publish-Build 1 @{ Expired = $true }
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 2 $result.Code 'exit code'
    Assert-Contains $result.Text 'has expired'
    Assert-SnapshotUnchanged $before $box.GameDir
}

Test-Case 'failed download keeps the installed jar' {
    $box = New-Sandbox
    $good = Publish-Build 1
    $null = Invoke-Updater $box
    $before = Get-Snapshot $box.GameDir
    $null = Publish-Build 2
    $script:Fake.DownloadFails = $true
    $result = Invoke-Updater $box
    Assert-Equal 1 $result.Code 'exit code'
    Assert-Contains $result.Text 'HTTP 500'
    Assert-SnapshotUnchanged $before $box.GameDir
    Assert-Installed $box $good
}

Test-Case 'not signed in to GitHub: explains how and changes nothing' {
    $box = New-Sandbox
    $null = Publish-Build 1
    $script:Fake.LoggedIn = $false
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 4 $result.Code 'exit code'
    Assert-Contains $result.Text 'gh auth login'
    Assert-SnapshotUnchanged $before $box.GameDir
}

Test-Case 'Minecraft running from this game directory: refuses and changes nothing' {
    $box = New-Sandbox
    $good = Publish-Build 1
    $null = Invoke-Updater $box
    $null = Publish-Build 2
    $script:Fake.RunningMinecraft = @([pscustomobject]@{ ProcessId = 4242; CommandLine = "javaw.exe --gameDir `"$($box.GameDir)`"" })
    $before = Get-Snapshot $box.GameDir
    $result = Invoke-Updater $box
    Assert-Equal 3 $result.Code 'exit code'
    Assert-Contains $result.Text 'Quit Minecraft'
    Assert-SnapshotUnchanged $before $box.GameDir
    Assert-Installed $box $good
}

# Reads a file another process is still writing (Windows locks redirected output against plain reads).
function Read-SharedText([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    $stream = New-Object System.IO.FileStream($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
    try {
        return (New-Object System.IO.StreamReader($stream)).ReadToEnd()
    } finally {
        $stream.Dispose()
    }
}

# A JDK 17+ for the real-Java test (GitHub's Windows runners have several).
function Find-TestJava {
    foreach ($name in @('JAVA_HOME_25_X64', 'JAVA_HOME_21_X64', 'JAVA_HOME_17_X64')) {
        $javaHome = [Environment]::GetEnvironmentVariable($name)
        if ($javaHome -and (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) { return (Join-Path $javaHome 'bin\java.exe') }
    }
    return $null
}

if (Test-IsWindows) {
    Test-Case 'jar held open like a running game: refuses and changes nothing' {
        $box = New-Sandbox
        $good = Publish-Build 1
        $null = Invoke-Updater $box
        $null = Publish-Build 2
        $before = Get-Snapshot $box.GameDir
        # Java opens jars for reading and lets others read and write, but not delete or rename them.
        $stream = New-Object System.IO.FileStream((Join-Path $box.Mods $good.JarName), [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
        try {
            $result = Invoke-Updater $box
        } finally {
            $stream.Dispose()
        }
        Assert-Equal 3 $result.Code 'exit code'
        Assert-Contains $result.Text 'is in use'
        Assert-SnapshotUnchanged $before $box.GameDir
        Assert-Installed $box $good
    }
} else {
    Skip-Case 'jar held open like a running game' 'Windows-only file locking'
}

$testJava = $null
if (Test-IsWindows) { $testJava = Find-TestJava }
if ($null -ne $testJava) {
    Test-Case 'a real Java process using the game directory is detected and blocks the update' {
        $box = New-Sandbox
        $good = Publish-Build 1
        $null = Invoke-Updater $box
        $null = Publish-Build 2
        $before = Get-Snapshot $box.GameDir

        # Stands in for Minecraft: keeps the installed mod jar open and has --gameDir on its command line.
        $source = Join-Path $box.Root 'HoldJar.java'
        [System.IO.File]::WriteAllText($source, 'public class HoldJar { public static void main(String[] a) throws Exception { try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(a[0])) { System.out.println("holding"); System.out.flush(); Thread.sleep(120000); } } }')
        $log = Join-Path $box.Root 'java.log'
        $arguments = "`"$source`" `"$(Join-Path $box.Mods $good.JarName)`" --gameDir `"$($box.GameDir)`""
        $java = Start-Process -FilePath $testJava -ArgumentList $arguments -PassThru -WindowStyle Hidden -RedirectStandardOutput $log
        try {
            $deadline = (Get-Date).AddSeconds(60)
            while ((Read-SharedText $log) -notmatch 'holding') {
                if ((Get-Date) -gt $deadline -or $java.HasExited) { throw 'the Java stand-in did not start' }
                Start-Sleep -Milliseconds 250
            }

            $found = @(& $script:RealGetMinecraftProcess $box.GameDir)
            Assert-True (@($found | Where-Object { $_.ProcessId -eq $java.Id }).Count -eq 1) "real process lookup should find java.exe $($java.Id) (found: $(@($found | ForEach-Object { $_.ProcessId }) -join ', '))"
            Assert-Equal 0 @(& $script:RealGetMinecraftProcess (Join-Path $box.Root 'other')).Count 'processes found for another game directory'

            # With the process lookup faked to "nothing running", the open jar alone must still stop the update.
            $result = Invoke-Updater $box
            Assert-Equal 3 $result.Code 'exit code'
            Assert-Contains $result.Text 'is in use'
        } finally {
            if (-not $java.HasExited) { Stop-Process -Id $java.Id -Force }
            $java.WaitForExit()
        }
        Assert-SnapshotUnchanged $before $box.GameDir
        Assert-Installed $box $good
    }
} else {
    Skip-Case 'a real Java process using the game directory is detected' 'needs Windows and a JDK 17+ (JAVA_HOME_17_X64 or newer)'
}

Test-Case 'failure during the swap puts the previous jar back' {
    $box = New-Sandbox
    $good = Publish-Build 1
    $null = Invoke-Updater $box
    $before = Get-Snapshot $box.GameDir
    $next = Publish-Build 2
    $script:FailMoveTo = $next.JarName
    $result = Invoke-Updater $box
    Assert-Equal 1 $result.Code 'exit code'
    Assert-Contains $result.Text 'previous Simple Block jar was put back'
    Assert-SnapshotUnchanged $before $box.GameDir
    Assert-Installed $box $good
}

Test-Case 'missing game directory: nothing is created' {
    $box = New-Sandbox
    $null = Publish-Build 1
    $missing = Join-Path $box.Root 'Not There\SimpleBlock'
    $result = Invoke-Updater $box @{ GameDir = $missing }
    Assert-Equal 4 $result.Code 'exit code'
    Assert-Contains $result.Text 'Game directory not found'
    Assert-True (-not (Test-Path -LiteralPath $missing)) 'the missing game directory must not be created'
}

Test-Case 'creates mods inside an existing game directory' {
    $box = New-Sandbox
    Remove-Item -LiteralPath $box.Mods -Recurse -Force
    $build = Publish-Build 1
    $result = Invoke-Updater $box
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Equal $build.JarName (@(Get-SimpleBlockJarNames $box) -join ', ') 'installed Simple Block jars'
}

Test-Case '-Force reinstalls the same build' {
    $box = New-Sandbox
    $build = Publish-Build 1
    $null = Invoke-Updater $box
    $result = Invoke-Updater $box @{ Force = $true }
    Assert-Equal 0 $result.Code 'exit code'
    Assert-Contains $result.Text 'Updated Simple Block'
    Assert-Installed $box $build
}

Test-Case 'settings file supplies GameDir; parameters override it' {
    $box = New-Sandbox
    $build = Publish-Build 1
    [System.IO.File]::WriteAllLines($box.Settings, [string[]]@('# comment', "GameDir = $($box.GameDir)", 'OpenLauncher = never'))
    $output = @(Invoke-SimpleBlockUpdate -WorkDir $box.WorkDir -SettingsPath $box.Settings -NonInteractive 6>&1)
    Assert-Equal 0 @($output | Where-Object { $_ -is [int] })[0] 'exit code using the settings file'
    Assert-Installed $box $build

    $result = Invoke-Updater $box @{ GameDir = (Join-Path $box.Root 'elsewhere') }
    Assert-Equal 4 $result.Code 'exit code when -GameDir overrides the file'

    [System.IO.File]::WriteAllLines($box.Settings, [string[]]@("GameDir = $($box.GameDir)", 'OpenLauncher = sometimes'))
    $output = @(Invoke-SimpleBlockUpdate -WorkDir $box.WorkDir -SettingsPath $box.Settings -NonInteractive 6>&1)
    Assert-Equal 4 @($output | Where-Object { $_ -is [int] })[0] 'exit code for an invalid OpenLauncher value'
}

Test-Case 'settings file round trip keeps other lines' {
    $path = Join-Path $script:TestRoot 'roundtrip.ini'
    [System.IO.File]::WriteAllLines($path, [string[]]@('# keep me', 'OpenLauncher = never', 'GameDir = C:\old'))
    Set-SettingInFile $path 'GameDir' '%USERPROFILE%\AppData\Minecraft-Instances\SimpleBlock'
    $read = Read-SettingsFile $path
    Assert-Equal '%USERPROFILE%\AppData\Minecraft-Instances\SimpleBlock' $read.GameDir 'GameDir'
    Assert-Equal 'never' $read.OpenLauncher 'OpenLauncher'
    Assert-Equal '# keep me' ([System.IO.File]::ReadAllLines($path)[0]) 'comment line'
}

Test-Case 'recognises Minecraft command lines for this game directory only' {
    $dir = 'C:\Users\me\AppData\Minecraft-Instances\SimpleBlock'
    $java = '"C:\Program Files\Java\bin\javaw.exe" -Xmx4G net.fabricmc.loader.impl.launch.knot.KnotClient --username Me --version fabric-loader-0.19.5-26.3'
    Assert-True (Test-CommandLineUsesGameDir "$java --gameDir $dir --assetsDir C:\x" $dir) 'unquoted path'
    Assert-True (Test-CommandLineUsesGameDir "$java --gameDir `"$dir`" --assetsDir C:\x" $dir) 'quoted path'
    Assert-True (Test-CommandLineUsesGameDir "$java --gameDir $($dir.ToLowerInvariant())\" $dir) 'different case and trailing backslash'
    Assert-True (-not (Test-CommandLineUsesGameDir "$java --gameDir $($dir)2 --assetsDir C:\x" $dir)) 'a sibling folder with a longer name'
    Assert-True (-not (Test-CommandLineUsesGameDir "$java --gameDir C:\Users\me\AppData\Roaming\.minecraft" $dir)) 'another installation'
    Assert-True (-not (Test-CommandLineUsesGameDir $null $dir)) 'no command line'
}

Test-Case 'quotes command-line arguments the Windows way' {
    Assert-Equal 'plain' (ConvertTo-CommandLineArgument 'plain') 'plain'
    Assert-Equal '"two words"' (ConvertTo-CommandLineArgument 'two words') 'spaces'
    Assert-Equal '""' (ConvertTo-CommandLineArgument '') 'empty'
    Assert-Equal '"say \"hi\""' (ConvertTo-CommandLineArgument 'say "hi"') 'quotes'
    Assert-Equal '"C:\Some Dir\\"' (ConvertTo-CommandLineArgument 'C:\Some Dir\') 'trailing backslash'
}

Test-Case 'runs a real program and passes paths with spaces intact' {
    $shell = (Get-Process -Id $PID).Path
    $echo = Join-Path $script:TestRoot 'echo args.ps1'
    [System.IO.File]::WriteAllText($echo, '[Console]::Out.Write(($args | ForEach-Object { "<$_>" }) -join "")')
    $result = Invoke-Native $shell @('-NoProfile', '-NonInteractive', '-File', $echo, 'plain', 'C:\Some Dir\file.jar', 'branch=main')
    Assert-Equal 0 $result.ExitCode 'exit code'
    Assert-Equal '<plain><C:\Some Dir\file.jar><branch=main>' $result.StdOut 'arguments seen by the program'
}

Test-Case 'script entry point returns the exit code (missing game directory)' {
    $shell = (Get-Process -Id $PID).Path
    $missing = Join-Path $script:TestRoot 'no such dir'
    $result = Invoke-Native $shell @('-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $updaterPath,
        '-GameDir', $missing, '-WorkDir', (Join-Path $script:TestRoot 'entry-work'), '-SettingsPath', (Join-Path $script:TestRoot 'none.ini'), '-NonInteractive')
    Assert-Equal 4 $result.ExitCode "exit code (output: $($result.StdOut) $($result.StdErr))"
    Assert-Contains $result.StdOut 'Game directory not found'
}

Remove-Item -LiteralPath $script:TestRoot -Recurse -Force -ErrorAction SilentlyContinue
Write-Host ''
Write-Host "$($script:Passed) passed, $($script:Failed.Count) failed, $($script:Skipped) skipped"
if ($script:Failed.Count -gt 0) {
    Write-Host "Failed: $($script:Failed -join '; ')" -ForegroundColor Red
    exit 1
}
exit 0
