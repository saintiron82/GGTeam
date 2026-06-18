import React from "react";
import ReactDOM from "react-dom/client";

// 라우팅/페이지 컴포넌트는 각 담당자가 구현 (04-frontend-assignment 참조).
// 이 파일은 공통 진입 골격이다.
function App() {
  return (
    <div data-testid="app-root">
      <h1>AI CS 문의 처리 에이전트</h1>
      <p>프론트엔드 스캐폴드 — 각 화면은 담당자가 구현합니다.</p>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
