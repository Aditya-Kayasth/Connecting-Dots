# ==============================================================================
# CONNECTING DOTS V2 - MASTER START ALL SERVICES SCRIPT
# Starts Eureka, Gateway, Core, AI Services, and Frontend
# ==============================================================================

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  CONNECTING-DOTS: MASTER SYSTEM LAUNCH  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$ROOT_DIR = "E:\GIT_HUB\Connecting-Dots-v2"

# 1. Ensure all existing processes are cleanly stopped
if (Test-Path "$ROOT_DIR\stop-system.ps1") {
    & "$ROOT_DIR\stop-system.ps1" -NoExit
}
Start-Sleep -Seconds 2

# 2. Select Connection Mode
Write-Host ""
Write-Host "Select AI Webhook Mode:" -ForegroundColor Yellow
Write-Host "1) Local Offline Mode (Uses local http://localhost:8082/api/v1/ai/webhook - Recommended)" -ForegroundColor White
Write-Host "2) Cloud Tunnel Mode  (Establishes localhost.run SSH tunnel for QStash Cloud)" -ForegroundColor White
$choice = Read-Host "Enter choice [1 or 2, default: 1]"

$aiWebhookUrl = "http://localhost:8082/api/v1/ai/webhook"

if ($choice -eq "2") {
    Write-Host ""
    Write-Host "Establishing localhost.run SSH tunnel for QStash AI webhooks..." -ForegroundColor Yellow
    $tunnelJob = Start-Job -ScriptBlock {
        ssh -R 80:localhost:8082 nokey@localhost.run 2>&1
    }
    
    $tunnelUrl = $null
    $maxRetries = 30
    $retry = 0
    
    Write-Host "Waiting for tunnel URL allocation..." -NoNewline
    while ($retry -lt $maxRetries -and -not $tunnelUrl) {
        Start-Sleep -Seconds 1
        Write-Host "." -NoNewline
        $logs = Receive-Job -Job $tunnelJob
        foreach ($line in $logs) {
            if ($line -match "https://[a-zA-Z0-9\.\-]+\.[a-zA-Z]{2,}") {
                $tunnelUrl = $matches[0]
                break
            }
        }
        $retry++
    }
    Write-Host ""
    
    if (-not $tunnelUrl) {
        Write-Host "-> Tunnel warning: Could not parse tunnel URL. Using local fallback." -ForegroundColor DarkYellow
    } else {
        Write-Host "-> Success! New Tunnel URL: $tunnelUrl" -ForegroundColor Green
        $aiWebhookUrl = "$tunnelUrl/api/v1/ai/webhook"
    }
} else {
    Write-Host "-> Using Local Offline Mode." -ForegroundColor Green
}

# 3. Update AI_WEBHOOK_URL in backend .env configuration
Write-Host ""
Write-Host "Updating AI_WEBHOOK_URL in .env configuration..." -ForegroundColor Yellow
$backendEnvPath = "$ROOT_DIR\connecting-dots-backend\.env"
if (Test-Path $backendEnvPath) {
    $content = Get-Content $backendEnvPath
    if ($content -match "AI_WEBHOOK_URL=") {
        $updated = $content -replace "AI_WEBHOOK_URL=.*", "AI_WEBHOOK_URL=$aiWebhookUrl"
    } else {
        $updated = $content + "`nAI_WEBHOOK_URL=$aiWebhookUrl"
    }
    $updated | Set-Content $backendEnvPath
    Write-Host "-> Updated backend .env successfully: $aiWebhookUrl" -ForegroundColor Green
}

# 4. Launch Microservices in separate terminal windows
Write-Host ""
Write-Host "Launching Backend Microservices..." -ForegroundColor Yellow

# Eureka Server (Port 8761)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\connecting-dots-backend\eureka-server'; Write-Host '=== EUREKA SERVER (Port 8761) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 5

# Gateway Service (Port 8080)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\connecting-dots-backend\gateway-service'; Write-Host '=== GATEWAY SERVICE (Port 8080) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

# Core Service (Port 8081)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\connecting-dots-backend\core-service'; Write-Host '=== CORE SERVICE (Port 8081) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

# AI Service (Port 8082)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\connecting-dots-backend\ai-service'; Write-Host '=== AI SERVICE (Port 8082) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

# 5. Launch Next.js Frontend (Port 3000)
Write-Host ""
Write-Host "Launching Next.js Frontend..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\connecting-dots-frontend'; Write-Host '=== NEXT.JS FRONTEND (Port 3000) ===' -ForegroundColor Green; npm run dev"

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  SYSTEM LAUNCH INITIATED!              " -ForegroundColor Green
Write-Host "  Frontend: http://localhost:3000       " -ForegroundColor White
Write-Host "  Eureka:   http://localhost:8761       " -ForegroundColor White
Write-Host "  Gateway:  http://localhost:8080       " -ForegroundColor White
Write-Host "  AI Webhook: $aiWebhookUrl             " -ForegroundColor White
Write-Host "  To stop all services: .\stop-system.ps1" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Green
