package com.redis.workshop.platform.executionplane.cloudrun;

public interface CloudRunSessionRuntimeClient {

    CloudRunSessionService createOrReplace(CloudRunServiceSpec serviceSpec);

    CloudRunSessionService restartManager(String projectId, String region, String serviceName, boolean rebuild);

    void delete(String projectId, String region, String serviceName);
}
