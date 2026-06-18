import { Box, Badge, TypeBadge, Btn, UrgencyBadge, ScreenTitle, colors } from "../wireframe-kit";

// US-15, US-18, US-21, US-23 / GET /inquiries/{id} + /history
export default function DetailScreen() {
  return (
    <div>
      <ScreenTitle api="GET /inquiries/{id} + GET /operator/inquiries/{id}/history">
        문의 상세
      </ScreenTitle>

      <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
        {/* 좌측: 본문 + 분석 + 진단 */}
        <div style={{ flex: 2 }}>
          <Box label="원본 문의" style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 12, color: colors.muted, marginBottom: 6 }}>
              INQ-1039 · 고객선택: 기타 · user12345 · 2026-06-18 15:00 KST
            </div>
            <div style={{ fontSize: 13 }}>결제는 완료됐는데 아이템이 지급되지 않았어요. 확인 부탁드립니다.</div>
          </Box>

          <Box label="AI 분석 결과" style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 13, lineHeight: 1.8 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
                AI 유형: <TypeBadge type="결제" /> / 서브: 아이템미지급 <UrgencyBadge level="HIGH" />
              </div>
              <div>요약: 결제 완료 후 아이템 지급 누락 추정</div>
              <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 4 }}>
                키워드:{" "}
                <Badge color="default">결제완료</Badge>
                <Badge color="default">아이템 미지급</Badge>
              </div>
            </div>
          </Box>

          <Box label="시스템 조회 결과 (Payment)" style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 12, fontFamily: "monospace", color: colors.text, background: colors.box, padding: 8, borderRadius: 4 }}>
              결제 PAY-8821 · 10,000원 · status=SUCCESS<br />
              아이템지급 ITM-5510 · status=FAILED (시스템 오류)
            </div>
          </Box>

          <Box label="AI 진단">
            <div style={{ fontSize: 13, lineHeight: 1.8 }}>
              <div>원인: 시스템 오류로 아이템 지급 누락</div>
              <div>처리방향: 보상 지급 및 안내</div>
              <div>신뢰도: 0.92</div>
            </div>
          </Box>
        </div>

        {/* 우측: 처리 이력 타임라인 */}
        <div style={{ flex: 1 }}>
          <Box label="처리 이력 (타임라인)">
            <div style={{ fontSize: 12, lineHeight: 2 }}>
              <div>● 15:03 답변초안 생성</div>
              <div>● 15:02 AI 진단 완료</div>
              <div>● 15:01 AI 분석 완료</div>
              <div>● 15:00 문의 접수</div>
            </div>
          </Box>
          <div style={{ marginTop: 12 }}>
            <Btn primary>담당하기 (Pull)</Btn>
          </div>
          <div style={{ fontSize: 11, color: colors.muted, marginTop: 8 }}>
            * "담당하기"는 PENDING_ASSIGNMENT 상태에서만 활성
          </div>
        </div>
      </div>

      {/* 담당하기(Pull) 이후 — 답변 편집기(S6)가 상세 하단에 펼쳐짐 */}
      <div
        style={{
          marginTop: 20,
          border: `1px dashed ${colors.lineInput}`,
          borderRadius: 8,
          padding: 20,
          textAlign: "center",
          color: colors.muted,
          background: colors.box,
          fontSize: 13,
          lineHeight: 1.7,
        }}
      >
        ⬇ <b style={{ color: colors.text }}>담당하기(Pull)</b> 클릭 시 상태가 <b style={{ color: colors.primary }}>운영자확인중</b>으로 바뀌고,
        <br />이 영역에 <b style={{ color: colors.text }}>답변 편집기(S6)</b>가 펼쳐집니다. (별도 페이지 아님 · 같은 상세 화면 내 영역)
      </div>
    </div>
  );
}
