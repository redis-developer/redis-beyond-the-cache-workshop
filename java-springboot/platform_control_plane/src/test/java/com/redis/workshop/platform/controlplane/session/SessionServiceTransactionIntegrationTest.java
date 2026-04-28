package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogEntry;
import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogService;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.policy.SessionAccessPolicy;
import com.redis.workshop.platform.controlplane.policy.SessionProvisioningPolicy;
import com.redis.workshop.platform.controlplane.policy.SessionProvisioningProfile;
import com.redis.workshop.platform.controlplane.release.PilotLaunchRuleService;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.release.ReleaseImageReferences;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;
import com.redis.workshop.platform.controlplane.security.PlatformAuthorities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:platform-control-plane-session-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionServiceTransactionIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PlatformSessionRecordRepository sessionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private WorkshopCatalogService workshopCatalogService;

    @MockitoBean
    private SessionAccessPolicy sessionAccessPolicy;

    @MockitoBean
    private SessionProvisioningPolicy sessionProvisioningPolicy;

    @MockitoBean
    private CurrentActorProvider currentActorProvider;

    @MockitoBean
    private PilotLaunchRuleService pilotLaunchRuleService;

    @MockitoBean
    private ReleaseCatalogService releaseCatalogService;

    @MockitoBean
    private SessionRuntimeLifecyclePort sessionRuntimeLifecyclePort;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        sessionRepository.deleteAll();

        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );

        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(new WorkshopCatalogEntry(
            "1_session_management",
            "Distributed Session Management",
            "Learn Redis backed sessions",
            "Beginner",
            30,
            "/workshop/session-management/",
            "current",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Sessions"),
            true
        ));
        when(sessionAccessPolicy.canCreate(any())).thenReturn(PolicyDecision.allow("self_service"));
        when(sessionAccessPolicy.canTerminate(any())).thenReturn(PolicyDecision.allow("owner_match"));
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(
            new SessionProvisioningProfile("standard", "small", Duration.ofMinutes(60))
        );
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "current"))
            .thenReturn("current");
        when(releaseCatalogService.findRelease("1_session_management", "current"))
            .thenReturn(Optional.of(release()));
    }

    @Test
    void defersLaunchDispatchUntilTransactionCommit() {
        String[] sessionId = new String[1];

        transactionTemplate.executeWithoutResult(status -> {
            SessionResponse response = sessionService.createSession(new CreateSessionRequest(
                "1_session_management",
                null,
                null
            ));
            sessionId[0] = response.sessionId();

            assertThat(response.state()).isEqualTo(SessionState.ADMITTED);
            assertThat(sessionRepository.findById(sessionId[0])).isPresent();
            verify(sessionRuntimeLifecyclePort, never()).requestLaunch(any());
        });

        verify(sessionRuntimeLifecyclePort).requestLaunch(argThat(request ->
            request.sessionId().equals(sessionId[0])
                && request.ownerUserId().equals("learner-1")
                && request.workshopId().equals("1_session_management")
                && request.artifacts().containsKey("frontend")
                && request.artifacts().containsKey("combined")
        ));
    }

    @Test
    void defersTerminationDispatchUntilTransactionCommit() {
        PlatformSessionRecord record = readySession("sess-terminate");
        sessionRepository.save(record);

        transactionTemplate.executeWithoutResult(status -> {
            SessionResponse response = sessionService.terminateSession("sess-terminate");

            assertThat(response.state()).isEqualTo(SessionState.TERMINATING);
            assertThat(sessionRepository.findById("sess-terminate")).get()
                .extracting(PlatformSessionRecord::getState)
                .isEqualTo(PlatformSessionState.TERMINATING);
            verify(sessionRuntimeLifecyclePort, never()).requestTermination(any());
        });

        verify(sessionRuntimeLifecyclePort).requestTermination(argThat(request ->
            request.sessionId().equals("sess-terminate")
                && request.ownerUserId().equals("learner-1")
                && request.runtimeRef().equals(cloudRunRuntimeRef("sess-terminate"))
                && request.reason().equals("user_requested")
        ));
    }

    @Test
    void defersTerminationRetryDispatchUntilTransactionCommit() {
        PlatformSessionRecord record = readySession("sess-expired");
        record.setExpiresAt(Instant.parse("2026-04-21T09:00:00Z"));
        sessionRepository.save(record);

        transactionTemplate.executeWithoutResult(status -> {
            int reconciled = sessionService.reconcileExpiredSessions(Instant.parse("2026-04-21T10:00:00Z"));

            assertThat(reconciled).isEqualTo(1);
            assertThat(sessionRepository.findById("sess-expired")).get()
                .extracting(PlatformSessionRecord::getState)
                .isEqualTo(PlatformSessionState.EXPIRED);
            verify(sessionRuntimeLifecyclePort, never()).requestTerminationRetry(any());
        });

        verify(sessionRuntimeLifecyclePort).requestTerminationRetry(argThat(request ->
            request.sessionId().equals("sess-expired")
                && request.ownerUserId().equals("learner-1")
                && request.runtimeRef().equals(cloudRunRuntimeRef("sess-expired"))
                && request.reason().equals("expired")
        ));
    }

    private PlatformSessionRecord readySession(String sessionId) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(PlatformSessionState.READY);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2099-04-21T11:00:00Z"));
        record.setLastActivityAt(Instant.parse("2026-04-21T10:05:00Z"));
        record.setRouteType(RouteType.PATH);
        record.setInternalRuntimeRef(cloudRunRuntimeRef(sessionId));
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }

    private ReleaseCatalogEntry release() {
        return new ReleaseCatalogEntry(
            "session-management-current",
            "1_session_management",
            "current",
            SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                "registry.example.com/workshops/session-management-runner@sha256:9999999999999999999999999999999999999999999999999999999999999999",
                null
            ),
            "small",
            60,
            List.of("redis")
        );
    }

    private String cloudRunRuntimeRef(String sessionId) {
        return "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-" + sessionId;
    }
}
