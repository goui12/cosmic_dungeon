# CurseForge -> stopped official server. No local-client install or post-publication hashes.
[CmdletBinding()] param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$ReleaseReceipt,
    [switch]$Apply
)
. "$PSScriptRoot\cd-common.ps1"
. "$PSScriptRoot\deploy-server-core.ps1"
. "$PSScriptRoot\official-server-session.ps1"
$release=Get-Content -LiteralPath $ReleaseReceipt -Raw | ConvertFrom-Json
Assert-CDServerReleaseChannel ([string]$release.version)
$c=Get-CDOfficialTarget
$source=Get-Item -LiteralPath $Jar
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
    $recordDir=Join-Path $script:CDCache ('releases\'+$tag+'\official')
    [void][IO.Directory]::CreateDirectory($recordDir)
    $manifestPath=Join-Path $recordDir 'server-installation.json'
    $lock=[IO.File]::Open((Join-Path $script:CDCache 'deployment.lock'),'OpenOrCreate','ReadWrite','None')
    if(Test-Path -LiteralPath $pending){throw 'Unresolved deployment journal; reconcile before another operation.'}
    if(Test-Path -LiteralPath $manifestPath){
        $prior=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        if($prior.State -eq 'complete' -and $prior.Commit -eq $commit -and $prior.Target -ceq $c.Name -and
            $prior.Identity.UserName -ceq $c.UserName -and $prior.Identity.Host -ceq $c.HostName -and
            $prior.Identity.Root -ceq $c.RemoteRoot -and $prior.Identity.HostKeyPin -ceq $c.Pin){
            Write-Output "Existing completed server receipt: $manifestPath"; return
        }
        throw 'Existing release installation record needs reconciliation.'
    }
    $s=Get-CDOfficialTransport
    $propertiesFile=[IO.Path]::GetTempFileName()
    try {
        [void](Receive-CDOfficialFile $s ($c.RemoteRoot+'/server.properties') $propertiesFile 65536)
        $observed=Assert-CDServerProperties ([IO.File]::ReadAllText($propertiesFile)) $c.ExpectedServerPort
    } finally {if(Test-Path -LiteralPath $propertiesFile){Remove-Item -LiteralPath $propertiesFile -Force}}
    $identity=[ordered]@{Host=$c.HostName;UserName=$c.UserName;SftpPort=$c.PortNumber;Root=$c.RemoteRoot;HostKeyPin=$c.Pin;Site=$c.Site;ServerPropertiesPort=$observed.ServerPropertiesPort;OnlineMode=$observed.OnlineMode;VerifiedUtc=[DateTime]::UtcNow.ToString('o')}
    function Read-Shutdown {
        $local=Join-Path $recordDir 'server-shutdown-latest.log'
        $info=Receive-CDOfficialFile $s ($c.RemoteRoot+'/logs/latest.log') $local 33554432
        $line=Assert-CDShutdownTail @(Get-Content -LiteralPath $local -Tail 100)
        return @{CheckedUtc=[DateTime]::UtcNow.ToString('o');LogModified=$info.LastWriteTime.ToString('o');Indicator=$line;Evidence=$local}
    }
    function Get-InstalledRuntime {
        $jars=@(Get-CDOfficialDirectory $s ($c.RemoteRoot+'/mods') | Where-Object {$_.Name -match '^cosmicdungeon-[0-9].*\.jar$'})
        if($jars.Count -ne 1 -or !$jars[0].IsFile -or $jars[0].Name -cnotmatch '^cosmicdungeon-[0-9]+\.[0-9]+\.[0-9]+(?:-(?:alpha|beta)(?:\.[0-9]+)?)?\.jar$'){
            throw 'Unexpected official runtime inventory; nothing replaced.'
        }
        return $jars[0]
    }
    $shutdown=Read-Shutdown
    $remoteMods=$c.RemoteRoot+'/mods'
    $old=Get-InstalledRuntime
    if($old.Name -eq $source.Name){throw 'Target version exists without a completed receipt; reconcile before retry.'}
    $plan=@{Target=$c.Name;Mode=$(if($Apply){'APPLY'}else{'DRY RUN'});Tag=$tag;Commit=$commit;Jar=$source.Name;Bytes=$source.Length;OldJar=$old.Name;Identity=$identity;Shutdown=$shutdown;ClientInstallation='not_requested';HashChecks='publisher receipt only'}
    if(!$Apply){$plan | ConvertTo-Json -Depth 5;return}
    $id=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
    $stageBase=$c.RemoteRoot+'/.cosmic-ai-staging';$backupBase=$c.RemoteRoot+'/.cosmic-ai-backups'
    $manifest=[ordered]@{Id=$id;Target=$c.Name;State='staging';Tag=$tag;Commit=$commit;Jar=$source.Name;Bytes=$source.Length;PublisherSHA256=$release.sha256.main;FileId=$release.files.main.file_id;RemoteStage=$stageBase+'/'+$id+'.jar.staged';OldPath=$remoteMods+'/'+$old.Name;BackupPath=$backupBase+'/'+$id+'-'+$old.Name;TargetPath=$remoteMods+'/'+$source.Name;Identity=$identity;Shutdown=$shutdown;CompletedOperations=@();ServerRestarted=$false;ClientInstalled=$false}
    $save={
        $json=$manifest | ConvertTo-Json -Depth 7
        $json | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        $json | Set-Content -LiteralPath $pending -Encoding UTF8
    }
    & $save
    $rootEntries=@(Get-CDOfficialDirectory $s $c.RemoteRoot)
    foreach($dir in @($stageBase,$backupBase)){
        $name=$dir.Substring($c.RemoteRoot.Length+1)
        $match=@($rootEntries | Where-Object {$_.Name -ceq $name})
        if($match.Count -eq 0){
            $ack=Invoke-CDOfficialCommand $s ('mkdir -- '+(ConvertTo-CDRemoteArgument $s $dir)) 'mkdir'
            if($ack.filename.value -cne $dir){throw 'Unexpected official directory acknowledgement.'}
        } elseif($match.Count -ne 1 -or !$match[0].IsDirectory){throw 'Unsafe official staging or recovery directory.'}
    }
    Send-CDOfficialStage $s $source.FullName $manifest.RemoteStage $source.Length
    $now=Get-InstalledRuntime
    if($now.Name -cne $old.Name -or $now.Length -ne $old.Length -or $now.LastWriteTime -ne $old.LastWriteTime){
        throw 'Official runtime inventory changed during staging.'
    }
    $manifest.Shutdown=Read-Shutdown
    Invoke-CDServerActivation $s $manifest $save
    Remove-Item -LiteralPath $pending
    Write-Output "Official server updated to $($release.version); server remains stopped. Receipt: $manifestPath"
} finally {
    if($lock){$lock.Dispose()};$sourceLock.Dispose()
}
