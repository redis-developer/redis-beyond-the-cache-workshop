package com.redis.workshop.springai.multiagents.agent.synthesisagent;

// Stage 5 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

/*
Stage 5 annotation to enable:
@Configuration
*/
public class SynthesisAgentConfig {

    private static final String DEFAULT_PROMPT = """
            STAGE 5 TODO:
            Replace this placeholder with the synthesis prompt.
            """;

    /*
    Stage 5 ChatClient bean to enable:

    @Bean("synthesisChatClient")
    public ChatClient synthesisChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
    */
}
