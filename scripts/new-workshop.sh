#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 4 ]; then
  echo "Usage: $0 <id> <title> <serviceName> <port>" >&2
  echo "Example: $0 4_rate_limiting \"Rate Limiting\" rate-limiting 8083" >&2
  exit 1
fi

ID="$1"
TITLE="$2"
SERVICE_NAME="$3"
PORT="$4"

# Convert service-name to PascalCase for component names (e.g., rate-limiting -> RateLimiting)
# Using awk for portability across macOS and Linux
PASCAL_CASE=$(echo "$SERVICE_NAME" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2))}1' OFS='')

# Convert to package-friendly name (lowercase, no dashes)
PACKAGE_NAME=$(echo "$SERVICE_NAME" | tr -d '-')

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REGISTRY_PATH="$ROOT_DIR/workshops.yaml"
MODULE_DIR="$ROOT_DIR/java-springboot/$ID"
FRONTEND_DIR="$MODULE_DIR/frontend"

if [ -e "$MODULE_DIR" ]; then
  echo "Module directory already exists: $MODULE_DIR" >&2
  exit 1
fi

if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Registry not found: $REGISTRY_PATH" >&2
  exit 1
fi

if command -v rg >/dev/null 2>&1; then
  if rg -n "id: $ID" "$REGISTRY_PATH" >/dev/null; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
else
  if grep -q "id: $ID" "$REGISTRY_PATH"; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
fi

# Create backend structure
mkdir -p "$MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME"
mkdir -p "$MODULE_DIR/src/main/resources"

# Create frontend structure
mkdir -p "$FRONTEND_DIR/public"
mkdir -p "$FRONTEND_DIR/src/assets/logo"
mkdir -p "$FRONTEND_DIR/src/router"
mkdir -p "$FRONTEND_DIR/src/utils"
mkdir -p "$FRONTEND_DIR/src/views"

# Copy logo from existing workshop
if [ -f "$ROOT_DIR/java-springboot/1_session_management/frontend/src/assets/logo/small.png" ]; then
  cp "$ROOT_DIR/java-springboot/1_session_management/frontend/src/assets/logo/small.png" "$FRONTEND_DIR/src/assets/logo/"
fi

cat <<'DOC' > "$MODULE_DIR/README.md"
# Workshop

TODO: Describe the workshop goals, prerequisites, and steps.
DOC

cat <<DOC > "$MODULE_DIR/Dockerfile"
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM gradle:8.5-jdk21 AS backend-build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY --from=frontend-build /frontend/../src/main/resources/static ./src/main/resources/static
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
DOC

# Create build.gradle.kts
cat <<DOC > "$MODULE_DIR/build.gradle.kts"
plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // TODO: Add Redis dependencies as needed
    // implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
DOC

# Create settings.gradle.kts for this module
cat <<DOC > "$MODULE_DIR/settings.gradle.kts"
rootProject.name = "$ID"
DOC

# Create application.properties
cat <<DOC > "$MODULE_DIR/src/main/resources/application.properties"
server.port=$PORT
spring.application.name=$SERVICE_NAME

# Redis configuration (uncomment to enable)
# spring.data.redis.host=redis
# spring.data.redis.port=6379
DOC

# Create main application class
cat <<DOC > "$MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/${PASCAL_CASE}Application.java"
package com.redis.workshop.$PACKAGE_NAME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${PASCAL_CASE}Application {
    public static void main(String[] args) {
        SpringApplication.run(${PASCAL_CASE}Application.class, args);
    }
}
DOC

# Create EditorController
cat <<DOC > "$MODULE_DIR/src/main/java/com/redis/workshop/$PACKAGE_NAME/EditorController.java"
package com.redis.workshop.$PACKAGE_NAME;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/editor")
public class EditorController {

