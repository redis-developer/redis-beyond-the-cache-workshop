<template>
  <WorkshopEditorLayout
    ref="layout"
    :title="editorTitle"
    :files="files"
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
</template>

<script>
import { WorkshopContentRenderer, WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/index.js';
import LocksEditorReferenceWidget from '../components/content/LocksEditorReferenceWidget.vue';
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';
import { fetchWorkshopContentView } from '../utils/workshopContent';

const CONTENT_WIDGETS = {
  'locks-editor.reference-review': LocksEditorReferenceWidget
};

export default {
  name: 'LocksEditor',
  components: {
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'LockManager.java'
      ],
      currentFile: null,
      purchaseServiceReview: '',
      editorContent: null,
      contentError: ''
    };
  },
  computed: {
    contentActionHandlers() {
      return {
        openFile: ({ args }) => this.loadFileStep(args.file),
        applyEditorStep: ({ args }) => this.applyEditorStep(args.stepId),
        saveFile: () => this.saveFile(),
        openReference: ({ args }) => this.openReference(args.referenceId),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args.route) {
            this.$router.push(args.route);
          }
        }
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
    workshopHubUrl() {
      return this.$refs.layout?.workshopHubUrl || getWorkshopHubUrl();
    }
  },
  async mounted() {
    await this.loadContent();
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
    onFileLoaded({ fileName }) {
      this.currentFile = fileName;
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
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
          this.$refs.layout.showStatus(data.error, 'error');
          return;
        }
        this.purchaseServiceReview = data.content || '';
        this.$refs.layout.showStatus('Read-only review loaded.', 'success');
      } catch (error) {
        this.$refs.layout.showStatus('Failed to load PurchaseService.java review.', 'error');
      }
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

:deep(.workshop-content-renderer) {
  gap: var(--spacing-5);
}

:deep(.content-renderer-header__title) {
  color: #ffffff;
  font-size: var(--font-size-lg);
}
</style>
