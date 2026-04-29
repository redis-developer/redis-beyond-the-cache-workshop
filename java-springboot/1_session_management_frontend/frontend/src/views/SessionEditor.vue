<template>
  <WorkshopCodeEditorShell
    title="Distributed Session Management with Redis"
    :editor-title="editorTitle"
    :editor-src="codeEditorUrl"
    :back-to-workshop-url="backToWorkshopUrl"
    :redis-insight-url="redisInsightUrl"
    :hub-url="workshopHubUrl"
    :use-embedded-editor="useEmbeddedEditor"
  >
    <template #instructions>
      <div v-if="contentError" class="content-state content-state--error">
        {{ contentError }}
      </div>
      <div v-else-if="!editorContent" class="content-state">
        Loading editor instructions...
      </div>
      <div v-if="editorNotice" class="content-state content-state--notice">
        {{ editorNotice }}
      </div>
      <WorkshopContentRenderer
        v-if="editorContent"
        :content="editorContent"
        :action-handlers="contentActionHandlers"
        :show-title="false"
        :show-summary="false"
        :show-stage-title="false"
        @render-error="handleContentRenderError"
      />
    </template>

    <template #fallback>
      <div class="legacy-editor-fallback">
        <WorkshopEditorLayout
          ref="layout"
          :title="editorTitle"
          :files="files"
          :show-session-restart-controls="true"
        >
          <template #instructions>
            <div v-if="contentError" class="content-state content-state--error">
              {{ contentError }}
            </div>
            <div v-else-if="!editorContent" class="content-state">
              Loading editor instructions...
            </div>
            <WorkshopContentRenderer
              v-else
              :content="editorContent"
              :action-handlers="contentActionHandlers"
              :show-title="false"
              :show-summary="false"
              :show-stage-title="false"
              @render-error="handleContentRenderError"
            />
          </template>
        </WorkshopEditorLayout>
      </div>
    </template>
  </WorkshopCodeEditorShell>
</template>

