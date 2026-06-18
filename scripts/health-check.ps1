# 인프라 접속 점검 스크립트 (Windows PowerShell)
# 사용법:
#   .\health-check.ps1                  # 기본 허브(172.24.121.128) 점검
#   .\health-check.ps1 -HubHost 192.168.0.10
#
# 점검 대상: PostgreSQL(5432), 백엔드 API(8080), 프론트엔드(80)
# 실행 정책 오류 시: powershell -ExecutionPolicy Bypass -File .\health-check.ps1

param(
    [string]$HubHost = "172.24.121.128",
    [int]$DbPort = 5432,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 80,
    [string]$DbUser = "csagent",
    [string]$DbName = "csagent",
    [string]$DbPassword = "csagent"
)

function Write-Ok   ($m) { Write-Host "[OK]   $m" -ForegroundColor Green }
function Write-Fail ($m) { Write-Host "[FAIL] $m" -ForegroundColor Red }
function Write-Warn ($m) { Write-Host "[--]   $m" -ForegroundColor Yellow }

Write-Host "================================================"
Write-Host " GGTeam 인프라 점검  (허브: $HubHost)"
Write-Host "================================================"

# 1) PostgreSQL TCP 포트
$db = Test-NetConnection -ComputerName $HubHost -Port $DbPort -WarningAction SilentlyContinue
if ($db.TcpTestSucceeded) {
    Write-Ok "PostgreSQL 포트 도달 ($HubHost`:$DbPort)"
    if (Get-Command psql -ErrorAction SilentlyContinue) {
        $env:PGPASSWORD = $DbPassword
        $r = & psql -h $HubHost -p $DbPort -U $DbUser -d $DbName -tAc "SELECT 1;" 2>$null
        if ($LASTEXITCODE -eq 0) { Write-Ok "PostgreSQL 쿼리 성공 (DB 살아있음)" }
        else { Write-Fail "포트는 열렸으나 인증/쿼리 실패 (계정/비번 확인)" }
    } else {
        Write-Warn "psql 미설치 — 포트 도달만 확인 (실쿼리 생략)"
    }
} else {
    Write-Fail "PostgreSQL 포트 도달 불가 ($HubHost`:$DbPort) — Colima/컨테이너/네트워크 확인"
}

# 2) 백엔드 헬스체크
$backendUrl = "http://$HubHost`:$BackendPort/actuator/health"
try {
    $resp = Invoke-WebRequest -Uri $backendUrl -TimeoutSec 5 -UseBasicParsing
    if ($resp.Content -match '"status":"UP"') { Write-Ok "백엔드 헬스체크 UP ($backendUrl)" }
    else { Write-Warn "백엔드 응답 형식 이상 ($backendUrl)" }
} catch {
    Write-Warn "백엔드 응답 없음/DOWN ($backendUrl) — 백엔드 미기동일 수 있음"
}

# 3) 프론트엔드
$frontendUrl = "http://$HubHost`:$FrontendPort"
try {
    Invoke-WebRequest -Uri $frontendUrl -TimeoutSec 5 -UseBasicParsing | Out-Null
    Write-Ok "프론트엔드 응답 ($frontendUrl)"
} catch {
    Write-Warn "프론트엔드 응답 없음 ($frontendUrl) — 프론트 미기동일 수 있음"
}

Write-Host "================================================"
Write-Host " 점검 완료"
Write-Host "================================================"
