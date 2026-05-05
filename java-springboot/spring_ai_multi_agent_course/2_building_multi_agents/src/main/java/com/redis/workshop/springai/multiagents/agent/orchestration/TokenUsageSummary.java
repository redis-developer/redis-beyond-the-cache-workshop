package com.redis.workshop.springai.multiagents.agent.orchestration;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

public record TokenUsageSummary(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {

    public static TokenUsageSummary from(ChatResponse response) {
        return response == null ? null : from(response.getMetadata());
    }

    public static TokenUsageSummary from(ChatResponseMetadata metadata) {
        return metadata == null ? null : from(metadata.getUsage());
    }

    public static TokenUsageSummary from(Usage usage) {
        if (usage == null) {
            return null;
        }

        return new TokenUsageSummary(
                normalize(usage.getPromptTokens()),
                normalize(usage.getCompletionTokens()),
                normalize(usage.getTotalTokens())
        );
    }

    private static Integer normalize(Integer value) {
        return value == null || value < 0 ? null : value;
    }
}
