#requires -Version 5.1
[CmdletBinding()]
param(
    [ValidateSet('Built','Deployed')][string]$Mode = 'Built',
    [string]$ReceiptPath,
    [switch]$Apply,
    [switch]$LibraryOnly
)

class CdTestBuildPublisher {
    [string]$Repo
    [string]$Cache
    [string]$Remote = 'https://github.com/goui12/cosmic_dungeon.git'
    [string]$Branch = 'test-builds'

    CdTestBuildPublisher([string]$repo, [string]$cache) {
        $this.Repo = $repo
        $this.Cache = $cache
    }

    [string] Git([string]$directory, [string[]]$arguments) {
        $output = @()
        $code = -1
        $previousPreference = Get-Variable -Name ErrorActionPreference -ValueOnly
        $ErrorActionPreference = 'Continue'
        try { $output = @(& git -C $directory @arguments 2>&1); $code = $LASTEXITCODE }
        finally { $ErrorActionPreference = $previousPreference }
        if ($code -ne 0) { throw ('Git ' + $arguments[0] + ' failed: ' + ($output -join [Environment]::NewLine)) }
        return ($output -join [Environment]::NewLine).Trim()
    }

    static [object] Manifest([object]$receipt, [string]$mode, [string]$jar) {
        $commit = [string]$receipt.Commit
        $expected = if ($mode -eq 'Built') { [string]$receipt.JarSHA256 } else { [string]$receipt.SHA256 }
        $name = [IO.Path]::GetFileName($jar)
        if ($commit -cnotmatch '^[0-9a-f]{40}$' -or $expected -notmatch '^[0-9a-fA-F]{64}$' -or
            $name -cnotmatch '^cosmicdungeon-[0-9]+\.[0-9]+\.[0-9]+(?:[-+][A-Za-z0-9._-]+)?\.jar$') {
            throw 'Invalid build identity or runtime JAR name.'
        }
        $file = Get-Item -LiteralPath $jar -ErrorAction Stop
        $actual = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -ne $expected.ToLowerInvariant() -or $file.Length -gt 67108864) { throw 'Receipt/JAR hash mismatch or JAR over 64 MiB.' }
        $fingerprint = ''
        if ($receipt.PSObject.Properties['SourceHash']) { $fingerprint = [string]$receipt.SourceHash }
        return [ordered]@{ schemaVersion = 1; channel = 'TEST'; status = $mode.ToLowerInvariant();
            sourceCommit = $commit; fileName = $name; sha256 = $actual; size = $file.Length;
            minecraft = '1.21.10'; neoForge = '21.10.64';
            sourceFingerprint = $fingerprint; publishedUtc = [DateTime]::UtcNow.ToString('o') }
    }

    [void] Publish([string]$mode, [string]$receiptPath, [bool]$apply) {
        foreach ($remoteArgs in @(@('remote','get-url','origin'), @('remote','get-url','--push','origin'))) {
            if ($this.Git($this.Repo, $remoteArgs) -cne $this.Remote) { throw 'Unexpected source repository origin/push URL.' }
        }
        $receipt = Get-Content -LiteralPath $receiptPath -Raw -ErrorAction Stop | ConvertFrom-Json
        $jar = ''
        $channelDirectory = ''
        if ($mode -eq 'Built') {
            if ($receipt.Task -ne 'build' -or $receipt.Commit -ne $this.Git($this.Repo, @('rev-parse','HEAD')) -or
                $receipt.SourceHash -ne (Get-CDSourceFingerprint)) { throw 'Build receipt no longer matches the current checkout.' }
            $jar = $receipt.Jar
            $channelDirectory = 'latest-built'
        } else {
            if ($receipt.State -ne 'complete' -or (Test-Path (Join-Path $this.Cache 'deploy-pending.json'))) {
                throw 'Only a completed, resolved TEST deployment can be advertised.'
            }
            $config = Get-CDConfig
            if ($receipt.ClientMods -ine (Join-Path $config.ClientRoot 'mods') -or
                $receipt.RemoteMods -cne ($config.RemoteRoot + '/mods')) { throw 'Deployment targets differ from verified TEST configuration.' }
            $jar = Join-Path $receipt.ClientMods $receipt.Jar
            $channelDirectory = 'current-test'
            # SFTP read only: a running server is safe to verify, never changed here.
            $session = New-CDSession
            try {
                $identity = Test-CDRemoteIdentity $session
                if ((Get-CDRemoteHash $session ($receipt.RemoteMods + '/' + $receipt.Jar)) -ne $receipt.SHA256) {
                    throw 'The deployed server no longer matches this receipt.'
                }
            } finally { $session.Dispose() }
        }
        $manifest = [CdTestBuildPublisher]::Manifest($receipt, $mode, $jar)
        if (!$apply) {
            Write-Host ('DRY RUN: publish ' + $channelDirectory + ' to ' + $this.Branch + ' / ' + $manifest.sha256)
            return
        }
        Assert-CDCacheSpace
        $publishingRoot = Join-Path $this.Cache 'publishing'
        [void][IO.Directory]::CreateDirectory($publishingRoot)
        $lock = [IO.File]::Open((Join-Path $publishingRoot 'publish.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
        $checkout = Join-Path $publishingRoot ([guid]::NewGuid().ToString('N'))
        $published = $false
        try {
            [void][IO.Directory]::CreateDirectory($checkout)
            [void]$this.Git($checkout, @('init','--initial-branch=test-builds'))
            [void]$this.Git($checkout, @('remote','add','origin',$this.Remote))
            $existing = $this.Git($checkout, @('ls-remote','--heads','origin',('refs/heads/' + $this.Branch)))
            if ($existing) {
                [void]$this.Git($checkout, @('fetch','--depth=1','origin',($this.Branch + ':refs/remotes/origin/' + $this.Branch)))
                [void]$this.Git($checkout, @('checkout','-B',$this.Branch,('refs/remotes/origin/' + $this.Branch)))
            }
            foreach ($key in @('user.name','user.email')) {
                $value = $this.Git($this.Repo, @('config',$key))
                [void]$this.Git($checkout, @('config',$key,$value))
            }
            $channel = Join-Path $checkout $channelDirectory
            [void][IO.Directory]::CreateDirectory($channel)
            $manifestPath = Join-Path $channel 'manifest.json'
            if (Test-Path -LiteralPath $manifestPath) {
                $previous = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
                $previousJar = Join-Path $channel ([string]$previous.fileName)
                if ($mode -eq 'Deployed' -and $previous.sha256 -eq $manifest.sha256 -and $previous.sourceCommit -eq $manifest.sourceCommit -and
                    $previous.status -eq $manifest.status -and (Test-Path -LiteralPath $previousJar) -and
                    (Get-FileHash -LiteralPath $previousJar).Hash -eq $manifest.sha256) {
                    $published = $true
                    Write-Host ($channelDirectory + ' already matches this verified revision on test-builds.')
                    return
                }
            }
            # This dedicated artifact checkout contains only owned channel files.
            foreach ($old in @(Get-ChildItem -LiteralPath $channel -File)) {
                if ($old.Name -eq 'manifest.json' -or $old.Name -match '^cosmicdungeon-[0-9].*\.jar$') {
                    Remove-Item -LiteralPath $old.FullName -ErrorAction Stop
                }
            }
            Copy-Item -LiteralPath $jar -Destination (Join-Path $channel $manifest.fileName) -ErrorAction Stop
            $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
            @'
# Cosmic Dungeon TEST builds

This artifact-only branch is maintained by scripts/publish-test-build.ps1 on the development branches.
latest-built is the most recent successful testing build. current-test is the revision verified on the TEST server and Cameron's installed client.
The portable updater reads current-test, pinning manifest and JAR to one Git commit. Both slots include a source commit and SHA-256. Builds can retain the same 1.5.1 version and still update correctly.
Do not merge this artifact branch into main. Runtime binaries are intentionally tracked here by Cameron's explicit instruction. Older revisions remain in Git history. Download only; no SFTP or GitHub credentials are required by testers.
'@ | Set-Content -LiteralPath (Join-Path $checkout 'README.md') -Encoding UTF8
            [void]$this.Git($checkout, @('add','--',$channelDirectory,'README.md'))
            $message = 'Publish ' + $channelDirectory + ' ' + $manifest.sourceCommit.Substring(0, 8) + ' ' + $manifest.sha256.Substring(0, 12)
            [void]$this.Git($checkout, @('commit','-m',$message))
            $blobSize = $this.Git($checkout, @('cat-file','-s',('HEAD:' + $channelDirectory + '/' + $manifest.fileName)))
            if ([int64]$blobSize -ne [int64]$manifest.size) { throw 'Artifact was transformed (possibly by Git LFS); refusing an unusable raw download.' }
            # Normal fast-forward push: concurrent remote work is never overwritten.
            [void]$this.Git($checkout, @('push','origin',('HEAD:refs/heads/' + $this.Branch)))
            $head = $this.Git($checkout, @('rev-parse','HEAD'))
            $remoteHead = $this.Git($checkout, @('ls-remote','--heads','origin',('refs/heads/' + $this.Branch)))
            if (!$remoteHead.StartsWith($head + "`t")) { throw 'Remote branch SHA did not match the published commit.' }
            @{ mode = $mode; artifactCommit = $head; sourceCommit = $manifest.sourceCommit; sha256 = $manifest.sha256;
                channel = $channelDirectory; utc = [DateTime]::UtcNow.ToString('o') } |
                ConvertTo-Json | Set-Content -LiteralPath (Join-Path $publishingRoot ('last-' + $mode.ToLowerInvariant() + '.json')) -Encoding UTF8
            $published = $true
            Write-Host ('Published ' + $channelDirectory + ': https://github.com/goui12/cosmic_dungeon/tree/test-builds (' + $head.Substring(0, 8) + ')')
        } finally {
            # Temporary artifact checkout is reproducible from the verified push; retain it on failure.
            if ($published -and (Test-Path -LiteralPath $checkout)) { Remove-Item -LiteralPath $checkout -Recurse -Force }
            $lock.Dispose()
        }
    }
}

if (!$LibraryOnly) {
    $ErrorActionPreference = 'Stop'
    . "$PSScriptRoot\cd-common.ps1"
    if (!$ReceiptPath) {
        if ($Mode -eq 'Deployed') { throw 'Supply the exact completed deployment manifest.' }
        $ReceiptPath = Join-Path $script:CDCache 'build-receipt.json'
    }
    [CdTestBuildPublisher]::new($script:CDRepo, $script:CDCache).Publish($Mode, $ReceiptPath, $Apply.IsPresent)
}
