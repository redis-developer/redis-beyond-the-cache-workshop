<template>
  <main class="session-editor-shell">
    <header class="session-editor-shell__top-bar">
      <div class="session-editor-shell__top-bar-content">
        <div class="session-editor-shell__brand" aria-label="Redis workshop">
          <RedisWordmark
            class="session-editor-shell__logo"
          />
          <h1>Distributed Session Management with Redis</h1>
        </div>

        <nav class="session-editor-shell__header-actions" aria-label="Editor navigation">
          <router-link
            class="session-editor-shell__header-action"
            :to="redisInsightRoute"
          >
            Redis Insight
          </router-link>
          <router-link
            class="session-editor-shell__header-action"
            :to="backToWorkshopRoute"
          >
            Back to Workshop
          </router-link>
          <a
            class="session-editor-shell__header-action"
            :href="workshopHubUrl"
          >
            Back to Hub
          </a>
        </nav>
      </div>
    </header>

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
  </main>
</template>

<script>
import {
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../workshop-frontend-shared/src/index.js';
import { getWorkshopHubUrl } from '../utils/basePath';
import RedisWordmark from '../components/RedisWordmark.vue';
import { loadWorkshopContentView } from '../utils/workshopContent';

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
    RedisWordmark,
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      editorContent: null,
      contentError: '',
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
    contentActionHandlers() {
      return {
        applyEditorStep: ({ args }) => this.applyEditorStep(args.stepId),
        openFile: ({ args }) => this.loadFileStep(args.file),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => this.$router.push(args.route),
        saveFile: () => this.saveFile()
      };
    },
    editorTitle() {
      return this.editorContent?.title || 'Code Editor: Enable Redis';
    },
    redisInsightRoute() {
      return `/redis-insight-view?returnTo=${encodeURIComponent(this.backToWorkshopRoute)}`;
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    await this.loadEditorContent();
  },
  methods: {
    applyEditorStep(stepId) {
      const handlers = {
        '4.changeStoreType': () => this.changeStoreType(),
        '4.uncommentGradleDependencies': () => this.uncommentGradleDependencies(),
        '4.uncommentRedisConfig': () => this.uncommentRedisConfig(),
        '4.uncommentSecurityConfig': () => this.uncommentSecurityConfig()
      };

      const handler = handlers[stepId];
      if (!handler) {
        this.$refs.layout.showStatus(`Unknown editor step: ${stepId}`, 'error');
        return;
      }

      handler();
    },
    changeStoreType() {
      updateEditorFile(this, 'application.properties', content => (
        content.replace('spring.session.store-type=none', 'spring.session.store-type=redis')
      ), 'Store type changed to redis! Click Save!');
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
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
    },
    saveFile() {
      this.$refs.layout.save();
    },
    uncommentGradleDependencies() {
      updateEditorFile(this, 'build.gradle.kts', content => (
        content
          .replace(
            '// implementation("org.springframework.boot:spring-boot-starter-data-redis")',
            'implementation("org.springframework.boot:spring-boot-starter-data-redis")'
          )
          .replace(
            '// implementation("org.springframework.session:spring-session-data-redis")',
            'implementation("org.springframework.session:spring-session-data-redis")'
          )
      ), 'Redis dependencies uncommented! Click Save!');
    },
    uncommentRedisConfig() {
      updateEditorFile(this, 'application.properties', content => (
        content
          .replace('#spring.session.redis.namespace=spring:session', 'spring.session.redis.namespace=spring:session')
          .replace('#spring.session.redis.flush-mode=immediate', 'spring.session.redis.flush-mode=immediate')
          .replace('#spring.session.redis.repository-type=default', 'spring.session.redis.repository-type=default')
      ), 'Redis config uncommented! Click Save!');
    },
    uncommentSecurityConfig() {
      updateEditorFile(this, 'SecurityConfig.java', content => (
        content
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
      ), 'Security config uncommented! Click Save!');
    }
  }
};
</script>

<style scoped>
.session-editor-shell {
  min-height: 100vh;
  background: var(--color-background, #0A151B);
  color: var(--color-text, #e2e8f0);
}

.session-editor-shell__top-bar {
  align-items: center;
  background-color: var(--color-dark-800, #0D1A22);
  border-bottom: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  display: flex;
  height: 72px;
}

.session-editor-shell__top-bar-content {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  justify-content: space-between;
  padding: 0 var(--spacing-6, 1.5rem);
  width: 100%;
}

.session-editor-shell__brand {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  min-width: 0;
}

.session-editor-shell__logo {
  flex: 0 0 auto;
  height: 1.75rem;
  width: 5.25rem;
}

.session-editor-shell__brand h1 {
  color: var(--color-text, #e2e8f0);
  font-size: clamp(1.125rem, 2vw, 1.5rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: -0.01em;
  line-height: 1.2;
  margin: 0;
  min-width: 0;
}

.session-editor-shell__header-actions {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  gap: var(--spacing-2, 0.5rem);
}

.session-editor-shell__header-action {
  align-items: center;
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  display: inline-flex;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: var(--font-weight-semibold, 600);
  justify-content: center;
  line-height: 1;
  min-height: 2.5rem;
  padding: 0.65rem 0.95rem;
  text-decoration: none;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.session-editor-shell__header-action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

:deep(.workshop-editor) {
  height: calc(100vh - 72px) !important;
}

:deep(.main-container) {
  height: calc(100vh - 72px) !important;
  width: 100% !important;
}

.content-state {
  color: #cccccc;
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
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

@media (max-width: 720px) {
  .session-editor-shell__top-bar {
    height: auto;
    min-height: 72px;
  }

  .session-editor-shell__top-bar-content {
    align-items: flex-start;
    flex-direction: column;
    padding: var(--spacing-3, 0.75rem) var(--spacing-4, 1rem);
  }

  .session-editor-shell__header-actions {
    flex-wrap: wrap;
    width: 100%;
  }

  :deep(.workshop-editor),
  :deep(.main-container) {
    height: calc(100vh - 120px) !important;
  }
}
</style>
