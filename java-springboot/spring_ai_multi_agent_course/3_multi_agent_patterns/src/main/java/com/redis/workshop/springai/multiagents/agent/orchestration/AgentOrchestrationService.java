package com.redis.workshop.springai.multiagents.agent.orchestration;

// Stage 5 imports to enable:
import org.springframework.stereotype.Service;

import com.redis.workshop.springai.multiagents.agent.coordinatoragent.CoordinatorAgent;
import com.redis.workshop.springai.multiagents.agent.coordinatoragent.ExecutionPlan;
import com.redis.workshop.springai.multiagents.agent.coordinatoragent.RoutingDecision;
import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsAgent;
import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;
import com.redis.workshop.springai.multiagents.agent.newsagent.NewsAgent;
import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;
import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisAgent;
import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisResult;

@Service
public class AgentOrchestrationService {

    // Stage 5 fields and constructor enabled.

    private final CoordinatorAgent coordinatorAgent;
    private final MarketDataAgent marketDataAgent;
    private final FundamentalsAgent fundamentalsAgent;
    private final NewsAgent newsAgent;
    private final SynthesisAgent synthesisAgent;

    public AgentOrchestrationService(
            CoordinatorAgent coordinatorAgent,
            MarketDataAgent marketDataAgent,
            FundamentalsAgent fundamentalsAgent,
            NewsAgent newsAgent,
            SynthesisAgent synthesisAgent
    ) {
        this.coordinatorAgent = coordinatorAgent;
        this.marketDataAgent = marketDataAgent;
        this.fundamentalsAgent = fundamentalsAgent;
        this.newsAgent = newsAgent;
        this.synthesisAgent = synthesisAgent;
    }

    public AnalysisResponse analyze(String userMessage) {
        return analyze(userMessage, "building-multi-agents-demo");
    }

    public AnalysisResponse analyze(String userMessage, String conversationId) {
        // throw new UnsupportedOperationException("Stage 5: implement analyze(...)");

        // Stage 5 orchestration block enabled.

        RoutingDecision decision = coordinatorAgent.execute(userMessage, conversationId);

        if (decision.getFinishReason() != RoutingDecision.FinishReason.COMPLETED) {
            String answer = nonBlank(decision.getFinalResponse(), decision.getNextPrompt());
            return new AnalysisResponse(answer, decision, null, null, null, null, null);
        }

        ExecutionPlan plan = coordinatorAgent.createPlan(decision);
        AnalysisRequest request = coordinatorAgent.toAnalysisRequest(decision);
        MarketDataResult marketData = plan.selectedAgents().contains(AgentType.MARKET_DATA)
                ? marketDataAgent.execute(request.ticker(), request.question())
                : null;
        FundamentalsResult fundamentals = plan.selectedAgents().contains(AgentType.FUNDAMENTALS)
                ? fundamentalsAgent.execute(request.ticker(), request.question())
                : null;
        NewsResult news = plan.selectedAgents().contains(AgentType.NEWS)
                ? newsAgent.execute(request.ticker(), request.question())
                : null;
        SynthesisResult synthesis = synthesisAgent.execute(request, marketData, fundamentals, news);

        return new AnalysisResponse(synthesis.finalAnswer(), decision, plan, marketData, fundamentals, news, synthesis);
    }

    private String nonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
