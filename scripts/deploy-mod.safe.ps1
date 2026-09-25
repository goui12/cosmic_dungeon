# Coordinated TEST deployment. Dry-run default; no wildcards for deletion.
[CmdletBinding()] param([string]$Jar, [switch]$Apply, [switch]$ServerStopped, [switch]$ClientClosed)
. "$PSScriptRoot\cd-common.ps1"
$c=Get-CDConfig
if (!$Jar) { $p=Get-Content (Join-Path $script:CDRepo 'gradle.properties') -Raw; $v=[regex]::Match($p,'(?m)^mod_version=(.+)$').Groups[1].Value.Trim(); $Jar=Join-Path $script:CDRepo ('build\libs\cosmicdungeon-'+$v+'.jar') }
$source=Get-Item -LiteralPath $Jar
if ($source.Name -notmatch '^cosmicdungeon-[A-Za-z0-9_.+-]+\.jar$' -or $source.Name -match '(-sources|-dev|-api)\.jar$') { throw 'Select the exact CosmicDungeon runtime jar, not a sources/dev/api jar.' }
$hash=(Get-FileHash -LiteralPath $source.FullName -Algorithm SHA256).Hash
$plan=[ordered]@{Mode='DRY RUN';Jar=$source.FullName;SHA256=$hash;ClientMods=(Join-Path $c.ClientRoot 'mods');ClientConfirmed=$c.ClientPathConfirmed;RemoteAccount=$c.UserName;RemoteMods=($c.RemoteRoot+'/mods');Requires='successful build receipt, confirmed client path, server stopped, client closed'}
if (!$Apply) { $plan | ConvertTo-Json; return }
if (!$ServerStopped -or !$ClientClosed) { throw 'Confirm the test server is stopped AND client closed before passing -ServerStopped -ClientClosed. SFTP cannot verify server process state.' }
Assert-CDClientTarget
Assert-CDCacheSpace
$receiptPath=Join-Path $script:CDCache 'build-receipt.json'
if (!(Test-Path $receiptPath)) { throw 'Run scripts/build-local.ps1 -Task build first; missing successful-build provenance.' }
$receipt=Get-Content $receiptPath -Raw | ConvertFrom-Json
if ($receipt.JarSHA256 -ne $hash -or $receipt.Commit -ne (& git -C $script:CDRepo rev-parse HEAD) -or $receipt.SourceHash -ne (Get-CDSourceFingerprint)) { throw 'Jar/source/commit no longer match the successful build receipt. Rebuild; do not deploy a stale jar.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead($source.FullName)
try { $entry=$zip.GetEntry('META-INF/neoforge.mods.toml'); if(!$entry){throw 'Missing NeoForge mod metadata.'}; $reader=[IO.StreamReader]::new($entry.Open()); try{$metadata=$reader.ReadToEnd()}finally{$reader.Dispose()}; if($metadata -notmatch 'modId\s*=\s*"cosmicdungeon"'){throw 'Jar does not identify as CosmicDungeon.'} } finally { $zip.Dispose() }
$pending=Join-Path $script:CDCache 'deploy-pending.json'
$lock=[IO.File]::Open((Join-Path $script:CDCache 'deployment.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
$s=$null; $manifest=$null; $remoteActivated=$false; $localActivated=$false; $localStage=$null
try {
    if(Test-Path $pending){throw 'Unresolved deployment journal exists. Recover it before another deployment or restart.'}
    $s=New-CDSession
    $identity=Test-CDRemoteIdentity $s
    $id=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
    $backup=Join-Path $script:CDCache ('backups\'+$id)
    [void][IO.Directory]::CreateDirectory($backup)
    $localBackup=Join-Path $backup 'client'; [void][IO.Directory]::CreateDirectory($localBackup)
    $localMods=Join-Path $c.ClientRoot 'mods'; $remoteMods=$c.RemoteRoot+'/mods'
    $remoteBackupBase=$c.RemoteRoot+'/.cosmic-ai-backups'
    $remoteStageBase=$c.RemoteRoot+'/.cosmic-ai-staging'
    foreach($dir in @($remoteBackupBase,$remoteStageBase)){if(!$s.FileExists($dir)){$s.CreateDirectory($dir)}}
    $remoteBackup=$remoteBackupBase+'/'+$id; $s.CreateDirectory($remoteBackup)
    $remoteStage=$remoteStageBase+'/'+$id+'.jar.staged'
    $localStage=Join-Path $localMods ($id+'.jar.staged')
    $target=Join-Path $localMods $source.Name; $remoteTarget=$remoteMods+'/'+$source.Name
    $oldLocal=@(Get-ChildItem -LiteralPath $localMods -File | Where-Object {$_.Name -match '^cosmicdungeon-.*\.jar$'})
    $oldRemote=@($s.ListDirectory($remoteMods).Files | Where-Object {!$_.IsDirectory -and $_.Name -match '^cosmicdungeon-.*\.jar$'})
    $manifest=[ordered]@{Id=$id;State='staging';Commit=$receipt.Commit;Jar=$source.Name;SHA256=$hash;ClientMods=$localMods;RemoteMods=$remoteMods;LocalBackup=$localBackup;RemoteBackup=$remoteBackup;RemoteStage=$remoteStage;LocalStage=$localStage;OldLocal=@($oldLocal | ForEach-Object {$_.Name});OldRemote=@($oldRemote | ForEach-Object {$_.Name});CompletedOperations=@();Identity=$identity}
    $manifestPath=Join-Path $backup 'manifest.json'
    function Save-DeploymentJournal { $manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding UTF8; $manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $pending -Encoding UTF8 }
    Save-DeploymentJournal
    Copy-Item -LiteralPath $source.FullName -Destination $localStage
    if((Get-FileHash -LiteralPath $localStage).Hash -ne $hash){throw 'Local staging hash mismatch.'}
    $s.PutFiles($source.FullName,[WinSCP.RemotePath]::EscapeFileMask($remoteStage),$false).Check()
    if((Get-CDRemoteHash $s $remoteStage) -ne $hash){throw 'Remote staging hash mismatch.'}
    $manifest.State='swapping'; Save-DeploymentJournal
    foreach($old in $oldRemote){$s.MoveFile($remoteMods+'/'+$old.Name,$remoteBackup+'/'+$old.Name); $manifest.CompletedOperations+=('remote-backup:'+ $old.Name); Save-DeploymentJournal}
    $s.MoveFile($remoteStage,$remoteTarget); $remoteActivated=$true
    $manifest.CompletedOperations+='remote-activated'; Save-DeploymentJournal
    foreach($old in $oldLocal){Move-Item -LiteralPath $old.FullName -Destination (Join-Path $localBackup $old.Name); $manifest.CompletedOperations+=('client-backup:'+ $old.Name); Save-DeploymentJournal}
    Move-Item -LiteralPath $localStage -Destination $target; $localActivated=$true
    $manifest.CompletedOperations+='client-activated'; Save-DeploymentJournal
    if((Get-FileHash -LiteralPath $target).Hash -ne $hash -or (Get-CDRemoteHash $s $remoteTarget) -ne $hash){throw 'Post-deployment hash verification failed.'}
    $manifest.State='complete'; $manifest['CompletedUtc']=[DateTime]::UtcNow.ToString('o'); Save-DeploymentJournal
    Remove-Item -LiteralPath $pending
    Write-Output "Verified same jar on client and TEST server. Backup manifest: $manifestPath"
    Write-Output 'Server remains stopped. Restart through verified hosting controls, then launch the licensed client.'
} catch {
    if($null -ne $manifest){
        $manifest.State='rolling-back'; try { Save-DeploymentJournal } catch { Write-Warning 'Journal write failed; attempting rollback regardless.' }
        try {
            if($remoteActivated -and $s.FileExists($remoteTarget)){$s.MoveFile($remoteTarget,$remoteStage+'.failed')}
            if($localActivated -and (Test-Path $target)){Move-Item -LiteralPath $target -Destination (Join-Path $backup 'failed-new.jar')}
            foreach($name in $manifest.OldRemote){if($s.FileExists($remoteBackup+'/'+$name)){if($s.FileExists($remoteMods+'/'+$name)){throw 'Unexpected remote file blocks rollback.'}; $s.MoveFile($remoteBackup+'/'+$name,$remoteMods+'/'+$name)}}
            foreach($name in $manifest.OldLocal){$b=Join-Path $localBackup $name; if(Test-Path $b){if(Test-Path (Join-Path $localMods $name)){throw 'Unexpected local file blocks rollback.'}; Move-Item -LiteralPath $b -Destination (Join-Path $localMods $name)}}
            $manifest.State='rolled-back'; Save-DeploymentJournal
            Remove-Item -LiteralPath $pending
        } catch { $manifest.State='NEEDS-MANUAL-RECOVERY'; Save-DeploymentJournal }
        throw "Deployment failed; state=$($manifest.State). Inspect $manifestPath. Keep server stopped until both targets are verified."
    }
    throw
} finally {
    if($localStage -and (Test-Path -LiteralPath $localStage)){Remove-Item -LiteralPath $localStage}
    try {if($null -ne $s){$s.Dispose()}} finally {$lock.Dispose()}
}


# Publish only after the completed deployment transaction released its lock.
# GitHub failure must not trigger the installation rollback above.
if ($null -ne $manifest -and $manifest.State -eq 'complete') {
    try {
        & "$PSScriptRoot\publish-test-build.ps1" -Mode Deployed -ReceiptPath $manifestPath -Apply
    } catch {
        throw ("TEST/client deployment is COMPLETE, but TEST download publication failed. Retry publish-test-build.ps1 with manifest " + $manifestPath + ". Cause: " + $_.Exception.Message)
    }
}
