# Domain Entities — Backend Unit

AI 기반 CS 문의 처리 에이전트 Backend Unit의 핵심 도메인 엔티티, 속성, 관계, 타입 정의 문서입니다. 기술 중립적으로 기술하되 데이터 모델은 구체적으로 명시합니다.

> 타입 표기: `UUID`, `String`, `Text`(장문 문자열), `Integer`, `Decimal`, `Boolean`, `Timestamp`(KST), `Enum`, `JSON`(구조화 데이터)는 논리 타입이며 특정 DB/언어에 종속되지 않습니다.

---

## 1. Enum 정의

### 1.1 InquiryStatus (문의 상태)
| 값 | 한글 | 설명 |
|----|------|------|
| `RECEIVED` | 접수 | 문의가 시스템에 등록된 초기 상태 |
| `AI_ANALYZING` | AI분석중 | AI 파이프라인이 분석을 진행 중 |
| `PENDING_ASSIGNMENT` | 담당자배정대기 | AI 분석 완료, 운영자 Pull 배정 대기 |
| `OPERATOR_REVIEWING` | 운영자확인중 | 운영자가 배정받아 검토/수정 중 |
| `APPROVED` | 승인완료 | 운영자가 답변을 승인함 |
| `SENT` | 발송완료 | 고객에게 답변이 전송됨 (종료 상태) |
| `MANUAL_CLASSIFICATION_PENDING` | 수동분류대기 | AI 분석 실패 등 예외, 수동 분류 필요 |

### 1.2 InquiryType (문의 유형)
| 값 | 한글 | MVP 범위 |
|----|------|----------|
| `PAYMENT` | 결제 | ✅ End-to-end 지원 (MVP) |
| `ITEM_DELIVERY` | 아이템지급 | 확장 예정 |
| `ACCOUNT` | 계정 | 확장 예정 |
| `ETC` | 기타 | 확장 예정 |

### 1.3 Urgency (긴급도)
| 값 | 한글 |
|----|------|
| `HIGH` | 긴급 |
| `NORMAL` | 보통 |
| `LOW` | 낮음 |

### 1.4 ApprovalAction (승인 이력 액션)
| 값 | 한글 |
|----|------|
| `APPROVE` | 승인 |
| `REJECT` | 반려 |
| `EDIT` | 수정 |
| `REGENERATE` | 재생성 |
| `ASSIGN` | 배정 |
| `REANALYZE` | 재분석 (운영자 수동 요청) |

### 1.5 FailureType (AI 분석 실패 유형)
| 값 | 설명 |
|----|------|
| `TIMEOUT` | LLM/조회 응답 타임아웃 (재시도 3회 소진 후 확정) |
| `API_ERROR` | LLM/외부 API 오류 (즉시 실패) |
| `null` | 정상 (실패 없음) |

### 1.6 DraftResponseStatus (초안 상태)
| 값 | 한글 | 설명 |
|----|------|------|
| `GENERATED` | 생성됨 | AI가 초안 생성 완료 |
| `EDITED` | 수정됨 | 운영자가 내용 수정 |
| `REJECTED` | 반려됨 | 운영자가 반려, 재생성 대상 |
| `APPROVED` | 승인됨 | 발송 대상으로 확정 |

### 1.7 OperatorRole (운영자 역할)
| 값 | 한글 |
|----|------|
| `OPERATOR` | 일반 운영자 |
| `ADMIN` | 관리자 |

### 1.8 AccountStatus / PaymentStatus / DeliveryStatus (데모 더미 테이블용)
- `PaymentStatus`: `SUCCESS`, `FAILED`, `PENDING`, `REFUNDED`
- `DeliveryStatus`: `DELIVERED`, `NOT_DELIVERED`, `PARTIAL`, `PENDING`
- `AccountStatus`: `ACTIVE`, `SUSPENDED`, `DORMANT`, `BANNED`

---

## 2. 핵심 엔티티

