<template>
  <div class="session-home">
    <WorkshopHeader :hub-url="workshopHubUrl" />

    <div class="welcome-container">
      <div class="instructions-column">
        <div class="instructions">
          <div v-if="contentError" class="content-state content-state--error">
            {{ contentError }}
          </div>
          <div v-else-if="!homeContent" class="content-state">
            Loading workshop instructions...
          </div>
          <WorkshopContentRenderer
            v-else
            :content="resolvedHomeContent"
            :tokens="contentTokens"
            :active-stage-id="activeStageId"
            :action-handlers="contentActionHandlers"
            :widgets="contentWidgets"
            :widget-props="contentWidgetProps"
            :show-title="false"
            :show-summary="false"
            @render-error="handleContentRenderError"
          />
        </div>
      </div>

      <div class="details-column">
        <div class="details-card">
          <div class="header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Session Details</h2>
          </div>

          <div class="session-info">
            <div class="info-item">
              <span class="info-label">Session ID</span>
              <span class="info-value">{{ sessionInfo.sessionId }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Storage Type</span>
              <span class="info-value" :class="{ 'redis-enabled': redisEnabled }">{{ sessionInfo.storageType }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Username</span>
              <span class="info-value">{{ sessionInfo.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created At</span>
              <span class="info-value">{{ sessionInfo.createdAt }}</span>
            </div>
          </div>

          <div class="actions">
            <button @click="refreshSession" class="btn btn-secondary">
              Refresh Session
            </button>
            <button @click="handleLogout" class="btn btn-danger">
              Logout
            </button>
          </div>

          <button @click="resetProgress" class="btn btn-link">
            Reset Test Progress
          </button>

          <div class="footer-info">
            <p v-if="redisEnabled"><strong>Tip:</strong> Session data persists in Redis even if the application restarts!</p>
            <p v-else><strong>Tip:</strong> In-memory sessions are lost when the application restarts.</p>
          </div>
        </div>
      </div>
    </div>

    <WorkshopModal
      v-model="modal.show"
      :title="modal.title"
      :message="modal.message"
      :type="modal.type"
      @confirm="handleModalConfirm"
      @cancel="handleModalCancel"
    />
  </div>
</template>

<script>
import { WorkshopContentRenderer } from '../../../../../workshop-frontend-shared/src/index.js';
import {
  getApiUrl,
  getBasePath,
  getRedisInsightUrl,
  getWorkshopHubUrl
} from '../utils/basePath';
import { WorkshopModal, WorkshopHeader } from '../utils/components';
import SessionComparisonWidget from '../components/content/SessionComparisonWidget.vue';
import { loadWorkshopContentView } from '../utils/workshopContent';

function createStage1Defaults() {
  return {
    test1: false,
    test2: false,
    test3: false
  };
}

function createStage3Defaults() {
  return {
    test1: false,
    test2: false,
    test3: false,
    test4: false,
    test5: false
  };
}

function loadSavedProgress(storageKey, defaults) {
  const saved = localStorage.getItem(storageKey);
  if (!saved) {
    return { ...defaults };
  }

  try {
    return { ...defaults, ...JSON.parse(saved) };
  } catch (error) {
    console.warn(`Unable to parse saved workshop progress for ${storageKey}`, error);
    return { ...defaults };
  }
}

function cloneContent(content) {
  return JSON.parse(JSON.stringify(content));
}

function prepareSequentialStepList(items, progress) {
  const preparedItems = items.map(item => (
    progress[item.itemId]
      ? { ...item, actions: [] }
      : item
  ));

  return preparedItems.filter((item, index) => (
    index === 0 || Boolean(progress[items[index - 1].itemId])
  ));
}

function updateStageStepList(stage, sectionId, listId, progress) {
  stage.sections = stage.sections.map(section => {
    if (section.sectionId !== sectionId) {
      return section;
    }

    return {
      ...section,
      blocks: section.blocks.map(block => {
        if (block.type !== 'stepList' || block.listId !== listId) {
          return block;
        }

        return {
          ...block,
          items: prepareSequentialStepList(block.items || [], progress)
        };
      })
    };
  });
}

function prepareSessionHomeContent(content, stage1Tests, stage3Tests) {
  const nextContent = cloneContent(content);
  const stages = nextContent.stages || [];

  const stage1 = stages.find(stage => stage.stageId === 'problem');
  if (stage1) {
    updateStageStepList(stage1, 'stage-1', 'stage1-tests', stage1Tests);
  }

  const stage3 = stages.find(stage => stage.stageId === 'redis-enabled');
  if (stage3) {
    updateStageStepList(stage3, 'stage-3-tests', 'stage3-tests', stage3Tests);
    if (!stage3Tests.test4) {
      stage3.sections = stage3.sections.filter(section => section.sectionId !== 'stage-3-completion');
    }
  }

  return nextContent;
}

export default {
  name: 'SessionHome',
  components: {
    WorkshopContentRenderer,
    WorkshopModal,
    WorkshopHeader
  },
  data() {
    return {
      homeContent: null,
      contentError: '',
      sessionInfo: {
        sessionId: 'ABC123DEF456',
        storageType: 'In-Memory',
        username: 'user',
        createdAt: new Date().toLocaleString()
      },
      redisEnabled: false,
      stage1Completed: false,
      restartingLab: false,
      previousSessionId: null,
      stage1Tests: createStage1Defaults(),
      stage3Tests: createStage3Defaults(),
      modal: {
        show: false,
        type: 'alert',
        title: '',
        message: '',
        onConfirm: null
      }
    };
  },
  computed: {
    activeStageId() {
      if (this.redisEnabled) {
        return 'redis-enabled';
      }

      if (this.stage1Completed) {
        return 'enable-redis';
      }

      return 'problem';
    },
    basePath() {
      return getBasePath();
    },
    contentActionHandlers() {
      return {
        markItemComplete: ({ args }) => this.completeContentItem(args.groupId, args.itemId),
        openEditor: () => this.$router.push('/editor'),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => window.open(this.redisInsightUrl, '_blank', 'noopener'),
        restartLab: () => this.restartLab(),
        resetProgress: () => this.resetProgress(),
        setStage: ({ args }) => this.handleStageChange(args.stageId)
      };
    },
    contentTokens() {
      return {
        sessionId: this.sessionInfo.sessionId
      };
    },
    contentWidgetProps() {
      return {
        'session-home.redis-session-comparison': {
          visible: Boolean(this.previousSessionId) && !this.stage3Tests.test3,
          showSuccess: true,
          previousSessionId: this.previousSessionId || '',
          currentSessionId: this.sessionInfo.sessionId,
          sessionIdChanged: this.sessionIdChanged,
          changedMessage: "Session ID changed - this shouldn't happen with Redis enabled.",
          unchangedMessage: 'Same Session ID! Redis preserved your session.',
          changedClass: 'warning',
          unchangedClass: 'success'
        },
        'session-home.session-comparison': {
          visible: Boolean(this.previousSessionId) && !this.stage1Tests.test3,
          showSuccess: !this.sessionIdChanged,
          currentLabel: 'New Session ID:',
          previousSessionId: this.previousSessionId || '',
          currentSessionId: this.sessionInfo.sessionId,
          sessionIdChanged: this.sessionIdChanged,
          changedMessage: 'Your session ID changed! This proves the old session was lost.',
          unchangedMessage: 'Session IDs match - try restarting the app and refreshing.',
          changedClass: '',
          unchangedClass: 'warning'
        }
      };
    },
    contentWidgets() {
      return {
        'session-home.redis-session-comparison': SessionComparisonWidget,
        'session-home.session-comparison': SessionComparisonWidget
      };
    },
    redisInsightUrl() {
      return getRedisInsightUrl();
    },
    resolvedHomeContent() {
      if (!this.homeContent) {
        return null;
      }

      return prepareSessionHomeContent(this.homeContent, this.stage1Tests, this.stage3Tests);
    },
    sessionIdChanged() {
      return Boolean(this.previousSessionId) && this.previousSessionId !== this.sessionInfo.sessionId;
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    this.loadTestProgress();
    await Promise.all([
      this.fetchSessionInfo(),
      this.loadHomeContent()
    ]);
  },
  methods: {
    completeContentItem(groupId, itemId) {
      if (itemId === 'test1') {
        this.saveSessionId();
      }

      if (groupId === 'stage1-tests') {
        this.stage1Tests = {
          ...this.stage1Tests,
          [itemId]: true
        };
      }

      if (groupId === 'stage3-tests') {
        this.stage3Tests = {
          ...this.stage3Tests,
          [itemId]: true
        };
      }

      this.saveTestProgress();
    },
    async fetchSessionInfo() {
      try {
        const response = await fetch(getApiUrl('/api/session-info'), {
          credentials: 'include'
        });
        if (response.status === 401 || response.status === 403) {
          window.location.href = `${this.basePath}/login`;
          return;
        }
        if (response.ok) {
          const data = await response.json();
          this.sessionInfo = {
            sessionId: data.sessionId,
            storageType: data.sessionStorage,
            username: data.username,
            createdAt: new Date(data.creationTime).toLocaleString()
          };
          this.redisEnabled = data.redisEnabled;
          this.stage1Completed = data.stage1Completed;
        }
      } catch (error) {
        console.error('Error fetching session info:', error);
      }
    },
    async handleLogout() {
      try {
        await fetch(`${this.basePath}/logout`, {
          method: 'POST',
          credentials: 'include'
        });
      } catch (error) {
        console.error('Error during logout:', error);
      } finally {
        window.location.href = `${this.basePath}/login?logout`;
      }
    },
    handleContentRenderError(issues) {
      console.warn('Session home content render issues:', issues);
    },
    handleModalCancel() {
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    handleModalConfirm() {
      if (this.modal.onConfirm) {
        this.modal.onConfirm();
      }
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    async handleStageChange(stageId) {
      if (stageId === 'enable-redis') {
        await this.markStage1Complete();
      }
    },
    loadTestProgress() {
      this.stage1Tests = loadSavedProgress('stage1Tests', createStage1Defaults());
      this.stage3Tests = loadSavedProgress('stage3Tests', createStage3Defaults());
      this.previousSessionId = localStorage.getItem('previousSessionId');
    },
    async loadHomeContent() {
      try {
        this.homeContent = await loadWorkshopContentView('session-home');
      } catch (error) {
        console.error('Error loading session workshop content:', error);
        this.contentError = 'Unable to load workshop instructions. Refresh the page and try again.';
      }
    },
    async markStage1Complete() {
      try {
        const response = await fetch(getApiUrl('/api/mark-stage1-complete'), {
          method: 'POST',
          credentials: 'include'
        });
        if (response.ok) {
          this.stage1Completed = true;
        }
      } catch (error) {
        console.error('Error marking stage 1 complete:', error);
      }
    },
    refreshSession() {
      this.fetchSessionInfo();
    },
    resetProgress() {
      this.showModal('confirm', 'Reset Progress', 'Reset all test progress? You will start from the first test again.', () => {
        this.stage1Tests = createStage1Defaults();
        this.stage3Tests = createStage3Defaults();
        this.previousSessionId = null;
        localStorage.removeItem('stage1Tests');
        localStorage.removeItem('stage3Tests');
        localStorage.removeItem('previousSessionId');
      });
    },
    async restartLab() {
      this.showModal('confirm', 'Restart Lab', 'Are you sure you want to restart the lab? This will restore all files to their original state and reset your progress. You will need to restart the workshop backend after this.', async () => {
        this.restartingLab = true;
        try {
          const response = await fetch(getApiUrl('/api/editor/restore'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
          });
          const data = await response.json();

          if (data.success) {
            this.stage1Tests = createStage1Defaults();
            this.stage3Tests = createStage3Defaults();
            this.previousSessionId = null;
            localStorage.removeItem('stage1Tests');
            localStorage.removeItem('stage3Tests');
            localStorage.removeItem('previousSessionId');
            this.showModal('alert', 'Lab Reset', 'Lab reset! Please restart the workshop backend from the Workshop Hub.\n\nThen refresh this page to start from Stage 1.');
          } else {
            this.showModal('alert', 'Error', `Error: ${data.error || 'Failed to restore files'}`);
          }
        } catch (error) {
          console.error('Error restarting lab:', error);
          this.showModal('alert', 'Error', 'Failed to restore files. Please try again.');
        } finally {
          this.restartingLab = false;
        }
      });
    },
    saveSessionId() {
      localStorage.setItem('previousSessionId', this.sessionInfo.sessionId);
      this.previousSessionId = this.sessionInfo.sessionId;
    },
    saveTestProgress() {
      localStorage.setItem('stage1Tests', JSON.stringify(this.stage1Tests));
      localStorage.setItem('stage3Tests', JSON.stringify(this.stage3Tests));
    },
    showModal(type, title, message, onConfirm = null) {
      this.modal = {
        show: true,
        type,
        title,
        message,
        onConfirm
      };
    }
  }
};
</script>

<style scoped>
.session-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: var(--spacing-6);
}

.welcome-container {
  width: 100%;
  max-width: 1200px;
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: var(--spacing-6);
  align-items: start;
  padding-top: var(--spacing-6);
}

/* Left Column - Instructions */
.instructions-column {
  min-width: 0;
}

.instructions {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.btn-link {
  width: 100%;
  text-align: center;
  margin-bottom: var(--spacing-3);
}

.content-state {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
}

.instructions :deep(.content-renderer-header__stage) {
  color: var(--color-text);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.instructions :deep(.content-step-item),
.instructions :deep(.content-callout),
.instructions :deep(.content-widget) {
  background: var(--color-dark-800);
}

.instructions :deep(.content-step-item__heading h4),
.instructions :deep(.content-section__header h3) {
  color: var(--color-text);
}

.instructions :deep(.content-section__header .workshop-markdown),
.instructions :deep(.workshop-markdown),
.instructions :deep(.content-step-item__heading .workshop-markdown) {
  color: var(--color-text-secondary);
}

.instructions :deep(.content-action--primary) {
  background: #DC382C;
  border-color: #DC382C;
}

.instructions :deep(.content-action--secondary) {
  background: transparent;
  border-color: var(--color-border);
}

.instructions :deep(.session-comparison) {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  margin: var(--spacing-3) 0;
}

.instructions :deep(.session-comparison.success) {
  background: rgba(16, 185, 129, 0.1);
  border-color: #10b981;
}

.instructions :deep(.session-old),
.instructions :deep(.session-new) {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.instructions :deep(.session-comparison .label) {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  min-width: 120px;
}

.instructions :deep(.session-comparison code) {
  background: var(--color-surface);
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  word-break: break-all;
}

.instructions :deep(.comparison-note) {
  margin: var(--spacing-2) 0 0 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: #ef4444;
}

.instructions :deep(.comparison-note.success) {
  color: #10b981;
}

.instructions :deep(.comparison-note.warning) {
  color: #f59e0b;
}

/* Right Column - Details */
.details-column {
  position: sticky;
  top: var(--spacing-6);
}

.details-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
  padding: var(--spacing-3);
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
}

.info-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  color: var(--color-text);
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.info-value.redis-enabled {
  color: #10b981;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.footer-info {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  text-align: center;
}

.footer-info p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.5;
}

.footer-info strong {
  color: #DC382C;
}

/* Responsive */
@media (max-width: 900px) {
  .welcome-container {
    grid-template-columns: 1fr;
  }

  .details-column {
    order: -1;
    position: static;
  }

  .actions {
    flex-direction: row;
  }

  .actions .btn {
    flex: 1;
  }
}
</style>
