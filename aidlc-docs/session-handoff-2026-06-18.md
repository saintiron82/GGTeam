# 세션 핸드오프 문서
> **날짜**: 2026-06-18 (목)  
> **프로젝트**: AI 기반 CS 문의 자동 분류 및 처리 에이전트 (GGTeam)  
> **방법론**: AI-DLC (AI-Driven Development Life Cycle)

---

## 1. 프로젝트 개요

**목표**: 게임 CS 문의를 AI가 자동 분류/분석하고, 초안 답변을 생성하며, 운영자 승인 후 발송하는 웹 대시보드 시스템 개발

**기술 스택** (확정):
| 영역 | 스택 |
|------|------|
| Backend | Spring Boot 3.x, JDK 17, 모놀리식 계층형 |
| Frontend | React + TypeScript, Vite SPA |
| Database | PostgreSQL |
| AI | AWS Bedrock (최신 최고성능 모델 선택 가능) |
| 배포 | Docker Compose (로컬 서버) |

**팀 구성** (5명):
- 프론트 기획자: 1명
- 프론트 개발자: 1명  
- 백엔드 개발자: 3명 (A/B/C)

---

## 2. AI-DLC 진행 상태

```
[x] INCEPTION
    [x] Workspace Detection
    [x] Requirements Analysis
    [x] User Stories (27개 스토리 확정)
    [x] Workflow Planning
    [x] Application Design
    [x] Units Generation (Backend + Frontend 2개 유닛)

[x] CONSTRUCTION
    [x] Functional Design (도메인/비즈니스 규칙 42개)
    [x] NFR Requirements (성능/가용성/보안 요구사항)
    [x] NFR Design (패턴 설계)
    [x] Infrastructure Design (Docker Compose 3서비스)
    [x] Work Breakdown (팀 작업 분장 - Code Generation 대체)
    [~] Frontend UX Design (진행 중)
    [ ] Build and Test (팀 구현 후)
```

---

## 3. 완료된 산출물

### 3.1 설계 문서 (aidlc-docs/)
```
aidlc-docs/
├── inception/
│   ├── requirements/requirements.md          # 기능/비기능 요구사항
│   ├── user-stories/stories.md               # US-01~US-27
│   ├── application-design/                   # 컴포넌트/서비스/의존성
│   └── plans/                                # 실행 계획
├── construction/
│   ├── backend/
│   │   ├── functional-design/                # 도메인 엔티티, 비즈니스 규칙
│   │   ├── nfr-requirements/                 # NFR 요구사항, 기술 스택 결정
│   │   ├── nfr-design/                       # 논리 컴포넌트, 설계 패턴
│   │   └── infrastructure-design/            # 배포 아키텍처
│   ├── frontend/
│   │   └── ux-design/                        # 정보구조, 와이어프레임, UX 흐름
│   ├── work-breakdown/                       # 팀 작업 분장 문서 6개
│   └── plans/
```

### 3.2 공통 스캐폴드 코드 (구현 완료)
```
backend/
├── src/main/java/com/ggteam/cs/
│   ├── common/enums/         # 8개 enum (InquiryStatus, Urgency 등)
│   ├── common/               # ApiResponse, ErrorCode, GlobalExceptionHandler
│   ├── aipipeline/           # AIAnalysisService, DraftResponseService 인터페이스
│   ├── workflow/             # InquiryStateMachine, ApprovalService 인터페이스
│   ├── external/             # LlmClient 인터페이스
│   └── notification/         # NotificationService 인터페이스
├── build.gradle.kts          # Spring Boot 3.3.0, PostgreSQL, Flyway
├── application.yml           # KST 타임존, DB 설정
└── Dockerfile

frontend/                     # (개발자용 - 포트 5173)
├── src/types.ts              # 백엔드 enum 1:1 매칭 타입
├── src/apiClient.ts          # JWT 인터셉터 포함 API 클라이언트
└── Dockerfile

wireframe/                    # (기획자용 - 포트 5174, 충돌 방지 분리)
├── src/wireframe-kit.tsx     # 공통 UI 컴포넌트
├── src/screens/              # 6개 화면 와이어프레임
│   ├── LoginScreen.tsx       # S1 로그인
│   ├── InquiryFormScreen.tsx # S2/S3 문의 접수
│   ├── KanbanScreen.tsx      # S4 칸반 보드
│   ├── ListScreen.tsx        # S7 리스트 뷰 (신규 추가)
│   ├── DetailScreen.tsx      # S5 문의 상세
│   └── EditorScreen.tsx      # S6 답변 편집기
└── main.tsx                  # 라우팅

docker-compose.yml            # postgres + backend + frontend
```

---

## 4. 현재 진행 중인 작업

### 4.1 Frontend UX Design (기획자 담당)

**상태**: 02-wireframes.md 작업 중 (S7 리스트 뷰 추가 완료)

