import { Box, Btn, ScreenTitle, colors } from "../wireframe-kit";

// US-14, US-16, US-17, US-24 / PATCH .../draft, POST .../approve|reject|reanalyze
export default function EditorScreen() {
  return (
    <div>
      <ScreenTitle api="PATCH .../draft · POST .../approve · .../reject · .../reanalyze">
        답변 편집기 (운영자확인중 상태)
      </ScreenTitle>

      <div
        style={{
          fontSize: 12,
          color: colors.muted,
          background: colors.primary50,
          border: `1px solid ${colors.line}`,
          borderRadius: 6,
          padding: "8px 12px",
          marginBottom: 16,
          lineHeight: 1.6,
        }}
      >
        ℹ 이 화면은 별도 페이지가 아니라 <b style={{ color: colors.text }}>문의 상세(S5)</b>에서 담당하기(Pull) 후
        하단에 펼쳐지는 <b style={{ color: colors.text }}>편집기 영역</b>입니다.
      </div>

      <Box label="AI 답변 초안 (편집 가능 · 재생성 1회)" style={{ marginBottom: 12 }}>
        <div
          style={{
            border: `1px solid ${colors.lineInput}`,
            borderRadius: 4,
            padding: 12,
            minHeight: 160,
            fontSize: 13,
            lineHeight: 1.7,
            background: colors.bgSurface,
          }}
        >
          안녕하세요, 고객님. 결제 후 아이템이 지급되지 않은 점 불편을 드려 죄송합니다.
          확인 결과 시스템 오류로 지급이 누락되어, 해당 아이템을 즉시 지급해 드렸습니다.
          이용에 불편을 드려 다시 한번 사과드립니다.
        </div>
        <div style={{ fontSize: 11, color: colors.muted, marginTop: 6 }}>
          * 인라인 편집 / 원문 대비 수정점 하이라이트
        </div>
      </Box>

      {/* 액션 버튼 — 상태전이 규약 기반 */}
      <Box label="처리 액션 (OPERATOR_REVIEWING 상태에서만 활성)">
        <div style={{ marginBottom: 10 }}>
          <Btn primary>승인</Btn>
          <Btn>수정 후 승인</Btn>
          <Btn danger>반려 및 재생성</Btn>
          <Btn>재분석</Btn>
        </div>
        <div style={{ fontSize: 12, color: colors.text, marginTop: 8 }}>
          <div>· 승인 → 상태 APPROVED → 자동 발송(SENT)</div>
          <div>· 반려 → 사유 입력(필수) → AI 재생성 (최대 3회, 초과 시 비활성)</div>
          <div>· 재분석 → AI_ANALYZING 으로 되돌림</div>
        </div>
      </Box>

      <Box label="반려 사유 입력 (반려 클릭 시 노출)" style={{ marginTop: 12 }}>
        <div
          style={{
            border: `1px solid ${colors.lineInput}`,
            borderRadius: 4,
            padding: 8,
            minHeight: 50,
            fontSize: 13,
            color: colors.muted,
            background: colors.bgSurface,
          }}
        >
          예: 보상 금액 안내가 빠졌습니다. 지급 아이템 수량을 명시해 주세요.
        </div>
        <div style={{ fontSize: 11, color: colors.error, marginTop: 6 }}>
          * 사유 없이 반려 불가 (REASON_REQUIRED 400)
        </div>
      </Box>
    </div>
  );
}
