package com.redis.workshop.hub.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redis.workshop.hub.model.Workshop;
import com.redis.workshop.hub.model.WorkshopHealthcheck;
import com.redis.workshop.hub.model.WorkshopRegistry;
import com.redis.workshop.hub.model.WorkshopSidecar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ComposeGenerator {

    private enum ServiceRole {
        FRONTEND,
        BACKEND,
        COMBINED
    }

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

        List<Workshop> redisWorkshops = workshopsUsingInfrastructure(workshops, "redis");
        services.put(
            "redis",
            buildRedisService(localMode, workshops.stream().anyMatch(ComposeGenerator::requiresRedisStack), redisWorkshops)
        );
        services.put("redis-insight", buildRedisInsightService());
        List<Workshop> postgresWorkshops = workshopsUsingInfrastructure(workshops, "postgres");
        if (!postgresWorkshops.isEmpty()) {
            services.put("postgres", buildPostgresService(localMode, postgresWorkshops));
        }

        for (Workshop workshop : workshops) {
            String frontendServiceName = getFrontendServiceName(workshop);
            String backendServiceName = getBackendServiceName(workshop);

            if (frontendServiceName.equals(backendServiceName)) {
                services.put(frontendServiceName, buildCombinedWorkshopService(workshop, localMode));
            } else {
                services.put(
                    backendServiceName,
                    buildWorkshopBackendService(workshop, localMode)
                );
                services.put(
                    frontendServiceName,
                    buildWorkshopFrontendService(workshop, localMode, backendServiceName)
                );
            }

            for (WorkshopSidecar sidecar : workshop.getSidecars()) {
                services.put(sidecar.getServiceName(), buildSidecarService(workshop, sidecar));
            }
        }

        root.put("services", services);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        Files.createDirectories(outputPath.getParent());
        mapper.writeValue(outputPath.toFile(), root);
    }

    private static Map<String, Object> buildRedisService(boolean localMode, boolean useRedisStack, List<Workshop> workshops) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", useRedisStack ? "redis/redis-stack-server:latest" : "redis:latest");
        service.put("ports", List.of("6379:6379"));

        Map<String, Object> healthcheck = new LinkedHashMap<>();
        healthcheck.put("test", List.of("CMD", "redis-cli", "-p", "6379", "ping"));
        healthcheck.put("interval", "5s");
        healthcheck.put("timeout", "5s");
        healthcheck.put("retries", 5);
        service.put("healthcheck", healthcheck);

        if (localMode) {
            service.put("profiles", buildLocalInfrastructureProfiles(workshops, true));
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
        String sourceModulePath = getFrontendSourceModulePath(workshop);
        int frontendPort = getFrontendPort(workshop);
        int backendPort = getBackendPort(workshop);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("build", buildBuildConfig(dockerfile, localMode, buildFrontendBuildArgs(workshop, localMode)));
        service.put("ports", List.of(frontendPort + ":" + frontendPort));
        service.put(
            "environment",
            buildWorkshopEnvironment(workshop, ServiceRole.FRONTEND, frontendPort, backendServiceName, backendPort)
        );

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + sourceModulePath + ":/workshop-sources"));

        List<String> dependsOn = buildFrontendDependencies(backendServiceName);
        if (!dependsOn.isEmpty()) {
            service.put("depends_on", dependsOn);
        }

        service.put("profiles", buildWorkshopProfiles(workshop));
        return service;
    }

    private static Map<String, Object> buildCombinedWorkshopService(Workshop workshop, boolean localMode) {
        String dockerfile = workshop.getEffectiveFrontendDockerfile();
        String sourceModulePath = getFrontendSourceModulePath(workshop);
        int frontendPort = getFrontendPort(workshop);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("build", buildBuildConfig(dockerfile, localMode, buildFrontendBuildArgs(workshop, localMode)));
        service.put("ports", List.of(frontendPort + ":" + frontendPort));
        service.put("environment", buildWorkshopEnvironment(workshop, ServiceRole.COMBINED, frontendPort, null, null));

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + sourceModulePath + ":/workshop-sources"));

        List<String> dependsOn = buildRuntimeDependencies(workshop, true);
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
        service.put("environment", buildWorkshopEnvironment(workshop, ServiceRole.BACKEND, backendPort, null, null));

        String volumeRoot = localMode ? "${WORKSHOP_ROOT_PATH:-.}" : "${WORKSHOP_ROOT_PATH:-/workshops}";
        service.put("volumes", List.of(volumeRoot + "/" + modulePath + ":/workshop-sources"));

        List<String> dependsOn = buildRuntimeDependencies(workshop, true);
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

    private static List<String> buildFrontendBuildArgs(Workshop workshop, boolean localMode) {
        if (localMode) {
            return List.of("SKIP_FRONTEND_BUILD=${SKIP_FRONTEND_BUILD:-false}");
        }
        return List.of(
            "VUE_APP_BASE_PATH=/",
            "SKIP_FRONTEND_BUILD=" + (workshop.isFrontendPrebuildEnabled() ? "true" : "false")
        );
    }

    private static List<String> buildWorkshopEnvironment(
        Workshop workshop,
        ServiceRole role,
        int runtimePort,
        String backendServiceName,
        Integer backendPort
    ) {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        if (hasInfrastructureDependency(workshop, "redis")) {
            env.put("SPRING_DATA_REDIS_HOST", "redis");
            env.put("SPRING_DATA_REDIS_PORT", "6379");
            env.put("SPRING_REDIS_HOST", "redis");
            env.put("SPRING_REDIS_PORT", "6379");
        }
        env.put("WORKSHOP_BASE_PATH", "/workshop-sources");
        applyEnvOverrides(env, workshop.getCommonEnvOverrides());
        applyEnvOverrides(env, resolveRoleEnvOverrides(workshop, role));

        if (runtimePort != 8080) {
            env.put("SERVER_PORT", String.valueOf(runtimePort));
        }

        if (backendServiceName != null && !backendServiceName.isBlank() && backendPort != null && backendPort > 0) {
            env.put("WORKSHOP_BACKEND_URL", "http://" + backendServiceName + ":" + backendPort);
        }

        return toEnvironmentList(env);
    }

    private static List<String> buildFrontendDependencies(String backendServiceName) {
        LinkedHashSet<String> deps = new LinkedHashSet<>();
        if (backendServiceName != null && !backendServiceName.isBlank()) {
            deps.add(backendServiceName);
        }
        return List.copyOf(deps);
    }

    private static List<String> buildRuntimeDependencies(Workshop workshop, boolean includeSidecars) {
        LinkedHashSet<String> deps = new LinkedHashSet<>(workshop.getInfrastructureDependencies());
        if (includeSidecars) {
            for (WorkshopSidecar sidecar : workshop.getSidecars()) {
                deps.add(sidecar.getServiceName());
            }
        }
        return List.copyOf(deps);
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

    private static List<Workshop> workshopsUsingInfrastructure(List<Workshop> workshops, String serviceName) {
        return workshops.stream()
            .filter(workshop -> hasInfrastructureDependency(workshop, serviceName))
            .toList();
    }

    private static boolean requiresRedisStack(Workshop workshop) {
        return hasInfrastructureDependency(workshop, "redis") && workshop.usesRedisStack();
    }

    private static boolean hasInfrastructureDependency(Workshop workshop, String serviceName) {
        return workshop.getInfrastructureDependencies().stream()
            .anyMatch(serviceName::equals);
    }

    private static Map<String, Object> buildPostgresService(boolean localMode, List<Workshop> workshops) {
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
            service.put("profiles", buildLocalInfrastructureProfiles(workshops, false));
        }

        return service;
    }

    private static List<String> buildLocalInfrastructureProfiles(List<Workshop> workshops, boolean includeInfrastructureProfile) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>();
        if (includeInfrastructureProfile) {
            profiles.add("infrastructure");
        }
        profiles.add("workshops");
        for (Workshop workshop : workshops) {
            profiles.add("workshop-" + workshop.getId());
        }
        return List.copyOf(profiles);
    }

    private static Map<String, String> resolveRoleEnvOverrides(Workshop workshop, ServiceRole role) {
        return switch (role) {
            case FRONTEND -> workshop.getFrontendEnvOverrides();
            case BACKEND -> workshop.getBackendEnvOverrides();
            case COMBINED -> mergeEnvOverrides(workshop.getFrontendEnvOverrides(), workshop.getBackendEnvOverrides());
        };
    }

    private static Map<String, String> mergeEnvOverrides(Map<String, String> first, Map<String, String> second) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        applyEnvOverrides(merged, first);
        applyEnvOverrides(merged, second);
        return merged;
    }

    private static void applyEnvOverrides(Map<String, String> target, Map<String, String> overrides) {
        overrides.forEach(target::put);
    }

    private static List<String> toEnvironmentList(Map<String, String> env) {
        return env.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toList();
    }

    private static Map<String, Object> buildSidecarService(Workshop workshop, WorkshopSidecar sidecar) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", sidecar.getImage());
        if (sidecar.getPort() != null && sidecar.getPort() > 0) {
            service.put("ports", List.of(sidecar.getPort() + ":" + sidecar.getPort()));
        }
        if (!sidecar.getEnv().isEmpty()) {
            service.put("environment", toEnvironmentList(sidecar.getEnv()));
        }
        if (!sidecar.getDependsOn().isEmpty()) {
            service.put("depends_on", sidecar.getDependsOn());
        }
        if (!sidecar.getCommand().isEmpty()) {
            service.put("command", sidecar.getCommand());
        }

        Map<String, Object> healthcheck = buildHealthcheck(sidecar.getHealthcheck());
        if (!healthcheck.isEmpty()) {
            service.put("healthcheck", healthcheck);
        }

        service.put("profiles", buildWorkshopProfiles(workshop));
        return service;
    }

    private static Map<String, Object> buildHealthcheck(WorkshopHealthcheck healthcheck) {
        if (healthcheck == null) {
            return Map.of();
        }

        Map<String, Object> config = new LinkedHashMap<>();
        if (!healthcheck.getTest().isEmpty()) {
            config.put("test", healthcheck.getTest());
        }
        if (healthcheck.getInterval() != null && !healthcheck.getInterval().isBlank()) {
            config.put("interval", healthcheck.getInterval());
        }
        if (healthcheck.getTimeout() != null && !healthcheck.getTimeout().isBlank()) {
            config.put("timeout", healthcheck.getTimeout());
        }
        if (healthcheck.getRetries() != null) {
            config.put("retries", healthcheck.getRetries());
        }
        if (healthcheck.getStartPeriod() != null && !healthcheck.getStartPeriod().isBlank()) {
            config.put("start_period", healthcheck.getStartPeriod());
        }
        return config;
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

    private static String getFrontendSourceModulePath(Workshop workshop) {
        String backendDockerfile = workshop.getEffectiveBackendDockerfile();
        if (backendDockerfile != null && !backendDockerfile.isBlank()) {
            return getModulePath(backendDockerfile);
        }
        return getModulePath(workshop.getEffectiveFrontendDockerfile());
    }

    private static Path resolvePath(Path rootDir, String relativeOrAbsolute) {
        Path path = Paths.get(relativeOrAbsolute);
        if (!path.isAbsolute()) {
            return rootDir.resolve(path).normalize();
        }
        return path.normalize();
    }
}
