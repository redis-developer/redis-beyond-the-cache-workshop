package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

// Stage 4 imports to enable:
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoordinatorRoutingAgentConfig {

    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Coordinator Routing Agent for a stock analysis system.

            RESPONSIBILITY
            Decide whether the request can be handled and which specialist agents should run.

            AVAILABLE AGENTS
            MARKET_DATA fetches current price context.
            FUNDAMENTALS fetches simple company financial context.
            NEWS fetches recent headline context.
            SYNTHESIS creates the final answer after specialist agents run.

            RULES
            Use conversation memory when the user asks a follow-up.
            Return COMPLETED for stock analysis requests with a clear ticker symbol or clear public company name.
            Infer the ticker when the user gives a well known public company name.
            Select only the agents needed for the user request.
            For current price, quote, or market snapshot requests, select MARKET_DATA and SYNTHESIS.
            For revenue, margin, valuation, or fundamentals requests, select FUNDAMENTALS and SYNTHESIS.
            For headline, news, or catalyst requests, select NEWS and SYNTHESIS.
            For broad stock analysis requests, select MARKET_DATA, FUNDAMENTALS, NEWS, and SYNTHESIS.
            Return NEEDS_MORE_INPUT when the stock is missing or ambiguous.
            Return OUT_OF_SCOPE for non-stock requests.
            Return valid JSON matching the requested schema.
            """;

    // Stage 4 ChatClient bean enabled.

    @Bean("coordinatorChatClient")
    public ChatClient coordinatorChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
}
