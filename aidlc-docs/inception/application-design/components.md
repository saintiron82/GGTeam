# 컴포넌트 정의 (Components)

> AI 기반 CS 문의 처리 에이전트 — Application Design
> 기술 스택: Spring Boot 3.x (JDK 17, 모놀리식 + 계층형), React + TypeScript (Vite SPA), PostgreSQL, AWS Bedrock, Docker Compose

---

## 1. 개요

본 시스템은 **계층형(Layered) 모놀리식 아키텍처**를 따른다. 백엔드는 `Controller → Service → Repository` 계층으로 구성되며, 문의 유형별 처리 로직은 **Strategy Pattern**으로 분리하여 플러그인 방식 확장을 지원한다.

```
[Frontend SPA] ──HTTP/JSON(JWT)──> [Controller Layer]
                                        │
                                   [Service Layer] ── AI 파이프라인 오케스트레이션
                                        │
                                   [Repository Layer] ──> PostgreSQL
                                        │
                                   [BedrockClient] ──────> AWS Bedrock
```

계층 규칙:
- Controller는 Service만 호출한다 (Repository 직접 접근 금지).
- Service는 비즈니스 로직과 오케스트레이션을 담당한다.
- 외부 연동(Bedrock)은 Client 컴포넌트로 추상화하여 교체 가능하게 한다.

---

## 2. 백엔드 컴포넌트

### 2.1 Controller Layer (API 진입점)

#### InquiryController
- **책임**: 고객 문의 접수 및 문의 조회 REST API 엔드포인트 제공.
- **주요 기능**: 문의 접수(POST), 문의 단건/목록 조회, 문의 상세(분석/조회결과/초안 포함) 반환.
- **인증**: 접수 API는 비인증(고객용), 조회 API는 JWT 인증 필요.
- **관련 스토리**: US-03, US-04, US-05, US-23

#### OperatorController
- **책임**: 운영자 워크플로우 관련 API(배정/승인/반려/수정) 엔드포인트 제공.
- **주요 기능**: 미배정 문의 가져오기(Pull), 답변 초안 수정, 승인, 반려 및 재생성, 처리 이력 조회.
- **인증**: 전 엔드포인트 JWT 인증 필요.
- **관련 스토리**: US-14, US-15, US-16, US-17, US-18, US-24

#### DashboardController
- **책임**: 칸반 보드 및 목록/필터/검색 데이터 제공 API.
- **주요 기능**: 상태별 칸반 보드 데이터 집계, 필터/검색 조회, 미배정/긴급 카운트 알림 데이터.
- **인증**: JWT 인증 필요.
- **관련 스토리**: US-12, US-20, US-22

#### AuthController (AuthService 노출)
- **책임**: 운영자 로그인/로그아웃/토큰 검증 API.
- **주요 기능**: 로그인(토큰 발급), 로그아웃(토큰 무효화), 토큰 갱신/검증.
- **관련 스토리**: US-01, US-02

---

### 2.2 Service Layer (비즈니스 로직)

#### InquiryService
- **책임**: 문의 라이프사이클의 핵심 진입 서비스. 문의 생성·저장·조회.
- **주요 기능**: 문의 ID/타임스탬프 부여, 문의 저장, AI 파이프라인 트리거, 문의 상세 조립.
- **협력**: AIAnalysisService(파이프라인 호출), InquiryStateMachine(상태 전이), Repository.

#### AIAnalysisService
- **책임**: AI 자동 분류 단계 수행. 문의 내용을 Bedrock에 전달하여 분류 결과 산출.
- **주요 기능**: customer_type/ai_type 분리, sub_category·긴급도·요약·키워드 추출, 분석 결과 저장.
- **실패 처리**: 타임아웃 → exponential backoff 최대 3회 재시도 / API 에러(4xx,5xx) → 즉시 실패. 최종 실패 시 `수동분류대기` 전이.
- **협력**: BedrockClient, InquiryStateMachine.
- **관련 스토리**: US-06, US-07, US-08

#### BedrockClient
- **책임**: AWS Bedrock 호출 추상화 레이어. 모델 교체 가능(NFR-03).
- **주요 기능**: 프롬프트 전송, 응답 파싱, 타임아웃 설정(120초), 에러 유형 구분(timeout/api_error), 호출 로그 기록.
- **추상화**: `LlmClient` 인터페이스를 구현하여 모델/프로바이더 교체에 대비.

#### SystemDataQueryService (Strategy Context)
- **책임**: 문의 유형별 시스템 데이터 조회의 진입점. 유형에 맞는 Strategy를 선택·실행.
- **주요 기능**: ai_type 기반 `QueryStrategy` 선택, 조회 실행, 결과 표준화(`SystemQueryResult`).
- **확장 구조**: `QueryStrategy` 인터페이스 + 구현체(`PaymentQueryStrategy` MVP, 이후 `AccountQueryStrategy`, `BugQueryStrategy`, `EventQueryStrategy`). 신규 유형은 Strategy 추가만으로 확장.
- **MVP 범위**: 결제(`PaymentQueryStrategy`)만 구현, 나머지는 인터페이스/등록 구조만 마련.
- **관련 스토리**: US-09, US-10

#### DiagnosisService
- **책임**: 조회 데이터 기반 AI 원인 진단.
- **주요 기능**: 조회 결과 + 문의 요약을 Bedrock에 전달, 원인·처리방향·신뢰도 산출. 자동 발송 불가(운영자 검토 전제).
- **협력**: BedrockClient, SystemDataQueryService 결과 입력.
- **관련 스토리**: US-11