    private static final String SOURCE_ROOT = System.getenv().getOrDefault("WORKSHOP_SOURCE_ROOT", "src/main/");

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Map<String, String>> getFile(@PathVariable String fileName) {
        try {
            String content = readFile(fileName);
            return ResponseEntity.ok(Map.of("content", content, "fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/file/{fileName}")
    public ResponseEntity<Map<String, String>> saveFile(@PathVariable String fileName, @RequestBody Map<String, String> body) {
        try {
            String content = body.get("content");
            writeFile(fileName, content);
            return ResponseEntity.ok(Map.of("message", "File saved successfully", "fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String readFile(String fileName) throws IOException {
        Path path = resolveFilePath(fileName);
        if (path != null && Files.exists(path)) {
            return Files.readString(path);
        }
        // Fall back to classpath resource
        ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeFile(String fileName, String content) throws IOException {
        Path path = resolveFilePath(fileName);
        if (path != null) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } else {
            throw new IOException("Cannot write to classpath resource: " + fileName);
        }
    }

    private Path resolveFilePath(String fileName) {
        // Map file names to their source paths
        if (fileName.endsWith(".java")) {
            return Paths.get(SOURCE_ROOT, "java/com/redis/workshop/$PACKAGE_NAME", fileName);
        } else if (fileName.equals("application.properties")) {
            return Paths.get(SOURCE_ROOT, "resources", fileName);
        } else if (fileName.endsWith(".kts")) {
            return Paths.get(fileName);
        }
        return null;
    }
}
DOC

# ========== FRONTEND FILES ==========

# package.json
cat <<DOC > "$FRONTEND_DIR/package.json"
{
  "name": "$SERVICE_NAME-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"
  },
  "dependencies": {
    "@redis-workshop/shared": "file:../../../../workshop-frontend-shared",
    "core-js": "^3.8.3",
    "highlight.js": "^11.11.1",
    "vue": "^3.2.13",
    "vue-router": "^4.0.3"
  },
  "devDependencies": {
    "@babel/core": "^7.12.16",
    "@babel/eslint-parser": "^7.12.16",
    "@vue/cli-plugin-babel": "~5.0.0",
    "@vue/cli-plugin-eslint": "~5.0.0",
    "@vue/cli-plugin-router": "~5.0.0",
    "@vue/cli-service": "~5.0.0",
    "eslint": "^7.32.0",
    "eslint-plugin-vue": "^8.0.3"
  },
  "eslintConfig": {
    "root": true,
    "env": { "node": true },
    "extends": ["plugin:vue/vue3-essential", "eslint:recommended"],
    "parserOptions": { "parser": "@babel/eslint-parser" },
    "rules": {}
  },
  "browserslist": ["> 1%", "last 2 versions", "not dead", "not ie 11"]
}
DOC

# babel.config.js
cat <<'DOC' > "$FRONTEND_DIR/babel.config.js"
module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset'
  ]
}
DOC

# vue.config.js
cat <<DOC > "$FRONTEND_DIR/vue.config.js"
const { defineConfig } = require('@vue/cli-service')

const basePath = process.env.VUE_APP_BASE_PATH || '/'

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:$PORT',
        changeOrigin: true
      }
    }
  }
})
DOC

# public/index.html
cat <<DOC > "$FRONTEND_DIR/public/index.html"
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <link rel="icon" href="<%= BASE_URL %>favicon.ico">
    <title>$TITLE Workshop</title>
  </head>
  <body>
    <noscript>
      <strong>We're sorry but this application doesn't work properly without JavaScript enabled. Please enable it to continue.</strong>
    </noscript>
    <div id="app"></div>
  </body>
</html>
DOC

# src/main.js
cat <<'DOC' > "$FRONTEND_DIR/src/main.js"
import { getBasePath } from './utils/basePath'

// Ensure dynamic chunks load from the correct base path when proxied by the Hub.
// eslint-disable-next-line no-undef
__webpack_public_path__ = `${getBasePath()}/`.replace(/\/+$/, '/')

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Import styles from shared package using relative path
import '../../../../workshop-frontend-shared/src/styles/tokens.css'
import '../../../../workshop-frontend-shared/src/styles/dark-theme.css'
import '../../../../workshop-frontend-shared/src/styles/components.css'

