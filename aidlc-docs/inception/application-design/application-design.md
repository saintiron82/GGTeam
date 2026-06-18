# Application Design (통합)

> AI 기반 CS 문의 처리 에이전트 — Application Design 통합 문서  
> 기술 스택: Spring Boot 3.x (JDK 17, 계층형 모놀리식), React + TypeScript (Vite SPA), PostgreSQL, AWS Bedrock, Docker Compose

본 문서는 아래 4개 상세 설계 문서를 요약·통합한다.
- `components.md` — 컴포넌트 정의 및 책임
- `component-methods.md` — 메서드 시그니처
- `services.md` — 서비스 오케스트레이션
- `component-dependency.md` — 의존성 및 데이터 흐름

---

## 1. 아키텍처 개요

**계층형(Layered) 모놀리식 아키텍처**
```
[React SPA] ──HTTP/JSON(JWT)──> [Controller] → [Service] → [Repository] → PostgreSQL
                                                  │
                                          [BedrockClient] → AWS Bedrock
```

- **Controller**는 Service만 호출 (Repository 직접 접근 금지)
- **Service**는 비즈니스 로직 + AI 파이프라인 오케스트레이션
- **외부 연동(Bedrock)**은 Client로 추상화하여 교체 가능

## 2. 핵심 컴포넌트 (요약)

| 계층 | 컴포넌트 |
|------|----------|
| Controller | InquiryController, OperatorController, DashboardController, AuthController |
| Service(핵심) | InquiryService, AIAnalysisService, DiagnosisService, DraftResponseService, ApprovalService |
| Service(지원) | SystemDataQueryService(Strategy), NotificationService, AuthService, InquiryStateMachine |
| Client/추상화 | BedrockClient (LlmClient 구현), QueryStrategy 구현체 |
| Frontend | 고객문의폼, 운영자로그인, 칸반보드, 문의상세, 답변편집기 |

## 3. AI 파이프라인 오케스트레이션

```
문의 접수 → [AIAnalysisService] 분류 → [SystemDataQueryService] 조회
         → [DiagnosisService] 진단 → [DraftResponseService] 답변 초안 자동 생성
         → 담당자배정대기 (운영자 검토 대기)
```
- 각 단계 완료 시 InquiryStateMachine을 통해 상태 전이
- AI 단독 발송 없음 (Human-in-the-loop)

## 4. 운영자 워크플로우

```
미배정 문의 → [Pull 배정] → 운영자확인중 → 검토
  ├─ 승인 → 승인완료 → [발송] → 발송완료
  ├─ 수정 후 승인 → 승인완료 → 발송완료
  └─ 반려(사유) → AI 재생성 → 재검토 (루프)
```

## 5. 핵심 설계 결정

1. **AI 모델 추상화**: 모든 LLM 호출은 `BedrockClient`(LlmClient 인터페이스) 단일 통로 → 모델 교체 용이 (NFR-03)
2. **유형 확장**: `QueryStrategy` 패턴 (MVP는 PaymentQueryStrategy만, 나머지는 인터페이스 구조만)
3. **상태 관리**: 모든 전이는 `InquiryStateMachine` 경유 → 흐름 변경 시 단일 지점 수정
4. **실패 처리**: 타임아웃(120초)은 3회 재시도, API 에러는 즉시 실패 → 수동분류대기
5. **Human-in-the-loop**: 반려는 발송이 아닌 AI 재생성으로 연결

## 6. 상태 머신

```
접수 → AI분석중 → 담당자배정대기 → 운영자확인중 → 승인완료 → 발송완료
              ↓ (실패)
          수동분류대기
```

## 7. 확장성 설계 포인트 (NFR-03)

1. **문의 유형 확장**: `Map<InquiryType, QueryStrategy>` Bean 등록 → 신규 유형 = Strategy 추가만
2. **AI 모델 교체**: `LlmClient` 인터페이스 추상화
3. **상태 흐름 변경**: `InquiryStateMachine` 단일 지점 수정

---

상세 내용은 각 개별 문서를 참조한다.
