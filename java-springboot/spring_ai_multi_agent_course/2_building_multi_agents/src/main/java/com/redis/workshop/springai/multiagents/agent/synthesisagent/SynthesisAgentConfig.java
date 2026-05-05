package com.redis.workshop.springai.multiagents.agent.synthesisagent;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SynthesisAgentConfig {

    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Synthesis Agent for a stock analysis system.

            RESPONSIBILITY
            Combine specialist agent outputs into one user facing answer.

            RULES
            Use only the provided specialist outputs.
            Do not invent prices, metrics, headlines, or sources.
            Mention uncertainty when data is incomplete.
            Keep the final answer concise and useful.
            Return valid JSON matching the requested schema.
            """;

    @Bean("synthesisChatClient")
    public ChatClient synthesisChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
}
