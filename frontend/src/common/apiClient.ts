// 공통 API 클라이언트 — Axios + JWT 자동 첨부 + 401 처리 (01 §0, 02 §4)
import axios, { AxiosInstance } from "axios";

const TOKEN_KEY = "cs_agent_token";
const OPERATOR_KEY = "cs_agent_operator";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// 기본은 상대경로(/api/v1, dev에선 MSW/프록시). VITE_API_BASE 지정 시 실 백엔드에 직접 연결.
const API_BASE =
  (import.meta.env as Record<string, string | undefined>).VITE_API_BASE ?? "/api/v1";

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
});

// 요청: JWT 자동 첨부 + 운영자 식별 폴백(X-Operator-Id)
apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // 보안필터가 없는 환경(sim/local)에서 운영자 액션(배정/승인 등)을 위해 운영자 id를 헤더로 전달.
  // 운영(보안 ON)에서는 서버가 SecurityContext principal을 우선 사용하므로 이 헤더는 무시된다.
  try {
    const opRaw = localStorage.getItem(OPERATOR_KEY);
    if (opRaw) {
      const op = JSON.parse(opRaw) as { id?: string };
      if (op?.id) {
        config.headers["X-Operator-Id"] = op.id;
      }
    }
  } catch {
    // 운영자 정보 파싱 실패는 무시
  }
  return config;
});

// 응답: 401 시 토큰 제거 후 로그인 페이지로
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);
