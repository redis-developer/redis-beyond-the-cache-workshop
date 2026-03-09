package com.redis.workshop.memory.infrastructure;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResult;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResults;
import com.redis.agentmemory.models.longtermemory.SearchRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI Advisor that retrieves relevant long-term memories from AMS
 * and injects them into the system prompt before sending to the LLM.
 *
 * This uses semantic search - the user's message is used as the query
 * to find relevant memories via vector similarity.
 */
public class LongTermMemoryAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_MEMORIES = "long_term_memory_retrieved";

    private static final int DEFAULT_ORDER = 100; // Run after MessageChatMemoryAdvisor

    private final MemoryAPIClient client;
    private final AmsChatMemoryRepository memoryRepository;
    private final int maxMemories;
    private final int order;

    public LongTermMemoryAdvisor(MemoryAPIClient client) {
        this(client, null, 5, DEFAULT_ORDER);
    }

    public LongTermMemoryAdvisor(MemoryAPIClient client, AmsChatMemoryRepository memoryRepository) {
        this(client, memoryRepository, 5, DEFAULT_ORDER);
    }

    public LongTermMemoryAdvisor(MemoryAPIClient client, AmsChatMemoryRepository memoryRepository, int maxMemories, int order) {
        this.client = client;
        this.memoryRepository = memoryRepository;
        this.maxMemories = maxMemories;
        this.order = order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        // Get conversationId from context (set by ChatMemory advisor)
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String userId = parseUserId(conversationId);

        if (userId == null || userId.equals("anonymous")) {
            return request;
        }

        // Get the user's message to use as semantic search query
        String userMessage = request.prompt().getUserMessage().getText();
        if (userMessage == null || userMessage.isBlank()) {
            return request;
        }

        // Search long-term memory
        List<String> memories = searchMemories(userMessage, userId);

        // Store retrieved memories for observability (even if empty)
        if (memoryRepository != null) {
            memoryRepository.setLastRetrievedMemories(memories);
        }

        if (memories.isEmpty()) {
            return request;
        }

        System.out.println("[LTM Advisor] Found " + memories.size() + " relevant memories for user: " + userId);

        // Build memory context to inject into system prompt
        StringBuilder memoryContext = new StringBuilder();
        memoryContext.append("\n\n--- Long-Term Memories About This User ---\n");
        for (String memory : memories) {
            memoryContext.append("- ").append(memory).append("\n");
        }
        memoryContext.append("--- Use these memories to personalize your response ---\n");

        // Store retrieved memories in context for later use
        return request.mutate()
                .prompt(request.prompt().augmentSystemMessage(memoryContext.toString()))
                .context(RETRIEVED_MEMORIES, memories)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        // No post-processing needed
        return response;
    }

    private String parseUserId(String conversationId) {
        if (conversationId == null) return null;
        int idx = conversationId.indexOf(AmsChatMemoryRepository.SEPARATOR);
        return idx > 0 ? conversationId.substring(0, idx) : null;
    }

    private List<String> searchMemories(String query, String userId) {
        List<String> results = new ArrayList<>();
        try {
            // Note: Recency boosting (recencyBoost, recencySemanticWeight, recencyRecencyWeight)
            // is supported by the AMS REST API but not yet available in SDK v0.1.0
            SearchRequest searchRequest = SearchRequest.builder()
                    .text(query)
                    .userId(userId)
                    .limit(maxMemories)
                    .build();

            MemoryRecordResults response = client.longTermMemory().searchLongTermMemories(searchRequest);
            if (response != null && response.getMemories() != null) {
                for (MemoryRecordResult memory : response.getMemories()) {
                    if (memory.getText() != null) {
                        results.add(memory.getText());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[LTM Advisor] Search failed: " + e.getMessage());
        }
        return results;
    }

    public static Builder builder(MemoryAPIClient client) {
        return new Builder(client);
    }

    public static class Builder {
        private final MemoryAPIClient client;
        private AmsChatMemoryRepository memoryRepository;
        private int maxMemories = 5;
        private int order = DEFAULT_ORDER;

        public Builder(MemoryAPIClient client) {
            this.client = client;
        }

        public Builder memoryRepository(AmsChatMemoryRepository memoryRepository) {
            this.memoryRepository = memoryRepository;
            return this;
        }

        public Builder maxMemories(int maxMemories) {
            this.maxMemories = maxMemories;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public LongTermMemoryAdvisor build() {
            return new LongTermMemoryAdvisor(client, memoryRepository, maxMemories, order);
        }
    }
}

