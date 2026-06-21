package com.ggteam.cs.sim;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 드립 엔진. duration/count로 간격을 계산해 ScheduledExecutorService로 한 건씩 전송한다.
 * 진행 상태(running/total/sent/errors)를 보유하고 start/stop/reset/status를 제공한다.
 */
@Service
@Profile("sim")
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final InquiryScenarioCatalog catalog;
    private final InquirySender sender;
    private final SimProperties props;
    private final String llmClient;

    private final AtomicInteger sent = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private volatile int total = 0;
    private volatile boolean running = false;
    private volatile Long startedAtMs = null;
    private ScheduledExecutorService scheduler;

    public SimulationService(InquiryScenarioCatalog catalog, InquirySender sender, SimProperties props,
                             @Value("${app.ai.llm-client:agentcli}") String llmClient) {
        this.catalog = catalog;
        this.sender = sender;
        this.props = props;
        this.llmClient = llmClient;
    }

    public synchronized SimulationStatus start(Integer count, Integer durationMinutes, Boolean jitter) {
        int n = count != null ? count : props.getCount();
        long durationMs = (durationMinutes != null ? durationMinutes : props.getDurationMinutes()) * 60_000L;
        boolean useJitter = jitter != null ? jitter : props.isJitter();
        return startInternal(n, durationMs, useJitter);
    }

    synchronized SimulationStatus startInternal(int count, long durationMs, boolean jitter) {
        if (running) {
            return status();
        }
        List<PlannedInquiry> plan = catalog.build(count);
        total = plan.size();
        sent.set(0);
        errors.set(0);
        running = true;
        startedAtMs = System.currentTimeMillis();

        long interval = total > 0 ? Math.max(1, durationMs / total) : durationMs;
        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sim-drip");
            t.setDaemon(true);
            return t;
        });
        this.scheduler = exec;

        for (int i = 0; i < total; i++) {
            final PlannedInquiry inq = plan.get(i);
            long base = i * interval;
            long delay = jitter ? Math.max(0, base + ((i * 9301L + 49297L) % interval) - interval / 2) : base;
            exec.schedule(() -> dispatch(inq), delay, TimeUnit.MILLISECONDS);
        }
        // 자연 완료 표시 + executor 종료. 단, 그 사이 stop()/start()로 새 실행이 시작됐다면(this.scheduler != exec)
        // 이 stale 완료 태스크가 새 실행의 running을 끄지 않도록 가드한다(교차 실행 플립 방지).
        exec.schedule(() -> {
            synchronized (this) {
                if (this.scheduler == exec) {
                    running = false;
                }
            }
            exec.shutdown();
        }, (long) total * interval + 50, TimeUnit.MILLISECONDS);

        log.info("[SIM] 드립 시작 total={} interval={}ms jitter={}", total, interval, jitter);
        return status();
    }

    private void dispatch(PlannedInquiry inq) {
        try {
            sender.send(inq);
            sent.incrementAndGet();
        } catch (Exception e) {
            errors.incrementAndGet();
            log.warn("[SIM] 전송 실패 {}: {}", inq.userId(), e.getMessage());
        }
    }

    public synchronized SimulationStatus stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        return status();
    }

    public synchronized SimulationStatus reset() {
        stop();
        sent.set(0);
        errors.set(0);
        total = 0;
        startedAtMs = null;
        return status();
    }

    public synchronized SimulationStatus status() {
        long elapsed = startedAtMs == null ? 0 : Math.max(0, (System.currentTimeMillis() - startedAtMs) / 1000);
        int done = sent.get();
        double ratePerMin = elapsed > 0 ? done * 60.0 / elapsed : 0.0;
        long eta = ratePerMin > 0 ? (long) ((total - done) / (ratePerMin / 60.0)) : 0;
        return new SimulationStatus(running, total, done, errors.get(),
                startedAtMs, elapsed, Math.max(0, eta), ratePerMin, llmClient);
    }
}
