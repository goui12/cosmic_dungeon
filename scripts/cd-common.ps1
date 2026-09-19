# Shared, local-only tooling. Never print credential objects or session options.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:CDRepo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$script:CDCache = Join-Path (Split-Path $script:CDRepo -Parent) 'CosmicDungeon_AI'
$script:CDConfigPath = Join-Path $script:CDCache 'config.json'
$script:CDCredentialPath = Join-Path $env:LOCALAPPDATA 'CosmicDungeon\secrets\test-sftp.credential.xml'
function Get-CDConfig {
    if (!(Test-Path -LiteralPath $script:CDConfigPath)) { throw 'Missing local CosmicDungeon_AI/config.json. See docs/LOCAL_AI_WORKFLOW.md.' }
    $c = Get-Content -LiteralPath $script:CDConfigPath -Raw | ConvertFrom-Json
    if ($c.HostName -ne 'bos-sr-4-16-7.akliz.net' -or $c.UserName -ne 'cprees112@gmail.com.503323' -or $c.PortNumber -ne 22) { throw 'Test-server allowlist mismatch. Ask Cameron before changing targets.' }
    if ($c.RemoteRoot -ne '/minecraft-neoforge' -or $c.GamePort -ne 12250) { throw 'Test-server root/port mismatch.' }
    return $c
}
function Import-CDWinScp {
    $c = Get-CDConfig
    $dll = Join-Path $c.WinScpDirectory 'WinSCPnet.dll'
    if (!(Test-Path -LiteralPath $dll)) { throw 'WinSCP .NET assembly not installed.' }
    Add-Type -Path $dll
}
function New-CDSession {
    Import-CDWinScp
    $c = Get-CDConfig
    if ($c.SshHostKeyFingerprint -notmatch '^ssh-ed25519 255 [A-Za-z0-9+/]{43}=?$') { throw 'A pinned SHA256 host fingerprint is required.' }
    if (!(Test-Path -LiteralPath $script:CDCredentialPath)) { throw 'Run scripts/set-sftp-credential.ps1 locally first. Do not send a password through chat.' }
    $cred = Import-Clixml -LiteralPath $script:CDCredentialPath
    if ($cred -isnot [Management.Automation.PSCredential] -or $cred.UserName -ne $c.UserName) { throw 'Invalid local credential.' }
    $o = New-Object WinSCP.SessionOptions -Property @{
        Protocol = [WinSCP.Protocol]::Sftp; HostName = $c.HostName; PortNumber = $c.PortNumber
        UserName = $c.UserName; SecurePassword = $cred.Password
        SshHostKeyFingerprint = $c.SshHostKeyFingerprint; Timeout = [TimeSpan]::FromSeconds(20)
    }
    $s = New-Object WinSCP.Session
    $s.ExecutablePath = Join-Path $c.WinScpDirectory 'WinSCP.exe'
    $s.ReconnectTime = [TimeSpan]::FromSeconds(15)
    try { $s.Open($o); return $s } catch { $s.Dispose(); throw 'SFTP connection failed. Check local credential, pinned host key, and network; no credential has been printed.' }
}
function Assert-CDRemotePath([string]$Path) {
    $root = (Get-CDConfig).RemoteRoot
    if ($Path -ne $root -and !$Path.StartsWith($root + '/', [StringComparison]::Ordinal)) { throw 'Path is outside the test-server root.' }
    if ($Path -match '[\\*?\[\]\r\n]' -or $Path -match '(^|/)\.\.?(/|$)' -or $Path.Contains('//')) { throw 'Unsafe remote path; use one exact absolute path.' }
}
function Assert-CDCacheSpace {
    $size = (Get-ChildItem -LiteralPath $script:CDCache -File -Recurse -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum
    if ($size -gt (Get-CDConfig).CacheBudgetBytes) { throw 'Local AI cache exceeds 512 MiB. Review backups/logs before more downloads; nothing was pruned automatically.' }
}
function Get-CDRemoteHash($Session, [string]$Path) {
    Assert-CDRemotePath $Path
    try { return ([BitConverter]::ToString($Session.CalculateFileChecksum('sha-256', $Path))).Replace('-','') } catch {
        Assert-CDCacheSpace
        $temp = Join-Path $script:CDCache ('snapshots\hash-' + [guid]::NewGuid().ToString('N'))
        try { $Session.GetFiles([WinSCP.RemotePath]::EscapeFileMask($Path), $temp, $false).Check(); return (Get-FileHash -LiteralPath $temp -Algorithm SHA256).Hash }
        finally { if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp } }
    }
}
function Assert-CDServerProperties([string]$Properties, [int]$ExpectedPort) {
    if ($ExpectedPort -lt 1 -or $ExpectedPort -gt 65535) { throw 'Missing/invalid expected internal server port in local configuration.' }
    $port = [regex]::Match($Properties, '(?m)^server-port\s*=\s*(\d+)\s*$').Groups[1].Value
    $online = [regex]::Match($Properties, '(?m)^online-mode\s*=\s*(true|false)\s*$').Groups[1].Value
    if ($port -ne [string]$ExpectedPort -or $online -ne 'true') { throw 'Remote internal server port or online-mode changed. Ask Cameron; never modify server.properties to pass this check.' }
    return [pscustomobject]@{ServerPropertiesPort=[int]$port;OnlineMode=$true}
}
function Test-CDRemoteIdentity($Session) {
    $c = Get-CDConfig
    $p = $c.RemoteRoot + '/server.properties'
    if (!$Session.FileExists($p) -or !$Session.FileExists($c.RemoteRoot + '/mods')) { throw 'Expected server root/mods not present.' }
    if ($Session.GetFileInfo($p).Length -gt 65536) { throw 'Unexpected server.properties size.' }
    $stream = $Session.GetFile($p)
    $reader = New-Object IO.StreamReader($stream)
    try { $properties = $reader.ReadToEnd() } finally { $reader.Dispose() }
    # Akliz public endpoint and server.properties listening port are separate fields.
    $observed = Assert-CDServerProperties $properties ([int]$c.ExpectedServerPort)
    $c.RemoteIdentityConfirmed = $true
    $c | ConvertTo-Json | Set-Content -LiteralPath $script:CDConfigPath -Encoding UTF8
    return [pscustomobject]@{Host=$c.HostName; Root=$c.RemoteRoot; PublicGamePort=$c.GamePort; ServerPropertiesPort=$observed.ServerPropertiesPort; OnlineMode=$observed.OnlineMode; VerifiedUtc=[DateTime]::UtcNow.ToString('o')}
}
function Assert-CDClientTarget {
    $c = Get-CDConfig
    if (!$c.ClientPathConfirmed) { throw 'Cameron must confirm the discovered ACCESS ONLY instance before deploying or launching.' }
    if (!(Test-Path -LiteralPath (Join-Path $c.ClientRoot 'mods'))) { throw 'Client mods directory is missing; do not create a substitute instance.' }
    $running = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($c.ClientRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
    if ($running.Count) { throw 'The target Minecraft client appears to be running. Close it before deployment.' }
}
function Get-CDSourceFingerprint {
    $files = @(& git -C $script:CDRepo ls-files --cached --others --exclude-standard -- src build.gradle settings.gradle gradle.properties gradlew gradlew.bat gradle)
    if ($LASTEXITCODE -ne 0) { throw 'Could not inventory build inputs.' }
    $lines = foreach ($relative in ($files | Sort-Object -Unique)) {
        $full = Join-Path $script:CDRepo $relative
        if (Test-Path -LiteralPath $full -PathType Leaf) { $relative + '=' + (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash } else { $relative + '=DELETED' }
    }
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($lines -join "`n"))))).Replace('-','') } finally { $sha.Dispose() }
}
