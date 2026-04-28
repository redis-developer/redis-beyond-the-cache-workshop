ARG BUILDER_BASE_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNNER_BASE_IMAGE=redis/redisinsight:latest

FROM ${BUILDER_BASE_IMAGE} AS builder

ARG MANAGER_MODULE
ARG WORKSHOP_ID

WORKDIR /workspace
COPY java-springboot ./java-springboot
COPY workshop-frontend-shared ./workshop-frontend-shared
COPY workshops.yaml ./workshops.yaml

WORKDIR /workspace/java-springboot
RUN ./gradlew --no-daemon -PskipFrontendBuild=true ":${MANAGER_MODULE}:bootJar" ":${WORKSHOP_ID}:bootJar"
RUN set -eux; \
    mkdir -p /image/manager /image/child /image/baseline /image/gradle-cache; \
    cp "${MANAGER_MODULE}/build/libs/"*.jar /image/manager/manager.jar; \
    cp "${WORKSHOP_ID}/build/libs/"*.jar /image/child/child.jar; \
    cp -R /workspace/java-springboot /image/baseline/java-springboot; \
    cp -R /workspace/workshop-frontend-shared /image/baseline/workshop-frontend-shared; \
    cp /workspace/workshops.yaml /image/baseline/workshops.yaml; \
    cp -a /root/.gradle/. /image/gradle-cache/; \
    rm -rf /image/gradle-cache/native

FROM ${RUNNER_BASE_IMAGE} AS runner

ARG MANAGER_MODULE
ARG WORKSHOP_ID
ARG RUNNER_VARIANT=local-redis-insight
ARG SOURCE_REVISION=local
ARG REDIS_INSIGHT_ARCHIVE_NAME=

USER root

LABEL org.opencontainers.image.title="Redis Workshop Cloud Run Session Runner"
LABEL org.opencontainers.image.description="Session scoped workshop runner image with manager and selected baseline source"
LABEL org.opencontainers.image.source-revision="${SOURCE_REVISION}"
LABEL com.redis.workshop.runner.module="${MANAGER_MODULE}"
LABEL com.redis.workshop.runner.workshop-id="${WORKSHOP_ID}"
LABEL com.redis.workshop.runner.variant="${RUNNER_VARIANT}"
LABEL com.redis.workshop.runner.manager-port="8080"
LABEL com.redis.workshop.runner.child-auto-start="true"

# Keep this fully musl based. libc compatibility packages make Gradle select
# linux-gnu native file event libraries, which fail inside Alpine at rebuild time.
RUN set -eux; \
    apk add --no-cache bash ca-certificates curl gradle openjdk21-jdk procps redis

WORKDIR /opt/runner
COPY --from=builder /image /opt/runner

