$ErrorActionPreference = "Stop"

# ==========================================
# Cosmic Dungeon deploy script
# ==========================================

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

$LocalModsDir = "C:\Users\Cameron\curseforge\minecraft\Instances\Cosmic Dungeon ADMINISTRATIVE ACCESS ONLY\mods"

$SftpHost = "bos-sr-4-16-7.akliz.net"
$SftpUser = "cprees112@gmail.com.503323"
$RemoteModsDir = "/minecraft-neoforge/mods"

$SftpPass = [Environment]::GetEnvironmentVariable("COSMIC_SFTP_PASS", "User")
if ([string]::IsNullOrWhiteSpace($SftpPass)) {
    throw "Missing COSMIC_SFTP_PASS environment variable.
Set once with:
[Environment]::SetEnvironmentVariable('COSMIC_SFTP_PASS','your_password_here','User')"
}

# PowerShell 5.1-safe WinSCP lookup
$cmd = Get-Command winscp.com -ErrorAction SilentlyContinue
if ($cmd) {
    $WinScpExe = $cmd.Source
} else {
    $WinScpExe = "C:\Program Files (x86)\WinSCP\WinSCP.com"
    if (-not (Test-Path $WinScpExe)) {
        $WinScpExe = "C:\Program Files\WinSCP\WinSCP.com"
    }
}
if (-not (Test-Path $WinScpExe)) {
    throw "WinSCP.com not found. Install WinSCP or add it to PATH."
}

$LibsDir = Join-Path $ProjectRoot "build\libs"
if (!(Test-Path $LibsDir)) { throw "Libs dir not found: $LibsDir" }

$Jar = Get-ChildItem $LibsDir -Filter "cosmicdungeon-*.jar" |
    Where-Object { $_.Name -notmatch "(-sources|-dev|-api)\.jar$" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $Jar) { throw "No cosmicdungeon-*.jar found in $LibsDir" }

Write-Host "=================================================="
Write-Host "[DEPLOY] ProjectRoot: $ProjectRoot"
Write-Host "[DEPLOY] Using jar:   $($Jar.FullName)"
Write-Host "=================================================="

if (!(Test-Path $LocalModsDir)) { New-Item -ItemType Directory -Path $LocalModsDir | Out-Null }

Get-ChildItem $LocalModsDir -Filter "cosmicdungeon-*.jar" -ErrorAction SilentlyContinue |
    Remove-Item -Force -ErrorAction SilentlyContinue

$LocalTarget = Join-Path $LocalModsDir $Jar.Name
Copy-Item $Jar.FullName $LocalTarget -Force
Write-Host "[DEPLOY] Local deploy done -> $LocalTarget"

$WinScpScript = @"
open sftp://${SftpUser}:`"$SftpPass`"@${SftpHost}/ -hostkey=*
cd ${RemoteModsDir}
rm cosmicdungeon-*.jar
put "$($Jar.FullName)" "${RemoteModsDir}/"
exit
"@

$TempScript = [System.IO.Path]::GetTempFileName()
Set-Content -Path $TempScript -Value $WinScpScript -Encoding ASCII

try {
    & $WinScpExe /ini=nul /script="$TempScript"
    if ($LASTEXITCODE -ne 0) { throw "WinSCP failed with exit code $LASTEXITCODE" }
    Write-Host "[DEPLOY] Remote deploy done -> ${RemoteModsDir}/$($Jar.Name)"
}
finally {
    Remove-Item $TempScript -Force -ErrorAction SilentlyContinue
}

Write-Host "=================================================="
Write-Host "[DEPLOY] DONE: local + remote deployed successfully."
Write-Host "=================================================="