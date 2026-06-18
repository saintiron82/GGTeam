# Deployment Architecture — Backend Unit

> 로컬 데모 수준 배포. 프로덕션 HA(고가용성)는 미적용 — 향후 확장 대상.

## 1. 배포 다이어그램

```
        [운영자/고객 브라우저]
                │ HTTP
                ▼
   ┌────────────────────────┐
   │  frontend (nginx :80)   │  React SPA 정적 서빙
   └────────────┬───────────┘
                │ /api → 프록시
                ▼
   ┌────────────────────────┐         ┌──────────────────┐
   │ backend (Spring :8080)  │────────▶│  AWS Bedrock     │ (외부, HTTPS)
   └────────────┬───────────┘         └──────────────────┘
                │ JDBC
                ▼
   ┌────────────────────────┐
   │ postgres (:5432, 내부)  │  pgdata 볼륨
   └────────────────────────┘

   [Docker Compose 네트워크: cs-agent-net]
```

## 2. docker-compose.yml 구조 (개요)

```yaml
services:
  postgres:
    image: postgres:16
    environment: [POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD]
    volumes: [pgdata:/var/lib/postgresql/data]
    networks: [cs-agent-net]

  backend:
    build: ./backend
    environment: [DB_URL, AWS_*, BEDROCK_MODEL_ID, JWT_SECRET, TZ=Asia/Seoul]
    ports: ["8080:8080"]
    depends_on: [postgres]
    networks: [cs-agent-net]
    healthcheck: curl /actuator/health

  frontend:
    build: ./frontend
    ports: ["80:80"]
    depends_on: [backend]
    networks: [cs-agent-net]

volumes: [pgdata]
networks: [cs-agent-net]
```

## 3. 빌드/실행 절차 (개요)

1. `backend`: Gradle 빌드 → JAR → Docker 이미지
2. `frontend`: `npm run build` → 정적 파일 → nginx 이미지
3. `.env` 파일에 자격증명/시크릿 설정
4. `docker compose up -d` 로 전체 기동
5. Flyway가 스키마 생성 + 더미 데이터 시딩
6. 헬스체크 통과 후 서비스 가용

## 4. 배포 특성

| 항목 | 결정 |
|------|------|
| 환경 | 단일 로컬 서버 (소규모 네트워크) |
| 확장성 | 단일 인스턴스 (수평 확장 미적용) |
| 고가용성 | 미적용 (Resiliency Skip) |
| 백업 | postgres 볼륨 수동 백업 (데모 수준) |
| 외부 의존 | AWS Bedrock (인터넷 연결 필요) |

> 프로덕션 전환 시: 컨테이너 오케스트레이션(ECS/K8s), DB 매니지드 서비스(RDS), 로드밸런서, 시크릿 매니저, HA 구성 등 재설계 필요.
