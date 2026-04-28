package com.redis.workshop.platform.controlplane.release;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/admin/releases")
public class ReleaseAdminObservabilityController {

    private final ReleaseAdminObservabilityService releaseAdminObservabilityService;

    public ReleaseAdminObservabilityController(ReleaseAdminObservabilityService releaseAdminObservabilityService) {
        this.releaseAdminObservabilityService = releaseAdminObservabilityService;
    }

    @GetMapping("/overview")
    ReleaseAdminOverviewResponse getOverview() {
        return releaseAdminObservabilityService.getOverview();
    }
}
