#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 4 ] || [ $# -gt 5 ]; then
  echo "Usage: $0 <id> <title> <serviceName> <frontendPort> [backendPort]" >&2
  echo "Example: $0 5_rate_limiting \"Rate Limiting\" rate-limiting 8084 18084" >&2
  exit 1
fi

ID="$1"
TITLE="$2"
SERVICE_NAME="$3"
FRONTEND_PORT="$4"
BACKEND_PORT="${5:-$((FRONTEND_PORT + 10000))}"

PASCAL_CASE=$(echo "$SERVICE_NAME" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2))}1' OFS='')
PACKAGE_NAME=$(echo "$SERVICE_NAME" | tr -d '-')
FRONTEND_MODULE="${ID}_frontend"
BACKEND_SERVICE_NAME="${SERVICE_NAME}-api"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$ROOT_DIR/java-springboot"
BACKEND_MODULE_DIR="$JAVA_DIR/$ID"
FRONTEND_MODULE_DIR="$JAVA_DIR/$FRONTEND_MODULE"
REGISTRY_PATH="$ROOT_DIR/workshops.yaml"
SETTINGS_PATH="$JAVA_DIR/settings.gradle.kts"
LOGO_SOURCE="$JAVA_DIR/1_session_management_frontend/frontend/src/assets/logo/small.png"

for path in "$BACKEND_MODULE_DIR" "$FRONTEND_MODULE_DIR"; do
  if [ -e "$path" ]; then
    echo "Path already exists: $path" >&2
    exit 1
  fi
done

if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Registry not found: $REGISTRY_PATH" >&2
  exit 1
fi

if ! [[ "$FRONTEND_PORT" =~ ^[0-9]+$ && "$BACKEND_PORT" =~ ^[0-9]+$ ]]; then
  echo "Ports must be numeric." >&2
  exit 1
fi

if command -v rg >/dev/null 2>&1; then
  if rg -n "id: $ID" "$REGISTRY_PATH" >/dev/null; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
  if rg -n "serviceName: $SERVICE_NAME" "$REGISTRY_PATH" >/dev/null; then
    echo "Service name already exists in registry: $SERVICE_NAME" >&2
    exit 1
  fi
else
  if grep -q "id: $ID" "$REGISTRY_PATH"; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
  if grep -q "serviceName: $SERVICE_NAME" "$REGISTRY_PATH"; then
    echo "Service name already exists in registry: $SERVICE_NAME" >&2
    exit 1
  fi
fi

mkdir -p "$BACKEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME"
mkdir -p "$BACKEND_MODULE_DIR/src/main/resources"
mkdir -p "$FRONTEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/frontend/infrastructure"
mkdir -p "$FRONTEND_MODULE_DIR/src/main/resources/static"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/public"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/assets/logo"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/router"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/utils"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/views"
mkdir -p "$FRONTEND_MODULE_DIR/src/test/java/com/redis/workshop/$PACKAGE_NAME/frontend"

if [ -f "$LOGO_SOURCE" ]; then
  cp "$LOGO_SOURCE" "$FRONTEND_MODULE_DIR/frontend/src/assets/logo/"
fi

cat <<EOF > "$BACKEND_MODULE_DIR/README.md"
# $TITLE

TODO: Describe the workshop goals, prerequisites, and learner steps.
EOF

cat <<EOF > "$BACKEND_MODULE_DIR/build.gradle.kts"
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.redis.workshop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
EOF

cat <<EOF > "$BACKEND_MODULE_DIR/settings.gradle.kts"
rootProject.name = "redis-springboot-workshop"

include("$ID")
EOF

cat <<EOF > "$BACKEND_MODULE_DIR/src/main/resources/application.properties"
spring.application.name=$SERVICE_NAME
server.port=\${SERVER_PORT:$BACKEND_PORT}
EOF

cat <<EOF > "$BACKEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/${PASCAL_CASE}Application.java"
package com.redis.workshop.$PACKAGE_NAME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${PASCAL_CASE}Application {

    public static void main(String[] args) {
        SpringApplication.run(${PASCAL_CASE}Application.class, args);
    }
}
EOF

cat <<EOF > "$BACKEND_MODULE_DIR/Dockerfile"
# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/$ID/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/$ID/build.gradle.kts $ID/

RUN --mount=type=cache,target=/root/.gradle \\
    ./gradlew :$ID:dependencies --no-daemon || true

COPY java-springboot/$ID/src $ID/src

RUN --mount=type=cache,target=/root/.gradle \\
    ./gradlew :$ID:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/$ID/build/libs/${ID}-0.0.1-SNAPSHOT.jar app.jar

EXPOSE $BACKEND_PORT

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/build.gradle.kts"
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.redis.workshop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":workshop-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/settings.gradle.kts"
rootProject.name = "redis-springboot-workshop"

