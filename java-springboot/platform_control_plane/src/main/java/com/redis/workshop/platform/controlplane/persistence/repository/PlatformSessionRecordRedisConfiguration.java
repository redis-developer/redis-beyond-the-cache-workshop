package com.redis.workshop.platform.controlplane.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("cloudrun")
@EnableConfigurationProperties(PlatformSessionRecordRedisProperties.class)
class PlatformSessionRecordRedisConfiguration {

    @Bean
    RedisTemplate<String, PlatformSessionRecord> platformSessionRecordRedisTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, PlatformSessionRecord> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, PlatformSessionRecord.class));
        template.afterPropertiesSet();
        return template;
    }
}
