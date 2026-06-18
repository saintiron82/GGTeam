# 로컬 개발 / 테스트 허브 환경 정의

> **목적**: 이 로컬 머신(macOS)이 전체 서비스의 **개발·통합 테스트 중추**가 된다.  
> Docker 스택 기동, 서비스 출력(로그), 통합 검증을 이 머신이 책임진다.  
> 모든 팀원의 통합 기준 환경이므로 버전을 통일한다.

---

## 1. 필수 도구 및 버전

| 도구 | 버전 | 용도 | 설치 명령 (macOS/brew) |
|------|------|------|------------------------|
| JDK | **17** (Temurin) | 백엔드 빌드/실행 | `brew install --cask temurin@17` |
| Docker Desktop | 최신 | postgres + 통합 스택 | `brew install --cask docker` |
| Node.js | 20+ (현재 v24) | 프론트엔드 | (설치됨) |
| Gradle | Wrapper 사용 | 백엔드 빌드 | `gradle wrapper` 로 gradlew 생성 |
| AWS CLI | v2 (선택) | Bedrock 자격증명/디버깅 | `brew install awscli` |

> JDK는 빌드 도구(Gradle) 요구에 맞춰 **17 고정**. 상위 버전 혼용 금지.

---

## 2. 환경 변수 / 시크릿 (.env)

루트에 `.env` 생성 (git 제외됨). docker-compose가 참조.

```bash
# DB
DB_PASSWORD=csagent

# AWS Bedrock (백엔드 B 필수)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=<발급키>
AWS_SECRET_ACCESS_KEY=<시크릿>
BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0

# 보안
JWT_SECRET=<로컬용 임의 문자열>

# LLM 클라이언트 선택: bedrock | mock
LLM_CLIENT=mock   # 초기 개발은 mock, Bedrock 검증 시 bedrock
```

> ⚠️ `.env`는 절대 커밋 금지 (.gitignore 처리됨). 자격증명은 각자 로컬에만 보관.

---

## 3. 실행 방식 (이 머신이 책임지는 출력)

### 3.1 개별 개발 모드 (각자 로컬)
```bash
# 백엔드 (출력: 콘솔 로그)
cd backend && ./gradlew bootRun

# 프론트엔드 (출력: vite dev server)
cd frontend && npm install && npm run dev
```

### 3.2 통합 모드 (이 허브 머신, Docker)
```bash
# 전체 스택 기동 (postgres + backend + frontend)
docker compose up -d --build

# 서비스 출력/로그 책임
docker compose logs -f backend     # 백엔드 로그
docker compose logs -f             # 전체 로그
docker compose ps                  # 상태 확인

# 종료
docker compose down                # 컨테이너 종료
docker compose down -v             # 볼륨 포함 초기화
```

### 3.3 접속 지점
- 프론트엔드: http://localhost (nginx :80)
- 백엔드 API: http://localhost:8080/api/v1
- 헬스체크: http://localhost:8080/actuator/health
- PostgreSQL: localhost:5432 (내부, 디버깅 시 직접 접속)

---

## 4. 로그 / 출력 책임 (허브 역할)

| 대상 | 출력 위치 | 확인 방법 |
|------|-----------|-----------|
| 백엔드 앱 로그 | stdout (Docker 로그) | `docker compose logs -f backend` |
| AI 호출 로그 | 구조화 로그 (요청/응답/소요/실패유형) | 동상 |
| DB | postgres 컨테이너 | `docker compose logs postgres` |
| 프론트 빌드 | nginx 컨테이너 | `docker compose logs frontend` |
| 통합 테스트 결과 | Gradle/Vitest 출력 | 각 빌드 콘솔 |

> 통합 이슈 발생 시 이 허브 머신의 `docker compose logs`가 1차 진단 소스.

---

## 5. 현재 머신 갭 (설치 필요)

| 항목 | 현재 | 조치 |
|------|------|------|
| JDK 17 | ❌ 미설치 | `brew install --cask temurin@17` |
| Docker Desktop | ❌ 미설치 | `brew install --cask docker` + 앱 실행 |
| Gradle Wrapper | ❌ 없음 | JDK 설치 후 `gradle wrapper` 또는 IDE 생성 |
| AWS 자격증명 | ❌ 없음 | `.env`에 설정 (Bedrock 검증 시) |
| Node | ✅ v24 | — |

---

## 6. 백엔드 B 착수 전 체크리스트
- [x] JDK 17 설치 및 `java -version` 확인 (Temurin 17.0.19)
- [x] `gradle wrapper`로 gradlew 생성, `./gradlew compileJava` 통과
- [x] Docker 런타임(Colima) 설치 및 데몬 실행
- [x] `docker compose up postgres` 로 공유 DB 기동 (healthy)
- [ ] `.env`에 LLM_CLIENT=mock 설정 (Bedrock 없이 우선 개발)
- [ ] (선택) AWS 자격증명 설정 후 LLM_CLIENT=bedrock 검증

---

## 7. 이 머신의 실제 셋업 (검증 완료 2026-06-18)

| 항목 | 설치/상태 | 비고 |
|------|-----------|------|
| JDK | Temurin 17.0.19 (arm64) | `/Library/Java/JavaVirtualMachines/temurin-17.jdk` |
| Docker 런타임 | **Colima** (Docker 29.5.3) | GUI 불필요, CLI 관리 |
| docker compose | v5.1.4 | 플러그인 경로 설정됨 |
| Gradle | 9.5.1 (wrapper 8.10) | `backend/gradlew` 생성됨 |
| Node | v24.13.0 | — |
| PostgreSQL | 16.14 (컨테이너, healthy) | KST 타임존 |

### JAVA_HOME 설정 (셸 프로파일에 추가 권장)
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

### Colima 관리
```bash
colima start          # 시작
colima status         # 상태
colima stop           # 중지
brew services start colima   # 로그인 시 자동 시작 등록(선택)
```

---

## 8. 공유 테스트 DB 접속 정보 (팀원용)

이 허브 머신에서 PostgreSQL이 LAN에 노출되어 있다. 팀원은 아래로 접속하여 테스트한다.

| 항목 | 값 |
|------|-----|
| Host | `172.24.121.128` (허브 머신 LAN IP) |
| Port | `5432` |
| Database | `csagent` |
| User | `csagent` |
| Password | `csagent` (로컬 기본값, `.env`의 DB_PASSWORD로 변경 가능) |
| JDBC URL | `jdbc:postgresql://172.24.121.128:5432/csagent` |

> ⚠️ LAN IP는 네트워크 환경에 따라 변할 수 있다. 변경 시 `ipconfig getifaddr en0`로 재확인 후 공유.  
> ⚠️ 같은 Wi-Fi/네트워크에 있어야 접속 가능. 방화벽에서 5432 허용 필요할 수 있음.

### 팀원 로컬에서 자기 백엔드를 공유 DB에 연결
```bash
export DB_URL=jdbc:postgresql://172.24.121.128:5432/csagent
export DB_USERNAME=csagent
export DB_PASSWORD=csagent
cd backend && ./gradlew bootRun
```

### 허브 운영 명령
```bash
docker compose up -d postgres     # 공유 DB 기동
docker compose logs -f postgres   # DB 로그
docker compose ps                 # 상태
docker compose down               # 중지 (볼륨 유지)
```
