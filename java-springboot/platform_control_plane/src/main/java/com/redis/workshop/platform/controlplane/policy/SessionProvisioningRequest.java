package com.redis.workshop.platform.controlplane.policy;

import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.springframework.util.StringUtils;

public record SessionProvisioningRequest(
    String workshopId,
    String releaseVersion,
    SessionMode mode
) {

    public SessionProvisioningRequest {
        workshopId = requireText(workshopId, "workshopId");
        releaseVersion = requireText(releaseVersion, "releaseVersion");
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
