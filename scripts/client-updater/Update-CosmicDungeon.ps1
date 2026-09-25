#requires -Version 5.1
[CmdletBinding()]
param([switch]$CheckOnly, [switch]$LibraryOnly)

# Public, read-only TEST feed. No credentials, admin rights, Git, or saved PC paths.
class CdTestFeed {
    [string]$Base = 'https://raw.githubusercontent.com/goui12/cosmic_dungeon/'

    [byte[]] ReadBytes([string]$url, [int64]$limit) {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $request = [Net.HttpWebRequest]::Create($url)
        $request.UserAgent = 'CosmicDungeon-Test-Updater/2.0'
        $request.Timeout = 30000
        $request.ReadWriteTimeout = 30000
        $request.Headers['Cache-Control'] = 'no-cache'
        $response = $request.GetResponse()
        $memory = [IO.MemoryStream]::new()
        try {
            if ($response.ContentLength -gt $limit) { throw 'Download exceeds its size limit.' }
            $stream = $response.GetResponseStream()
            try {
                $buffer = [byte[]]::new(65536)
                while (($count = $stream.Read($buffer, 0, $buffer.Length)) -gt 0) {
                    if ($memory.Length + $count -gt $limit) { throw 'Download exceeds its size limit.' }
                    $memory.Write($buffer, 0, $count)
                }
                return $memory.ToArray()
            } finally { $stream.Dispose() }
        } finally { $memory.Dispose(); $response.Dispose() }
    }

    [object] ReadCurrent() {
        $api = 'https://api.github.com/repos/goui12/cosmic_dungeon/branches/test-builds'
        $branch = [Text.Encoding]::UTF8.GetString($this.ReadBytes($api, 262144)) | ConvertFrom-Json
        $revision = [string]$branch.commit.sha
        if ($revision -cnotmatch '^[0-9a-f]{40}$') { throw 'Invalid TEST branch revision.' }
        # Both manifest and JAR are pinned to this immutable Git commit.
        $url = $this.Base + $revision + '/current-test/manifest.json'
        $manifest = [Text.Encoding]::UTF8.GetString($this.ReadBytes($url, 65536)).TrimStart([char]0xFEFF) | ConvertFrom-Json
        return [pscustomobject]@{ Revision = $revision; Manifest = $manifest }
    }

    [void] Download([string]$revision, [string]$name, [string]$destination, [int64]$size) {
        $url = $this.Base + $revision + '/current-test/' + $name
        [IO.File]::WriteAllBytes($destination, $this.ReadBytes($url, $size))
    }
}

class CdClientEnvironment {
    [void] EnsureClosed() {
        $processes = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction Stop)
        foreach ($process in $processes) {
            # A portable copy may use another launcher path or an argument file.
            # Unknown Java processes cannot prove Minecraft is closed; fail safely.
            if (!$process.CommandLine -or $process.CommandLine -notmatch 'GradleDaemon|GradleWrapperMain') {
                throw 'Close Minecraft before updating (another Java application may also need closing). Then run this BAT again.'
            }
        }
    }
}

class CdClientUpdater {
    [string]$Mods
    [string]$Work
    [CdTestFeed]$Feed
    [CdClientEnvironment]$Environment

