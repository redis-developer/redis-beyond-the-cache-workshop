<template>
  <div class="session-home">
    <WorkshopShell
      title="Distributed Session Management with Redis"
      eyebrow="Session Management"
      summary="Follow the instructions on the left while the learner application runs in the stable app frame."
      :runtime="shellRuntime"
      :links="shellLinks"
      :actions="shellActions"
      :learner-app="learnerApp"
      :runtime-actions-disabled="runtimeBusy"
      @runtime-action="handleRuntimeAction"
    >
      <template #hero-right>
        <div class="session-shell-card">
          <span class="session-shell-card__label">Current session</span>
          <code>{{ sessionInfo.sessionId }}</code>
          <span class="session-shell-card__storage" :class="{ 'session-shell-card__storage--redis': redisEnabled }">
            {{ sessionInfo.storageType }}
          </span>
        </div>
      </template>

      <template #instructions>
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
          :context="contentContext"
          :active-stage-id="activeStageId"
          :action-handlers="contentActionHandlers"
          :widgets="contentWidgets"
          :widget-props="contentWidgetProps"
          :show-title="false"
          :show-summary="false"
          @render-error="handleContentRenderError"
        />
      </template>
    </WorkshopShell>

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
import {
  getApiUrl,
  getBasePath,
  getRedisInsightUrl,
  getWorkshopHubUrl
} from '../utils/basePath';
import {
  WorkshopContentRenderer,
  WorkshopModal,
  WorkshopShell
} from '../utils/components';
import SessionComparisonWidget from '../components/content/SessionComparisonWidget.vue';
import { loadWorkshopContentView } from '../utils/workshopContent';

const READY_STATES = new Set(['CHILD_READY', 'READY', 'ready', 'running']);
const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 180000;

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

