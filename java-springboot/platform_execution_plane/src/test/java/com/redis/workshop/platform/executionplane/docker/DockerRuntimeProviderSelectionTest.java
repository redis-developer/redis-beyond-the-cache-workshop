package com.redis.workshop.platform.executionplane.docker;

import com.redis.workshop.platform.executionplane.cloudrun.CloudRunRuntimeProperties;
import com.redis.workshop.platform.executionplane.cloudrun.CloudRunSessionRuntimeAdapter;
import com.redis.workshop.platform.executionplane.cloudrun.NoopCloudRunSessionRuntimeClient;
import com.redis.workshop.platform.executionplane.session.ExecutionPlaneRuntimeAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DockerRuntimeProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(RuntimeProviderSelectionConfiguration.class);

    @Test
    void selectsCloudRunProviderByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExecutionPlaneRuntimeAdapter.class);
            assertThat(context).hasSingleBean(CloudRunSessionRuntimeAdapter.class);
            assertThat(context).doesNotHaveBean(DockerSessionRuntimeAdapter.class);
        });
    }

    @Test
    void selectsDockerProviderWhenConfigured() {
        contextRunner.withPropertyValues("platform.execution-plane.runtime-provider=docker").run(context -> {
            assertThat(context).hasSingleBean(ExecutionPlaneRuntimeAdapter.class);
            assertThat(context).hasSingleBean(DockerSessionRuntimeAdapter.class);
            assertThat(context).doesNotHaveBean(CloudRunSessionRuntimeAdapter.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
        CloudRunSessionRuntimeAdapter.class,
        CloudRunRuntimeProperties.class,
        NoopCloudRunSessionRuntimeClient.class,
        DockerSessionRuntimeAdapter.class,
        DockerRuntimeProperties.class
    })
    static class RuntimeProviderSelectionConfiguration {

        @Bean(name = "conversionService")
        ConversionService conversionService() {
            return ApplicationConversionService.getSharedInstance();
        }

        @Bean
        DockerCommand dockerCommand() {
            return mock(DockerCommand.class);
        }

        @Bean
        DockerSessionRunnerManagerClient dockerSessionRunnerManagerClient() {
            return mock(DockerSessionRunnerManagerClient.class);
        }
    }
}
