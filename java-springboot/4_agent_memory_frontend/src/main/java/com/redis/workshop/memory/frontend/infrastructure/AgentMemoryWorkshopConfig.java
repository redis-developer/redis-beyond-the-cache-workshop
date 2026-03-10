package com.redis.workshop.memory.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent Memory Workshop Configuration.
 * Defines which files can be edited and their original content for the workshop.
 */
@Component
public class AgentMemoryWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.ofEntries(
        Map.entry("build.gradle.kts", "build.gradle.kts"),
        Map.entry("application.properties", "src/main/resources/application.properties"),
        Map.entry("AgentMemoryService.java", "src/main/java/com/redis/workshop/memory/service/AgentMemoryService.java"),
        Map.entry("ChatService.java", "src/main/java/com/redis/workshop/memory/service/ChatService.java"),
        Map.entry("AmsChatMemoryRepository.java", "src/main/java/com/redis/workshop/memory/infrastructure/AmsChatMemoryRepository.java")
    );

    // ==================== Original File Contents ====================

    private static final String BUILD_GRADLE_ORIGINAL = """
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.redis.workshop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

val springAiVersion = "1.1.2"

dependencies {
    implementation(project(":workshop-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI for OpenAI integration
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Agent Memory Server Java SDK (from Maven Central)
    implementation("com.redis:agent-memory-client-java:0.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
""";

    private static final String APPLICATION_PROPERTIES_ORIGINAL = """
# Application name
spring.application.name=agent-memory

# Server configuration
server.port=${SERVER_PORT:8083}

# Agent Memory Server configuration
agent-memory.server.url=http://localhost:8000
agent-memory.server.namespace=workshop

# Disable authentication for development (AMS default)
# agent-memory.server.auth-token=your-token-here

# Disable Spring AI auto-config that requires API key at startup
spring.ai.openai.audio.speech.enabled=false
spring.ai.openai.audio.transcription.enabled=false
spring.ai.openai.image.enabled=false
spring.ai.openai.moderation.enabled=false
spring.ai.openai.embedding.enabled=false

# Logging
logging.level.com.redis.workshop.memory=INFO
logging.level.org.springframework.web.reactive.function.client=DEBUG
""";

    private static final String AGENT_MEMORY_SERVICE_ORIGINAL = """
package com.redis.workshop.memory.service;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.models.common.AckResponse;
import com.redis.agentmemory.models.health.HealthCheckResponse;
import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResults;
import com.redis.agentmemory.models.longtermemory.MemoryType;
import com.redis.agentmemory.models.longtermemory.SearchRequest;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for interacting with the Agent Memory Server (AMS) using the official Java SDK.
 *
 * The SDK provides type-safe access to:
 * - Working Memory: Session-scoped conversation context (short-term)
 * - Long-Term Memory: Persistent facts with semantic search
 *
 * In this workshop, you will implement the methods to interact with AMS using the SDK.
 */
@Service
public class AgentMemoryService {

    // TODO: Step 0 - Initialize the client field
    // private final MemoryAPIClient client;
    private final MemoryAPIClient client = null; // Placeholder until initialized

    public AgentMemoryService(
            @Value("${agent-memory.server.url}") String serverUrl,
            @Value("${agent-memory.server.namespace:workshop}") String namespace) {
        // TODO: Step 0 - Initialize the MemoryAPIClient
        // this.client = MemoryAPIClient.builder(serverUrl)
        //         .defaultNamespace(namespace)
        //         .timeout(30.0)
        //         .build();
    }

    // ==================== Working Memory Operations ====================

    public WorkingMemoryResponse getWorkingMemory(String sessionId) {
        // TODO: Step 1 - Implement using client.workingMemory().getWorkingMemory(sessionId)
        // try {
        //     return client.workingMemory().getWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get working memory", e);
        // }
        return null; // Placeholder
    }

    public WorkingMemoryResponse putWorkingMemory(String sessionId, WorkingMemory memory) {
        // TODO: Step 2 - Implement using client.workingMemory().putWorkingMemory(sessionId, memory)
        // try {
        //     return client.workingMemory().putWorkingMemory(sessionId, memory);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to put working memory", e);
        // }
        return null; // Placeholder
    }

    public AckResponse deleteWorkingMemory(String sessionId) {
        // TODO: Step 3 - Implement using client.workingMemory().deleteWorkingMemory(sessionId)
        // try {
        //     return client.workingMemory().deleteWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to delete working memory", e);
        // }
        return null; // Placeholder
    }

    // ==================== Long-Term Memory Operations ====================

    public AckResponse createLongTermMemory(List<MemoryRecord> memories) {
        // TODO: Step 4 - Implement using client.longTermMemory().createLongTermMemories(memories)
        // try {
        //     return client.longTermMemory().createLongTermMemories(memories);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to create long-term memory", e);
        // }
        return null; // Placeholder
    }

    public MemoryRecordResults searchLongTermMemory(String text, String userId, int limit) {
        // TODO: Step 5 - Implement using client.longTermMemory().searchLongTermMemories(searchRequest)
        // try {
        //     SearchRequest request = SearchRequest.builder()
        //             .text(text)
        //             .userId(userId)
        //             .limit(limit)
        //             .build();
        //     return client.longTermMemory().searchLongTermMemories(request);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to search long-term memory", e);
        // }
        return null; // Placeholder
    }

    public MemoryRecord getMemory(String memoryId) {
        // TODO: Step 6 - Implement using client.longTermMemory().getLongTermMemory(memoryId)
        // try {
        //     return client.longTermMemory().getLongTermMemory(memoryId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get memory", e);
        // }
        return null; // Placeholder
    }

    public AckResponse deleteMemories(List<String> memoryIds) {
        // TODO: Step 7 - Implement using client.longTermMemory().deleteLongTermMemories(memoryIds)
        // try {
        //     return client.longTermMemory().deleteLongTermMemories(memoryIds);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to delete memories", e);
        // }
        return null; // Placeholder
    }

    // ==================== Health Check ====================

    public HealthCheckResponse healthCheck() {
        // TODO: Implement using client.health().healthCheck()
        // try {
        //     return client.health().healthCheck();
        // } catch (Exception e) {
        //     throw new RuntimeException("Health check failed", e);
        // }
        return null; // Placeholder
    }

    // ==================== Helper Methods ====================

    public static MemoryMessage createMessage(String role, String content) {
        return MemoryMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    public static MemoryRecord createMemoryRecord(String text, String sessionId, String userId) {
        return MemoryRecord.builder()
                .text(text)
                .sessionId(sessionId)
                .userId(userId)
                .memoryType(MemoryType.SEMANTIC)
                .build();
    }
}
""";

    private static final String CHAT_SERVICE_ORIGINAL = """
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

        // TODO: Step 12 - Add advisor params to pass conversationId
        // Change the call below to include: .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
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

            // TODO: Step 13 - Add MessageChatMemoryAdvisor to enable working memory
            // Change: var builder = ChatClient.builder(chatModel);
            // To: var builder = ChatClient.builder(chatModel)
            //         .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
            var builder = ChatClient.builder(chatModel);

            // TODO: Step 14 - Add LongTermMemoryAdvisor for semantic memory search
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
""";

    private static final String AMS_CHAT_MEMORY_REPOSITORY_ORIGINAL = """
package com.redis.workshop.memory.infrastructure;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.exceptions.MemoryClientException;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI ChatMemoryRepository implementation backed by Agent Memory Server.
 *
 * This allows Spring AI's ChatMemory abstraction to use AMS for storing
 * conversation history (working memory).
 *
 * ConversationId format: "userId:sessionId" - allows passing both user and session context.
 */
public class AmsChatMemoryRepository implements ChatMemoryRepository {

    public static final String SEPARATOR = ":";
    private static final String DEFAULT_NAMESPACE = "workshop";

    private final MemoryAPIClient client;
    private final String namespace;

    // Observability: store memories retrieved by LongTermMemoryAdvisor
    private volatile List<String> lastRetrievedMemories = List.of();

    public AmsChatMemoryRepository(MemoryAPIClient client) {
        this(client, DEFAULT_NAMESPACE);
    }

    public AmsChatMemoryRepository(MemoryAPIClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    /** Store memories retrieved by the advisor for UI display */
    public void setLastRetrievedMemories(List<String> memories) {
        this.lastRetrievedMemories = memories != null ? memories : List.of();
    }

    /** Get memories retrieved during the last chat request */
    public List<String> getLastRetrievedMemories() {
        return lastRetrievedMemories;
    }

    /**
     * Helper to create a conversationId from userId and sessionId.
     */
    public static String createConversationId(String userId, String sessionId) {
        return userId + SEPARATOR + sessionId;
    }

    /**
     * Parse the sessionId from a conversationId.
     */
    private String parseSessionId(String conversationId) {
        if (conversationId == null) return "default";
        int idx = conversationId.indexOf(SEPARATOR);
        return idx > 0 ? conversationId.substring(idx + 1) : conversationId;
    }

    /**
     * Parse the userId from a conversationId.
     */
    private String parseUserId(String conversationId) {
        if (conversationId == null) return null;
        int idx = conversationId.indexOf(SEPARATOR);
        return idx > 0 ? conversationId.substring(0, idx) : null;
    }

    /**
     * Get context usage percentage for a conversation.
     * Returns the percentage of context window used (0.0 to 1.0), or null if unavailable.
     */
    public Double getContextPercentage(String conversationId) {
        // TODO: Step 9 - Implement getContextPercentage
        // try {
        //     // Use conversationId with model name to get token percentage
        //     WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
        //             parseSessionId(conversationId),
        //             parseUserId(conversationId),
        //             namespace,      // namespace
        //             "gpt-4o-mini",  // modelName - enables token calculation
        //             null            // contextWindowMax
        //     );
        //     return response != null ? response.getContextPercentageTotalUsed() : null;
        // } catch (Exception e) {
        //     return null;
        // }
        return null; // Placeholder
    }

    @Override
    public List<String> findConversationIds() {
        try {
            return client.workingMemory().listSessions().getSessions();
        } catch (MemoryClientException e) {
            System.out.println("[AMS] Error loading sessions: " + e.getMessage());
        }
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // TODO: Step 10 - Implement findByConversationId
        // System.out.println("[AMS] findByConversationId - conversationId: " + conversationId);
        // try {
        //     WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
        //             parseSessionId(conversationId),
        //             parseUserId(conversationId),      // userId
        //             namespace,           // namespace
        //             null,           // modelName
        //             null            // contextWindowMax
        //     );
        //     if (response == null || response.getMessages() == null) {
        //         System.out.println("[AMS] No messages found");
        //         return List.of();
        //     }
        //
        //     System.out.println("[AMS] Found " + response.getMessages().size() + " messages in AMS");
        //     List<Message> messages = new ArrayList<>();
        //     for (MemoryMessage msg : response.getMessages()) {
        //         Message springMessage = convertToSpringMessage(msg);
        //         if (springMessage != null) {
        //             messages.add(springMessage);
        //         }
        //     }
        //     return messages;
        // } catch (Exception e) {
        //     System.out.println("[AMS] findByConversationId error: " + e.getMessage());
        //     e.printStackTrace();
        //     return List.of();
        // }
        return List.of(); // Placeholder
    }

    // Default TTL: 30 minutes (1800 seconds)
    private static final int DEFAULT_TTL_SECONDS = 1800;

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // TODO: Step 11 - Implement saveAll
        // // Use full conversationId (userId:sessionId) as the AMS session key
        // System.out.println("[AMS] saveAll - conversationId: " + conversationId);
        // System.out.println("[AMS] Incoming messages count: " + messages.size());
        //
        // String userId = parseUserId(conversationId);
        // String sessionId = parseSessionId(conversationId);
        //
        // try {
        //     // Check if session exists and load existing messages for deduplication
        //     List<MemoryMessage> existingMessages = new ArrayList<>();
        //     boolean sessionExists = false;
        //
        //     try {
        //         WorkingMemoryResponse existing = client.workingMemory().getWorkingMemory(
        //                 sessionId,
        //                 userId,
        //                 namespace,
        //                 null,           // modelName
        //                 null            // contextWindowMax
        //         );
        //         if (existing != null) {
        //             sessionExists = true;  // Session exists (even if empty)
        //             if (existing.getMessages() != null) {
        //                 existingMessages.addAll(existing.getMessages());
        //             }
        //         }
        //     } catch (Exception e) {
        //         // Session doesn't exist yet
        //         sessionExists = false;
        //     }
        //
        //     System.out.println("[AMS] Session exists: " + sessionExists + ", existing messages: " + existingMessages.size());
        //
        //     // Filter out messages that already exist
        //     List<MemoryMessage> newMessages = new ArrayList<>();
        //     for (Message msg : messages) {
        //         MemoryMessage amsMsg = convertToAmsMessage(msg);
        //         if (amsMsg != null && !isDuplicate(amsMsg, existingMessages)) {
        //             newMessages.add(amsMsg);
        //         }
        //     }
        //
        //     System.out.println("[AMS] New messages to append: " + newMessages.size());
        //
        //     if (!newMessages.isEmpty()) {
        //         boolean isFirstMessage = existingMessages.isEmpty();
        //
        //         // Always use append - it works reliably
        //         client.workingMemory().appendMessagesToWorkingMemory(
        //                 sessionId,
        //                 newMessages,
        //                 namespace,      // namespace
        //                 "gpt-4o-mini",  // modelName
        //                 null,           // contextWindowMax
        //                 userId            // userId
        //         );
        //         System.out.println("[AMS] Appended " + newMessages.size() + " messages");
        //
        //         // Set TTL on first message by updating session metadata
        //         if (isFirstMessage) {
        //             try {
        //                 // Get the session we just created (with namespace)
        //                 WorkingMemoryResponse current = client.workingMemory().getWorkingMemory(
        //                         sessionId,
        //                         userId,
        //                         namespace,           // namespace
        //                         null,           // modelName
        //                         null            // contextWindowMax
        //                 );
        //                 if (current != null && current.getMessages() != null) {
        //                     // Update with TTL
        //                     WorkingMemory withTtl = WorkingMemory.builder()
        //                             .namespace(namespace)
        //                             .sessionId(sessionId)
        //                             .messages(current.getMessages())
        //                             .userId(userId)
        //                             .ttlSeconds(DEFAULT_TTL_SECONDS)
        //                             .build();
        //                     client.workingMemory().putWorkingMemory(
        //                             sessionId,
        //                             withTtl,
        //                             userId,
        //                             namespace,           // namespace
        //                             "gpt-4o-mini",  // modelName
        //                             null            // contextWindowMax
        //                     );
        //                     System.out.println("[AMS] Set TTL: " + DEFAULT_TTL_SECONDS + "s");
        //                 }
        //             } catch (Exception e) {
        //                 System.out.println("[AMS] Warning: Could not set TTL: " + e.getMessage());
        //             }
        //         }
        //     } else {
        //         System.out.println("[AMS] No new messages to append (all duplicates)");
        //     }
        // } catch (Exception e) {
        //     System.out.println("[AMS] Save failed: " + e.getMessage());
        //     throw new RuntimeException("Failed to save conversation to AMS", e);
        // }
    }

    private boolean isDuplicate(MemoryMessage newMsg, List<MemoryMessage> existing) {
        return existing.stream().anyMatch(m ->
                m.getRole().equals(newMsg.getRole()) &&
                        m.getContent().equals(newMsg.getContent())
        );
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            client.workingMemory().deleteWorkingMemory(parseSessionId(conversationId), parseUserId(conversationId), namespace);
        } catch (Exception e) {
            // Ignore if session doesn't exist
        }
    }

    private Message convertToSpringMessage(MemoryMessage msg) {
        if (msg == null || msg.getRole() == null) return null;

        String role = msg.getRole();
        String content = msg.getContent() != null ? msg.getContent() : "";

        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> null;
        };
    }

    private MemoryMessage convertToAmsMessage(Message msg) {
        if (msg == null) return null;

        String role = switch (msg.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            default -> null;
        };

        if (role == null) return null;

        return MemoryMessage.builder()
                .role(role)
                .content(msg.getText())
                .build();
    }
}
""";

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.ofEntries(
        Map.entry("build.gradle.kts", BUILD_GRADLE_ORIGINAL),
        Map.entry("application.properties", APPLICATION_PROPERTIES_ORIGINAL),
        Map.entry("AgentMemoryService.java", AGENT_MEMORY_SERVICE_ORIGINAL),
        Map.entry("ChatService.java", CHAT_SERVICE_ORIGINAL),
        Map.entry("AmsChatMemoryRepository.java", AMS_CHAT_MEMORY_REPOSITORY_ORIGINAL)
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "4_agent_memory";
    }

    @Override
    public String getWorkshopTitle() {
        return "Agent Memory Server";
    }

    @Override
    public String getWorkshopDescription() {
        return "Give your AI agents persistent memory with Redis-powered Agent Memory Server.";
    }
}
