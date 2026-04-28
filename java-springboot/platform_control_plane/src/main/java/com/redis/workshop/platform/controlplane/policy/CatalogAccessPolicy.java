package com.redis.workshop.platform.controlplane.policy;

public interface CatalogAccessPolicy {

    PolicyDecision canRead(CatalogAccessRequest request);
}
