import { Box, TypeBadge, StatusBadge, UrgencyBadge, Select, SearchBox, Pagination, ScreenTitle, colors } from "../wireframe-kit";

// US-22 / GET /api/v1/dashboard/inquiries (필터/검색/페이징)
// S7 리스트 뷰 — S4 칸반과 동일 데이터의 표 형태 뷰
type Row = {
  id: string;
  customerType: string; // 고객이 접수 시 직접 고른 유형
  aiType: string; // AI가 분석으로 판단한 유형
  urgency: "HIGH" | "NORMAL" | "LOW";
  status: string;
  assignee: string;
  summary: string;
  createdAt: string;
  completedAt: string | null; // 발송완료 시각 (미완료면 null)
};

const rows: Row[] = [
  { id: "INQ-1039", customerType: "기타", aiType: "결제", urgency: "HIGH", status: "담당자배정대기", assignee: "-", summary: "결제 완료, 아이템 미지급", createdAt: "2026-06-18 15:00", completedAt: null },
  { id: "INQ-1038", customerType: "계정", aiType: "계정", urgency: "NORMAL", status: "담당자배정대기", assignee: "-", summary: "로그인 불가", createdAt: "2026-06-18 14:50", completedAt: null },
  { id: "INQ-1035", customerType: "결제", aiType: "결제", urgency: "HIGH", status: "운영자확인중", assignee: "김운영", summary: "중복 결제", createdAt: "2026-06-18 14:30", completedAt: null },
  { id: "INQ-1030", customerType: "결제", aiType: "결제", urgency: "NORMAL", status: "승인완료", assignee: "이운영", summary: "결제 취소 안내", createdAt: "2026-06-18 13:10", completedAt: null },
  { id: "INQ-1021", customerType: "기타", aiType: "기타", urgency: "LOW", status: "발송완료", assignee: "박운영", summary: "처리 완료", createdAt: "2026-06-18 11:05", completedAt: "2026-06-18 11:32" },
];

const th: React.CSSProperties = {
  textAlign: "left",
  padding: "8px 10px",
  borderBottom: `2px solid ${colors.line}`,
  fontSize: 12,
  color: colors.text,
  background: colors.boxAlt,
  whiteSpace: "nowrap",
};
const td: React.CSSProperties = {
  padding: "8px 10px",
  borderBottom: `1px solid #eee`,
  fontSize: 12,
};

export default function ListScreen() {
  return (
    <div>
      <ScreenTitle api="GET /dashboard/inquiries?status=&urgency=&type=&assignee=&keyword=&from=&to=&page=&size=">
        목록 / 리스트 뷰
      </ScreenTitle>

      {/* 보드/리스트 토글 */}
      <Box style={{ marginBottom: 12, display: "flex", alignItems: "center", gap: 8 }}>
        <span style={{ border: `1px solid ${colors.line}`, borderRadius: 4, overflow: "hidden", fontSize: 12 }}>
          <span style={{ padding: "5px 12px", background: "#fff", color: colors.muted, cursor: "pointer" }}>칸반</span>
          <span style={{ padding: "5px 12px", background: colors.primary, color: "#fff", cursor: "pointer", fontWeight: 600 }}>리스트</span>
        </span>
      </Box>

      {/* 필터/검색/기간 바 — 디자인 가이드 Select/Search */}
      <Box style={{ marginBottom: 12, display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
        <Select label="상태" placeholder="전체" />
        <Select label="긴급도" placeholder="전체" />
        <Select label="유형" placeholder="전체" />
        <Select label="담당자" placeholder="전체" />
        <Select label="기간" placeholder="📅 전체 기간" width={140} />
        <div style={{ flex: 1 }} />
        <SearchBox placeholder="문의 ID·내용 검색" />
      </Box>

      {/* 표 */}
      <Box>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th style={th}>문의 ID</th>
              <th style={th}>유형 (고객 → AI)</th>
              <th style={th}>긴급도</th>
              <th style={th}>상태</th>
              <th style={th}>담당자</th>
              <th style={th}>요약</th>
              <th style={th}>접수시각</th>
              <th style={th}>완료시각</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="g-row--clickable">
                <td style={{ ...td, fontFamily: "'DM Sans', monospace", color: colors.primary, fontWeight: 600 }}>{r.id}</td>
                <td style={td}>
                  <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                    <span style={{ color: colors.muted }}>{r.customerType}</span>
                    <span style={{ color: colors.muted }}>→</span>
                    <TypeBadge type={r.aiType} />
                  </span>
                </td>
                <td style={td}>
                  <UrgencyBadge level={r.urgency} />
                </td>
                <td style={td}>
                  <StatusBadge status={r.status} />
                </td>
                <td style={{ ...td, color: r.assignee === "-" ? colors.muted : colors.text }}>{r.assignee}</td>
                <td style={td}>{r.summary}</td>
                <td style={{ ...td, color: colors.muted, fontFamily: "'DM Sans', monospace" }}>{r.createdAt}</td>
                <td style={{ ...td, color: r.completedAt ? colors.text : colors.muted, fontFamily: "'DM Sans', monospace" }}>
                  {r.completedAt ?? "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <Pagination current={1} total={3} totalElements={53} pageSize={20} />
      </Box>
      <div style={{ fontSize: 11, color: colors.muted, marginTop: 10, lineHeight: 1.7 }}>
        * 행 클릭 → 문의 상세(S5) 이동 / 칸반과 동일 데이터<br />
        * "유형 (고객 → AI)": 고객이 접수 시 고른 유형 → AI가 분석으로 판단한 유형 (불일치 시 AI 판단 우선)<br />
        * 완료시각은 발송완료(SENT) 건만 표시, 그 외 "-"
      </div>
    </div>
  );
}