**완료된 UX 산출물**:
| 문서 | 상태 | 내용 |
|------|------|------|
| 01-information-architecture.md | ✅ 확정 | 7개 화면, 네비게이션 흐름 |
| 02-wireframes.md | 🔄 진행중 | S1~S7 와이어프레임 명세 |
| 03-ux-flows.md | 📋 대기 | 주요 사용자 흐름 |
| 04-screen-api-mapping.md | 📋 대기 | 화면-API 매핑 |
| 05-display-rules.md | 📋 대기 | 표시 규칙/조건부 렌더링 |

**와이어프레임 화면 (wireframe/)**:
| 화면 | 파일 | 상태 |
|------|------|------|
| S1 로그인 | LoginScreen.tsx | ✅ 완료 |
| S2/S3 문의 접수 | InquiryFormScreen.tsx | ✅ 완료 |
| S4 칸반 보드 | KanbanScreen.tsx | ✅ 완료 (토글 추가) |
| S5 문의 상세 | DetailScreen.tsx | ✅ 완료 |
| S6 답변 편집기 | EditorScreen.tsx | ✅ 완료 |
| S7 리스트 뷰 | ListScreen.tsx | ✅ 완료 (신규) |

### 4.2 백엔드 개발 (3명 병렬)

**작업 분장** (03-backend-assignment.md 참조):
| 담당자 | 모듈 | 상태 |
|--------|------|------|
| 백엔드 A | 인프라/DB/공통, 문의 접수 | 대기 (M1 착수 가능) |
| 백엔드 B | AI 파이프라인 (Bedrock 연동) | 대기 (M1 착수 가능) |
| 백엔드 C | 운영자 워크플로우, 알림 | 대기 (M1 착수 가능) |

---

## 5. 다음 세션에서 처리할 작업

### 5.1 기획자 (Frontend UX)
1. **02-wireframes.md 마무리** - 각 화면별 상세 UI 요소 명세 검토/확정
2. **03-ux-flows.md 작성** - 주요 사용자 시나리오 흐름도
3. **04-screen-api-mapping.md 작성** - 화면별 호출 API 매핑
4. **05-display-rules.md 작성** - 조건부 렌더링, 상태별 UI 변화 규칙

### 5.2 개발팀 (병렬)
1. **M1 마일스톤 착수** (05-parallel-work-plan.md 참조)
   - 백엔드 A: 도메인 엔티티 + Flyway 마이그레이션 + JPA Repository
   - 백엔드 B: LlmClient 구현체 (Bedrock) + 프롬프트 템플릿
   - 백엔드 C: InquiryStateMachine 구현 + 상태 전이 로직
   - 프론트 개발: 와이어프레임 → 실제 컴포넌트 변환 착수

### 5.3 QA (기획자 주도)
1. **06-test-case-guide.md** 기반 테스트 케이스 공동 도출
2. 케이스 정의 공통 포맷 활용 (케이스ID/시나리오/입력/기대결과/담당파트/근거)

---

## 6. 주요 결정 사항 및 제약

### 확정된 결정
- **AI 단독 발송 불가**: 모든 답변은 운영자 승인 필수 (FR-06)
- **담당자 배정 시점**: AI 분석 이후 Pull 방식 (운영자가 스스로 배정)
- **타임존**: 모든 시간은 KST (Asia/Seoul)
- **AI 분석 타임아웃**: 점증식 120s → 180s → 240s (3회 재시도)
- **답변 재생성**: 반려 사유를 AI에 전달하여 개선된 답변 생성

### 주의사항
- **wireframe/** 폴더는 기획자 전용 (포트 5174)
- **frontend/** 폴더는 개발자 전용 (포트 5173)
- 두 폴더 간 충돌 방지를 위해 분리 운영

---

## 7. 참조 문서

| 문서 | 경로 | 용도 |
|------|------|------|
| 상태 추적 | aidlc-docs/aidlc-state.md | 현재 단계 확인 |
| 감사 로그 | aidlc-docs/audit.md | 의사결정 이력 |
| 요구사항 | aidlc-docs/inception/requirements/requirements.md | FR/NFR 참조 |
| 스토리 | aidlc-docs/inception/user-stories/stories.md | US-01~US-27 |
| API 계약 | aidlc-docs/construction/work-breakdown/01-api-contract.md | 백/프론트 인터페이스 |
| 작업 분장 | aidlc-docs/construction/work-breakdown/03-backend-assignment.md | 백엔드 담당자별 |
| 병렬 계획 | aidlc-docs/construction/work-breakdown/05-parallel-work-plan.md | M0~M4 마일스톤 |

---

## 8. 와이어프레임 실행 방법

```bash
# 기획자 와이어프레임 (포트 5174)
cd wireframe
npm install
npm run dev

# 개발자 프론트엔드 (포트 5173)
cd frontend
npm install
npm run dev
```

---

*문서 작성: Kiro AI*  
*다음 세션에서 이 문서를 참조하여 작업을 이어가세요.*
