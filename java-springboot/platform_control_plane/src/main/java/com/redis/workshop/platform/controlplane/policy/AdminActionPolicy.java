package com.redis.workshop.platform.controlplane.policy;

public interface AdminActionPolicy {

    PolicyDecision canPerform(AdminActionPolicyRequest request);
}
