package com.ggteam.cs.dashboard;

import com.ggteam.cs.common.ApiResponse;
import com.ggteam.cs.dashboard.dto.OperatorStat;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자별 처리 현황 API (US-27 확장). 담당: 백엔드 C.
 * GET /api/v1/dashboard/operator-stats — 운영자별 담당/승인/액션 수.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class OperatorStatsController {

    private final OperatorStatsService service;

    public OperatorStatsController(OperatorStatsService service) {
        this.service = service;
    }

    @GetMapping("/operator-stats")
    public ResponseEntity<ApiResponse<List<OperatorStat>>> operatorStats() {
        return ResponseEntity.ok(ApiResponse.of(service.compute()));
    }
}
