package com.redis.workshop.platform.controlplane.policy;

import com.redis.workshop.platform.controlplane.release.PilotLaunchRuleService;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.release.WorkshopLaunchDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class DefaultSessionProvisioningPolicy implements SessionProvisioningPolicy {

    private final ReleaseCatalogService releaseCatalogService;
    private final PilotLaunchRuleService pilotLaunchRuleService;
    private final Duration defaultSessionTtl;
    private final String defaultQuotaClass;
    private final String defaultResourceClass;

    public DefaultSessionProvisioningPolicy(
        ReleaseCatalogService releaseCatalogService,
        PilotLaunchRuleService pilotLaunchRuleService,
        @Value("${platform.controlplane.session.defaults.ttl:60m}") Duration defaultSessionTtl,
        @Value("${platform.controlplane.session.defaults.quota-class:standard}") String defaultQuotaClass,
        @Value("${platform.controlplane.session.defaults.resource-class:small}") String defaultResourceClass
    ) {
        this.releaseCatalogService = releaseCatalogService;
        this.pilotLaunchRuleService = pilotLaunchRuleService;
        this.defaultSessionTtl = defaultSessionTtl;
        this.defaultQuotaClass = defaultQuotaClass == null ? "standard" : defaultQuotaClass.trim();
        this.defaultResourceClass = defaultResourceClass == null ? "small" : defaultResourceClass.trim();
    }

    @Override
    public SessionProvisioningProfile resolve(SessionProvisioningRequest request) {
        WorkshopLaunchDefaults launchDefaults = pilotLaunchRuleService.resolveLaunchDefaults(request.workshopId());
        if (!launchDefaults.releaseBacked()) {
            return new SessionProvisioningProfile(defaultQuotaClass, defaultResourceClass, defaultSessionTtl);
        }

        ReleaseCatalogEntry release = releaseCatalogService.findRelease(request.workshopId(), request.releaseVersion())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Release backed session policy is missing catalog data for workshop "
                    + request.workshopId() + " and releaseVersion " + request.releaseVersion()
            ));
        if (release.mode() != request.mode()) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Release backed session policy mode mismatch for workshop "
                    + request.workshopId() + " and releaseVersion " + request.releaseVersion()
            );
        }

        return new SessionProvisioningProfile(
            defaultQuotaClass,
            release.resourceClass(),
            Duration.ofMinutes(release.sessionTtlMinutes())
        );
    }
}
