# Unit of Work (작업 단위 정의)

> AI 기반 CS 문의 처리 에이전트 — 시스템 분해

## 분해 전략

소규모 로컬 데모 + 계층형 모놀리식 + SPA 구조이므로, **2개의 작업 단위**로 분해한다. 각 단위는 독립적으로 빌드/배포 가능하며 REST API 계약으로 연결된다.

---

## Unit 1: Backend (Spring Boot API)

- **유형**: 독립 배포 단위 (Spring Boot 애플리케이션)
- **책임**: REST API, AI 파이프라인 오케스트레이션, 상태 머신, 데이터 영속화, 인증
- **포함 논리 모듈**:
  - `auth` — 운영자 인증 (JWT)
  - `inquiry` — 문의 접수/조회/상세
  - `ai-pipeline` — AI 분석, 시스템 조회(Strategy), 진단, 답변 생성
  - `workflow` — 담당자 배정, 승인/반려, 상태 머신, 이력
  - `dashboard` — 칸반 보드/필터/검색 데이터
  - `notification` — 발송, 알림
  - `persistence` — JPA 엔티티, Repository, 데모 더미 데이터
  - `external` — BedrockClient (LlmClient 추상화)
- **기술**: Spring Boot 3.x, JDK 17, Spring Data JPA, Spring Security, AWS SDK (Bedrock), PostgreSQL
- **관련 스토리**: US-01~US-21, US-25 (백엔드 로직 전반)

## Unit 2: Frontend (React SPA)

- **유형**: 독립 배포 단위 (정적 SPA)
- **책임**: 고객 문의 폼, 운영자 대시보드(칸반/상세/편집기), 인증 UI
- **포함 논리 모듈**:
  - `customer` — 고객 문의 폼
  - `auth-ui` — 로그인, 토큰 관리
  - `kanban` — 칸반 보드, 필터/검색
  - `detail` — 문의 상세, 타임라인
  - `editor` — 답변 편집기, 승인/반려 UI
  - `common` — ApiClient, AuthContext, Router
- **기술**: React, TypeScript, Vite, Axios
- **관련 스토리**: US-01~US-05, US-12, US-14~US-24

---

## 코드 조직 전략 (Greenfield)

```
GGTeam/
├── backend/                    # Unit 1: Spring Boot
│   ├── src/main/java/com/ggteam/cs/
│   │   ├── auth/
│   │   ├── inquiry/
│   │   ├── aipipeline/
│   │   ├── workflow/
│   │   ├── dashboard/
│   │   ├── notification/
│   │   ├── persistence/
│   │   └── external/
│   ├── src/main/resources/
│   ├── src/test/java/
│   └── build.gradle
├── frontend/                   # Unit 2: React SPA
│   ├── src/
│   │   ├── customer/
│   │   ├── auth/
│   │   ├── kanban/
│   │   ├── detail/
│   │   ├── editor/
│   │   └── common/
│   ├── package.json
│   └── vite.config.ts
└── docker-compose.yml          # PostgreSQL + backend + frontend
```
