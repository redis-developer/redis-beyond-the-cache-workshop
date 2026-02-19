package com.redis.workshop.hub.service;

import com.redis.workshop.hub.dto.CommandResponse;
import com.redis.workshop.hub.dto.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WorkshopManagerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopManagerService.class);

    @Value("${workshop.root.path:/workshops}")
    private String workshopRootPath;

    // Path to the internal docker-compose file (for DinD mode)
    private static final String INTERNAL_COMPOSE_FILE = "/app/docker-compose.internal.yml";

    public List<ServiceStatus> getAllServiceStatus() {
        List<ServiceStatus> statuses = new ArrayList<>();

        // Check Docker Compose services (internal to DinD)
        statuses.add(checkDockerService("redis", "6379"));
        statuses.add(checkDockerService("redis-insight", "5540"));

        // Check workshop applications - URL points to proxy path
        statuses.add(checkWorkshopService("1_session_management", "8080"));

        return statuses;
    }

    private ServiceStatus checkDockerService(String serviceName, String port) {
        ServiceStatus status = new ServiceStatus();
        status.setName(serviceName);
        status.setType("infrastructure");
        status.setPort(port);

        // For DinD, Redis Insight is accessible via localhost inside the container
        if ("redis".equals(serviceName)) {
            status.setUrl("redis://localhost:" + port);
        } else if ("redis-insight".equals(serviceName)) {
            // Redis Insight accessible via proxy
            status.setUrl("/workshop/redis-insight/");
        } else {
            status.setUrl("http://localhost:" + port);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "docker ps --format '{{.Names}} {{.Status}}' | grep " + serviceName);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && line.contains("Up")) {
                status.setStatus("running");
            } else {
                status.setStatus("stopped");
            }
        } catch (Exception e) {
            logger.error("Error checking Docker service: " + serviceName, e);
            status.setStatus("stopped");
        }

        return status;
    }

    private ServiceStatus checkWorkshopService(String workshopId, String port) {
        ServiceStatus status = new ServiceStatus();
        status.setName(workshopId);
        status.setType("workshop");
        status.setPort(port);
        // URL uses the proxy path instead of direct port access
        status.setUrl("/workshop/session-management/");

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "docker ps --format '{{.Names}} {{.Status}}' | grep session-management");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && line.contains("Up")) {
                status.setStatus("running");
            } else {
                status.setStatus("stopped");
            }
        } catch (Exception e) {
            logger.error("Error checking workshop service: " + workshopId, e);
            status.setStatus("stopped");
        }

        return status;
    }

    public CommandResponse startInfrastructure() {
        try {
            logger.info("Starting infrastructure services inside DinD...");

            File composeFile = new File(INTERNAL_COMPOSE_FILE);
            if (!composeFile.exists()) {
                String error = "Internal docker-compose.yml not found at: " + INTERNAL_COMPOSE_FILE;
                logger.error(error);
                return new CommandResponse(false, error);
            }

            // Start only the infrastructure services (Redis and Redis Insight)
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", INTERNAL_COMPOSE_FILE, "up", "-d", "redis", "redis-insight");
            pb.redirectErrorStream(true);

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
            logger.info("Stopping infrastructure services inside DinD...");

            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", INTERNAL_COMPOSE_FILE, "down");
            pb.redirectErrorStream(true);

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
        try {
            logger.info("Starting workshop inside DinD: {}", workshopId);

            // If something is already running, stop it first
            ServiceStatus status = checkWorkshopService(workshopId, getPortForWorkshop(workshopId));
            if ("running".equals(status.getStatus())) {
                logger.info("Workshop appears to be running already, stopping existing container before start");
                stopWorkshop(workshopId);
                Thread.sleep(2000);
            }

            // Build the image and start the session-management service
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", INTERNAL_COMPOSE_FILE, "up", "-d", "--build", "session-management");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose up session-management] {}", line);
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Workshop container is starting...", output.toString());
            } else {
                String errorMsg = "Failed to start workshop container. Exit code: " + process.exitValue();
                logger.error(errorMsg + "\nOutput:\n{}", output);
                return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
            }
        } catch (Exception e) {
            logger.error("Error starting workshop inside DinD: " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    public CommandResponse stopWorkshop(String workshopId) {
        try {
            logger.info("Stopping workshop inside DinD: {}", workshopId);

            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", INTERNAL_COMPOSE_FILE, "stop", "session-management");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose stop session-management] {}", line);
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
        CommandResponse stopResponse = stopWorkshop(workshopId);

        if (!stopResponse.isSuccess()) {
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

        CommandResponse stopResponse = stopWorkshop(workshopId);
        if (!stopResponse.isSuccess()) {
            return stopResponse;
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", INTERNAL_COMPOSE_FILE, "up", "-d", "--force-recreate", "session-management");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[docker-compose up --force-recreate session-management] {}", line);
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return new CommandResponse(true, "Workshop container restarted without image rebuild...", output.toString());
            } else {
                String errorMsg = "Failed to restart workshop container. Exit code: " + process.exitValue();
                logger.error(errorMsg + "\nOutput:\n{}", output);
                return new CommandResponse(false, errorMsg + "\nOutput:\n" + output);
            }
        } catch (Exception e) {
            logger.error("Error restarting workshop inside DinD: " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    private String getPortForWorkshop(String workshopId) {
        return switch (workshopId) {
            case "1_session_management" -> "8080";
            default -> "8080";
        };
    }
}