function loadSavedProgress(storageKey, legacyKey, defaults) {
  const saved = localStorage.getItem(storageKey) || localStorage.getItem(legacyKey);
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
    WorkshopShell
  },
  data() {
    return {
      homeContent: null,
      contentError: '',
      sessionInfo: {
        sessionId: 'Sign in inside the app',
        storageType: 'Unknown',
        username: '',
        createdAt: ''
      },
      redisEnabled: false,
      stage1Completed: false,
      manualStageId: '',
      previousSessionId: null,
      stage1Tests: createStage1Defaults(),
      stage3Tests: createStage3Defaults(),
      runtimeState: 'READY',
      frontendState: 'READY',
      backendState: 'READY',
      runtimeBusy: false,
      runtimeStatusMessage: '',
      appFrameVersion: 0,
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
    automaticStageId() {
      if (this.redisEnabled) {
        return 'redis-enabled';
      }

      if (this.stage1Completed) {
        return 'enable-redis';
      }

      return 'problem';
    },
    activeStageId() {
      return this.manualStageId || this.automaticStageId;
    },
    appFrameMessage() {
      return this.runtimeBusy
        ? 'The learner runtime is restarting or rebuilding. The shell remains available while the app comes back.'
        : '';
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
        rebuildRuntime: () => this.restartRuntime(true),
        resetProgress: () => this.resetProgress(),
        restartRuntime: () => this.restartRuntime(false),
        setStage: ({ args }) => this.handleStageChange(args.stageId)
      };
    },
    contentContext() {
      return {
        session: {
          id: this.sessionInfo.sessionId,
          previousId: this.previousSessionId || '',
          changed: this.sessionIdChanged
        },
        links: {
          hub: this.workshopHubUrl,
          learnerApp: this.learnerAppUrl,
          redisInsight: this.redisInsightUrl,
          workshopHub: this.workshopHubUrl
        },
        runtime: {
          state: this.runtimeState,
          status: this.runtimeStatusMessage || this.runtimeState,
          backendStatus: this.backendState,
          frontendStatus: this.frontendState
        }
      };
    },
    contentTokens() {
      return {
        sessionId: this.sessionInfo.sessionId,
        previousSessionId: this.previousSessionId || '',
        sessionIdChanged: this.sessionIdChanged
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
    learnerApp() {
      return {
        title: 'Session management app',
        url: this.learnerAppUrl,
        message: this.appFrameMessage
      };
    },
    learnerAppUrl() {
      const route = this.buildRouteUrl('/app');
      const separator = route.includes('?') ? '&' : '?';
      return `${route}${separator}frame=${this.appFrameVersion}`;
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
    shellActions() {
      return ['restartRuntime', 'rebuildRuntime', 'openRedisInsight', 'openHub', 'refreshStatus'];
    },
    shellLinks() {
      return {
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightUrl,
        workshopHub: this.workshopHubUrl
      };
    },
    shellRuntime() {
      return {
        state: this.runtimeState,
        frontendState: this.frontendState,
        backendState: this.backendState,
        status: this.runtimeStatusMessage,
        rebuildAvailable: true
      };
    },
    storagePrefix() {
      return `session-management:${this.basePath || 'standalone'}`;
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    window.addEventListener('message', this.handleAppMessage);
    this.loadTestProgress();
    await Promise.all([
      this.fetchSessionInfo(),
      this.loadHomeContent(),
      this.refreshRuntimeState()
    ]);
  },
  beforeUnmount() {
    window.removeEventListener('message', this.handleAppMessage);
  },
  methods: {
    buildRouteUrl(route) {
      const normalizedRoute = route.startsWith('/') ? route : `/${route}`;
      if (!this.basePath || this.basePath === '/') {
        return normalizedRoute;
      }
      return `${this.basePath}${normalizedRoute}`;
    },
    buildRunnerUrl(endpointPath) {
      return this.buildRouteUrl(endpointPath);
    },
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
          this.sessionInfo = {
            sessionId: 'Sign in inside the app',
            storageType: 'Unauthenticated',
            username: '',
            createdAt: ''
          };
          this.redisEnabled = false;
          return;
        }

        if (response.ok) {
          const data = await response.json();
          this.applySessionInfo(data);
        }
      } catch (error) {
        console.error('Error fetching session info:', error);
      }
    },
    applySessionInfo(data) {
      this.sessionInfo = {
        sessionId: data.sessionId,
        storageType: data.sessionStorage,
        username: data.username,
        createdAt: new Date(data.creationTime).toLocaleString()
      };
      this.redisEnabled = data.redisEnabled;
      this.stage1Completed = data.stage1Completed;
    },
    handleAppMessage(event) {
      if (event.origin !== window.location.origin || !event.data?.type) {
        return;
      }

      if (event.data.type === 'session-app-authenticated') {
        if (event.data.payload?.sessionId) {
          this.fetchSessionInfo();
        }
        return;
      }

      if (event.data.type === 'session-app-unauthenticated') {
        this.fetchSessionInfo();
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
    handleRuntimeAction(action) {
      if (action.type === 'restart') {
        this.restartRuntime(false);
      }

      if (action.type === 'rebuild') {
        this.restartRuntime(true);
      }

      if (action.type === 'refresh') {
        this.refreshRuntimeState();
        this.fetchSessionInfo();
        this.appFrameVersion += 1;
      }
    },
    async handleStageChange(stageId) {
      if (stageId === 'enable-redis') {
        await this.markStage1Complete();
      }

      this.manualStageId = stageId;
    },
    loadTestProgress() {
      this.stage1Tests = loadSavedProgress(
        this.storageKey('stage1Tests'),
        'stage1Tests',
        createStage1Defaults()
      );
      this.stage3Tests = loadSavedProgress(
        this.storageKey('stage3Tests'),
        'stage3Tests',
        createStage3Defaults()
      );
      this.previousSessionId = localStorage.getItem(this.storageKey('previousSessionId'))
        || localStorage.getItem('previousSessionId');
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
    async refreshRuntimeState() {
      const state = await this.fetchSessionState();
      if (state) {
        this.runtimeState = state;
        this.backendState = state;
        this.frontendState = 'READY';
      }
    },
    async restartRuntime(rebuild) {
      if (this.runtimeBusy) {
        return;
      }

      this.runtimeBusy = true;
      this.runtimeState = rebuild ? 'REBUILDING' : 'RESTARTING';
      this.backendState = this.runtimeState;
      this.runtimeStatusMessage = rebuild ? 'Rebuilding learner runtime...' : 'Restarting learner runtime...';

      try {
        await this.sendRestartRequest(rebuild);
        await this.waitForReadyState();
        this.runtimeState = 'READY';
        this.backendState = 'READY';
        this.frontendState = 'READY';
        this.runtimeStatusMessage = 'Runtime is ready';
        this.appFrameVersion += 1;
        await this.fetchSessionInfo();
      } catch (error) {
        this.runtimeState = 'FAILED';
        this.backendState = 'FAILED';
        this.runtimeStatusMessage = error?.message || 'Restart failed';
      } finally {
        this.runtimeBusy = false;
      }
    },
    async sendRestartRequest(rebuild) {
      const response = await fetch(this.buildRunnerUrl('/internal/session-runner/restart'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ rebuild })
      });

      if (!response.ok) {
        throw new Error(await this.readErrorMessage(response, 'Failed to restart session'));
      }
    },
    async waitForReadyState() {
      const startedAt = Date.now();

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const state = await this.fetchSessionState();

        if (READY_STATES.has(state)) {
          return;
        }

        await this.sleep(POLL_INTERVAL_MS);
      }

      throw new Error('Timed out waiting for the session to restart');
    },
    async fetchSessionState() {
      try {
        const response = await fetch(this.buildRunnerUrl('/internal/session-runner/status'), {
          credentials: 'include'
        });

        if (!response.ok) {
          return null;
        }

        const data = await response.json();
        return data?.state || null;
      } catch {
        return null;
      }
    },
    resetProgress() {
      this.showModal('confirm', 'Reset Progress', 'Reset all test progress? You will start from the first test again.', () => {
        this.stage1Tests = createStage1Defaults();
        this.stage3Tests = createStage3Defaults();
        this.manualStageId = '';
        this.previousSessionId = null;
        localStorage.removeItem(this.storageKey('stage1Tests'));
        localStorage.removeItem(this.storageKey('stage3Tests'));
        localStorage.removeItem(this.storageKey('previousSessionId'));
        localStorage.removeItem('stage1Tests');
        localStorage.removeItem('stage3Tests');
        localStorage.removeItem('previousSessionId');
      });
    },
    saveSessionId() {
      localStorage.setItem(this.storageKey('previousSessionId'), this.sessionInfo.sessionId);
      this.previousSessionId = this.sessionInfo.sessionId;
    },
    saveTestProgress() {
      localStorage.setItem(this.storageKey('stage1Tests'), JSON.stringify(this.stage1Tests));
      localStorage.setItem(this.storageKey('stage3Tests'), JSON.stringify(this.stage3Tests));
    },
    storageKey(name) {
      return `${this.storagePrefix}:${name}`;
    },
    showModal(type, title, message, onConfirm = null) {
      this.modal = {
        show: true,
        type,
        title,
        message,
        onConfirm
      };
    },
    async readErrorMessage(response, fallbackMessage) {
      const text = await response.text();

      if (!text) {
        return fallbackMessage;
      }

      try {
        const data = JSON.parse(text);
        if (data?.message) {
          return `${fallbackMessage}: ${data.message}`;
        }
        if (data?.detail) {
          return `${fallbackMessage}: ${data.detail}`;
        }
        if (data?.error) {
          return `${fallbackMessage}: ${data.error}`;
        }
      } catch {
        return `${fallbackMessage}: ${text}`;
      }

      return fallbackMessage;
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }
  }
};
</script>

