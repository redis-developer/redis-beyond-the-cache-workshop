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
import com.redis.workshop.platform.controlplane.policy.DefaultSessionAccessPolicy;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private PlatformSessionRecordRepository sessionRepository;

    @Mock
    private WorkshopCatalogService workshopCatalogService;

    @Mock
    private SessionAccessPolicy sessionAccessPolicy;

    @Mock
    private SessionProvisioningPolicy sessionProvisioningPolicy;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private PilotLaunchRuleService pilotLaunchRuleService;

    @Mock
    private ReleaseCatalogService releaseCatalogService;

    @Mock
    private SessionRuntimeLifecyclePort sessionRuntimeLifecyclePort;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
            sessionRepository,
            workshopCatalogService,
            sessionAccessPolicy,
            sessionProvisioningPolicy,
            currentActorProvider,
            pilotLaunchRuleService,
            releaseCatalogService,
            sessionRuntimeLifecyclePort
        );
        lenient().when(releaseCatalogService.findRelease(anyString(), anyString()))
            .thenAnswer(invocation -> Optional.of(release(
                invocation.getArgument(0),
                invocation.getArgument(1),
                SessionMode.LAB
            )));
    }

    @Test
    void createsSessionsAsRequestedThenAdmittedAndRequestsLaunch() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        List<PlatformSessionState> savedStates = new ArrayList<>();
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
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "current"))
            .thenReturn("current");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        doAnswer(invocation -> {
            PlatformSessionRecord record = invocation.getArgument(0);
            savedStates.add(record.getState());
            return record;
        }).when(sessionRepository).save(any(PlatformSessionRecord.class));

        SessionResponse response = sessionService.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        ));

        assertThat(savedStates).containsExactly(PlatformSessionState.REQUESTED, PlatformSessionState.ADMITTED);
        assertThat(response.state()).isEqualTo(SessionState.ADMITTED);
        verify(sessionRuntimeLifecyclePort).requestLaunch(any(SessionLaunchRequest.class));
    }

    @Test
    void launchRequestCarriesReleaseArtifactsAndPolicies() {
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
            "2026.04.1",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Sessions"),
            true
        ));
        when(sessionAccessPolicy.canCreate(any())).thenReturn(PolicyDecision.allow("self_service"));
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "2026.04.1"))
            .thenReturn("2026.04.1");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(releaseCatalogService.findRelease("1_session_management", "2026.04.1"))
            .thenReturn(Optional.of(release("1_session_management", "2026.04.1", SessionMode.LAB)));

        sessionService.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        ));

        verify(sessionRuntimeLifecyclePort).requestLaunch(argThat(request ->
            request.releaseId().equals("1_session_management-2026.04.1")
                && request.artifacts().containsKey("frontend")
                && request.artifacts().containsKey("backend")
                && request.artifacts().containsKey("combined")
                && request.resourcePolicy().resourceClass().equals("small")
                && request.workspacePolicy().mountPath().equals("/workshop-sources")
                && request.workspacePolicy().writable()
        ));
    }

    @Test
    void failsClosedWhenReleaseArtifactsAreMissing() {
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
            "2026.04.1",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Sessions"),
            true
        ));
        when(sessionAccessPolicy.canCreate(any())).thenReturn(PolicyDecision.allow("self_service"));
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "2026.04.1"))
            .thenReturn("2026.04.1");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        when(releaseCatalogService.findRelease("1_session_management", "2026.04.1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("500 INTERNAL_SERVER_ERROR");

        verify(sessionRepository, never()).save(any());
        verify(sessionRuntimeLifecyclePort, never()).requestLaunch(any());
    }

    @Test
    void usesPilotLaunchRuleToResolveExplicitReleaseVersion() {
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
            "2026.04.1",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Sessions"),
            true
        ));
        when(sessionAccessPolicy.canCreate(any())).thenReturn(PolicyDecision.allow("self_service"));
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", "2026.04.1", "2026.04.1"))
            .thenReturn("2026.04.1");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            "2026.04.1"
        ));

        assertThat(response.releaseVersion()).isEqualTo("2026.04.1");
        verify(pilotLaunchRuleService).resolveRequestedReleaseVersion("1_session_management", "2026.04.1", "2026.04.1");
    }

    @Test
    void launchesRemainingWorkshopUsingReleaseBackedDefaultVersion() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(workshopCatalogService.getWorkshopEntry("4_agent_memory")).thenReturn(new WorkshopCatalogEntry(
            "4_agent_memory",
            "Agent Memory",
            "Build memory aware agents",
            "Intermediate",
            40,
            "/workshop/agent-memory/",
            "2026.04.1",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Agents", "Redis"),
            true
        ));
        when(sessionAccessPolicy.canCreate(any())).thenReturn(PolicyDecision.allow("self_service"));
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("4_agent_memory", null, "2026.04.1"))
            .thenReturn("2026.04.1");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.createSession(new CreateSessionRequest(
            "4_agent_memory",
            null,
            null
        ));

        assertThat(response.releaseVersion()).isEqualTo("2026.04.1");
        verify(sessionRuntimeLifecyclePort).requestLaunch(argThat(request ->
            request.workshopId().equals("4_agent_memory")
                && request.releaseVersion().equals("2026.04.1")
        ));
    }

    @Test
    void rejectsCreatingASecondActiveSessionForTheSameWorkshop() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord existingSession = sessionRecord("sess-duplicate", PlatformSessionState.READY);
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
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "current"))
            .thenReturn("current");
        when(sessionRepository.findFirstByOwnerUserIdAndWorkshopIdAndStateInOrderByCreatedAtDesc(
            "learner-1",
            "1_session_management",
            java.util.EnumSet.of(
                PlatformSessionState.REQUESTED,
                PlatformSessionState.ADMITTED,
                PlatformSessionState.PROVISIONING,
                PlatformSessionState.INITIALIZING,
                PlatformSessionState.READY,
                PlatformSessionState.DEGRADED,
                PlatformSessionState.TERMINATING
            )
        )).thenReturn(Optional.of(existingSession));

        assertThatThrownBy(() -> sessionService.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        )))
            .isInstanceOf(org.springframework.web.ErrorResponseException.class)
            .hasMessageContaining("409 CONFLICT");

        verify(sessionRepository, never()).save(any());
        verify(sessionRuntimeLifecyclePort, never()).requestLaunch(any(SessionLaunchRequest.class));
    }

    @Test
    void allowsDifferentEmailLearnersToCreateSessionsForTheSameWorkshop() {
        SessionService service = sessionServiceWithDefaultPolicy();
        CurrentActor alice = learnerActor("alice@example.com");
        CurrentActor bob = learnerActor("bob@example.com");
        List<String> savedOwnerIds = new ArrayList<>();
        when(currentActorProvider.requireCurrentActor()).thenReturn(alice, bob);
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(workshopEntry());
        when(pilotLaunchRuleService.resolveRequestedReleaseVersion("1_session_management", null, "current"))
            .thenReturn("current");
        when(sessionProvisioningPolicy.resolve(any())).thenReturn(defaultProvisioningProfile());
        when(sessionRepository.findFirstByOwnerUserIdAndWorkshopIdAndStateInOrderByCreatedAtDesc(
            anyString(),
            anyString(),
            any()
        )).thenReturn(Optional.empty());
        when(sessionRepository.save(any(PlatformSessionRecord.class))).thenAnswer(invocation -> {
            PlatformSessionRecord record = invocation.getArgument(0);
            savedOwnerIds.add(record.getOwnerUserId());
            return record;
        });

        SessionResponse aliceSession = service.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        ));
        SessionResponse bobSession = service.createSession(new CreateSessionRequest(
            "1_session_management",
            null,
            null
        ));

        assertThat(aliceSession.workshopId()).isEqualTo("1_session_management");
        assertThat(bobSession.workshopId()).isEqualTo("1_session_management");
        assertThat(aliceSession.sessionId()).isNotEqualTo(bobSession.sessionId());
        assertThat(savedOwnerIds)
            .containsExactly("alice@example.com", "alice@example.com", "bob@example.com", "bob@example.com");
        ArgumentCaptor<SessionLaunchRequest> launchRequest = ArgumentCaptor.forClass(SessionLaunchRequest.class);
        verify(sessionRuntimeLifecyclePort, times(2)).requestLaunch(launchRequest.capture());
        assertThat(launchRequest.getAllValues())
            .extracting(SessionLaunchRequest::ownerUserId)
            .containsExactly("alice@example.com", "bob@example.com");
    }

    @Test
    void deniesOtherEmailFromReadingRestartingOrTerminatingOwnedSession() {
        SessionService service = sessionServiceWithDefaultPolicy();
        CurrentActor bob = learnerActor("bob@example.com");
        PlatformSessionRecord readable = sessionRecord("sess-read", PlatformSessionState.READY);
        PlatformSessionRecord restartable = sessionRecord("sess-restart", PlatformSessionState.READY);
        PlatformSessionRecord terminable = sessionRecord("sess-terminate", PlatformSessionState.READY);
        readable.setOwnerUserId("alice@example.com");
        restartable.setOwnerUserId("alice@example.com");
        terminable.setOwnerUserId("alice@example.com");
        when(currentActorProvider.requireCurrentActor()).thenReturn(bob, bob, bob);
        when(sessionRepository.findById("sess-read")).thenReturn(Optional.of(readable));
        when(sessionRepository.findById("sess-restart")).thenReturn(Optional.of(restartable));
        when(sessionRepository.findById("sess-terminate")).thenReturn(Optional.of(terminable));

        assertThatThrownBy(() -> service.getSession("sess-read"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("session_read_not_authorized");
        assertThatThrownBy(() -> service.restartSession("sess-restart", new RestartSessionRequest(false)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("session_restart_not_authorized");
        assertThatThrownBy(() -> service.terminateSession("sess-terminate"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("session_terminate_not_authorized");

        verify(sessionRepository, never()).save(any());
        verify(sessionRuntimeLifecyclePort, never()).requestRestart(any());
        verify(sessionRuntimeLifecyclePort, never()).requestTermination(any());
    }

    @Test
    void deniesSessionReadsWhenPolicyRejectsOwnership() {
        CurrentActor learner = new CurrentActor(
            "learner-2",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(sessionRecord("sess-001", PlatformSessionState.READY)));
        when(sessionAccessPolicy.canRead(any())).thenReturn(PolicyDecision.deny("session_read_not_authorized"));

        assertThatThrownBy(() -> sessionService.getSession("sess-001"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void returnsShellContractForLocalSessionRoute() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-local", PlatformSessionState.READY);
        record.setPublicEntryUrl("/workshop/session-management/");
        record.setRuntimeStatus(SessionRuntimeStatuses.READY);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-local")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("owner_match"));

        SessionShellResponse response = sessionService.getSessionShell("sess-local");

        assertThat(response.sessionId()).isEqualTo("sess-local");
        assertThat(response.workshopId()).isEqualTo("1_session_management");
        assertThat(response.learnerAppUrl()).isEqualTo("/workshop/session-management/");
        assertThat(response.redisInsightUrl()).isEqualTo("/workshop/session-management/redis-insight/");
        assertThat(response.hubUrl()).isEqualTo("/");
        assertThat(response.runtime().state()).isEqualTo("READY");
        assertThat(response.frontend().state()).isEqualTo("READY");
        assertThat(response.backend().state()).isEqualTo("READY");
        assertThat(action(response, "restartRuntime").url()).isEqualTo("/api/sessions/sess-local/restart");
        assertThat(action(response, "rebuildRuntime").available()).isTrue();
        assertThat(action(response, "rebuildRuntime").body()).containsEntry("rebuild", true);
    }

    @Test
    void returnsShellContractForCloudRunSessionRoute() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-cloud", PlatformSessionState.READY);
        record.setPublicEntryUrl("/session/sess-cloud/");
        record.setInternalRuntimeRef("cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-cloud");
        record.setRuntimeStatus(SessionRuntimeStatuses.READY);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-cloud")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("owner_match"));

        SessionShellResponse response = sessionService.getSessionShell("sess-cloud");

        assertThat(response.learnerAppUrl()).isEqualTo("/session/sess-cloud/");
        assertThat(response.redisInsightUrl()).isEqualTo("/session/sess-cloud/redis-insight/");
        assertThat(response.hubUrl()).isEqualTo("/");
        assertThat(response.runtime().state()).isEqualTo("READY");
        assertThat(response.frontend().state()).isEqualTo("READY");
        assertThat(response.backend().state()).isEqualTo("READY");
        assertThat(action(response, "terminateSession").available()).isTrue();
    }

    @Test
    void shellContractRepresentsMissingOptionalCapabilitiesCleanly() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-starting", PlatformSessionState.PROVISIONING);
        record.setPublicEntryUrl(null);
        record.setRuntimeStatus(SessionRuntimeStatuses.PROVISIONING);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-starting")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("owner_match"));

        SessionShellResponse response = sessionService.getSessionShell("sess-starting");

        assertThat(response.learnerAppUrl()).isNull();
        assertThat(response.redisInsightUrl()).isNull();
        assertThat(response.hubUrl()).isEqualTo("/");
        assertThat(response.runtime().state()).isEqualTo("STARTING");
        assertThat(response.frontend().state()).isEqualTo("STARTING");
        assertThat(response.backend().state()).isEqualTo("STARTING");
        assertThat(action(response, "openRedisInsight").available()).isFalse();
        assertThat(action(response, "rebuildRuntime").available()).isFalse();
        assertThat(action(response, "rebuildRuntime").unavailableReason()).isEqualTo("rebuild_unavailable");
    }

    @Test
    void transitionsActiveSessionsToTerminating() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-001", PlatformSessionState.READY);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canTerminate(any())).thenReturn(PolicyDecision.allow("owner_match"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.terminateSession("sess-001");

        assertThat(response.state()).isEqualTo(SessionState.TERMINATING);
        assertThat(response.terminationReason()).isEqualTo("user_requested");
        verify(sessionRuntimeLifecyclePort).requestTermination(any(SessionTerminationRequest.class));
    }

    @Test
    void transitionsReadySessionsToProvisioningAndRequestsRestart() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-001b", PlatformSessionState.READY);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001b")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRestart(any())).thenReturn(PolicyDecision.allow("owner_match"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.restartSession("sess-001b", new RestartSessionRequest(true));

        assertThat(response.state()).isEqualTo(SessionState.PROVISIONING);
        assertThat(record.getRuntimeStatus()).isEqualTo("restart_requested_rebuild");
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.ACTIVE);
        verify(sessionRuntimeLifecyclePort).requestRestart(any(SessionRestartRequest.class));
    }

    @Test
    void restartWithoutRebuildRequestsManagerRestartWithReleaseArtifacts() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-001d", PlatformSessionState.READY);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001d")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRestart(any())).thenReturn(PolicyDecision.allow("owner_match"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.restartSession("sess-001d", new RestartSessionRequest(false));

        assertThat(response.state()).isEqualTo(SessionState.PROVISIONING);
        assertThat(record.getRuntimeStatus()).isEqualTo("restart_requested");
        verify(sessionRuntimeLifecyclePort).requestRestart(argThat(request ->
            !request.rebuild()
                && request.artifacts().containsKey("frontend")
                && request.artifacts().containsKey("backend")
                && request.artifacts().containsKey("combined")
        ));
    }

    @Test
    void rejectsRestartFromFailedTerminationState() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-001c", PlatformSessionState.FAILED);
        record.setTerminationReason("user_requested");
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001c")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRestart(any())).thenReturn(PolicyDecision.allow("owner_match"));

        assertThatThrownBy(() -> sessionService.restartSession("sess-001c", new RestartSessionRequest(false)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409 CONFLICT");

        verify(sessionRepository, never()).save(any());
        verify(sessionRuntimeLifecyclePort, never()).requestRestart(any(SessionRestartRequest.class));
    }

    @Test
    void keepsTerminalSessionsIdempotentOnDelete() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-001", PlatformSessionState.FAILED);
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canTerminate(any())).thenReturn(PolicyDecision.allow("owner_match"));

        SessionResponse response = sessionService.terminateSession("sess-001");

        assertThat(response.state()).isEqualTo(SessionState.FAILED);
        verify(sessionRepository, never()).save(any());
        verify(sessionRuntimeLifecyclePort, never()).requestTermination(any());
    }

    @Test
    void expiresReadySessionsWhenTheyAreReadAfterTtl() {
        CurrentActor learner = new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
        PlatformSessionRecord record = sessionRecord("sess-009", PlatformSessionState.READY);
        record.setExpiresAt(Instant.now().minusSeconds(30));
        when(currentActorProvider.requireCurrentActor()).thenReturn(learner);
        when(sessionRepository.findById("sess-009")).thenReturn(Optional.of(record));
        when(sessionAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("owner_match"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.getSession("sess-009");

        assertThat(response.state()).isEqualTo(SessionState.EXPIRED);
        assertThat(response.terminationReason()).isEqualTo("expired");
        assertThat(response.workspaceCleanupStatus()).isEqualTo(WorkspaceCleanupStatus.PENDING);
        assertThat(record.getExpiredAt()).isNotNull();
        assertThat(record.getTerminationRequestedAt()).isNotNull();
    }

    @Test
    void findsCleanupPendingAndRuntimeTrackedSessionsFromOwnedSeams() {
        PlatformSessionRecord expired = sessionRecord("sess-010", PlatformSessionState.EXPIRED);
        expired.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        expired.setInternalRuntimeRef("cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-010");
        when(sessionRepository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(any()))
            .thenReturn(List.of(expired));
        when(sessionRepository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(any()))
            .thenReturn(List.of(expired));

        List<SessionResponse> cleanupPending = sessionService.findCleanupPendingSessions();
        List<SessionResponse> runtimeTracked = sessionService.findLogicalEndedSessionsWithRuntimeRefs();

        assertThat(cleanupPending).singleElement().satisfies(session ->
            assertThat(session.workspaceCleanupStatus()).isEqualTo(WorkspaceCleanupStatus.PENDING)
        );
        assertThat(runtimeTracked).singleElement().satisfies(session ->
            assertThat(session.sessionId()).isEqualTo("sess-010")
        );
    }

    @Test
    void reconcileExpiredSessionsMarksExpiredAndRequestsTerminationRetry() {
        PlatformSessionRecord record = sessionRecord("sess-011", PlatformSessionState.READY);
        record.setExpiresAt(Instant.parse("2026-04-21T09:59:00Z"));
        when(sessionRepository.findAllByExpiresAtBeforeAndStateInOrderByExpiresAtAsc(any(), any()))
            .thenReturn(List.of(record));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int reconciled = sessionService.reconcileExpiredSessions(Instant.parse("2026-04-21T10:00:00Z"));

        assertThat(reconciled).isEqualTo(1);
        assertThat(record.getState()).isEqualTo(PlatformSessionState.EXPIRED);
        assertThat(record.getTerminationReason()).isEqualTo("expired");
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.PENDING);
        verify(sessionRuntimeLifecyclePort).requestTerminationRetry(any(SessionTerminationRequest.class));
    }

    @Test
    void reconcileLogicalEndedTerminationRetriesUsesDefaultReasonWhenMissing() {
        PlatformSessionRecord record = sessionRecord("sess-012", PlatformSessionState.FAILED);
        record.setInternalRuntimeRef("cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-012");
        record.setTerminationReason(null);
        when(sessionRepository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(any()))
            .thenReturn(List.of(record));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int retried = sessionService.reconcileLogicalEndedRuntimeTerminationRetries();

        assertThat(retried).isEqualTo(1);
        assertThat(record.getTerminationReason()).isEqualTo("runtime_failed");
        verify(sessionRuntimeLifecyclePort).requestTerminationRetry(any(SessionTerminationRequest.class));
    }

    @Test
    void reconcileCompletedCleanupMetadataCompletesDetachedCleanup() {
        PlatformSessionRecord record = sessionRecord("sess-013", PlatformSessionState.EXPIRED);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.FAILED);
        record.setInternalRuntimeRef(null);
        when(sessionRepository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(any()))
            .thenReturn(List.of(record));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int completed = sessionService.reconcileCompletedCleanupMetadata();

        assertThat(completed).isEqualTo(1);
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.COMPLETED);
        assertThat(record.getCleanupCompletedAt()).isNotNull();
    }

    private PlatformSessionRecord sessionRecord(String sessionId, PlatformSessionState state) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(state);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2099-04-21T11:00:00Z"));
        record.setLastActivityAt(Instant.parse("2026-04-21T10:05:00Z"));
        record.setRouteType(RouteType.SUBDOMAIN);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }

    private SessionService sessionServiceWithDefaultPolicy() {
        return new SessionService(
            sessionRepository,
            workshopCatalogService,
            new DefaultSessionAccessPolicy(),
            sessionProvisioningPolicy,
            currentActorProvider,
            pilotLaunchRuleService,
            releaseCatalogService,
            sessionRuntimeLifecyclePort
        );
    }

    private CurrentActor learnerActor(String email) {
        return new CurrentActor(
            email,
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        );
    }

    private WorkshopCatalogEntry workshopEntry() {
        return new WorkshopCatalogEntry(
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
        );
    }

    private SessionProvisioningProfile defaultProvisioningProfile() {
        return new SessionProvisioningProfile("standard", "small", Duration.ofMinutes(60));
    }

    private ReleaseCatalogEntry release(String workshopId, String releaseVersion, SessionMode mode) {
        return new ReleaseCatalogEntry(
            workshopId + "-" + releaseVersion,
            workshopId,
            releaseVersion,
            mode,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/" + workshopId + "-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/" + workshopId + "-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                "registry.example.com/workshops/" + workshopId + "-runner@sha256:9999999999999999999999999999999999999999999999999999999999999999",
                null
            ),
            "small",
            60,
            List.of("redis")
        );
    }

    private SessionShellResponse.Action action(SessionShellResponse response, String name) {
        return response.actions().stream()
            .filter(candidate -> candidate.name().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
