<template>
  <WorkshopCodeEditorShell
    title="Distributed Locks"
    :editor-title="editorTitle"
    :editor-src="codeEditorUrl"
    :back-to-workshop-url="backToWorkshopUrl"
    :redis-insight-url="redisInsightUrl"
    :hub-url="workshopHubUrl"
    :use-embedded-editor="useEmbeddedEditor"
  >
    <template #instructions>
      <div v-if="contentError" class="content-error">
        {{ contentError }}
      </div>

      <div v-if="editorNotice" class="content-notice">
        {{ editorNotice }}
      </div>

      <WorkshopContentRenderer
        v-if="editorContent"
        :content="editorContent"
        :widgets="contentWidgets"
        :widget-props="contentWidgetProps"
        :action-handlers="contentActionHandlers"
        :show-title="true"
        :show-summary="true"
        :show-stage-title="false"
      />
    </template>

    <template #fallback>
      <div class="legacy-editor-fallback">
        <WorkshopEditorLayout
          ref="layout"
          :title="editorTitle"
          :files="files"
          show-session-restart-controls
          @file-loaded="onFileLoaded"
        >
          <template #instructions>
            <div v-if="contentError" class="content-error">
              {{ contentError }}
            </div>

            <WorkshopContentRenderer
              v-else-if="editorContent"
              :content="editorContent"
              :widgets="contentWidgets"
              :widget-props="contentWidgetProps"
              :action-handlers="contentActionHandlers"
              :show-title="true"
              :show-summary="true"
              :show-stage-title="false"
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
  WorkshopContentRenderer,
  WorkshopCodeEditorShell,
  WorkshopEditorLayout,
  getApiUrl,
  getCodeEditorFileUrl,
  getCodeEditorUrl,
  getRedisInsightUrl,
  loadEditorWorkspaceMetadata,
  getWorkshopHubUrl
} from '../../../../../workshop-frontend-shared/src/index.js';
import LocksEditorReferenceWidget from '../components/content/LocksEditorReferenceWidget.vue';
import { fetchWorkshopContentView } from '../utils/workshopContent';

const CONTENT_WIDGETS = {
  'locks-editor.reference-review': LocksEditorReferenceWidget
};

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
    ? `Open ${target} in VS Code and save changes there. Guided auto-apply remains available in the legacy editor fallback.`
    : 'Use VS Code to edit and save files. Guided auto-apply remains available in the legacy editor fallback.';
}