createApp(App).use(router).mount('#app')
DOC

# src/App.vue
cat <<'DOC' > "$FRONTEND_DIR/src/App.vue"
<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-family-base);
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: var(--line-height-normal);
}

#app {
  min-height: 100vh;
}
</style>
DOC

# src/utils/basePath.js
cat <<'DOC' > "$FRONTEND_DIR/src/utils/basePath.js"
/**
 * Re-export from shared package for backward compatibility.
 */
export { getBasePath, getApiUrl } from '../../../../../workshop-frontend-shared/src/utils/basePath.js';
DOC

# src/utils/components.js
cat <<'DOC' > "$FRONTEND_DIR/src/utils/components.js"
/**
 * Re-export shared components for simplified imports.
 * Uses relative path to avoid webpack module resolution issues with file: links.
 *
 * Usage:
 *   import { WorkshopModal, WorkshopHeader } from '../utils/components';
 */
export { default as WorkshopModal } from '../../../../../workshop-frontend-shared/src/components/WorkshopModal.vue';
export { default as WorkshopStageNav } from '../../../../../workshop-frontend-shared/src/components/WorkshopStageNav.vue';
export { default as WorkshopProgressIndicator } from '../../../../../workshop-frontend-shared/src/components/WorkshopProgressIndicator.vue';
export { default as WorkshopHubLink } from '../../../../../workshop-frontend-shared/src/components/WorkshopHubLink.vue';
export { default as WorkshopHeader } from '../../../../../workshop-frontend-shared/src/components/WorkshopHeader.vue';
DOC

# src/utils/progress.js
cat <<'DOC' > "$FRONTEND_DIR/src/utils/progress.js"
/**
 * Workshop progress tracking utility.
 * Stores progress in localStorage to persist across page reloads.
 */

const STORAGE_KEY_PREFIX = 'workshop_progress_';

export function getProgress(workshopId) {
  const key = STORAGE_KEY_PREFIX + workshopId;
  const stored = localStorage.getItem(key);
  return stored ? JSON.parse(stored) : { currentStage: 0, completedStages: [], testsCompleted: {} };
}

export function saveProgress(workshopId, progress) {
  const key = STORAGE_KEY_PREFIX + workshopId;
  localStorage.setItem(key, JSON.stringify(progress));
}

export function resetProgress(workshopId) {
  const key = STORAGE_KEY_PREFIX + workshopId;
  localStorage.removeItem(key);
}

export function completeStage(workshopId, stageIndex) {
  const progress = getProgress(workshopId);
  if (!progress.completedStages.includes(stageIndex)) {
    progress.completedStages.push(stageIndex);
  }
  if (stageIndex >= progress.currentStage) {
    progress.currentStage = stageIndex + 1;
  }
  saveProgress(workshopId, progress);
  return progress;
}

export function completeTest(workshopId, stageIndex, testId) {
  const progress = getProgress(workshopId);
  if (!progress.testsCompleted[stageIndex]) {
    progress.testsCompleted[stageIndex] = [];
  }
  if (!progress.testsCompleted[stageIndex].includes(testId)) {
    progress.testsCompleted[stageIndex].push(testId);
  }
  saveProgress(workshopId, progress);
  return progress;
}

export function isTestCompleted(workshopId, stageIndex, testId) {
  const progress = getProgress(workshopId);
  return progress.testsCompleted[stageIndex]?.includes(testId) || false;
}
DOC

# src/router/index.js
cat <<DOC > "$FRONTEND_DIR/src/router/index.js"
import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const ${PASCAL_CASE}Home = () => import('../views/${PASCAL_CASE}Home.vue')
const ${PASCAL_CASE}Editor = () => import('../views/${PASCAL_CASE}Editor.vue')

const routes = [
  {
    path: '/',
    name: '${PASCAL_CASE}Home',
    component: ${PASCAL_CASE}Home
  },
  {
    path: '/editor',
    name: '${PASCAL_CASE}Editor',
    component: ${PASCAL_CASE}Editor
  }
]

const router = createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})

