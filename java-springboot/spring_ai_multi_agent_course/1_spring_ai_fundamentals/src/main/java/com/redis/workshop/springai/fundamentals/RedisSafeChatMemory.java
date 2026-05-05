package com.redis.workshop.springai.fundamentals;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

final class RedisSafeChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    RedisSafeChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages.stream().map(this::safeMessage).toList());
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    private Message safeMessage(Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            return AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .toolCalls(assistantMessage.getToolCalls())
                    .media(assistantMessage.getMedia())
                    .build();
        }
        if (message instanceof UserMessage userMessage) {
            return UserMessage.builder()
                    .text(userMessage.getText())
                    .media(userMessage.getMedia())
                    .build();
        }
        if (message instanceof SystemMessage systemMessage) {
            return SystemMessage.builder()
                    .text(systemMessage.getText())
                    .build();
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return ToolResponseMessage.builder()
                    .responses(toolResponseMessage.getResponses())
                    .build();
        }
        return message;
    }
}
