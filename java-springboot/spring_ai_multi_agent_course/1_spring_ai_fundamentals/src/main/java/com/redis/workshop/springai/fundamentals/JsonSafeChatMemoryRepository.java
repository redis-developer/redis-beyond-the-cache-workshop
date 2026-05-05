package com.redis.workshop.springai.fundamentals;

import java.net.URI;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

final class JsonSafeChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;

    JsonSafeChatMemoryRepository(ChatMemoryRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        delegate.saveAll(conversationId, messages.stream()
                .map(this::sanitize)
                .toList());
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }

    private Message sanitize(Message message) {
        Map<String, Object> metadata = sanitizeMetadata(message.getMetadata());

        if (message instanceof UserMessage userMessage) {
            return UserMessage.builder()
                    .text(userMessage.getText())
                    .metadata(metadata)
                    .media(userMessage.getMedia())
                    .build();
        }

        if (message instanceof AssistantMessage assistantMessage) {
            return AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .properties(metadata)
                    .toolCalls(assistantMessage.getToolCalls())
                    .media(assistantMessage.getMedia())
                    .build();
        }

        if (message instanceof SystemMessage) {
            return SystemMessage.builder()
                    .text(message.getText())
                    .metadata(metadata)
                    .build();
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return ToolResponseMessage.builder()
                    .responses(toolResponseMessage.getResponses())
                    .metadata(metadata)
                    .build();
        }

        return UserMessage.builder()
                .text(message.getText())
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            Object sanitizedValue = sanitizeValue(value);
            if (sanitizedValue != null) {
                sanitized.put(key, sanitizedValue);
            }
        });
        return sanitized;
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Optional<?> optional) {
            return optional.map(this::sanitizeValue).orElse(null);
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Enum<?> || value instanceof URI || value instanceof TemporalAccessor) {
            return value.toString();
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((mapKey, mapValue) -> {
                Object sanitizedValue = sanitizeValue(mapValue);
                if (sanitizedValue != null) {
                    sanitized.put(String.valueOf(mapKey), sanitizedValue);
                }
            });
            return sanitized;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : collection) {
                Object sanitizedValue = sanitizeValue(item);
                if (sanitizedValue != null) {
                    sanitized.add(sanitizedValue);
                }
            }
            return sanitized;
        }

        return value.toString();
    }
}
