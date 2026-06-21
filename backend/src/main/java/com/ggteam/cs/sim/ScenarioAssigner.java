package com.ggteam.cs.sim;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 가중치 기반 시나리오 분배기. 결정적(난수 없음)으로 userId↔시나리오를 배정한다.
 *
 * <p>동작 방식:
 * <ol>
 *   <li>각 {@link SimScenario}의 weight(합 100)를 {@code count}로 스케일한 뒤
 *       {@link Math#round}로 정수화하여 덱(deck)에 추가한다.</li>
 *   <li>반올림 결과 덱 크기가 {@code count}보다 <em>작으면</em>
 *       {@link SimScenario#PAID_NOT_DELIVERED}로 부족분을 채운다(padding).</li>
 *   <li>반올림 결과 덱 크기가 {@code count}보다 <em>크면</em>
 *       덱의 말미(ETC 쪽)를 초과분만큼 잘라낸다(trim).</li>
 * </ol>
 * {@code assign(count)}는 항상 정확히 {@code count}개의 결정적 배정을 반환한다.
 * </p>
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
