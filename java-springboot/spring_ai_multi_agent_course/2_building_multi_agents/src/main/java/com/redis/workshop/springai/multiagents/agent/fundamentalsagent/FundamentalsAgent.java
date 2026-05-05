package com.redis.workshop.springai.multiagents.agent.fundamentalsagent;

// Stage 5 imports to enable:
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.client.ResponseEntity;
// import org.springframework.ai.chat.model.ChatResponse;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.stereotype.Service;

// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;

/*
Stage 5 annotation to enable:
@Service
*/
public class FundamentalsAgent {

    /*
    Stage 5 field and constructor to enable:

    private final ChatClient fundamentalsChatClient;

    public FundamentalsAgent(@Qualifier("fundamentalsChatClient") ChatClient fundamentalsChatClient) {
        this.fundamentalsChatClient = fundamentalsChatClient;
    }
    */

    public FundamentalsResult execute(String ticker, String question) {
        return FundamentalsResult.error("Fundamentals Agent is not wired yet.");

        /*
        Stage 5 execute block to enable:
        Comment out the scaffold return above, then uncomment this block.

        ResponseEntity<ChatResponse, FundamentalsResult> response = fundamentalsChatClient
                .prompt()
                .user(buildPrompt(ticker, question))
                .call()
                .responseEntity(FundamentalsResult.class);

        TokenUsageSummary tokenUsage = TokenUsageSummary.from(response.response());
        FundamentalsResult entity = response.entity();

        if (entity == null || entity.getFinalResponse() == null
                || entity.getFinishReason() != FundamentalsResult.FinishReason.COMPLETED) {
            throw new IllegalStateException("Fundamentals Agent returned an invalid response.");
        }

        entity.setTokenUsage(tokenUsage);
        return entity;
        */
    }

    private String buildPrompt(String ticker, String question) {
        return "";

        /*
        Stage 5 runtime prompt to enable:
        Comment out the scaffold return above, then uncomment this block.

        return """
                TICKER
                %s

                USER_QUESTION
                %s

                INSTRUCTIONS
                Use the available tool to fetch the fundamentals snapshot.
                Populate finalResponse with the exact tool values.
                Keep message to one concise sentence.
                """.formatted(ticker.toUpperCase(), question);
        */
    }
}
