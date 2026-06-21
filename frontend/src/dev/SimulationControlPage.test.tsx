import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach } from "vitest";
import { SimulationControlPage } from "./SimulationControlPage";

// simApi 전체를 목 처리
vi.mock("./simApi", () => ({
  startSimulation: vi.fn(),
  stopSimulation: vi.fn(),
  resetSimulation: vi.fn(),
  fetchSimulationStatus: vi.fn(),
}));

import * as simApi from "./simApi";

const mockStatus = {
  running: false,
  totalCount: 50,
  sentCount: 20,
  failedCount: 2,
  startedAt: null,
  elapsedSeconds: 0,
  remainingSeconds: 0,
  progressRate: 0,
  mode: "manual",
};

describe("SimulationControlPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(mockStatus);
    vi.mocked(simApi.startSimulation).mockResolvedValue({ ...mockStatus, running: true });
    vi.mocked(simApi.stopSimulation).mockResolvedValue(mockStatus);
    vi.mocked(simApi.resetSimulation).mockResolvedValue(mockStatus);
  });

  it("시작 버튼 클릭 시 startSimulation 이 호출된다", async () => {
    render(<SimulationControlPage />);

    const startBtn = screen.getByRole("button", { name: /시작/i });
    fireEvent.click(startBtn);

    await waitFor(() => {
      expect(simApi.startSimulation).toHaveBeenCalledTimes(1);
    });
  });

  it("sent/total 수치를 렌더링한다", async () => {
    render(<SimulationControlPage />);

    await waitFor(() => {
      expect(screen.getByText(/20/)).toBeInTheDocument();
      expect(screen.getByText(/50/)).toBeInTheDocument();
    });
  });
});
