# GGTeam — AI 기반 CS 문의 처리 에이전트

5명 팀 병렬 개발 프로젝트. 작업 분장과 계약은 `aidlc-docs/construction/work-breakdown/` 참조.

## 프로젝트 구조

```
GGTeam/
├── backend/                 # Spring Boot 3.x (JDK 17)
│   └── src/main/java/com/ggteam/cs/
│       ├── common/          # 공통 응답/에러/enum (공유, 백엔드 A 관리)
│       ├── auth/            # 인증 (백엔드 A)
│       ├── inquiry/         # 문의 코어 (백엔드 A)
│       ├── persistence/     # 엔티티/Repository (백엔드 A)
│       ├── aipipeline/      # AI 분석/진단/초안 (백엔드 B)
│       ├── external/        # LlmClient/BedrockClient (백엔드 B)
│       ├── workflow/        # 배정/승인/상태머신 (백엔드 A·C)
│       ├── dashboard/       # 칸반/조회 (백엔드 C)
│       └── notification/    # 발송/알림 (백엔드 C)
├── frontend/                # React + TypeScript (Vite)
│   └── src/
│       ├── common/          # ApiClient, 공유 타입 (공유)
│       ├── auth/ customer/ kanban/ detail/ editor/
├── docker-compose.yml       # postgres + backend + frontend
└── aidlc-docs/              # 설계·기획 문서
```

## 이미 제공된 공통 기반 (M0)

**백엔드**
- 공유 enum: `common/enums/` (InquiryStatus, InquiryType, Urgency, ApprovalAction, FailureType, DraftResponseStatus, OperatorRole, DemoEnums)
- 공통 응답/에러: `common/` (ApiResponse, ErrorResponse, ErrorCode, BusinessException, GlobalExceptionHandler)
- 공유 인터페이스: LlmClient, QueryStrategy, AIAnalysisService, DraftResponseService, InquiryStateMachine, ApprovalService, NotificationService
- 설정: application.yml, build.gradle.kts

**프론트엔드**
- 공유 타입: `src/common/types.ts` (백엔드 enum과 1:1)
- API 클라이언트: `src/common/apiClient.ts` (JWT 인터셉터)

## 각 담당자 시작 지점

| 역할 | 시작 | 참조 |
|------|------|------|
| 백엔드 A | persistence 엔티티 + 마이그레이션(V1 교체) → auth → inquiry → 상태머신 구현 | 03 §A |
| 백엔드 B | external(BedrockClient/MockLlmClient) → aipipeline 구현 | 03 §B |
| 백엔드 C | workflow(ApprovalService) → dashboard → notification 구현 | 03 §C |
| 프론트 기획 | 와이어프레임/UX | 04 기획 |
| 프론트 개발 | auth → customer → kanban → detail → editor (MSW 병렬) | 04 개발 |

## 빌드 & 실행

```bash
# 백엔드 (Gradle Wrapper는 백엔드 A가 gradle wrapper로 생성 필요)
cd backend && ./gradlew build

# 프론트엔드
cd frontend && npm install && npm run dev

# 전체 (Docker)
docker compose up -d
```

> **참고**: 현재는 공통 스캐폴드 단계입니다. 엔티티/Security 설정/실제 로직은 각 담당자가 구현하면 앱이 완전히 부팅됩니다. Gradle Wrapper(`gradlew`)는 `gradle wrapper` 명령으로 생성하세요.

## 핵심 규칙
- 모든 시각: KST (Asia/Seoul)
- 모든 답변: 운영자 승인 후 발송 (AI 단독 발송 금지)
- 상태 전이: InquiryStateMachine 경유만
- 공유 계약 변경: 팀 협의 필수
