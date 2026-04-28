package com.redis.workshop.memory.controller;

import com.redis.agentmemory.models.health.HealthCheckResponse;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryControllerTest {

    private final AgentMemoryService memoryService = mock(AgentMemoryService.class);
    private final MemoryController controller = new MemoryController(memoryService);

    @Test
    void healthCheckReportsConnectedWhenAgentMemoryIsReachable() {
        when(memoryService.healthCheck()).thenReturn(mock(HealthCheckResponse.class));

        Map<String, Object> response = controller.healthCheck();

        assertEquals("connected", response.get("status"));
    }

    @Test
    void healthCheckReportsDisconnectedWhenAgentMemoryFails() {
        when(memoryService.healthCheck()).thenThrow(new RuntimeException("boom"));

        Map<String, Object> response = controller.healthCheck();

        assertEquals("disconnected", response.get("status"));
    }
}
