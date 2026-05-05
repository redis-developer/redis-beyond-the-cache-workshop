package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

// Stage 4 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// import org.springframework.ai.chat.memory.ChatMemory;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

/*
Stage 4 annotation to enable:
@Configuration
*/
public class CoordinatorRoutingAgentConfig {

    private static final String DEFAULT_PROMPT = """
            STAGE 4 TODO:
            Replace this placeholder with the coordinator routing prompt.
            """;

    /*
    Stage 4 ChatClient bean to enable:

    @Bean("coordinatorChatClient")
    public ChatClient coordinatorChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(DEFAULT_PROMPT)
                .build();
    }
    */
}