<style scoped>
.session-home {
  min-height: 100vh;
}

.session-shell-card {
  display: flex;
  min-width: min(100%, 18rem);
  flex-direction: column;
  gap: var(--spacing-2);
  padding: var(--spacing-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(16, 26, 33, 0.88);
}

.session-shell-card__label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.session-shell-card code {
  color: var(--color-text);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.session-shell-card__storage {
  align-self: flex-start;
  padding: 0.2rem 0.55rem;
  border: 1px solid rgba(251, 191, 36, 0.4);
  border-radius: 999px;
  color: var(--color-warning);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.session-shell-card__storage--redis {
  border-color: rgba(74, 222, 128, 0.4);
  color: var(--color-success);
}

.content-state {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
}

:deep(.content-renderer-header__stage) {
  color: var(--color-text);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

:deep(.content-step-item),
:deep(.content-widget) {
  background: var(--color-dark-800);
}

:deep(.content-step-item__heading h4),
:deep(.content-section__header h3) {
  color: var(--color-text);
}

:deep(.content-section__header .workshop-markdown),
:deep(.workshop-markdown),
:deep(.content-step-item__heading .workshop-markdown) {
  color: var(--color-text-secondary);
}

:deep(.session-comparison) {
  margin: var(--spacing-4) 0 0 calc(2rem + var(--spacing-3));
  padding: var(--spacing-3);
  border: 1px solid #ef4444;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.1);
}

:deep(.session-comparison.success) {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

:deep(.session-old),
:deep(.session-new) {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

:deep(.session-comparison .label) {
  min-width: 120px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

:deep(.session-comparison code) {
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-size: var(--font-size-xs);
  word-break: break-all;
}

:deep(.comparison-note) {
  margin: var(--spacing-2) 0 0;
  color: #ef4444;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

:deep(.comparison-note.success) {
  color: #10b981;
}

:deep(.comparison-note.warning) {
  color: #f59e0b;
}

@media (max-width: 768px) {
  :deep(.session-comparison) {
    margin-left: 0;
  }
}
</style>
