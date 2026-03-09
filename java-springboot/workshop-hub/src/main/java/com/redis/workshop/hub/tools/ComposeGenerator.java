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
            String frontendServiceName = getFrontendServiceName(workshop);
            String backendServiceName = getBackendServiceName(workshop);

            if (frontendServiceName.equals(backendServiceName)) {
                services.put(
                    frontendServiceName,
                    buildWorkshopFrontendService(workshop, localMode, null)
                );
                continue;
            }

            services.put(
                backendServiceName,
                buildWorkshopBackendService(workshop, localMode)
            );
            services.put(
                frontendServiceName,
                buildWorkshopFrontendService(workshop, localMode, backendServiceName)
            );
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

    private static Map<String, Object> buildWorkshopFrontendService(
        Workshop workshop,
        boolean localMode,
        String backendServiceName
    ) {
        String dockerfile = workshop.getEffectiveFrontendDockerfile();
        String modulePath = getModulePath(dockerfile);
        int frontendPort = getFrontendPort(workshop);
        int backendPort = getBackendPort(workshop);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("build", buildBuildConfig(dockerfile, localMode, buildFrontendBuildArgs(localMode)));
        service.put("ports", List.of(frontendPort + ":" + frontendPort));
        service.put(
            "environment",
            buildWorkshopEnvironment(workshop, localMode, frontendPort, backendServiceName, backendPort)
        );

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + modulePath + ":/workshop-sources"));

        List<String> dependsOn = buildFrontendDependencies(workshop, localMode, backendServiceName);
        if (!dependsOn.isEmpty()) {
            service.put("depends_on", dependsOn);
        }

        service.put("profiles", buildWorkshopProfiles(workshop));
        return service;
    }

    private static Map<String, Object> buildWorkshopBackendService(Workshop workshop, boolean localMode) {
        String dockerfile = workshop.getEffectiveBackendDockerfile();
        String modulePath = getModulePath(dockerfile);
        int backendPort = getBackendPort(workshop);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("build", buildBuildConfig(dockerfile, localMode, List.of("SKIP_FRONTEND_BUILD=true")));
        service.put("ports", List.of(backendPort + ":" + backendPort));
        service.put("environment", buildWorkshopEnvironment(workshop, localMode, backendPort, null, null));

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + modulePath + ":/workshop-sources"));

        List<String> dependsOn = buildBackendDependencies(workshop, localMode);
        if (!dependsOn.isEmpty()) {
            service.put("depends_on", dependsOn);
        }

        service.put("profiles", buildWorkshopProfiles(workshop));
        return service;
    }

    private static Map<String, Object> buildBuildConfig(String dockerfile, boolean localMode, List<String> args) {
        Map<String, Object> build = new LinkedHashMap<>();
        String context = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        build.put("context", context);
        build.put("dockerfile", dockerfile);
        build.put("args", args);
        return build;
    }

    private static List<String> buildFrontendBuildArgs(boolean localMode) {
        if (localMode) {
            return List.of("SKIP_FRONTEND_BUILD=${SKIP_FRONTEND_BUILD:-false}");
        }
        return List.of(
            "VUE_APP_BASE_PATH=/",
            "SKIP_FRONTEND_BUILD=true"
        );
    }

    private static List<String> buildWorkshopEnvironment(
        Workshop workshop,
        boolean localMode,
        int runtimePort,
        String backendServiceName,
        Integer backendPort
    ) {
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

        if (runtimePort != 8080) {
            env.add("SERVER_PORT=" + runtimePort);
        }

        if (backendServiceName != null && !backendServiceName.isBlank() && backendPort != null && backendPort > 0) {
            env.add("WORKSHOP_BACKEND_URL=http://" + backendServiceName + ":" + backendPort);
        }

        // Keep local and internal mode behavior consistent for runtime port binding.
        if (localMode && runtimePort == 8080) {
            return env;
        }
        return env;
    }

    private static List<String> buildFrontendDependencies(Workshop workshop, boolean localMode, String backendServiceName) {
        List<String> deps = new ArrayList<>();

        if (backendServiceName != null && !backendServiceName.isBlank()) {
            deps.add(backendServiceName);
        }

        if (localMode) {
            deps.addAll(buildInfrastructureDependencies(workshop));
        } else if (isDistributedLocks(workshop)) {
            deps.addAll(buildInfrastructureDependencies(workshop));
        }

        return deps;
    }

    private static List<String> buildBackendDependencies(Workshop workshop, boolean localMode) {
        if (localMode) {
            return buildInfrastructureDependencies(workshop);
        }
        if (isDistributedLocks(workshop)) {
            return buildInfrastructureDependencies(workshop);
        }
        return List.of();
    }

    private static List<String> buildInfrastructureDependencies(Workshop workshop) {
        List<String> deps = new ArrayList<>();
        deps.add("redis");
        if (isDistributedLocks(workshop)) {
            deps.add("postgres");
        }
        return deps;
    }

    private static List<String> buildWorkshopProfiles(Workshop workshop) {
        return List.of("workshops", "workshop-" + workshop.getId());
    }

    private static String getFrontendServiceName(Workshop workshop) {
        String name = workshop.getEffectiveFrontendServiceName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return workshop.getId();
    }

    private static String getBackendServiceName(Workshop workshop) {
        String name = workshop.getEffectiveBackendServiceName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return getFrontendServiceName(workshop);
    }

    private static int getFrontendPort(Workshop workshop) {
        int port = workshop.getEffectiveFrontendPort();
        if (port > 0) {
            return port;
        }
        return 8080;
    }

    private static int getBackendPort(Workshop workshop) {
        int port = workshop.getEffectiveBackendPort();
        if (port > 0) {
            return port;
        }
        return 8080;
    }

    private static boolean needsPostgres(List<Workshop> workshops) {
        return workshops.stream().anyMatch(ComposeGenerator::isDistributedLocks);
    }

    private static boolean isDistributedLocks(Workshop workshop) {
        String id = workshop.getId();
        String serviceName = workshop.getServiceName();
        String frontendService = workshop.getEffectiveFrontendServiceName();
        String backendService = workshop.getEffectiveBackendServiceName();
        return "3_distributed_locks".equals(id)
            || "distributed-locks".equals(serviceName)
            || "distributed-locks".equals(frontendService)
            || "distributed-locks-api".equals(backendService);
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
