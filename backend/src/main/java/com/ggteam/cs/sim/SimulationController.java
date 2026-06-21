package com.ggteam.cs.sim;

import com.ggteam.cs.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시뮬레이션 제어 API (sim 프로파일 전용).
 * POST start/stop/reset, GET status.
 */
@RestController
@RequestMapping("/api/v1/dev/simulation")
@Profile("sim")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SimulationStatus>> start(
            @RequestBody(required = false) StartRequest req) {
        StartRequest r = req != null ? req : new StartRequest(null, null, null);
        return ResponseEntity.ok(ApiResponse.of(service.start(r.count(), r.durationMinutes(), r.jitter())));
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<SimulationStatus>> stop() {
        return ResponseEntity.ok(ApiResponse.of(service.stop()));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<SimulationStatus>> reset() {
        return ResponseEntity.ok(ApiResponse.of(service.reset()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SimulationStatus>> status() {
        return ResponseEntity.ok(ApiResponse.of(service.status()));
    }

    public record StartRequest(Integer count, Integer durationMinutes, Boolean jitter) {}
}
