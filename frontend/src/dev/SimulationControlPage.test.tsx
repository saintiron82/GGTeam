import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach } from "vitest";
import { SimulationControlPage } from "./SimulationControlPage";
import type { SimulationStatus } from "./simApi";

// simApi 전체를 목 처리
vi.mock("./simApi", () => ({
  startSimulation: vi.fn(),
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
    ...overrides,
  };
}

describe("SimulationControlPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(makeStatus());
    vi.mocked(simApi.startSimulation).mockResolvedValue(makeStatus({ running: true }));
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
});
