# NFR Design Patterns — Backend Unit

NFR 요구사항을 충족하기 위해 적용하는 설계 패턴과 적용 위치를 정의한다.

## 1. 복원성 패턴 (Resilience)

| 패턴 | 적용 위치 | 내용 |
|------|-----------|------|
| Retry with Backoff | BedrockClient, SystemDataQueryService | 타임아웃 시 exponential backoff(1s/2s/4s) + 타임아웃 값 점증(120/180/240s), 최대 3회 |
| 실패 유형 구분 | BedrockClient | TIMEOUT(재시도) vs API_ERROR(즉시실패) 분기 |
| Graceful Degradation | AIAnalysisService | AI 실패 시 `MANUAL_CLASSIFICATION_PENDING` 전환 (서비스 중단 없음) |
| 품질 검증 후 재생성 | DraftResponseService | 답변 불량 시 자동 1회 재생성, 재불량 시 수동 위임 |

## 2. 성능 패턴 (Performance)

| 패턴 | 적용 위치 | 내용 |
|------|-----------|------|
| 비동기 처리 (@Async) | InquiryService → AIAnalysisService | 문의 접수 즉시 응답, AI 파이프라인은 백그라운드 실행 |
| DB 인덱싱 | Repository 계층 | `userId`, `status`, `urgency`, `createdAt` 인덱스 |
| 페이지네이션 | DashboardController | 문의 목록 20건 단위 페이징 |
| 조회 결과 스냅샷 | AIAnalysis.systemQueryResult | 시스템 조회 결과 캐싱(재조회 방지) |

## 3. 보안 패턴 (Security)

| 패턴 | 적용 위치 | 내용 |
|------|-----------|------|
| JWT 인증 필터 | Spring Security FilterChain | 운영자 API 토큰 검증, 8시간 만료 |
| 비밀번호 해시 | AuthService | BCrypt 단방향 해시 |
| 역할 기반 인가 | OperatorController, Security Config | ADMIN 전용 작업 분리 |
| 입력 검증 | Controller (Bean Validation) | content 최소 10자, enum 검증 |
| 계정 잠금 | AuthService | 5회 실패 시 locked=true |

## 4. 동시성 패턴 (Concurrency)

| 패턴 | 적용 위치 | 내용 |
|------|-----------|------|
| 원자적 조건부 갱신 | ApprovalService.pullAssign | `WHERE status=PENDING AND assignedOperatorId IS NULL` 조건부 UPDATE, 영향행수로 점유 판정 |
| 상태 전이 검증 | InquiryStateMachine | 전이 테이블 기반 비정상 전이 차단 |

## 5. 적용 우선순위
- 복원성/동시성 패턴은 필수 (데이터 정합성 직결)
- 성능 패턴은 비동기 + 인덱싱 중심 (로컬 데모 규모)
- 캐시/큐 등 무거운 인프라 패턴은 미적용 (소규모)
