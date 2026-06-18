import React, { useState } from "react";
import ReactDOM from "react-dom/client";
import { colors, elevation } from "./wireframe-kit";
import LoginScreen from "./screens/LoginScreen";
import InquiryFormScreen from "./screens/InquiryFormScreen";
import KanbanScreen from "./screens/KanbanScreen";
import ListScreen from "./screens/ListScreen";
import DetailScreen from "./screens/DetailScreen";
import EditorScreen from "./screens/EditorScreen";

const SCREENS = [
  { key: "kanban", label: "1. 칸반 보드", el: <KanbanScreen /> },
  { key: "list", label: "2. 리스트 뷰", el: <ListScreen /> },
  { key: "detail", label: "3. 문의 상세", el: <DetailScreen /> },
  { key: "editor", label: "4. 답변 편집기", el: <EditorScreen /> },
  { key: "form", label: "5. 고객 문의 폼", el: <InquiryFormScreen /> },
  { key: "login", label: "6. 로그인", el: <LoginScreen /> },
];

const HEADER_H = 56;
const SIDEBAR_W = 220;

function App() {
  const [active, setActive] = useState("kanban");
  const current = SCREENS.find((s) => s.key === active)!;

  return (
    <div style={{ minHeight: "100vh", background: colors.bgPage }}>
      {/* === 탑 헤더 (GRAVITY 레이아웃: height 56px, sticky) === */}
      <header
        style={{
          height: HEADER_H,
          background: colors.bgSurface,
          borderBottom: `1px solid ${colors.line}`,
          display: "flex",
          alignItems: "center",
          padding: "0 24px",
          gap: 16,
          position: "sticky",
          top: 0,
          zIndex: 100,
          boxShadow: elevation.level1,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 8, width: SIDEBAR_W - 24 }}>
          <div
            style={{
              width: 28,
              height: 28,
              background: colors.primary,
              borderRadius: 4,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#fff",
              fontSize: 12,
              fontWeight: 700,
            }}
          >
            G
          </div>
          <span style={{ fontSize: 14, fontWeight: 700, color: colors.primary, letterSpacing: ".02em" }}>
            GRAVITY
          </span>
        </div>
        <span style={{ fontSize: 13, color: colors.muted }}>AI CS 문의 처리 에이전트 · 와이어프레임</span>
        <div style={{ flex: 1 }} />
        <span
          style={{
            fontSize: 11,
            color: colors.primary,
            background: colors.primary50,
            padding: "4px 10px",
            borderRadius: 4,
            fontWeight: 600,
          }}
        >
          기획자 전용 · 포트 5174
        </span>
      </header>

      <div style={{ display: "flex", minHeight: `calc(100vh - ${HEADER_H}px)` }}>
        {/* === 좌측 사이드바 (라이트, Neutral/100) === */}
        <nav
          style={{
            width: SIDEBAR_W,
            background: colors.bgSidebar,
            borderRight: `1px solid ${colors.line}`,
            padding: 8,
            flexShrink: 0,
          }}
        >
          <div
            style={{
              fontSize: 10,
              fontWeight: 700,
              letterSpacing: ".08em",
              textTransform: "uppercase",
              color: "#9AA1AF",
              padding: "8px 8px 4px",
            }}
          >
            화면 목록
          </div>
          {SCREENS.map((s) => {
            const on = active === s.key;
            return (
              <div
                key={s.key}
                onClick={() => setActive(s.key)}
                style={{
                  padding: "9px 12px",
                  marginBottom: 2,
                  borderRadius: 4,
                  fontSize: 13,
                  fontWeight: on ? 600 : 400,
                  cursor: "pointer",
                  color: on ? colors.primary : colors.muted,
                  background: on ? "rgba(5,83,154,.08)" : "transparent",
                }}
              >
                {s.label}
              </div>
            );
          })}
          <div style={{ fontSize: 10, color: "#9AA1AF", marginTop: 20, padding: "0 8px", lineHeight: 1.6 }}>
            개발자 frontend(5173)와 분리됨
          </div>
        </nav>

        {/* === 콘텐츠 영역 (padding 24px 32px, max-width 1200px) === */}
        <main style={{ flex: 1, padding: "24px 32px", overflowX: "auto" }}>
          <div style={{ maxWidth: 1200, margin: "0 auto" }}>{current.el}</div>
        </main>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
