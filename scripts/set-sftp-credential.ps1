# Run in Windows PowerShell -STA. Password input stays on Cameron's desktop.
[CmdletBinding()] param([switch]$Reset)
. "$PSScriptRoot\cd-common.ps1"
if ((Test-Path -LiteralPath $script:CDCredentialPath) -and !$Reset) { Write-Output 'Encrypted credential already exists. Use -Reset to replace it.'; return }
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$c = Get-CDConfig
$form = New-Object Windows.Forms.Form
$form.Text = 'Cosmic Dungeon - LOCAL TEST SFTP credential'
$form.Size = New-Object Drawing.Size(640,350)
$form.StartPosition = 'CenterScreen'; $form.TopMost = $true
$label = New-Object Windows.Forms.Label
$label.Location = New-Object Drawing.Point(18,16); $label.Size = New-Object Drawing.Size(590,110)
$label.Text = "Host: $($c.HostName):22`r`nUser: $($c.UserName)`r`nPassword stays local and is encrypted for this Windows account/computer.`r`nAuthentication uses SSH to this host only; nothing is sent to ChatGPT.`r`nThis does NOT deploy a mod, restart a server, or log into Microsoft."
$form.Controls.Add($label)
$box = New-Object Windows.Forms.TextBox
$box.Location = New-Object Drawing.Point(18,133); $box.Width = 580; $box.UseSystemPasswordChar = $true
$form.Controls.Add($box)
$reuse = New-Object Windows.Forms.CheckBox
$reuse.Location = New-Object Drawing.Point(18,172); $reuse.Size = New-Object Drawing.Size(590,24)
$reuse.Text = 'Use existing COSMIC_SFTP_PASS locally instead of typing (never display it)'
$reuse.Enabled = ![string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable('COSMIC_SFTP_PASS','User'))
$reuse.Add_CheckedChanged({ $box.Enabled = !$reuse.Checked })
$form.Controls.Add($reuse)
$save = New-Object Windows.Forms.Button
$save.Text = 'Save encrypted'; $save.Location = New-Object Drawing.Point(350,230); $save.Width = 125
$save.DialogResult = [Windows.Forms.DialogResult]::OK; $form.Controls.Add($save)
$cancel = New-Object Windows.Forms.Button
$cancel.Text = 'Cancel'; $cancel.Location = New-Object Drawing.Point(490,230); $cancel.Width = 105
$cancel.DialogResult = [Windows.Forms.DialogResult]::Cancel; $form.Controls.Add($cancel)
$form.AcceptButton = $save; $form.CancelButton = $cancel
$secure = $null
try {
    if ($form.ShowDialog() -ne [Windows.Forms.DialogResult]::OK) { return }
    $secure = if ($reuse.Checked) { ConvertTo-SecureString ([Environment]::GetEnvironmentVariable('COSMIC_SFTP_PASS','User')) -AsPlainText -Force } else { ConvertTo-SecureString $box.Text -AsPlainText -Force }
    $box.Clear()
    if (!$secure -or $secure.Length -eq 0) { throw 'Empty input.' }
    $folder = Split-Path $script:CDCredentialPath -Parent
    [void][IO.Directory]::CreateDirectory($folder)
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($sid)
    foreach ($identity in @($sid, [Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $rule = [Security.AccessControl.FileSystemAccessRule]::new($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $acl.AddAccessRule($rule)
    }
    Set-Acl -LiteralPath $folder -AclObject $acl
    $cred = [Management.Automation.PSCredential]::new($c.UserName, $secure)
    $temp = $script:CDCredentialPath + '.new'
    $cred | Export-Clixml -LiteralPath $temp
    Move-Item -LiteralPath $temp -Destination $script:CDCredentialPath -Force
    @{State='Saved; authentication not yet tested'; SavedUtc=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content (Join-Path $script:CDCache 'credential-status.json') -Encoding UTF8
    [void][Windows.Forms.MessageBox]::Show('Encrypted credential saved locally. The old environment variable was left unchanged. No deployment occurred.', 'Cosmic Dungeon')
} catch { [void][Windows.Forms.MessageBox]::Show('Credential was not saved. Check nonempty input and local folder permissions; do not paste the password into chat.', 'Cosmic Dungeon') }
finally { $box.Clear(); $form.Dispose(); if ($secure) { $secure.Dispose() } }