include("workshop-infrastructure")
include("$FRONTEND_MODULE")
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/application.properties"
spring.application.name=${SERVICE_NAME}-frontend
server.port=\${SERVER_PORT:$FRONTEND_PORT}
workshop.backend.url=\${WORKSHOP_BACKEND_URL:http://127.0.0.1:$BACKEND_PORT}
workshop.source.path=\${WORKSHOP_SOURCE_PATH:\${WORKSHOP_BASE_PATH:}}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/frontend/${PASCAL_CASE}FrontendApplication.java"
package com.redis.workshop.$PACKAGE_NAME.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.$PACKAGE_NAME.frontend",
    "com.redis.workshop.infrastructure"
})
public class ${PASCAL_CASE}FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(${PASCAL_CASE}FrontendApplication.class, args);
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/frontend/infrastructure/${PASCAL_CASE}SpaController.java"
package com.redis.workshop.$PACKAGE_NAME.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ${PASCAL_CASE}SpaController {

    @GetMapping({"/", "/editor"})
    public String app() {
        return "forward:/index.html";
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/frontend/infrastructure/${PASCAL_CASE}WorkshopConfig.java"
package com.redis.workshop.$PACKAGE_NAME.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ${PASCAL_CASE}WorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.ofEntries(
        Map.entry("build.gradle.kts", "build.gradle.kts"),
        Map.entry("application.properties", "src/main/resources/application.properties"),
        Map.entry("${PASCAL_CASE}Application.java", "src/main/java/com/redis/workshop/$PACKAGE_NAME/${PASCAL_CASE}Application.java")
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.ofEntries(
        Map.entry("build.gradle.kts", """
            plugins {
                java
                id("org.springframework.boot")
                id("io.spring.dependency-management")
            }

            group = "com.redis.workshop"
            version = "0.0.1-SNAPSHOT"

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """),
        Map.entry("application.properties", """
            spring.application.name=$SERVICE_NAME
            server.port=\${SERVER_PORT:$BACKEND_PORT}
            """),
        Map.entry("${PASCAL_CASE}Application.java", """
            package com.redis.workshop.$PACKAGE_NAME;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ${PASCAL_CASE}Application {

                public static void main(String[] args) {
                    SpringApplication.run(${PASCAL_CASE}Application.class, args);
                }
            }
            """)
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "$ID";
    }

    @Override
    public String getWorkshopTitle() {
        return "$TITLE";
    }

    @Override
    public String getWorkshopDescription() {
        return "TODO: Describe the workshop outcomes.";
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/test/java/com/redis/workshop/$PACKAGE_NAME/frontend/${PASCAL_CASE}FrontendIntegrationTest.java"
package com.redis.workshop.$PACKAGE_NAME.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = ${PASCAL_CASE}FrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class ${PASCAL_CASE}FrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void editorApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasKey("files")))
            .andExpect(jsonPath("$", hasKey("workshopTitle")));
    }

    @Test
    void backendApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isBadGateway());
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/Dockerfile"
# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

ARG SKIP_FRONTEND_BUILD=false

