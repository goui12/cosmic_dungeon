#requires -Version 5.1
# Offline filesystem/transaction tests. No game launch, network request, or installed mod edit.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-updater\Update-CosmicDungeon.ps1" -LibraryOnly
. "$PSScriptRoot\publish-test-build.ps1" -LibraryOnly

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$regressionTypes = @'
class CdFixtureFeed : CdTestFeed {
    [object]$Value
    [string]$Fixture
    [bool]$FailDownload = $false
    [int]$Downloads = 0
    [object] ReadCurrent() { return [pscustomobject]@{ Revision = ('a' * 40); Manifest = $this.Value } }
    [void] Download([string]$revision, [string]$name, [string]$destination, [int64]$size) {
        $this.Downloads++
        if ($this.FailDownload) { throw 'Simulated interrupted download' }
        [IO.File]::Copy($this.Fixture, $destination)
    }
}

class CdFixtureEnvironment : CdClientEnvironment {
    [bool]$Running = $false
    [void] EnsureClosed() { if ($this.Running) { throw 'Simulated running client' } }
}

class CdUpdaterRegression {
    [string]$Root
    [int]$Checks = 0

    [void] Assert([bool]$condition, [string]$message) {
        if (!$condition) { throw ('FAILED: ' + $message) }
        $this.Checks++
    }

