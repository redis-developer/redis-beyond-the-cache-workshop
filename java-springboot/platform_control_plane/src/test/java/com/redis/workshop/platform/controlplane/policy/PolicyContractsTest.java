package com.redis.workshop.platform.controlplane.policy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyContractsTest {

    private final DefaultCatalogAccessPolicy catalogAccessPolicy = new DefaultCatalogAccessPolicy();
    private final DefaultSessionAccessPolicy sessionAccessPolicy = new DefaultSessionAccessPolicy();
    private final DefaultAdminActionPolicy adminActionPolicy = new DefaultAdminActionPolicy();

    @Test
    void allowsAnonymousReadsForPublicCatalogEntries() {
        PolicyDecision decision = catalogAccessPolicy.canRead(new CatalogAccessRequest(
            null,
            PolicyActorType.ANONYMOUS,
            Set.of(),
            "1_session_management",
            true
        ));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void allowsLearnersToCreateAndReadTheirOwnSessions() {
        PolicyDecision createDecision = sessionAccessPolicy.canCreate(new SessionCreatePolicyRequest(
            "learner-1",
            PolicyActorType.LEARNER,
            Set.of("platform:learner"),
            "learner-1",
            "1_session_management"
        ));
        PolicyDecision readDecision = sessionAccessPolicy.canRead(new SessionAccessPolicyRequest(
            "learner-1",
            PolicyActorType.LEARNER,
            Set.of("platform:learner"),
            "learner-1",
            "sess-001"
        ));

        assertThat(createDecision.allowed()).isTrue();
        assertThat(readDecision.allowed()).isTrue();
    }

    @Test
    void deniesLearnerAccessToAnotherUsersSession() {
        PolicyDecision decision = sessionAccessPolicy.canTerminate(new SessionAccessPolicyRequest(
            "learner-2",
            PolicyActorType.LEARNER,
            Set.of("platform:learner"),
            "learner-1",
            "sess-001"
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("session_terminate_not_authorized");
    }

    @Test
    void allowsAdminOverrideForSessionAccess() {
        PolicyDecision decision = sessionAccessPolicy.canRead(new SessionAccessPolicyRequest(
            "admin-1",
            PolicyActorType.ADMIN,
            Set.of("platform:admin"),
            "learner-1",
            "sess-001"
        ));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void reservesAdminActionsForAdminAndInternalServiceActors() {
        PolicyDecision adminDecision = adminActionPolicy.canPerform(new AdminActionPolicyRequest(
            "admin-1",
            PolicyActorType.ADMIN,
            Set.of("platform:admin"),
            "terminate_session",
            "session",
            "sess-001"
        ));
        PolicyDecision serviceDecision = adminActionPolicy.canPerform(new AdminActionPolicyRequest(
            "svc-launcher",
            PolicyActorType.INTERNAL_SERVICE,
            Set.of("platform:internal-service"),
            "publish_status",
            "session",
            "sess-001"
        ));

        assertThat(adminDecision.allowed()).isTrue();
        assertThat(serviceDecision.allowed()).isTrue();
    }
}
