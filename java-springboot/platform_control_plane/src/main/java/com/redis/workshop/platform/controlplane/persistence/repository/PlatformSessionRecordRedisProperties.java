package com.redis.workshop.platform.controlplane.persistence.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "platform.controlplane.session.records.redis")
public record PlatformSessionRecordRedisProperties(
    String keyPrefix,
    String indexKey
) {

    private static final String DEFAULT_KEY_PREFIX = "platform:sessions:records:";
    private static final String DEFAULT_INDEX_KEY = "platform:sessions:index";

    public PlatformSessionRecordRedisProperties {
        keyPrefix = StringUtils.hasText(keyPrefix) ? keyPrefix.trim() : DEFAULT_KEY_PREFIX;
        indexKey = StringUtils.hasText(indexKey) ? indexKey.trim() : DEFAULT_INDEX_KEY;
    }
}
