package com.redis.workshop.platform.controlplane.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/workshops")
public class CatalogController {

    private final WorkshopCatalogService workshopCatalogService;

    public CatalogController(WorkshopCatalogService workshopCatalogService) {
        this.workshopCatalogService = workshopCatalogService;
    }

    @GetMapping
    List<CatalogWorkshopResponse> listWorkshops() {
        return workshopCatalogService.getVisibleWorkshops();
    }

    @GetMapping("/{workshopId}")
    CatalogWorkshopResponse getWorkshop(@PathVariable String workshopId) {
        return workshopCatalogService.getVisibleWorkshop(workshopId);
    }
}
