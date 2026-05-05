package com.redis.workshop.springai.multiagents.agent.fundamentalsagent;

// Stage 5 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import com.redis.workshop.springai.multiagents.agent.tools.FundamentalsTools;

/*
Stage 5 annotation to enable:
@Configuration
*/
public class FundamentalsAgentConfig {

    private static final String DEFAULT_PROMPT = """
            STAGE 5 TODO:
            Replace this placeholder with the fundamentals prompt.
            """;

    /*
    Stage 5 ChatClient bean to enable:

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
    */
}