export default {
  name: 'LocksEditor',
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
      files: [
        'build.gradle.kts',
        'application.properties',
        'LockManager.java'
      ],
      currentFile: null,
      purchaseServiceReview: '',
      editorContent: null,
      editorNotice: '',
      contentError: ''
    };
  },
  computed: {
    backToWorkshopUrl() {
      return buildRouteUrl('/reentrant/implement');
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
        openReference: ({ args }) => this.openReference(args?.referenceId),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route);
          }
        }
      };

      if (this.useEmbeddedEditor) {
        return {
          ...navigationHandlers,
          applyEditorStep: () => showEmbeddedEditorNotice(this),
          openFile: ({ args }) => this.openEmbeddedEditorFile(args?.file),
          saveFile: () => showEmbeddedEditorNotice(this)
        };
      }

      return {
        ...navigationHandlers,
        openFile: ({ args }) => this.loadFileStep(args.file),
        applyEditorStep: ({ args }) => this.applyEditorStep(args.stepId),
        saveFile: () => this.saveFile()
      };
    },
    contentWidgetProps() {
      return {
        'locks-editor.reference-review': {
          referenceContent: this.purchaseServiceReview
        }
      };
    },
    contentWidgets() {
      return CONTENT_WIDGETS;
    },
    editorTitle() {
      return this.editorContent?.title || 'Implement the Reentrant Lock';
    },
    redisInsightUrl() {
      return getRedisInsightUrl();
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
      this.loadContent(),
      this.loadCodeEditorFiles()
    ]);
  },
  methods: {
    async loadContent() {
      this.contentError = '';

      try {
        this.editorContent = await fetchWorkshopContentView('locks-editor');
      } catch (error) {
        this.contentError = error.message;
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
    onFileLoaded({ fileName }) {
      this.currentFile = fileName;
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
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
    async openReference(referenceId) {
      if (referenceId === 'locks-editor.purchase-service-review') {
        await this.loadPurchaseServiceReview();
      }
    },
    async loadPurchaseServiceReview() {
      try {
        const response = await fetch(getApiUrl('/api/review/purchase-service'));
        const data = await response.json();
        if (data.error) {
          this.showEditorStatus(data.error, 'error');
          return;
        }
        this.purchaseServiceReview = data.content || '';
        this.showEditorStatus('Read-only review loaded.', 'success');
      } catch (error) {
        this.showEditorStatus('Failed to load PurchaseService.java review.', 'error');
      }
    },
    showEditorStatus(message, type = 'info') {
      if (this.$refs.layout) {
        this.$refs.layout.showStatus(message, type);
        return;
      }

      this.editorNotice = message;
    },
    async applyEditorStep(stepId) {
      if (stepId === 'locks-editor.enable-dependencies') {
        await this.enableDependencies();
        return;
      }

      if (stepId === 'locks-editor.enable-lock-mode') {
        await this.enableLockMode();
        return;
      }

      if (stepId === 'locks-editor.apply-redisson-lock') {
        await this.applyRedissonLock();
        return;
      }

      this.$refs.layout.showStatus(`Unknown editor step: ${stepId}`, 'error');
    },
    saveFile() {
      this.$refs.layout.save();
    },
    async enableDependencies() {
      if (!this.currentFile?.endsWith('build.gradle.kts')) {
        await this.$refs.layout.loadFile('build.gradle.kts');
      }

      const content = this.$refs.layout.getCurrentContent();
      if (!content.includes('// implementation("org.springframework.boot:spring-boot-starter-data-redis")')) {
        this.$refs.layout.showStatus('Dependencies already enabled. Click Save to persist.', 'success');
        return;
      }

      const updated = content
        .replace('// implementation("org.springframework.boot:spring-boot-starter-data-redis")', 'implementation("org.springframework.boot:spring-boot-starter-data-redis")')
        .replace('// implementation("org.redisson:redisson-spring-boot-starter:3.27.2")', 'implementation("org.redisson:redisson-spring-boot-starter:3.27.2")');
      this.$refs.layout.updateContent(updated);
      this.$refs.layout.showStatus('Dependencies enabled. Click Save to persist.', 'success');
    },
    async enableLockMode() {
      if (!this.currentFile?.endsWith('application.properties')) {
        await this.$refs.layout.loadFile('application.properties');
      }

      const content = this.$refs.layout.getCurrentContent();
      if (!content.includes('workshop.lock.mode')) {
        this.$refs.layout.showStatus('Open application.properties first.', 'error');
        return;
      }

      this.$refs.layout.updateContent(content.replace('workshop.lock.mode=none', 'workshop.lock.mode=redisson'));
      this.$refs.layout.showStatus('Lock mode updated. Click Save to persist.', 'success');
    },
    async applyRedissonLock() {
      if (!this.currentFile?.endsWith('LockManager.java')) {
        await this.$refs.layout.loadFile('LockManager.java');
      }

      let content = this.$refs.layout.getCurrentContent();
      if (content.includes('lock.tryLock(') && content.includes('lock.unlock()')) {
        this.$refs.layout.showStatus('Lock already implemented. Click Save to persist.', 'success');
        return;
      }

      content = content.replace('// import org.redisson.api.RedissonClient;', 'import org.redisson.api.RedissonClient;');
      content = content.replace('// private final RedissonClient redissonClient;', 'private final RedissonClient redissonClient;');
      content = content.replace('// @Autowired(required = false) RedissonClient redissonClient,', '@Autowired(required = false) RedissonClient redissonClient,');
      content = content.replace('// this.redissonClient = redissonClient;', 'this.redissonClient = redissonClient;');
      content = content.replace(
        /public boolean isEnabled\(\) \{\s*\/\/ TODO: Update this check after implementing the lock\s*\/\/ return "redisson"\.equalsIgnoreCase\(lockMode\) && redissonClient != null;\s*return false;\s*\}/,
        `public boolean isEnabled() {\n        return "redisson".equalsIgnoreCase(lockMode) && redissonClient != null;\n    }`
      );
      content = this.ensureImport(content, 'import java.util.concurrent.TimeUnit;');
      content = this.ensureImport(content, 'import org.redisson.api.RLock;');

      const lockImpl = `RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                return onBusy.get();
            }
            return onAcquired.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return onBusy.get();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }`;

      content = content.replace(/\/\/ TODO: Implement Redisson lock here\s*\n\s*return onAcquired\.get\(\);/, lockImpl);
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('All changes applied. Click Save to persist.', 'success');
    },
    ensureImport(content, importLine) {
      if (content.includes(importLine)) {
        return content;
      }

      const lines = content.split('\n');
      let lastImportIndex = -1;
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].startsWith('import ')) {
          lastImportIndex = i;
        }
      }

      if (lastImportIndex >= 0) {
        lines.splice(lastImportIndex + 1, 0, importLine);
        return lines.join('\n');
      }

      return `${importLine}\n${content}`;
    }
  }
};
</script>

<style scoped>
.content-error {
  padding: var(--spacing-4);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(239, 68, 68, 0.45);
  background: rgba(127, 29, 29, 0.2);
  color: #fecaca;
}

.content-notice {
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(59, 130, 246, 0.32);
  background: rgba(59, 130, 246, 0.14);
  color: #bfdbfe;
}

:deep(.workshop-content-renderer) {
  gap: var(--spacing-5);
}

:deep(.content-renderer-header__title) {
  color: #ffffff;
  font-size: var(--font-size-lg);
}

.legacy-editor-fallback {
  height: calc(100vh - 72px);
}

.legacy-editor-fallback :deep(.workshop-editor),
.legacy-editor-fallback :deep(.main-container) {
  height: 100%;
  width: 100%;
}
</style>