export default router
DOC

# src/views/Home.vue (standardized structure with stages and progress tracking)
cat <<DOC > "$FRONTEND_DIR/src/views/${PASCAL_CASE}Home.vue"
<template>
  <div class="workshop-home">
    <!-- Workshop Header with Hub Link and Progress -->
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="stageNames"
      :current-step="currentStage"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <!-- STAGE 1: Introduction -->
      <div v-if="currentStage === 0" class="stage-content">
        <div class="instructions">
          <h2>STAGE 1: Introduction</h2>
          <p class="intro">
            TODO: Add workshop introduction and goals here.
          </p>

          <div class="step-item">
            <h4>What You'll Learn</h4>
            <p class="step-description">
              TODO: Describe what the user will learn in this workshop.
            </p>
            <ul class="step-list">
              <li>TODO: Learning objective 1</li>
              <li>TODO: Learning objective 2</li>
              <li>TODO: Learning objective 3</li>
            </ul>
          </div>

          <div class="button-group">
            <button @click="goToStage(1)" class="btn btn-primary">
              Start Learning →
            </button>
          </div>
        </div>
      </div>

      <!-- STAGE 2: Learn -->
      <div v-if="currentStage === 1" class="stage-content">
        <div class="instructions">
          <h2>STAGE 2: Learn the Concepts</h2>
          <p class="intro">
            TODO: Add learning content here.
          </p>

          <div class="step-item">
            <h4>Step 1: TODO</h4>
            <p class="step-description">
              TODO: Explain the first concept.
            </p>
          </div>

          <div class="step-item">
            <h4>Step 2: TODO</h4>
            <p class="step-description">
              TODO: Explain the second concept.
            </p>
          </div>

          <div class="button-group">
            <button @click="goToStage(0)" class="btn btn-secondary">
              ← Back
            </button>
            <button @click="goToStage(2)" class="btn btn-primary">
              Start Building →
            </button>
          </div>
        </div>
      </div>

      <!-- STAGE 3: Build -->
      <div v-if="currentStage === 2" class="stage-content">
        <div class="instructions">
          <h2>STAGE 3: Build It Yourself</h2>

          <div class="alert alert-info">
            <strong>Option 1: Use the In-Browser Code Editor</strong>
            <p style="margin: 0.5rem 0 0 0;">
              <router-link to="/editor" class="editor-link">Open Code Editor →</router-link>
            </p>
          </div>

          <div class="step-item">
            <h4>Option 2: Manual Implementation</h4>
            <p class="step-description">
              TODO: Add manual implementation steps here.
            </p>
          </div>

          <div class="button-group">
            <button @click="goToStage(1)" class="btn btn-secondary">
              ← Back
            </button>
            <button @click="goToStage(3)" class="btn btn-primary">
              Test Your Work →
            </button>
          </div>
        </div>
      </div>

      <!-- STAGE 4: Test -->
      <div v-if="currentStage === 3" class="stage-content">
        <div class="instructions">
          <h2>STAGE 4: Test & Verify</h2>

          <div class="step-item" :class="{ 'completed': testsCompleted.test1 }">
            <div class="test-header">
              <h4>Test 1: TODO</h4>
              <span v-if="testsCompleted.test1" class="test-check">✓ Done</span>
            </div>
            <p class="test-description">
              TODO: Add test description here.
            </p>
            <button v-if="!testsCompleted.test1" @click="completeTest('test1')" class="btn btn-outline btn-sm">
              Mark Complete
            </button>
          </div>

          <div class="step-item" :class="{ 'completed': testsCompleted.test2 }">
            <div class="test-header">
              <h4>Test 2: TODO</h4>
              <span v-if="testsCompleted.test2" class="test-check">✓ Done</span>
            </div>
            <p class="test-description">
              TODO: Add test description here.
            </p>
            <button v-if="!testsCompleted.test2" @click="completeTest('test2')" class="btn btn-outline btn-sm">
              Mark Complete
            </button>
          </div>

          <div v-if="allTestsCompleted" class="alert alert-success">
            <strong>🎉 Congratulations!</strong> You've completed the $TITLE workshop!
          </div>

          <div class="button-group">
            <button @click="goToStage(2)" class="btn btn-secondary">
              ← Back
            </button>
            <button @click="resetWorkshop" class="btn btn-warning">
              Restart Workshop
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Confirmation Modal -->
    <WorkshopModal
      v-model="showRestartModal"
      title="Restart Workshop?"
      message="This will reset all your progress. Are you sure?"
      type="confirm"
      confirm-text="Yes, Restart"
      @confirm="confirmRestart"
    />
  </div>
