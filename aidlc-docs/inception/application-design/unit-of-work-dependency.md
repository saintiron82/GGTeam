# Unit of Work Dependency (작업 단위 의존성)

## 의존성 매트릭스

| 단위 | 의존 대상 | 의존 유형 |
|------|-----------|-----------|
| Unit 1: Backend | PostgreSQL, AWS Bedrock | 런타임 (외부 서비스) |
| Unit 2: Frontend | Unit 1: Backend (REST API) | 런타임 (API 계약) |

## 의존성 다이어그램

```
[Unit 2: Frontend] ──REST API(JWT)──> [Unit 1: Backend] ──> PostgreSQL
                                                          └─> AWS Bedrock
```

## 개발 순서 (권장)

1. **Unit 1 (Backend) 우선** — API 계약 정의 및 핵심 로직 구현
   - 이유: Frontend가 Backend API에 의존하므로 API 계약이 먼저 확정되어야 함
2. **Unit 2 (Frontend) 후속** — 확정된 API를 소비하는 UI 구현

> **병렬화 가능**: API 계약(OpenAPI/엔드포인트 명세)이 먼저 합의되면 Backend 구현과 Frontend 구현을 부분적으로 병렬 진행 가능.

## 조율 지점 (Coordination Points)

- **REST API 계약**: 엔드포인트 경로, 요청/응답 DTO (Backend가 정의, Frontend가 소비)
- **인증 방식**: JWT 토큰 형식 및 헤더 규약
- **상태 enum**: 문의 상태 값 (Backend ↔ Frontend 공유)
- **에러 응답 형식**: 표준 에러 스키마

## 테스트 전략

- Unit 1: JUnit 5 단위 테스트 + Spring Boot 통합 테스트 (API 레벨)
- Unit 2: React Testing Library 컴포넌트 테스트
- 통합: 결제 유형 end-to-end 시나리오 (Build and Test 단계)
