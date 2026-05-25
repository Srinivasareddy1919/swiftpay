<#
.SYNOPSIS
  One-shot SwiftPay load test + PCAP capture + git commit.

.DESCRIPTION
  1. Verifies prerequisites (docker, k6, dumpcap, git).
  2. Brings the SwiftPay stack up via docker compose.
  3. Starts a packet capture on the Npcap loopback adapter.
  4. Runs the k6 load test (250 TPS x 1,000,000 transactions).
  5. Stops the capture, merges ring-buffer files, samples a small artifact.
  6. Commits swiftpay-load.pcapng + results.json via Git LFS.

.PARAMETER Duration
  Test duration in seconds. Default 4000 -> 250 TPS * 4000 = 1,000,000 txns.

.PARAMETER SkipCommit
  Skip the final git commit/push step.

.EXAMPLE
  pwsh .\scripts\run-load-and-capture.ps1
  pwsh .\scripts\run-load-and-capture.ps1 -Duration 60 -SkipCommit
#>
[CmdletBinding()]
param(
    [int]$Duration   = 4000,
    [int]$Rate       = 250,
    [switch]$SkipCommit
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot
Write-Host ">>> Repo: $repoRoot" -ForegroundColor Cyan

# ---------------------------------------------------------------- prerequisites
function Require-Tool($name, $hint) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Missing tool: $name. $hint"
    }
}
Require-Tool docker "Install Docker Desktop: https://www.docker.com/products/docker-desktop"
Require-Tool k6     "Install k6: winget install k6 --source winget"
Require-Tool git    "Install Git for Windows."

$dumpcap = "C:\Program Files\Wireshark\dumpcap.exe"
$mergecap = "C:\Program Files\Wireshark\mergecap.exe"
$editcap  = "C:\Program Files\Wireshark\editcap.exe"
if (-not (Test-Path $dumpcap)) {
    throw "Wireshark (dumpcap) not found at $dumpcap. Install from https://www.wireshark.org/ and enable Npcap loopback support."
}

# ----------------------------------------------------- pick Npcap loopback iface
Write-Host ">>> Locating Npcap loopback adapter..." -ForegroundColor Cyan
$ifaceLine = & $dumpcap -D 2>&1 | Select-String -Pattern 'Loopback' | Select-Object -First 1
if (-not $ifaceLine) { throw "Could not find a 'Loopback' interface. Reinstall Npcap with 'Support loopback traffic capture' checked." }
$ifaceIndex = ([string]$ifaceLine).Split('.')[0].Trim()
Write-Host "    using interface index $ifaceIndex"

# -------------------------------------------------------------- start the stack
Write-Host ">>> docker compose up -d --build" -ForegroundColor Cyan
docker compose up -d --build | Out-Host

Write-Host ">>> waiting for gateway health..." -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8081/actuator/health" -TimeoutSec 2
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ready) { throw "transaction-gateway never became healthy" }
Write-Host "    gateway is up."

# ----------------------------------------------------------------- start capture
$captureBase = Join-Path $repoRoot "swiftpay-load.pcapng"
Get-ChildItem -Path $repoRoot -Filter "swiftpay-load_*.pcapng" -ErrorAction SilentlyContinue | Remove-Item -Force
if (Test-Path $captureBase) { Remove-Item $captureBase -Force }

$capFilter = "tcp port 8081 or tcp port 8082 or tcp port 8083 or tcp port 9092 or tcp port 5432 or tcp port 6379"
Write-Host ">>> starting dumpcap on iface $ifaceIndex (ring buffer 20 x ~200MB)" -ForegroundColor Cyan
$dumpProc = Start-Process -FilePath $dumpcap `
    -ArgumentList @('-i', $ifaceIndex, '-f', "`"$capFilter`"",
                    '-b', 'filesize:200000', '-b', 'files:20',
                    '-w', "`"$captureBase`"") `
    -PassThru -WindowStyle Minimized

Start-Sleep -Seconds 3
if ($dumpProc.HasExited) { throw "dumpcap exited immediately. Run PowerShell as Administrator." }

# -------------------------------------------------------------------- run k6
$env:SWIFTPAY_URL = "http://localhost:8081"
$k6Duration = "${Duration}s"
Write-Host ">>> k6 run: $Rate TPS for $k6Duration  (~$([math]::Round($Rate*$Duration/1e6,2))M txns)" -ForegroundColor Cyan
$resultsJson = Join-Path $repoRoot "results.json"
& k6 run `
    --env SWIFTPAY_URL=$env:SWIFTPAY_URL `
    --out json=$resultsJson `
    -e RATE=$Rate -e DURATION=$k6Duration `
    (Join-Path $repoRoot "load-test\swiftpay-load.js")

# -------------------------------------------------------------- stop capture
Write-Host ">>> stopping dumpcap (PID $($dumpProc.Id))" -ForegroundColor Cyan
Stop-Process -Id $dumpProc.Id -Force
Start-Sleep -Seconds 2

# ----------------------------------------------------- merge + downsample
$ringFiles = Get-ChildItem -Path $repoRoot -Filter "swiftpay-load_*.pcapng" | Sort-Object Name
if ($ringFiles) {
    Write-Host ">>> merging $($ringFiles.Count) ring-buffer files" -ForegroundColor Cyan
    & $mergecap -w $captureBase @($ringFiles.FullName)
    $ringFiles | Remove-Item -Force
}

$sample = Join-Path $repoRoot "swiftpay-load-sample.pcapng"
Write-Host ">>> creating 500k-packet sample for the repo" -ForegroundColor Cyan
& $editcap -c 500000 $captureBase $sample | Out-Null

Write-Host "PCAP files:" -ForegroundColor Green
Get-Item $captureBase, $sample | Format-Table FullName, @{n='SizeMB';e={[math]::Round($_.Length/1MB,1)}}

# ------------------------------------------------------------------ git commit
if ($SkipCommit) {
    Write-Host ">>> skipping git commit (-SkipCommit)" -ForegroundColor Yellow
    return
}

Write-Host ">>> configuring git LFS and committing artifacts" -ForegroundColor Cyan
git lfs install | Out-Host
git lfs track "*.pcapng" "*.pcap" | Out-Host
git add .gitattributes (Split-Path $sample -Leaf) (Split-Path $resultsJson -Leaf)
$bigSize = (Get-Item $captureBase).Length
if ($bigSize -lt 2GB) {
    git add (Split-Path $captureBase -Leaf)
} else {
    Write-Host "    full PCAP is $([math]::Round($bigSize/1GB,2)) GB - committing the sample only" -ForegroundColor Yellow
}
git commit -m "load: 250 TPS x 1M txns - PCAP + k6 results" | Out-Host
Write-Host ">>> done. Push with: git push" -ForegroundColor Green
