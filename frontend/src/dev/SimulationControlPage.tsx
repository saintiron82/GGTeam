import { useState, useEffect, useRef } from "react";
import {
  startSimulation,
  pauseSimulation,
  resumeSimulation,
  stopSimulation,
  resetSimulation,
  fetchSimulationStatus,
} from "./simApi";
import type { SimulationStatus } from "./simApi";

const POLL_INTERVAL_MS = 2000;

export function SimulationControlPage() {
  const [status, setStatus] = useState<SimulationStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 시작 파라미터 입력
  const [count, setCount] = useState(50);
  const [durationMinutes, setDurationMinutes] = useState(5);
  const [jitter, setJitter] = useState(true);

  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 상태 조회
  const refreshStatus = async () => {
    try {
      const s = await fetchSimulationStatus();
      setStatus(s);
    } catch (e) {
      // 폴링 중 오류는 무시 (연결 끊김 등)
      console.warn("상태 조회 실패:", e);
    }
  };

  // 2초 폴링
  useEffect(() => {
    refreshStatus();
    pollingRef.current = setInterval(refreshStatus, POLL_INTERVAL_MS);
    return () => {
      if (pollingRef.current !== null) {
        clearInterval(pollingRef.current);
      }
    };
  }, []);

  const handleAction = async (action: () => Promise<SimulationStatus>) => {
    setLoading(true);
    setError(null);
    try {
      const s = await action();
      setStatus(s);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: { message?: string } } } };
      setError(err?.response?.data?.error?.message ?? "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleStart = () =>
    handleAction(() => startSimulation({ count, durationMinutes, jitter }));
  const handlePause = () => handleAction(pauseSimulation);
  const handleResume = () => handleAction(resumeSimulation);
  const handleStop = () => handleAction(stopSimulation);
  const handleReset = () => handleAction(resetSimulation);

  const sent = status?.sent ?? 0;
  const total = status?.total ?? 0;
  const errors = status?.errors ?? 0;
  const running = status?.running ?? false;
  const paused = status?.paused ?? false;
  const elapsedSeconds = status?.elapsedSeconds ?? 0;
  const etaSeconds = status?.etaSeconds ?? 0;
  const ratePerMin = status?.ratePerMin ?? 0;
  const llmClient = status?.llmClient ?? "-";
  const progressRate = total > 0 ? sent / total : 0;

  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif", maxWidth: 640 }}>
      <h1 style={{ marginBottom: "1.5rem" }}>트래픽 시뮬레이터 제어판</h1>

      {/* 시작 파라미터 입력 */}
      <div
        style={{
          display: "flex",
          gap: "1rem",
          flexWrap: "wrap",
          alignItems: "flex-end",
          marginBottom: "1.5rem",
        }}
      >
        <label style={{ display: "flex", flexDirection: "column", fontSize: "0.8rem", color: "#555" }}>
          건수(count)
          <input
            type="number"
            value={count}
            onChange={(e) => setCount(Number(e.target.value))}
            style={inputStyle}
          />
        </label>
        <label style={{ display: "flex", flexDirection: "column", fontSize: "0.8rem", color: "#555" }}>
          소요(durationMinutes)
          <input
            type="number"
            value={durationMinutes}
            onChange={(e) => setDurationMinutes(Number(e.target.value))}
            style={inputStyle}
          />
        </label>
        <label style={{ display: "flex", alignItems: "center", gap: "0.4rem", fontSize: "0.9rem", color: "#555" }}>
          <input
            type="checkbox"
            checked={jitter}
            onChange={(e) => setJitter(e.target.checked)}
          />
          jitter
        </label>
      </div>

      {/* 상태 카드 */}
      <div
        style={{
          background: "#f5f5f5",
          borderRadius: 8,
          padding: "1rem 1.5rem",
          marginBottom: "1.5rem",
        }}
      >
        <div style={{ display: "flex", gap: "1.5rem", flexWrap: "wrap" }}>
          <Stat label="running" value={paused ? "일시정지" : running ? "실행 중" : "정지"} />
          <Stat label="전송" value={`${sent} / ${total}`} />
          <Stat label="errors" value={String(errors)} />
          <Stat label="경과(s)" value={String(elapsedSeconds)} />
          <Stat label="남음(s)" value={String(etaSeconds)} />
          <Stat label="분당" value={ratePerMin.toFixed(1)} />
          <Stat label="LLM" value={llmClient} />
        </div>

        {/* 프로그레스 바 */}
        <div
          style={{
            marginTop: "0.75rem",
            background: "#ddd",
            borderRadius: 4,
            height: 8,
          }}
        >
          <div
            style={{
              background: paused ? "#ff9800" : running ? "#4caf50" : "#90caf9",
              width: `${Math.min(progressRate * 100, 100)}%`,
              height: "100%",
              borderRadius: 4,
              transition: "width 0.3s ease",
            }}
          />
        </div>
      </div>

      {/* 제어 버튼 */}
      <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
        <button
          onClick={handleStart}
          disabled={loading || running}
          style={btnStyle("#4caf50", loading || running)}
        >
          시작
        </button>
        <button
          onClick={paused ? handleResume : handlePause}
          disabled={loading || !running}
          style={btnStyle(paused ? "#ff9800" : "#ffb300", loading || !running)}
        >
          {paused ? "재개" : "일시정지"}
        </button>
        <button
          onClick={handleStop}
          disabled={loading || !running}
          style={btnStyle("#f44336", loading || !running)}
        >
          정지
        </button>
        <button
          onClick={handleReset}
          disabled={loading || running}
          style={btnStyle("#9e9e9e", loading || running)}
        >
          리셋
        </button>
      </div>

      {/* 오류 메시지 */}
      {error && (
        <div
          style={{
            marginTop: "1rem",
            color: "#c62828",
            background: "#ffebee",
            borderRadius: 6,
            padding: "0.5rem 1rem",
          }}
        >
          {error}
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: "0.75rem", color: "#777", marginBottom: 2 }}>{label}</div>
      <div style={{ fontWeight: 600, fontSize: "1.1rem" }}>{value}</div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  marginTop: 4,
  padding: "0.35rem 0.5rem",
  border: "1px solid #ccc",
  borderRadius: 4,
  width: 120,
  fontSize: "0.95rem",
};

function btnStyle(color: string, disabled: boolean): React.CSSProperties {
  return {
    background: disabled ? "#e0e0e0" : color,
    color: disabled ? "#9e9e9e" : "#fff",
    border: "none",
    borderRadius: 6,
    padding: "0.5rem 1.25rem",
    fontSize: "1rem",
    cursor: disabled ? "not-allowed" : "pointer",
    transition: "background 0.2s",
  };
}
