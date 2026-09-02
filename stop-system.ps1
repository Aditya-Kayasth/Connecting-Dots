# ==============================================================================
# CONNECTING DOTS V2 - STOP ALL SERVICES SCRIPT
# Runs on Windows PowerShell to cleanly terminate backend, frontend, and tunnels
# ==============================================================================
param(
    [switch]$NoExit
)


Write-Host "=========================================" -ForegroundColor Red
Write-Host "  CONNECTING-DOTS: STOPPING ALL SERVICES " -ForegroundColor Red
Write-Host "=========================================" -ForegroundColor Red

# 1. Stop Java processes (Eureka, Gateway, Core, AI microservices)
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "-> Stopped Java microservices." -ForegroundColor Yellow

# 2. Stop Node processes (Next.js frontend)
Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "-> Stopped Next.js Frontend." -ForegroundColor Yellow

# 3. Stop SSH Tunnels (QStash localhost.run SSH tunnel)
Get-Process -Name "ssh" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "-> Stopped SSH Tunnels." -ForegroundColor Yellow

# 4. Clean up any remaining background jobs
Get-Job | Remove-Job -Force -ErrorAction SilentlyContinue

Write-Host "=========================================" -ForegroundColor Green
Write-Host "  ALL SERVICES SUCCESSFULLY TERMINATED  " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

if (-not $NoExit) {
    exit
}
