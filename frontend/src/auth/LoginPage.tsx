import { useState, FormEvent } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { login as loginApi, extractErrorMessage } from "../common/api";
import { useAuth } from "../common/AuthContext";

export function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/board";

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!username.trim() || !password) {
      setError("아이디와 비밀번호를 입력하세요.");
      return;
    }
    setSubmitting(true);
    try {
      const res = await loginApi({ username: username.trim(), password });
      login(res.token, res.operator);
      navigate(from, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, "로그인에 실패했습니다."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="center-screen">
      <form
        className="card"
        style={{ width: 360 }}
        onSubmit={handleSubmit}
        data-testid="login-form"
      >
        <h1 style={{ fontSize: 20, marginTop: 0, marginBottom: 4 }}>
          CS 운영자 로그인
        </h1>
        <p style={{ color: "var(--color-muted)", marginTop: 0, marginBottom: 20 }}>
          AI 문의 처리 에이전트
        </p>

        <div className="field">
          <label htmlFor="username">아이디</label>
          <input
            id="username"
            data-testid="login-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            disabled={submitting}
          />
        </div>

        <div className="field">
          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            data-testid="login-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            disabled={submitting}
          />
        </div>

        {error && (
          <div className="error-msg" data-testid="login-error" role="alert">
            {error}
          </div>
        )}

        <button
          type="submit"
          className="primary"
          style={{ width: "100%", marginTop: 8 }}
          disabled={submitting}
          data-testid="login-submit"
        >
          {submitting ? "로그인 중..." : "로그인"}
        </button>

        <div style={{ marginTop: 12, textAlign: "right" }}>
          <button
            type="button"
            className="linklike"
            data-testid="login-forgot"
            onClick={() =>
              setError("비밀번호 찾기는 관리자에게 문의하세요. (재설정 플로우 협의 중)")
            }
          >
            비밀번호 찾기
          </button>
        </div>

        <div
          className="helper"
          style={{ marginTop: 12, lineHeight: 1.6 }}
          data-testid="login-lock-notice"
        >
          * 실패 시 "아이디 또는 비밀번호가 올바르지 않습니다" · 5회 실패 시 계정 잠금
        </div>
      </form>
    </div>
  );
}
