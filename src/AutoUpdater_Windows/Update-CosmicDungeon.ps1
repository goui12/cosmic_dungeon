# Update-CosmicDungeon.ps1
# Scans GitHub build/libs for cosmicdungeon-x.y.z.jar and downloads the highest version
# into the same directory as this script, if it's newer than the local version.
# If it updates: shows a popup + writes to "Cosmic Dungeon Update History" log file.
# If no update: stays silent (no popup).

$ErrorActionPreference = "Stop"

# ---- Config ----
$Owner = "goui12"
$Repo  = "cosmic_dungeon"
$Path  = "build/libs"

# Match cosmicdungeon-1.3.3.jar etc (strictly 3 octets)
$JarRegex = '^cosmicdungeon-(\d+)\.(\d+)\.(\d+)\.jar$'

# ---- Helpers ----
function Parse-VersionFromJarName([string]$name) {
    $m = [regex]::Match($name, $JarRegex)
    if (-not $m.Success) { return $null }

    return [Version]::new(
        [int]$m.Groups[1].Value,
        [int]$m.Groups[2].Value,
        [int]$m.Groups[3].Value
    )
}

function Show-UpdatePopup([Version]$oldVersion, [Version]$newVersion) {
    Add-Type -AssemblyName System.Windows.Forms

    $msg = "Cosmic Dungeon updated!`r`n`r`n" +
           "Previous: $oldVersion`r`n" +
           "New:      $newVersion"

    [System.Windows.Forms.MessageBox]::Show(
        $msg,
        "Cosmic Dungeon Updated",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Information
    ) | Out-Null
}

function Append-UpdateLog([string]$logPath, [Version]$oldVersion, [Version]$newVersion) {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line  = "$stamp  |  $oldVersion -> $newVersion"
    Add-Content -Path $logPath -Value $line -Encoding UTF8
}

function Get-HighestLocalJar([string]$dir) {
    $best = $null

    Get-ChildItem -Path $dir -File -Filter "cosmicdungeon-*.jar" -ErrorAction SilentlyContinue | ForEach-Object {
        $v = Parse-VersionFromJarName $_.Name
        if ($null -ne $v) {
            if ($null -eq $best -or $v -gt $best.Version) {
                $best = [pscustomobject]@{
                    Path    = $_.FullName
                    Name    = $_.Name
                    Version = $v
                }
            }
        }
    }

    return $best
}

# ---- Main ----
$Here = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogPath = Join-Path $Here "Cosmic Dungeon Update History"

# GitHub sometimes needs TLS 1.2 on older Windows
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Local state
$local = Get-HighestLocalJar $Here
$localVersion = if ($local) { $local.Version } else { [Version]"0.0.0" }

Write-Host "Local highest version:" $localVersion

# List directory via GitHub Contents API
$apiUrl = "https://api.github.com/repos/$Owner/$Repo/contents/$Path"

$headers = @{
    "Accept"     = "application/vnd.github+json"
    "User-Agent" = "CosmicDungeonUpdater/1.0"
}

$entries = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method GET

# Find highest remote jar
$bestRemote = $null

foreach ($e in $entries) {
    if ($e.type -ne "file") { continue }

    $v = Parse-VersionFromJarName $e.name
    if ($null -eq $v) { continue }

    if ($null -eq $bestRemote -or $v -gt $bestRemote.Version) {
        $bestRemote = [pscustomobject]@{
            Name        = $e.name
            Version     = $v
            DownloadUrl = $e.download_url
        }
    }
}

if ($null -eq $bestRemote) {
    throw "No matching cosmicdungeon-x.y.z.jar found in $Owner/$Repo/$Path"
}

Write-Host "Remote highest version:" $bestRemote.Version
Write-Host "Remote jar:" $bestRemote.Name

# Compare
if ($bestRemote.Version -le $localVersion) {
    Write-Host "No update needed."
    exit 0
}

# Download to script directory
$OutPath    = Join-Path $Here $bestRemote.Name
$TempPath   = Join-Path $Here ($bestRemote.Name + ".download")
$BackupPath = if ($local) { ($local.Path + ".bak") } else { $null }

Write-Host "Updating:" $localVersion "->" $bestRemote.Version
Write-Host "Downloading:" $bestRemote.DownloadUrl
Write-Host "To:" $OutPath

Invoke-WebRequest -Uri $bestRemote.DownloadUrl -OutFile $TempPath -UseBasicParsing

# Basic sanity check
if ((Get-Item $TempPath).Length -lt 1024) {
    throw "Download looks too small. Aborting to avoid saving a bad file."
}

# Backup the previous highest local jar (if any)
if ($local) {
    Copy-Item -Path $local.Path -Destination $BackupPath -Force
}

# Replace / install new jar
Move-Item -Path $TempPath -Destination $OutPath -Force

# Optional cleanup: remove older versions (keeps the new one + the .bak)
Get-ChildItem -Path $Here -File -Filter "cosmicdungeon-*.jar" | ForEach-Object {
    if ($_.FullName -ne $OutPath) {
        Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
    }
}

# Log + Popup (ONLY when update happened)
Append-UpdateLog -logPath $LogPath -oldVersion $localVersion -newVersion $bestRemote.Version
Write-Host "Done! Updated Cosmic Dungeon to version:" $bestRemote.Version
Show-UpdatePopup -oldVersion $localVersion -newVersion $bestRemote.Version
