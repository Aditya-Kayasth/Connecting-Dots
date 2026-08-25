Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  CONNECTING-DOTS: FULL AUTO RESTART & TEST" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Kill all running Java and SSH instances
Write-Host "[1/5] Stopping active Java processes and SSH tunnels..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "ssh" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

if (Test-Path "tunnel_out.txt") { Remove-Item "tunnel_out.txt" }
if (Test-Path "tunnel_err.txt") { Remove-Item "tunnel_err.txt" }

# 2. Start SSH tunnel and capture the new URL
Write-Host "[2/5] Establishing new localhost.run SSH tunnel..." -ForegroundColor Yellow
Start-Process "ssh" -ArgumentList "-R 80:localhost:8080 nokey@localhost.run" -NoNewWindow -RedirectStandardOutput "tunnel_out.txt" -RedirectStandardError "tunnel_err.txt"

Write-Host "Waiting for tunnel URL allocation..." -ForegroundColor Cyan
$tunnelUrl = $null
for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep -Seconds 1
    $content = ""
    if (Test-Path "tunnel_out.txt") { $content += Get-Content "tunnel_out.txt" -Raw -ErrorAction SilentlyContinue }
    if (Test-Path "tunnel_err.txt") { $content += Get-Content "tunnel_err.txt" -Raw -ErrorAction SilentlyContinue }
    
    if ($content -match '(https://[a-zA-Z0-9-]+\.lhr\.life)') {
        $tunnelUrl = $matches[1]
        break
    }
}

if (-not $tunnelUrl) {
    Write-Host "ERROR: Could not capture tunnel URL. Check your SSH connection." -ForegroundColor Red
    exit
}

Write-Host "-> Success! New Tunnel URL: $tunnelUrl" -ForegroundColor Green

# 3. Update .env inside core-service folder
Write-Host "[3/5] Updating AI_WEBHOOK_URL in core-service\.env..." -ForegroundColor Yellow
$envFile = ".\core-service\.env"
$targetWebhook = "$tunnelUrl/api/v1/ai/webhook"

if (Test-Path $envFile) {
    $envContent = Get-Content $envFile -Raw
    if ($envContent -match 'AI_WEBHOOK_URL=.*') {
        $envContent = $envContent -replace 'AI_WEBHOOK_URL=.*', "AI_WEBHOOK_URL=$targetWebhook"
    } else {
        $envContent += "`nAI_WEBHOOK_URL=$targetWebhook"
    }
    Set-Content -Path $envFile -Value $envContent
    Write-Host "-> Updated core-service\.env successfully!" -ForegroundColor Green
} else {
    Write-Host "ERROR: .env file not found at $envFile!" -ForegroundColor Red
    exit
}

# 4. Automatically boot up all microservices in separate windows
Write-Host "[4/5] Launching Microservices..." -ForegroundColor Yellow

# Eureka Server (Registry first)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd eureka-server; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 4

# Gateway Service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd gateway-service; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 4

# AI Service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ai-service; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 4

# Core Service (Boots last, triggering the automated seeder test)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd core-service; .\mvnw.cmd spring-boot:run"

Write-Host "[5/5] All services are launching automatically!" -ForegroundColor Cyan
Write-Host "Watch the AI Service and Core Service windows to see the live Gemini extraction execute!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan