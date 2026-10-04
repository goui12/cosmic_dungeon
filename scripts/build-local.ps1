# Wrapper preserves the project's Gradle tasks; adds local logs and build provenance.
[CmdletBinding()] param([ValidateSet('build','runServerData','runClientData','runClient','clean')][string]$Task='build', [switch]$AllowDevClient)
. "$PSScriptRoot\cd-common.ps1"
if ($Task -eq 'runClient' -and !$AllowDevClient) { throw 'Dev-client execution is not realistic licensed-server QA. Ask Cameron before using -AllowDevClient.' }
if ($Task -eq 'clean' -and @(& git -C $script:CDRepo ls-files build).Count) { throw 'clean would delete a tracked build jar. Ask Cameron and preserve that artifact before cleaning.' }
Assert-CDCacheSpace
$stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$log=Join-Path $script:CDCache ('logs\gradle-'+$Task+'-'+$stamp+'.log')
$before=if($Task -eq 'build'){Get-CDSourceFingerprint}else{''}
Push-Location $script:CDRepo
try {
    $ErrorActionPreference='Continue'
    & .\gradlew.bat $Task --console=plain *> $log
    $code=$LASTEXITCODE
} finally { Pop-Location; $ErrorActionPreference='Stop' }
Write-Output "Gradle $Task exit=$code; full output: $log"
Get-Content -LiteralPath $log -Tail 24
if ($code -ne 0) { throw "Gradle task failed (exit $code). No deployment receipt was issued." }
if ($Task -eq 'build') {
    $after=Get-CDSourceFingerprint
    if ($before -ne $after) { throw 'Build inputs changed during the build. Rebuild before deployment.' }
    $properties=Get-Content (Join-Path $script:CDRepo 'gradle.properties') -Raw
    $version=[regex]::Match($properties,'(?m)^mod_version=(.+)$').Groups[1].Value.Trim()
    $jar=Join-Path $script:CDRepo ('build\libs\cosmicdungeon-'+$version+'.jar')
    if (!(Test-Path -LiteralPath $jar)) { throw 'Expected runtime jar was not produced.' }
    @{Task='build';BuiltUtc=[DateTime]::UtcNow.ToString('o');Commit=(& git -C $script:CDRepo rev-parse HEAD);SourceHash=$after;Jar=$jar;JarSHA256=(Get-FileHash -LiteralPath $jar).Hash;Log=$log} | ConvertTo-Json | Set-Content (Join-Path $script:CDCache 'build-receipt.json') -Encoding UTF8
    Write-Output 'Build provenance saved. Build is not proof of multiplayer gameplay QA.'
    & "$PSScriptRoot\publish-test-build.ps1" -Mode Built -ReceiptPath (Join-Path $script:CDCache 'build-receipt.json') -Apply
}
