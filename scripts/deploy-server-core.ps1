# Pure release/deployment guards; no credentials or network connection creation.
function Assert-CDPublishedRelease($Receipt, [string]$JarName, [string]$Commit) {
    $v = [string]$Receipt.version
    if ($v -notmatch '^\d+\.\d+\.\d+(-(alpha\.\d+|beta(\.\d+)?))?$' -or
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
    # Recognize the vanilla console and NeoForge latest.log layouts. Player chat,
    # commands and other loggers can quote the stop text while the server is active.
    $timestamp = '(?:\[(?:(?:\d{1,2}[A-Za-z]{3}\d{4}|\d{4}-\d{2}-\d{2}) )?\d{2}:\d{2}:\d{2}(?:[.,]\d{1,3})?\] )?'
    $logger = '(?: \[(?:(?:net\.minecraft\.server\.|ne\.mi\.se\.|minecraft/)?MinecraftServer)/?\])?'
    $header = $timestamp + '\[Server thread/INFO\]' + $logger + ': '
    $stopLine = '^' + $header + 'Stopping (?:the )?server$'
    $saveLine = '^(?:' + $header + ')?(?:Saving (?:players|worlds)|Saving chunks for level ''[^'']+''/[a-z0-9_.-]+:[a-z0-9_./-]+|ThreadedAnvilChunkStorage(?: \([^)]+\))?: All (?:chunks|dimensions) are saved)$'

    $nonempty = @($Lines | Where-Object {$_.Trim()})
    $stop = -1
    for ($i=0; $i -lt $nonempty.Count; $i++) {
        if ($nonempty[$i] -cmatch $stopLine) {$stop=$i}
    }
    if ($stop -lt 0) {throw 'No current shutdown indicator; server state is not established.'}
    for ($i=$stop+1; $i -lt $nonempty.Count; $i++) {
        if ($nonempty[$i] -cnotmatch $saveLine) {
            throw 'Activity after shutdown indicator requires investigation.'
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

# Alpha publication is complete without any server I/O (Cameron 2026-10-11).
function Assert-CDServerReleaseChannel([string]$Version) {
    if ($Version -notmatch '^\d+\.\d+\.\d+(-beta(\.\d+)?)?$') {
        throw 'Server delivery is only for Beta and official releases. Alpha builds stop after CurseForge.'
    }
}