#### DraftResponseService
- **책임**: 진단 직후 답변 초안 자동 생성 및 관리.
- **주요 기능**: 진단 결과 기반 고객 친화 답변 생성, 초안 저장, 반려 사유 반영 재생성(재생성 횟수 추적), 수정본 저장(이력 보관).
- **협력**: BedrockClient, InquiryStateMachine.
- **관련 스토리**: US-13, US-14, US-17

#### ApprovalService
- **책임**: 운영자 승인/반려 워크플로우 및 이력 관리.
- **주요 기능**: 미배정 문의 Pull 배정(동시성 처리), 승인 처리(발송 트리거), 반려(사유 → 재생성 위임), 승인/반려/수정/재생성 이력 기록.
- **협력**: DraftResponseService(재생성), NotificationService(발송), InquiryStateMachine.
- **관련 스토리**: US-15, US-16, US-17, US-18

#### InquiryStateMachine
- **책임**: 문의 상태 전이 규칙 강제(비정상 전이 차단).
- **상태**: `접수 → AI분석중 → 담당자배정대기 → 운영자확인중 → 승인완료 → 발송완료` (+ 예외: `수동분류대기`).
- **주요 기능**: 전이 가능 여부 검증, 전이 실행 + 타임스탬프/이벤트 기록.
- **관련 스토리**: US-08, US-19, US-21

#### NotificationService
- **책임**: 답변 발송 및 알림 처리.
- **주요 기능**: 승인된 답변 발송(MVP: 인앱/로그 기반 발송 + 상태 전이), 발송 실패 재시도, 미배정/긴급 알림 데이터 생성.
- **MVP 범위**: 실제 이메일/푸시는 Phase 2. MVP는 발송 완료 처리 및 상태 전이에 집중.
- **관련 스토리**: US-12, US-25

#### AuthService
- **책임**: 운영자 인증/인가.
- **주요 기능**: 자격증명 검증, JWT 발급(유효시간 8시간), 토큰 검증/무효화, 로그인 실패 5회 계정 잠금, 비밀번호 해시 검증.
- **관련 스토리**: US-01, US-02

---

### 2.3 Repository Layer
PostgreSQL 접근 계층(Spring Data JPA). 핵심 엔티티별 Repository:
`InquiryRepository`, `AIAnalysisRepository`, `DraftResponseRepository`, `ApprovalHistoryRepository`, `OperatorRepository`, 그리고 데모 더미 테이블용 `PaymentRepository`, `ItemDeliveryRepository`, `AccountRepository`.

---

## 3. 프론트엔드 컴포넌트 (React + TypeScript, Vite SPA)

| 컴포넌트 | 책임 | 주요 화면/기능 | 관련 스토리 |
|---|---|---|---|
| **고객 문의 폼** (`InquiryFormPage`) | 비로그인 고객 문의 접수 | 유형 드롭다운(결제/계정/버그/이벤트/기타), 내용 입력(최소 10자), 제출 후 문의 ID 표시 | US-03, US-04, US-05 |
| **운영자 로그인** (`LoginPage`) | 운영자 인증 진입 | ID/Password 입력, JWT 저장, 실패 메시지, 토큰 만료 시 리다이렉트 | US-01, US-02 |
| **칸반 보드** (`KanbanBoardPage`) | 상태별 처리 현황 시각화 | 상태별 컬럼(6단계) 카드 배치, 긴급 강조, 필터/검색, 미배정/긴급 알림, 리스트 뷰 토글 | US-12, US-20, US-22 |
| **문의 상세** (`InquiryDetailPage`) | 단건 처리 정보 종합 | 원본 문의·고객유형, AI 분석 결과, 시스템 조회 결과, 답변 초안, 처리 타임라인, 담당하기/승인/반려 버튼 | US-18, US-21, US-23 |
| **답변 편집기** (`DraftEditor`) | 답변 초안 편집·승인 통합 UI | 인라인 텍스트 편집, 원문 대비 변경점 하이라이트, "승인"/"반려 및 재생성(사유 입력)"/"수정 후 승인" | US-14, US-16, US-17, US-24 |

### 공통 프론트엔드 인프라
- **ApiClient**: Axios 기반, JWT 자동 첨부 인터셉터, 401 시 로그인 리다이렉트.
- **AuthContext**: 토큰/사용자 상태 전역 관리.
- **Router**: 보호 라우트(운영자 영역) / 공개 라우트(문의 폼).

---

## 4. 컴포넌트 분류 요약

| 구분 | 컴포넌트 |
|---|---|
| Controller | InquiryController, OperatorController, DashboardController, AuthController |
| Service (핵심) | InquiryService, AIAnalysisService, DiagnosisService, DraftResponseService, ApprovalService |
| Service (지원) | SystemDataQueryService, NotificationService, AuthService, InquiryStateMachine |
| Client/추상화 | BedrockClient (LlmClient 구현), QueryStrategy 구현체 |
| Frontend | 고객 문의 폼, 운영자 로그인, 칸반 보드, 문의 상세, 답변 편집기 |

---

## 5. 확장성 설계 포인트 (NFR-03)

1. **문의 유형 확장**: `QueryStrategy` 인터페이스 + Spring Bean 등록(`Map<InquiryType, QueryStrategy>`). 신규 유형 = Strategy 구현체 추가만.
2. **AI 모델 교체**: `LlmClient` 인터페이스로 추상화. `BedrockClient`는 구현체 중 하나.
3. **상태 흐름 변경**: `InquiryStateMachine`에 전이 규칙 집중 → 흐름 변경 시 단일 지점 수정.