RUN if [ "\$SKIP_FRONTEND_BUILD" != "true" ]; then \\
        apt-get update \\
        && apt-get install -y nodejs npm \\
        && rm -rf /var/lib/apt/lists/*; \\
    fi

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/$FRONTEND_MODULE/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/$FRONTEND_MODULE/build.gradle.kts $FRONTEND_MODULE/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \\
    ./gradlew :$FRONTEND_MODULE:dependencies --no-daemon || true

COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

COPY java-springboot/$FRONTEND_MODULE/frontend/package*.json $FRONTEND_MODULE/frontend/
COPY java-springboot/$FRONTEND_MODULE/frontend/babel.config.js $FRONTEND_MODULE/frontend/

RUN --mount=type=cache,target=/root/.npm \\
    if [ "\$SKIP_FRONTEND_BUILD" != "true" ]; then \\
        cd $FRONTEND_MODULE/frontend && npm install; \\
    fi

COPY java-springboot/$FRONTEND_MODULE/src $FRONTEND_MODULE/src
COPY java-springboot/$FRONTEND_MODULE/frontend/src $FRONTEND_MODULE/frontend/src
COPY java-springboot/$FRONTEND_MODULE/frontend/public $FRONTEND_MODULE/frontend/public
COPY java-springboot/$FRONTEND_MODULE/frontend/vue.config.js $FRONTEND_MODULE/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

RUN if [ "\$SKIP_FRONTEND_BUILD" = "true" ]; then \\
        test -f $FRONTEND_MODULE/src/main/resources/static/index.html; \\
    fi

ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=\${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \\
    if [ "\$SKIP_FRONTEND_BUILD" != "true" ]; then \\
        cd $FRONTEND_MODULE/frontend && npm run build; \\
    fi

RUN --mount=type=cache,target=/root/.gradle \\
    ./gradlew :$FRONTEND_MODULE:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/$FRONTEND_MODULE/build/libs/${FRONTEND_MODULE}-0.0.1-SNAPSHOT.jar app.jar

ENV WORKSHOP_SOURCE_PATH=/workshop-sources
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE $FRONTEND_PORT

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/package.json"
{
  "name": "$SERVICE_NAME-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build"
  },
  "dependencies": {
    "@redis-workshop/shared": "file:../../../../workshop-frontend-shared",
    "core-js": "^3.8.3",
    "vue": "^3.2.13",
    "vue-router": "^4.0.3"
  },
  "devDependencies": {
    "@babel/core": "^7.12.16",
    "@vue/cli-plugin-babel": "~5.0.0",
    "@vue/cli-plugin-router": "~5.0.0",
    "@vue/cli-service": "~5.0.0"
  }
}
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/babel.config.js"
module.exports = {
  presets: ['@vue/cli-plugin-babel/preset']
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/vue.config.js"
const { defineConfig } = require('@vue/cli-service')

const basePath = process.env.VUE_APP_BASE_PATH || '/'

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: $FRONTEND_PORT,
    proxy: {
      '/api': {
        target: 'http://localhost:$BACKEND_PORT',
        changeOrigin: true
      }
    }
  }
})
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/public/index.html"
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <title>$TITLE</title>
  </head>
  <body>
    <div id="app"></div>
  </body>
</html>
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/src/main.js"
import { getBasePath } from './utils/basePath'

// eslint-disable-next-line no-undef
__webpack_public_path__ = `${getBasePath()}/`.replace(/\/+$/, '/')

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import '../../../../workshop-frontend-shared/src/styles/tokens.css'
import '../../../../workshop-frontend-shared/src/styles/dark-theme.css'
import '../../../../workshop-frontend-shared/src/styles/components.css'

createApp(App).use(router).mount('#app')
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/src/App.vue"
<template>
  <router-view />
</template>
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/src/router/index.js"
import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const HomeView = () => import('../views/${PASCAL_CASE}Home.vue')
const EditorView = () => import('../views/${PASCAL_CASE}Editor.vue')

const routes = [
  { path: '/', name: '${PASCAL_CASE}Home', component: HomeView },
  { path: '/editor', name: '${PASCAL_CASE}Editor', component: EditorView }
]

export default createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/src/utils/basePath.js"
export { getBasePath, getApiUrl } from '../../../../../workshop-frontend-shared/src/utils/basePath.js'
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/src/utils/components.js"
export { default as WorkshopHeader } from '../../../../../workshop-frontend-shared/src/components/WorkshopHeader.vue'
export { default as WorkshopModal } from '../../../../../workshop-frontend-shared/src/components/WorkshopModal.vue'
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/src/views/${PASCAL_CASE}Home.vue"
<template>
  <div class="workshop-home">
    <WorkshopHeader :hub-url="workshopHubUrl" />

    <div class="main-container">
      <h1>$TITLE</h1>
      <p>TODO: Add workshop guidance, demos, and success criteria.</p>
      <router-link to="/editor" class="btn btn-primary">Open Editor</router-link>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader } from '../utils/components'

export default {
  name: '${PASCAL_CASE}Home',
  components: { WorkshopHeader },
  computed: {
    workshopHubUrl() {
      return window.location.protocol + '//' + window.location.hostname + ':9000'
    }
  }
}
</script>

<style scoped>
.workshop-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.main-container {
  max-width: 960px;
  margin: 0 auto;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
}
</style>
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/src/views/${PASCAL_CASE}Editor.vue"
<template>
  <WorkshopEditorLayout title="$TITLE Editor" :files="files" />
</template>

<script>
import { WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/index.js'

export default {
  name: '${PASCAL_CASE}Editor',
  components: { WorkshopEditorLayout },
  data() {
    return {
      files: ['build.gradle.kts', 'application.properties', '${PASCAL_CASE}Application.java']
    }
  }
}
</script>
EOF

append_if_missing() {
  local line="$1"
  local file="$2"
  if ! grep -Fqx "$line" "$file"; then
    printf '%s\n' "$line" >> "$file"
  fi
}

append_if_missing "include(\"$ID\")" "$SETTINGS_PATH"
append_if_missing "include(\"$FRONTEND_MODULE\")" "$SETTINGS_PATH"

cat <<EOF >> "$REGISTRY_PATH"
  - id: $ID
    title: $TITLE
    description: >-
      TODO: Describe the workshop goals and learner outcomes.
    difficulty: Beginner
    estimatedMinutes: 30
    serviceName: $SERVICE_NAME
    port: $FRONTEND_PORT
    url: /workshop/$SERVICE_NAME/
    dockerfile: java-springboot/$ID/Dockerfile
    frontendServiceName: $SERVICE_NAME
    frontendPort: $FRONTEND_PORT
    frontendDockerfile: java-springboot/$FRONTEND_MODULE/Dockerfile
    backendServiceName: $BACKEND_SERVICE_NAME
    backendPort: $BACKEND_PORT
    backendDockerfile: java-springboot/$ID/Dockerfile
    topics:
      - TODO
EOF

cat <<EOF
Created:
- backend module: java-springboot/$ID
- frontend module: java-springboot/$FRONTEND_MODULE

Registered:
- workshops.yaml
- java-springboot/settings.gradle.kts

Next steps:
1. Fill in the TODOs in both generated modules.
2. Run ./java-springboot/gradlew -p java-springboot :workshop-hub:generateCompose
3. If the hub DinD image should ship this workshop with prebuilt frontend assets, add $FRONTEND_MODULE to java-springboot/workshop-hub/Dockerfile.
EOF
