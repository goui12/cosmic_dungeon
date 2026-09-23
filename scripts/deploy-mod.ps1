# Safe replacement entry point. No upload unless -Apply is supplied.
[CmdletBinding()] param([string]$Jar, [switch]$Apply, [switch]$ServerStopped, [switch]$ClientClosed)
& "$PSScriptRoot\deploy-mod.safe.ps1" @PSBoundParameters
