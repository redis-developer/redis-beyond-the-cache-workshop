package com.redis.workshop.springai.multiagents.agent.marketdataagent;

// Stage 3 imports to enable:
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.client.ResponseEntity;
// import org.springframework.ai.chat.model.ChatResponse;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.stereotype.Service;

// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;

/*
Stage 3 annotation to enable:
@Service
*/
public class MarketDataAgent {

    /*
    Stage 3 field and constructor to enable:

    private final ChatClient marketDataChatClient;

    public MarketDataAgent(@Qualifier("marketDataChatClient") ChatClient marketDataChatClient) {
        this.marketDataChatClient = marketDataChatClient;
    }
    */

    public MarketDataResult execute(String ticker, String question) {
        return MarketDataResult.error("Market Data Agent is not wired yet.");

        /*
        Stage 3 execute block to enable:
        Comment out the scaffold return above, then uncomment this block.

        ResponseEntity<ChatResponse, MarketDataResult> response = marketDataChatClient
                .prompt()
                .user(buildPrompt(ticker, question))
                .call()
                .responseEntity(MarketDataResult.class);

        TokenUsageSummary tokenUsage = TokenUsageSummary.from(response.response());
        MarketDataResult entity = response.entity();

        if (entity == null || entity.getFinalResponse() == null
                || entity.getFinishReason() != MarketDataResult.FinishReason.COMPLETED) {
            throw new IllegalStateException("Market Data Agent returned an invalid response.");
        }

        entity.setTokenUsage(tokenUsage);
        return entity;
        */
    }

    private String buildPrompt(String ticker, String question) {
        return "";

        /*
        Stage 3 runtime prompt to enable:
        Comment out the scaffold return above, then uncomment this block.

        return """
                TICKER
                %s

                USER_QUESTION
                %s

                INSTRUCTIONS
                Use the available tool to fetch the current market snapshot.
                Populate finalResponse with the exact tool values.
                Keep message to one concise sentence.
                """.formatted(ticker.toUpperCase(), question);
        */
    }
}
