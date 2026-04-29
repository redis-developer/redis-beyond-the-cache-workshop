#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 3 ]; then
  echo "Usage: $0 <id> <title> <serviceName>" >&2
  echo "Example: $0 5_rate_limiting \"Rate Limiting\" rate-limiting" >&2
  exit 1
fi

ID="$1"
TITLE="$2"
SERVICE_NAME="$3"
DEFAULT_FRONTEND_PORT=8080
DEFAULT_BACKEND_PORT=18080

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
mkdir -p "$FRONTEND_MODULE_DIR/src/main/resources/workshop-content/views"
mkdir -p "$FRONTEND_MODULE_DIR/src/main/resources/workshop-manifest-reset"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/public"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/router"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/utils"
mkdir -p "$FRONTEND_MODULE_DIR/frontend/src/views"
mkdir -p "$FRONTEND_MODULE_DIR/src/test/java/com/redis/workshop/$PACKAGE_NAME/frontend"

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
server.port=\${SERVER_PORT:$DEFAULT_BACKEND_PORT}
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

cat <<EOF > "$BACKEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/${PASCAL_CASE}LearnerAppController.java"
package com.redis.workshop.$PACKAGE_NAME;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ${PASCAL_CASE}LearnerAppController {

    @GetMapping(value = "/api/learner-app", produces = MediaType.TEXT_HTML_VALUE)
    public String learnerApp() {
        return """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>$TITLE Learner App</title>
                <style>
                  body {
                    margin: 0;
                    min-height: 100vh;
                    display: grid;
                    place-items: center;
                    font-family: Arial, sans-serif;
                    background: #f8fafc;
                    color: #0f172a;
                  }
                  main {
                    max-width: 42rem;
                    padding: 2rem;
                  }
                  code {
                    background: #e2e8f0;
                    border-radius: 0.25rem;
                    padding: 0.15rem 0.35rem;
                  }
                </style>
              </head>
              <body>
                <main>
                  <h1>$TITLE Learner App</h1>
                  <p>This is the learner application surface generated by the scaffold.</p>
                  <p>Replace this endpoint with the actual app UI for your workshop.</p>
                  <p>Source: <code>${PASCAL_CASE}LearnerAppController.java</code></p>
                </main>
              </body>
            </html>
            """;
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
server.port=\${SERVER_PORT:$DEFAULT_FRONTEND_PORT}
workshop.backend.url=\${WORKSHOP_BACKEND_URL:http://127.0.0.1:$DEFAULT_BACKEND_PORT}
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

    @GetMapping({"/", "/0", "/1"})
    public String app() {
        return "forward:/index.html";
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/frontend/infrastructure/${PASCAL_CASE}WorkshopManifestConfiguration.java"
package com.redis.workshop.$PACKAGE_NAME.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ${PASCAL_CASE}WorkshopManifestConfiguration {

    @Bean
    @ConditionalOnMissingBean(WorkshopConfig.class)
    WorkshopConfig workshopConfig(WorkshopManifestLoader workshopManifestLoader) {
        return workshopManifestLoader.loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected workshop-manifest.yaml to be available"));
    }
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-manifest.yaml"
moduleName: $ID
title: $TITLE
description: >-
  TODO: Describe the workshop outcomes.
editableFiles:
  - name: build.gradle.kts
    path: build.gradle.kts
    resetContentLocation: workshop-manifest-reset/build.gradle.kts
  - name: application.properties
    path: src/main/resources/application.properties
    resetContentLocation: workshop-manifest-reset/application.properties
  - name: ${PASCAL_CASE}Application.java
    path: src/main/java/com/redis/workshop/$PACKAGE_NAME/${PASCAL_CASE}Application.java
    resetContentLocation: workshop-manifest-reset/${PASCAL_CASE}Application.java
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-manifest-reset/build.gradle.kts"
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

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-manifest-reset/application.properties"
spring.application.name=$SERVICE_NAME
server.port=\${SERVER_PORT:$DEFAULT_BACKEND_PORT}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-manifest-reset/${PASCAL_CASE}Application.java"
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

cat <<EOF > "$FRONTEND_MODULE_DIR/src/test/java/com/redis/workshop/$PACKAGE_NAME/frontend/${PASCAL_CASE}FrontendIntegrationTest.java"
package com.redis.workshop.$PACKAGE_NAME.frontend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
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

    @Test
    void contentApiExposesWorkshopOwnedViews() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("$ID"))
            .andExpect(jsonPath("$.views", hasSize(2)));

        mockMvc.perform(get("/api/content/views/0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("0"))
            .andExpect(jsonPath("$.pageType").value("narrative"));

        mockMvc.perform(get("/api/content/views/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("1"))
            .andExpect(jsonPath("$.pageType").value("editor"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/0", "/1"})
    void spaRoutesResolveToFrontend(String route) throws Exception {
        mockMvc.perform(get(route))
            .andExpect(status().isOk());
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
const backendTarget = process.env.WORKSHOP_BACKEND_URL

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: process.env.FRONTEND_PORT ? Number(process.env.FRONTEND_PORT) : undefined,
    proxy: backendTarget ? {
      '/api': {
        target: backendTarget,
        changeOrigin: true
      }
    } : undefined
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

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/static/index.html"
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
import { getBasePath } from '../../../../workshop-frontend-shared/src/utils/basePath.js'

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
import { getBasePath } from '../../../../../workshop-frontend-shared/src/utils/basePath.js'

const HomeView = () => import('../views/${PASCAL_CASE}Home.vue')
const EditorView = () => import('../views/${PASCAL_CASE}Editor.vue')

const routes = [
  { path: '/', redirect: '/0' },
  { path: '/0', name: '${PASCAL_CASE}Home', component: HomeView },
  { path: '/1', name: '${PASCAL_CASE}Editor', component: EditorView }
]

export default createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})
EOF

cat <<'EOF' > "$FRONTEND_MODULE_DIR/frontend/src/utils/workshopContent.js"
import { getApiUrl } from '../../../../../workshop-frontend-shared/src/utils/basePath.js'

export async function fetchWorkshopContent(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${encodeURIComponent(viewId)}`), {
    credentials: 'include'
  })

  if (!response.ok) {
    let message = `Failed to load workshop content (${response.status})`

    try {
      const errorBody = await response.json()
      if (errorBody?.message) {
        message = errorBody.message
      }
    } catch (error) {
      // Ignore parse failures and use the default message.
    }

    throw new Error(message)
  }

  return response.json()
}
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/src/views/${PASCAL_CASE}Home.vue"
<template>
  <div class="workshop-home">
    <WorkshopShell
      v-if="content"
      title="$TITLE"
      eyebrow="Workshop"
      summary="Use the shared shell for instructions, runtime controls, Redis Insight, and the learner app frame."
      :content="content"
      :context="contentContext"
      :links="shellLinks"
      :learner-app="learnerApp"
      :runtime="runtime"
      :actions="shellActions"
      :action-handlers="actionHandlers"
      :show-title="true"
      :show-summary="true"
      :show-stage-title="false"
      @runtime-action="handleRuntimeAction"
    />

    <main v-else class="main-container">
      <div class="content-status" :class="{ 'content-status--error': loadError }">
        {{ loadError || 'Loading workshop instructions...' }}
      </div>
    </main>
  </div>
</template>

<script>
import {
  getApiUrl,
  getRedisInsightUrl,
  getWorkshopHubUrl,
  WorkshopShell
} from '../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

export default {
  name: '${PASCAL_CASE}Home',
  components: {
    WorkshopShell
  },
  data() {
    return {
      content: null,
      loading: true,
      loadError: ''
    }
  },
  async mounted() {
    await this.loadContent()
  },
  computed: {
    contentContext() {
      return {
        session: {
          id: 'scaffold-session'
        },
        links: this.shellLinks,
        runtime: {
          state: this.runtime.state,
          status: this.runtime.status
        }
      }
    },
    learnerApp() {
      return {
        title: '$TITLE learner app',
        url: this.learnerAppUrl
      }
    },
    learnerAppUrl() {
      return getApiUrl('/api/learner-app')
    },
    redisInsightUrl() {
      return getRedisInsightUrl()
    },
    runtime() {
      return {
        state: 'READY',
        frontendState: 'READY',
        backendState: 'READY',
        status: 'ready',
        rebuildAvailable: true
      }
    },
    shellActions() {
      return ['restartRuntime', 'rebuildRuntime', 'openRedisInsight', 'openHub']
    },
    shellLinks() {
      return {
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightUrl,
        workshopHub: this.workshopHubUrl
      }
    },
    workshopHubUrl() {
      return getWorkshopHubUrl()
    },
    actionHandlers() {
      return {
        openEditor: () => this.openRoute('/1'),
        openApp: () => window.open(this.learnerAppUrl, '_blank', 'noopener'),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => this.openRoute(args?.route)
      }
    }
  },
  methods: {
    async loadContent() {
      this.loading = true
      this.loadError = ''

      try {
        this.content = await fetchWorkshopContent('0')
      } catch (error) {
        this.loadError = error.message || 'Failed to load workshop instructions.'
      } finally {
        this.loading = false
      }
    },
    openRoute(route) {
      if (!route) {
        return
      }

      this.\$router.push(route).catch(() => {})
    },
    handleRuntimeAction(action) {
      if (action.type === 'redisInsight') {
        window.open(this.redisInsightUrl, '_blank', 'noopener')
        return
      }

      if (action.type === 'restart' || action.type === 'rebuild') {
        window.open(this.workshopHubUrl, '_blank', 'noopener')
      }
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
  max-width: 1120px;
  margin: 0 auto;
  padding-top: var(--spacing-4);
}

.content-status {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.content-status--error {
  background: rgba(239, 68, 68, 0.18);
}
</style>
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/frontend/src/views/${PASCAL_CASE}Editor.vue"
<template>
  <WorkshopEditorLayout
    ref="layout"
    title="$TITLE Editor"
    :files="files"
    show-session-restart-controls
  >
    <template #instructions>
      <div v-if="loading" class="content-status">
        Loading workshop instructions...
      </div>

      <div v-else-if="loadError" class="content-status content-status--error">
        {{ loadError }}
      </div>

      <WorkshopContentRenderer
        v-else-if="content"
        :content="content"
        :show-title="false"
        :show-summary="true"
        :show-stage-title="false"
        :action-handlers="actionHandlers"
      />
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import {
  getWorkshopHubUrl,
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

export default {
  name: '${PASCAL_CASE}Editor',
  components: {
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      files: ['build.gradle.kts', 'application.properties', '${PASCAL_CASE}Application.java'],
      content: null,
      loading: true,
      loadError: ''
    }
  },
  async mounted() {
    await this.loadContent()
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl()
    },
    actionHandlers() {
      return {
        openFile: ({ args }) => this.loadFile(args?.file),
        saveFile: () => this.saveFile(),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => this.openRoute(args?.route)
      }
    }
  },
  methods: {
    async loadContent() {
      this.loading = true
      this.loadError = ''

      try {
        this.content = await fetchWorkshopContent('1')
      } catch (error) {
        this.loadError = error.message || 'Failed to load workshop instructions.'
      } finally {
        this.loading = false
      }
    },
    loadFile(fileName) {
      if (!fileName) {
        return
      }

      return this.\$refs.layout?.loadFile(fileName)
    },
    saveFile() {
      return this.\$refs.layout?.save()
    },
    openRoute(route) {
      if (!route) {
        return
      }

      this.\$router.push(route).catch(() => {})
    }
  }
}
</script>

<style scoped>
.content-status {
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  background: rgba(59, 130, 246, 0.12);
  color: var(--color-text);
}

.content-status--error {
  background: rgba(239, 68, 68, 0.18);
}
</style>
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-content/manifest.yaml"
schemaVersion: 1
workshopId: $ID
views:
  - viewId: "0"
    route: /0
    pageType: narrative
    file: views/0.yaml
  - viewId: "1"
    route: /1
    pageType: editor
    file: views/1.yaml
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-content/views/0.yaml"
schemaVersion: 1
viewId: "0"
route: /0
pageType: narrative
title: $TITLE
slot: instructions
summary: Author workshop instructions in these YAML files so the Vue views stay focused on state, API calls, and action handlers.
header:
  showHubLink: true
sections:
  - sectionId: content-driven-overview
    title: Content-Driven Workshop Authoring
    blocks:
      - type: markdown
        body: |-
          ## Edit content here

          Update learner-facing copy in \`src/main/resources/workshop-content/views\` instead of writing large instruction templates in Vue.

          Dynamic placeholders are available in markdown. For example, the current session is \`{{sessionId}}\`, Redis Insight is [available here]({{links.redisInsight}}), and the learner app can be linked with [open app]({{links.learnerApp}}).
      - type: stepList
        listId: scaffold-overview
        variant: ordered
        items:
          - itemId: review-home-content
            title: Review the generated content files
            body: Start with 0.yaml and 1.yaml, then add more numbered view files as the workshop grows.
          - itemId: keep-vue-thin
            title: Keep the Vue route thin
            body: The generated home and editor views fetch content from the shared content API and delegate rendering to the shared renderer.
          - itemId: open-editor
            title: Continue in the editor
            body: Use the editor route to guide learners through the backend files that still live in the workshop manifest.
            actions:
              - id: openEditor
                label: Open Editor
          - itemId: open-learner-app
            title: Preview the learner app
            body: The scaffold includes a visible iframe route that targets the backend through the workshop proxy.
            actions:
              - id: openApp
                label: Open Learner App
      - type: callout
        tone: success
        title: After you customize the workshop
        body: Replace the placeholder copy in this file, update the backend TODOs, then validate the scaffold and start the workshop through scripts/run-workshop.sh.
        actions:
          - id: openHub
            label: Open Workshop Hub
EOF

cat <<EOF > "$FRONTEND_MODULE_DIR/src/main/resources/workshop-content/views/1.yaml"
schemaVersion: 1
viewId: "1"
route: /1
pageType: editor
title: $TITLE
slot: instructions
summary: Keep guided editing instructions in YAML while the editor view manages file loading and save behavior in code.
sections:
  - sectionId: editor-overview
    blocks:
      - type: callout
        tone: info
        body: The scaffolded editor view loads this document through the shared content path and keeps only file operations and routing behavior in Vue.
  - sectionId: starter-workflow
    title: Suggested first edits
    blocks:
      - type: editorStepList
        listId: backend-build
        title: "Step 1: Review dependencies"
        startAt: 1
        items:
          - itemId: open-build-file
            body: Open build.gradle.kts and replace the scaffold TODOs with the dependencies your workshop needs.
            action:
              id: openFile
              label: Open build.gradle.kts
              args:
                file: build.gradle.kts
          - itemId: save-build-file
            body: Save the file after updating the dependency list.
            action:
              id: saveFile
              label: Save Changes
      - type: editorStepList
        listId: backend-config
        title: "Step 2: Configure application properties"
        startAt: 3
        items:
          - itemId: open-application-properties
            body: Open application.properties and update runtime settings for the workshop scenario you are building.
            action:
              id: openFile
              label: Open application.properties
              args:
                file: application.properties
          - itemId: save-application-properties
            body: Save the configuration updates.
            action:
              id: saveFile
              label: Save Changes
      - type: editorStepList
        listId: backend-application
        title: "Step 3: Implement the backend entry point"
        startAt: 5
        items:
          - itemId: open-application-class
            body: Open ${PASCAL_CASE}Application.java and replace the scaffold with the workshop-specific application setup.
            action:
              id: openFile
              label: Open ${PASCAL_CASE}Application.java
              args:
                file: ${PASCAL_CASE}Application.java
          - itemId: save-application-class
            body: Save the file after the initial implementation is in place.
            action:
              id: saveFile
              label: Save Changes
      - type: callout
        tone: success
        title: Extend the editor workflow here
        body: Add or refine editor steps in this YAML file instead of expanding the Vue template with instructional markup.
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
    url: /workshop/$SERVICE_NAME/
    dockerfile: java-springboot/$FRONTEND_MODULE/Dockerfile
    frontendServiceName: $SERVICE_NAME
    frontendDockerfile: java-springboot/$FRONTEND_MODULE/Dockerfile
    backendServiceName: $BACKEND_SERVICE_NAME
    backendDockerfile: java-springboot/$ID/Dockerfile
    infrastructureDependencies:
      - redis
    redisFlavor: standard
    frontendPrebuild: true
    topics:
      - TODO
    releases:
      - releaseId: ${SERVICE_NAME}-0.1.0
        releaseVersion: 0.1.0
        mode: LAB
        defaultForWorkshop: true
        enabled: true
        environments:
          - local
          - cloud-run
        images:
          combined: registry.example.com/workshops/${SERVICE_NAME}-runner@sha256:0000000000000000000000000000000000000000000000000000000000000000
        resourceClass: small
        sessionTtlMinutes: 60
        mutableDependencies:
          - redis
EOF

cat <<EOF
Created:
1. Backend module: java-springboot/$ID
2. Frontend composition module: java-springboot/$FRONTEND_MODULE
3. content manifest: java-springboot/$FRONTEND_MODULE/src/main/resources/workshop-content/manifest.yaml
4. content views:
   java-springboot/$FRONTEND_MODULE/src/main/resources/workshop-content/views/0.yaml
   java-springboot/$FRONTEND_MODULE/src/main/resources/workshop-content/views/1.yaml
5. Runtime shell/app split:
   The generated app imports shared shell APIs directly from workshop-frontend-shared.
   The only generated frontend utility is the app-owned workshopContent.js helper.
   Shared restart controls are explicitly enabled on the generated header and editor layout.
   The shared shell renders a learner app iframe pointed at the workshop backend proxy.

Registered:
1. workshops.yaml
2. java-springboot/settings.gradle.kts

Next steps:
1. Replace the placeholder content in the generated YAML view files and keep new instructional copy in workshop-content/.
2. Fill in the TODOs in the backend module and workshop manifest.
3. Run bash scripts/validate-workshops.sh to check the registry, modules, and content.
4. Run bash scripts/run-workshop.sh up $ID to start this workshop locally.
EOF