RUN set -eux; \
    mkdir -p /opt/runner/bin /opt/runner/workspace; \
    printf '%s\n' \
      'org.gradle.daemon=false' \
      'org.gradle.native=false' \
      'org.gradle.vfs.watch=false' \
      'org.gradle.jvmargs=-Xmx512m -Dfile.encoding=UTF-8 -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false' \
      > /opt/runner/gradle-cache/gradle.properties; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'rm -rf "${GRADLE_USER_HOME:-/opt/runner/gradle-cache}/native"' \
      'export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false"' \
      'gradle --no-daemon --no-watch-fs -PskipFrontendBuild=true ":${WORKSHOP_ID}:bootJar" -x test' \
      'rebuilt_jar="$(find "${WORKSHOP_ID}/build/libs" -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" -print -quit)"' \
      'test -n "${rebuilt_jar}"' \
      'cp "${rebuilt_jar}" "${WORKSHOP_REBUILT_CHILD_JAR}"' \
      > /opt/runner/bin/rebuild-child.sh; \
    chmod +x /opt/runner/bin/rebuild-child.sh; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'export SERVER_PORT="${PORT:-${SERVER_PORT:-8080}}"' \
      'export WORKSHOP_SESSION_RUNNER_ENABLED="${WORKSHOP_SESSION_RUNNER_ENABLED:-true}"' \
      'export WORKSHOP_SESSION_RUNNER_AUTO_START="${WORKSHOP_SESSION_RUNNER_AUTO_START:-true}"' \
      'export WORKSHOP_SESSION_RUNNER_STARTUP_TIMEOUT="${WORKSHOP_SESSION_RUNNER_STARTUP_TIMEOUT:-90s}"' \
      'export WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT="${WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT:-180s}"' \
      'export WORKSHOP_BASELINE_PATH="${WORKSHOP_BASELINE_PATH:-/opt/runner/baseline}"' \
      'export WORKSHOP_SESSION_ROOT_PATH="${WORKSHOP_SESSION_ROOT_PATH:-/opt/runner/workspace}"' \
      'export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/opt/runner/gradle-cache}"' \
      'export WORKSHOP_REBUILT_CHILD_JAR="${WORKSHOP_REBUILT_CHILD_JAR:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/child.jar}"' \
      'export WORKSHOP_CHILD_PORT="${WORKSHOP_CHILD_PORT:-18080}"' \
      'if [ ! -f "${WORKSHOP_SESSION_ROOT_PATH}/java-springboot/settings.gradle.kts" ]; then' \
      '  rm -rf "${WORKSHOP_SESSION_ROOT_PATH:?}/"*' \
      '  cp -a "${WORKSHOP_BASELINE_PATH}/." "${WORKSHOP_SESSION_ROOT_PATH}/"' \
      'fi' \
      'mkdir -p "${WORKSHOP_SESSION_ROOT_PATH}/.runner"' \
      'export WORKSHOP_SESSION_WORKSPACE_PATH="${WORKSHOP_SESSION_WORKSPACE_PATH:-${WORKSHOP_SESSION_ROOT_PATH}/java-springboot/${WORKSHOP_ID}}"' \
      'export WORKSHOP_CHILD_WORKING_DIRECTORY="${WORKSHOP_CHILD_WORKING_DIRECTORY:-${WORKSHOP_SESSION_ROOT_PATH}/java-springboot}"' \
      'export WORKSHOP_CHILD_REBUILD_COMMAND="${WORKSHOP_CHILD_REBUILD_COMMAND:-/opt/runner/bin/rebuild-child.sh}"' \
      'export WORKSHOP_CHILD_COMMAND="${WORKSHOP_CHILD_COMMAND:-if [ -f \"${WORKSHOP_REBUILT_CHILD_JAR}\" ]; then exec java ${WORKSHOP_CHILD_JAVA_OPTS:-} -jar \"${WORKSHOP_REBUILT_CHILD_JAR}\"; else exec java ${WORKSHOP_CHILD_JAVA_OPTS:-} -jar /opt/runner/child/child.jar; fi}"' \
      'export WORKSHOP_CHILD_HEALTH_PATH="${WORKSHOP_CHILD_HEALTH_PATH:-/login}"' \
      'export WORKSHOP_SESSION_BACKEND_URL="${WORKSHOP_SESSION_BACKEND_URL:-http://127.0.0.1:${WORKSHOP_CHILD_PORT}}"' \
      'export WORKSHOP_BACKEND_URL="${WORKSHOP_BACKEND_URL:-http://127.0.0.1:${WORKSHOP_CHILD_PORT}}"' \
      'export WORKSHOP_REDIS_MODE="local-process"' \
      'export WORKSHOP_LOCAL_REDIS_PORT="${WORKSHOP_LOCAL_REDIS_PORT:-6379}"' \
      'export WORKSHOP_LOCAL_REDIS_COMMAND="${WORKSHOP_LOCAL_REDIS_COMMAND:-redis-server --bind 127.0.0.1 --port ${WORKSHOP_LOCAL_REDIS_PORT} --save \"\" --appendonly no}"' \
      'export WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND="${WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND:-redis-cli -h 127.0.0.1 -p ${WORKSHOP_LOCAL_REDIS_PORT} ping}"' \
      'export WORKSHOP_LOCAL_REDIS_INSIGHT_PORT="${WORKSHOP_LOCAL_REDIS_INSIGHT_PORT:-5540}"' \
      'export WORKSHOP_LOCAL_REDIS_INSIGHT_HEALTH_PATH="${WORKSHOP_LOCAL_REDIS_INSIGHT_HEALTH_PATH:-/redis-insight/api/settings}"' \
      'export WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND="${WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND:-cd /usr/src/app && env RI_APP_HOST=127.0.0.1 RI_APP_PORT=${WORKSHOP_LOCAL_REDIS_INSIGHT_PORT} RI_PROXY_PATH=/redis-insight RI_APP_FOLDER_ABSOLUTE_PATH=${WORKSHOP_SESSION_ROOT_PATH}/.redisinsight RI_REDIS_HOST=127.0.0.1 RI_REDIS_PORT=${WORKSHOP_LOCAL_REDIS_PORT} RI_REDIS_ALIAS=SessionRedis RI_ACCEPT_TERMS_AND_CONDITIONS=true RI_REDIS_TLS=false NODE_ENV=production RI_SERVE_STATICS=true RI_BUILD_TYPE=DOCKER_ON_PREMISE ./docker-entry.sh node redisinsight/api/dist/src/main}"' \
      'exec java ${JAVA_OPTS:-} -jar /opt/runner/manager/manager.jar --server.port="${SERVER_PORT}"' \
      > /opt/runner/bin/entrypoint.sh; \
    chmod +x /opt/runner/bin/entrypoint.sh

ENV RUNNER_VARIANT="${RUNNER_VARIANT}"
ENV WORKSHOP_ID="${WORKSHOP_ID}"
ENV MANAGER_MODULE="${MANAGER_MODULE}"
ENV PORT=8080
ENV WORKSHOP_CHILD_PORT=18080
ENV WORKSHOP_REBUILT_CHILD_JAR=/opt/runner/workspace/.runner/child.jar
ENV WORKSHOP_SESSION_RUNNER_ENABLED=true
ENV WORKSHOP_SESSION_RUNNER_AUTO_START=true
ENV WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT=180s
ENV WORKSHOP_REDIS_MODE=local-process
ENV WORKSHOP_BASELINE_PATH=/opt/runner/baseline
ENV WORKSHOP_SESSION_ROOT_PATH=/opt/runner/workspace
ENV GRADLE_USER_HOME=/opt/runner/gradle-cache
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl -fsS "http://127.0.0.1:${PORT}/health" || exit 1
ENTRYPOINT ["/opt/runner/bin/entrypoint.sh"]
