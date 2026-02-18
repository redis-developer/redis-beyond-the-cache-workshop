package com.redis.workshop.hub.controller;

import com.redis.workshop.hub.dto.CommandResponse;
import com.redis.workshop.hub.dto.ServiceStatus;
import com.redis.workshop.hub.service.WorkshopManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/manager")
public class WorkshopManagerController {

    @Autowired
    private WorkshopManagerService workshopManagerService;

    @GetMapping
    public String managerPage(Model model) {
        return "manager";
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<List<ServiceStatus>> getStatus() {
        List<ServiceStatus> statuses = workshopManagerService.getAllServiceStatus();
        return ResponseEntity.ok(statuses);
    }

    @PostMapping("/api/infrastructure/start")
    @ResponseBody
    public ResponseEntity<CommandResponse> startInfrastructure() {
        CommandResponse response = workshopManagerService.startInfrastructure();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/infrastructure/stop")
    @ResponseBody
    public ResponseEntity<CommandResponse> stopInfrastructure() {
        CommandResponse response = workshopManagerService.stopInfrastructure();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/workshop/{id}/start")
    @ResponseBody
    public ResponseEntity<CommandResponse> startWorkshop(@PathVariable String id) {
        CommandResponse response = workshopManagerService.startWorkshop(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/workshop/{id}/stop")
    @ResponseBody
    public ResponseEntity<CommandResponse> stopWorkshop(@PathVariable String id) {
        CommandResponse response = workshopManagerService.stopWorkshop(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/workshop/{id}/restart")
    @ResponseBody
    public ResponseEntity<CommandResponse> restartWorkshop(@PathVariable String id) {
        CommandResponse response = workshopManagerService.restartWorkshop(id);
        return ResponseEntity.ok(response);
    }
}

