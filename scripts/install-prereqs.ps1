<#
.SYNOPSIS
  Installs k6 + Wireshark (with Npcap) via winget. RUN AS ADMINISTRATOR.

.NOTES
  Docker Desktop must be installed manually (requires WSL2 setup):
    https://www.docker.com/products/docker-desktop
  During Wireshark install, ENSURE "Npcap" is selected and
  "Support raw 802.11 traffic" / "loopback traffic capture" are checked.
#>
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'

if (-not ([Security.Principal.WindowsPrincipal] `
        [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Run this script in an *elevated* PowerShell (Run as Administrator)."
}

Write-Host ">>> installing k6"        -ForegroundColor Cyan
winget install --id=k6.k6 -e --accept-package-agreements --accept-source-agreements

Write-Host ">>> installing Wireshark" -ForegroundColor Cyan
winget install --id=WiresharkFoundation.Wireshark -e --accept-package-agreements --accept-source-agreements

Write-Host ">>> installing Npcap (loopback support)" -ForegroundColor Cyan
$npcap = Join-Path $env:TEMP "npcap-installer.exe"
Invoke-WebRequest -Uri "https://npcap.com/dist/npcap-1.79.exe" -OutFile $npcap
Start-Process -FilePath $npcap -ArgumentList "/loopback_support=yes /S" -Wait
Remove-Item $npcap -Force

Write-Host "Done. Open a NEW terminal so PATH refreshes, then run scripts\run-load-and-capture.ps1" -ForegroundColor Green
Write-Host "If Docker Desktop is missing, install it manually: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
