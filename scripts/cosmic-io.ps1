# Safe local/test-server I/O. Status is default; writes require -Apply.
[CmdletBinding()] param(
    [ValidateSet('Status','Probe','List','Download','Upload','Logs','Launch')][string]$Action='Status',
    [string]$RemotePath, [string]$LocalPath, [switch]$Apply
)
. "$PSScriptRoot\cd-common.ps1"
$c = Get-CDConfig
if ($Action -eq 'Status') {
    $state = [ordered]@{CheckedUtc=[DateTime]::UtcNow.ToString('o');Repo=$script:CDRepo;Branch=(& git -C $script:CDRepo branch --show-current);Commit=(& git -C $script:CDRepo rev-parse HEAD);Changes=@(& git -C $script:CDRepo status --short);CredentialSaved=(Test-Path $script:CDCredentialPath);ClientRoot=$c.ClientRoot;ClientPathConfirmed=$c.ClientPathConfirmed;RemoteIdentityConfirmed=$c.RemoteIdentityConfirmed}
    $state | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $script:CDCache 'repo-status.json') -Encoding UTF8
    $state | ConvertTo-Json -Depth 5; return
}
if ($Action -eq 'Launch') {
    Assert-CDClientTarget
    if (!$Apply) { Write-Output 'DRY RUN: would open the existing CurseForge shortcut; login/Play remains with Cameron.'; return }
    if (!(Test-Path -LiteralPath $c.LauncherShortcut)) { throw 'CurseForge shortcut not found.' }
    Start-Process -FilePath $c.LauncherShortcut; return
}
if ($Action -in @('List','Download','Upload')) {
    if (!$RemotePath) { $RemotePath = $c.RemoteRoot }
    Assert-CDRemotePath $RemotePath
}
if ($Action -eq 'Upload') {
    if (!$LocalPath -or !(Test-Path -LiteralPath $LocalPath -PathType Leaf)) { throw 'Supply an existing local file.' }
    if (!$RemotePath.StartsWith($c.RemoteRoot+'/.cosmic-ai-staging/',[StringComparison]::Ordinal)) { throw 'Generic uploads are staging-only. Use deploy-mod.ps1 for coordinated jar replacement.' }
    if ((Get-Item -LiteralPath $LocalPath).Length -gt 67108864) { throw 'Upload exceeds 64 MiB; plan larger transfers explicitly.' }
    if (!$Apply) { Write-Output "DRY RUN: staging upload $LocalPath -> $RemotePath"; return }
}
Assert-CDCacheSpace
$s = New-CDSession
try {
    $identity = Test-CDRemoteIdentity $s
    switch ($Action) {
        'Probe' {
            $result = [ordered]@{Identity=$identity;SftpRead='Verified';SftpWrite='Not tested'}
            if ($Apply) {
                $p = $c.RemoteRoot + '/.cosmic-ai-probe-' + [guid]::NewGuid().ToString('N') + '.txt'
                if ($s.FileExists($p)) { throw 'Unique probe path already exists; no existing file will be replaced.' }
                $bytes = [Text.Encoding]::UTF8.GetBytes('Cosmic Dungeon authorized test SFTP probe')
                $memory = [IO.MemoryStream]::new($bytes)
                $sha=[Security.Cryptography.SHA256]::Create()
                try {
                    $expected=([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','')
                    $s.PutFile($memory,$p)
                    $download=$s.GetFile($p)
                    try { $actual=([BitConverter]::ToString($sha.ComputeHash($download))).Replace('-','') }
                    finally { $download.Dispose() }
                    if($actual -ne $expected){throw 'Probe download hash mismatch.'}
                    $result.SftpWrite='Upload and download hash verified'
                    $result.ProbePath=$p; $result.ProbeBytes=$bytes.Length
                    $result.ExpectedSHA256=$expected; $result.DownloadedSHA256=$actual
                } finally { $memory.Dispose(); $sha.Dispose(); if ($s.FileExists($p)) { [void]$s.RemoveFile($p) } }
                $result.ProbeRemoved=(!$s.FileExists($p))
                if (!$result.ProbeRemoved) { throw 'Probe cleanup not verified; inspect the exact probe path.' }
            }
            $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $script:CDCache 'sftp-probe.json') -Encoding UTF8
            $result | ConvertTo-Json -Depth 5
        }
        'List' {
            $files=@($s.ListDirectory($RemotePath).Files | Where-Object {!$_.IsThisDirectory -and !$_.IsParentDirectory})
            [pscustomobject]@{Path=$RemotePath;Total=$files.Count;Entries=@($files | Select-Object -First 100 Name,Length,IsDirectory,LastWriteTime)} | ConvertTo-Json -Depth 4
        }
        'Download' {
            if (!$LocalPath) { $LocalPath=Join-Path $script:CDCache ('snapshots\'+[guid]::NewGuid().ToString('N')+'-'+[IO.Path]::GetFileName($RemotePath)) }
            if (Test-Path -LiteralPath $LocalPath) { throw 'Destination exists; choose a new local snapshot path.' }
            $downloadInfo=$s.GetFileInfo($RemotePath)
            if ($downloadInfo.IsDirectory) { throw 'Download requires a file, not a directory.' }
            if ($downloadInfo.Length -gt 67108864) { throw 'Single download exceeds 64 MiB; explicitly plan larger transfers.' }
            $s.GetFiles([WinSCP.RemotePath]::EscapeFileMask($RemotePath),$LocalPath,$false).Check()
            [pscustomobject]@{Saved=$LocalPath;SHA256=(Get-FileHash -LiteralPath $LocalPath).Hash} | ConvertTo-Json
        }
        'Upload' {
            if ($s.FileExists($RemotePath)) { throw 'Remote destination already exists; overwriting is not allowed here.' }
            $parent=[WinSCP.RemotePath]::GetDirectoryName($RemotePath)
            if (!$s.FileExists($parent)) { $s.CreateDirectory($parent) }
            $s.PutFiles($LocalPath,[WinSCP.RemotePath]::EscapeFileMask($RemotePath),$false).Check()
            if ((Get-CDRemoteHash $s $RemotePath) -ne (Get-FileHash -LiteralPath $LocalPath).Hash) { throw 'Staged upload checksum mismatch; file not activated.' }
            Write-Output 'Upload hash verified; file staged only, not installed.'
        }
        'Logs' {
            $remote=$c.RemoteRoot+'/logs/latest.log'
            $target=Join-Path $script:CDCache 'logs\server-latest.log'
            $info=$s.GetFileInfo($remote)
            if ($info.Length -gt $c.MaxLogBytes) { throw 'Server log exceeds the 16 MiB snapshot cap. Plan a targeted tail or rotated-log read.' }
            $stampPath=Join-Path $script:CDCache 'logs\server-log-stamp.json'
            $stamp=$info.Length.ToString()+':'+$info.LastWriteTime.ToUniversalTime().ToString('o')
            $old=if(Test-Path $stampPath){Get-Content $stampPath -Raw}else{''}
            if (!(Test-Path $target) -or $old.Trim() -ne $stamp) {
                $s.GetFiles([WinSCP.RemotePath]::EscapeFileMask($remote),($target+'.part'),$false).Check()
                Move-Item -LiteralPath ($target+'.part') -Destination $target -Force
                Set-Content -LiteralPath $stampPath -Value $stamp -Encoding ASCII
            }
            $client=Join-Path $c.ClientRoot 'logs\latest.log'
            if ((Test-Path $client) -and (Get-Item $client).Length -le $c.MaxLogBytes) { Copy-Item -LiteralPath $client -Destination (Join-Path $script:CDCache 'logs\client-latest.log') -Force }
            foreach($side in @('server','client')) { $f=Join-Path $script:CDCache ('logs\'+$side+'-latest.log'); if(Test-Path $f){ $tail=@(Get-Content -LiteralPath $f -Tail 500); [pscustomobject]@{Side=$side;Snapshot=$f;SampledLines=$tail.Count;WarningErrorLines=@($tail | Where-Object {$_ -match '(?i)\bWARN\b|\bERROR\b|Exception|Can.t keep up'}).Count} | ConvertTo-Json -Compress } }
        }
    }
} finally { $s.Dispose() }
