# Requirements Document: AI 기반 CS 문의 처리 에이전트

## Intent Analysis
- **User Request**: AI 기반 CS 문의 자동 분류 및 처리 에이전트 개발
- **Request Type**: New Project (Greenfield)
- **Scope**: Multiple Components
- **Complexity**: Complex
- **Development Priority**: 공통 프레임워크 + 결제 유형 우선 구현, 이후 확장

---

## 1. 프로젝트 개요

### 1.1 목적
AI 에이전트가 고객 CS 문의를 분석하고, 관련 시스템 데이터를 조회하여 현재 상태를 파악한 뒤, 처리 방향 및 답변 초안을 생성한다. 운영자는 AI 결과를 검토 후 최종 승인하여 고객에게 발송한다.

### 1.2 대상 사용자
- **운영자** (CS팀, 1~5명): 웹 대시보드에서 문의 관리 및 답변 승인
- **고객**: 웹 폼을 통해 문의 접수

### 1.3 서비스 형태
- 웹 기반 관리자 대시보드 (운영자용)
- 고객 문의 접수 웹 폼

---

## 2. 기술 스택

| 영역 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.x (JDK 17) |
| 프론트엔드 | React + TypeScript (Next.js) |
| AI/LLM | AWS Bedrock (최신 최고 성능 모델, 선택 가능) |
| 데이터베이스 | PostgreSQL (추천 - 관계형 데이터 적합) |
| 배포 | 소규모 로컬 네트워크 서버 (Docker 기반 권장) |

---

## 3. Functional Requirements

### FR-01: 문의 접수
- 고객이 웹 폼으로 문의 접수 (유형 선택, 내용 입력)
- 문의 유형: 결제, 계정, 버그, 이벤트, 기타
- 접수 시 자동 타임스탬프, 고유 문의 ID 생성

### FR-02: AI 문의 분석
- 문의 내용으로부터 유형, 긴급도, 핵심 키워드 자동 분류
- AWS Bedrock 호출을 통한 NLP 분석
- 분석 결과: 문의 유형, 긴급도(긴급/보통/낮음), 핵심 내용 요약

### FR-03: 시스템 데이터 조회
- 문의 유형에 따라 관련 DB 테이블 조회
- 결제: 결제 이력, 성공/실패 여부, 오류 로그
- 계정: 로그인 이력, 계정 상태, 제재 이력
- 버그: 최근 오류 로그, 서버 상태
- 이벤트: 이벤트 참여 이력, 보상 지급 이력
- (1차 구현: 결제 관련 테이블만, 나머지는 확장 구조 마련)

### FR-04: 상태 진단 및 처리 방향 제안
- 조회 결과 기반 원인 진단
- 즉시 처리 가능 여부 판단
- 추가 확인 필요 시 담당자 알림 생성

### FR-05: 답변 초안 생성
- AI가 조회 결과와 진단을 기반으로 고객 응대 문구 생성
- 운영자가 수정 가능한 형태로 제공

### FR-06: 운영자 승인 워크플로우
- 모든 답변은 운영자 승인 후 발송 (Human-in-the-loop 필수)
- 대시보드에서 승인/반려/수정 후 승인 가능
- 승인 이력 기록

### FR-07: 처리 현황 관리
- 문의 상태: 접수 → AI분석중 → 담당자배정대기 → 운영자확인중 → 승인완료 → 발송완료
- AI 분석 완료 후 유형/긴급도 기반 담당자 자동 추천 또는 관리자 수동 배정
- 문의별 처리 이력(타임라인) 조회
- 필터/검색 (상태, 유형, 긴급도, 날짜, 담당자)

### FR-08: 운영자 대시보드
- 문의 목록 (상태별, 긴급도별 정렬)
- 문의 상세 (AI 분석 결과, 시스템 조회 결과, 답변 초안)
- 답변 편집 및 승인 UI
- 간단한 통계 (처리 건수, 평균 처리 시간)

---

## 4. Non-Functional Requirements

### NFR-01: 성능
- AI 분석 + 답변 생성: 600초 이내 완료
- 대시보드 페이지 로딩: 2초 이내
- 동시 접속 5명 대응

### NFR-02: 보안
- 운영자 로그인 인증 (ID/Password)
- API 인증 (JWT 또는 세션)
- DB 접근 권한 분리

### NFR-03: 확장성
- 문의 유형별 처리 로직을 플러그인 구조로 설계
- 새로운 문의 유형 추가 시 최소 코드 변경
- AI 모델 교체 가능한 추상화 레이어

### NFR-04: 테스트
- 단위 테스트 필수 (JUnit 5 + React Testing Library)
- API 통합 테스트
- 테스트 커버리지 목표: 핵심 비즈니스 로직 80% 이상
- 자동 테스트 케이스 생성기능

### NFR-05: 운영
- Docker Compose 기반 로컬 배포
- 로그 수집 (애플리케이션 로그 + AI 호출 로그)
- 헬스체크 엔드포인트

---

## 5. 데이터 모델 (개요)

### 5.1 핵심 엔티티
- **Inquiry** (문의): id, customer_info, type, urgency, content, status, created_at
- **AIAnalysis** (AI 분석 결과): id, inquiry_id, category, urgency, summary, system_query_results
- **DraftResponse** (답변 초안): id, inquiry_id, content, status, approved_by, approved_at
- **Operator** (운영자): id, username, password_hash, role

### 5.2 게임 서비스 데모 테이블 (더미)
- **Payment** (결제): id, user_id, amount, status, error_log, created_at
- **ItemDelivery** (아이템 지급): id, payment_id, user_id, item_id, status, created_at
- **Account** (계정): id, user_id, status, last_login, restriction_history

---

## 6. 개발 범위 (MVP / Phase 1)

### 포함
- 공통 프레임워크 (문의 접수, AI 분석 파이프라인, 답변 생성, 승인 워크플로우)
- 결제 유형 end-to-end 구현
- 운영자 대시보드 (기본 기능)
- 데모용 더미 DB 스키마 및 샘플 데이터
- 기본 인증 (운영자 로그인)

### 미포함 (Phase 2 이후)
- 계정/버그/이벤트/기타 유형 상세 처리 로직
- 이메일/푸시 알림 발송
- 고객 포털 (문의 상태 확인)
- 복원력/고가용성 설계
- Property-Based Testing

---

## 7. Extension Configuration

| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | No (최소 보안만: 로그인/인증) | Requirements Analysis |
| Property-Based Testing | No (추후 논의, 기본 테스트는 필수) | Requirements Analysis |
| Resiliency Baseline | No | Requirements Analysis |

---
