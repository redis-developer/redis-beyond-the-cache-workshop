package com.redis.workshop.memory.service;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.agentmemory.models.longtermemory.MemoryType;
import com.redis.workshop.memory.infrastructure.AmsChatMemoryRepository;
import com.redis.workshop.memory.infrastructure.LongTermMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat service using Spring AI's ChatClient with AMS-backed memory advisors.
 *
 * Spring AI Advisors intercept and enhance chat requests/responses:
 * - MessageChatMemoryAdvisor: Handles working memory (conversation history)
 * - LongTermMemoryAdvisor: Retrieves relevant long-term memories via semantic search
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = "You are a helpful AI assistant with memory. You remember things about the user.";

    private final ChatMemory chatMemory;
    private final LongTermMemoryAdvisor longTermMemoryAdvisor;
    private final MemoryAPIClient memoryClient;
    private final AmsChatMemoryRepository memoryRepository;
    private final Map<String, ChatClient> chatClients = new ConcurrentHashMap<>();

    public ChatService(ChatMemory chatMemory, LongTermMemoryAdvisor longTermMemoryAdvisor,
                       MemoryAPIClient memoryClient, AmsChatMemoryRepository memoryRepository) {
        this.chatMemory = chatMemory;
        this.longTermMemoryAdvisor = longTermMemoryAdvisor;
        this.memoryClient = memoryClient;
        this.memoryRepository = memoryRepository;
    }

    public record ChatRequest(String sessionId, String userId, String message, String apiKey, boolean useMemory) {}
    public record ChatResponse(String response, List<String> relevantMemories, int messageCount, Double contextPercentage, SessionInfo sessionInfo) {}
    public record SessionInfo(String conversationId, int messageCount, int ttlSeconds) {}

    // Default TTL: 30 minutes
    private static final int DEFAULT_TTL_SECONDS = 1800;

    public ChatResponse chat(ChatRequest request) {
        ChatClient chatClient = getOrCreateChatClient(request.apiKey(), request.useMemory());

        String conversationId = AmsChatMemoryRepository.createConversationId(
                request.userId() != null ? request.userId() : "anonymous",
                request.sessionId()
        );

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        Double contextPercentage = memoryRepository.getContextPercentage(conversationId);
        List<String> relevantMemories = memoryRepository.getLastRetrievedMemories();

        // Get message count from repository
        int msgCount = memoryRepository.findByConversationId(conversationId).size();
        SessionInfo sessionInfo = new SessionInfo(conversationId, msgCount, DEFAULT_TTL_SECONDS);

        return new ChatResponse(response, relevantMemories, msgCount, contextPercentage, sessionInfo);
    }

    private ChatClient getOrCreateChatClient(String apiKey, boolean useLongTermMemory) {
        String cacheKey = apiKey + ":" + useLongTermMemory;

        return chatClients.computeIfAbsent(cacheKey, key -> {
            OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.7).build())
                    .build();

            var builder = ChatClient.builder(chatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());

            if (useLongTermMemory) {
                builder.defaultAdvisors(longTermMemoryAdvisor);
            }

            return builder.build();
        });
    }

    public void storeMemory(String userId, String text) {
        try {
            MemoryRecord mem = MemoryRecord.builder()
                    .text(text)
                    .userId(userId)
                    .memoryType(MemoryType.SEMANTIC)
                    .build();
            memoryClient.longTermMemory().createLongTermMemories(List.of(mem));
            System.out.println("[ChatService] Stored long-term memory for user: " + userId);
        } catch (Exception e) {
            System.err.println("[ChatService] Memory store failed: " + e.getMessage());
        }
    }

    public void clearSession(String userId, String sessionId) {
        try {
            String conversationId = AmsChatMemoryRepository.createConversationId(
                    userId != null ? userId : "anonymous",
                    sessionId
            );
            chatMemory.clear(conversationId);
        } catch (Exception e) { /* ignore */ }
    }

    /**
     * Get all long-term memories for a user.
     * Uses filter-only search (no text) to retrieve all memories for the user.
     */
    public List<MemoryRecord> getLongTermMemories(String userId) {
        try {
            var results = memoryClient.longTermMemory().searchLongTermMemories(
                    com.redis.agentmemory.models.longtermemory.SearchRequest.builder()
                            .userId(userId)
                            .limit(100)
                            .build()
            );
            if (results != null && results.getMemories() != null) {
                return results.getMemories().stream()
                        .map(r -> MemoryRecord.builder()
                                .id(r.getId())
                                .text(r.getText())
                                .userId(r.getUserId())
                                .memoryType(r.getMemoryType())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("[ChatService] Failed to get memories: " + e.getMessage());
        }
        return List.of();
    }
}
