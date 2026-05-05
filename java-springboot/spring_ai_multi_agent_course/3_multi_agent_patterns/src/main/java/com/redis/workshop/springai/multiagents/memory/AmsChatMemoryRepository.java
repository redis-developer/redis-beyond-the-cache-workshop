package com.redis.workshop.springai.multiagents.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import com.redis.workshop.springai.multiagents.memory.service.AgentMemoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

public class AmsChatMemoryRepository implements ChatMemoryRepository {

    public static final String SEPARATOR = ":";

    private static final Logger log = LoggerFactory.getLogger(AmsChatMemoryRepository.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_TTL_SECONDS = 1800;
    private static final String MEMORY_MODEL = "gpt-4o";

    private final AgentMemoryService agentMemoryService;
    private List<String> lastRetrievedMemories = List.of();

    public AmsChatMemoryRepository(AgentMemoryService agentMemoryService) {
        this.agentMemoryService = agentMemoryService;
    }

    public static String createConversationId(String userId, String sessionId) {
        return userId + SEPARATOR + sessionId;
    }

    public void setLastRetrievedMemories(List<String> memories) {
        this.lastRetrievedMemories = memories == null ? List.of() : List.copyOf(memories);
    }

    public List<String> getLastRetrievedMemories() {
        return lastRetrievedMemories;
    }

    @Override
    public List<String> findConversationIds() {
        return callOrDefault("list memory sessions", agentMemoryService::listSessions, List.of());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // Stage 2: replace this method body with the working-memory load block.
        return List.of();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // Stage 3: replace this method body with the working-memory save block.
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        runSafely("delete working memory for conversation " + conversationId, () -> {
            agentMemoryService.deleteWorkingMemory(
                    parseSessionId(conversationId),
                    parseUserId(conversationId)
            );
        });
    }

    private String parseSessionId(String conversationId) {
        if (conversationId == null) {
            return "default";
        }
        int separatorIndex = conversationId.indexOf(SEPARATOR);
        return separatorIndex > 0 ? conversationId.substring(separatorIndex + 1) : conversationId;
    }

    private String parseUserId(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        int separatorIndex = conversationId.indexOf(SEPARATOR);
        return separatorIndex > 0 ? conversationId.substring(0, separatorIndex) : null;
    }

    private boolean isDuplicate(MemoryMessage newMessage, List<MemoryMessage> existingMessages) {
        return existingMessages.stream().anyMatch(message ->
                message.getRole().equals(newMessage.getRole())
                        && message.getContent().equals(newMessage.getContent())
        );
    }

    private Message convertToSpringMessage(MemoryMessage message) {
        if (message == null || message.getRole() == null) {
            return null;
        }

        String content = message.getContent() == null ? "" : message.getContent();
        if ("assistant".equals(message.getRole())) {
            content = normalizeAssistantContent(content);
        }

        return switch (message.getRole()) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> null;
        };
    }

    private MemoryMessage convertToAmsMessage(Message message) {
        if (message == null) {
            return null;
        }

        String role = switch (message.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            default -> null;
        };

        if (role == null) {
            return null;
        }

        String content = message.getText();
        if ("assistant".equals(role)) {
            content = normalizeAssistantContent(content);
        }

        return MemoryMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    private String normalizeAssistantContent(String content) {
        if (!isStructuredAgentResponse(content)) {
            return content;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            String finalResponse = textValue(root, "finalResponse");
            if (!finalResponse.isBlank()) {
                return finalResponse;
            }

            String nextPrompt = textValue(root, "nextPrompt");
            return nextPrompt.isBlank() ? content : nextPrompt;
        } catch (JsonProcessingException e) {
            return content;
        }
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private boolean isStructuredAgentResponse(String content) {
        if (content == null) {
            return false;
        }

        String value = content.trim();
        return value.startsWith("{")
                && value.endsWith("}")
                && value.contains("\"finishReason\"")
                && (value.contains("\"selectedAgents\"") || value.contains("\"resolvedTicker\""));
    }

    private WorkingMemoryResponse loadWorkingMemory(String conversationId) {
        return callOrDefault(
                "load working memory for conversation " + conversationId,
                () -> agentMemoryService.getWorkingMemory(
                        parseSessionId(conversationId),
                        parseUserId(conversationId),
                        null
                ),
                null
        );
    }

    private List<MemoryMessage> existingMessages(String conversationId) {
        WorkingMemoryResponse existing = loadWorkingMemory(conversationId);
        if (existing == null || existing.getMessages() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(existing.getMessages());
    }

    private List<MemoryMessage> toNewMessages(List<Message> messages, List<MemoryMessage> existingMessages) {
        List<MemoryMessage> newMessages = new ArrayList<>();
        for (Message message : messages) {
            MemoryMessage amsMessage = convertToAmsMessage(message);
            if (amsMessage != null && !isDuplicate(amsMessage, existingMessages)) {
                newMessages.add(amsMessage);
            }
        }
        return newMessages;
    }

    private void applyTtl(String sessionId, String userId) {
        WorkingMemoryResponse current = agentMemoryService.getWorkingMemory(sessionId, userId, null);
        if (current == null || current.getMessages() == null) {
            return;
        }

        WorkingMemory withTtl = WorkingMemory.builder()
                .namespace(agentMemoryService.namespace())
                .sessionId(sessionId)
                .messages(current.getMessages())
                .userId(userId)
                .ttlSeconds(DEFAULT_TTL_SECONDS)
                .build();

        agentMemoryService.putWorkingMemory(sessionId, withTtl, userId, MEMORY_MODEL);
    }

    private void runSafely(String action, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            log.warn("Skipping Agent Memory Server action because {} failed.", action, e);
        }
    }

    private <T> T callOrDefault(String action, Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            log.warn("Returning fallback because {} failed.", action, e);
            return fallback;
        }
    }
}
