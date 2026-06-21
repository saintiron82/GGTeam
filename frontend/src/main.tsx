import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { AuthProvider } from "./common/AuthContext";
import { ProtectedRoute } from "./common/ProtectedRoute";
import { LoginPage } from "./auth/LoginPage";
import { InquiryFormPage } from "./customer/InquiryFormPage";
import { KanbanBoardPage } from "./kanban/KanbanBoardPage";
import { InquiryDetailPage } from "./detail/InquiryDetailPage";
import { SimulationControlPage } from "./dev/SimulationControlPage";
import "./common/styles.css";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* 루트 → 운영자 보드. 미인증이면 ProtectedRoute가 /login으로 보냄 */}
          <Route path="/" element={<Navigate to="/board" replace />} />

          {/* 공개: 운영자 로그인 / 고객 문의 접수 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/submit" element={<InquiryFormPage />} />

          {/* 개발용: 트래픽 시뮬레이터 제어판 (sim 프로파일 백엔드 필요) */}
          <Route path="/dev/sim" element={<SimulationControlPage />} />

          {/* 운영자 전용 (인증 필요) */}
          <Route
            path="/board"
            element={
              <ProtectedRoute>
                <KanbanBoardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiry/:id"
            element={
              <ProtectedRoute>
                <InquiryDetailPage />
              </ProtectedRoute>
            }
          />

          {/* 그 외 → 보드(→ 미인증 시 로그인) */}
          <Route path="*" element={<Navigate to="/board" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

async function bootstrap() {
  // MSW 목업 워커 기동 조건:
  //  - 개발 서버(npm run dev), 또는
  //  - VITE_USE_MOCK=true 로 빌드한 경우 (빌드/프리뷰/도커에서도 목업 사용)
  const useMock =
    import.meta.env.DEV || import.meta.env.VITE_USE_MOCK === "true";
  if (useMock) {
    const { startMockWorker } = await import("./mocks/browser");
    await startMockWorker();
  }

  ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
}

bootstrap();
