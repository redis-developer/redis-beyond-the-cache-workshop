ARG BUILDER_BASE_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNNER_BASE_IMAGE=redis/redisinsight:latest

FROM ${BUILDER_BASE_IMAGE} AS builder

ARG MANAGER_MODULE
ARG WORKSHOP_ID
ARG MANAGER_SOURCE_DIR=${MANAGER_MODULE}
ARG WORKSHOP_SOURCE_DIR=${WORKSHOP_ID}

WORKDIR /workspace
COPY java-springboot ./java-springboot
COPY workshop-frontend-shared ./workshop-frontend-shared
COPY workshops.yaml ./workshops.yaml

WORKDIR /workspace/java-springboot
RUN ./gradlew --no-daemon -PskipFrontendBuild=true ":${MANAGER_MODULE}:bootJar" ":${WORKSHOP_ID}:bootJar"
RUN set -eux; \
    printf '%s\n' \
      'gradle.projectsEvaluated {' \
      '  allprojects { project ->' \
      '    if (project.path == ":'"${WORKSHOP_ID}"'") {' \
      '      def warmup = project.configurations.findByName("workshopWarmupRuntime") ?: project.configurations.create("workshopWarmupRuntime")' \
      '      project.dependencies.add("workshopWarmupRuntime", "org.springframework.boot:spring-boot-starter-data-redis")' \
      '      project.dependencies.add("workshopWarmupRuntime", "org.springframework.session:spring-session-data-redis")' \
      '      project.tasks.register("warmWorkshopDependencies") {' \
      '        doLast { warmup.resolve() }' \
      '      }' \
      '    }' \
      '  }' \
      '}' \
      > /tmp/workshop-warm-dependencies.gradle; \
    ./gradlew --no-daemon -I /tmp/workshop-warm-dependencies.gradle -PskipFrontendBuild=true ":${WORKSHOP_ID}:warmWorkshopDependencies" || true
