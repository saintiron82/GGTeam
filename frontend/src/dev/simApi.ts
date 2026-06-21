/**
 * 시뮬레이션 제어 API 클라이언트.
 * 절대 URL(http://localhost:8080/api/v1)을 사용하여 MSW를 우회하고
 * 실제 백엔드에 직접 연결합니다.
 */
import axios from "axios";

const SIM_BASE =
  (import.meta.env as Record<string, string>).VITE_SIM_BASE ??
  "http://localhost:8080/api/v1";

const simClient = axios.create({
  baseURL: SIM_BASE,
  headers: { "Content-Type": "application/json" },
});

// ---- 타입 정의 ----

export interface SimulationStatus {
  running: boolean;
  totalCount: number;
  sentCount: number;
  failedCount: number;
  startedAt: number | null;
  elapsedSeconds: number;
  remainingSeconds: number;
  progressRate: number;
  mode: string;
}

export interface StartParams {
  count?: number;
  durationMinutes?: number;
  jitter?: boolean;
}

// ---- API 함수 ----

export async function startSimulation(params?: StartParams): Promise<SimulationStatus> {
  const res = await simClient.post<{ data: SimulationStatus }>("/dev/simulation/start", params ?? {});
  return res.data.data;
}

export async function stopSimulation(): Promise<SimulationStatus> {
  const res = await simClient.post<{ data: SimulationStatus }>("/dev/simulation/stop");
  return res.data.data;
}

export async function resetSimulation(): Promise<SimulationStatus> {
  const res = await simClient.post<{ data: SimulationStatus }>("/dev/simulation/reset");
  return res.data.data;
}

export async function fetchSimulationStatus(): Promise<SimulationStatus> {
  const res = await simClient.get<{ data: SimulationStatus }>("/dev/simulation/status");
  return res.data.data;
}
