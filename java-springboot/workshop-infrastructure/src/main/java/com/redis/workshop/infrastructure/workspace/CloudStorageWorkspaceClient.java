package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Path;

public interface CloudStorageWorkspaceClient {

    boolean prefixExists(String bucket, String prefix) throws IOException;

    int uploadDirectory(Path sourceDirectory, String bucket, String prefix) throws IOException;

    int downloadPrefix(String bucket, String prefix, Path targetDirectory) throws IOException;

    boolean deletePrefix(String bucket, String prefix) throws IOException;
}
