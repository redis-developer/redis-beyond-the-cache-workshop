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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WorkshopManagerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopManagerService.class);

    private final ConcurrentHashMap<String, Process> runningProcesses = new ConcurrentHashMap<>();

    @Value("${workshop.root.path:..}")
    private String workshopRootPath;

    public List<ServiceStatus> getAllServiceStatus() {
        List<ServiceStatus> statuses = new ArrayList<>();

        // Check Docker Compose services
        statuses.add(checkDockerService("redis", "6379"));
        statuses.add(checkDockerService("redis-insight", "5540"));

        // Check workshop applications
        statuses.add(checkWorkshopService("1_session_management", "8080"));

        return statuses;
    }

    private ServiceStatus checkDockerService(String serviceName, String port) {
        ServiceStatus status = new ServiceStatus();
        status.setName(serviceName);
        status.setType("infrastructure");
        status.setPort(port);

        if ("redis".equals(serviceName)) {
            status.setUrl("redis://localhost:" + port);
        } else {
            status.setUrl("http://localhost:" + port);
        }

        try {
            // Use docker ps with grep to find containers with the service name
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
        status.setUrl("http://localhost:" + port);

        // Check if port is in use
        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-ti:" + port);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && !line.trim().isEmpty()) {
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
            File rootDir = new File(workshopRootPath).getAbsoluteFile();
            logger.info("Starting Docker Compose infrastructure from: {}", rootDir.getAbsolutePath());

            if (!rootDir.exists()) {
                String error = "Workshop root directory does not exist: " + rootDir.getAbsolutePath();
                logger.error(error);
                return new CommandResponse(false, error);
            }

            File dockerComposeFile = new File(rootDir, "docker-compose.yml");
            if (!dockerComposeFile.exists()) {
                String error = "docker-compose.yml not found at: " + dockerComposeFile.getAbsolutePath();
                logger.error(error);
                return new CommandResponse(false, error);
            }

            ProcessBuilder pb = new ProcessBuilder("docker-compose", "up", "-d");
            pb.directory(rootDir);
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
                String errorMsg = "Failed to start infrastructure. Exit code: " + process.exitValue() + "\nOutput: " + output.toString();
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
            logger.info("Stopping Docker Compose infrastructure...");

            File rootDir = new File(workshopRootPath);
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "down");
            pb.directory(rootDir);
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
            logger.info("Starting workshop: " + workshopId);

            // Kill any existing process on the port first
            ServiceStatus status = checkWorkshopService(workshopId, getPortForWorkshop(workshopId));
            if ("running".equals(status.getStatus())) {
                stopWorkshop(workshopId);
                Thread.sleep(2000); // Wait for port to be released
            }

            File workshopDir = new File(workshopRootPath + "/java-springboot").getAbsoluteFile();
            logger.info("Workshop directory: " + workshopDir.getAbsolutePath());

            if (!workshopDir.exists()) {
                String error = "Workshop directory does not exist: " + workshopDir.getAbsolutePath();
                logger.error(error);
                return new CommandResponse(false, error);
            }

            File gradlewFile = new File(workshopDir, "gradlew");
            if (!gradlewFile.exists()) {
                String error = "gradlew not found at: " + gradlewFile.getAbsolutePath();
                logger.error(error);
                return new CommandResponse(false, error);
            }

            // Run clean build first to ensure changes to build.gradle.kts are applied
            logger.info("Running clean build for workshop: " + workshopId);
            ProcessBuilder cleanBuild = new ProcessBuilder(gradlewFile.getAbsolutePath(), ":" + workshopId + ":clean", ":" + workshopId + ":build", "-x", "test", "--no-daemon");
            cleanBuild.directory(workshopDir);
            cleanBuild.redirectErrorStream(true);
            Process buildProcess = cleanBuild.start();

            // Read build output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(buildProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[" + workshopId + " build] " + line);
                }
            }

            boolean buildFinished = buildProcess.waitFor(120, TimeUnit.SECONDS);
            if (!buildFinished || buildProcess.exitValue() != 0) {
                String error = "Build failed for workshop: " + workshopId;
                logger.error(error);
                return new CommandResponse(false, error);
            }
            logger.info("Build completed successfully for workshop: " + workshopId);

            ProcessBuilder pb = new ProcessBuilder(gradlewFile.getAbsolutePath(), ":" + workshopId + ":bootRun", "--no-daemon");
            pb.directory(workshopDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            runningProcesses.put(workshopId, process);

            // Start a thread to read output
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[" + workshopId + "] " + line);
                    }
                } catch (Exception e) {
                    logger.error("Error reading workshop output", e);
                }
            }).start();

            // Wait a bit to see if it starts successfully
            Thread.sleep(3000);

            if (process.isAlive()) {
                return new CommandResponse(true, "Workshop " + workshopId + " is starting...");
            } else {
                return new CommandResponse(false, "Workshop " + workshopId + " failed to start");
            }
        } catch (Exception e) {
            logger.error("Error starting workshop: " + workshopId, e);
            return new CommandResponse(false, "Error: " + e.getMessage());
        }
    }

    public CommandResponse stopWorkshop(String workshopId) {
        try {
            logger.info("Stopping workshop: " + workshopId);

            String port = getPortForWorkshop(workshopId);

            // Kill process by port
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "lsof -ti:" + port + " | xargs kill -9 2>/dev/null || true");
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);

            // Remove from running processes
            Process runningProcess = runningProcesses.remove(workshopId);
            if (runningProcess != null && runningProcess.isAlive()) {
                runningProcess.destroy();
                runningProcess.waitFor(5, TimeUnit.SECONDS);
                if (runningProcess.isAlive()) {
                    runningProcess.destroyForcibly();
                }
            }

            return new CommandResponse(true, "Workshop " + workshopId + " stopped");
        } catch (Exception e) {
            logger.error("Error stopping workshop: " + workshopId, e);
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
            Thread.sleep(3000); // Wait longer for port to be released and cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return startWorkshop(workshopId);
    }

    private String getPortForWorkshop(String workshopId) {
        // Map workshop IDs to their ports
        switch (workshopId) {
            case "1_session_management":
                return "8080";
            default:
                return "8080";
        }
    }
}

