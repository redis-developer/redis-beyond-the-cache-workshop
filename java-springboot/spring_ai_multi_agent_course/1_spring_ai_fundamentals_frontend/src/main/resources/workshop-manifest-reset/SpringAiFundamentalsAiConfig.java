package com.redis.workshop.springai.fundamentals;

/*
Stage 4 imports to enable later:

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;
*/

import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiFundamentalsAiConfig {

    /*
    Stage 4 will enable Redis chat memory.

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port
    ) {
        return new JedisPooled(host, port);
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(JedisPooled jedisPooled) {
        return RedisChatMemoryRepository.builder()
                .jedisClient(jedisPooled)
                .keyPrefix("spring-ai-fundamentals:")
                .maxMessagesPerConversation(12)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return new RedisSafeChatMemory(MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(12)
                .build());
    }
    */
}
