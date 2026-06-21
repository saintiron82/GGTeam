package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScenarioAssignerTest {

    private final ScenarioAssigner assigner = new ScenarioAssigner();

    @Test
    void count만큼_결정적으로_분배한다() {
        List<SimAssignment> a = assigner.assign(100);

        assertThat(a).hasSize(100);
        assertThat(a.get(0).userId()).isEqualTo("sim-user-0001");
        assertThat(a.get(99).userId()).isEqualTo("sim-user-0100");
        assertThat(a.stream().map(SimAssignment::userId).distinct().count()).isEqualTo(100);
    }

    @Test
    void 가중치대로_분포한다() {
        var counts = assigner.assign(100).stream()
                .collect(Collectors.groupingBy(SimAssignment::scenario, Collectors.counting()));

        assertThat(counts.get(SimScenario.PAID_NOT_DELIVERED)).isEqualTo(25L);
        assertThat(counts.get(SimScenario.DUPLICATE_CHARGE)).isEqualTo(15L);
        assertThat(counts.get(SimScenario.ACCOUNT_ISSUE)).isEqualTo(10L);
        assertThat(counts.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100L);
    }
}
