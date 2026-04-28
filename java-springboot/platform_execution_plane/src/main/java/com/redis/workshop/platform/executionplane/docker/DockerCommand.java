package com.redis.workshop.platform.executionplane.docker;

import java.util.List;

public interface DockerCommand {

    DockerCommandResult run(List<String> arguments);
}
