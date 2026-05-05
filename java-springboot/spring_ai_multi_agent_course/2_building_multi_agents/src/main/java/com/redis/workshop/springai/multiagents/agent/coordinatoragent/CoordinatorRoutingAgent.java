package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

// Stage 4 imports to enable:
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CoordinatorRoutingAgent {

    // Stage 4 field and constructor enabled.

    private final ChatClient coordinatorChatClient;

    public CoordinatorRoutingAgent(@Qualifier("coordinatorChatClient") ChatClient coordinatorChatClient) {
        this.coordinatorChatClient = coordinatorChatClient;
    }

    public RoutingDecision route(String userMessage) {
        return route(userMessage, ChatMemory.DEFAULT_CONVERSATION_ID);
    }

    public RoutingDecision route(String userMessage, String conversationId) {
        // throw new UnsupportedOperationException("Stage 4: implement route(...)");

        // Stage 4 route block enabled.

        RoutingDecision decision = coordinatorChatClient.prompt()
                .user(buildPrompt(userMessage))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(RoutingDecision.class);

        if (decision == null || decision.getFinishReason() == null) {
            throw new IllegalStateException("Coordinator returned an invalid routing decision.");
        }

        return decision;
    }

    private String buildPrompt(String userMessage) {
        // return "";

        // Stage 4 runtime prompt enabled.

        return """
                USER_REQUEST
                %s

                ROUTING RULES
                Use the conversation history when the current request is a follow-up.
                If the request is about a stock, return finishReason COMPLETED.
                Infer the ticker from a ticker symbol or a clear public company name.
                Select only the agents needed for the request.
                For current price, quote, or market snapshot requests, select MARKET_DATA and SYNTHESIS.
                For revenue, margin, valuation, or fundamentals requests, select FUNDAMENTALS and SYNTHESIS.
                For headline, news, or catalyst requests, select NEWS and SYNTHESIS.
                For broad stock analysis requests, select MARKET_DATA, FUNDAMENTALS, NEWS, and SYNTHESIS.
                Put the ticker in resolvedTicker.
                Put the normalized question in resolvedQuestion.
                If the stock is ambiguous or missing, return NEEDS_MORE_INPUT and set nextPrompt.
                If the request is not about stocks, return OUT_OF_SCOPE and set finalResponse.
                """.formatted(userMessage);
    }
}
