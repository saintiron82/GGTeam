# 인프라 점검 & 운영 스크립트

GGTeam 개발/테스트 인프라를 점검하고 운영하기 위한 스크립트 모음.

## 파일

| 스크립트 | 대상 | 용도 |
|----------|------|------|
| `health-check.sh` | macOS / Linux (팀 전원) | 허브 인프라 접속 점검 (DB/백엔드/프론트) |
| `health-check.ps1` | Windows (팀 전원) | 동일 (PowerShell) |
| `hub.sh` | macOS (인프라 담당자) | Colima + 공유 DB 기동/중지/상태 |

---

## 1. 인프라 담당자 (허브 머신) — 무엇을 켜두는가

**최소 상시 구동: Colima + PostgreSQL 컨테이너**

```bash
cd <프로젝트 루트>
./scripts/hub.sh up        # Colima + postgres 기동 (상시)
./scripts/hub.sh status    # 상태 확인
```

통합 테스트 시점에만 전체 기동:
```bash
./scripts/hub.sh up-all    # postgres + backend + frontend
```

| 명령 | 동작 |
|------|------|
| `./scripts/hub.sh up` | Colima + 공유 PostgreSQL (상시 권장) |
| `./scripts/hub.sh up-all` | 전체 스택 |
| `./scripts/hub.sh status` | Colima/컨테이너 상태 |
| `./scripts/hub.sh logs` | 로그 follow |
| `./scripts/hub.sh down` | 컨테이너 중지(볼륨 유지) |
| `./scripts/hub.sh stop` | 컨테이너 + Colima 중지 |

---

## 2. 팀원 — 인프라 접속 점검

허브가 살아있는지(특히 공유 DB) 확인하는 용도.

### macOS / Linux
```bash
chmod +x scripts/health-check.sh      # 최초 1회
./scripts/health-check.sh             # 기본 허브(172.24.121.128)
./scripts/health-check.sh 192.168.0.10  # 다른 허브 IP
```

### Windows (PowerShell)
```powershell
.\scripts\health-check.ps1
.\scripts\health-check.ps1 -HubHost 192.168.0.10
# 실행정책 막히면:
powershell -ExecutionPolicy Bypass -File .\scripts\health-check.ps1
```

### 점검 항목 & 결과 해석
| 표시 | 의미 |
|------|------|
| `[OK]` | 정상 |
| `[FAIL]` | DB 포트 도달 불가 또는 인증 실패 → 허브 담당자에게 확인 요청 |
| `[--]` | 해당 서비스 미기동(정상일 수 있음) 또는 도구 미설치 |

점검 대상: PostgreSQL(5432), 백엔드 API(8080 `/actuator/health`), 프론트(80)

---

## 3. 허브 접속 정보 (기본값)

| 항목 | 값 |
|------|-----|
| 허브 IP | `172.24.121.128` (변경 시 `ipconfig getifaddr en0`로 재확인) |
| DB | `172.24.121.128:5432` / `csagent` / `csagent` |
| 백엔드 | `http://172.24.121.128:8080` |
| 프론트 | `http://172.24.121.128:80` |

> ⚠️ 같은 네트워크(LAN)에 있어야 접속 가능. IP가 바뀌면 스크립트 인자로 새 IP를 넘기세요.  
> 상세 환경 정의: `aidlc-docs/construction/work-breakdown/07-local-dev-environment.md`
