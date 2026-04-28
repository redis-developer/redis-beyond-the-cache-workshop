package com.redis.workshop.platform.controlplane.policy;

public interface SessionAccessPolicy {

    PolicyDecision canCreate(SessionCreatePolicyRequest request);

    PolicyDecision canList(SessionListPolicyRequest request);

    PolicyDecision canRead(SessionAccessPolicyRequest request);

    PolicyDecision canRestart(SessionAccessPolicyRequest request);

    PolicyDecision canTerminate(SessionAccessPolicyRequest request);
}
