package com.redis.workshop.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redis.workshop.hub.model.Workshop;
import com.redis.workshop.hub.model.WorkshopRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class WorkshopRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopRegistryService.class);

    @Value("${workshop.registry.path:workshops.yaml}")
    private String registryPath;

    @Value("${workshop.root.path:/workshops}")
    private String workshopRootPath;

    private final ObjectMapper mapper;
    private final ResourceLoader resourceLoader;

    public WorkshopRegistryService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.findAndRegisterModules();
    }

    public List<Workshop> getWorkshops() {
        WorkshopRegistry registry = loadRegistry();
        if (registry.getWorkshops() == null) {
            return Collections.emptyList();
        }
        return registry.getWorkshops();
    }

    private WorkshopRegistry loadRegistry() {
        Path resolvedPath = resolveRegistryPath();
        if (Files.exists(resolvedPath)) {
            try (InputStream inputStream = Files.newInputStream(resolvedPath)) {
                return mapper.readValue(inputStream, WorkshopRegistry.class);
            } catch (IOException e) {
                logger.error("Failed to read workshop registry from {}", resolvedPath, e);
                return emptyRegistry();
            }
        }

        Resource classpathResource = resourceLoader.getResource("classpath:workshops.yaml");
        if (classpathResource.exists()) {
            try (InputStream inputStream = classpathResource.getInputStream()) {
                return mapper.readValue(inputStream, WorkshopRegistry.class);
            } catch (IOException e) {
                logger.error("Failed to read workshop registry from classpath", e);
                return emptyRegistry();
            }
        }

        logger.warn("Workshop registry not found at {} and not available on classpath", resolvedPath);
        return emptyRegistry();
    }

    private Path resolveRegistryPath() {
        Path path = Paths.get(registryPath);
        if (!path.isAbsolute()) {
            path = Paths.get(workshopRootPath, registryPath);
        }
        return path.normalize();
    }

    private WorkshopRegistry emptyRegistry() {
        WorkshopRegistry registry = new WorkshopRegistry();
        registry.setVersion(1);
        registry.setWorkshops(Collections.emptyList());
        return registry;
    }
}
