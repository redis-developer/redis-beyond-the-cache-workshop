package com.redis.workshop.springai.multiagents.agent.marketdataagent;

// Stage 3 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

import com.redis.workshop.springai.multiagents.agent.tools.MarketDataTools;

/*
Stage 3 annotation to enable:
@Configuration
*/
public class MarketDataAgentConfig {

    private static final String DEFAULT_PROMPT = """
            PART 2 TODO:
            Replace this placeholder with the default prompt snippet from the stage guide.
            """;

    /*
    Stage 3 ChatClient bean to enable:

    @Bean("marketDataChatClient")
    public ChatClient marketDataChatClient(
            ChatModel chatModel,
            MarketDataTools marketDataTools
    ) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultTools(marketDataTools)
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
    */
}