</template>

<script>
import { WorkshopHeader, WorkshopModal } from '../utils/components';
import { getProgress, saveProgress, resetProgress } from '../utils/progress';

const WORKSHOP_ID = '$ID';

export default {
  name: '${PASCAL_CASE}Home',
  components: { WorkshopHeader, WorkshopModal },
  data() {
    return {
      currentStage: 0,
      stageNames: ['Intro', 'Learn', 'Build', 'Test'],
      testsCompleted: {},
      showRestartModal: false
    };
  },
  computed: {
    workshopHubUrl() {
      return window.location.protocol + '//' + window.location.hostname + ':9000';
    },
    allTestsCompleted() {
      return this.testsCompleted.test1 && this.testsCompleted.test2;
    }
  },
  mounted() {
    this.loadProgress();
  },
  methods: {
    loadProgress() {
      const progress = getProgress(WORKSHOP_ID);
      this.currentStage = progress.currentStage || 0;
      this.testsCompleted = progress.testsCompleted?.[3]
        ? { test1: progress.testsCompleted[3].includes('test1'), test2: progress.testsCompleted[3].includes('test2') }
        : {};
    },
    saveCurrentProgress() {
      const progress = getProgress(WORKSHOP_ID);
      progress.currentStage = this.currentStage;
      if (!progress.testsCompleted) progress.testsCompleted = {};
      progress.testsCompleted[3] = Object.keys(this.testsCompleted).filter(k => this.testsCompleted[k]);
      saveProgress(WORKSHOP_ID, progress);
    },
    goToStage(stage) {
      this.currentStage = stage;
      this.saveCurrentProgress();
    },
    completeTest(testId) {
      this.testsCompleted[testId] = true;
      this.saveCurrentProgress();
    },
    resetWorkshop() {
      this.showRestartModal = true;
    },
    confirmRestart() {
      resetProgress(WORKSHOP_ID);
      this.currentStage = 0;
      this.testsCompleted = {};
      this.showRestartModal = false;
    }
  }
};
</script>

<style scoped>
.workshop-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.main-container {
  max-width: 900px;
  margin: 0 auto;
  padding-top: var(--spacing-4);
}

.stage-content {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.instructions h2 {
  color: var(--color-text);
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-xl);
}

.intro {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-5);
  line-height: 1.6;
}

.step-list {
  margin: var(--spacing-3) 0 0 var(--spacing-5);
  color: var(--color-text-secondary);
  line-height: 1.8;
}

.step-list li {
  margin-bottom: var(--spacing-2);
}

.step-item.completed {
  opacity: 0.7;
  border-left-color: #10b981;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2);
}

