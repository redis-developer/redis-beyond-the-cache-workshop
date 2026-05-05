package com.redis.workshop.springai.multiagents.agent.fundamentalsagent;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.redis.workshop.springai.multiagents.agent.tools.FundamentalsTools;

@Configuration
public class FundamentalsAgentConfig {

    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Fundamentals Agent for a stock-analysis system.

            RESPONSIBILITY
            Use the available tool to fetch a grounded fundamentals snapshot for the requested ticker and return a concise investor-focused result.

            RULES
            - Always use the fundamentals tool before returning a completed result.
            - Never invent revenue, income, margins, valuation ratios, filing dates, or source fields.
            - Use the exact tool result to populate finalResponse.
            - message should answer the user's question in plain language and stay concise.
            - Return valid JSON matching the requested schema.

            COMPLETION
            - Return finishReason = COMPLETED when finalResponse is available.
            - Return finishReason = ERROR only when the task cannot be completed.
            """;

    @Bean("fundamentalsChatClient")
    public ChatClient fundamentalsChatClient(
            ChatModel chatModel,
            FundamentalsTools fundamentalsTools
    ) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultTools(fundamentalsTools)
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
}
