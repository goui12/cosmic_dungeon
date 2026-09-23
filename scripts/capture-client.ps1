# Explicit, finite process sampling. No profiler, JVM attach, or runtime mod installed.
[CmdletBinding()] param([ValidateRange(5,300)][int]$Seconds=30, [ValidateRange(5,30)][int]$IntervalSeconds=5)
. "$PSScriptRoot\cd-common.ps1"
$c=Get-CDConfig
Assert-CDCacheSpace
$start=[DateTime]::UtcNow; $end=$start.AddSeconds($Seconds)
$records=[Collections.Generic.List[object]]::new(); $previous=@{}
$path=Join-Path $script:CDCache ('logs\client-process-'+$start.ToString('yyyyMMddTHHmmssfffZ')+'.csv')
while([DateTime]::UtcNow -lt $end){
    $now=[DateTime]::UtcNow
    # Read command lines solely to select this instance; never persist or print them.
    $targets=@(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.IndexOf($c.ClientRoot,[StringComparison]::OrdinalIgnoreCase) -ge 0})
    foreach($item in $targets){
        $p=Get-Process -Id $item.ProcessId -ErrorAction SilentlyContinue
        if($null -eq $p){continue}
        $cpu=$p.TotalProcessorTime.TotalSeconds; $percent=$null
        if($previous.ContainsKey($p.Id)){$old=$previous[$p.Id]; $wall=($now-$old.Time).TotalSeconds; if($wall -gt 0){$percent=[Math]::Round(100*($cpu-$old.Cpu)/$wall/[Environment]::ProcessorCount,2)}}
        $previous[$p.Id]=@{Time=$now;Cpu=$cpu}
        $records.Add([pscustomobject]@{Utc=$now.ToString('o');Pid=$p.Id;CpuPercentWholeMachine=$percent;WorkingSetMiB=[Math]::Round($p.WorkingSet64/1MB,2);PrivateMiB=[Math]::Round($p.PrivateMemorySize64/1MB,2);CpuTotalSeconds=$cpu})
    }
    $remaining=($end-[DateTime]::UtcNow).TotalMilliseconds
    if($remaining -gt 0){Start-Sleep -Milliseconds ([int][Math]::Min($IntervalSeconds*1000,$remaining))}
}
if($records.Count){$records | Export-Csv -LiteralPath $path -NoTypeInformation -Encoding UTF8; Write-Output "Captured $($records.Count) samples: $path"}else{Write-Output 'No running Java process for the target instance was observed. Nothing was launched.'}
Write-Output 'This measures process CPU/RAM only, not FPS, frame time, server TPS/MSPT, or correctness. Use cosmic-io.ps1 -Action Logs for bounded log snapshots.'
