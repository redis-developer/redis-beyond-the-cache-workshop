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
        try {
            // Use conversationId with model name to get token percentage
            WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
                    parseSessionId(conversationId),
                    parseUserId(conversationId),
                    namespace,      // namespace
                    "gpt-4o-mini",  // modelName - enables token calculation
                    null            // contextWindowMax
            );
            return response != null ? response.getContextPercentageTotalUsed() : null;
        } catch (Exception e) {
            return null;
        }
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
        System.out.println("[AMS] findByConversationId - conversationId: " + conversationId);
        try {
            WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
                    parseSessionId(conversationId),
                    parseUserId(conversationId),      // userId
                    namespace,           // namespace
                    null,           // modelName
                    null            // contextWindowMax
            );
            if (response == null || response.getMessages() == null) {
                System.out.println("[AMS] No messages found");
                return List.of();
            }

            System.out.println("[AMS] Found " + response.getMessages().size() + " messages in AMS");
            List<Message> messages = new ArrayList<>();
            for (MemoryMessage msg : response.getMessages()) {
                Message springMessage = convertToSpringMessage(msg);
                if (springMessage != null) {
                    messages.add(springMessage);
                }
            }
            return messages;
        } catch (Exception e) {
            System.out.println("[AMS] findByConversationId error: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    // Default TTL: 30 minutes (1800 seconds)
    private static final int DEFAULT_TTL_SECONDS = 1800;

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // Use full conversationId (userId:sessionId) as the AMS session key
        System.out.println("[AMS] saveAll - conversationId: " + conversationId);
        System.out.println("[AMS] Incoming messages count: " + messages.size());



        String userId = parseUserId(conversationId);
        String sessionId = parseSessionId(conversationId);

        try {
            // Check if session exists and load existing messages for deduplication
            List<MemoryMessage> existingMessages = new ArrayList<>();
            boolean sessionExists = false;

            try {
                WorkingMemoryResponse existing = client.workingMemory().getWorkingMemory(
                        sessionId,
                        userId,
                        namespace,
                        null,           // modelName
                        null            // contextWindowMax
                );
                if (existing != null) {
                    sessionExists = true;  // Session exists (even if empty)
                    if (existing.getMessages() != null) {
                        existingMessages.addAll(existing.getMessages());
                    }
                }
            } catch (Exception e) {
                // Session doesn't exist yet
                sessionExists = false;
            }

            System.out.println("[AMS] Session exists: " + sessionExists + ", existing messages: " + existingMessages.size());

            // Filter out messages that already exist
            List<MemoryMessage> newMessages = new ArrayList<>();
            for (Message msg : messages) {
                MemoryMessage amsMsg = convertToAmsMessage(msg);
                if (amsMsg != null && !isDuplicate(amsMsg, existingMessages)) {
                    newMessages.add(amsMsg);
                }
            }

            System.out.println("[AMS] New messages to append: " + newMessages.size());

            if (!newMessages.isEmpty()) {
                boolean isFirstMessage = existingMessages.isEmpty();

                // Always use append - it works reliably
                client.workingMemory().appendMessagesToWorkingMemory(
                        sessionId,
                        newMessages,
                        namespace,      // namespace
                        null,  // modelName
                        3000,           // contextWindowMax
                        userId            // userId
                );
                System.out.println("[AMS] Appended " + newMessages.size() + " messages");

                // Set TTL on first message by updating session metadata
                if (isFirstMessage) {
                    try {
                        // Get the session we just created (with namespace)
                        WorkingMemoryResponse current = client.workingMemory().getWorkingMemory(
                                sessionId,
                                userId,
                                namespace,           // namespace
                                null,           // modelName
                                null            // contextWindowMax
                        );
                        if (current != null && current.getMessages() != null) {
                            // Update with TTL
                            WorkingMemory withTtl = WorkingMemory.builder()
                                    .namespace(namespace)
                                    .sessionId(sessionId)
                                    .messages(current.getMessages())
                                    .userId(userId)
                                    .ttlSeconds(DEFAULT_TTL_SECONDS)
                                    .build();
                            client.workingMemory().putWorkingMemory(
                                    sessionId,
                                    withTtl,
                                    userId,
                                    namespace,           // namespace
                                    "gpt-4o-mini",  // modelName
                                    null            // contextWindowMax
                            );
                            System.out.println("[AMS] Set TTL: " + DEFAULT_TTL_SECONDS + "s");
                        }
                    } catch (Exception e) {
                        System.out.println("[AMS] Warning: Could not set TTL: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("[AMS] No new messages to append (all duplicates)");
            }
        } catch (Exception e) {
            System.out.println("[AMS] Save failed: " + e.getMessage());
            throw new RuntimeException("Failed to save conversation to AMS", e);
        }
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
