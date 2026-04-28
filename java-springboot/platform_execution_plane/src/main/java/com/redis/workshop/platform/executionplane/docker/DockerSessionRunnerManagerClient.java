package com.redis.workshop.platform.executionplane.docker;

public interface DockerSessionRunnerManagerClient {

    void restart(String upstreamBaseUrl, boolean rebuild);
}
