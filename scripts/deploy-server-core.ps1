# Pure release/deployment guards; no credentials or network connection creation.
function Assert-CDPublishedRelease($Receipt, [string]$JarName, [string]$Commit) {
    $v = [string]$Receipt.version
    if ($v -notmatch '^\d+\.\d+\.\d+-(alpha\.\d+|beta(\.\d+)?)$' -or
        $JarName -cne ('cosmicdungeon-' + $v + '.jar') -or
        $Receipt.commit -cne $Commit -or $Commit -notmatch '^[0-9a-f]{40}$' -or
        $Receipt.main_project -ne 1326805 -or
        $Receipt.status -notin @('submitted','main_uploaded_archive_pending') -or
        !$Receipt.files.main.file_id -or !$Receipt.files.loading_companion.file_id -or
        $Receipt.sha256.main -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'Exact published runtime, companion and source receipt required.'
    }
}
function Assert-CDShutdownTail([string[]]$Lines) {
    $nonempty = @($Lines | Where-Object {$_.Trim()})
    $stop = -1
    for ($i=0; $i -lt $nonempty.Count; $i++) {
        if ($nonempty[$i] -match 'Stopping the server') {$stop=$i}
    }
    if ($stop -lt 0) {throw 'No current shutdown indicator; server state is not established.'}
    for ($i=$stop+1; $i -lt $nonempty.Count; $i++) {
        if ($nonempty[$i] -notmatch '(?i)(Saving (players|worlds|chunks)|saved|ThreadedAnvilChunkStorage|Closing|Stopping|Unloading|Shutting down|All dimensions|Flushing)') {
            throw 'Activity after shutdown indicator requires investigation.'
        }
        if ($nonempty[$i] -match '(?i)(Starting|Done \(|joined the game|logged in|issued server command)') {
            throw 'Server resumed after its shutdown indicator.'
        }
    }
    return $nonempty[$stop]
}
function Invoke-CDServerActivation($Session, [System.Collections.IDictionary]$Manifest, [scriptblock]$SaveJournal) {
    # Save intended operations BEFORE remote renames. An uncertain acknowledgement
    # retains the journal and both paths for reconciliation; never blindly retry.
    try {
        $Manifest.State='moving-old'; & $SaveJournal
        $Session.MoveFile($Manifest.OldPath,$Manifest.BackupPath)
        $Manifest.CompletedOperations+=@('old-moved')
        $Manifest.State='activating'; & $SaveJournal
        $Session.MoveFile($Manifest.RemoteStage,$Manifest.TargetPath)
        $Manifest.CompletedOperations+=@('runtime-activated')
        $Manifest.State='complete'
        $Manifest.CompletedUtc=[DateTime]::UtcNow.ToString('o')
        & $SaveJournal
    } catch {
        $Manifest.State='NEEDS-MANUAL-RECOVERY'
        try {& $SaveJournal} catch {Write-Warning 'Journal update failed; retain its prepared operation and remote files.'}
        throw
    }
}
