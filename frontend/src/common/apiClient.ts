// 공통 API 클라이언트 — Axios + JWT 자동 첨부 + 401 처리 (01 §0, 02 §4)
import axios, { AxiosInstance } from "axios";

const TOKEN_KEY = "cs_agent_token";

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

// 요청: JWT 자동 첨부
apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
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
