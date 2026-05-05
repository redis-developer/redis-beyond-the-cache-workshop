package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

import java.util.List;

import com.redis.workshop.springai.multiagents.agent.orchestration.AgentType;

public record ExecutionPlan(
        List<AgentType> selectedAgents,
        String routingReasoning
) {
}
