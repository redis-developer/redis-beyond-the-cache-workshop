package com.redis.workshop.platform.controlplane.policy;

import org.springframework.stereotype.Component;

@Component
public class DefaultAdminActionPolicy implements AdminActionPolicy {

    @Override
    public PolicyDecision canPerform(AdminActionPolicyRequest request) {
        if (request.actorType() == PolicyActorType.ADMIN || request.actorType() == PolicyActorType.INTERNAL_SERVICE) {
            return PolicyDecision.allow("elevated_actor");
        }
        return PolicyDecision.deny("admin_action_not_authorized");
    }
}
