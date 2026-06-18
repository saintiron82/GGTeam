# 컴포넌트 메서드 시그니처 (Component Methods)

> 각 컴포넌트의 주요 메서드 시그니처와 입출력 타입을 정의한다.
> 상세 비즈니스 로직(분기/검증 규칙/프롬프트 등)은 **Functional Design** 단계에서 정의한다.
> 표기는 Java 시그니처 스타일이며, 프론트엔드는 TypeScript 시그니처로 표기한다.

---

## 1. 공통 DTO / 타입 (개요)

```java
// 열거형
enum InquiryType { 결제, 계정, 버그, 이벤트, 기타 }
enum Urgency { 긴급, 보통, 낮음 }
enum InquiryStatus { 접수, AI분석중, 담당자배정대기, 운영자확인중, 승인완료, 발송완료, 수동분류대기 }
enum DraftStatus { 생성됨, 수정됨, 승인됨, 반려됨 }
enum LlmErrorType { TIMEOUT, API_ERROR }

// 요청 DTO
record InquiryCreateRequest(String customerInfo, InquiryType customerType, String content) {}
record LoginRequest(String username, String password) {}
record DraftUpdateRequest(Long inquiryId, String content) {}
record RejectRequest(Long inquiryId, String reason) {}
record DashboardFilter(InquiryStatus status, Urgency urgency, InquiryType aiType,
                       Long operatorId, String keyword, LocalDate from, LocalDate to) {}

// 응답 DTO
record InquiryDetail(Inquiry inquiry, AIAnalysis analysis,
                     SystemQueryResult queryResult, DraftResponse draft,
                     List<HistoryEvent> timeline) {}
record AnalysisResult(InquiryType aiType, String subCategory, Urgency urgency,
                      String summary, List<String> keywords) {}
record DiagnosisResult(String cause, String suggestedAction, double confidence) {}
record SystemQueryResult(InquiryType type, Map<String,Object> data) {}
record KanbanBoard(Map<InquiryStatus, List<InquiryCard>> columns,
                   int unassignedCount, int urgentCount) {}
record TokenResponse(String accessToken, long expiresInSeconds) {}
```

---

## 2. Controller Layer

### InquiryController
```java
@PostMapping("/api/inquiries")            // 공개(고객)
ResponseEntity<InquiryCreatedResponse> create(InquiryCreateRequest req);

@GetMapping("/api/inquiries/{id}")        // JWT
ResponseEntity<InquiryDetail> getDetail(Long id);

@GetMapping("/api/inquiries")             // JWT
ResponseEntity<Page<InquiryCard>> list(DashboardFilter filter, Pageable pageable);
```

### OperatorController
```java
@PostMapping("/api/operator/inquiries/{id}/claim")          // Pull 배정
ResponseEntity<InquiryDetail> claim(Long id, @AuthOperator Long operatorId);

@PutMapping("/api/operator/inquiries/{id}/draft")           // 초안 수정
ResponseEntity<DraftResponse> updateDraft(Long id, DraftUpdateRequest req, @AuthOperator Long operatorId);

@PostMapping("/api/operator/inquiries/{id}/approve")        // 승인(수정 후 승인 포함)
ResponseEntity<ApprovalResult> approve(Long id, @AuthOperator Long operatorId);

@PostMapping("/api/operator/inquiries/{id}/reject")         // 반려 + AI 재생성
ResponseEntity<DraftResponse> reject(Long id, RejectRequest req, @AuthOperator Long operatorId);

@GetMapping("/api/operator/inquiries/{id}/history")
ResponseEntity<List<HistoryEvent>> getHistory(Long id);
```

### DashboardController
```java
@GetMapping("/api/dashboard/board")
ResponseEntity<KanbanBoard> getBoard(DashboardFilter filter);

@GetMapping("/api/dashboard/notifications")
ResponseEntity<NotificationSummary> getNotifications();   // 미배정/긴급 카운트
```

### AuthController
```java
@PostMapping("/api/auth/login")
ResponseEntity<TokenResponse> login(LoginRequest req);

@PostMapping("/api/auth/logout")
ResponseEntity<Void> logout(@AuthOperator Long operatorId);
```

---

## 3. Service Layer

### InquiryService
```java
Inquiry createInquiry(InquiryCreateRequest req);          // ID/타임스탬프 부여 + 저장
void triggerAnalysisPipeline(Long inquiryId);             // 비동기 파이프라인 시작
InquiryDetail getDetail(Long inquiryId);                  // 상세 조립
Page<InquiryCard> search(DashboardFilter filter, Pageable pageable);
```

