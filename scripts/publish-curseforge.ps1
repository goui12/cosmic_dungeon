# Manual fallback publisher; normal release tags publish through GitHub Actions.
[CmdletBinding()]
param([switch]$Upload, [string]$Tag, [int]$LoadingProjectId = 0, [string]$LoadingProjectSlug)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (!$Tag) {
    $properties = Get-Content -LiteralPath (Join-Path $repo 'gradle.properties') -Raw
    $Tag = 'v' + [regex]::Match($properties, '(?m)^mod_version=(.+)$').Groups[1].Value.Trim()
}
$arguments = @((Join-Path $PSScriptRoot 'curseforge_release.py'), 'validate', '--tag', $Tag)
if ($Upload) {
    $arguments[1] = 'upload'
    $arguments += @('--receipt', (Join-Path (Split-Path $repo -Parent) ('CosmicDungeon_AI\releases\' + $Tag + '\curseforge-receipt.json')))
}
if ($LoadingProjectId) { $arguments += @('--loading-project', [string]$LoadingProjectId) }
if ($LoadingProjectSlug) { $arguments += @('--loading-slug', $LoadingProjectSlug) }
$previousToken = $env:CURSEFORGE_API_TOKEN
try {
    if ($Upload -and !$env:CURSEFORGE_API_TOKEN) {
        $credentialPath = Join-Path $env:LOCALAPPDATA 'CosmicDungeon\secrets\curseforge.credential.xml'
        if (Test-Path -LiteralPath $credentialPath) {
            $credential = Import-Clixml -LiteralPath $credentialPath
            if ($credential -isnot [Management.Automation.PSCredential]) { throw 'Invalid CurseForge credential store.' }
            $env:CURSEFORGE_API_TOKEN = $credential.GetNetworkCredential().Password
        } else {
            $env:CURSEFORGE_API_TOKEN = [Environment]::GetEnvironmentVariable('CURSEFORGE_API_TOKEN', 'User')
        }
        if (!$env:CURSEFORGE_API_TOKEN) { throw 'Missing local CurseForge credential. Never paste it into chat.' }
    }
    & python @arguments
    if ($LASTEXITCODE -ne 0) { throw 'CurseForge validation/publication failed. Inspect the non-secret receipt.' }
} finally {
    $env:CURSEFORGE_API_TOKEN = $previousToken
    Remove-Variable credential -ErrorAction SilentlyContinue
}
