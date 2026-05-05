package com.redis.workshop.springai.multiagents;

import java.util.List;

import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;
import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;
import com.redis.workshop.springai.multiagents.agent.orchestration.AnalysisResponse;
import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisResult;

record MultiAgentsStatusResponse(String module, boolean apiKeyConfigured, String model, String status) {
}

record MultiAgentSummary(String name, String role, String status) {
}

record MultiAgentsWorkflowResponse(List<MultiAgentSummary> agents, String nextStep) {
}

record MultiAgentsRunRequest(String conversationId, String ticker, String task) {
}

record MultiAgentStep(String agent, String message) {
}

record MultiAgentsRunResponse(
        boolean success,
        String answer,
        List<MultiAgentStep> steps,
        List<String> retrievedMemories,
        MarketDataResult marketData,
        FundamentalsResult fundamentals,
        NewsResult news,
        SynthesisResult synthesis,
        AnalysisResponse analysis,
        String error
) {
}
