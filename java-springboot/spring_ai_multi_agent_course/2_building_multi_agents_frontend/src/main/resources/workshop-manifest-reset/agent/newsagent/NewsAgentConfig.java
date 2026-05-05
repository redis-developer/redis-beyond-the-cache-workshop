package com.redis.workshop.springai.multiagents.agent.newsagent;

// Stage 5 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import com.redis.workshop.springai.multiagents.agent.tools.NewsTools;

/*
Stage 5 annotation to enable:
@Configuration
*/
public class NewsAgentConfig {

    private static final String DEFAULT_PROMPT = """
            STAGE 5 TODO:
            Replace this placeholder with the news prompt.
            """;

    /*
    Stage 5 ChatClient bean to enable:

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
    */
}
