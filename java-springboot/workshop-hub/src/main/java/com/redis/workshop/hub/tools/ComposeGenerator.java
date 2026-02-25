package com.redis.workshop.hub.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redis.workshop.hub.model.Workshop;
import com.redis.workshop.hub.model.WorkshopRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComposeGenerator {

    public static void main(String[] args) throws IOException {
        String rootPath = args.length > 0 && !args[0].isBlank()
            ? args[0]
            : System.getenv().getOrDefault("WORKSHOP_ROOT_PATH", ".");
        Path rootDir = Paths.get(rootPath).toAbsolutePath().normalize();

        String registryArg = args.length > 1 && !args[1].isBlank()
            ? args[1]
            : System.getenv().getOrDefault("WORKSHOP_REGISTRY_PATH", "workshops.yaml");
        Path registryPath = resolvePath(rootDir, registryArg);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        WorkshopRegistry registry = mapper.readValue(Files.newInputStream(registryPath), WorkshopRegistry.class);
        List<Workshop> workshops = registry.getWorkshops() == null ? List.of() : registry.getWorkshops();

        writeComposeFile(
            rootDir,
            workshops,
            rootDir.resolve("java-springboot/workshop-hub/docker-compose.local.yml"),
            true
        );
        writeComposeFile(
            rootDir,
            workshops,
            rootDir.resolve("java-springboot/workshop-hub/docker-compose.internal.yml"),
            false
        );
    }

    private static void writeComposeFile(Path rootDir, List<Workshop> workshops, Path outputPath, boolean localMode) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();

        services.put("redis", buildRedisService(localMode));
        services.put("redis-insight", buildRedisInsightService());
        if (needsPostgres(workshops)) {
            services.put("postgres", buildPostgresService(localMode));
        }

        for (Workshop workshop : workshops) {
            String serviceName = getServiceName(workshop);
            services.put(serviceName, buildWorkshopService(rootDir, workshop, localMode));
        }

        root.put("services", services);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        Files.createDirectories(outputPath.getParent());
        mapper.writeValue(outputPath.toFile(), root);
    }

    private static Map<String, Object> buildRedisService(boolean localMode) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", "redis:latest");
        service.put("ports", List.of("6379:6379"));

        Map<String, Object> healthcheck = new LinkedHashMap<>();
        healthcheck.put("test", List.of("CMD", "redis-cli", "-p", "6379", "ping"));
        healthcheck.put("interval", "5s");
        healthcheck.put("timeout", "5s");
        healthcheck.put("retries", 5);
        service.put("healthcheck", healthcheck);

        if (localMode) {
            service.put("profiles", List.of("infrastructure", "workshops"));
        }

        return service;
    }

    private static Map<String, Object> buildRedisInsightService() {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", "redis/redisinsight:latest");
        service.put("ports", List.of("5540:5540"));

        List<String> env = new ArrayList<>();
        env.add("RI_REDIS_HOST=redis");
        env.add("RI_REDIS_PORT=6379");
        service.put("environment", env);

        service.put("profiles", List.of("infrastructure"));

        return service;
    }

    private static Map<String, Object> buildWorkshopService(Path rootDir, Workshop workshop, boolean localMode) {
        Map<String, Object> service = new LinkedHashMap<>();

        String dockerfile = workshop.getDockerfile();
        String modulePath = getModulePath(dockerfile);
        int port = getPort(workshop);

        Map<String, Object> build = new LinkedHashMap<>();
        String context = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        build.put("context", context);
        build.put("dockerfile", dockerfile);

        List<String> args = new ArrayList<>();
        if (localMode) {
            args.add("SKIP_FRONTEND_BUILD=${SKIP_FRONTEND_BUILD:-false}");
        } else {
            args.add("VUE_APP_BASE_PATH=/");
            args.add("SKIP_FRONTEND_BUILD=true");
        }
        build.put("args", args);

        service.put("build", build);
        service.put("ports", List.of(port + ":" + (localMode ? 8080 : port)));

        List<String> env = new ArrayList<>();
        env.add("SPRING_DATA_REDIS_HOST=redis");
        env.add("SPRING_DATA_REDIS_PORT=6379");
        env.add("SPRING_REDIS_HOST=redis");
        env.add("SPRING_REDIS_PORT=6379");
        env.add("WORKSHOP_BASE_PATH=/workshop-sources");
        if (isDistributedLocks(workshop)) {
            env.add("SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/workshop");
            env.add("SPRING_DATASOURCE_USERNAME=workshop");
            env.add("SPRING_DATASOURCE_PASSWORD=workshop");
        }
        if (!localMode && port != 8080) {
            env.add("SERVER_PORT=" + port);
        }
        service.put("environment", env);

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + modulePath + ":/workshop-sources"));

        if (localMode) {
            service.put("depends_on", buildDependencies(workshop));
        } else if (isDistributedLocks(workshop)) {
            service.put("depends_on", buildDependencies(workshop));
        }

        service.put("profiles", List.of("workshops", "workshop-" + workshop.getId()));

        return service;
    }

    private static String getServiceName(Workshop workshop) {
        if (workshop.getServiceName() != null && !workshop.getServiceName().isBlank()) {
            return workshop.getServiceName();
        }
        return workshop.getId();
    }

    private static boolean needsPostgres(List<Workshop> workshops) {
        return workshops.stream().anyMatch(ComposeGenerator::isDistributedLocks);
    }

    private static boolean isDistributedLocks(Workshop workshop) {
        String id = workshop.getId();
        String serviceName = workshop.getServiceName();
        return "3_distributed_locks".equals(id) || "distributed-locks".equals(serviceName);
    }

    private static List<String> buildDependencies(Workshop workshop) {
        List<String> deps = new ArrayList<>();
        deps.add("redis");
        if (isDistributedLocks(workshop)) {
            deps.add("postgres");
        }
        return deps;
    }

    private static Map<String, Object> buildPostgresService(boolean localMode) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", "postgres:16");
        service.put("ports", List.of("5432:5432"));

        List<String> env = new ArrayList<>();
        env.add("POSTGRES_DB=workshop");
        env.add("POSTGRES_USER=workshop");
        env.add("POSTGRES_PASSWORD=workshop");
        service.put("environment", env);

        Map<String, Object> healthcheck = new LinkedHashMap<>();
        healthcheck.put("test", List.of("CMD-SHELL", "pg_isready -U workshop -d workshop"));
        healthcheck.put("interval", "5s");
        healthcheck.put("timeout", "5s");
        healthcheck.put("retries", 5);
        service.put("healthcheck", healthcheck);

        if (localMode) {
            service.put("profiles", List.of("workshops", "workshop-3_distributed_locks"));
        }

        return service;
    }

    private static int getPort(Workshop workshop) {
        if (workshop.getPort() > 0) {
            return workshop.getPort();
        }
        return 8080;
    }

    private static String getModulePath(String dockerfile) {
        if (dockerfile == null || dockerfile.isBlank()) {
            return "java-springboot";
        }
        Path dockerPath = Paths.get(dockerfile);
        Path parent = dockerPath.getParent();
        if (parent == null) {
            return dockerfile;
        }
        return parent.toString().replace("\\", "/");
    }

    private static Path resolvePath(Path rootDir, String relativeOrAbsolute) {
        Path path = Paths.get(relativeOrAbsolute);
        if (!path.isAbsolute()) {
            return rootDir.resolve(path).normalize();
        }
        return path.normalize();
    }
}