### 2.1 Inquiry (문의)
고객 문의의 루트 엔티티. 전체 처리 흐름의 중심.

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 문의 고유 식별자 |
| `customerInfo` | JSON | 필수 | 고객 식별 정보 (userId, 닉네임, 연락 채널 등) |
| `customerType` | Enum(InquiryType) | 필수 | 문의 유형. MVP에서는 `PAYMENT`만 E2E |
| `content` | Text | 필수, 최소 10자 | 고객이 작성한 문의 본문 |
| `status` | Enum(InquiryStatus) | 필수, 기본값 `RECEIVED` | 현재 상태 |
| `createdAt` | Timestamp | 필수, 자동 생성 | 접수 시각 (KST) |
| `assignedOperatorId` | UUID | nullable, FK→Operator.id | 배정된 운영자. 미배정 시 null |

관계:
- Inquiry `1 : 0..1` AIAnalysis
- Inquiry `1 : 0..1` Diagnosis
- Inquiry `1 : 0..N` DraftResponse (재생성으로 여러 버전 발생 가능)
- Inquiry `1 : 0..N` ApprovalHistory
- Inquiry `N : 0..1` Operator (assignedOperatorId 경유)

### 2.2 AIAnalysis (AI 분석 결과)
AI 파이프라인의 분류·요약·시스템 조회 통합 결과.

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 분석 결과 식별자 |
| `inquiryId` | UUID | FK→Inquiry.id, 필수, UNIQUE | 대상 문의 (1:1) |
| `aiType` | Enum(InquiryType) | 필수 | AI가 분류한 문의 유형 |
| `subCategory` | String | nullable | 세부 분류 (예: 결제실패, 중복결제, 환불요청) |
| `urgency` | Enum(Urgency) | 필수 | 긴급/보통/낮음 |
| `summary` | Text | nullable | 문의 요약문 |
| `keywords` | JSON(String[]) | nullable | 추출 키워드 배열 |
| `systemQueryResult` | JSON | nullable | SystemDataQueryService 조회 결과 스냅샷 |
| `analyzedAt` | Timestamp | nullable | 분석 완료 시각. 실패 시 시도 종료 시각 |
| `failureType` | Enum(FailureType) | nullable | 실패 유형. 정상 시 null |

관계:
- AIAnalysis `0..1 : 1` Inquiry (inquiryId UNIQUE)

### 2.3 Diagnosis (진단)
원인 분석과 처리 방향 제안.

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 진단 식별자 |
| `inquiryId` | UUID | FK→Inquiry.id, 필수, UNIQUE | 대상 문의 (1:1) |
| `cause` | Text | 필수 | 추정 원인 |
| `suggestedDirection` | Text | 필수 | 제안 처리 방향 |
| `confidence` | Decimal | 필수, 0.0 ~ 1.0 | 진단 신뢰도 |

관계:
- Diagnosis `0..1 : 1` Inquiry (inquiryId UNIQUE)

### 2.4 DraftResponse (답변 초안)
AI가 생성한 답변 초안. 재생성 시 새 레코드 또는 버전 증가로 관리.

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 초안 식별자 |
| `inquiryId` | UUID | FK→Inquiry.id, 필수 | 대상 문의 (1:N) |
| `content` | Text | 필수 | 답변 초안 본문 |
| `status` | Enum(DraftResponseStatus) | 필수, 기본값 `GENERATED` | 초안 상태 |
| `regenerationCount` | Integer | 필수, 기본값 0, ≥0 | 재생성 누적 횟수 |
| `createdAt` | Timestamp | 필수, 자동 생성 | 생성 시각 |

관계:
- DraftResponse `N : 1` Inquiry
- 동일 inquiryId 내 최신 초안 1건이 활성(current) 초안

### 2.5 ApprovalHistory (승인/처리 이력)
운영자의 모든 액션을 불변(append-only) 기록.

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 이력 식별자 |
| `inquiryId` | UUID | FK→Inquiry.id, 필수 | 대상 문의 (1:N) |
| `action` | Enum(ApprovalAction) | 필수 | 승인/반려/수정/재생성/배정 |
| `operatorId` | UUID | FK→Operator.id, 필수 | 액션 수행 운영자 |
| `reason` | Text | 조건부 필수 | 반려 시 필수, 그 외 선택 |
| `timestamp` | Timestamp | 필수, 자동 생성 | 액션 시각 |

