package com.redis.workshop.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EditorDiagnosticsService {

    private static final Pattern JAVAC_PATTERN = Pattern.compile("^(?<path>.+?):(?<line>\\d+): (?<severity>error|warning): (?<message>.+)$");
    private static final Pattern KOTLIN_SCRIPT_PATTERN = Pattern.compile("^(?<prefix>[ew]): file:/{2,3}(?<path>.+?):(?<line>\\d+):(?<column>\\d+)\\s+(?<message>.+)$");
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(20);

    private final WorkshopConfig workshopConfig;
    private final FrontendRuntimeProperties runtimeProperties;
    private final CommandExecutor commandExecutor;
    private final Object compileLock = new Object();

    EditorDiagnosticsService(WorkshopConfig workshopConfig, FrontendRuntimeProperties runtimeProperties) {
        this(workshopConfig, runtimeProperties, new ProcessCommandExecutor());
    }

    EditorDiagnosticsService(
        WorkshopConfig workshopConfig,
        FrontendRuntimeProperties runtimeProperties,
        CommandExecutor commandExecutor
    ) {
        this.workshopConfig = workshopConfig;
        this.runtimeProperties = runtimeProperties;
        this.commandExecutor = commandExecutor;
    }

    DiagnosticsResponse collectDiagnostics(Map<String, String> overrides) {
        synchronized (compileLock) {
            Map<String, Path> editablePaths = resolveEditablePaths();
            Map<String, String> normalizedOverrides = normalizeOverrides(overrides, editablePaths);
            Map<String, String> originalContents = new LinkedHashMap<>();

            try {
                applyOverrides(normalizedOverrides, editablePaths, originalContents);

                Path moduleRoot = resolveModuleRoot();
                CommandResult result = commandExecutor.execute(
                    buildCommand(moduleRoot),
                    moduleRoot.getParent(),
                    buildEnvironment(),
                    COMMAND_TIMEOUT
                );

                List<EditorDiagnostic> diagnostics = parseDiagnostics(result.output(), editablePaths);
                String error = null;
                if (result.exitCode() != 0 && diagnostics.isEmpty()) {
                    error = "Unable to collect compiler diagnostics right now.";
                }

                return new DiagnosticsResponse(diagnostics, error);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return new DiagnosticsResponse(List.of(), "Unable to collect compiler diagnostics right now.");
            } catch (IOException exception) {
                return new DiagnosticsResponse(List.of(), "Unable to collect compiler diagnostics right now.");
            } finally {
                restoreOriginalContents(originalContents, editablePaths);
            }
        }
    }

    private void applyOverrides(
        Map<String, String> overrides,
        Map<String, Path> editablePaths,
        Map<String, String> originalContents
    ) throws IOException {
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            Path filePath = editablePaths.get(entry.getKey());
            if (filePath == null) {
                continue;
            }

            originalContents.put(entry.getKey(), Files.readString(filePath, StandardCharsets.UTF_8));
            Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> buildEnvironment() {
        Map<String, String> environment = new LinkedHashMap<>();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            environment.put("JAVA_HOME", javaHome);
        }
        return environment;
    }

    private List<String> buildCommand(Path moduleRoot) {
        Path gradleProjectDir = moduleRoot.getParent();
        Path gradleWrapper = gradleProjectDir.resolve("gradlew");
        return List.of(
            "bash",
            gradleWrapper.toString(),
            "-p",
            gradleProjectDir.toString(),
            "--console=plain",
            ":" + workshopConfig.getModuleName() + ":compileJava"
        );
    }

    private Map<String, String> normalizeOverrides(Map<String, String> overrides, Map<String, Path> editablePaths) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (overrides == null) {
            return normalized;
        }

        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            if (editablePaths.containsKey(entry.getKey()) && entry.getValue() != null) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return normalized;
    }

    private List<EditorDiagnostic> parseDiagnostics(String output, Map<String, Path> editablePaths) {
        Set<EditorDiagnostic> diagnostics = new LinkedHashSet<>();

        for (String line : output.split("\\R")) {
            EditorDiagnostic diagnostic = parseJavaDiagnostic(line, editablePaths);
            if (diagnostic == null) {
                diagnostic = parseKotlinScriptDiagnostic(line, editablePaths);
            }
            if (diagnostic != null) {
                diagnostics.add(diagnostic);
            }
        }

        return diagnostics.stream()
            .sorted(Comparator.comparing(EditorDiagnostic::file).thenComparing(EditorDiagnostic::line))
            .toList();
    }

    private EditorDiagnostic parseJavaDiagnostic(String line, Map<String, Path> editablePaths) {
        Matcher matcher = JAVAC_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String fileName = resolveFileName(matcher.group("path"), editablePaths);
        if (fileName == null) {
            return null;
        }

        return new EditorDiagnostic(
            diagnosticId(fileName, matcher.group("line"), matcher.group("message")),
            fileName,
            Integer.parseInt(matcher.group("line")),
            matcher.group("severity").toLowerCase(Locale.ROOT),
            matcher.group("message")
        );
    }

    private EditorDiagnostic parseKotlinScriptDiagnostic(String line, Map<String, Path> editablePaths) {
        Matcher matcher = KOTLIN_SCRIPT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String fileName = resolveFileName(matcher.group("path"), editablePaths);
        if (fileName == null) {
            return null;
        }

        String severity = matcher.group("prefix").equals("w") ? "warning" : "error";
        return new EditorDiagnostic(
            diagnosticId(fileName, matcher.group("line"), matcher.group("message")),
            fileName,
            Integer.parseInt(matcher.group("line")),
            severity,
            matcher.group("message")
        );
    }

    private String diagnosticId(String fileName, String line, String message) {
        return fileName + "-" + line + "-" + Integer.toHexString(message.hashCode());
    }

    private Map<String, Path> resolveEditablePaths() {
        Path moduleRoot = resolveModuleRoot();
        Map<String, Path> editablePaths = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : workshopConfig.getEditableFiles().entrySet()) {
            editablePaths.put(entry.getKey(), moduleRoot.resolve(entry.getValue()).normalize());
        }
        return editablePaths;
    }

    private String resolveFileName(String rawPath, Map<String, Path> editablePaths) {
        Path diagnosticPath = Paths.get(rawPath).toAbsolutePath().normalize();
        for (Map.Entry<String, Path> entry : editablePaths.entrySet()) {
            if (diagnosticPath.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Path resolveModuleRoot() {
        return runtimeProperties.resolveSourcePath()
            .orElse(Paths.get(workshopConfig.getBasePath()).toAbsolutePath().normalize());
    }

    private void restoreOriginalContents(Map<String, String> originalContents, Map<String, Path> editablePaths) {
        for (Map.Entry<String, String> entry : originalContents.entrySet()) {
            Path filePath = editablePaths.get(entry.getKey());
            if (filePath == null) {
                continue;
            }

            try {
                Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // Best-effort restore keeps diagnostics collection from leaving the editor unusable.
            }
        }
    }

    record DiagnosticsResponse(List<EditorDiagnostic> diagnostics, String error) {
    }

    record EditorDiagnostic(String id, String file, int line, String severity, String message) {
    }

    interface CommandExecutor {
        CommandResult execute(List<String> command, Path workingDirectory, Map<String, String> environment, Duration timeout)
            throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String output) {
    }

    static final class ProcessCommandExecutor implements CommandExecutor {
        @Override
        public CommandResult execute(List<String> command, Path workingDirectory, Map<String, String> environment, Duration timeout)
            throws IOException, InterruptedException {
            Path outputFile = Files.createTempFile("editor-diagnostics", ".log");
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(workingDirectory.toFile());
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(outputFile.toFile());
                processBuilder.environment().putAll(environment);

                Process process = processBuilder.start();
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                }

                String output = Files.readString(outputFile, StandardCharsets.UTF_8);
                if (!finished) {
                    return new CommandResult(-1, output);
                }

                return new CommandResult(process.exitValue(), output);
            } finally {
                Files.deleteIfExists(outputFile);
            }
        }
    }
}