<script>
import {
  getBasePath,
  getApiUrl,
  getCodeEditorFileUrl,
  getCodeEditorUrl,
  getWorkshopHubUrl,
  loadEditorWorkspaceMetadata,
  WorkshopCodeEditorShell,
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../workshop-frontend-shared/src/index.js';
import { loadWorkshopContentView } from '../utils/workshopContent';

function buildRouteUrl(routePath) {
  const basePath = getBasePath();
  const normalizedRoutePath = routePath.startsWith('/') ? routePath : `/${routePath}`;

  if (!basePath || basePath === '/') {
    return normalizedRoutePath;
  }

  return `${basePath}${normalizedRoutePath}`;
}

function showEmbeddedEditorNotice(view, target) {
  view.editorNotice = target
    ? `Open ${target} in VS Code and save manual edits there. Apply Change can also update the file automatically.`
    : 'Use VS Code to edit and save manual changes. Apply Change saves guided edits automatically.';
}

const EDITOR_STEP_CONFIG = {
  '4.changeStoreType': {
    fileName: 'application.properties',
    successMessage: 'Store type changed to redis.',
    transform: content => content.replace(
      'spring.session.store-type=none',
      'spring.session.store-type=redis'
    )
  },
  '4.uncommentGradleDependencies': {
    fileName: 'build.gradle.kts',
    successMessage: 'Redis dependencies uncommented.',
    transform: content => content
      .replace(
        '// implementation("org.springframework.boot:spring-boot-starter-data-redis")',
        'implementation("org.springframework.boot:spring-boot-starter-data-redis")'
      )
      .replace(
        '// implementation("org.springframework.session:spring-session-data-redis")',
        'implementation("org.springframework.session:spring-session-data-redis")'
      )
  },
  '4.uncommentRedisConfig': {
    fileName: 'application.properties',
    successMessage: 'Redis session config uncommented.',
    transform: content => content
      .replace('#spring.session.redis.namespace=spring:session', 'spring.session.redis.namespace=spring:session')
      .replace('#spring.session.redis.flush-mode=immediate', 'spring.session.redis.flush-mode=immediate')
      .replace('#spring.session.redis.repository-type=default', 'spring.session.redis.repository-type=default')
  },
  '4.uncommentSecurityConfig': {
    fileName: 'SecurityConfig.java',
    successMessage: 'Security session config uncommented.',
    transform: content => content
      .replace(
        '// import org.springframework.security.web.context.HttpSessionSecurityContextRepository;',
        'import org.springframework.security.web.context.HttpSessionSecurityContextRepository;'
      )
      .replace(
        '// import org.springframework.security.web.context.SecurityContextRepository;',
        'import org.springframework.security.web.context.SecurityContextRepository;'
      )
      .replace(
        '            // .securityContext(context -> context\n            //     .securityContextRepository(securityContextRepository())\n            // )',
        '            .securityContext(context -> context\n                .securityContextRepository(securityContextRepository())\n            )'
      )
      .replace(
        '    // @Bean\n    // public SecurityContextRepository securityContextRepository() {\n    //     return new HttpSessionSecurityContextRepository();\n    // }',
        '    @Bean\n    public SecurityContextRepository securityContextRepository() {\n        return new HttpSessionSecurityContextRepository();\n    }'
      )
  }
};

function updateEditorFile(view, fileName, transform, successMessage) {
  if (view.$refs.layout.getCurrentFile() !== fileName) {
    view.$refs.layout.showStatus(`Please open ${fileName} first!`, 'error');
    return;
  }

  const content = transform(view.$refs.layout.getCurrentContent());
  view.$refs.layout.updateContent(content);
  view.$refs.layout.showStatus(successMessage, 'success');
}

export default {
  name: 'SessionEditor',
  components: {
    WorkshopCodeEditorShell,
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      activeCodeEditorFilePath: '',
      codeEditorFilePathByName: {},
      codeEditorWorkspaceRoot: '',
      codeEditorOpenRequest: 0,
      editorContent: null,
      contentError: '',
      editorNotice: '',
      files: [
        'build.gradle.kts',
        'application.properties',
        'SecurityConfig.java'
      ]
    };
  },
  computed: {
    backToWorkshopRoute() {
      const route = this.$route.query.returnTo;
      if (typeof route === 'string' && /^\/[0-3]$/.test(route)) {
        return route;
      }

      return '/2';
    },
    backToWorkshopUrl() {
      return buildRouteUrl(this.backToWorkshopRoute);
    },
    codeEditorUrl() {
      if (this.activeCodeEditorFilePath) {
        return getCodeEditorFileUrl(this.activeCodeEditorFilePath, {
          workspaceRoot: this.codeEditorWorkspaceRoot,
          requestId: this.codeEditorOpenRequest
        });
      }

      return getCodeEditorUrl({ workspaceRoot: this.codeEditorWorkspaceRoot });
    },
    contentActionHandlers() {
      const navigationHandlers = {
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route);
          }
        }
      };

      if (this.useEmbeddedEditor) {
        return {
          ...navigationHandlers,
          applyEditorStep: ({ args }) => this.applyEmbeddedEditorStep(args?.stepId),
          openFile: ({ args }) => this.openEmbeddedEditorFile(args?.file),
          recompileApp: () => this.recompileLearnerApp(),
          saveFile: () => showEmbeddedEditorNotice(this)
        };
      }

      return {
        ...navigationHandlers,
        applyEditorStep: ({ args }) => this.applyEditorStep(args.stepId),
        openFile: ({ args }) => this.loadFileStep(args.file),
        recompileApp: () => this.recompileLearnerApp(),
        saveFile: () => this.saveFile()
      };
    },
    editorTitle() {
      return this.editorContent?.title || 'Code Editor: Enable Redis';
    },
    redisInsightUrl() {
      return buildRouteUrl(`${this.backToWorkshopRoute}?tool=redis-insight`);
    },
    useEmbeddedEditor() {
      return this.$route.query.editor !== 'legacy';
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    await Promise.all([
      this.loadEditorContent(),
      this.loadCodeEditorFiles()
    ]);
  },
  methods: {
    applyEditorStep(stepId) {
      const config = EDITOR_STEP_CONFIG[stepId];
      if (!config) {
        this.$refs.layout.showStatus(`Unknown editor step: ${stepId}`, 'error');
        return;
      }

      updateEditorFile(
        this,
        config.fileName,
        config.transform,
        `${config.successMessage} Click Save!`
      );
    },
    async applyEmbeddedEditorStep(stepId) {
      const config = EDITOR_STEP_CONFIG[stepId];
      if (!config) {
        this.editorNotice = `Unknown editor step: ${stepId || 'missing step id'}.`;
        return;
      }

      this.editorNotice = `Applying change to ${config.fileName}...`;

      try {
        const currentContent = await this.loadEditableFile(config.fileName);
        const nextContent = config.transform(currentContent);

        if (nextContent === currentContent) {
          this.editorNotice = `${config.successMessage} No file changes were needed.`;
          await this.refreshEmbeddedEditorFile(config.fileName);
          return;
        }

        await this.saveEditableFile(config.fileName, nextContent);
        this.editorNotice = `${config.successMessage} File saved. Recompile App when you finish all changes.`;
        await this.refreshEmbeddedEditorFile(config.fileName);
      } catch (error) {
        console.error('Failed to apply editor step:', error);
        this.editorNotice = `Could not apply change: ${error.message}`;
      }
    },
    handleContentRenderError(issues) {
      console.warn('Session editor content render issues:', issues);
    },
    async loadEditorContent() {
      try {
        this.editorContent = await loadWorkshopContentView('4');
      } catch (error) {
        console.error('Error loading session editor content:', error);
        this.contentError = 'Unable to load editor instructions. Refresh the page and try again.';
      }
    },
    async loadCodeEditorFiles() {
      if (!this.useEmbeddedEditor) {
        return;
      }

      try {
        const metadata = await loadEditorWorkspaceMetadata();
        this.codeEditorFilePathByName = metadata.filePathMap;
        this.codeEditorWorkspaceRoot = metadata.codeEditorWorkspaceRoot || metadata.workspaceRoot;
      } catch (error) {
        console.warn('Failed to load code editor file metadata:', error);
      }
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
    },
    async loadEditableFile(fileName) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        cache: 'no-store',
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`Failed to load ${fileName}: ${response.status}`);
      }

      const payload = await response.json();
      if (payload.error) {
        throw new Error(payload.error);
      }

      if (typeof payload.content !== 'string') {
        throw new Error(`No content returned for ${fileName}`);
      }

      return payload.content;
    },
    async openEmbeddedEditorFile(fileName) {
      if (!fileName) {
        showEmbeddedEditorNotice(this);
        return;
      }

      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles();
      }

      const workspacePath = this.codeEditorFilePathByName[fileName];
      if (!workspacePath) {
        this.editorNotice = `Could not resolve ${fileName} from the workshop manifest. Open it manually in VS Code.`;
        return;
      }

      this.codeEditorOpenRequest += 1;
      this.activeCodeEditorFilePath = workspacePath;
      this.editorNotice = `Opening ${fileName} in VS Code.`;
    },
    async refreshEmbeddedEditorFile(fileName) {
      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles();
      }

      const workspacePath = this.codeEditorFilePathByName[fileName];
      if (!workspacePath) {
        return;
      }

      this.activeCodeEditorFilePath = workspacePath;
      this.codeEditorOpenRequest += 1;
    },
    saveFile() {
      this.$refs.layout.save();
    },
    async recompileLearnerApp() {
      this.editorNotice = 'Recompiling and restarting the learner app...';

      try {
        const response = await fetch(getApiUrl('/internal/session-runner/restart'), {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ rebuild: true, async: true })
        });
        const data = await this.readResponseBody(response);

        if (!response.ok || data.error) {
          throw new Error(data.error || data.message || 'Failed to recompile app');
        }

        this.editorNotice = 'Recompile started. When the app is ready, return to the learner app and verify the session behavior.';
      } catch (error) {
        this.editorNotice = `Could not recompile app: ${error.message}`;
      }
    },
    async readResponseBody(response) {
      const text = await response.text();

      if (!text) {
        return {};
      }

      try {
        return JSON.parse(text);
      } catch {
        return {
          error: response.ok ? '' : text
        };
      }
    },
    async saveEditableFile(fileName, content) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        method: 'POST',
        cache: 'no-store',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content })
      });

      if (!response.ok) {
        throw new Error(`Failed to save ${fileName}: ${response.status}`);
      }

      const payload = await response.json();
      if (payload.error) {
        throw new Error(payload.error);
      }
    }
  }
};
</script>

<style scoped>
.content-state {
  color: #cccccc;
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
}

.content-state--notice {
  background: rgba(59, 130, 246, 0.14);
  border: 1px solid rgba(59, 130, 246, 0.32);
  border-radius: var(--radius-lg, 8px);
  color: #bfdbfe;
  margin-bottom: var(--spacing-4, 1rem);
  padding: var(--spacing-3, 0.75rem);
}

.legacy-editor-fallback {
  height: calc(100vh - 72px);
}

.legacy-editor-fallback :deep(.workshop-editor),
.legacy-editor-fallback :deep(.main-container) {
  height: 100%;
  width: 100%;
}

:deep(.content-editor-step-item__hint),
:deep(.content-section__header .workshop-markdown),
:deep(.workshop-markdown) {
  color: #cccccc;
}

:deep(.content-section__header h3),
:deep(.content-editor-step-list__title),
:deep(.content-step-item__heading h4) {
  color: #ffffff;
}
</style>
