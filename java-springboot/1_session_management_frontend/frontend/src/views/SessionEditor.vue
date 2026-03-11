<template>
  <WorkshopEditorLayout
    ref="layout"
    :title="editorTitle"
    :files="files"
    @file-loaded="onFileLoaded"
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
</template>

<script>
import {
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../workshop-frontend-shared/src/index.js';
import { getWorkshopHubUrl } from '../utils/basePath';
import { loadWorkshopContentView } from '../utils/workshopContent';

function updateEditorFile(view, fileName, transform, successMessage) {
  if (view.currentFile !== fileName) {
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
      ],
      currentFile: null
    };
  },
  computed: {
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
      return this.editorContent?.title || 'Stage 2: Enable Redis';
    },
    workshopHubUrl() {
      return this.$refs.layout?.workshopHubUrl || getWorkshopHubUrl();
    }
  },
  async mounted() {
    await this.loadEditorContent();
  },
  methods: {
    applyEditorStep(stepId) {
      const handlers = {
        'session-editor.changeStoreType': () => this.changeStoreType(),
        'session-editor.uncommentGradleDependencies': () => this.uncommentGradleDependencies(),
        'session-editor.uncommentRedisConfig': () => this.uncommentRedisConfig(),
        'session-editor.uncommentSecurityConfig': () => this.uncommentSecurityConfig()
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
        this.editorContent = await loadWorkshopContentView('session-editor');
      } catch (error) {
        console.error('Error loading session editor content:', error);
        this.contentError = 'Unable to load editor instructions. Refresh the page and try again.';
      }
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
    },
    onFileLoaded({ fileName }) {
      this.currentFile = fileName;
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
</style>