    CdClientUpdater([string]$mods, [CdTestFeed]$feed, [CdClientEnvironment]$environment) {
        $this.Mods = [IO.Path]::GetFullPath($mods)
        if (!(Test-Path -LiteralPath $this.Mods -PathType Container) -or
            [IO.Path]::GetFileName($this.Mods.TrimEnd('\', '/')) -ine 'mods') {
            throw 'Keep Update-CosmicDungeon.ps1 and its BAT inside your copied Minecraft mods folder.'
        }
        $this.Work = Join-Path ([IO.Directory]::GetParent($this.Mods).FullName) 'CosmicDungeonUpdater'
        $this.Feed = $feed
        $this.Environment = $environment
    }

    static [bool] IsManaged([string]$name) {
        # Only the main mod; preserve loading-screen modules and unrelated mods.
        return $name -cmatch '^cosmicdungeon-[0-9]+\.[0-9]+\.[0-9]+(?:[-+][A-Za-z0-9._-]+)?\.jar$'
    }

    static [string] Hash([string]$path) {
        return (Get-FileHash -LiteralPath $path -Algorithm SHA256 -ErrorAction Stop).Hash.ToLowerInvariant()
    }

    static [void] ValidateManifest([object]$manifest) {
        if ($manifest.schemaVersion -ne 1 -or $manifest.channel -cne 'TEST' -or $manifest.status -cne 'deployed') {
            throw 'This feed is not a confirmed deployed TEST revision.'
        }
        if (!( [CdClientUpdater]::IsManaged([string]$manifest.fileName)) -or
            [string]$manifest.sha256 -cnotmatch '^[0-9a-f]{64}$' -or
            [string]$manifest.sourceCommit -cnotmatch '^[0-9a-f]{40}$' -or
            $manifest.size -lt 1 -or $manifest.size -gt 67108864) {
            throw 'Invalid TEST build manifest; no installed files were changed.'
        }
        if ($manifest.minecraft -ne '1.21.10' -or $manifest.neoForge -ne '21.10.64') {
            throw 'This TEST build needs a different base modpack. Ask Cameron for the updated CurseForge profile.'
        }
    }

    static [void] VerifyJar([string]$path, [object]$manifest) {
        if ((Get-Item -LiteralPath $path).Length -ne [int64]$manifest.size -or
            [CdClientUpdater]::Hash($path) -cne $manifest.sha256) {
            throw 'Download checksum/size mismatch. Your installed mod was not replaced.'
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zipType = 'System.IO.Compression.ZipFile' -as [type]
        $zip = $zipType::OpenRead($path)
        try {
            $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
            if (!$entry -or $entry.Length -gt 65536) { throw 'Missing or invalid NeoForge mod metadata.' }
            $metadata = ''
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($metadata -notmatch 'modId\s*=\s*"cosmicdungeon"') { throw 'Downloaded JAR is not CosmicDungeon.' }
        } finally { $zip.Dispose() }
    }

    [object[]] Installed() {
        return @(Get-ChildItem -LiteralPath $this.Mods -File | Where-Object { [CdClientUpdater]::IsManaged($_.Name) })
    }

    [int] Run([bool]$checkOnly) {
        $snapshot = $this.Feed.ReadCurrent()
        $manifest = $snapshot.Manifest
        [CdClientUpdater]::ValidateManifest($manifest)
        Write-Host ('TEST revision: ' + $manifest.sourceCommit.Substring(0, 8) + ' / ' + $manifest.sha256.Substring(0, 12))
        $target = Join-Path $this.Mods $manifest.fileName
        $installed = $this.Installed()
        $pending = Join-Path $this.Work 'pending.json'
        if (Test-Path -LiteralPath $pending) {
            throw ('An interrupted update needs recovery. Keep Minecraft closed; backup details: ' + $pending)
        }
        if ($installed.Count -eq 1 -and $installed[0].Name -ceq $manifest.fileName -and
            [CdClientUpdater]::Hash($installed[0].FullName) -ceq $manifest.sha256) {
            Write-Host 'Already current. Open your copied profile in CurseForge and press Play.'
            return 0
        }
        if ($checkOnly) { Write-Host 'A different TEST revision is available. Check-only: no files changed.'; return 10 }

        $this.Environment.EnsureClosed()
        [void][IO.Directory]::CreateDirectory($this.Work)
        $lock = [IO.File]::Open((Join-Path $this.Work 'update.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
        $stage = Join-Path $this.Work ([guid]::NewGuid().ToString('N') + '.download')
        try {
            if (Test-Path -LiteralPath $pending) { throw 'Another update left a recovery journal. Keep Minecraft closed.' }
            Write-Host 'Downloading and checking the deployed TEST build...'
            $this.Feed.Download($snapshot.Revision, $manifest.fileName, $stage, [int64]$manifest.size)
            [CdClientUpdater]::VerifyJar($stage, $manifest)
            $this.Environment.EnsureClosed()
            $installed = $this.Installed()
            $backup = Join-Path $this.Work ('backups\' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
            [void][IO.Directory]::CreateDirectory($backup)
            $record = [ordered]@{ state = 'swapping'; revision = $snapshot.Revision; sourceCommit = $manifest.sourceCommit;
                sha256 = $manifest.sha256; backup = $backup; target = $manifest.fileName; oldFiles = @($installed | ForEach-Object { $_.Name }) }
            $record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $pending -Encoding UTF8 -ErrorAction Stop
            $activated = $false
            try {
                foreach ($old in $installed) { [IO.File]::Move($old.FullName, (Join-Path $backup $old.Name)) }
                [IO.File]::Move($stage, $target)
                $activated = $true
                [CdClientUpdater]::VerifyJar($target, $manifest)
                $record.state = 'complete'
                $record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'receipt.json') -Encoding UTF8 -ErrorAction Stop
                Remove-Item -LiteralPath $pending -ErrorAction Stop
            } catch {
                $failure = $_
                try {
                    if ($activated) { [IO.File]::Move($target, (Join-Path $backup 'failed-new.jar')) }
                    foreach ($old in $installed) {
                        $saved = Join-Path $backup $old.Name
                        if (Test-Path -LiteralPath $saved) { [IO.File]::Move($saved, $old.FullName) }
                    }
                    Remove-Item -LiteralPath $pending -ErrorAction Stop
                } catch { throw ('Update recovery needs attention. Keep Minecraft closed; backups: ' + $backup) }
                throw ('Update failed; previous JARs restored. ' + $failure.Exception.Message)
            }
            Write-Host ('Updated successfully. Previous JARs: ' + $backup)
            Write-Host 'Open your copied profile in CurseForge and press Play.'
            return 0
        } finally {
            if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -ErrorAction SilentlyContinue }
            $lock.Dispose()
        }
    }
}

if (!$LibraryOnly) {
    $ErrorActionPreference = 'Stop'
    try { exit ([CdClientUpdater]::new($PSScriptRoot, [CdTestFeed]::new(), [CdClientEnvironment]::new()).Run($CheckOnly.IsPresent)) }
    catch { Write-Host ('Update stopped: ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
}
