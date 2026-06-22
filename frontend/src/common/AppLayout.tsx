import { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { logout as logoutApi } from "./api";

const NAV = [
  { to: "/dashboard", label: "대시보드", icon: "▦" },
  { to: "/board", label: "문의 보드", icon: "▤" },
];

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

  const initial = (operator?.username ?? "U").charAt(0).toUpperCase();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-logo" />
          <span>CS Agent</span>
        </div>
        <nav className="sidebar-nav">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-chip">
            <span className="avatar">{initial}</span>
            <div className="user-meta">
              <span className="user-name">{operator?.username ?? "운영자"}</span>
              <span className="user-role">{operator?.role ?? ""}</span>
            </div>
          </div>
          <button onClick={handleLogout} data-testid="logout-btn" style={{ width: "100%" }}>
            로그아웃
          </button>
        </div>
      </aside>
      <main className="app-main">{children}</main>
    </div>
  );
}
