package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

import java.util.ArrayList;
import java.util.List;

import com.redis.workshop.springai.multiagents.agent.orchestration.AgentType;

public class RoutingDecision {

    public enum FinishReason {
        NEEDS_MORE_INPUT,
        DIRECT_RESPONSE,
        COMPLETED,
        CANNOT_PROCEED,
        OUT_OF_SCOPE
    }

    private FinishReason finishReason;
    private String nextPrompt;
    private String finalResponse;
    private String resolvedTicker;
    private String resolvedQuestion;
    private List<AgentType> selectedAgents = new ArrayList<>();
    private String reasoning;

    public RoutingDecision() {
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public String getNextPrompt() {
        return nextPrompt;
    }

    public void setNextPrompt(String nextPrompt) {
        this.nextPrompt = nextPrompt;
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }

    public String getResolvedTicker() {
        return resolvedTicker;
    }

    public void setResolvedTicker(String resolvedTicker) {
        this.resolvedTicker = resolvedTicker;
    }

    public String getResolvedQuestion() {
        return resolvedQuestion;
    }

    public void setResolvedQuestion(String resolvedQuestion) {
        this.resolvedQuestion = resolvedQuestion;
    }

    public List<AgentType> getSelectedAgents() {
        return selectedAgents;
    }

    public void setSelectedAgents(List<AgentType> selectedAgents) {
        this.selectedAgents = selectedAgents;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
