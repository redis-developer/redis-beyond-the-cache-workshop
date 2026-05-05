package com.redis.workshop.springai.multiagents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Stage 3 import to enable:
import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;
// Stage 5 import to enable:
import com.redis.workshop.springai.multiagents.agent.orchestration.AnalysisResponse;
import com.redis.workshop.springai.multiagents.agent.orchestration.AgentOrchestrationService;

@Service
public class MultiAgentsService {

    private static final String MODULE_NAME = "building-multi-agents";
    private static final String PLACEHOLDER_ANSWER = """
            Multi agent orchestration is not implemented yet.
            This placeholder proves that the learner app, controller, and service path are ready.
            """;

    private final String apiKey;
    private final String model;

    // Stage 3 field to enable:
    private final MarketDataAgent marketDataAgent;
    // Stage 5 field to enable:
    private final AgentOrchestrationService orchestrationService;

    public MultiAgentsService(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model
            // Stage 3 constructor parameter to enable:
            , MarketDataAgent marketDataAgent
            // Stage 5 constructor parameter to enable:
            , AgentOrchestrationService orchestrationService
    ) {
        this.apiKey = apiKey;
        this.model = model;
        // Stage 3 assignment to enable:
        this.marketDataAgent = marketDataAgent;
        // Stage 5 assignment to enable:
        this.orchestrationService = orchestrationService;
    }

    public MultiAgentsStatusResponse status() {
        return new MultiAgentsStatusResponse(MODULE_NAME, apiKeyConfigured(), model, "ready");
    }

    public MultiAgentsWorkflowResponse workflow() {
        return new MultiAgentsWorkflowResponse(List.of(
                new MultiAgentSummary("Coordinator Agent", "Decides what should run", "design"),
                new MultiAgentSummary("Market Data Agent", "Fetches current price context", "stage 3"),
                new MultiAgentSummary("Fundamentals Agent", "Fetches simple financial context", "final flow"),
                new MultiAgentSummary("News Agent", "Fetches recent headline context", "final flow"),
                new MultiAgentSummary("Synthesis Agent", "Turns specialist outputs into the final answer", "stage 5")
        ), "Stage 5 connects the coordinator and orchestration service.");
    }

    public MultiAgentsRunResponse run(MultiAgentsRunRequest request) {
        String task = nonBlank(request.task(), "What is the current price for Duolingo?");
        String conversationId = nonBlank(request.conversationId(), "building-multi-agents-demo");
        String userMessage = userMessage(request, task);
        // return new MultiAgentsRunResponse(true, PLACEHOLDER_ANSWER.trim(), List.of(
        //         new MultiAgentStep("Coordinator Agent", "Received: " + userMessage),
        //         new MultiAgentStep("Market Data Agent", "Stage 3 will run this specialist."),
        //         new MultiAgentStep("Fundamentals Agent", "Stage 5 will run this specialist."),
        //         new MultiAgentStep("News Agent", "Stage 5 will run this specialist."),
        //         new MultiAgentStep("Synthesis Agent", "Later stages will combine specialist outputs.")
        // ), null, null, null, null, null, null);

        // Stage 3 run block was used before orchestration was enabled.
        // var result = marketDataAgent.execute(ticker, task);

        // return new MultiAgentsRunResponse(true, result.getMessage(), List.of(
        //         new MultiAgentStep("Coordinator Agent", "Using the requested ticker " + ticker + "."),
        //         new MultiAgentStep("Market Data Agent", "Fetched a market snapshot with a Spring AI tool."),
        //         new MultiAgentStep("Fundamentals Agent", "Not implemented yet."),
        //         new MultiAgentStep("News Agent", "Not implemented yet."),
        //         new MultiAgentStep("Synthesis Agent", "Not implemented yet.")
        // ), result, null, null, null, null, null);

        // Stage 5 orchestration run block enabled.

        var analysis = orchestrationService.analyze(userMessage, conversationId);

        return new MultiAgentsRunResponse(
                true,
                analysis.answer(),
                stepsFor(analysis),
                analysis.marketData(),
                analysis.fundamentals(),
                analysis.news(),
                analysis.synthesis(),
                analysis,
                null
        );
    }

    private boolean apiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String userMessage(MultiAgentsRunRequest request, String task) {
        if (request.ticker() == null || request.ticker().isBlank()) {
            return task;
        }

        return request.ticker().trim().toUpperCase() + " " + task;
    }

    private List<MultiAgentStep> stepsFor(AnalysisResponse analysis) {
        List<MultiAgentStep> steps = new ArrayList<>();
        steps.add(new MultiAgentStep("Coordinator Agent", "Created a routing decision."));

        if (analysis.marketData() != null) {
            steps.add(new MultiAgentStep("Market Data Agent", "Fetched current price context."));
        }
        if (analysis.fundamentals() != null) {
            steps.add(new MultiAgentStep("Fundamentals Agent", "Fetched company fundamentals."));
        }
        if (analysis.news() != null) {
            steps.add(new MultiAgentStep("News Agent", "Fetched headline context."));
        }
        if (analysis.synthesis() != null) {
            steps.add(new MultiAgentStep("Synthesis Agent", "Combined selected outputs into one answer."));
        }

        return List.copyOf(steps);
    }
}
