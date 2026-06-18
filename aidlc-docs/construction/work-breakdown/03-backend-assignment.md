# 작업 분장: 백엔드 3명

> **원칙**: 모듈 경계를 명확히 하여 충돌 없이 병렬 개발. 공유 지점은 §4 인터페이스 계약으로 사전 합의.  
> 패키지 루트: `com.ggteam.cs`

---

## 백엔드 A — 인증 & 문의 코어 (Foundation)

> 다른 두 명이 의존하는 **기반(엔티티/Repository/인증)** 을 담당. 가장 먼저 시작·공유.

### 담당 모듈
- `common` — 공통 응답/에러 핸들러, BaseEntity, KST 시각 유틸
- `persistence` (엔티티/Repo 전체 소유) — 전 엔티티 JPA 매핑, Repository 인터페이스
- `auth` — AuthService, JWT 발급/검증, Spring Security 설정, BCrypt, 계정 잠금
- `inquiry` — InquiryController, InquiryService (접수/조회/상세 조립)
- `workflow/state` — InquiryStateMachine (상태 전이 검증)

### 담당 스토리
US-01, US-02 (인증), US-03, US-04, US-05 (접수), US-19 (상태전이), US-23 (상세 조회 조립)

### 산출물
- 전 엔티티/Enum/Repository (다른 팀원이 import)
- 인증 API, 문의 접수/조회 API
- 상태머신 + 단위 테스트
- Flyway V1 스키마 마이그레이션

---

## 백엔드 B — AI 파이프라인 (Intelligence)

> 외부 AI 연동과 분석/진단/초안 생성. 가장 독립적, MockLlmClient로 단독 개발 가능.

### 담당 모듈
- `external` — LlmClient 인터페이스, BedrockClient(타임아웃 점증/재시도/실패구분), MockLlmClient
- `aipipeline` — AIAnalysisService(분류/요약/품질검증), DiagnosisService, DraftResponseService(생성/재생성)
- `aipipeline/query` — QueryStrategy 인터페이스, PaymentQueryStrategy, SystemDataQueryService
- 더미 데이터 조회 로직 (Payment/ItemDelivery/Account Repository 사용)

### 담당 스토리
US-06, US-07, US-08 (분석/실패처리), US-09 (결제조회), US-11 (진단), US-13 (초안생성), US-14(초안수정 서비스), US-17(재생성 로직), US-10/US-26 (확장 구조)

### 산출물
- LlmClient 추상화 + BedrockClient + MockLlmClient
- 분석/진단/초안 서비스 + 비동기 파이프라인 오케스트레이션
- QueryStrategy 확장 구조 (Map 등록)
- 단위 테스트 (MockLlmClient 기반, Bedrock 없이 검증)
- Flyway V2 더미 데이터 시딩

### 의존
- 백엔드 A의 엔티티/Repository (AIAnalysis, Diagnosis, DraftResponse, Payment 등)
- → §4 인터페이스 합의 후 Mock 엔티티로 선행 개발 가능

---

## 백엔드 C — 워크플로우 & 대시보드 (Operations)

> 운영자 업무 처리와 조회/집계. 프론트와 가장 많이 맞닿음.

### 담당 모듈
- `workflow` — ApprovalService(Pull 배정 동시성, 승인/반려/수정/재분석, 이력 기록)
- `notification` — NotificationService(발송, 재시도, 알림 데이터)
- `dashboard` — DashboardController, DashboardService(칸반/필터/검색/알림 집계)
- `workflow/api` — OperatorController (배정/수정/승인/반려/재분석/이력 API)

### 담당 스토리
US-12 (알림), US-15 (Pull 배정), US-16 (승인), US-17(반려 API), US-18 (이력), US-20 (칸반 데이터), US-21 (타임라인), US-22 (필터/검색), US-24 (편집/승인 통합 API), US-25 (발송), US-27 (통계 확장)

### 산출물
- 운영자 워크플로우 API 전체
- 대시보드/칸반/필터 조회 API
- Pull 배정 동시성 처리 + 단위 테스트
- API 통합 테스트 (MockMvc)

### 의존
- 백엔드 A: 엔티티/Repository, 상태머신, 인증
- 백엔드 B: DraftResponseService(재생성 호출), 재분석 트리거
- → §4 인터페이스 합의 후 stub으로 선행 개발

---

## 4. 공유 인터페이스 계약 (백엔드 내부, 사전 합의 필수)

병렬 개발을 위해 아래 Java 인터페이스를 **백엔드 A가 가장 먼저 정의·커밋**한다. B/C는 이 인터페이스에 의존(구현/호출).

```java
// 백엔드 A 소유 (가장 먼저 확정)
interface InquiryStateMachine {
    void transition(UUID inquiryId, InquiryStatus to, ApprovalAction action, UUID operatorId);
    boolean canTransition(InquiryStatus from, InquiryStatus to);
}

// 백엔드 B 소유
interface LlmClient {
    LlmResponse complete(LlmRequest request); // 타임아웃/재시도 내부 처리
}
interface AIAnalysisService { void analyze(UUID inquiryId); } // 비동기
interface DraftResponseService {
    DraftResponse generate(UUID inquiryId);
    DraftResponse regenerate(UUID inquiryId, String rejectReason);
}
interface QueryStrategy { SystemQueryResult query(InquiryType type, String userId); }

// 백엔드 C 소유
interface ApprovalService {
    InquiryDetail pullAssign(UUID operatorId);
    void approve(UUID inquiryId, UUID operatorId, String editedContent);
    DraftResponse reject(UUID inquiryId, UUID operatorId, String reason);
}
interface NotificationService { void send(UUID inquiryId); }
```

> **규칙**: 인터페이스 시그니처 변경은 PR + 담당자 합의. 구현 세부는 각자 자유.

## 5. 코드 충돌 방지
- 각자 자기 패키지 디렉토리에서만 작업- 패키지 명까지 사전 정의
- `persistence`(엔티티)는 백엔드 A 단독 수정, B/C는 변경 요청
- 공유 enum/DTO는 `common`에 두고 A가 관리
- Feature 브랜치 전략: `feat/be-a-auth`, `feat/be-b-pipeline`, `feat/be-c-workflow`
