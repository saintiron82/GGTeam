# 와이어프레임 (기획자 전용)

> 프론트 개발자의 `frontend/`와 **완전히 분리된** 기획용 프로젝트입니다.
> 와이어프레임만 그리는 용도이며, 실제 제품 코드가 아닙니다.

## 충돌 방지 규칙
- 개발자 영역: `GGTeam/frontend/` (포트 5173) — **여기는 건드리지 않습니다**
- 기획자 영역: `GGTeam/wireframe/` (포트 5174) — **이 폴더만 작업**
- 서로 다른 폴더 + 다른 포트라 동시에 띄워도 충돌 없음

## 실행 방법
```bash
cd wireframe
npm install
npm run dev
```
브라우저에서 http://localhost:5174 접속 → 좌측 메뉴로 화면 전환

## 포함된 화면 (6개)
1. 칸반 보드 — 운영자 메인 (상태별 6컬럼 + 알림 + 필터)
2. 리스트 뷰 — 칸반과 동일 데이터의 표 형태 + 필터/검색/페이징
3. 문의 상세 — AI분석/조회결과/진단/타임라인
4. 답변 편집기 — 승인/반려/수정후승인/재분석
5. 고객 문의 폼 — 문의 접수
6. 로그인 — 운영자 인증

## 디자인 가이드 (GRAVITY)
- 토큰 출처: `sample/gravity_*.html` (디자인 토큰 / Atomic 컴포넌트 / 레이아웃 스펙)
- 컬러: Primary `#05539A`, Accent `#06ADEC`(강조 한정), Neutral Gray
- 타이포: Pretendard(국문) / DM Sans(영문·숫자) — `index.html`에서 로드
- Radius sm 4px(버튼·입력·뱃지) / lg 8px(카드), Elevation Level1~2
- 상태 컬러: 성공=green, 진행/정보=Primary, 경고=amber, 에러=red
- 긴급도 매핑: HIGH=error(빨강) / NORMAL=warning(주황) / LOW=neutral(회색)

## 수정 방법
- 각 화면: `src/screens/*.tsx`
- 공통 조각(버튼/박스/뱃지/입력): `src/wireframe-kit.tsx`
- 디자인 토큰(색/폰트/상태 CSS): `src/wireframe-kit.tsx`의 `colors`·`elevation`·`radius` + `index.html`
- 보면서 "여기 바꿔줘" 하면 즉시 반영됩니다 (저장 시 자동 새로고침)
