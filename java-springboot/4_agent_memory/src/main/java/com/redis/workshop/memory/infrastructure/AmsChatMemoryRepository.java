package com.redis.workshop.memory.infrastructure;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.exceptions.MemoryClientException;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
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

    private final MemoryAPIClient client;

    public AmsChatMemoryRepository(MemoryAPIClient client) {
        this.client = client;
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
        String sessionId = parseSessionId(conversationId);
        String userId = parseUserId(conversationId);
        System.out.println("[AMS] findByConversationId - session: " + sessionId + ", user: " + userId);
        try {
            WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(sessionId);
            if (response == null) {
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
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String sessionId = parseSessionId(conversationId);
        String userId = parseUserId(conversationId);
        System.out.println("[AMS] saveAll - session: " + sessionId + ", user: " + userId);
        System.out.println("[AMS] Incoming messages count: " + messages.size());

        try {
            // Load existing messages to deduplicate
            List<MemoryMessage> existingMessages = new ArrayList<>();
            try {
                WorkingMemoryResponse existing = client.workingMemory().getWorkingMemory(sessionId);
                if (existing != null && existing.getMessages() != null) {
                    existingMessages.addAll(existing.getMessages());
                }
            } catch (Exception e) {
                // No existing session
            }

            System.out.println("[AMS] Existing messages: " + existingMessages.size());

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
                client.workingMemory().appendMessagesToWorkingMemory(sessionId, newMessages);
                System.out.println("[AMS] Appended " + newMessages.size() + " messages successfully");
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
        String sessionId = parseSessionId(conversationId);
        try {
            client.workingMemory().deleteWorkingMemory(sessionId);
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

