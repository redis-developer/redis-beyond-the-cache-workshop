package com.redis.workshop.platform.controlplane.policy;

import org.springframework.stereotype.Component;

@Component
public class DefaultCatalogAccessPolicy implements CatalogAccessPolicy {

    @Override
    public PolicyDecision canRead(CatalogAccessRequest request) {
        if (request.publiclyVisible()) {
            return PolicyDecision.allow("public_workshop");
        }
        if (request.actorType() == PolicyActorType.ADMIN || request.actorType() == PolicyActorType.INTERNAL_SERVICE) {
            return PolicyDecision.allow("elevated_actor");
        }
        return PolicyDecision.deny("catalog_not_visible");
    }
}
