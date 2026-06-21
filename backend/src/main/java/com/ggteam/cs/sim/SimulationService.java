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
 * 드립 엔진. duration/count로 간격을 계산해 한 건씩(단일 틱 재스케줄) 전송한다.
 * 진행 상태(running/paused/total/sent/errors)를 보유하고 start/pause/resume/stop/reset/status를 제공한다.
 *
 * <p>일시정지(pause)는 현재 스케줄러를 취소하고 nextIndex를 보존하며, 재개(resume) 시 남은 건부터
 * 다시 드립한다. 일시정지 동안의 경과는 rate/eta 계산에서 제외한다.
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
    private volatile int nextIndex = 0;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile Long startedAtMs = null;
    private volatile long pausedAccumMs = 0;
    private volatile long pauseStartedMs = 0;
    private volatile long intervalMs = 1;
    private volatile boolean jitter = false;
    private List<PlannedInquiry> plan;
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
        this.plan = catalog.build(count);
        this.total = plan.size();
        this.jitter = jitter;
        this.intervalMs = total > 0 ? Math.max(1, durationMs / total) : Math.max(1, durationMs);
        sent.set(0);
        errors.set(0);
        nextIndex = 0;
        pausedAccumMs = 0;
        paused = false;
        running = total > 0;
        startedAtMs = System.currentTimeMillis();
        if (running) {
            scheduler = newScheduler();
            armTick(0);
        }
        log.info("[SIM] 드립 시작 total={} interval={}ms jitter={}", total, intervalMs, jitter);
        return status();
    }

    /** 일시정지: 다음 전송을 취소하고 진행 위치(nextIndex)를 보존한다. */
    public synchronized SimulationStatus pause() {
        if (running && !paused) {
            paused = true;
            pauseStartedMs = System.currentTimeMillis();
            shutdownScheduler();
            log.info("[SIM] 일시정지 (sent={}/{})", sent.get(), total);
        }
        return status();
    }

    /** 재개: 남은 건부터 드립을 다시 시작한다. */
    public synchronized SimulationStatus resume() {
        if (running && paused) {
            pausedAccumMs += System.currentTimeMillis() - pauseStartedMs;
            paused = false;
            scheduler = newScheduler();
            armTick(0);
            log.info("[SIM] 재개 (sent={}/{})", sent.get(), total);
        }
        return status();
    }

    public synchronized SimulationStatus stop() {
        running = false;
        paused = false;
        shutdownScheduler();
        return status();
    }

    public synchronized SimulationStatus reset() {
        stop();
        sent.set(0);
        errors.set(0);
        total = 0;
        nextIndex = 0;
        startedAtMs = null;
        pausedAccumMs = 0;
        plan = null;
        return status();
    }

    public synchronized SimulationStatus status() {
        long elapsedMs;
        if (startedAtMs == null) {
            elapsedMs = 0;
        } else {
            long end = paused ? pauseStartedMs : System.currentTimeMillis();
            elapsedMs = Math.max(0, end - startedAtMs - pausedAccumMs);
        }
        int done = sent.get();
        double ratePerMin = elapsedMs > 0 ? done * 60_000.0 / elapsedMs : 0.0;
        long eta = ratePerMin > 0 ? (long) ((total - done) / (ratePerMin / 60.0)) : 0;
        return new SimulationStatus(running, total, done, errors.get(),
                startedAtMs, elapsedMs / 1000, Math.max(0, eta), ratePerMin, llmClient, paused);
    }

    /** 단일 틱: 한 건 전송 후 다음 틱을 예약(체인). 정지/일시정지/완료 시 체인을 종료한다. */
    private void tick() {
        PlannedInquiry inq;
        boolean last;
        synchronized (this) {
            if (!running || paused || plan == null || nextIndex >= total) {
                return;
            }
            inq = plan.get(nextIndex);
            nextIndex++;
            last = nextIndex >= total;
        }
        dispatch(inq); // HTTP — 락 밖에서 수행
        synchronized (this) {
            if (!running || paused) {
                return;
            }
            if (last) {
                running = false;
                shutdownScheduler();
            } else {
                armTick(nextTickDelay());
            }
        }
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

    private void armTick(long delayMs) {
        ScheduledExecutorService exec = scheduler;
        if (exec != null) {
            exec.schedule(this::tick, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private long nextTickDelay() {
        if (!jitter) {
            return intervalMs;
        }
        return Math.max(1, intervalMs + ((nextIndex * 9301L + 49297L) % intervalMs) - intervalMs / 2);
    }

    private ScheduledExecutorService newScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sim-drip");
            t.setDaemon(true);
            return t;
        });
    }

    private void shutdownScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
