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
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(RoutingDecision.class);

        if (decision == null || decision.getFinishReason() == null) {
            throw new IllegalStateException("Coordinator returned an invalid routing decision.");
        }

        return decision;
    }
}
