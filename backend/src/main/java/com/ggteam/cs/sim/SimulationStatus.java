package com.ggteam.cs.sim;

/** 시뮬레이션 진행 상태 스냅샷. */
public record SimulationStatus(
        boolean running,
        int total,
        int sent,
        int errors,
        Long startedAtEpochMs,
        long elapsedSeconds,
        long etaSeconds,
        double ratePerMin,
        String llmClient,
        boolean paused) {}
