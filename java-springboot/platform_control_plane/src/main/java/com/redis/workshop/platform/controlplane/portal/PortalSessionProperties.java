package com.redis.workshop.platform.controlplane.portal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.controlplane.portal.session")
public record PortalSessionProperties(
    String cookieName,
    Duration ttl,
    boolean secureCookie,
    String redisKeyPrefix,
    String contactPreferenceRedisKeyPrefix
) {

    private static final String DEFAULT_COOKIE_NAME = "portal_session";
    private static final Duration DEFAULT_TTL = Duration.ofHours(3);
    private static final String DEFAULT_REDIS_KEY_PREFIX = "platform:portal:sessions:";
    private static final String DEFAULT_CONTACT_PREFERENCE_REDIS_KEY_PREFIX = "platform:portal:contact_preferences:";

    public PortalSessionProperties {
        cookieName = StringUtils.hasText(cookieName) ? cookieName.trim() : DEFAULT_COOKIE_NAME;
        ttl = ttl == null || ttl.isZero() || ttl.isNegative() ? DEFAULT_TTL : ttl;
        redisKeyPrefix = StringUtils.hasText(redisKeyPrefix) ? redisKeyPrefix.trim() : DEFAULT_REDIS_KEY_PREFIX;
        contactPreferenceRedisKeyPrefix = StringUtils.hasText(contactPreferenceRedisKeyPrefix)
            ? contactPreferenceRedisKeyPrefix.trim()
            : DEFAULT_CONTACT_PREFERENCE_REDIS_KEY_PREFIX;
    }
}
