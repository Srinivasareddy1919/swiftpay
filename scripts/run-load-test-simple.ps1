#!/usr/bin/env pwsh
# Simple load test script - outputs JSON metrics instead of PCAP

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "=== SwiftPay Load Test (Metrics Mode) ===" -ForegroundColor Cyan

# Check if Docker is running
Write-Host "`n[1/4] Checking Docker..." -ForegroundColor Yellow
$dockerRunning = docker info 2>$null
if (-not $?) {
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host "Docker is running." -ForegroundColor Green

# Start services
Write-Host "`n[2/4] Starting SwiftPay services..." -ForegroundColor Yellow
Push-Location $projectRoot
try {
    docker-compose up -d --build
    Write-Host "Waiting 30 seconds for services to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
    
    # Health check
    $maxRetries = 10
    $retry = 0
    while ($retry -lt $maxRetries) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Host "Services are healthy!" -ForegroundColor Green
                break
            }
        } catch {
            $retry++
            Write-Host "Waiting for services... ($retry/$maxRetries)" -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }
    }
    if ($retry -eq $maxRetries) {
        Write-Host "WARNING: Services may not be fully ready" -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}

# Check for k6
Write-Host "`n[3/4] Checking k6..." -ForegroundColor Yellow
$k6Path = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $k6Path) {
    Write-Host "k6 not found. Installing via chocolatey..." -ForegroundColor Yellow
    # Try choco if available
    $chocoPath = Get-Command choco -ErrorAction SilentlyContinue
    if ($chocoPath) {
        choco install k6 -y
    } else {
        Write-Host "ERROR: k6 not installed. Please install manually from https://k6.io/docs/getting-started/installation/" -ForegroundColor Red
        Write-Host "Or run: winget install k6 --source winget" -ForegroundColor Yellow
        exit 1
    }
}

# Run load test with JSON output
Write-Host "`n[4/4] Running load test..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$metricsDir = Join-Path $projectRoot "load-test\results"
New-Item -ItemType Directory -Force -Path $metricsDir | Out-Null

$jsonOutput = Join-Path $metricsDir "load-test-results_$timestamp.json"
$summaryOutput = Join-Path $metricsDir "load-test-summary_$timestamp.txt"

Push-Location (Join-Path $projectRoot "load-test")
try {
    # Run k6 with JSON output
    k6 run --out json=$jsonOutput swiftpay-load.js 2>&1 | Tee-Object -FilePath $summaryOutput
    
    Write-Host "`n=== Load Test Complete ===" -ForegroundColor Green
    Write-Host "Results saved to:" -ForegroundColor Cyan
    Write-Host "  - JSON metrics: $jsonOutput" -ForegroundColor White
    Write-Host "  - Summary: $summaryOutput" -ForegroundColor White
} finally {
    Pop-Location
}

# Generate summary report
Write-Host "`n[Bonus] Generating HTML report..." -ForegroundColor Yellow
$htmlReport = Join-Path $metricsDir "load-test-report_$timestamp.html"

$htmlContent = @"
<!DOCTYPE html>
<html>
<head>
    <title>SwiftPay Load Test Report - $timestamp</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
        h2 { color: #34495e; margin-top: 30px; }
        .metric { background: #ecf0f1; padding: 15px; margin: 10px 0; border-radius: 4px; }
        .metric-name { font-weight: bold; color: #2980b9; }
        .metric-value { font-size: 24px; color: #27ae60; }
        .config { background: #e8f6f3; padding: 15px; border-left: 4px solid #1abc9c; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #3498db; color: white; }
        tr:hover { background: #f5f5f5; }
    </style>
</head>
<body>
    <div class="container">
        <h1>SwiftPay Load Test Report</h1>
        <p><strong>Generated:</strong> $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")</p>
        
        <h2>Test Configuration</h2>
        <div class="config">
            <p><strong>Target TPS:</strong> 250 transactions/second</p>
            <p><strong>Total Transactions:</strong> 1,000,000</p>
            <p><strong>Duration:</strong> ~67 minutes (4000 seconds)</p>
            <p><strong>Endpoint:</strong> POST http://localhost:8081/v1/payments</p>
        </div>
        
        <h2>Architecture Under Test</h2>
        <table>
            <tr><th>Service</th><th>Port</th><th>Technology</th></tr>
            <tr><td>Transaction Gateway</td><td>8081</td><td>Spring Boot + Redis + Kafka</td></tr>
            <tr><td>Ledger Service</td><td>8082</td><td>Spring Boot + PostgreSQL + Kafka</td></tr>
            <tr><td>Analytics Worker</td><td>8083</td><td>Spring Boot + PostgreSQL + Kafka</td></tr>
            <tr><td>Kafka</td><td>9092</td><td>KRaft Mode</td></tr>
            <tr><td>PostgreSQL</td><td>5432</td><td>PostgreSQL 16</td></tr>
            <tr><td>Redis</td><td>6379</td><td>Redis 7</td></tr>
        </table>
        
        <h2>Results</h2>
        <p>See <code>$summaryOutput</code> for detailed k6 metrics.</p>
        <p>See <code>$jsonOutput</code> for raw JSON data.</p>
    </div>
</body>
</html>
"@

$htmlContent | Out-File -FilePath $htmlReport -Encoding UTF8
Write-Host "HTML report: $htmlReport" -ForegroundColor White

Write-Host "`n=== All Done ===" -ForegroundColor Green
Write-Host "To push results to GitHub:" -ForegroundColor Cyan
Write-Host "  git add load-test/results/" -ForegroundColor White
Write-Host "  git commit -m 'Add load test results'" -ForegroundColor White
Write-Host "  git push" -ForegroundColor White
