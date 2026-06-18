# Backend Code Generation Plan

> Unit: Backend (Spring Boot 3.x, JDK 17)  
> 코드 위치: `/Users/saintiron/Projects/GGTeam/backend/` (워크스페이스 루트, NOT aidlc-docs)  
> 문서 위치: `aidlc-docs/construction/backend/code/`

## 컨텍스트
- **구현 스토리 (MVP)**: US-01~US-09, US-11~US-25 (결제 유형 end-to-end + 공통 프레임워크)
- **확장 구조만**: US-10(유형확장), US-26(자동추천), US-27(통계) — 인터페이스/구조만
- **의존성**: PostgreSQL, AWS Bedrock (외부)
- **단일 소스 오브 트루스**: 이 계획서

## 코드 생성 단계

### Step 1: 프로젝트 구조 설정
- [ ] Gradle 프로젝트 (build.gradle.kts, settings.gradle.kts)
- [ ] Spring Boot 메인 클래스, application.yml (KST, Bedrock, DB 설정)
- [ ] 패키지 구조: auth, inquiry, aipipeline, workflow, dashboard, notification, persistence, external, common
- [ ] 의존성: web, data-jpa, security, validation, flyway, postgresql, aws-sdk bedrock, jjwt

### Step 2: 도메인 엔티티 & Enum (persistence)
- [ ] Enum: InquiryStatus, InquiryType, Urgency, ApprovalAction, FailureType, DraftResponseStatus, OperatorRole, PaymentStatus, DeliveryStatus, AccountStatus
- [ ] 엔티티: Inquiry, AIAnalysis, Diagnosis, DraftResponse, ApprovalHistory, Operator
- [ ] 데모 더미 엔티티: Payment, ItemDelivery, Account
- [ ] 매핑/제약조건 (UNIQUE, FK, KST 타임스탬프)

### Step 3: Repository 계층
- [ ] InquiryRepository, AIAnalysisRepository, DiagnosisRepository, DraftResponseRepository, ApprovalHistoryRepository, OperatorRepository
- [ ] 더미: PaymentRepository, ItemDeliveryRepository, AccountRepository
- [ ] Pull 배정용 원자적 갱신 쿼리
- [ ] Repository 단위 테스트

### Step 4: 상태 머신 (workflow)
- [ ] InquiryStateMachine (전이 테이블 검증)
- [ ] 상태 전이 단위 테스트 (정상/비정상 전이)

### Step 5: 외부 연동 추상화 (external)
- [ ] LlmClient 인터페이스
- [ ] BedrockClient 구현 (타임아웃 점증, 재시도, 실패유형 구분)
- [ ] 로컬 테스트용 MockLlmClient (프로파일 분리)

### Step 6: AI 파이프라인 (aipipeline)
- [ ] AIAnalysisService (분류/요약, 재시도, 품질검증)
- [ ] QueryStrategy 인터페이스 + PaymentQueryStrategy (Map 등록)
- [ ] SystemDataQueryService (Strategy Context)
- [ ] DiagnosisService (원인 진단)
- [ ] DraftResponseService (초안 생성/재생성/품질검증)
- [ ] 파이프라인 오케스트레이션 (@Async)
- [ ] 단위 테스트 (MockLlmClient 사용)

### Step 7: 워크플로우 서비스 (workflow)
- [ ] InquiryService (접수, 조회, 상세 조립)
- [ ] ApprovalService (Pull 배정 동시성, 승인/반려/수정/재분석, 이력)
- [ ] NotificationService (발송, 재시도, 알림)
- [ ] 단위 테스트

### Step 8: 인증 (auth)
- [ ] AuthService (로그인, JWT 발급/검증, 5회 잠금, BCrypt)
- [ ] JWT 필터 + Spring Security 설정
- [ ] 단위 테스트

### Step 9: API 계층 (Controller)
- [ ] InquiryController (문의 접수/조회/상세)
- [ ] OperatorController (배정/수정/승인/반려/재분석/이력)
- [ ] DashboardController (칸반/필터/검색/알림)
- [ ] AuthController (로그인/로그아웃)
- [ ] DTO 정의 + Bean Validation
- [ ] API 통합 테스트 (MockMvc)

### Step 10: DB 마이그레이션 & 시딩
- [ ] Flyway 스키마 마이그레이션 (V1__init.sql)
- [ ] 데모 더미 데이터 시딩 (V2__demo_data.sql) — 결제 시나리오 샘플
- [ ] 운영자 초기 계정

### Step 11: 배포 아티팩트
- [ ] backend/Dockerfile
- [ ] application.yml (prod/local 프로파일)

### Step 12: 문서화
- [ ] API 명세 요약 (aidlc-docs/construction/backend/code/)
- [ ] README (빌드/실행)

## 스토리 추적
각 Step 완료 시 관련 US-xx를 stories.md 기준으로 체크.

## 검증
- 핵심 비즈니스 로직 테스트 커버리지 80% 목표
- 결제 유형 end-to-end 시나리오 통과