RUN set -eux; \
    mkdir -p /image/manager /image/child /image/baseline /image/gradle-cache; \
    cp "${MANAGER_SOURCE_DIR}/build/libs/"*.jar /image/manager/manager.jar; \
    cp "${WORKSHOP_SOURCE_DIR}/build/libs/"*.jar /image/child/child.jar; \
    cp -R /workspace/java-springboot /image/baseline/java-springboot; \
    cp -R /workspace/workshop-frontend-shared /image/baseline/workshop-frontend-shared; \
    cp /workspace/workshops.yaml /image/baseline/workshops.yaml; \
    rm -rf \
      /root/.gradle/.tmp \
      /root/.gradle/daemon \
      /root/.gradle/native \
      /root/.gradle/caches/build-cache-* \
      /root/.gradle/caches/journal-* \
      /root/.gradle/caches/*/executionHistory \
      /root/.gradle/caches/*/fileChanges \
      /root/.gradle/caches/*/generated-gradle-jars; \
    cp -a /root/.gradle/caches /root/.gradle/wrapper /image/gradle-cache/

FROM ${RUNNER_BASE_IMAGE} AS runner

ARG MANAGER_MODULE
ARG WORKSHOP_ID
ARG MANAGER_SOURCE_DIR=${MANAGER_MODULE}
ARG WORKSHOP_SOURCE_DIR=${WORKSHOP_ID}
ARG RUNNER_VARIANT=local-redis-insight
ARG SOURCE_REVISION=local
ARG REDIS_INSIGHT_ARCHIVE_NAME=
ARG CODE_SERVER_VERSION=4.103.2

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
    apk add --no-cache bash build-base ca-certificates curl gradle krb5-dev libstdc++ nodejs npm openjdk21-jdk procps python3 redis; \
    PYTHON=/usr/bin/python3 npm_config_python=/usr/bin/python3 npm install --global "code-server@${CODE_SERVER_VERSION}" --unsafe-perm --ignore-scripts; \
    code_server_module="$(npm root --global)/code-server"; \
    cd "${code_server_module}"; \
    PYTHON=/usr/bin/python3 npm_config_python=/usr/bin/python3 npm_config_unsafe_perm=true npm rebuild; \
    PYTHON=/usr/bin/python3 npm_config_python=/usr/bin/python3 npm_config_unsafe_perm=true npm run postinstall; \
    vsda_module_dir="${code_server_module}/lib/vscode/node_modules/vsda"; \
    vsda_web_dir="${vsda_module_dir}/rust/web"; \
    mkdir -p "${vsda_web_dir}"; \
    printf '%s\n' \
      '{"name":"vsda","version":"0.0.0-workshop-shim","main":"index.js"}' \
      > "${vsda_module_dir}/package.json"; \
    printf '%s\n' \
      'class signer {' \
      '  sign(value) { return typeof value === "string" ? value : String(value ?? ""); }' \
      '}' \
      'class validator {' \
      '  createNewMessage(value) { return typeof value === "string" ? value : String(value ?? ""); }' \
      '  validate() { return "ok"; }' \
      '}' \
      'module.exports = { signer, validator };' \
      > "${vsda_module_dir}/index.js"; \
    printf '%s\n' \
      'const workshopVsda = {' \
      '  default: async () => undefined,' \
      '  sign: async value => value,' \
      '  validator: class {' \
      '    createNewMessage(value) { return value; }' \
      '    validate() { return "ok"; }' \
      '    free() {}' \
      '  }' \
      '};' \
      'globalThis.vsda_web = globalThis.vsda_web || workshopVsda;' \
      'if (typeof define === "function") {' \
      '  define([], () => workshopVsda);' \
      '}' \
      > "${vsda_web_dir}/vsda.js"; \
    : > "${vsda_web_dir}/vsda_bg.wasm"; \
    vscode_extensions_dir="${code_server_module}/lib/vscode/extensions"; \
    write_extension_stub() { \
      extension_stub_path="${vscode_extensions_dir}/$1"; \
      mkdir -p "$(dirname "${extension_stub_path}")"; \
      printf '%s\n' \
        'define(["require", "exports"], function (require, exports) {' \
        '  "use strict";' \
        '  Object.defineProperty(exports, "__esModule", { value: true });' \
        '  exports.activate = function () { return {}; };' \
        '  exports.deactivate = function () {};' \
        '});' \
        > "${extension_stub_path}"; \
    }; \
    write_extension_stub "configuration-editing/dist/browser/configurationEditingMain.js"; \
    write_extension_stub "css-language-features/client/dist/browser/cssClientMain.js"; \
    write_extension_stub "emmet/dist/browser/emmetBrowserMain.js"; \
    write_extension_stub "extension-editing/dist/browser/extensionEditingBrowserMain.js"; \
    write_extension_stub "git-base/dist/browser/extension.js"; \
    write_extension_stub "github-authentication/dist/browser/extension.js"; \
    write_extension_stub "html-language-features/client/dist/browser/htmlClientMain.js"; \
    write_extension_stub "ipynb/dist/browser/ipynbMain.browser.js"; \
    write_extension_stub "json-language-features/client/dist/browser/jsonClientMain.js"; \
    write_extension_stub "markdown-language-features/dist/browser/extension.js"; \
    write_extension_stub "markdown-math/dist/browser/extension.js"; \
    write_extension_stub "media-preview/dist/browser/extension.js"; \
    write_extension_stub "merge-conflict/dist/browser/mergeConflictMain.js"; \
    write_extension_stub "microsoft-authentication/dist/browser/extension.js"; \
    write_extension_stub "npm/dist/browser/npmBrowserMain.js"; \
    write_extension_stub "simple-browser/dist/browser/extension.js"; \
    write_extension_stub "typescript-language-features/dist/browser/extension.js"; \
    code-server --version; \
    npm cache clean --force

WORKDIR /opt/runner
COPY --from=builder /image /opt/runner

RUN set -eux; \
    mkdir -p /opt/runner/bin /opt/runner/workspace; \
    printf '%s\n' \
      'org.gradle.daemon=true' \
      'org.gradle.configuration-cache=true' \
      'org.gradle.configuration-cache.problems=warn' \
      'org.gradle.native=false' \
      'org.gradle.vfs.watch=false' \
      'org.gradle.jvmargs=-Xmx512m -Dfile.encoding=UTF-8 -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false' \
      > /opt/runner/gradle-cache/gradle.properties; \
    printf '%s\n' \
      'import org.gradle.api.DefaultTask' \
      'import org.gradle.api.file.ConfigurableFileCollection' \
      'import org.gradle.api.file.RegularFileProperty' \
      'import org.gradle.api.tasks.Classpath' \
      'import org.gradle.api.tasks.OutputFile' \
      'import org.gradle.api.tasks.TaskAction' \
      '' \
      'abstract class WriteWorkshopRuntimeClasspath extends DefaultTask {' \
      '  @Classpath' \
      '  abstract ConfigurableFileCollection getRuntimeClasspath()' \
      '' \
      '  @OutputFile' \
      '  abstract RegularFileProperty getOutputFile()' \
      '' \
      '  @TaskAction' \
      '  void writeRuntimeClasspath() {' \
      '    def file = outputFile.get().asFile' \
      '    file.parentFile.mkdirs()' \
      '    file.text = runtimeClasspath.asPath' \
      '  }' \
      '}' \
      '' \
      'gradle.projectsEvaluated {' \
      '  allprojects { project ->' \
      '    def sourceSets = project.extensions.findByName("sourceSets")' \
      '    if (sourceSets != null && project.tasks.findByName("writeWorkshopRuntimeClasspath") == null) {' \
      '      def outputPath = project.providers.gradleProperty("workshopRuntimeClasspathFile").get()' \
      '      def runtimeClasspathOutputFile = project.file(outputPath)' \
      '      project.tasks.register("writeWorkshopRuntimeClasspath", WriteWorkshopRuntimeClasspath) {' \
      '        runtimeClasspath.from(sourceSets.named("main").map { it.runtimeClasspath })' \
      '        outputFile.set(runtimeClasspathOutputFile)' \
      '      }' \
      '    }' \
      '  }' \
      '}' \
      > /opt/runner/bin/workshop-runtime-classpath.gradle; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'rm -rf "${GRADLE_USER_HOME:-/opt/runner/gradle-cache}/native"' \
      'export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false"' \
      'export WORKSHOP_RUNTIME_CLASSPATH_FILE="${WORKSHOP_RUNTIME_CLASSPATH_FILE:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/runtime-classpath.txt}"' \
      'export WORKSHOP_DEV_CLASSES_READY="${WORKSHOP_DEV_CLASSES_READY:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/dev-classes-ready}"' \
      'mkdir -p "$(dirname "${WORKSHOP_RUNTIME_CLASSPATH_FILE}")"' \
      'lock_dir="${WORKSHOP_SESSION_ROOT_PATH}/.runner/gradle-build.lock"' \
      'while ! mkdir "${lock_dir}" 2>/dev/null; do' \
      '  echo "waiting for Gradle warmup to finish"' \
      '  sleep 1' \
      'done' \
      'trap '"'"'rmdir "${lock_dir}" 2>/dev/null || true'"'"' EXIT' \
      'chmod +x ./gradlew' \
      './gradlew --console=plain --no-watch-fs -I /opt/runner/bin/workshop-runtime-classpath.gradle -PskipFrontendBuild=true -PworkshopRuntimeClasspathFile="${WORKSHOP_RUNTIME_CLASSPATH_FILE}" ":${WORKSHOP_ID}:classes" ":${WORKSHOP_ID}:writeWorkshopRuntimeClasspath" -x test' \
      'touch "${WORKSHOP_DEV_CLASSES_READY}"' \
      > /opt/runner/bin/rebuild-child.sh; \
    chmod +x /opt/runner/bin/rebuild-child.sh; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false"' \
      'export WORKSHOP_RUNTIME_CLASSPATH_FILE="${WORKSHOP_RUNTIME_CLASSPATH_FILE:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/runtime-classpath.txt}"' \
      'mkdir -p "$(dirname "${WORKSHOP_RUNTIME_CLASSPATH_FILE}")"' \
      'lock_dir="${WORKSHOP_SESSION_ROOT_PATH}/.runner/gradle-build.lock"' \
      'if ! mkdir "${lock_dir}" 2>/dev/null; then' \
      '  echo "gradle warmup skipped because another build is running"' \
      '  exit 0' \
      'fi' \
      'trap '"'"'rmdir "${lock_dir}" 2>/dev/null || true'"'"' EXIT' \
      'echo "gradle warmup started"' \
      'chmod +x ./gradlew' \
      'if ./gradlew --console=plain --no-watch-fs -I /opt/runner/bin/workshop-runtime-classpath.gradle -PskipFrontendBuild=true -PworkshopRuntimeClasspathFile="${WORKSHOP_RUNTIME_CLASSPATH_FILE}" ":${WORKSHOP_ID}:classes" ":${WORKSHOP_ID}:writeWorkshopRuntimeClasspath" -x test >/tmp/workshop-gradle-warmup.log 2>&1; then' \
      '  echo "gradle warmup completed"' \
      'else' \
      '  echo "gradle warmup failed, rebuild will retry on demand"' \
      '  tail -n 20 /tmp/workshop-gradle-warmup.log || true' \
      'fi' \
      > /opt/runner/bin/warm-gradle.sh; \
    chmod +x /opt/runner/bin/warm-gradle.sh; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'export WORKSHOP_RUNTIME_CLASSPATH_FILE="${WORKSHOP_RUNTIME_CLASSPATH_FILE:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/runtime-classpath.txt}"' \
      'export WORKSHOP_DEV_CLASSES_READY="${WORKSHOP_DEV_CLASSES_READY:-${WORKSHOP_SESSION_ROOT_PATH}/.runner/dev-classes-ready}"' \
      'if [ -f "${WORKSHOP_DEV_CLASSES_READY}" ] && [ -s "${WORKSHOP_RUNTIME_CLASSPATH_FILE}" ]; then' \
      '  main_source="$(find "${WORKSHOP_SOURCE_DIR}/src/main/java" -name "*Application.java" -print -quit)"' \
      '  if [ -n "${main_source}" ]; then' \
      '    package_name="$(sed -n '"'"'s/^package \(.*\);$/\1/p'"'"' "${main_source}" | head -n 1)"' \
      '    class_name="$(basename "${main_source}" .java)"' \
      '    main_class="${package_name:+${package_name}.}${class_name}"' \
      '    classpath="${WORKSHOP_SOURCE_DIR}/build/classes/java/main:${WORKSHOP_SOURCE_DIR}/build/resources/main:$(cat "${WORKSHOP_RUNTIME_CLASSPATH_FILE}")"' \
      '    exec java ${WORKSHOP_CHILD_JAVA_OPTS:-} -cp "${classpath}" "${main_class}"' \
      '  fi' \
      'fi' \
      'if [ -f "${WORKSHOP_REBUILT_CHILD_JAR}" ]; then' \
      '  exec java ${WORKSHOP_CHILD_JAVA_OPTS:-} -jar "${WORKSHOP_REBUILT_CHILD_JAR}"' \
      'fi' \
      'exec java ${WORKSHOP_CHILD_JAVA_OPTS:-} -jar /opt/runner/child/child.jar' \
      > /opt/runner/bin/start-child.sh; \
    chmod +x /opt/runner/bin/start-child.sh; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      ': "${WORKSHOP_SESSION_WORKSPACE_PATH:?WORKSHOP_SESSION_WORKSPACE_PATH is required}"' \
      'editor_port="${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"' \
      'editor_data_dir="${WORKSHOP_LOCAL_CODE_EDITOR_DATA_DIR:-${WORKSHOP_SESSION_ROOT_PATH:-/opt/runner/workspace}/.code-server}"' \
      'mkdir -p "${editor_data_dir}/User" "${editor_data_dir}/extensions"' \
      'cat > "${editor_data_dir}/User/settings.json" <<'"'"'JSON'"'"'' \
      '{' \
      '  "extensions.autoCheckUpdates": false,' \
      '  "extensions.autoUpdate": false,' \
      '  "extensions.ignoreRecommendations": true,' \
      '  "git.autofetch": false,' \
      '  "git.decorations.enabled": false,' \
      '  "git.enabled": false,' \
      '  "remote.autoForwardPorts": false,' \
      '  "security.workspace.trust.enabled": false,' \
      '  "telemetry.telemetryLevel": "off",' \
      '  "terminal.integrated.defaultProfile.linux": "workshop-disabled",' \
      '  "terminal.integrated.profiles.linux": {' \
      '    "workshop-disabled": {' \
      '      "path": "/bin/false"' \
      '    }' \
      '  },' \
      '  "typescript.disableAutomaticTypeAcquisition": true,' \
      '  "update.mode": "none",' \
      '  "workbench.enableExperiments": false,' \
      '  "workbench.iconTheme": null,' \
      '  "workbench.settings.enableNaturalLanguageSearch": false,' \
      '  "workbench.startupEditor": "none"' \
      '}' \
      'JSON' \
      'unset PORT SERVER_PORT' \
      'export SHELL=/bin/false' \
      'exec code-server \' \
      '  --auth none \' \
      '  --bind-addr "127.0.0.1:${editor_port}" \' \
      '  --disable-proxy \' \
      '  --disable-telemetry \' \
      '  --disable-update-check \' \
      '  --disable-workspace-trust \' \
      '  --extensions-dir "${editor_data_dir}/extensions" \' \
      '  --user-data-dir "${editor_data_dir}" \' \
      '  "${WORKSHOP_SESSION_WORKSPACE_PATH}"' \
      > /opt/runner/bin/start-code-editor.sh; \
    chmod +x /opt/runner/bin/start-code-editor.sh; \
    printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      'export SERVER_PORT="${PORT:-${SERVER_PORT:-8080}}"' \
      'export WORKSHOP_SESSION_RUNNER_ENABLED="${WORKSHOP_SESSION_RUNNER_ENABLED:-true}"' \
      'export WORKSHOP_SESSION_RUNNER_AUTO_START="${WORKSHOP_SESSION_RUNNER_AUTO_START:-true}"' \
      'export WORKSHOP_SESSION_RUNNER_STARTUP_TIMEOUT="${WORKSHOP_SESSION_RUNNER_STARTUP_TIMEOUT:-90s}"' \
      'export WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT="${WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT:-420s}"' \
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
      'export WORKSHOP_SESSION_WORKSPACE_PATH="${WORKSHOP_SESSION_WORKSPACE_PATH:-${WORKSHOP_SESSION_ROOT_PATH}/java-springboot/${WORKSHOP_SOURCE_DIR}}"' \
      'export WORKSHOP_LOCAL_CODE_EDITOR_PORT="${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"' \
      'export WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH="${WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH:-/healthz}"' \
      'export WORKSHOP_LOCAL_CODE_EDITOR_COMMAND="${WORKSHOP_LOCAL_CODE_EDITOR_COMMAND:-/opt/runner/bin/start-code-editor.sh}"' \
      'export WORKSHOP_CHILD_WORKING_DIRECTORY="${WORKSHOP_CHILD_WORKING_DIRECTORY:-${WORKSHOP_SESSION_ROOT_PATH}/java-springboot}"' \
      'export WORKSHOP_CHILD_REBUILD_COMMAND="${WORKSHOP_CHILD_REBUILD_COMMAND:-/opt/runner/bin/rebuild-child.sh}"' \
      'export WORKSHOP_CHILD_COMMAND="${WORKSHOP_CHILD_COMMAND:-/opt/runner/bin/start-child.sh}"' \
      'export WORKSHOP_CHILD_HEALTH_PATH="${WORKSHOP_CHILD_HEALTH_PATH:-/login}"' \
      'if [ "${WORKSHOP_GRADLE_WARMUP_ENABLED:-true}" = "true" ]; then' \
      '  (sleep "${WORKSHOP_GRADLE_WARMUP_DELAY_SECONDS:-10}"; cd "${WORKSHOP_CHILD_WORKING_DIRECTORY}" && /opt/runner/bin/warm-gradle.sh) &' \
      'fi' \
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
ENV WORKSHOP_SOURCE_DIR="${WORKSHOP_SOURCE_DIR}"
ENV MANAGER_SOURCE_DIR="${MANAGER_SOURCE_DIR}"
ENV PORT=8080
ENV WORKSHOP_CHILD_PORT=18080
ENV WORKSHOP_REBUILT_CHILD_JAR=/opt/runner/workspace/.runner/child.jar
ENV WORKSHOP_SESSION_RUNNER_ENABLED=true
ENV WORKSHOP_SESSION_RUNNER_AUTO_START=true
ENV WORKSHOP_SESSION_RUNNER_REBUILD_TIMEOUT=420s
ENV WORKSHOP_REDIS_MODE=local-process
ENV WORKSHOP_BASELINE_PATH=/opt/runner/baseline
ENV WORKSHOP_SESSION_ROOT_PATH=/opt/runner/workspace
ENV WORKSHOP_LOCAL_CODE_EDITOR_COMMAND=/opt/runner/bin/start-code-editor.sh
ENV WORKSHOP_LOCAL_CODE_EDITOR_PORT=39000
ENV WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH=/healthz
ENV WORKSHOP_GRADLE_WARMUP_ENABLED=true
ENV WORKSHOP_GRADLE_WARMUP_DELAY_SECONDS=10
ENV GRADLE_USER_HOME=/opt/runner/gradle-cache
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl -fsS "http://127.0.0.1:${PORT}/health" || exit 1
ENTRYPOINT ["/opt/runner/bin/entrypoint.sh"]
