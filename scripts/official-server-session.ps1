# Release delivery uses the official WinSCP site belonging to the current Windows user.
# Read only endpoint properties. WinSCP alone handles the saved site's authentication.
function Get-CDOfficialTarget {
    [pscustomobject]@{
        Name='official'; Site='cosmicdungeon'; HostName='bos-sr-4-16-7.akliz.net'; PortNumber=22
        UserName='cprees112@gmail.com.463499'; RemoteRoot='/minecraft-neoforge'; ExpectedServerPort=25565
        Pin='ssh-ed25519 255 bagp9xWoTIKXpGgyWzfzT5bzDafSthr5OwZzLDfVeBE'
    }
}
function Assert-CDOfficialSite($Target, $Stored) {
    if ($Stored.HostName -ine $Target.HostName -or $Stored.UserName -cne $Target.UserName -or
        [int]$Stored.PortNumber -ne 22 -or [int]$Stored.Protocol -ne 2) {
        throw 'Saved official WinSCP site has a different host, account, port or file protocol.'
    }
}
function Get-CDOfficialTransport {
    $target=Get-CDOfficialTarget
    $key=[Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Software\Martin Prikryl\WinSCP 2\Sessions\cosmicdungeon')
    if (!$key) {throw 'Saved official WinSCP site cosmicdungeon is missing for the current Windows user.'}
    try {
        Assert-CDOfficialSite $target ([pscustomobject]@{
            HostName=$key.GetValue('HostName'); UserName=$key.GetValue('UserName')
            PortNumber=$key.GetValue('PortNumber',22); Protocol=$key.GetValue('FSProtocol',2)
        })
    } finally {$key.Dispose()}
    $exe=Join-Path ${env:ProgramFiles(x86)} 'WinSCP\WinSCP.com'
    if (!(Test-Path -LiteralPath $exe -PathType Leaf)) {throw 'Installed WinSCP console was not found.'}
    if (Test-Path -LiteralPath (Join-Path (Split-Path $exe) 'WinSCP.ini')) {throw 'Portable INI configuration would override the verified registry site.'}
    $transport=[pscustomobject]@{Target=$target; Executable=$exe}
    $transport | Add-Member ScriptMethod MoveFile {
        param($from,$to)
        $cmd='mv -- '+(ConvertTo-CDRemoteArgument $this $from)+' '+(ConvertTo-CDRemoteArgument $this $to)
        $ack=Invoke-CDOfficialCommand $this $cmd 'mv'
        if ($ack.filename.value -cne $from -or $ack.destination.value -cne $to) {throw 'Unexpected official rename acknowledgement.'}
    }
    return $transport
}
function ConvertTo-CDScriptArgument([string]$Value) {
    if ([string]::IsNullOrEmpty($Value) -or $Value -match '[\x00-\x1f"%*?\[\]]') {throw 'Unsafe character in a WinSCP operation path.'}
    return '"'+$Value+'"'
}
function ConvertTo-CDRemoteArgument($Transport,[string]$Path) {
    $root=$Transport.Target.RemoteRoot
    if (($Path -cne $root -and !$Path.StartsWith($root+'/',[StringComparison]::Ordinal)) -or
        $Path -match '\\|//|(^|/)\.\.?(/|$)') {throw 'Remote path is outside the official server root.'}
    return ConvertTo-CDScriptArgument $Path
}
function Read-CDOfficialAcknowledgement([xml]$Xml,[string]$Operation) {
    $ns=[Xml.XmlNamespaceManager]::new($Xml.NameTable)
    $ns.AddNamespace('w','http://winscp.net/schema/session/1.0')
    $ops=$Xml.SelectNodes("/w:session/w:$Operation",$ns)
    if ($ops.Count -ne 1 -or $ops[0].SelectSingleNode('w:result[@success="true"]',$ns) -eq $null -or
        $Xml.SelectSingleNode('//w:failure|//w:result[@success="false"]',$ns) -ne $null) {
        throw "Missing or uncertain WinSCP $Operation acknowledgement."
    }
    return $ops[0]
}
function Invoke-CDOfficialCommand($Transport,[string]$Command,[string]$Operation) {
    $target=$Transport.Target
    $work=Join-Path ([IO.Path]::GetTempPath()) ('cd-winscp-'+[Guid]::NewGuid().ToString('N'))
    [void][IO.Directory]::CreateDirectory($work)
    try {
        $scriptPath=Join-Path $work 'operation.txt';$xmlPath=Join-Path $work 'result.xml'
        # Absolute target settings also pin the effective connection if the profile changes after preflight.
        $open='open "'+$target.Site+'" -hostkey="'+$target.Pin+'" -username="'+$target.UserName+'" -timeout=20 -rawsettings FSProtocol=2 HostName='+$target.HostName+' PortNumber=22'
        [IO.File]::WriteAllLines($scriptPath,[string[]]@('option batch abort','option confirm off','option reconnecttime 15',$open,$Command,'exit'),[Text.Encoding]::UTF8)
        & $Transport.Executable /nointeractiveinput "/script=$scriptPath" "/xmllog=$xmlPath" *> $null
        if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $xmlPath -PathType Leaf)) {throw "Official WinSCP $Operation failed. Check the saved site, pinned host key and server state."}
        $xml=[Xml.XmlDocument]::new();$xml.XmlResolver=$null;$xml.Load($xmlPath)
        return Read-CDOfficialAcknowledgement $xml $Operation
    } finally {Remove-Item -LiteralPath $work -Recurse -Force}
}
function Get-CDOfficialEntry($Node,[string]$Name) {
    $kind=[string]$Node.type.value
    [pscustomobject]@{
        Name=$Name; IsDirectory=($kind -ceq 'D' -or $kind -ceq 'd'); IsFile=($kind -ceq '-')
        Length=$(if ($kind -ceq '-') {[long]$Node.size.value} else {0})
        LastWriteTime=$(if ($Node.SelectSingleNode('*[local-name()="modification"]')) {[datetimeoffset]::Parse($Node.modification.value).UtcDateTime} else {$null})
    }
}
function Get-CDOfficialDirectory($Transport,[string]$Path) {
    $cmd='ls -- '+(ConvertTo-CDRemoteArgument $Transport $Path)
    $ack=Invoke-CDOfficialCommand $Transport $cmd 'ls'
    foreach ($file in $ack.SelectNodes('*[local-name()="files"]/*[local-name()="file"]')) {
        Get-CDOfficialEntry $file ([string]$file.filename.value)
    }
}
function Get-CDOfficialInfo($Transport,[string]$Path) {
    $cmd='stat -- '+(ConvertTo-CDRemoteArgument $Transport $Path)
    $ack=Invoke-CDOfficialCommand $Transport $cmd 'stat'
    if ($ack.filename.value -cne $Path) {throw 'Unexpected official stat path.'}
    return Get-CDOfficialEntry $ack.file $Path
}
function Receive-CDOfficialFile($Transport,[string]$Remote,[string]$Local,[long]$Limit) {
    $info=Get-CDOfficialInfo $Transport $Remote
    if (!$info.IsFile -or $info.Length -gt $Limit) {throw 'Unexpected remote file type or size.'}
    $cmd='get -transfer=binary -resumesupport=off -- '+(ConvertTo-CDRemoteArgument $Transport $Remote)+' '+(ConvertTo-CDScriptArgument $Local)
    $ack=Invoke-CDOfficialCommand $Transport $cmd 'download'
    if ($ack.filename.value -cne $Remote -or !(Test-Path -LiteralPath $Local -PathType Leaf) -or
        (Get-Item -LiteralPath $Local).Length -gt $Limit) {throw 'Unexpected official download acknowledgement.'}
    return $info
}
function Send-CDOfficialStage($Transport,[string]$Source,[string]$Destination,[long]$Bytes) {
    $cmd='put -transfer=binary -resumesupport=off -- '+(ConvertTo-CDScriptArgument $Source)+' '+(ConvertTo-CDRemoteArgument $Transport $Destination)
    $ack=Invoke-CDOfficialCommand $Transport $cmd 'upload'
    if ($ack.destination.value -cne $Destination -or [long]$ack.size.value -ne $Bytes) {throw 'Unexpected official staging acknowledgement.'}
    $stage=Get-CDOfficialInfo $Transport $Destination
    if (!$stage.IsFile -or $stage.Length -ne $Bytes) {throw 'Incomplete official staging transfer.'}
}
