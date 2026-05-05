<template>
  <div class="session-home">
    <WorkshopShell
      title="Distributed Session Management with Redis"
      eyebrow=""
      summary=""
      :runtime="shellRuntime"
      :links="shellLinks"
      :actions="shellActions"
      :learner-app="learnerApp"
      :runtime-logs="runtimeLogs"
      :runtime-actions-disabled="runtimeInteractionBusy"
      @runtime-action="handleRuntimeAction"
      @logs-toggle="handleRuntimeLogsToggle"
    >
      <template #instructions>
        <div class="session-instructions">
          <header class="session-instructions__header">
            <p class="session-instructions__title">{{ instructionsHeaderTitle }}</p>
            <button
              type="button"
              class="session-instructions__action"
              @click="resetProgress"
            >
              Reset Instructions
            </button>
          </header>

          <div class="session-instructions__body">
            <div v-if="contentError" class="content-state content-state--error">
              {{ contentError }}
            </div>
            <div v-else-if="!homeContent" class="content-state">
              Loading workshop instructions...
            </div>
            <template v-else>
              <WorkshopContentRenderer
                :content="resolvedHomeContent"
                :tokens="contentTokens"
                :context="contentContext"
                :action-handlers="contentActionHandlers"
                :widgets="contentWidgets"
                :widget-props="contentWidgetProps"
                :show-title="false"
                :show-summary="false"
                @render-error="handleContentRenderError"
              />

              <nav
                v-if="hasStageNavigation"
                class="stage-navigation"
                aria-label="Stage navigation"
              >
                <button
                  v-if="previousStage"
                  class="stage-navigation__button stage-navigation__button--previous"
                  type="button"
                  :aria-label="`Go to ${previousStageAriaLabel}`"
                  @click="goToPage(previousStage)"
                >
                  <span aria-hidden="true">&larr;</span>
                  <span class="stage-navigation__label">{{ previousStageLabel }}</span>
                </button>

                <button
                  v-if="nextStage"
                  class="stage-navigation__button stage-navigation__button--next"
                  type="button"
                  :aria-label="`Go to ${nextStageAriaLabel}`"
                  @click="goToPage(nextStage)"
                >
                  <span class="stage-navigation__label">{{ nextStageLabel }}</span>
                  <span aria-hidden="true">&rarr;</span>
                </button>
              </nav>
            </template>
          </div>
        </div>
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
  getCodeEditorUrl,
  getWorkshopHubUrl
} from '../utils/basePath';
import {
  WorkshopContentRenderer,
  WorkshopModal,
  WorkshopShell
} from '../utils/components';
import SessionComparisonWidget from '../components/content/SessionComparisonWidget.vue';
import { loadWorkshopContentView } from '../utils/workshopContent';

const READY_STATES = new Set(['CHILD_READY', 'READY', 'RUNNING']);
const FAILED_STATES = new Set(['CHILD_FAILED', 'DISABLED', 'FAILED']);
const BUSY_STATES = new Set(['CHILD_STARTING', 'REBUILDING', 'RESTARTING', 'STARTING']);
const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 480000;
const MIN_RUNTIME_BUSY_MS = 2000;

function normalizeRunnerState(state) {
  return String(state || '').trim().toUpperCase();
}

function isBusyRunnerState(state) {
  return BUSY_STATES.has(normalizeRunnerState(state));
}

function isRebuildRunnerState(state) {
  return normalizeRunnerState(state) === 'REBUILDING';
}

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

