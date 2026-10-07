# CurseForge -> stopped TEST only. No local-client install or post-publication hashes.
[CmdletBinding()] param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$ReleaseReceipt,
    [switch]$Apply
)
. "$PSScriptRoot\cd-common.ps1"
. "$PSScriptRoot\deploy-server-core.ps1"
$c=Get-CDConfig
$source=Get-Item -LiteralPath $Jar
$release=Get-Content -LiteralPath $ReleaseReceipt -Raw | ConvertFrom-Json
$tag='v'+$release.version
$commit=(& git -C $script:CDRepo rev-parse ($tag+'^{commit}')).Trim()
if($LASTEXITCODE -ne 0){throw 'Release tag is not available locally.'}
Assert-CDPublishedRelease $release $source.Name $commit
# Hold the downloaded immutable release asset against local modification.
$sourceLock=[IO.File]::Open($source.FullName,'Open','Read','Read')
$s=$null; $lock=$null; $manifest=$null
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($source.FullName)
    try {
        $entry=$zip.GetEntry('META-INF/neoforge.mods.toml')
        if(!$entry){throw 'Missing runtime metadata.'}
        $reader=[IO.StreamReader]::new($entry.Open())
        try {$metadata=$reader.ReadToEnd()} finally {$reader.Dispose()}
        if($metadata -notmatch 'modId\s*=\s*"cosmicdungeon"' -or
           $metadata -notmatch ('version\s*=\s*"'+[regex]::Escape($release.version)+'"')){
            throw 'Runtime metadata does not match the published version.'
        }
    } finally {$zip.Dispose()}
    $pending=Join-Path $script:CDCache 'deploy-pending.json'
    $recordDir=Join-Path $script:CDCache ('releases\'+$tag)
    [void][IO.Directory]::CreateDirectory($recordDir)
    $manifestPath=Join-Path $recordDir 'server-installation.json'
    $lock=[IO.File]::Open((Join-Path $script:CDCache 'deployment.lock'),'OpenOrCreate','ReadWrite','None')
    if(Test-Path -LiteralPath $pending){throw 'Unresolved deployment journal; reconcile before another operation.'}
    if(Test-Path -LiteralPath $manifestPath){
        $prior=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        if($prior.State -eq 'complete' -and $prior.Commit -eq $commit){
            Write-Output "Existing completed server receipt: $manifestPath"; return
        }
        throw 'Existing release installation record needs reconciliation.'
    }
    $s=New-CDSession
    $identity=Test-CDRemoteIdentity $s
    function Read-Shutdown {
        $remote=$c.RemoteRoot+'/logs/latest.log'
        $info=$s.GetFileInfo($remote)
        if($info.Length -gt 33554432){throw 'Latest log exceeds bounded shutdown inspection.'}
        $local=Join-Path $recordDir 'server-shutdown-latest.log'
        $s.GetFiles([WinSCP.RemotePath]::EscapeFileMask($remote),$local,$false).Check()
        $line=Assert-CDShutdownTail @(Get-Content -LiteralPath $local -Tail 100)
        return @{CheckedUtc=[DateTime]::UtcNow.ToString('o');LogModified=$info.LastWriteTime.ToString('o');Indicator=$line;Evidence=$local}
    }
    $shutdown=Read-Shutdown
    $remoteMods=$c.RemoteRoot+'/mods'
    $old=@($s.ListDirectory($remoteMods).Files | Where-Object {!$_.IsDirectory -and $_.Name -match '^cosmicdungeon-[0-9].*\.jar$'})
    if($old.Count -ne 1 -or $old[0].Name -match '-(loading-screen|sources|dev|api)\.jar$'){
        throw 'Unexpected server runtime inventory; nothing replaced.'
    }
    $old=$old[0]
    if($old.Name -eq $source.Name){throw 'Target version exists without a completed receipt; reconcile before retry.'}
    $plan=@{Mode=$(if($Apply){'APPLY'}else{'DRY RUN'});Tag=$tag;Commit=$commit;Jar=$source.Name;Bytes=$source.Length;OldJar=$old.Name;Identity=$identity;Shutdown=$shutdown;ClientInstallation='not_requested';HashChecks='publisher receipt only'}
    if(!$Apply){$plan | ConvertTo-Json -Depth 5;return}
    $id=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
    $stageBase=$c.RemoteRoot+'/.cosmic-ai-staging';$backupBase=$c.RemoteRoot+'/.cosmic-ai-backups'
    foreach($dir in @($stageBase,$backupBase)){if(!$s.FileExists($dir)){$s.CreateDirectory($dir)}}
    $manifest=[ordered]@{Id=$id;State='staging';Tag=$tag;Commit=$commit;Jar=$source.Name;Bytes=$source.Length;PublisherSHA256=$release.sha256.main;FileId=$release.files.main.file_id;RemoteStage=$stageBase+'/'+$id+'.jar.staged';OldPath=$remoteMods+'/'+$old.Name;BackupPath=$backupBase+'/'+$id+'-'+$old.Name;TargetPath=$remoteMods+'/'+$source.Name;Identity=$identity;Shutdown=$shutdown;CompletedOperations=@();ServerRestarted=$false;ClientInstalled=$false}
    $save={
        $json=$manifest | ConvertTo-Json -Depth 7
        $json | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        $json | Set-Content -LiteralPath $pending -Encoding UTF8
    }
    & $save
    $s.PutFiles($source.FullName,[WinSCP.RemotePath]::EscapeFileMask($manifest.RemoteStage),$false).Check()
    if($s.GetFileInfo($manifest.RemoteStage).Length -ne $source.Length){throw 'Incomplete staging transfer.'}
    $manifest.Shutdown=Read-Shutdown
    $now=@($s.ListDirectory($remoteMods).Files | Where-Object {!$_.IsDirectory -and $_.Name -match '^cosmicdungeon-[0-9].*\.jar$'})
    if($now.Count -ne 1 -or $now[0].Name -ne $old.Name -or $now[0].Length -ne $old.Length -or $now[0].LastWriteTime -ne $old.LastWriteTime){
        throw 'Server runtime inventory changed during staging.'
    }
    Invoke-CDServerActivation $s $manifest $save
    Remove-Item -LiteralPath $pending
    Write-Output "TEST updated to $($release.version); server remains stopped. Receipt: $manifestPath"
} finally {
    if($s){$s.Dispose()};if($lock){$lock.Dispose()};$sourceLock.Dispose()
}
