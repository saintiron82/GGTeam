import { Box, Badge, TypeBadge, UrgencyBadge, Select, SearchBox, ScreenTitle, colors } from "../wireframe-kit";

// US-12, US-20, US-22 / GET /api/v1/dashboard/board + /notifications
// 보드 컬럼: 백엔드 상태 7종 중 RECEIVED + AI_ANALYZING은 "접수·분석중"으로 표시 그룹핑.
//  → 둘 다 시스템 자동 처리 구간(운영자 무액션)이라 한 컬럼으로 묶음. 백엔드 enum은 변경 없음.
const columns = [
  { key: "INTAKE", label: "접수·분석중", states: ["RECEIVED", "AI_ANALYZING"] },
  { key: "PENDING_ASSIGNMENT", label: "담당자배정대기", states: ["PENDING_ASSIGNMENT"] },
  { key: "OPERATOR_REVIEWING", label: "운영자확인중", states: ["OPERATOR_REVIEWING"] },
  { key: "APPROVED", label: "승인완료", states: ["APPROVED"] },
  { key: "SENT", label: "발송완료", states: ["SENT"] },
];

type Card = { id: string; type: string; urgency: "HIGH" | "NORMAL" | "LOW"; summary: string };

const sample: Record<string, Card[]> = {
  INTAKE: [
    { id: "INQ-1042", type: "결제", urgency: "NORMAL", summary: "결제 후 환불 문의" },
    { id: "INQ-1041", type: "기타", urgency: "LOW", summary: "이용 문의" },
  ],
  PENDING_ASSIGNMENT: [
    { id: "INQ-1039", type: "결제", urgency: "HIGH", summary: "결제 완료, 아이템 미지급" },
    { id: "INQ-1038", type: "계정", urgency: "NORMAL", summary: "로그인 불가" },
  ],
  OPERATOR_REVIEWING: [{ id: "INQ-1035", type: "결제", urgency: "HIGH", summary: "중복 결제" }],
  APPROVED: [{ id: "INQ-1030", type: "결제", urgency: "NORMAL", summary: "결제 취소 안내" }],
  SENT: [{ id: "INQ-1021", type: "기타", urgency: "LOW", summary: "처리 완료" }],
};

export default function KanbanScreen() {
  return (
    <div>
      <ScreenTitle api="GET /dashboard/board + GET /dashboard/notifications">
        칸반 보드 (운영자 메인 화면)
      </ScreenTitle>

      {/* 상단 바: 알림 칩 + 보드/리스트 토글 */}
      <Box style={{ marginBottom: 12, display: "flex", alignItems: "center", gap: 12 }}>
        <span style={{ border: `1px solid ${colors.line}`, borderRadius: 4, overflow: "hidden", fontSize: 12 }}>
          <span style={{ padding: "5px 12px", background: colors.primary, color: "#fff", cursor: "pointer", fontWeight: 600 }}>칸반</span>
          <span style={{ padding: "5px 12px", background: "#fff", color: colors.muted, cursor: "pointer" }}>리스트</span>
        </span>
        <Badge color="error">미배정 5건</Badge>
        <Badge color="warning">긴급 2건</Badge>
      </Box>

      {/* 필터/검색 바 — 디자인 가이드 Select/Search */}
      <Box style={{ marginBottom: 12, display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
        <Select label="상태" placeholder="전체" />
        <Select label="긴급도" placeholder="전체" />
        <Select label="유형" placeholder="전체" />
        <Select label="담당자" placeholder="전체" />
        <div style={{ flex: 1 }} />
        <SearchBox placeholder="문의 ID·내용 검색" />
      </Box>

      {/* 5컬럼 보드 */}
      <div style={{ display: "flex", gap: 8, overflowX: "auto" }}>
        {columns.map((col) => (
          <div key={col.key} style={{ flex: "1 0 190px", minWidth: 190 }}>
            <div
              style={{
                background: colors.boxAlt,
                padding: "8px 12px",
                borderRadius: "8px 8px 0 0",
                fontSize: 12,
                fontWeight: 600,
                color: colors.text,
              }}
            >
              {col.label} ({sample[col.key]?.length ?? 0})
            </div>
            <div style={{ background: colors.box, padding: 8, minHeight: 300, borderRadius: "0 0 8px 8px" }}>
              {(sample[col.key] ?? []).map((c) => (
                <div
                  key={c.id}
                  className="g-card g-row--clickable"
                  style={{
                    background: "#fff",
                    border: `1px solid ${c.urgency === "HIGH" ? colors.high : colors.line}`,
                    borderLeft: c.urgency === "HIGH" ? `3px solid ${colors.high}` : `1px solid ${colors.line}`,
                    borderRadius: 6,
                    padding: 10,
                    marginBottom: 8,
                    fontSize: 12,
                    boxShadow: "0 1px 2px rgba(0,0,0,.05)",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontFamily: "'DM Sans', monospace", color: colors.muted, fontSize: 11 }}>{c.id}</span>
                    <UrgencyBadge level={c.urgency} />
                  </div>
                  <div style={{ marginBottom: 4 }}>
                    <TypeBadge type={c.type} />
                  </div>
                  <div style={{ color: colors.text, lineHeight: 1.5 }}>{c.summary}</div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div style={{ fontSize: 11, color: colors.muted, marginTop: 10, lineHeight: 1.7 }}>
        * 카드 클릭 → 문의 상세 이동 / 긴급 카드는 빨간 테두리 강조<br />
        * "접수·분석중"은 RECEIVED + AI_ANALYZING 표시 그룹 (시스템 자동 처리 구간, 운영자 무액션) — 백엔드 상태값은 그대로 유지
      </div>
    </div>
  );
}
