package com.redis.workshop.locks.controller;

import com.redis.workshop.locks.model.CacheSimulationResult;
import com.redis.workshop.locks.model.EventSimulationResult;
import com.redis.workshop.locks.model.ImportSimulationResult;
import com.redis.workshop.locks.model.JobSimulationResult;
import com.redis.workshop.locks.service.ScenarioService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {
    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/jobs/run")
    public JobSimulationResult runJobs(@RequestParam(defaultValue = "5") int workers) {
        return scenarioService.runJobSimulation(workers);
    }

    @PostMapping("/cache/simulate")
    public CacheSimulationResult runCache(@RequestParam(defaultValue = "5") int requests) {
        return scenarioService.runCacheSimulation(requests);
    }

    @PostMapping("/cache/reset")
    public Map<String, Boolean> resetCache() {
        scenarioService.resetCache();
        return Map.of("success", true);
    }

    @PostMapping("/events/process")
    public EventSimulationResult runEvents(
        @RequestParam(defaultValue = "evt-101") String eventId,
        @RequestParam(defaultValue = "5") int attempts
    ) {
        return scenarioService.runEventSimulation(eventId, attempts);
    }

    @PostMapping("/events/reset")
    public Map<String, Boolean> resetEvents() {
        scenarioService.resetEvents();
        return Map.of("success", true);
    }

    @PostMapping("/imports/run")
    public ImportSimulationResult runImports(@RequestParam(defaultValue = "3") int attempts) {
        return scenarioService.runImportSimulation(attempts);
    }
}
