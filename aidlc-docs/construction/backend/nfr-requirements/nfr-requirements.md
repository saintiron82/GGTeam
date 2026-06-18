# NFR Requirements — Backend Unit

> AI 기반 CS 문의 처리 에이전트 Backend Unit의 비기능 요구사항. requirements.md의 NFR-01~05를 Backend 관점에서 구체화한다.

## 1. 성능 (Performance)

| ID | 요구사항 | 목표 |
|----|----------|------|
| PERF-01 | AI 파이프라인(분석+조회+진단+초안생성) 완료 시간 | 600초 이내 (타임아웃 점증 120/180/240s 반영) |
| PERF-02 | 일반 REST API(조회/배정/승인) 응답 시간 | 95퍼센타일 2초 이내 |
| PERF-03 | 칸반 보드 데이터 집계 조회 | 2초 이내 |
| PERF-04 | 동시 처리 | 운영자 5명 동시 작업 무지연 |

## 2. 확장성 (Scalability)

| ID | 요구사항 |
|----|----------|
| SCAL-01 | 문의 유형 확장은 QueryStrategy 구현체 추가만으로 가능 (코드 변경 최소화) |
| SCAL-02 | AI 모델 교체는 LlmClient 구현체 교체로 가능 |
| SCAL-03 | 소규모(1~5 운영자) 기준 설계, 단일 인스턴스로 충분 |

## 3. 가용성 (Availability)

| ID | 요구사항 |
|----|----------|
| AVAIL-01 | 로컬 데모 수준, 고가용성(HA) 미적용 (Resiliency 확장 Skip) |
| AVAIL-02 | 헬스체크 엔드포인트 제공 (`/actuator/health`) |
| AVAIL-03 | AI 외부 호출 실패 시 graceful degradation (수동분류대기 전환) |

## 4. 보안 (Security)

| ID | 요구사항 |
|----|----------|
| SEC-01 | 운영자 인증: JWT 기반, 유효기간 8시간 (BR-34) |
| SEC-02 | 비밀번호 단방향 해시 저장 (BCrypt 권장) (BR-36) |
| SEC-03 | 로그인 5회 실패 시 계정 잠금 (BR-31) |
| SEC-04 | 운영자 API는 인증 필수, 고객 문의 접수 API만 비인증 허용 |
| SEC-05 | ADMIN 역할 권한 분리 (계정 잠금 해제 등) (BR-38) |
| SEC-06 | DB 접근 계정 권한 최소화 |

> 보안 확장(Security Baseline)은 Requirements에서 Skip — 최소 인증/인가만 적용.

## 5. 신뢰성 (Reliability)

| ID | 요구사항 |
|----|----------|
| REL-01 | AI 호출 실패 구분 처리 (타임아웃 재시도 / API에러 즉시실패) |
| REL-02 | 상태 전이는 StateMachine으로 강제 (비정상 전이 차단) |
| REL-03 | 발송 실패 시 재시도 + 이력 기록 (BR-25a) |
| REL-04 | ApprovalHistory append-only로 감사 추적 보장 |

## 6. 유지보수성 (Maintainability)

| ID | 요구사항 |
|----|----------|
| MAINT-01 | 계층형 아키텍처 (Controller/Service/Repository) 준수 |
| MAINT-02 | 핵심 비즈니스 로직 단위 테스트 커버리지 80% 이상 (NFR-04) |
| MAINT-03 | 자동 테스트 케이스 생성 기능 포함 |
| MAINT-04 | 애플리케이션 로그 + AI 호출 로그 구조화 기록 |

## 7. 시각/로케일 (Locale)

| ID | 요구사항 |
|----|----------|
| LOC-01 | 모든 Timestamp는 KST(Asia/Seoul)로 저장 및 표시 (BR-41) |
