package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimPropertiesTest {

    @Test
    void 기본값() {
        SimProperties p = new SimProperties();

        assertThat(p.getCount()).isEqualTo(100);
        assertThat(p.getItemTarget()).isEqualTo(500);
        assertThat(p.getDurationMinutes()).isEqualTo(20);
        assertThat(p.isJitter()).isFalse();
        assertThat(p.isAutoStart()).isFalse();
    }
}
