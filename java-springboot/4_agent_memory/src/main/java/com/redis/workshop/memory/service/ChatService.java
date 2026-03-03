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
 *
 * Advisors are executed in order (lower order value = earlier execution).
 * The MessageChatMemoryAdvisor runs first (order 0) to load conversation history,
 * then LongTermMemoryAdvisor (order 100) searches for relevant memories.
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = "You are a helpful AI assistant with memory. You remember things about the user.";

    private final ChatMemory chatMemory;
    private final LongTermMemoryAdvisor longTermMemoryAdvisor;
    private final MemoryAPIClient memoryClient;
    private final Map<String, ChatClient> chatClients = new ConcurrentHashMap<>();

    public ChatService(ChatMemory chatMemory, LongTermMemoryAdvisor longTermMemoryAdvisor, MemoryAPIClient memoryClient) {
        this.chatMemory = chatMemory;
        this.longTermMemoryAdvisor = longTermMemoryAdvisor;
        this.memoryClient = memoryClient;
    }

    public record ChatRequest(String sessionId, String userId, String message, String apiKey, boolean useMemory) {}
    public record ChatResponse(String response, List<String> relevantMemories, int messageCount) {}

    public ChatResponse chat(ChatRequest request) {
        ChatClient chatClient = getOrCreateChatClient(request.apiKey(), request.useMemory());

        // TODO: Step 7 - Create conversationId with userId:sessionId format for AMS
        // String conversationId = AmsChatMemoryRepository.createConversationId(
        //         request.userId() != null ? request.userId() : "anonymous",
        //         request.sessionId()
        // );
        String conversationId = request.sessionId();

        // TODO: Step 8 - Pass conversationId to advisors at runtime
        // The advisors use this to load/save memory for the correct user session.
        // String response = chatClient.prompt()
        //         .system(SYSTEM_PROMPT)
        //         .user(request.message())
        //         .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
        //         .call()
        //         .content();
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
                .call()
                .content();

        List<String> relevantMemories = new ArrayList<>();
        return new ChatResponse(response, relevantMemories, 0);
    }

    private ChatClient getOrCreateChatClient(String apiKey, boolean useLongTermMemory) {
        String cacheKey = apiKey + ":" + useLongTermMemory;

        return chatClients.computeIfAbsent(cacheKey, key -> {
            OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.7).build())
                    .build();

            // TODO: Step 9 - Add MessageChatMemoryAdvisor for working memory
            // This advisor automatically loads conversation history before the LLM call
            // and saves the response after. It uses AmsChatMemoryRepository under the hood.
            // var builder = ChatClient.builder(chatModel)
            //         .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
            var builder = ChatClient.builder(chatModel);

            // TODO: Step 10 - Add LongTermMemoryAdvisor for semantic memory retrieval
            // This advisor searches AMS for relevant long-term memories using the user's
            // message as a semantic query, then injects them into the system prompt.
            // if (useLongTermMemory) {
            //     builder.defaultAdvisors(longTermMemoryAdvisor);
            // }

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
}

