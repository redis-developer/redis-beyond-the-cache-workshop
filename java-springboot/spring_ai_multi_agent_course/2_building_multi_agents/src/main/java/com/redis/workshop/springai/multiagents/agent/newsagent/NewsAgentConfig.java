package com.redis.workshop.springai.multiagents.agent.newsagent;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.redis.workshop.springai.multiagents.agent.tools.NewsTools;

@Configuration
public class NewsAgentConfig {

    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the News Agent for a stock analysis system.

            RESPONSIBILITY
            Use the available tools to fetch a short news snapshot for the requested ticker.

            RULES
            Always use the news tools before returning a completed result.
            Never invent headlines, timestamps, or sources.
            Use the exact tool result to populate finalResponse.
            Keep message to one concise sentence.
            Return valid JSON matching the requested schema.

            COMPLETION
            Return finishReason = COMPLETED when finalResponse is available.
            Return finishReason = ERROR only when the task cannot be completed.
            """;

    @Bean("newsChatClient")
    public ChatClient newsChatClient(
            ChatModel chatModel,
            NewsTools newsTools
    ) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultTools(newsTools)
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
}
