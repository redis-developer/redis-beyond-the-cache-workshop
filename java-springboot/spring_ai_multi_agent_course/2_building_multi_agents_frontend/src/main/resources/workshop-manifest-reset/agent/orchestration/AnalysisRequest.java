package com.redis.workshop.springai.multiagents.agent.orchestration;

public record AnalysisRequest(
        String ticker,
        String question
) {
}
