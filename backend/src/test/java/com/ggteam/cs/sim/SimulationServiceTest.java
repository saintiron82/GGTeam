package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

    private SimulationService newService(InquirySender sender) {
        InquiryScenarioCatalog catalog = new InquiryScenarioCatalog(new ScenarioAssigner());
        return new SimulationService(catalog, sender, new SimProperties(), "mock");
    }

    private void awaitSent(SimulationService svc, int target) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (svc.status().sent() < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    @Test
    void 짧은_윈도우에_전건을_전송한다() throws InterruptedException {
        List<PlannedInquiry> recorded = Collections.synchronizedList(new ArrayList<>());
        SimulationService svc = newService(recorded::add);

        svc.startInternal(3, 60, false);
        awaitSent(svc, 3);

        assertThat(svc.status().sent()).isEqualTo(3);
        assertThat(svc.status().total()).isEqualTo(3);
        assertThat(recorded).hasSize(3);
    }

    @Test
    void stop_후_running은_false() throws InterruptedException {
        SimulationService svc = newService(p -> { });

        SimulationStatus before = svc.startInternal(50, 60_000, false); // 긴 간격
        assertThat(before.running()).isTrue();
        SimulationStatus after = svc.stop();

        assertThat(after.running()).isFalse();
        assertThat(after.sent()).isLessThan(50);
    }

    @Test
    void pause하면_전송이_멈추고_resume하면_남은건을_마저_전송한다() throws InterruptedException {
        List<PlannedInquiry> recorded = Collections.synchronizedList(new ArrayList<>());
        SimulationService svc = newService(recorded::add);

        svc.startInternal(10, 2000, false); // interval=200ms
        awaitSent(svc, 1);                   // 최소 1건 전송될 때까지

        SimulationStatus paused = svc.pause();
        assertThat(paused.paused()).isTrue();
        assertThat(paused.running()).isTrue();   // 일시정지는 running 유지
        Thread.sleep(60);                        // in-flight 정착 대기
        int atPause = svc.status().sent();

        Thread.sleep(600);                       // 일시정지 중에는 추가 전송 없어야(약 3 interval)
        assertThat(svc.status().sent()).isEqualTo(atPause);
        assertThat(svc.status().sent()).isLessThan(10);

        svc.resume();
        assertThat(svc.status().paused()).isFalse();
        awaitSent(svc, 10);

        assertThat(svc.status().sent()).isEqualTo(10);
        assertThat(recorded).hasSize(10);
    }
}
