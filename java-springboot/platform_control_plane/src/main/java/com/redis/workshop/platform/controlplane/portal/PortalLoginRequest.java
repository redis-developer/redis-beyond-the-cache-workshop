package com.redis.workshop.platform.controlplane.portal;

import jakarta.validation.constraints.NotBlank;

public record PortalLoginRequest(
    @NotBlank String email,
    Boolean allowMarketingContact
) {

    boolean allowsMarketingContact() {
        return allowMarketingContact == null || allowMarketingContact;
    }
}
