package com.redis.workshop.platform.controlplane.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

@Configuration
class PortalSessionRedisConfiguration {

    @Bean
    RedisTemplate<String, PortalSession> portalSessionRedisTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, PortalSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, PortalSession.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisTemplate<String, Map<String, PortalMarketingPreference>> portalMarketingPreferencesRedisTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Map<String, PortalMarketingPreference>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, Map.class));
        template.afterPropertiesSet();
        return template;
    }
}
