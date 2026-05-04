package com.redis.workshop.platform.controlplane.portal;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortalUserResponse(
    boolean authenticated,
    String email,
    List<String> sessionIds
) {

    public PortalUserResponse {
        sessionIds = sessionIds == null ? null : List.copyOf(sessionIds);
    }

    public static PortalUserResponse authenticated(String email) {
        return new PortalUserResponse(true, email, List.of());
    }

    public static PortalUserResponse authenticated(PortalSession session) {
        return new PortalUserResponse(true, session.email(), session.sessionIds());
    }

    public static PortalUserResponse anonymous() {
        return new PortalUserResponse(false, null, null);
    }
}
