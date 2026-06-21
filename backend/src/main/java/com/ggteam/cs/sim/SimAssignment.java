package com.ggteam.cs.sim;

/** userId ↔ 시나리오 배정 (시더·카탈로그 공유 단일 진실원). */
public record SimAssignment(String userId, SimScenario scenario, int index) {}
