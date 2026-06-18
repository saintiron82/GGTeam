# Tech Stack Decisions — Backend Unit

## 확정 기술 스택

| 영역 | 선택 | 근거 |
|------|------|------|
| 언어/런타임 | Java 17 (JDK 17) | 사용자 지정 (LTS) |
| 프레임워크 | Spring Boot 3.x | 사용자 지정, 표준 엔터프라이즈 백엔드 |
| 웹 | Spring Web (REST) | SPA 프론트엔드와 JSON API 연동 |
| 보안 | Spring Security + JWT | 운영자 인증/인가, JWT 8시간 |
| 데이터 접근 | Spring Data JPA (Hibernate) | 엔티티 매핑, Repository 추상화 |
| DB | PostgreSQL | 관계형 데이터(상태/이력/관계) 적합, 추천 채택 |
| DB 마이그레이션 | Flyway | 스키마 버전 관리 + 데모 더미 데이터 시딩 |
| AI 연동 | AWS SDK for Java v2 (Bedrock Runtime) | AWS Bedrock 호출, 최신 최고 성능 모델 사용 |
| 비밀번호 해시 | BCrypt (Spring Security 내장) | 단방향 해시 |
| 빌드 | Gradle (Kotlin DSL) | 표준 빌드 도구 |
| 테스트 | JUnit 5 + Mockito + Spring Boot Test | 단위/통합 테스트, 커버리지 80% |
| 비동기 처리 | Spring `@Async` / 이벤트 | AI 파이프라인 비동기 트리거 |
| 시각 처리 | `ZonedDateTime` (Asia/Seoul) | KST 통일 (BR-41) |
| 로깅 | SLF4J + Logback (구조화 JSON) | 앱 로그 + AI 호출 로그 |
| 컨테이너 | Docker | 로컬 배포 (docker-compose) |

## 핵심 추상화 결정

1. **LlmClient 인터페이스** — `BedrockClient`가 구현. AI 모델/프로바이더 교체 대비.
2. **QueryStrategy 인터페이스** — `Map<InquiryType, QueryStrategy>` Bean 주입. MVP는 `PaymentQueryStrategy`만.
3. **InquiryStateMachine** — 상태 전이 단일 관리 (Spring StateMachine 또는 자체 enum 기반 전이 검증).

## AI 모델 선택

- **프로바이더**: AWS Bedrock
- **모델**: 호출 시점 최신 최고 성능 모델 (설정으로 교체 가능, application.yml의 `bedrock.model-id`)
- **타임아웃**: 초기 120s, 재시도 시 180s→240s 점증
- **리전**: 설정값 (기본 us-east-1, 환경변수 override)

## 의존성 버전 정책
- 정확한 버전 핀 (Gradle version catalog)
- Spring Boot BOM으로 호환 버전 관리
