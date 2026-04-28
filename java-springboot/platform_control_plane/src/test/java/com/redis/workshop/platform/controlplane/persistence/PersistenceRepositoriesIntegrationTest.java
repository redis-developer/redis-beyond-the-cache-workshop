package com.redis.workshop.platform.controlplane.persistence;

import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditActorType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditEventRecord;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.AuditEventRecordRepository;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:platform-control-plane;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceRepositoriesIntegrationTest {

    @Autowired
    private PlatformSessionRecordRepository sessionRepository;

    @Autowired
    private AuditEventRecordRepository auditRepository;

    @Test
    void storesAndQueriesSessionRecordsByOwnerAndState() {
        PlatformSessionRecord requested = new PlatformSessionRecord();
        requested.setSessionId("sess-001");
        requested.setOwnerUserId("learner-1");
        requested.setWorkshopId("1_session_management");
        requested.setReleaseVersion("2026.04.1");
        requested.setMode(PlatformSessionMode.LAB);
        requested.setState(PlatformSessionState.REQUESTED);
        requested.setQuotaClass("standard");
        requested.setResourceClass("small");
        requested.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        requested.setExpiresAt(Instant.parse("2026-04-21T11:00:00Z"));
        requested.setLastActivityAt(Instant.parse("2026-04-21T10:00:00Z"));
        requested.setRouteType(RouteType.SUBDOMAIN);
        requested.setPublicEntryUrl("https://s-sess-001.labs.example.com");
        requested.setInternalRuntimeRef("runtime-001");
        requested.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        requested.setWorkspaceRef("session-workspace:sess-001");
        requested.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);

        PlatformSessionRecord ready = new PlatformSessionRecord();
        ready.setSessionId("sess-002");
        ready.setOwnerUserId("learner-1");
        ready.setWorkshopId("1_session_management");
        ready.setReleaseVersion("2026.04.1");
        ready.setMode(PlatformSessionMode.LAB);
        ready.setState(PlatformSessionState.READY);
        ready.setQuotaClass("standard");
        ready.setResourceClass("small");
        ready.setCreatedAt(Instant.parse("2026-04-21T10:05:00Z"));
        ready.setExpiresAt(Instant.parse("2026-04-21T11:05:00Z"));
        ready.setLastActivityAt(Instant.parse("2026-04-21T10:06:00Z"));
        ready.setRouteType(RouteType.SUBDOMAIN);
        ready.setPublicEntryUrl("https://s-sess-002.labs.example.com");
        ready.setInternalRuntimeRef("runtime-002");
        ready.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        ready.setWorkspaceRef("session-workspace:sess-002");
        ready.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        ready.setTerminationRequestedAt(Instant.parse("2026-04-21T10:50:00Z"));

        sessionRepository.saveAll(List.of(requested, ready));

        List<PlatformSessionRecord> sessions =
            sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc("learner-1");

        assertThat(sessions).extracting(PlatformSessionRecord::getSessionId)
            .containsExactly("sess-002", "sess-001");
        assertThat(sessionRepository.findBySessionIdAndOwnerUserId("sess-001", "learner-1"))
            .isPresent();
        assertThat(sessionRepository.countByWorkshopIdAndModeAndState(
            "1_session_management",
            PlatformSessionMode.LAB,
            PlatformSessionState.READY
        )).isEqualTo(1);
        assertThat(sessionRepository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(List.of(
            WorkspaceCleanupState.PENDING
        ))).extracting(PlatformSessionRecord::getSessionId).containsExactly("sess-002");
        assertThat(sessionRepository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(List.of(
            PlatformSessionState.READY
        ))).extracting(PlatformSessionRecord::getSessionId).containsExactly("sess-002");
    }

    @Test
    void storesAndQueriesAuditEventsBySession() {
        AuditEventRecord created = new AuditEventRecord();
        created.setActorId("learner-1");
        created.setActorType(AuditActorType.LEARNER);
        created.setActionType(AuditActionType.SESSION_CREATED);
        created.setTargetResourceType(AuditTargetType.SESSION);
        created.setTargetResourceId("sess-001");
        created.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        created.setResult(AuditResult.SUCCESS);
        created.setRequestId("req-001");
        created.setSessionId("sess-001");
        created.setWorkshopId("1_session_management");
        created.setReleaseVersion("2026.04.1");
        created.setRuntimeRef("runtime-001");

        AuditEventRecord terminated = new AuditEventRecord();
        terminated.setActorId("learner-1");
        terminated.setActorType(AuditActorType.LEARNER);
        terminated.setActionType(AuditActionType.SESSION_TERMINATED_BY_USER);
        terminated.setTargetResourceType(AuditTargetType.SESSION);
        terminated.setTargetResourceId("sess-001");
        terminated.setCreatedAt(Instant.parse("2026-04-21T10:30:00Z"));
        terminated.setResult(AuditResult.SUCCESS);
        terminated.setReason("user-requested");
        terminated.setRequestId("req-002");
        terminated.setSessionId("sess-001");
        terminated.setWorkshopId("1_session_management");
        terminated.setReleaseVersion("2026.04.1");
        terminated.setRuntimeRef("runtime-001");

        auditRepository.saveAll(List.of(created, terminated));

        List<AuditEventRecord> sessionEvents =
            auditRepository.findAllBySessionIdOrderByCreatedAtDesc("sess-001");

        assertThat(sessionEvents).extracting(AuditEventRecord::getActionType)
            .containsExactly(
                AuditActionType.SESSION_TERMINATED_BY_USER,
                AuditActionType.SESSION_CREATED
            );
        assertThat(auditRepository.findAllByTargetResourceIdOrderByCreatedAtDesc("sess-001"))
            .hasSize(2);
    }
}
