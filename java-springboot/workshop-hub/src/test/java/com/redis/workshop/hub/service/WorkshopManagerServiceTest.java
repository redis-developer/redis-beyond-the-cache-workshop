package com.redis.workshop.hub.service;

import com.redis.workshop.hub.model.Workshop;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopManagerServiceTest {

    private final WorkshopManagerService service =
        new WorkshopManagerService(new WorkshopRegistryService(new DefaultResourceLoader()));

    @Test
    void splitWorkshopIsDetectedWhenFrontendAndBackendServicesDiffer() {
        Workshop workshop = new Workshop();
        workshop.setServiceName("full-text-search");
        workshop.setFrontendServiceName("full-text-search");
        workshop.setBackendServiceName("full-text-search-api");

        assertTrue(service.hasDedicatedFrontend(workshop));
    }

    @Test
    void singleServiceWorkshopIsNotTreatedAsSplit() {
        Workshop workshop = new Workshop();
        workshop.setServiceName("session-management");
        workshop.setFrontendServiceName("session-management");
        workshop.setBackendServiceName("session-management");

        assertFalse(service.hasDedicatedFrontend(workshop));
    }

    @Test
    void frontendOnlyPartialStateStillCountsAsRunning() {
        assertEquals("running", service.resolveWorkshopStatus(true, false));
        assertEquals(
            "Frontend is available, backend is unavailable",
            service.resolveWorkshopMessage(true, false)
        );
    }

    @Test
    void backendOnlyPartialStateRemainsStopped() {
        assertEquals("stopped", service.resolveWorkshopStatus(false, true));
        assertEquals(
            "Partial state: frontend=false, backend=true",
            service.resolveWorkshopMessage(false, true)
        );
    }

    @Test
    void fullyHealthyAndFullyStoppedStatesHaveNoPartialMessage() {
        assertNull(service.resolveWorkshopMessage(true, true));
        assertNull(service.resolveWorkshopMessage(false, false));
    }
}
