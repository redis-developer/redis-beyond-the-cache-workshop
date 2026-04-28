package com.redis.workshop.platform.executionplane.cloudrun;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "platform.execution-plane.cloud-run",
    name = "client",
    havingValue = "noop",
    matchIfMissing = true
)
public class NoopCloudRunSessionRuntimeClient implements CloudRunSessionRuntimeClient {

    @Override
    public CloudRunSessionService createOrReplace(CloudRunServiceSpec serviceSpec) {
        String uri = "https://" + serviceSpec.serviceName() + "-" + serviceSpec.region() + ".run.app";
        return new CloudRunSessionService(
            serviceSpec.projectId(),
            serviceSpec.region(),
            serviceSpec.serviceName(),
            uri
        );
    }

    @Override
    public CloudRunSessionService restartManager(String projectId, String region, String serviceName, boolean rebuild) {
        return new CloudRunSessionService(
            projectId,
            region,
            serviceName,
            "https://" + serviceName + "-" + region + ".run.app"
        );
    }

    @Override
    public void delete(String projectId, String region, String serviceName) {
        // Real Cloud Run integration is provided by an environment specific client.
    }
}