### AIAnalysisService
```java
AnalysisResult analyze(Long inquiryId);                   // 분류 수행 + 저장
// 내부: 타임아웃 재시도(최대 3회, backoff) / API에러 즉시 실패 → 수동분류대기 전이
AnalysisResult getResult(Long inquiryId);
```

### BedrockClient  (implements LlmClient)
```java
LlmResponse invoke(LlmRequest request) throws LlmException;   // 프롬프트 호출
// LlmRequest(prompt, modelId, maxTokens, timeoutMillis=120000)
// LlmException(LlmErrorType errorType, String detail)  -> TIMEOUT | API_ERROR
```

### SystemDataQueryService  (Strategy Context)
```java
SystemQueryResult query(Long inquiryId, InquiryType aiType, String customerKey);
// 내부: strategyMap.get(aiType).execute(customerKey)
```

```java
// Strategy 인터페이스
interface QueryStrategy {
    InquiryType supports();
    SystemQueryResult execute(String customerKey);
}
// MVP 구현체
class PaymentQueryStrategy implements QueryStrategy { ... }   // 결제이력/성공실패/오류로그 최근 10건
```

### DiagnosisService
```java
DiagnosisResult diagnose(Long inquiryId, SystemQueryResult queryResult);  // 원인/처리방향/신뢰도
```

### DraftResponseService
```java
DraftResponse generate(Long inquiryId, DiagnosisResult diagnosis);   // 진단 직후 자동 생성
DraftResponse update(Long inquiryId, String content, Long operatorId);  // 수정(이력 보관)
DraftResponse regenerate(Long inquiryId, String rejectReason, Long operatorId);  // 반려 사유 반영 재생성
```

### ApprovalService
```java
InquiryDetail claim(Long inquiryId, Long operatorId);     // Pull 배정(동시성: 낙관적 락)
ApprovalResult approve(Long inquiryId, Long operatorId);  // 승인 → 발송 트리거
DraftResponse reject(Long inquiryId, String reason, Long operatorId);  // 반려 → regenerate 위임
List<HistoryEvent> getHistory(Long inquiryId);
```

### InquiryStateMachine
```java
boolean canTransition(InquiryStatus from, InquiryStatus to);
void transition(Long inquiryId, InquiryStatus to, Long actorId, String note); // 검증+기록
```

### NotificationService
```java
SendResult sendApprovedResponse(Long inquiryId);          // 발송 + 상태 전이(발송완료), 실패 재시도
NotificationSummary buildNotificationSummary();           // 미배정/긴급 카운트
```

### AuthService
```java
TokenResponse login(String username, String password);   // 검증 + JWT 발급(8h)
void logout(Long operatorId);                             // 토큰 무효화
Optional<Operator> verifyToken(String token);             // 인증 필터에서 사용
```

---

## 4. Frontend 메서드 (TypeScript 시그니처)

### 고객 문의 폼 (InquiryFormPage)
```typescript
function submitInquiry(req: InquiryCreateRequest): Promise<{ inquiryId: string }>;
function validateForm(form: InquiryForm): ValidationResult;   // 유형 필수, 내용 최소 10자
```

### 운영자 로그인 (LoginPage)
```typescript
function login(username: string, password: string): Promise<TokenResponse>;
```

### 칸반 보드 (KanbanBoardPage)
```typescript
function fetchBoard(filter: DashboardFilter): Promise<KanbanBoard>;
function fetchNotifications(): Promise<NotificationSummary>;
function onCardClick(inquiryId: string): void;   // 상세 이동
```

### 문의 상세 (InquiryDetailPage)
```typescript
function fetchDetail(inquiryId: string): Promise<InquiryDetail>;
function claimInquiry(inquiryId: string): Promise<InquiryDetail>;
function fetchHistory(inquiryId: string): Promise<HistoryEvent[]>;
```

### 답변 편집기 (DraftEditor)
```typescript
function saveDraft(inquiryId: string, content: string): Promise<DraftResponse>;
function approve(inquiryId: string, content?: string): Promise<ApprovalResult>;  // 수정 후 승인
function reject(inquiryId: string, reason: string): Promise<DraftResponse>;       // 반려+재생성
```

### 공통 ApiClient
```typescript
const apiClient: AxiosInstance;   // JWT 인터셉터 + 401 리다이렉트
```

---

> **Note**: 위 시그니처는 인터페이스 계약 수준이다. 트랜잭션 경계, 검증 규칙 상세, 프롬프트 템플릿, 예외 메시지 등은 Functional Design에서 확정한다.
