package com.redis.workshop.platform.controlplane.admin;

import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/admin")
public class AdminObservabilityController {

    private final AdminObservabilityService adminObservabilityService;

    public AdminObservabilityController(AdminObservabilityService adminObservabilityService) {
        this.adminObservabilityService = adminObservabilityService;
    }

    @GetMapping("/overview")
    AdminObservabilityOverviewResponse getOverview() {
        return adminObservabilityService.getOverview();
    }

    @GetMapping("/audit-events")
    List<AuditEventResponse> getRecentAuditEvents(@RequestParam(defaultValue = "25") int limit) {
        return adminObservabilityService.getRecentAuditEvents(Math.max(1, Math.min(limit, 100)));
    }
}
