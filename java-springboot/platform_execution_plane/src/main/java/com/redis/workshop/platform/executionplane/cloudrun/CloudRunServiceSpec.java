package com.redis.workshop.platform.executionplane.cloudrun;

import java.util.Map;

public record CloudRunServiceSpec(
    String projectId,
    String region,
    String serviceName,
    String image,
    String serviceAccount,
    String ingress,
    String vpcConnector,
    int managerPort,
    int cpuMillis,
    int memoryMiB,
    int concurrency,
    int minInstances,
    int maxInstances,
    Map<String, String> labels,
    Map<String, String> environment,
    Map<String, String> secretEnvironment
) {
}
