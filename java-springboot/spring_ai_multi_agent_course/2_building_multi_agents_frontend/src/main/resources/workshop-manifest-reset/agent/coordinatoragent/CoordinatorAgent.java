package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import com.redis.workshop.springai.multiagents.agent.orchestration.AgentType;
import com.redis.workshop.springai.multiagents.agent.orchestration.AnalysisRequest;

// Stage 4 import to enable:
// import org.springframework.stereotype.Service;

/*
Stage 4 annotation to enable:
@Service
*/
public class CoordinatorAgent {

    private final CoordinatorRoutingAgent coordinatorRoutingAgent;

    public CoordinatorAgent(CoordinatorRoutingAgent coordinatorRoutingAgent) {
        this.coordinatorRoutingAgent = coordinatorRoutingAgent;
    }

    public RoutingDecision execute(String userMessage) {
        return coordinatorRoutingAgent.route(userMessage);
    }

    public ExecutionPlan createPlan(RoutingDecision routingDecision) {
        List<AgentType> selectedAgents = new ArrayList<>(new LinkedHashSet<>(selectedSpecialists(routingDecision.getSelectedAgents())));
        if (selectedAgents.isEmpty()) {
            throw new IllegalStateException("Coordinator returned no specialist agents.");
        }
        selectedAgents.add(AgentType.SYNTHESIS);

        return new ExecutionPlan(List.copyOf(selectedAgents), routingDecision.getReasoning());
    }

    public AnalysisRequest toAnalysisRequest(RoutingDecision routingDecision) {
        return new AnalysisRequest(
                routingDecision.getResolvedTicker().trim().toUpperCase(),
                routingDecision.getResolvedQuestion().trim()
        );
    }

    private List<AgentType> selectedSpecialists(List<AgentType> selectedAgents) {
        return (selectedAgents == null ? List.<AgentType>of() : selectedAgents).stream()
                .filter(Objects::nonNull)
                .filter(agentType -> agentType != AgentType.SYNTHESIS)
                .distinct()
                .toList();
    }
}
