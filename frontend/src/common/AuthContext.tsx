import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { getToken, setToken, clearToken } from "./apiClient";

interface Operator {
  id: string;
  username: string;
  role: "OPERATOR" | "ADMIN";
}

interface AuthState {
  token: string | null;
  operator: Operator | null;
  login: (token: string, operator: Operator) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(getToken);
  const [operator, setOperator] = useState<Operator | null>(() => {
    const stored = localStorage.getItem("cs_agent_operator");
    return stored ? JSON.parse(stored) : null;
  });

  const login = (newToken: string, op: Operator) => {
    setToken(newToken);
    setTokenState(newToken);
    setOperator(op);
    localStorage.setItem("cs_agent_operator", JSON.stringify(op));
  };

  const logout = () => {
    clearToken();
    setTokenState(null);
    setOperator(null);
    localStorage.removeItem("cs_agent_operator");
  };

  useEffect(() => {
    const handleStorage = () => {
      const t = getToken();
      setTokenState(t);
      if (!t) setOperator(null);
    };
    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, []);

  return (
    <AuthContext.Provider value={{ token, operator, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
