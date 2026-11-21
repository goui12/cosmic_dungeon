$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
. "$PSScriptRoot\..\deploy-server-core.ps1"
$count=0
function Check([bool]$Ok,[string]$Name){if(!$Ok){throw $Name};$script:count++}
function Reject([scriptblock]$Action,[string]$Name){$failed=$false;try{& $Action}catch{$failed=$true};Check $failed $Name}
foreach($v in @('1.8.0-beta','1.8.0-beta.1','1.8.0')) {Assert-CDServerReleaseChannel $v;$count++}
foreach($v in @('1.7.10-alpha.1','1.8.0-rc.1','', '1.8.0garbage')) {Reject {Assert-CDServerReleaseChannel $v} "Wrong delivery channel: $v"}
$stop='[Server thread/INFO]: Stopping the server'
Check ((Assert-CDShutdownTail @('Starting minecraft server version 1.21.10',$stop)) -eq $stop) 'Latest shutdown accepted'
Check ((Assert-CDShutdownTail @($stop,'Saving players','Saving worlds','ThreadedAnvilChunkStorage: All chunks are saved')) -eq $stop) 'Shutdown saves accepted'
Reject {Assert-CDShutdownTail @('quiet log')} 'Quiet log denied'
Reject {Assert-CDShutdownTail @($stop,'Starting minecraft server version 1.21.10')} 'Restart denied'
Reject {Assert-CDShutdownTail @($stop,'Done (12 seconds)!')} 'Completed restart denied'
Reject {Assert-CDShutdownTail @($stop,'Player joined the game')} 'Resumed activity denied'
Reject {Assert-CDShutdownTail @($stop,'Unknown activity')} 'Unknown later activity denied'
$commit='a'*40
$r=[pscustomobject]@{version='1.6.7-alpha.1';commit=$commit;main_project=1326805;status='submitted';files=@{main=@{file_id=1};loading_companion=@{file_id=2}};sha256=@{main='b'*64}}
Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.7-alpha.1.jar' $commit
$count++
Reject {Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.7-alpha.1-loading-screen.jar' $commit} 'Helper cannot deploy to server'
Reject {Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.6-beta.1.jar' $commit} 'Wrong runtime denied'
Reject {Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.7-alpha.1.jar' ('c'*40)} 'Wrong source denied'
$r.status='pending'
Reject {Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.7-alpha.1.jar' $commit} 'Uncertain publication denied'
$r.status='submitted';$r.files.loading_companion.file_id=0
Reject {Assert-CDPublishedRelease $r 'cosmicdungeon-1.6.7-alpha.1.jar' $commit} 'Incomplete companion denied'
$r.files.loading_companion.file_id=2;$r.version='1.8.0'
Assert-CDPublishedRelease $r 'cosmicdungeon-1.8.0.jar' $commit;$count++
function FakeSession([int]$FailAt){
    $x=[pscustomobject]@{Files=@{'old'=1;'stage'=2};Calls=0;FailAt=$FailAt}
    $x | Add-Member ScriptMethod MoveFile {
        param($from,$to)
        $this.Calls++
        if(!$this.Files.ContainsKey($from)-or$this.Files.ContainsKey($to)){throw 'Unsafe move'}
        $this.Files[$to]=$this.Files[$from];$this.Files.Remove($from)
        # Simulate a server-side successful operation whose ACK is lost.
        if($this.Calls -eq $this.FailAt){throw 'Lost acknowledgement'}
    }
    return $x
}
function Manifest {return [ordered]@{State='staging';OldPath='old';BackupPath='backup';RemoteStage='stage';TargetPath='target';CompletedOperations=@()}}
foreach($fail in @(0,1,2)){
    $s=FakeSession $fail;$m=Manifest;$seen=[Collections.Generic.List[string]]::new()
    $save={$seen.Add([string]$m.State)}
    if($fail){Reject {Invoke-CDServerActivation $s $m $save} "Failure $fail surfaced"
        Check ($m.State -eq 'NEEDS-MANUAL-RECOVERY') "Failure $fail retains pending journal"
        Check ($s.Files.ContainsKey('backup')) "Failure $fail preserves old runtime"
        Check (!$seen.Contains('complete')) "Failure $fail never claims success"
        Check ($s.Calls -eq $fail) "Failure $fail never blindly retries"
    }else{
        Invoke-CDServerActivation $s $m $save
        Check ($m.State -eq 'complete' -and $s.Files['target'] -eq 2 -and $s.Files['backup'] -eq 1) 'Exact staged runtime activated'
        Check (($seen -join ',') -eq 'moving-old,activating,complete') 'Intents journaled before each move'
    }
}
Write-Output "$count deployment safeguard checks passed."

# Official release transport uses a pinned saved site. All operations below are offline fixtures.
. "$PSScriptRoot\..\official-server-session.ps1"
$t=Get-CDOfficialTarget
$site=[pscustomobject]@{HostName=$t.HostName;UserName=$t.UserName;PortNumber=22;Protocol=2}
Assert-CDOfficialSite $t $site
Check ($t.Name -ceq 'official' -and $t.Site -ceq 'cosmicdungeon' -and $t.UserName -cmatch '\.463499$') 'official site and account'
Check ($t.Pin -cmatch '^ssh-ed25519 255 [A-Za-z0-9+/]{43}$') 'single fixed host key'
foreach ($property in @('HostName','PortNumber','Protocol','UserName')) {
    $wrong=$site.PSObject.Copy()
    $wrong.$property=switch($property){HostName{'other.example'} PortNumber{23} Protocol{0} UserName{'cprees112@gmail.com.503323'}}
    Reject {Assert-CDOfficialSite $t $wrong} "misdirected saved site: $property"
}
$transport=[pscustomobject]@{Target=$t}
Check ((ConvertTo-CDRemoteArgument $transport '/minecraft-neoforge/mods/runtime.jar') -ceq '"/minecraft-neoforge/mods/runtime.jar"') 'quoted literal root path'
foreach($bad in @('/other/mod.jar','/minecraft-neoforge/../world','/minecraft-neoforge/*','/minecraft-neoforge/m%TEMP%','/minecraft-neoforge/x"','/minecraft-neoforge//mods')){
    Reject {ConvertTo-CDRemoteArgument $transport $bad} 'unsafe scripting path'
}
$ns='http://winscp.net/schema/session/1.0'
$xml=[xml]"<session xmlns='$ns'><stat><filename value='/minecraft-neoforge/mod.jar'/><file><type value='-'/><size value='42'/><modification value='2026-11-20T04:03:02.000Z'/></file><result success='true'/></stat></session>"
$stat=Read-CDOfficialAcknowledgement $xml 'stat'
$info=Get-CDOfficialEntry $stat.file 'mod.jar'
Check ($info.IsFile -and !$info.IsDirectory -and $info.Length -eq 42 -and $info.LastWriteTime.Kind -eq 'Utc') 'WinSCP stat acknowledgement and metadata'
Reject {Read-CDOfficialAcknowledgement $xml 'upload'} 'different operation'
foreach($content in @("<mv><result success='false'/></mv>","<mv><result success='true'/></mv><failure/>","<mv><result success='true'/></mv><mv><result success='true'/></mv>")){
    Reject {Read-CDOfficialAcknowledgement ([xml]"<session xmlns='$ns'>$content</session>") 'mv'} 'uncertain official rename'
}
Write-Output "$count total deployment safeguard checks passed."
