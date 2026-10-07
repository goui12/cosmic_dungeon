# Dry-run by default; publish through CurseForge before TEST deployment.
[CmdletBinding()] param([Parameter(Mandatory=$true)][string]$Jar,[Parameter(Mandatory=$true)][string]$ReleaseReceipt,[switch]$Apply)
& "$PSScriptRoot\deploy-mod.safe.ps1" @PSBoundParameters
