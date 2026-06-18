import { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { logout as logoutApi } from "./api";

export function AppLayout({ children }: { children: ReactNode }) {
  const { operator, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await logoutApi();
    } catch {
      // 무시: 서버 실패해도 로컬 토큰은 제거
    }
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div>
      <header className="app-header">
        <div className="app-brand">
          <span className="app-logo">G</span>
          <span className="app-brand-name">GRAVITY</span>
        </div>
        <span className="app-title">AI CS 문의 처리 에이전트</span>
        <div style={{ flex: 1 }} />
        <nav>
          <NavLink
            to="/board"
            className={({ isActive }) => (isActive ? "active" : "")}
          >
            칸반 보드
          </NavLink>
          <span className="app-user">{operator?.username ?? "운영자"}</span>
          <button className="ghost" onClick={handleLogout} data-testid="logout-btn">
            로그아웃
          </button>
        </nav>
      </header>
      {children}
    </div>
  );
}
