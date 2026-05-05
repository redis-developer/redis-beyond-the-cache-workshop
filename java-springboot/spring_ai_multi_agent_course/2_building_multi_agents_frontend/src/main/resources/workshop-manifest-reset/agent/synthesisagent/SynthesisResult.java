package com.redis.workshop.springai.multiagents.agent.synthesisagent;

import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;

public record SynthesisResult(
        String finalAnswer,
        TokenUsageSummary tokenUsage
) {
}