function updateContentStepList(content, sectionId, listId, progress) {
  content.sections = content.sections.map(section => {
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

function prepareSessionHomeContent(content, pageId, stage1Tests, stage3Tests) {
  const nextContent = cloneContent(content);

  if (pageId === '1') {
    updateContentStepList(nextContent, 'stage-1', 'stage1-tests', stage1Tests);
  }

  if (pageId === '3') {
    updateContentStepList(nextContent, 'stage-3-tests', 'stage3-tests', stage3Tests);
    if (!stage3Tests.test4) {
      nextContent.sections = nextContent.sections.filter(section => section.sectionId !== 'stage-3-completion');
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
  props: {
    pageId: { type: String, default: '0' }
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
      previousSessionId: null,
      stage1Tests: createStage1Defaults(),
      stage3Tests: createStage3Defaults(),
      runtimeState: 'READY',
      frontendState: 'READY',
      backendState: 'READY',
      runtimeBusy: false,
      runtimeMonitorActive: false,
      runtimeStatusLoaded: false,
      runtimeStatusMessage: '',
      runtimeLogs: [],
      appFrameVersion: 0,
      navigationPageTitles: {},
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
    activePageIndex() {
      return this.pageOrder.findIndex(page => page === this.pageId);
    },
    appFrameMessage() {
      if (!this.runtimeStatusLoaded) {
        return 'Checking learner app status before loading the frame.';
      }

      if (!this.runtimeInteractionBusy) {
        return '';
      }

      return this.runtimeDisplayState === 'REBUILDING'
        ? 'The learner app is rebuilding. The frame will reload automatically when it is ready.'
        : 'The learner app is restarting. The frame will reload automatically when it is ready.';
    },
    basePath() {
      return getBasePath();
    },
    contentActionHandlers() {
      return {
        markItemComplete: ({ args }) => this.completeContentItem(args.groupId, args.itemId),
        openEditor: () => this.openCodeEditorPanel(),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => this.openRedisInsightPanel(),
        openRoute: ({ args }) => this.openRoute(args.route),
        rebuildRuntime: () => this.restartRuntime(true),
        resetProgress: () => this.resetProgress(),
        restartRuntime: () => this.restartRuntime(false),
        setStage: ({ args }) => this.openStage(args.stageId)
      };
    },
    hasStageNavigation() {
      return this.pageOrder.length > 1 && this.activePageIndex >= 0;
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
          redisInsight: this.redisInsightFrameUrl,
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
        '3.redis-session-comparison': {
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
        '1.session-comparison': {
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
        '3.redis-session-comparison': SessionComparisonWidget,
        '1.session-comparison': SessionComparisonWidget
      };
    },
    learnerApp() {
      return {
        title: 'Learner app',
        url: this.learnerAppUrl,
        message: this.appFrameMessage
      };
    },
    learnerAppUrl() {
      const route = this.buildRouteUrl('/app/');
      const separator = route.includes('?') ? '&' : '?';
      return `${route}${separator}frame=${this.appFrameVersion}`;
    },
    runtimeDisplayState() {
      if (!this.runtimeStatusLoaded) {
        return 'STARTING';
      }

      return isRebuildRunnerState(this.runtimeState) ? 'REBUILDING' : 'RESTARTING';
    },
    runtimeInteractionBusy() {
      return !this.runtimeStatusLoaded
        || this.runtimeBusy
        || isBusyRunnerState(this.runtimeState)
        || isBusyRunnerState(this.backendState);
    },
    instructionsHeaderTitle() {
      const title = this.homeContent?.title
        || this.navigationPageTitles[this.pageId]
        || `Stage ${this.pageId}`;

      return this.formatInstructionsTitle(title);
    },
    redisInsightFrameUrl() {
      const normalizedBasePath = this.basePath && this.basePath !== '/' ? this.basePath : '';
      return `${window.location.origin}${normalizedBasePath}/redis-insight/`;
    },
    resolvedHomeContent() {
      if (!this.homeContent) {
        return null;
      }

      return prepareSessionHomeContent(this.homeContent, this.pageId, this.stage1Tests, this.stage3Tests);
    },
    nextStage() {
      if (!this.hasStageNavigation) {
        return null;
      }

      return this.pageOrder[this.activePageIndex + 1] || null;
    },
    nextStageLabel() {
      return this.stageNavigationLabel('nextLabel', this.nextStage);
    },
    nextStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.nextStageLabel, this.nextStage);
    },
    previousStage() {
      if (!this.hasStageNavigation) {
        return null;
      }

      return this.pageOrder[this.activePageIndex - 1] || null;
    },
    previousStageLabel() {
      return this.stageNavigationLabel('previousLabel', this.previousStage);
    },
    previousStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.previousStageLabel, this.previousStage);
    },
    sessionIdChanged() {
      return Boolean(this.previousSessionId) && this.previousSessionId !== this.sessionInfo.sessionId;
    },
    shellActions() {
      return ['restartRuntime', 'rebuildRuntime', 'openRedisInsight', 'openEditor', 'openHub', 'refreshStatus'];
    },
    shellLinks() {
      return {
        editor: getCodeEditorUrl(),
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightFrameUrl,
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
    pageOrder() {
      return this.redisEnabled ? ['0', '1', '2', '3'] : ['0', '1', '2'];
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
    await this.loadNavigationPageTitles();
    this.redirectIfPageUnavailable();
  },
  watch: {
    async pageId() {
      this.contentError = '';
      this.homeContent = null;
      await Promise.all([
        this.fetchSessionInfo(),
        this.loadHomeContent()
      ]);
      await this.loadNavigationPageTitles();
      this.redirectIfPageUnavailable();
    },
    async redisEnabled() {
      await this.loadNavigationPageTitles();
      this.redirectIfPageUnavailable();
    },
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
      this.redirectIfPageUnavailable();
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
        this.refreshLearnerAppFrame();
      }

      if (action.type === 'recompile-success') {
        this.refreshLearnerAppFrame();
      }
    },
    formatInstructionsTitle(title) {
      const match = title.match(/^STAGE\s+(\d+):\s*(.+)$/i);
      if (!match) {
        return title;
      }

      return `#${match[1]}: ${match[2]}`;
    },
    async openStage(stageId) {
      const routes = {
        intro: '/0',
        problem: '/1',
        'enable-redis': '/2',
        'redis-enabled': '/3'
      };

      await this.openRoute(routes[stageId] || '/0');
    },
    openRedisInsightPanel() {
      this.replaceToolQuery('redis-insight');
    },
    openCodeEditorPanel() {
      this.replaceToolQuery('code-editor');
    },
    refreshLearnerAppFrame() {
      this.showLearnerAppPanel();
      this.refreshRuntimeState();
      this.fetchSessionInfo();
      this.appFrameVersion += 1;
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
        this.homeContent = await loadWorkshopContentView(this.pageId);
        this.storeNavigationPageTitle(this.pageId, this.homeContent);
      } catch (error) {
        console.error('Error loading session workshop content:', error);
        this.contentError = 'Unable to load workshop instructions. Refresh the page and try again.';
      }
    },
    async loadNavigationPageTitles() {
      const missingPageIds = this.pageOrder.filter(pageId => !this.navigationPageTitles[pageId]);
      if (missingPageIds.length === 0) {
        return;
      }

      const titles = {};

      await Promise.all(missingPageIds.map(async pageId => {
        try {
          const content = pageId === this.pageId && this.homeContent
            ? this.homeContent
            : await loadWorkshopContentView(pageId);

          if (content?.title) {
            titles[pageId] = content.title;
          }
        } catch (error) {
          console.warn(`Unable to load navigation title for page ${pageId}`, error);
        }
      }));

      if (Object.keys(titles).length > 0) {
        this.navigationPageTitles = {
          ...this.navigationPageTitles,
          ...titles
        };
      }
    },
    stageNavigationAriaLabel(label, pageId) {
      return label || this.stageNavigationFallbackLabel(pageId);
    },
    stageNavigationFallbackLabel(pageId) {
      if (!pageId) {
        return '';
      }

      return this.navigationPageTitles[pageId] || `Stage ${pageId}`;
    },
    stageNavigationLabel(labelKey, pageId) {
      if (!pageId) {
        return '';
      }

      if (
        this.homeContent?.navigation
        && Object.prototype.hasOwnProperty.call(this.homeContent.navigation, labelKey)
      ) {
        return this.homeContent.navigation[labelKey] || '';
      }

      return this.stageNavigationFallbackLabel(pageId);
    },
    storeNavigationPageTitle(pageId, content) {
      if (!pageId || !content?.title) {
        return;
      }

      this.navigationPageTitles = {
        ...this.navigationPageTitles,
        [pageId]: content.title
      };
    },
    async goToPage(pageId) {
      if (!pageId) {
        return;
      }

      await this.openRoute(`/${pageId}`);
    },
    async openRoute(route) {
      if (!route) {
        return;
      }

      if (route === '/2') {
        await this.markStage1Complete();
      }

      await this.$router.push(route);
    },
    showLearnerAppPanel() {
      this.replaceToolQuery(null);
    },
    replaceToolQuery(tool) {
      const nextQuery = { ...this.$route.query };
      if (tool) {
        nextQuery.tool = tool;
      } else {
        delete nextQuery.tool;
      }

      if (nextQuery.tool === this.$route.query.tool) {
        return;
      }

      this.$router.replace({
        path: this.$route.path,
        query: nextQuery
      });
    },
    redirectIfPageUnavailable() {
      if (this.pageOrder.includes(this.pageId)) {
        return;
      }

      this.$router.replace('/2');
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
      try {
        const status = await this.fetchSessionStatus();
        const state = status?.state;
        if (state) {
          this.applyRuntimeStatus(status);
          if (isBusyRunnerState(state)) {
            this.monitorRuntimeUntilReady();
          }
        }
      } finally {
        this.runtimeStatusLoaded = true;
      }
    },
    applyRuntimeStatus(status) {
      const state = status?.state;
      this.applyRuntimeLogs(status);

      if (!state) {
        return;
      }

      this.runtimeState = normalizeRunnerState(state);
      this.backendState = this.runtimeState;
      this.frontendState = 'READY';
      if (isBusyRunnerState(this.runtimeState)) {
        this.runtimeStatusMessage = isRebuildRunnerState(this.runtimeState)
          ? 'Rebuilding learner runtime...'
          : 'Restarting learner runtime...';
      }
    },
    async restartRuntime(rebuild) {
      if (this.runtimeInteractionBusy) {
        return;
      }

      this.runtimeBusy = true;
      this.runtimeState = rebuild ? 'REBUILDING' : 'RESTARTING';
      this.backendState = this.runtimeState;
      this.runtimeStatusMessage = rebuild ? 'Rebuilding learner runtime...' : 'Restarting learner runtime...';
      const visibleStateStartedAt = Date.now();

      try {
        await this.$nextTick();
        await this.sendRestartRequest(rebuild);
        await this.waitForReadyState();
        await this.waitForMinimumBusyState(visibleStateStartedAt);
        this.runtimeState = 'READY';
        this.backendState = 'READY';
        this.frontendState = 'READY';
        this.runtimeStatusMessage = 'Runtime is ready';
        this.appFrameVersion += 1;
        await this.fetchSessionInfo();
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt);
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
        body: JSON.stringify({ rebuild, async: true })
      });

      if (!response.ok) {
        throw new Error(await this.readErrorMessage(response, 'Failed to restart session'));
      }
    },
    async waitForReadyState() {
      const startedAt = Date.now();

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const status = await this.fetchSessionStatus();
        const state = normalizeRunnerState(status?.state);

        if (READY_STATES.has(state)) {
          return;
        }
        if (FAILED_STATES.has(state)) {
          throw new Error(status?.lastError || 'Session restart failed');
        }

        await this.sleep(POLL_INTERVAL_MS);
      }

      throw new Error('Timed out waiting for the session to restart');
    },
    async monitorRuntimeUntilReady() {
      if (this.runtimeMonitorActive) {
        return;
      }

      this.runtimeMonitorActive = true;
      this.runtimeBusy = true;
      const visibleStateStartedAt = Date.now();

      try {
        await this.waitForReadyState();
        await this.waitForMinimumBusyState(visibleStateStartedAt);
        this.runtimeState = 'READY';
        this.backendState = 'READY';
        this.frontendState = 'READY';
        this.runtimeStatusMessage = 'Runtime is ready';
        this.appFrameVersion += 1;
        await this.fetchSessionInfo();
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt);
        this.runtimeState = 'FAILED';
        this.backendState = 'FAILED';
        this.runtimeStatusMessage = error?.message || 'Restart failed';
      } finally {
        this.runtimeBusy = false;
        this.runtimeMonitorActive = false;
      }
    },
    async waitForMinimumBusyState(startedAt) {
      const remainingMs = MIN_RUNTIME_BUSY_MS - (Date.now() - startedAt);
      if (remainingMs > 0) {
        await this.sleep(remainingMs);
      }
    },
    async fetchSessionState() {
      const status = await this.fetchSessionStatus();
      return status?.state || null;
    },
    async fetchSessionStatus() {
      try {
        const response = await fetch(this.buildRunnerUrl('/internal/session-runner/status'), {
          credentials: 'include'
        });

        if (!response.ok) {
          return null;
        }

        const status = await response.json();
        this.applyRuntimeLogs(status);
        return status;
      } catch {
        return null;
      }
    },
    applyRuntimeLogs(status) {
      if (!Array.isArray(status?.recentLogs)) {
        return;
      }

      this.runtimeLogs = status.recentLogs.slice(-80);
    },
    async handleRuntimeLogsToggle(open) {
      if (open) {
        await this.refreshRuntimeState();
      }
    },
    resetProgress() {
      this.showModal('confirm', 'Reset Instructions', 'Reset all instruction progress? You will start from the first test again.', () => {
        this.stage1Tests = createStage1Defaults();
        this.stage3Tests = createStage3Defaults();
        this.previousSessionId = null;
        localStorage.removeItem(this.storageKey('stage1Tests'));
        localStorage.removeItem(this.storageKey('stage3Tests'));
        localStorage.removeItem(this.storageKey('previousSessionId'));
        localStorage.removeItem('stage1Tests');
        localStorage.removeItem('stage3Tests');
        localStorage.removeItem('previousSessionId');
        this.$router.push('/0');
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

:deep(.workshop-shell__instructions) {
  display: flex;
  padding: 0;
  overflow: hidden;
}

.session-instructions {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.session-instructions__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  min-height: 2.75rem;
  padding: var(--spacing-2) var(--spacing-3);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.session-instructions__title {
  min-width: 0;
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.2;
}

.session-instructions__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-height: 1.85rem;
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.session-instructions__action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.session-instructions__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: auto;
  padding: var(--spacing-4);
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

:deep(.workshop-markdown h2),
:deep(.workshop-markdown h3),
:deep(.workshop-markdown h4) {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
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

.stage-navigation {
  position: sticky;
  bottom: 0;
  z-index: 1;
  display: flex;
  gap: var(--spacing-3);
  justify-content: space-between;
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-3);
  background: rgba(13, 26, 34, 0.96);
  border-top: 1px solid var(--color-border);
}

.stage-navigation__button {
  align-items: center;
  display: inline-flex;
  gap: var(--spacing-2);
  min-height: 2.5rem;
  padding: 0.65rem 0.95rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.stage-navigation__button:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.stage-navigation__button--next {
  margin-left: auto;
}

.stage-navigation__button--previous {
  margin-right: auto;
}

.stage-navigation__label {
  line-height: 1.25;
  text-align: left;
}

.stage-navigation__button--next .stage-navigation__label {
  text-align: right;
}

@media (max-width: 768px) {
  :deep(.session-comparison) {
    margin-left: 0;
  }

  .session-instructions__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .stage-navigation {
    flex-wrap: wrap;
  }

  .stage-navigation__button {
    flex: 1 1 auto;
    justify-content: center;
  }
}
</style>
