#!/usr/bin/env bash
# 인프라 접속 점검 스크립트 (macOS / Linux)
# 사용법:
#   ./health-check.sh                 # 기본 허브(172.24.121.128) 점검
#   ./health-check.sh 192.168.0.10    # 다른 허브 IP 지정
#   HUB_HOST=10.0.0.5 ./health-check.sh
#
# 점검 대상: PostgreSQL(5432), 백엔드 API(8080), 프론트엔드(80)

set -u

HUB_HOST="${1:-${HUB_HOST:-172.24.121.128}}"
DB_PORT="${DB_PORT:-5432}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-80}"
DB_USER="${DB_USER:-csagent}"
DB_NAME="${DB_NAME:-csagent}"

GREEN='\033[0;32m'; RED='\033[0;31m'; YEL='\033[0;33m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[OK]${NC}   $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
warn() { echo -e "${YEL}[--]${NC}   $1"; }

echo "================================================"
echo " GGTeam 인프라 점검  (허브: ${HUB_HOST})"
echo "================================================"

# 1) PostgreSQL TCP 포트
if nc -z -G 3 "$HUB_HOST" "$DB_PORT" 2>/dev/null; then
  ok "PostgreSQL 포트 도달 (${HUB_HOST}:${DB_PORT})"
  # psql 있으면 실제 쿼리
  if command -v psql >/dev/null 2>&1; then
    if PGPASSWORD="${DB_PASSWORD:-csagent}" psql -h "$HUB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT 1;" >/dev/null 2>&1; then
      ok "PostgreSQL 쿼리 성공 (DB 살아있음)"
    else
      fail "PostgreSQL 포트는 열렸으나 인증/쿼리 실패 (계정/비번 확인)"
    fi
  else
    warn "psql 미설치 — 포트 도달만 확인 (실쿼리 생략)"
  fi
else
  fail "PostgreSQL 포트 도달 불가 (${HUB_HOST}:${DB_PORT}) — Colima/컨테이너/네트워크 확인"
fi

# 2) 백엔드 헬스체크
BACKEND_URL="http://${HUB_HOST}:${BACKEND_PORT}/actuator/health"
if curl -fsS --max-time 5 "$BACKEND_URL" 2>/dev/null | grep -q '"status":"UP"'; then
  ok "백엔드 헬스체크 UP (${BACKEND_URL})"
else
  warn "백엔드 응답 없음/DOWN (${BACKEND_URL}) — 백엔드 미기동일 수 있음"
fi

# 3) 프론트엔드
FRONTEND_URL="http://${HUB_HOST}:${FRONTEND_PORT}"
if curl -fsS --max-time 5 -o /dev/null "$FRONTEND_URL" 2>/dev/null; then
  ok "프론트엔드 응답 (${FRONTEND_URL})"
else
  warn "프론트엔드 응답 없음 (${FRONTEND_URL}) — 프론트 미기동일 수 있음"
fi

echo "================================================"
echo " 점검 완료"
echo "================================================"
