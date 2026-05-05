package com.redis.workshop.springai.multiagents.agent.synthesisagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;
import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;
import com.redis.workshop.springai.multiagents.agent.orchestration.AnalysisRequest;
import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;

@Service
public class SynthesisAgent {

    private final ChatClient synthesisChatClient;

    public SynthesisAgent(@Qualifier("synthesisChatClient") ChatClient synthesisChatClient) {
        this.synthesisChatClient = synthesisChatClient;
    }

    public SynthesisResult execute(
            AnalysisRequest request,
            MarketDataResult marketData,
            FundamentalsResult fundamentals,
            NewsResult news
    ) {
        ResponseEntity<ChatResponse, SynthesisResponse> response = synthesisChatClient
                .prompt()
                .user(buildPrompt(request, marketData, fundamentals, news))
                .call()
                .responseEntity(SynthesisResponse.class);

        SynthesisResponse entity = response.entity();

        if (entity == null || entity.finalAnswer() == null || entity.finalAnswer().isBlank()) {
            throw new IllegalStateException("Synthesis Agent returned an invalid response.");
        }

        return new SynthesisResult(
                entity.finalAnswer().trim(),
                TokenUsageSummary.from(response.response())
        );
    }

    private String buildPrompt(
            AnalysisRequest request,
            MarketDataResult marketData,
            FundamentalsResult fundamentals,
            NewsResult news
    ) {
        return """
                QUESTION
                %s

                REQUESTED_TICKER
                %s

                MARKET_DATA
                %s

                FUNDAMENTALS
                %s

                NEWS
                %s

                INSTRUCTIONS
                Answer the user directly.
                Ground the answer in the specialist outputs.
                Keep the answer to one short paragraph.
                """.formatted(
                request.question(),
                request.ticker(),
                section(marketData, "No market data result."),
                section(fundamentals, "No fundamentals result."),
                section(news, "No news result.")
        );
    }

    private String section(MarketDataResult result, String fallback) {
        if (result == null) {
            return fallback;
        }

        return """
                MESSAGE
                %s

                DATA
                %s
                """.formatted(result.getMessage(), result.getFinalResponse()).trim();
    }

    private String section(FundamentalsResult result, String fallback) {
        if (result == null) {
            return fallback;
        }

        return """
                MESSAGE
                %s

                DATA
                %s
                """.formatted(result.getMessage(), result.getFinalResponse()).trim();
    }

    private String section(NewsResult result, String fallback) {
        if (result == null) {
            return fallback;
        }

        return """
                MESSAGE
                %s

                DATA
                %s
                """.formatted(result.getMessage(), result.getFinalResponse()).trim();
    }
}
