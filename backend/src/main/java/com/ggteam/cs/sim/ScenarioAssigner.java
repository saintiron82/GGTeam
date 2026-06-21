package com.ggteam.cs.sim;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 가중치 기반 시나리오 분배기. 결정적(난수 없음)으로 userId↔시나리오를 배정한다.
 * weight 합 100을 count로 스케일하고 반올림 오차는 PAID_NOT_DELIVERED로 보정한다.
 */
@Component
public class ScenarioAssigner {

    public List<SimAssignment> assign(int count) {
        List<SimScenario> deck = new ArrayList<>();
        for (SimScenario s : SimScenario.values()) {
            int n = Math.round(s.weight() * count / 100f);
            for (int i = 0; i < n; i++) {
                deck.add(s);
            }
        }
        while (deck.size() < count) {
            deck.add(SimScenario.PAID_NOT_DELIVERED);
        }
        while (deck.size() > count) {
            deck.remove(deck.size() - 1);
        }

        List<SimAssignment> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new SimAssignment(String.format("sim-user-%04d", i + 1), deck.get(i), i));
        }
        return out;
    }
}
