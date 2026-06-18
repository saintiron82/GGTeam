# Frontend Code Generation Plan

> Unit: Frontend (React 18 + TypeScript, Vite)
> 코드 위치: 워크스페이스 루트 `frontend/` (NOT aidlc-docs)
> 문서 위치: `aidlc-docs/construction/plans/`
> 기준 계약: `01-api-contract.md`, `02-shared-contracts.md`, `04-frontend-assignment.md`

## 컨텍스트
- **구현 스토리 (MVP)**: US-01~05, US-12, US-14~24 (04-frontend-assignment 개발자 담당)
- **병렬 개발 전략**: 백엔드 API 미완성 구간은 MSW(Mock Service Worker)로 01 계약 기반 모킹 → 독립 개발
- **기술 스택**: React 18, TypeScript, Vite, react-router-dom v6, Axios
- **단일 소스 오브 트루스**: 이 계획서 (프론트 개발 영역)

## 코드 생성 단계

### Step 1: 프로젝트 구조 & 공통 기반 (M0 제공)
- [x] Vite + TS 프로젝트 (package.json, vite.config.ts, tsconfig.json, index.html)
- [x] 공유 타입 `src/common/types.ts` (백엔드 enum 1:1, 한글 라벨 맵 포함)
- [x] API 클라이언트 골격 `src/common/apiClient.ts` (Axios + JWT 인터셉터, 토큰 헬퍼)
- [x] 배포 아티팩트: Dockerfile, nginx.conf

### Step 2: 공통 컴포넌트 & 인증 컨텍스트 (common)
- [x] `apiTypes.ts` — 01 계약 기반 요청/응답 DTO 타입
- [x] `api.ts` — 도메인별 API 호출 함수 + `extractErrorMessage`
- [x] `AuthContext.tsx` — 토큰/운영자 상태, login/logout, localStorage 동기화
- [x] `ProtectedRoute.tsx` — 미인증 시 `/login` 리다이렉트
- [x] `AppLayout.tsx` — 운영자 공통 헤더/네비/로그아웃
- [x] `UrgencyBadge.tsx` — 긴급도 색상 배지
- [x] `styles.css` — 공통 스타일 토큰/레이아웃

### Step 3: 인증 화면 (auth) — US-14
- [x] `LoginPage.tsx` — 로그인 폼, 검증, 에러 표시, 로그인 후 원래 목적지(from) 복귀
- [x] data-testid 부여 (login-form/username/password/submit/error)

### Step 4: 고객 문의 접수 (customer) — US-01~02
- [x] `InquiryFormPage.tsx` — 유형 선택, 내용 최소 길이 검증, 접수 성공 화면(문의번호 안내)
- [x] data-testid 부여 (inquiry-form/type/content/submit/success/id)

### Step 5: 칸반 보드 & 조회 (kanban) — US-15~19
- [x] `KanbanBoardPage.tsx` — 상태별 6컬럼, 칸반/리스트 뷰 전환, 필터(유형/긴급도/키워드), 알림 배지
- [x] `InquiryCardItem.tsx` — 문의 카드
- [x] data-testid 부여

### Step 6: 문의 상세 & 타임라인 (detail) — US-12, US-20~22
- [x] `InquiryDetailPage.tsx` — AI 분석/조회 결과/초안/타임라인, Pull 배정, 뒤로가기
- [x] data-testid 부여 (detail-back 등)

### Step 7: 답변 편집기 (editor) — US-23~24
- [x] `DraftEditor.tsx` — 인라인 편집, 승인/반려/수정후승인/재분석 버튼
- [x] 상태별 버튼 활성/비활성 (02 §5 상태전이 규약 기반)

### Step 8: MSW 목 서버 (병렬 개발 장치)
- [x] `mocks/store.ts` — 인메모리 상태 저장소
- [x] `mocks/handlers.ts` — 01 계약 기반 핸들러 (인증/문의/대시보드/배정/승인 등)
- [x] `mocks/browser.ts` — `setupWorker` + `startMockWorker`
- [x] `public/mockServiceWorker.js` — MSW 워커 스크립트

### Step 9: 라우팅 통합 (main.tsx)
- [x] `AuthProvider > BrowserRouter > Routes` 구성
- [x] 개발 환경(`import.meta.env.DEV`)에서 MSW 워커 선기동 후 렌더
- [x] `src/vite-env.d.ts` 추가 (`vite/client` 타입 — `import.meta.env` 타입 오류 해결)
- [x] 공통 `styles.css` 진입점 import

### Step 10: 빌드 & 검증
- [x] `npm run build` (tsc 타입체크 + vite build) 통과 — 98 modules, dist 생성
- [ ] 컴포넌트 테스트 (React Testing Library) — **미완성** (테스트 파일 없음, 후속 작업)

## 라우팅 설계 (결정 기록)

| 경로 | 화면 | 접근 |
|------|------|------|
| `/` | → `/board` 리다이렉트 | 공개(미인증 시 로그인으로) |
| `/login` | LoginPage | 공개 |
| `/submit` | InquiryFormPage (고객 접수) | 공개 |
| `/board` | KanbanBoardPage | 운영자 전용 (ProtectedRoute) |
| `/inquiry/:id` | InquiryDetailPage | 운영자 전용 (ProtectedRoute) |
| `*` | → `/board` 리다이렉트 | — |

**결정 배경**: 본 앱은 운영자용 CS 처리 도구이므로 루트(`/`) 진입 시 인증 흐름을 우선한다.
미인증 사용자는 `/` → `/board` → (ProtectedRoute) → `/login` 으로 자연스럽게 로그인 화면에 도달한다.
고객 문의 접수 폼은 인증이 필요 없는 별도 공개 경로(`/submit`)로 분리하여 고객에게 직접 안내한다.
(초기 구현에서는 `/`를 고객 폼으로 두었으나, "운영자 앱은 로그인 우선" 원칙에 따라 변경)

## 스토리 추적
- 화면 구현 완료: US-01~05, US-12, US-14~24 (Step 3~7)
- 미완성: 컴포넌트 테스트 (06-test-case-guide 기반, 후속)

## 검증
- 타입 안전 빌드(`npm run build`) 통과
- MSW로 백엔드 없이 화면 동작 가능
- 실제 백엔드는 통합 시점(M4)에 `/api` 프록시로 연결
