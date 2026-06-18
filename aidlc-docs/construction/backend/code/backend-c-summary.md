# 백엔드 C 구현 요약 — 워크플로우 & 대시보드 (Operations)

> 브랜치: `feature/backend-c`  
> 범위: 03-backend-assignment.md 백엔드 C 역할. 담당 스토리: US-12, US-15, US-16, US-17, US-18, US-20, US-21, US-22, US-24, US-25, US-27(확장구조)  
> 병렬 전략: 05-parallel-work-plan M1 — 백엔드 A/B 의존부는 stub 선행으로 독립 개발.

## 1. 구현 산출물 (백엔드 C 소유)

### workflow — 운영자 워크플로우
| 파일 | 내용 | 스토리 / 규칙 |
|------|------|---------------|
| `workflow/ApprovalService.java` | C 소유 인터페이스 (pull/edit/approve/reject/reanalyze) | US-15~18, 24 |
| `workflow/ApprovalServiceImpl.java` | Pull 배정 동시성, 승인/수정/반려/재분석, 이력 기록 | BR-11~22, BR-30a~c |
| `workflow/InquiryDetailAssembler.java` | InquiryDetail(§6) + 이력 타임라인 조립 | US-21, US-23(임시) |
| `workflow/api/OperatorController.java` | 운영자 API 6종 | 01-api §3 |
| `workflow/api/CurrentOperatorProvider.java` | JWT principal 해석 (A 보안 연동 추상화, 헤더 폴백) | BR-37 |
| `workflow/dto/*` | InquiryDetail / 요청·응답 DTO | 01-api §3, §6 |

### notification — 발송 & 알림
| 파일 | 내용 | 스토리 / 규칙 |
|------|------|---------------|
| `notification/NotificationServiceImpl.java` | 발송, 재시도 루프, SENT 전이 | US-25, BR-23~25a |
| `notification/NotificationChannel.java` | 발송 채널 추상화 | 확장 구조 |
| `notification/SimulatedNotificationChannel.java` | MVP 시뮬레이션 채널 | — |
| `notification/NotificationConfig.java` | 채널 빈 구성(@ConditionalOnMissingBean) | — |

### dashboard — 조회 & 집계
| 파일 | 내용 | 스토리 |
|------|------|--------|
| `dashboard/DashboardService.java` | 칸반 그룹/필터검색/알림 집계 | US-20, 22, 12 |
| `dashboard/DashboardController.java` | 대시보드 API 3종 | 01-api §4 |
| `dashboard/dto/*` | InquiryCard / NotificationCounts / PageResponse | 01-api §0, §4 |

### persistence — 이력 엔티티 (C 소유)
- `persistence/entity/ApprovalHistory.java` (append-only, BR-40)
- `persistence/repository/ApprovalHistoryRepository.java`

## 2. 병렬 개발용 stub (백엔드 A/B 정식 구현으로 대체 예정)

`@ConditionalOnMissingBean`으로 보호 — A/B의 정식 빈이 머지되면 코드 변경 없이 자동 비활성화.

| stub | 대체 주체 | 위치 |
|------|-----------|------|
| `StubInquiryStateMachine` (전이 테이블 BR-07~10 충실 구현) | 백엔드 A | `workflow/stub/` |
| `StubDraftResponseService` (재생성/한도 BR-17~19) | 백엔드 B | `aipipeline/stub/` |
| `StubAIAnalysisService` (재분석 트리거 로깅) | 백엔드 B | `aipipeline/stub/` |
| 엔티티/Repository (Inquiry/AIAnalysis/Diagnosis/DraftResponse/Operator 등) | 백엔드 A/B | `persistence/` |
| `BackendCStubConfig` (stub 빈 등록) | — | `workflow/stub/` |

> **머지 가이드**: A/B의 정식 엔티티·Repository·서비스 빈이 등록되면 stub은 자동 비활성화된다.
> 엔티티/Repository 파일은 계약(domain-entities.md) 기준으로 작성되었으므로 A/B 정식본과
> 충돌 시 A/B 본을 단일 소스로 채택한다(05-parallel-work-plan §5 조율 지점).

## 3. 핵심 비즈니스 규칙 반영

- **Pull 배정 동시성(BR-12, BR-14)**: `InquiryRepository.claimAssignment` 원자적 조건부 UPDATE.
  미배정+PENDING_ASSIGNMENT인 경우에만 갱신(0행이면 충돌 → 다음 후보). 낙관적 잠금(@Version) 보조.
- **배정 우선순위(BR-13)**: 긴급도(HIGH→NORMAL→LOW) → 접수시각 FIFO (AIAnalysis join 정렬).
- **상태 전이 단일화(BR-09)**: 모든 전이는 InquiryStateMachine 경유.
- **이력 append-only(BR-22, BR-40)**: 모든 운영자 액션(ASSIGN/EDIT/APPROVE/REJECT/REGENERATE/REANALYZE) 기록.
- **반려→재생성(BR-16~19)**: 사유 필수, 재생성 한도 3회, REJECT+REGENERATE 이력.
- **발송 정책(BR-23~25a)**: APPROVED만 발송, 재시도 후 SENT 전이, 한도 초과 시 실패 노출.

## 4. 검증

- **빌드**: `./gradlew compileJava` BUILD SUCCESSFUL (JDK 17, Gradle 8.10.2).
- **테스트**: `./gradlew test` — 31개 통과, 0 실패.
  - ApprovalServiceImplTest (8): Pull 배정 성공/동시성 충돌/없음, 승인/발송, 반려/한도, 재분석
  - NotificationServiceImplTest (4): 발송 성공/상태가드/재시도 성공/한도 초과
  - StubInquiryStateMachineTest (8): 전이 테이블 정합성
  - DashboardServiceTest (3): 칸반 컬럼/카드 조립/알림 집계
  - OperatorControllerTest (8, MockMvc): API 계약 §3 (pull 204/edit/approve/reject 검증·한도/reanalyze/history)
- **Gradle Wrapper**: 프로젝트에 부재하여 추가(gradlew/gradlew.bat/wrapper, Gradle 8.10.2).

## 5. 통합(M2/M3) 시 인계 사항

- 백엔드 A: 정식 엔티티/Repository/InquiryStateMachine/Spring Security 빈 등록 시 C stub 자동 비활성화. InquiryDetail 조립(US-23)은 A 단일본으로 통합.
- 백엔드 B: DraftResponseService/AIAnalysisService 정식 빈 등록 시 stub 대체.
- 프론트: 01-api-contract §3·§4 응답 형식 그대로 사용 가능(ApiResponse 래퍼, 페이지네이션 §0).
