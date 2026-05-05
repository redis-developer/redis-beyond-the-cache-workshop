package com.redis.workshop.springai.multiagents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiMultiAgentsApplicationTest {

    @Test
    void controllerReturnsModuleData() {
        MultiAgentsService service = new MultiAgentsService("", "gpt-4o-mini", null, null, null);
        MultiAgentsController controller = new MultiAgentsController(service);

        assertEquals("ready", controller.health().get("status"));
        assertEquals("redis-agent-memory", controller.status().module());
        assertEquals("ready", controller.status().status());
        assertEquals(5, controller.workflow().agents().size());
    }
}
