package com.redis.workshop.platform.controlplane.policy;

import org.springframework.stereotype.Component;

@Component
public class DefaultSessionAccessPolicy implements SessionAccessPolicy {

    @Override
    public PolicyDecision canCreate(SessionCreatePolicyRequest request) {
        if (request.actorType() == PolicyActorType.ADMIN) {
            return PolicyDecision.allow("admin_override");
        }
        if (request.actorType() == PolicyActorType.LEARNER && request.actorId() != null
            && request.actorId().equals(request.ownerUserId())) {
            return PolicyDecision.allow("self_service");
        }
        return PolicyDecision.deny("session_create_not_authorized");
    }

    @Override
    public PolicyDecision canList(SessionListPolicyRequest request) {
        if (request.actorType() == PolicyActorType.ADMIN) {
            return PolicyDecision.allow("admin_own_scope");
        }
        if (request.actorType() == PolicyActorType.LEARNER && request.actorId() != null
            && request.actorId().equals(request.ownerUserId())) {
            return PolicyDecision.allow("own_sessions");
        }
        return PolicyDecision.deny("session_list_not_authorized");
    }

    @Override
    public PolicyDecision canRead(SessionAccessPolicyRequest request) {
        return canAccessOwnedSession(request, "session_read_not_authorized");
    }

    @Override
    public PolicyDecision canRestart(SessionAccessPolicyRequest request) {
        return canAccessOwnedSession(request, "session_restart_not_authorized");
    }

    @Override
    public PolicyDecision canTerminate(SessionAccessPolicyRequest request) {
        return canAccessOwnedSession(request, "session_terminate_not_authorized");
    }

    private PolicyDecision canAccessOwnedSession(SessionAccessPolicyRequest request, String deniedReason) {
        if (request.actorType() == PolicyActorType.ADMIN) {
            return PolicyDecision.allow("admin_override");
        }
        if (request.actorType() == PolicyActorType.LEARNER && request.actorId() != null
            && request.actorId().equals(request.ownerUserId())) {
            return PolicyDecision.allow("owner_match");
        }
        return PolicyDecision.deny(deniedReason);
    }
}
