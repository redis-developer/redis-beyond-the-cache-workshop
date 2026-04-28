package com.redis.workshop.memory.controller;

import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoControllerTest {

    private final AgentMemoryService memoryService = mock(AgentMemoryService.class);
    private final DemoController controller = new DemoController(memoryService);

    @Test
    void startConversationUsesProvidedSessionId() {
        when(memoryService.putWorkingMemory(anyString(), any())).thenReturn(WorkingMemoryResponse.builder().build());

        Map<String, Object> response = controller.startConversation("alice", "session-from-ui");

        assertEquals("session-from-ui", response.get("sessionId"));
        verify(memoryService).putWorkingMemory(anyString(), any());
        verify(memoryService).putWorkingMemory(org.mockito.ArgumentMatchers.eq("session-from-ui"), any());
    }

    @Test
    void startConversationGeneratesSessionIdWhenMissing() {
        when(memoryService.putWorkingMemory(anyString(), any())).thenReturn(WorkingMemoryResponse.builder().build());

        Map<String, Object> response = controller.startConversation("alice", null);

        assertTrue(((String) response.get("sessionId")).startsWith("session-"));
        verify(memoryService).putWorkingMemory(anyString(), any());
    }
}
