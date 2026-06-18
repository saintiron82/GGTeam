# Requirements Verification Questions

## Intent Analysis Summary
- **User Request**: AI 기반 CS 문의 처리 에이전트 개발
- **Request Type**: New Project
- **Scope Estimate**: Multiple Components (AI 분석, 시스템 조회, 답변 생성, 운영자 인터페이스)
- **Complexity Estimate**: Complex

---

## Question 1: 대상 플랫폼 및 서비스 형태
이 시스템은 어떤 형태로 제공될 예정인가요?

A) 웹 기반 관리자 대시보드 (운영자가 브라우저에서 사용)
B) Slack/Teams 등 메신저 봇 연동
C) 기존 CS 툴(Zendesk, Freshdesk 등)에 플러그인 형태
D) REST API 서비스 (다른 시스템에서 호출)
E) CLI 도구
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 2: 게임 서비스 시스템 연동
문서에서 "결제 서버", "지급 서버", "오류 로그"를 조회하는 시나리오가 있습니다. 연동 대상 시스템의 형태는?

A) 기존 REST API가 이미 존재 (조회용 API 호출만 하면 됨)
B) 데이터베이스 직접 조회 (DB 연결 필요)
C) Mock/Stub 서버로 데모 수준 구현 (실제 시스템 미연동)
D) 아직 미정 - 연동 인터페이스는 추후 결정
X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 3: AI/LLM 모델 선택
문의 분석 및 답변 생성에 사용할 AI 모델은?

A) AWS Bedrock (Claude, Titan 등)
B) OpenAI API (GPT-4 등)
C) 자체 호스팅 오픈소스 모델 (Llama 등)
D) 아직 미정 - 추후 결정
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 4: 프로그래밍 언어 및 기술 스택
선호하는 기술 스택이 있나요?

A) Python (FastAPI/Flask) + React/Next.js 프론트엔드
B) TypeScript (Node.js/NestJS) + React/Next.js 프론트엔드
C) Python 백엔드만 (프론트엔드 없이 API 서비스)
D) 전체 기술 스택 추천을 원함
X) Other (please describe after [Answer]: tag below)

[Answer]: B / 백엔드는 스프링 부트 로 진행 jdk17

---

## Question 5: 문의 유형 범위
처리할 문의 유형은 결제 관련만인가요, 아니면 다른 유형도 포함하나요?

A) 결제 관련 문의만 우선 구현
B) 결제 + 계정(로그인/비밀번호) 문의
C) 결제 + 계정 + 게임 내 버그 신고
D) 모든 유형의 CS 문의 (결제, 계정, 버그, 이벤트, 기타)
X) Other (please describe after [Answer]: tag below)

[Answer]: D

---

## Question 6: 운영자 워크플로우
AI가 생성한 답변에 대해 운영자는 어떻게 처리하나요?

A) 반드시 운영자 승인 후 고객에게 발송 (Human-in-the-loop 필수)
B) 긴급도 낮은 건은 자동 발송, 긴급/민감한 건만 운영자 승인
C) 모두 자동 발송하고 운영자는 사후 모니터링만
D) 아직 미정
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 7: 배포 환경
배포 대상 인프라는?

A) AWS (ECS/Lambda + RDS 등 AWS 서비스 활용)
B) 자체 서버/온프레미스
C) 기타 클라우드 (GCP, Azure 등)
D) 로컬 데모 수준으로 우선 구현 (배포는 추후)
X) Other (please describe after [Answer]: tag below)

[Answer]: D / 가능하면 소규모 네트워크 로컬 서버 기반

---

## Question 8: 데이터 저장소
문의 이력, 처리 상태 등의 데이터 저장은?

A) RDB (PostgreSQL/MySQL 등)
B) NoSQL (DynamoDB/MongoDB 등)
C) 추천을 원함
D) 아직 미정
X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Question 9: Security Extensions
이 프로젝트에 보안 확장 규칙을 적용할까요?

A) Yes — 모든 SECURITY 규칙을 차단 조건으로 적용 (프로덕션 수준 권장)
B) No — SECURITY 규칙 건너뛰기 (PoC, 프로토타입, 실험적 프로젝트에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: 최소한의 로그인 비번 정도

---

## Question 10: Property-Based Testing Extension
Property-Based Testing (PBT) 규칙을 적용할까요?

A) Yes — 모든 PBT 규칙 적용 (비즈니스 로직, 데이터 변환이 있는 프로젝트에 권장)
B) Partial — 순수 함수와 직렬화 라운드트립에만 적용
C) No — PBT 규칙 건너뛰기 (단순 CRUD, UI 위주 프로젝트에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: 차후 논의 하지만 반드시 테스트 시스템은 만들어야 한다 

---

## Question 11: Resiliency Extensions
Resiliency Baseline을 적용할까요? (AWS Well-Architected Framework 기반 복원력 설계 가이드)

A) Yes — 복원력 기준 적용 (비즈니스 크리티컬 워크로드에 권장, go-live 전 검증 필요)
B) No — 복원력 기준 건너뛰기 (PoC, 프로토타입에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Follow-up Questions (추가 질문)

---

## FQ1: DB 직접 조회 상세
DB 직접 조회(Q2:B)를 선택하셨는데, 구체적으로:

A) 게임 서비스 DB에 직접 연결 (읽기 전용 계정 사용)
B) 별도 레플리카/분석용 DB에 연결
C) 아직 DB 스키마가 없으므로, 데모용 더미 테이블을 직접 설계해서 진행
D) 실제 DB 연결은 추후이고, 우선은 Mock 데이터로 개발

[Answer]: C

---

## FQ2: 프론트엔드 상세
백엔드는 Spring Boot(JDK17)로 확인했습니다. 프론트엔드는:

A) React + TypeScript (Vite)
B) Next.js (SSR/SSG)
C) 프론트엔드도 추천을 원함
D) 프론트엔드 없이 백엔드 API만 우선 개발, UI는 추후

[Answer]: A

---

## FQ3: 문의 접수 채널
모든 유형의 CS를 처리한다고 하셨는데, 고객 문의가 시스템에 들어오는 경로는?

A) 고객이 직접 웹 폼으로 문의 접수
B) 이메일로 접수된 문의를 운영자가 수동 입력
C) 기존 CS 시스템에서 API로 문의 데이터 전달
D) 운영자가 대시보드에서 문의 내용을 직접 붙여넣기
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## FQ4: AWS Bedrock 리전 및 모델
Bedrock 사용 시:

A) Claude 3.5 Sonnet (빠르고 비용 효율적)
B) Claude 3 Opus (최고 품질)
C) 아직 미정, 추천을 원함
D) 로컬 데모 시에는 OpenAI API 등으로 대체하고, 추후 Bedrock 전환

[Answer]: 선택가능 최신 최고성능 모델

---

## FQ5: 사용자 규모 및 동시 접속
로컬 서버 기반이라고 하셨는데, 예상 운영자 수는?

A) 1~5명 (소규모 CS팀)
B) 5~20명 (중규모)
C) 20명 이상
D) 데모 단계라 1~2명 수준

[Answer]: A

---

## FQ6: 문의 유형별 우선순위
모든 CS 유형(결제, 계정, 버그, 이벤트, 기타)을 지원하되, 개발 우선순위는?

A) 모든 유형 동시에 구현 (MVP에 전부 포함)
B) 결제 → 계정 → 버그 → 이벤트 순으로 단계적 구현
C) 결제만 먼저 완성하고, 나머지는 확장 가능한 구조만 마련
X) Other (please describe after [Answer]: tag below)

[Answer]: C 

---
