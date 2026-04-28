package com.redis.workshop.platform.controlplane.catalog;

import com.redis.workshop.platform.controlplane.policy.CatalogAccessPolicy;
import com.redis.workshop.platform.controlplane.policy.CatalogAccessRequest;
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.release.PilotLaunchRuleService;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.release.WorkshopLaunchDefaults;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.security.PolicyActorMapper;
import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class WorkshopCatalogService {

    private final WorkshopCatalogLoader workshopCatalogLoader;
    private final ReleaseCatalogService releaseCatalogService;
    private final PilotLaunchRuleService pilotLaunchRuleService;
    private final CatalogAccessPolicy catalogAccessPolicy;
    private final CurrentActorProvider currentActorProvider;

    public WorkshopCatalogService(
        WorkshopCatalogLoader workshopCatalogLoader,
        ReleaseCatalogService releaseCatalogService,
        PilotLaunchRuleService pilotLaunchRuleService,
        CatalogAccessPolicy catalogAccessPolicy,
        CurrentActorProvider currentActorProvider
    ) {
        this.workshopCatalogLoader = workshopCatalogLoader;
        this.releaseCatalogService = releaseCatalogService;
        this.pilotLaunchRuleService = pilotLaunchRuleService;
        this.catalogAccessPolicy = catalogAccessPolicy;
        this.currentActorProvider = currentActorProvider;
    }

    public List<CatalogWorkshopResponse> getVisibleWorkshops() {
        CurrentActor actor = currentActorProvider.findCurrentActor().orElse(null);
        return workshopCatalogLoader.loadWorkshops().stream()
            .map(this::toEntry)
            .filter(entry -> isVisible(actor, entry))
            .map(CatalogWorkshopResponse::fromEntry)
            .toList();
    }

    public CatalogWorkshopResponse getVisibleWorkshop(String workshopId) {
        return CatalogWorkshopResponse.fromEntry(getWorkshopEntry(workshopId));
    }

    public WorkshopCatalogEntry getWorkshopEntry(String workshopId) {
        CurrentActor actor = currentActorProvider.findCurrentActor().orElse(null);
        WorkshopCatalogEntry entry = workshopCatalogLoader.loadWorkshops().stream()
            .map(this::toEntry)
            .filter(item -> item.workshopId().equals(workshopId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workshop not found"));

        if (!isVisible(actor, entry)) {
            throw new ResponseStatusException(FORBIDDEN, "Workshop is not visible");
        }
        return entry;
    }

    private boolean isVisible(CurrentActor actor, WorkshopCatalogEntry entry) {
        PolicyDecision decision = catalogAccessPolicy.canRead(new CatalogAccessRequest(
            actor == null ? null : actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor == null ? java.util.Set.<String>of() : actor.authorities(),
            entry.workshopId(),
            entry.publiclyVisible()
        ));
        return decision.allowed();
    }

    private WorkshopCatalogEntry toEntry(WorkshopCatalogLoader.WorkshopCatalogDocument document) {
        WorkshopLaunchDefaults launchDefaults = pilotLaunchRuleService.resolveLaunchDefaults(document.getId());
        List<SessionMode> supportedModes = launchDefaults.releaseBacked()
            ? releaseCatalogService.findSupportedModes(document.getId())
            : List.of(launchDefaults.defaultMode());
        return new WorkshopCatalogEntry(
            document.getId(),
            document.getTitle(),
            document.getDescription(),
            document.getDifficulty(),
            document.getEstimatedMinutes(),
            Optional.ofNullable(document.getUrl()).orElse(""),
            launchDefaults.defaultReleaseVersion(),
            launchDefaults.defaultMode(),
            supportedModes.isEmpty() ? List.of(launchDefaults.defaultMode()) : List.copyOf(supportedModes),
            document.getTopics() == null ? List.of() : List.copyOf(document.getTopics()),
            true
        );
    }
}
