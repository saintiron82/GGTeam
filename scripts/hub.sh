#!/usr/bin/env bash
# 인프라 허브 운영 스크립트 (이 머신 = 개발/테스트 중추, macOS)
# 인프라 담당자가 사용. Colima(도커 런타임) + 공유 PostgreSQL을 관리한다.
#
# 사용법:
#   ./hub.sh up        # Colima + postgres 기동 (상시 켜둘 최소 구성)
#   ./hub.sh up-all    # 전체 스택 기동 (postgres + backend + frontend)
#   ./hub.sh status    # 상태 확인
#   ./hub.sh logs      # 컨테이너 로그(follow)
#   ./hub.sh down      # 컨테이너 중지 (볼륨 유지)
#   ./hub.sh stop      # Colima까지 중지

set -u
cd "$(dirname "$0")/.." || exit 1   # 프로젝트 루트로 이동

ensure_colima() {
  if ! colima status >/dev/null 2>&1; then
    echo "[hub] Colima 시작 중..."
    colima start
  else
    echo "[hub] Colima 이미 실행중"
  fi
}

case "${1:-status}" in
  up)
    ensure_colima
    echo "[hub] PostgreSQL(공유 DB) 기동..."
    docker compose up -d postgres
    docker compose ps postgres
    ;;
  up-all)
    ensure_colima
    echo "[hub] 전체 스택 기동 (postgres + backend + frontend)..."
    docker compose up -d --build
    docker compose ps
    ;;
  status)
    echo "=== Colima ===" ; colima status 2>&1 | tail -3
    echo "=== Containers ===" ; docker compose ps 2>&1
    ;;
  logs)
    docker compose logs -f
    ;;
  down)
    echo "[hub] 컨테이너 중지 (볼륨 유지)..."
    docker compose down
    ;;
  stop)
    echo "[hub] 컨테이너 + Colima 중지..."
    docker compose down
    colima stop
    ;;
  *)
    echo "사용법: ./hub.sh {up|up-all|status|logs|down|stop}"
    exit 1
    ;;
esac
