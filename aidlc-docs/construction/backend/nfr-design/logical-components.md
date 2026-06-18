# Logical Components — Backend Unit

NFR을 지원하는 논리적 인프라/기술 요소. 로컬 데모 규모이므로 과도한 인프라는 지양한다.

## 1. 비동기 작업 실행기
- **요소**: Spring `@Async` + `ThreadPoolTaskExecutor`
- **역할**: AI 파이프라인(분석→조회→진단→초안생성)을 백그라운드 실행
- **설정**: 코어 풀 크기 소규모(예: 4), 큐 용량 제한
- **이유**: 문의 접수 API가 AI 처리(최대 600초)를 기다리지 않도록 분리

## 2. 상태 머신 컴포넌트
- **요소**: InquiryStateMachine (enum 기반 전이 검증 + 이벤트 기록)
- **역할**: 모든 상태 전이를 단일 지점에서 검증/실행, 타임스탬프·이력 기록
- **구현**: 경량 자체 구현 (Spring StateMachine 라이브러리는 과함)

## 3. 재시도 핸들러
- **요소**: Spring Retry (`@Retryable`) 또는 자체 재시도 래퍼
- **역할**: BedrockClient/조회 호출의 타임아웃 재시도 + 백오프 + 타임아웃 점증
- **범위**: 외부 호출에만 적용

## 4. 감사 로그 기록기
- **요소**: ApprovalHistory Repository (append-only) + 구조화 로깅(Logback)
- **역할**: 운영자 액션 및 AI 호출 감사 추적
- **로그 채널**: 애플리케이션 로그 / AI 호출 로그(요청·응답·소요시간·실패유형) 분리

## 5. 인증 컴포넌트
- **요소**: Spring Security FilterChain + JWT 발급/검증 유틸
- **역할**: 토큰 기반 인증/인가, 만료 처리

## 6. 미적용 (의도적 제외)
- **메시지 큐**(Kafka/SQS): 로컬 단일 인스턴스, 비동기는 인메모리 Executor로 충분
- **분산 캐시**(Redis): 소규모, 필요 시 인메모리 캐시만
- **서킷 브레이커**: 외부 의존성이 Bedrock 1종, 재시도+graceful degradation으로 충분
- **로드 밸런서**: 단일 인스턴스 (HA 미적용)

> 이 결정들은 "소규모 로컬 데모"라는 배포 제약과 Resiliency 확장 Skip 결정에 따른 것이다. 운영 확장 시 재검토 대상.
