package com.ggteam.cs.dashboard;

import com.ggteam.cs.common.ApiResponse;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.dashboard.dto.InquiryCard;
import com.ggteam.cs.dashboard.dto.NotificationCounts;
import com.ggteam.cs.dashboard.dto.PageResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API (01-api-contract §4). <b>담당: 백엔드 C.</b>
 *
 * <p>모든 엔드포인트는 인증 필요(BR-37, 보안 설정은 백엔드 A).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final int MAX_PAGE_SIZE = 100;

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 칸반 보드: 상태별 카드 그룹 (US-20). */
    @GetMapping("/board")
    public ResponseEntity<ApiResponse<Map<String, List<InquiryCard>>>> board() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getBoard()));
    }

    /** 목록 조회: 필터/검색/페이징 (US-22). */
    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<InquiryCard>> inquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Urgency urgency,
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<InquiryCard> result =
                dashboardService.search(status, urgency, type, assignee, keyword, from, to, pageable);
        return ResponseEntity.ok(result);
    }

    /** 알림 집계: 미배정/긴급 카운트 (US-12). */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationCounts>> notifications() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getNotifications()));
    }

    /** 통계: 상태별/유형별 분포 + 핵심 지표 (US-27). */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<com.ggteam.cs.dashboard.dto.DashboardStats>> stats() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getStats()));
    }
}
