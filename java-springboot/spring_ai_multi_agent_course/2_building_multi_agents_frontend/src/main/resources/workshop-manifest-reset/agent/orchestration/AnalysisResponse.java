package com.redis.workshop.springai.multiagents.agent.orchestration;

import com.redis.workshop.springai.multiagents.agent.coordinatoragent.ExecutionPlan;
import com.redis.workshop.springai.multiagents.agent.coordinatoragent.RoutingDecision;
import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;
import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;
import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisResult;

public record AnalysisResponse(
        String answer,
        RoutingDecision routingDecision,
        ExecutionPlan executionPlan,
        MarketDataResult marketData,
        FundamentalsResult fundamentals,
        NewsResult news,
        SynthesisResult synthesis
) {
}