관계:
- ApprovalHistory `N : 1` Inquiry
- ApprovalHistory `N : 1` Operator

### 2.6 Operator (운영자)
시스템 사용자(상담 운영자/관리자).

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 운영자 식별자 |
| `username` | String | 필수, UNIQUE | 로그인 ID |
| `passwordHash` | String | 필수 | 해시된 비밀번호 (평문 저장 금지) |
| `role` | Enum(OperatorRole) | 필수, 기본값 `OPERATOR` | 역할 |
| `failedLoginCount` | Integer | 필수, 기본값 0, ≥0 | 연속 로그인 실패 횟수 |
| `locked` | Boolean | 필수, 기본값 false | 계정 잠금 여부 (5회 실패 시 true) |

관계:
- Operator `1 : 0..N` Inquiry (배정)
- Operator `1 : 0..N` ApprovalHistory

---

## 3. 데모 더미(Mock) 시스템 테이블

MVP 데모용 사내 시스템 모사 테이블. SystemDataQueryService가 QueryStrategy를 통해 조회한다. 운영 시스템 연동 시 대체된다.

### 3.1 Payment (결제)
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 결제 식별자 |
| `userId` | String | 필수, 인덱스 | 고객 식별자 |
| `amount` | Decimal | 필수, ≥0 | 결제 금액 |
| `status` | Enum(PaymentStatus) | 필수 | 결제 상태 |
| `errorLog` | Text | nullable | 실패 시 오류 로그 |
| `createdAt` | Timestamp | 필수 | 결제 시각 |

### 3.2 ItemDelivery (아이템 지급)
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 지급 식별자 |
| `paymentId` | UUID | FK→Payment.id, nullable | 연관 결제 (1:N) |
| `userId` | String | 필수, 인덱스 | 고객 식별자 |
| `itemId` | String | 필수 | 지급 아이템 식별자 |
| `status` | Enum(DeliveryStatus) | 필수 | 지급 상태 |
| `createdAt` | Timestamp | 필수 | 지급 시각 |

### 3.3 Account (계정)
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | UUID | PK, 필수 | 계정 식별자 |
| `userId` | String | 필수, UNIQUE, 인덱스 | 고객 식별자 |
| `status` | Enum(AccountStatus) | 필수 | 계정 상태 |
| `lastLogin` | Timestamp | nullable | 마지막 로그인 시각 |

관계:
- Payment `1 : 0..N` ItemDelivery (paymentId 경유)
- Account / Payment / ItemDelivery는 `userId`로 논리 연결 (동일 고객 기준 조회)

---

## 4. 엔티티 관계 요약 (ERD 텍스트)

```text
Operator 1 ──────< Inquiry (assignedOperatorId)
Operator 1 ──────< ApprovalHistory (operatorId)

Inquiry  1 ─────── 1  AIAnalysis      (inquiryId UNIQUE, 1:1)
Inquiry  1 ─────── 1  Diagnosis       (inquiryId UNIQUE, 1:1)
Inquiry  1 ──────< N  DraftResponse   (1:N, 재생성 버전)
Inquiry  1 ──────< N  ApprovalHistory (1:N, 이력)

[데모 더미 — userId 논리 연결]
Account  1 ~~~~~ N  Payment       (userId)
Payment  1 ──────< N  ItemDelivery  (paymentId)
Account  1 ~~~~~ N  ItemDelivery  (userId)
```

범례: `1 ─── 1` = 1:1, `1 ───< N` = 1:N, `~~~` = userId 기반 논리 연결(물리 FK 아님).

---

## 5. 데이터 무결성 규칙 (요약)

- `AIAnalysis.inquiryId`, `Diagnosis.inquiryId`는 UNIQUE → 문의당 최대 1건.
- `ApprovalHistory`는 append-only → 수정/삭제 불가.
- `DraftResponse.regenerationCount`는 단조 증가.
- `Operator.passwordHash`는 단방향 해시만 저장.
- 모든 Timestamp는 KST 저장, 표시 시 변환.
