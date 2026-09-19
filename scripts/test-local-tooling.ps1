# Offline safety tests. Does not connect, deploy, clean, or launch Minecraft.
. "$PSScriptRoot\cd-common.ps1"
$script:Passed=0
function Check([bool]$Condition,[string]$Name){if(!$Condition){throw ('FAIL: '+$Name)}; $script:Passed++; Write-Output ('PASS: '+$Name)}
function Must-Throw([scriptblock]$Code,[string]$Pattern,[string]$Name){$message=$null; try{& $Code | Out-Null}catch{$message=$_.Exception.Message}; Check ($null -ne $message -and $message -match $Pattern) $Name}
foreach($file in Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1'){
    $tokens=$null; $errors=$null
    [void][Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors)
    Check ($errors.Count -eq 0) ('syntax: '+$file.Name)
}
$c=Get-CDConfig
Check ($c.HostName -eq 'bos-sr-4-16-7.akliz.net' -and $c.UserName -eq 'cprees112@gmail.com.503323') 'test account allowlist'
Check ($c.SshHostKeyFingerprint -match '^ssh-ed25519 255 [A-Za-z0-9+/]{43}=?$') 'pinned SHA256 key present'
foreach($path in @('/production/mods','/minecraft-neoforge/../world','/minecraft-neoforge/mods/*.jar','/minecraft-neoforge//mods','/minecraft-neoforge/mods\x')){
    Must-Throw {Assert-CDRemotePath $path} 'outside|Unsafe' ('reject unsafe path: '+$path)
}
Assert-CDRemotePath '/minecraft-neoforge/mods/example.jar'
Check $true 'accept exact test-root path'
Must-Throw {& "$PSScriptRoot\build-local.ps1" -Task runClient} 'Dev-client' 'block unapproved Dev client'
if(@(& git -C $script:CDRepo ls-files build).Count){Must-Throw {& "$PSScriptRoot\build-local.ps1" -Task clean} 'tracked' 'preserve tracked jar from clean'}
Must-Throw {& "$PSScriptRoot\cosmic-io.ps1" -Action Upload -LocalPath (Join-Path $script:CDRepo 'AGENTS.md') -RemotePath '/minecraft-neoforge/mods/unapproved.jar'} 'staging-only' 'generic upload cannot bypass coordinated deploy'
if(!$c.ClientPathConfirmed){Must-Throw {Assert-CDClientTarget} 'confirm' 'block unconfirmed client path'}
$plan=(& "$PSScriptRoot\deploy-mod.ps1" | Out-String | ConvertFrom-Json)
Check ($plan.Mode -eq 'DRY RUN') 'deployment defaults to dry-run'
Must-Throw {& "$PSScriptRoot\deploy-mod.ps1" -Apply} 'server is stopped' 'deployment requires stopped-server/client assertions'
# Regression checks for confirmed client and distinct public/internal ports.
Check ($c.ClientPathConfirmed -and (Test-Path -LiteralPath (Join-Path $c.ClientRoot 'mods'))) 'confirmed existing client instance'
Check ($c.GamePort -eq 12250 -and $c.ExpectedServerPort -eq 25565 -and $c.PortNumber -eq 22) 'keep public, internal, and SFTP ports separate'
$observed=Assert-CDServerProperties "server-port=25565`nonline-mode=true`n" 25565
Check ($observed.ServerPropertiesPort -eq 25565 -and $observed.OnlineMode) 'accept unchanged working internal port'
Must-Throw {Assert-CDServerProperties "server-port=12250`nonline-mode=true`n" 25565} 'port|online-mode' 'reject changed internal port instead of repairing server'
Must-Throw {Assert-CDServerProperties "server-port=25565`nonline-mode=false`n" 25565} 'online-mode' 'reject disabled authentication'
Must-Throw {Assert-CDServerProperties "online-mode=true`n" 25565} 'port|online-mode' 'reject missing internal port'
Check (!$c.ServerPropertiesChangesAllowed) 'server.properties changes prohibited'
Must-Throw {& "$PSScriptRoot\cosmic-io.ps1" -Action Upload -LocalPath (Join-Path $script:CDRepo 'AGENTS.md') -RemotePath '/minecraft-neoforge/server.properties' -Apply} 'staging-only' 'block server.properties upload'
Check ($c.ServerControlMethod -eq 'Akliz web panel (Cameron)' -and !$c.AutomatedServerControlVerified) 'manual Akliz lifecycle only'
$mock=[pscustomobject]@{}
$mock | Add-Member ScriptMethod CalculateFileChecksum {param($Algorithm,$Path) return [byte[]](0..31)}
Check ((Get-CDRemoteHash $mock '/minecraft-neoforge/mock-checksum') -eq '000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F') 'convert WinSCP checksum bytes to hex without fallback download'
$originalConfigPath=$script:CDConfigPath
$mockConfig=Join-Path $script:CDCache ('snapshots\offline-config-'+[guid]::NewGuid().ToString('N')+'.json')
try {
    $unconfirmed=Get-Content -LiteralPath $originalConfigPath -Raw | ConvertFrom-Json
    $unconfirmed.ClientPathConfirmed=$false
    $unconfirmed | ConvertTo-Json | Set-Content -LiteralPath $mockConfig -Encoding UTF8
    $script:CDConfigPath=$mockConfig
    Must-Throw {Assert-CDClientTarget} 'confirm' 'still block unconfirmed client path using isolated fixture'
} finally { $script:CDConfigPath=$originalConfigPath; if(Test-Path -LiteralPath $mockConfig){Remove-Item -LiteralPath $mockConfig} }
$script:Passed | ForEach-Object {Write-Output "Offline checks passed: $_. Authentication, live transfers, activation/rollback, UI and multiplayer remain separate tests."}
