package com.redis.workshop.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
    org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.redis.workshop.memory",
    "com.redis.workshop.infrastructure"
})
public class AgentMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentMemoryApplication.class, args);
    }
}

