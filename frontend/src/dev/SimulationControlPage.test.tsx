import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach } from "vitest";
import { SimulationControlPage } from "./SimulationControlPage";
import type { SimulationStatus } from "./simApi";

// simApi 전체를 목 처리
vi.mock("./simApi", () => ({
  startSimulation: vi.fn(),
  pauseSimulation: vi.fn(),
  resumeSimulation: vi.fn(),
  stopSimulation: vi.fn(),
  resetSimulation: vi.fn(),
  fetchSimulationStatus: vi.fn(),
}));

import * as simApi from "./simApi";

function makeStatus(overrides: Partial<SimulationStatus> = {}): SimulationStatus {
  return {
    running: false,
    total: 100,
    sent: 42,
    errors: 2,
    startedAtEpochMs: null,
    elapsedSeconds: 0,
    etaSeconds: 0,
    ratePerMin: 0,
    llmClient: "agentcli",
    paused: false,
    ...overrides,
  };
}

describe("SimulationControlPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(makeStatus());
    vi.mocked(simApi.startSimulation).mockResolvedValue(makeStatus({ running: true }));
    vi.mocked(simApi.pauseSimulation).mockResolvedValue(makeStatus({ running: true, paused: true }));
    vi.mocked(simApi.resumeSimulation).mockResolvedValue(makeStatus({ running: true, paused: false }));
    vi.mocked(simApi.stopSimulation).mockResolvedValue(makeStatus());
    vi.mocked(simApi.resetSimulation).mockResolvedValue(makeStatus());
  });

  it("시작 버튼 클릭 시 startSimulation 이 호출된다", async () => {
    render(<SimulationControlPage />);

    const startBtn = screen.getByRole("button", { name: /시작/i });
    fireEvent.click(startBtn);

    await waitFor(() => {
      expect(simApi.startSimulation).toHaveBeenCalledTimes(1);
    });
  });

  it("sent/total 수치(42 / 100)를 렌더링한다", async () => {
    render(<SimulationControlPage />);

    await waitFor(() => {
      expect(screen.getByText(/42\s*\/\s*100/)).toBeInTheDocument();
    });
  });

  it("실행 중 일시정지 버튼 클릭 시 pauseSimulation 이 호출된다", async () => {
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(makeStatus({ running: true }));
    render(<SimulationControlPage />);

    const pauseBtn = await screen.findByRole("button", { name: /일시정지/i });
    fireEvent.click(pauseBtn);

    await waitFor(() => {
      expect(simApi.pauseSimulation).toHaveBeenCalledTimes(1);
    });
  });

  it("일시정지 상태에서 재개 버튼 클릭 시 resumeSimulation 이 호출된다", async () => {
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(
      makeStatus({ running: true, paused: true }),
    );
    render(<SimulationControlPage />);

    const resumeBtn = await screen.findByRole("button", { name: /재개/i });
    fireEvent.click(resumeBtn);

    await waitFor(() => {
      expect(simApi.resumeSimulation).toHaveBeenCalledTimes(1);
    });
  });
});
