# Infrastructure Design — Backend Unit

> 배포 환경: 소규모 로컬 네트워크 서버, Docker Compose 기반. 고가용성(HA) 미적용. AWS Bedrock만 외부 클라우드 연동.

## 1. 컨테이너 구성 (Docker Compose)

| 서비스 | 이미지/베이스 | 역할 | 포트 |
|--------|---------------|------|------|
| `backend` | eclipse-temurin:17 (Spring Boot) | REST API, AI 파이프라인 | 8080 (외부 노출) |
| `postgres` | postgres:16 | 데이터 영속화 | 5432 (내부만) |
| `frontend` | nginx (React 정적 빌드 서빙) | SPA 제공 | 80 (외부 노출) |

> AWS Bedrock은 컨테이너가 아닌 **외부 클라우드 서비스**로, backend가 AWS SDK로 호출. 자격증명은 환경변수/크리덴셜로 주입.

## 2. 네트워크

- Docker 내부 브리지 네트워크 (`cs-agent-net`)
- 외부 노출: `frontend`(80), `backend`(8080)
- `postgres`는 내부 네트워크에서만 접근 (외부 미노출)
- backend → Bedrock: 아웃바운드 HTTPS (인터넷 접근 필요)

## 3. 볼륨

| 볼륨 | 용도 |
|------|------|
| `pgdata` | PostgreSQL 데이터 영속화 |
| `app-logs` | 애플리케이션/AI 호출 로그 (선택) |

## 4. 환경변수 (주요)

| 변수 | 설명 |
|------|------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL 접속 정보 |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` | Bedrock 자격증명 |
| `BEDROCK_MODEL_ID` | 사용할 모델 ID (교체 가능) |
| `JWT_SECRET` | JWT 서명 키 |
| `TZ=Asia/Seoul` | 컨테이너 타임존 KST 통일 |

> 보안: 자격증명/시크릿은 `.env` 파일 또는 Docker secrets로 관리, 코드/이미지에 하드코딩 금지.

## 5. 모니터링/관측성

- **헬스체크**: Spring Boot Actuator `/actuator/health` (Docker healthcheck 연동)
- **로깅**: Logback 구조화 로그 → stdout (Docker 로그 수집) + 파일
- **AI 호출 로그**: 요청/응답/소요시간/실패유형 별도 기록

## 6. 데이터 초기화
- Flyway 마이그레이션으로 스키마 생성
- 데모 더미 데이터(Payment/ItemDelivery/Account) 시딩 스크립트 포함
