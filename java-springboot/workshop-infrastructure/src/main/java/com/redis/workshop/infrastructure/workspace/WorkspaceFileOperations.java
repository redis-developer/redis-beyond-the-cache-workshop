package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class WorkspaceFileOperations {

    private WorkspaceFileOperations() {
    }

    static int copyDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        Path source = Objects.requireNonNull(sourceDirectory, "sourceDirectory").toAbsolutePath().normalize();
        Path target = Objects.requireNonNull(targetDirectory, "targetDirectory").toAbsolutePath().normalize();
        assertNotFilesystemRoot(target, "targetDirectory");
        assertNoPathOverlap(source, target, "sourceDirectory and targetDirectory must not overlap");

        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Source directory does not exist: " + source);
        }

        Files.createDirectories(target);
        int fileCount = 0;
        try (Stream<Path> stream = Files.walk(source)) {
            List<Path> paths = stream.toList();
            for (Path current : paths) {
                Path relative = source.relativize(current);
                Path destination = target.resolve(relative).normalize();
                if (!destination.startsWith(target)) {
                    throw new IOException("Copy target escapes workspace: " + destination);
                }
                if (Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(current, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(current)) {
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(
                        current,
                        destination,
                        LinkOption.NOFOLLOW_LINKS,
                        StandardCopyOption.REPLACE_EXISTING
                    );
                    fileCount++;
                }
            }
        }
        return fileCount;
    }

    static boolean deleteRecursively(Path directory) throws IOException {
        Path target = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        assertNotFilesystemRoot(target, "directory");
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        try (Stream<Path> stream = Files.walk(target)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path current : paths) {
                Files.deleteIfExists(current);
            }
        }
        return true;
    }

    static void assertNoPathOverlap(Path first, Path second, String message) {
        Path left = Objects.requireNonNull(first, "first").toAbsolutePath().normalize();
        Path right = Objects.requireNonNull(second, "second").toAbsolutePath().normalize();
        if (left.equals(right) || left.startsWith(right) || right.startsWith(left)) {
            throw new IllegalArgumentException(message + ": " + left + " overlaps " + right);
        }
    }

    static void assertNotFilesystemRoot(Path path, String name) {
        Path normalized = Objects.requireNonNull(path, name).toAbsolutePath().normalize();
        if (normalized.getParent() == null) {
            throw new IllegalArgumentException(name + " must not be a filesystem root");
        }
    }
}
