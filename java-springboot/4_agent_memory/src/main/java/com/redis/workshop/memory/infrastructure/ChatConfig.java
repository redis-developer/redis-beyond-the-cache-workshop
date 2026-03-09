package com.redis.workshop.memory.infrastructure;

import com.redis.agentmemory.MemoryAPIClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI chat with AMS-backed memory.
 */
@Configuration
public class ChatConfig {

    @Bean
    public MemoryAPIClient memoryAPIClient(
            @Value("${agent-memory.server.url}") String serverUrl,
            @Value("${agent-memory.server.namespace:workshop}") String namespace) {
        return MemoryAPIClient.builder(serverUrl)
                .defaultNamespace(namespace)
                .timeout(30.0)
                .build();
    }

    @Bean
    public AmsChatMemoryRepository amsChatMemoryRepository(MemoryAPIClient client) {
        return new AmsChatMemoryRepository(client);
    }

    @Bean
    public ChatMemory chatMemory(AmsChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public LongTermMemoryAdvisor longTermMemoryAdvisor(MemoryAPIClient client, AmsChatMemoryRepository memoryRepository) {
        return LongTermMemoryAdvisor.builder(client)
                .memoryRepository(memoryRepository)
                .maxMemories(5)
                .build();
    }
}