    [void] Jar([string]$path, [string]$marker) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $stream = [IO.File]::Open($path, 'CreateNew')
        $zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            $entry = $zip.CreateEntry('META-INF/neoforge.mods.toml')
            $writer = [IO.StreamWriter]::new($entry.Open())
            try { $writer.Write('modId="cosmicdungeon"' + "`n# " + $marker) } finally { $writer.Dispose() }
        } finally { $zip.Dispose(); $stream.Dispose() }
    }

    [object] Setup([string]$name) {
        $dir = Join-Path $this.Root $name
        $mods = Join-Path $dir 'mods'
        [void][IO.Directory]::CreateDirectory($mods)
        $fixture = Join-Path $dir 'new.jar'
        $this.Jar($fixture, 'new-content')
        $feed = [CdFixtureFeed]::new()
        $feed.Fixture = $fixture
        $feed.Value = [pscustomobject]@{ schemaVersion = 1; channel = 'TEST'; status = 'deployed';
            fileName = 'cosmicdungeon-1.5.1.jar'; sha256 = [CdClientUpdater]::Hash($fixture);
            sourceCommit = ('b' * 40); size = (Get-Item $fixture).Length; minecraft = '1.21.10'; neoForge = '21.10.64' }
        $environment = [CdFixtureEnvironment]::new()
        $app = [CdClientUpdater]::new($mods, $feed, $environment)
        return [pscustomobject]@{ Mods = $mods; Feed = $feed; Environment = $environment; App = $app; Fixture = $fixture }
    }

    [void] ExpectFailure([CdClientUpdater]$app, [string]$name) {
        $failed = $false
        try { [void]$app.Run($false) } catch { $failed = $true }
        $this.Assert($failed, $name)
    }

    [void] Run() {
        $this.Root = Join-Path ([IO.Path]::GetTempPath()) ('cd-updater-tests-' + [guid]::NewGuid().ToString('N'))
        try {
            $f = $this.Setup('same-version')
            $target = Join-Path $f.Mods 'cosmicdungeon-1.5.1.jar'
            $this.Jar($target, 'old-content')
            $oldHash = [CdClientUpdater]::Hash($target)
            $this.Jar((Join-Path $f.Mods 'cosmicdungeon-1.5.0.jar'), 'older')
            [IO.File]::WriteAllText((Join-Path $f.Mods 'other-mod.jar'), 'preserve')
            [IO.File]::WriteAllText((Join-Path $f.Mods 'cosmicdungeon-loading-screen-1.5.1.jar'), 'preserve-loader')
            $this.Assert($f.App.Run($false) -eq 0, 'same version update succeeds')
            $this.Assert([CdClientUpdater]::Hash($target) -eq $f.Feed.Value.sha256, 'new content installed')
            $this.Assert(!(Test-Path (Join-Path $f.Mods 'cosmicdungeon-1.5.0.jar')), 'old duplicate removed from mods')
            $this.Assert((Get-ChildItem (Join-Path $f.App.Work 'backups') -Recurse -Filter 'cosmicdungeon-*.jar').Count -eq 2, 'both old versions backed up')
            $this.Assert([IO.File]::ReadAllText((Join-Path $f.Mods 'other-mod.jar')) -eq 'preserve', 'other mods preserved')
            $this.Assert([IO.File]::ReadAllText((Join-Path $f.Mods 'cosmicdungeon-loading-screen-1.5.1.jar')) -eq 'preserve-loader', 'loading-screen module preserved')
            $this.Assert($f.App.Run($false) -eq 0 -and $f.Feed.Downloads -eq 1, 'current hash avoids another download')

            foreach ($name in @('bad-hash','running','download-failure','bad-name','wrong-pack','built-only','bad-zip')) {
                $f = $this.Setup($name)
                $target = Join-Path $f.Mods 'cosmicdungeon-1.5.1.jar'
                $this.Jar($target, 'original')
                $oldHash = [CdClientUpdater]::Hash($target)
                switch ($name) {
                    'bad-hash' { $f.Feed.Value.sha256 = '0' * 64 }
                    'running' { $f.Environment.Running = $true }
                    'download-failure' { $f.Feed.FailDownload = $true }
                    'bad-name' { $f.Feed.Value.fileName = '..\other-mod.jar' }
                    'wrong-pack' { $f.Feed.Value.minecraft = '1.0' }
                    'built-only' { $f.Feed.Value.status = 'built' }
                    'bad-zip' {
                        [IO.File]::WriteAllText($f.Fixture, 'this is not a jar')
                        $f.Feed.Value.sha256 = [CdClientUpdater]::Hash($f.Fixture)
                        $f.Feed.Value.size = (Get-Item $f.Fixture).Length
                    }
                }
                $this.ExpectFailure($f.App, $name + ' rejected')
                $this.Assert([CdClientUpdater]::Hash($target) -eq $oldHash, $name + ' preserves installed content')
            }

            $f = $this.Setup('check-only')
            $this.Assert($f.App.Run($true) -eq 10 -and $f.Feed.Downloads -eq 0 -and !(Test-Path $f.App.Work), 'check-only is read-only')

            $f = $this.Setup('rollback')
            $first = Join-Path $f.Mods 'cosmicdungeon-1.5.0.jar'
            $second = Join-Path $f.Mods 'cosmicdungeon-1.5.1.jar'
            $this.Jar($first, 'first')
            $this.Jar($second, 'second')
            $firstHash = [CdClientUpdater]::Hash($first)
            $secondHash = [CdClientUpdater]::Hash($second)
            $held = [IO.File]::Open($second, 'Open', 'Read', 'Read')
            try { $this.ExpectFailure($f.App, 'locked second JAR rejects swap') } finally { $held.Dispose() }
            $this.Assert([CdClientUpdater]::Hash($first) -eq $firstHash -and [CdClientUpdater]::Hash($second) -eq $secondHash, 'partial swap restores previous versions')
            $this.Assert(!(Test-Path (Join-Path $f.App.Work 'pending.json')), 'successful rollback clears journal')

            $f = $this.Setup('interrupted')
            [void][IO.Directory]::CreateDirectory($f.App.Work)
            [IO.File]::WriteAllText((Join-Path $f.App.Work 'pending.json'), '{}')
            $this.ExpectFailure($f.App, 'unfinished previous update requires recovery')
            $this.Assert($f.Feed.Downloads -eq 0, 'no download after interrupted update')

            $receipt = [pscustomobject]@{ Commit = ('c' * 40); JarSHA256 = $f.Feed.Value.sha256; SourceHash = ('d' * 64) }
            $jar = Join-Path $this.Root 'cosmicdungeon-1.5.1.jar'
            [IO.File]::Copy($f.Fixture, $jar)
            $manifest = [CdTestBuildPublisher]::Manifest($receipt, 'Built', $jar)
            $this.Assert($manifest.status -eq 'built' -and $manifest.sha256 -eq $receipt.JarSHA256, 'publisher produces candidate manifest')
            $deployedReceipt = [pscustomobject]@{ Commit = ('c' * 40); SHA256 = $f.Feed.Value.sha256 }
            $deployedManifest = [CdTestBuildPublisher]::Manifest($deployedReceipt, 'Deployed', $jar)
            $this.Assert($deployedManifest.status -eq 'deployed' -and $deployedManifest.sourceFingerprint -eq '', 'completed deployment receipt without optional source fingerprint')
            $receipt.JarSHA256 = '0' * 64
            $failed = $false
            try { [void][CdTestBuildPublisher]::Manifest($receipt, 'Built', $jar) } catch { $failed = $true }
            $this.Assert($failed, 'publisher refuses stale receipt')
            Write-Host ('PASS: ' + $this.Checks + ' offline updater/publisher assertions.')
        } finally { if (Test-Path $this.Root) { Remove-Item -LiteralPath $this.Root -Recurse -Force } }
    }
}
[CdUpdaterRegression]::new().Run()

'@
Invoke-Expression $regressionTypes