.test-check {
  color: #10b981;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}
</style>
DOC

# src/views/Editor.vue (using shared WorkshopEditorLayout)
cat <<DOC > "$FRONTEND_DIR/src/views/${PASCAL_CASE}Editor.vue"
<template>
  <WorkshopEditorLayout
    ref="layout"
    title="$TITLE"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
      <div class="alert">
        <strong>Your Task:</strong> TODO: Add task description here.
      </div>

      <h3>Instructions:</h3>
      <p class="note">
        Click the play button (▶) next to any step to automatically apply that change!
      </p>

      <h4>Step 1: TODO</h4>
      <ol>
        <li class="step-with-button">
          <span class="step-content">Open <code>build.gradle.kts</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Gradle build file">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('build.gradle.kts')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">TODO: Add step description</span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 2: Rebuild & Test</h4>
      <ol start="3">
        <li>Go to <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> and rebuild the app</li>
        <li><router-link to="/" class="link">Return to home</router-link> to verify!</li>
      </ol>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/index.js';

export default {
  name: '${PASCAL_CASE}Editor',
  components: { WorkshopEditorLayout },
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties'
      ],
      currentFile: null,
      fileContent: ''
    };
  },
  computed: {
    workshopHubUrl() {
      return this.\$refs.layout?.workshopHubUrl || (window.location.protocol + '//' + window.location.hostname + ':9000');
    }
  },
  methods: {
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    async loadFileStep(fileName) {
      await this.\$refs.layout.loadFile(fileName);
    },
    saveFile() {
      this.\$refs.layout.save();
    }
    // TODO: Add workshop-specific auto-fix methods here
  }
};
</script>

<style scoped>
/* All styling provided by WorkshopEditorLayout */
</style>
DOC

# Update registry
cat <<DOC >> "$REGISTRY_PATH"
  - id: $ID
    title: $TITLE
    description: >-
      TODO: Add workshop description.
    difficulty: Beginner
    estimatedMinutes: 30
    serviceName: $SERVICE_NAME
    port: $PORT
    url: /workshop/$SERVICE_NAME/
    dockerfile: java-springboot/$ID/Dockerfile
    topics:
      - TODO
DOC

# Update settings.gradle.kts
SETTINGS_FILE="$ROOT_DIR/java-springboot/settings.gradle.kts"
if [ -f "$SETTINGS_FILE" ]; then
  if command -v rg >/dev/null 2>&1; then
    if ! rg -n "include\(\"$ID\"\)" "$SETTINGS_FILE" >/dev/null; then
      echo "include(\"$ID\")" >> "$SETTINGS_FILE"
    fi
  else
    if ! grep -q "include(\"$ID\")" "$SETTINGS_FILE"; then
      echo "include(\"$ID\")" >> "$SETTINGS_FILE"
    fi
  fi
fi

echo ""
echo "✅ Workshop scaffold created: $MODULE_DIR"
echo ""
echo "Structure created:"
echo "  $ID/"
echo "  ├── Dockerfile"
echo "  ├── README.md"
echo "  ├── build.gradle.kts"
echo "  ├── settings.gradle.kts"
echo "  ├── src/main/java/com/redis/workshop/$PACKAGE_NAME/"
echo "  │   ├── ${PASCAL_CASE}Application.java"
echo "  │   └── EditorController.java"
echo "  ├── src/main/resources/application.properties"
echo "  └── frontend/"
echo "      ├── package.json"
echo "      ├── vue.config.js"
echo "      ├── public/index.html"
echo "      └── src/"
echo "          ├── main.js (with shared CSS imports)"
echo "          ├── App.vue"
echo "          ├── router/index.js"
echo "          ├── utils/"
echo "          │   ├── basePath.js"
echo "          │   ├── components.js (shared component re-exports)"
echo "          │   └── progress.js (progress tracking utility)"
echo "          └── views/"
echo "              ├── ${PASCAL_CASE}Home.vue (standardized 4-stage structure)"
echo "              └── ${PASCAL_CASE}Editor.vue"
echo ""
echo "Features included:"
echo "  • WorkshopHeader with progress indicator and Hub link"
echo "  • 4-stage workshop flow (Intro → Learn → Build → Test)"
echo "  • Progress tracking with localStorage persistence"
echo "  • Shared CSS imports (tokens, dark-theme, components)"
echo "  • WorkshopModal for confirmations"
echo ""
echo "Next steps:"
echo "  1. cd java-springboot/$ID/frontend && npm install"
echo "  2. Customize stages and content in ${PASCAL_CASE}Home.vue"
echo "  3. Customize editor instructions in ${PASCAL_CASE}Editor.vue"
echo "  4. Add your workshop-specific Java files"
echo "  5. Run: ./java-springboot/gradlew :workshop-hub:generateCompose"
echo ""
