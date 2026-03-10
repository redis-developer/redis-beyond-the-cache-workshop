package com.redis.workshop.hub.service;

import com.redis.workshop.hub.dto.CommandResponse;
import com.redis.workshop.hub.dto.ServiceStatus;
import com.redis.workshop.hub.model.Workshop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WorkshopManagerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopManagerService.class);

    private final WorkshopRegistryService registryService;

    // Track deployment stages for each workshop
    private final Map<String, String> deploymentStages = new ConcurrentHashMap<>();

    @Value("${workshop.root.path:/workshops}")
    private String workshopRootPath;

    // Local mode flag - set to true when running Hub locally (not in container)
    // When true: uses docker-compose.local.yml and returns direct URLs (localhost:8080)
    // When false: uses docker-compose.internal.yml and returns proxy URLs (/workshop/...)
    @Value("${workshop.local.mode:false}")
    private boolean localMode;

    // Path to the docker-compose file directory
    @Value("${workshop.compose.dir:java-springboot/workshop-hub}")
    private String composeDir;

    public WorkshopManagerService(WorkshopRegistryService registryService) {
        this.registryService = registryService;
    }

    private String getComposeFile() {
        String fileName = localMode ? "docker-compose.local.yml" : "docker-compose.internal.yml";
        return composeDir + "/" + fileName;
    }

    public List<ServiceStatus> getAllServiceStatus() {
        List<ServiceStatus> statuses = new ArrayList<>();

        // Check Docker Compose services (internal to DinD)
        statuses.add(checkDockerService("redis", "6379"));
        statuses.add(checkDockerService("redis-insight", "5540"));

        // Check workshop applications - URL points to proxy path
        for (Workshop workshop : registryService.getWorkshops()) {
            statuses.add(checkWorkshopService(workshop));
        }

        return statuses;
    }

    private ServiceStatus checkDockerService(String serviceName, String port) {
        ServiceStatus status = new ServiceStatus();
        status.setName(serviceName);
        status.setType("infrastructure");
        status.setPort(port);

        // Set URL based on mode
        if ("redis".equals(serviceName)) {
            status.setUrl("redis://localhost:" + port);
        } else if ("redis-insight".equals(serviceName)) {
            // Local mode: direct URL, Container mode: proxy URL
            status.setUrl(localMode ? "http://localhost:" + port : "/workshop/redis-insight/");
        } else {
            status.setUrl("http://localhost:" + port);
        }

        status.setStatus(isDockerServiceRunning(serviceName) ? "running" : "stopped");

        return status;
    }

    private ServiceStatus checkWorkshopService(Workshop workshop) {
        String workshopId = workshop.getId();
        int frontendPort = getFrontendPortForWorkshop(workshop);
        String port = String.valueOf(frontendPort);
        ServiceStatus status = new ServiceStatus();
        status.setName(workshopId);
        status.setType("workshop");
        status.setPort(port);

        String frontendServiceName = getFrontendServiceNameForWorkshop(workshop);
        String backendServiceName = getBackendServiceNameForWorkshop(workshop);
        // Local mode: direct URL, Container mode: proxy URL
        if (localMode) {
            status.setUrl("http://localhost:" + port + "/");
        } else if (workshop.getUrl() != null && !workshop.getUrl().isBlank()) {
            status.setUrl(workshop.getUrl());
        } else {
            status.setUrl("/workshop/" + frontendServiceName + "/");
        }

        boolean frontendRunning = isDockerServiceRunning(frontendServiceName);
        boolean backendRunning = frontendServiceName.equals(backendServiceName)
            || isDockerServiceRunning(backendServiceName);
        status.setStatus(resolveWorkshopStatus(frontendRunning, backendRunning));
        String message = resolveWorkshopMessage(frontendRunning, backendRunning);
        if (message != null) {
            status.setMessage(message);
        }

        // Add deployment stage if workshop is being deployed
        String deploymentStage = deploymentStages.get(workshopId);
        if (deploymentStage != null) {
            status.setDeploymentStage(deploymentStage);
        }

        return status;
    }

    public CommandResponse startInfrastructure() {
        try {
            logger.info("Starting infrastructure services (localMode={})...", localMode);

            String composeFilePath = getComposeFile();
            File composeFile = new File(composeFilePath);
            if (!composeFile.exists()) {
                String error = "Docker compose file not found at: " + composeFilePath;
                logger.error(error);
                return new CommandResponse(false, error);
            }

            // Start only the infrastructure services (Redis and Redis Insight)
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", composeFilePath, "up", "-d", "redis", "redis-insight");
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info(line);
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Infrastructure started successfully", output.toString());
            } else {
                String errorMsg = "Failed to start infrastructure. Exit code: " + process.exitValue() + "\nOutput: " + output;
                logger.error(errorMsg);
                return new CommandResponse(false, errorMsg);
            }
        } catch (Exception e) {
            logger.error("Error starting infrastructure", e);
            return new CommandResponse(false, "Error: " + e.getMessage() + " - " + e.getClass().getSimpleName());
        }
    }

    public CommandResponse stopInfrastructure() {
        try {
            logger.info("Stopping infrastructure services...");

            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", getComposeFile(), "down");
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info(line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Infrastructure stopped successfully", output.toString());
            } else {
                return new CommandResponse(false, "Failed to stop infrastructure", output.toString());
            }
        } catch (Exception e) {
            logger.error("Error stopping infrastructure", e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    public CommandResponse startWorkshop(String workshopId) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Starting workshop (localMode={}): {}", localMode, workshopId);

            Workshop workshop = getWorkshopById(workshopId);
            if (workshop == null) {
                return new CommandResponse(false, "Workshop not found: " + workshopId);
            }

            if (hasDedicatedFrontend(workshop)) {
                return startSplitWorkshop(workshopId, workshop, startTime);
            }

            // If something is already running, stop it first
            ServiceStatus status = checkWorkshopService(workshop);
            if ("running".equals(status.getStatus())) {
                logger.info("Workshop appears to be running already, stopping existing container before start");
                deploymentStages.put(workshopId, "stopping");
                stopWorkshop(workshopId);
                Thread.sleep(2000);
            }

            // Build the image and start the workshop service
            deploymentStages.put(workshopId, "building");
            List<String> serviceNames = getServiceNamesForWorkshop(workshop);
            List<String> command = new ArrayList<>(List.of("docker-compose", "-f", getComposeFile(), "up", "-d", "--build"));
            command.addAll(serviceNames);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose up {}] {}", String.join(",", serviceNames), line);

                    // Update stage based on docker-compose output
                    if (line.contains("Building")) {
                        deploymentStages.put(workshopId, "building");
                    } else if (line.contains("Creating") || line.contains("Starting")) {
                        deploymentStages.put(workshopId, "starting");
                    }
                }
            }

            deploymentStages.put(workshopId, "initializing");
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            if (finished && process.exitValue() == 0) {
                deploymentStages.put(workshopId, "ready");
                logger.info("Workshop {} started successfully in {} seconds", workshopId, duration / 1000.0);

                // Clear stage after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        deploymentStages.remove(workshopId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                return new CommandResponse(true, String.format("Workshop container started in %.1f seconds", duration / 1000.0), output.toString());
            } else {
                deploymentStages.remove(workshopId);
                String errorMsg = "Failed to start workshop container. Exit code: " + process.exitValue();
                logger.error(errorMsg + "\nOutput:\n{}", output);
                return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
            }
        } catch (Exception e) {
            deploymentStages.remove(workshopId);
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error starting workshop inside DinD: {} (failed after {} seconds)", workshopId, duration / 1000.0, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    public CommandResponse stopWorkshop(String workshopId) {
        try {
            logger.info("Stopping workshop: {}", workshopId);

            Workshop workshop = getWorkshopById(workshopId);
            if (workshop == null) {
                return new CommandResponse(false, "Workshop not found: " + workshopId);
            }

            List<String> serviceNames = getServiceNamesForWorkshop(workshop);
            List<String> command = new ArrayList<>(List.of("docker-compose", "-f", getComposeFile(), "stop"));
            command.addAll(serviceNames);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose stop {}] {}", String.join(",", serviceNames), line);
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Workshop " + workshopId + " stopped", output.toString());
            } else {
                String errorMsg = "Failed to stop workshop container. Exit code: " + process.exitValue();
                logger.error(errorMsg + "\nOutput:\n{}", output);
                return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
            }
        } catch (Exception e) {
            logger.error("Error stopping workshop inside DinD: " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    public CommandResponse restartWorkshop(String workshopId) {
        logger.info("Restarting workshop: " + workshopId);
        Workshop workshop = getWorkshopById(workshopId);
        if (workshop == null) {
            return new CommandResponse(false, "Workshop not found: " + workshopId);
        }

        if (hasDedicatedFrontend(workshop)) {
            deploymentStages.put(workshopId, "stopping");
            CommandResponse stopResponse = stopBackendOnly(workshopId, workshop);
            if (!stopResponse.isSuccess()) {
                deploymentStages.remove(workshopId);
                return stopResponse;
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long startTime = System.currentTimeMillis();
            return startSplitWorkshop(workshopId, workshop, startTime);
        }

        deploymentStages.put(workshopId, "stopping");
        CommandResponse stopResponse = stopWorkshop(workshopId);

        if (!stopResponse.isSuccess()) {
            deploymentStages.remove(workshopId);
            return stopResponse;
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return startWorkshop(workshopId);
    }

    /**
     * Restart the workshop container without rebuilding the Docker image.
     */
    public CommandResponse restartWorkshopWithoutBuild(String workshopId) {
        logger.info("Restarting workshop without image rebuild: {}", workshopId);

        Workshop workshop = getWorkshopById(workshopId);
        if (workshop == null) {
            return new CommandResponse(false, "Workshop not found: " + workshopId);
        }

        if (hasDedicatedFrontend(workshop)) {
            deploymentStages.put(workshopId, "stopping");
            CommandResponse stopResponse = stopBackendOnly(workshopId, workshop);
            if (!stopResponse.isSuccess()) {
                deploymentStages.remove(workshopId);
                return stopResponse;
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return restartBackendOnlyWithoutBuild(workshopId, workshop);
        }

        deploymentStages.put(workshopId, "stopping");
        CommandResponse stopResponse = stopWorkshop(workshopId);
        if (!stopResponse.isSuccess()) {
            deploymentStages.remove(workshopId);
            return stopResponse;
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            deploymentStages.put(workshopId, "starting");
            List<String> serviceNames = getServiceNamesForWorkshop(workshop);
            List<String> command = new ArrayList<>(List.of("docker-compose", "-f", getComposeFile(), "up", "-d", "--force-recreate"));
            command.addAll(serviceNames);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose up --force-recreate {}] {}", String.join(",", serviceNames), line);

                    // Update stage based on docker-compose output
                    if (line.contains("Creating") || line.contains("Starting")) {
                        deploymentStages.put(workshopId, "starting");
                    }
                }
            }

            deploymentStages.put(workshopId, "initializing");
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                deploymentStages.put(workshopId, "ready");

                // Clear stage after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        deploymentStages.remove(workshopId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                return new CommandResponse(true, "Workshop container restarted without image rebuild...", output.toString());
            } else {
                deploymentStages.remove(workshopId);
                String errorMsg = "Failed to restart workshop container. Exit code: " + process.exitValue();
                logger.error(errorMsg + "\nOutput:\n{}", output);
                return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
            }
        } catch (Exception e) {
            deploymentStages.remove(workshopId);
            logger.error("Error restarting workshop inside DinD: " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    private CommandResponse startSplitWorkshop(String workshopId, Workshop workshop, long startTime) {
        String frontendServiceName = getFrontendServiceNameForWorkshop(workshop);
        if (!isDockerServiceRunning(frontendServiceName)) {
            CommandResponse frontendResponse = startFrontendOnly(workshopId, workshop);
            if (!frontendResponse.isSuccess()) {
                deploymentStages.remove(workshopId);
                return frontendResponse;
            }
        }

        return startBackendOnlyWithBuild(workshopId, workshop, startTime);
    }

    private CommandResponse startFrontendOnly(String workshopId, Workshop workshop) {
        deploymentStages.put(workshopId, "building");
        String frontendServiceName = getFrontendServiceNameForWorkshop(workshop);
        List<String> command = new ArrayList<>(List.of(
            "docker-compose",
            "-f",
            getComposeFile(),
            "up",
            "-d",
            "--build",
            "--no-deps",
            frontendServiceName
        ));
        return runComposeLifecycleCommand(
            workshopId,
            command,
            "[docker-compose up split-frontend {}]",
            "Failed to start workshop frontend."
        );
    }

    private CommandResponse startBackendOnlyWithBuild(String workshopId, Workshop workshop, long startTime) {
        deploymentStages.put(workshopId, "building");
        String backendServiceName = getBackendServiceNameForWorkshop(workshop);
        List<String> command = new ArrayList<>(List.of("docker-compose", "-f", getComposeFile(), "up", "-d", "--build", backendServiceName));
        CommandResponse response = runComposeLifecycleCommand(
            workshopId,
            command,
            "[docker-compose up split-backend {}]",
            "Failed to start workshop backend."
        );
        if (!response.isSuccess()) {
            deploymentStages.remove(workshopId);
            return response;
        }
        return finalizeSuccessfulLifecycle(workshopId, startTime, response.getOutput(), "Workshop backend started in %.1f seconds");
    }

    private CommandResponse restartBackendOnlyWithoutBuild(String workshopId, Workshop workshop) {
        deploymentStages.put(workshopId, "starting");
        String backendServiceName = getBackendServiceNameForWorkshop(workshop);
        List<String> command = new ArrayList<>(List.of(
            "docker-compose",
            "-f",
            getComposeFile(),
            "up",
            "-d",
            "--force-recreate",
            backendServiceName
        ));
        CommandResponse response = runComposeLifecycleCommand(
            workshopId,
            command,
            "[docker-compose up --force-recreate split-backend {}]",
            "Failed to restart workshop backend."
        );
        if (!response.isSuccess()) {
            deploymentStages.remove(workshopId);
            return response;
        }
        deploymentStages.put(workshopId, "ready");
        clearDeploymentStageAsync(workshopId);
        return new CommandResponse(true, "Workshop backend restarted without image rebuild...", response.getOutput());
    }

    private CommandResponse stopBackendOnly(String workshopId, Workshop workshop) {
        String backendServiceName = getBackendServiceNameForWorkshop(workshop);
        List<String> command = new ArrayList<>(List.of("docker-compose", "-f", getComposeFile(), "stop", backendServiceName));
        return runComposeLifecycleCommand(
            workshopId,
            command,
            "[docker-compose stop split-backend {}]",
            "Failed to stop workshop backend."
        );
    }

    private CommandResponse runComposeLifecycleCommand(
        String workshopId,
        List<String> command,
        String logLabel,
        String errorPrefix
    ) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            configureDockerComposeEnvironment(pb);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("{} {}", logLabel, line);
                    updateDeploymentStageFromComposeOutput(workshopId, line);
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Command completed", output.toString());
            }

            String errorMsg = errorPrefix + " Exit code: " + process.exitValue();
            logger.error(errorMsg + "\nOutput:\n{}", output);
            return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
        } catch (Exception e) {
            logger.error(errorPrefix + " " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    private void updateDeploymentStageFromComposeOutput(String workshopId, String line) {
        if (line.contains("Building")) {
            deploymentStages.put(workshopId, "building");
        } else if (line.contains("Creating") || line.contains("Starting") || line.contains("Recreating")) {
            deploymentStages.put(workshopId, "starting");
        }
    }

    private CommandResponse finalizeSuccessfulLifecycle(
        String workshopId,
        long startTime,
        String output,
        String successMessageTemplate
    ) {
        long duration = System.currentTimeMillis() - startTime;
        deploymentStages.put(workshopId, "ready");
        logger.info("Workshop {} lifecycle step finished successfully in {} seconds", workshopId, duration / 1000.0);
        clearDeploymentStageAsync(workshopId);
        return new CommandResponse(true, String.format(successMessageTemplate, duration / 1000.0), output);
    }

    private void clearDeploymentStageAsync(String workshopId) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                deploymentStages.remove(workshopId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private String getFrontendServiceNameForWorkshop(Workshop workshop) {
        String serviceName = workshop.getEffectiveFrontendServiceName();
        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }
        if (workshop.getServiceName() != null && !workshop.getServiceName().isBlank()) {
            return workshop.getServiceName();
        }
        return workshop.getId();
    }

    private String getBackendServiceNameForWorkshop(Workshop workshop) {
        String serviceName = workshop.getEffectiveBackendServiceName();
        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }
        return getFrontendServiceNameForWorkshop(workshop);
    }

    private List<String> getServiceNamesForWorkshop(Workshop workshop) {
        String frontendService = getFrontendServiceNameForWorkshop(workshop);
        String backendService = getBackendServiceNameForWorkshop(workshop);
        if (frontendService.equals(backendService)) {
            return List.of(frontendService);
        }
        return List.of(frontendService, backendService);
    }

    boolean hasDedicatedFrontend(Workshop workshop) {
        return !getFrontendServiceNameForWorkshop(workshop).equals(getBackendServiceNameForWorkshop(workshop));
    }

    String resolveWorkshopStatus(boolean frontendRunning, boolean backendRunning) {
        return frontendRunning ? "running" : "stopped";
    }

    String resolveWorkshopMessage(boolean frontendRunning, boolean backendRunning) {
        if (frontendRunning && !backendRunning) {
            return "Frontend is available, backend is unavailable";
        }
        if (!frontendRunning && backendRunning) {
            return "Partial state: frontend=false, backend=true";
        }
        return null;
    }

    private int getFrontendPortForWorkshop(Workshop workshop) {
        int port = workshop.getEffectiveFrontendPort();
        if (port > 0) {
            return port;
        }
        if (workshop.getPort() > 0) {
            return workshop.getPort();
        }
        return 8080;
    }

    private boolean isDockerServiceRunning(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "sh",
                "-c",
                "docker ps --format '{{.Names}} {{.Status}}' | grep " + serviceName
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("Up");
        } catch (Exception e) {
            logger.error("Error checking Docker service: {}", serviceName, e);
            return false;
        }
    }

    private Workshop getWorkshopById(String workshopId) {
        return registryService.getWorkshops().stream()
            .filter(workshop -> workshopId.equals(workshop.getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Configure environment variables for docker-compose commands.
     * Sets WORKSHOP_ROOT_PATH so docker-compose.internal.yml can use it for build context.
     */
    private void configureDockerComposeEnvironment(ProcessBuilder pb) {
        try {
            // Get the canonical (absolute, normalized) path to the workshop root
            File rootDir = new File(workshopRootPath);
            String absolutePath = rootDir.getCanonicalPath();

            pb.environment().put("WORKSHOP_ROOT_PATH", absolutePath);
            logger.info("Setting WORKSHOP_ROOT_PATH={} for docker-compose (from configured path: {})",
                       absolutePath, workshopRootPath);
        } catch (Exception e) {
            logger.error("Failed to resolve workshop root path: {}", workshopRootPath, e);
            // Fallback to absolute path
            File rootDir = new File(workshopRootPath);
            String absolutePath = rootDir.getAbsolutePath();
            pb.environment().put("WORKSHOP_ROOT_PATH", absolutePath);
            logger.warn("Using fallback absolute path: {}", absolutePath);
        }
    }
}
