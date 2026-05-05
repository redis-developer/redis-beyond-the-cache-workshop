package com.redis.workshop.springai.multiagents.agent.coordinatoragent;

// Stage 4 imports to enable:
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.memory.ChatMemory;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.stereotype.Service;

/*
Stage 4 annotation to enable:
@Service
*/
public class CoordinatorRoutingAgent {

    /*
    Stage 4 field and constructor to enable:

    private final ChatClient coordinatorChatClient;

    public CoordinatorRoutingAgent(@Qualifier("coordinatorChatClient") ChatClient coordinatorChatClient) {
        this.coordinatorChatClient = coordinatorChatClient;
    }
    */

    public RoutingDecision route(String userMessage) {
        throw new UnsupportedOperationException("Stage 4: implement route(...)");

        /*
        Stage 4 route block to enable:
        Comment out the scaffold throw above, then uncomment this block.

        RoutingDecision decision = coordinatorChatClient.prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, ChatMemory.DEFAULT_CONVERSATION_ID))
                .call()
                .entity(RoutingDecision.class);

        if (decision == null || decision.getFinishReason() == null) {
            throw new IllegalStateException("Coordinator returned an invalid routing decision.");
        }

        return decision;
        */
    }
}
