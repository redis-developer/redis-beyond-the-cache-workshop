<template>
  <div class="patterns-home">
    <WorkshopShell
      title="Module 3: Redis Agent Memory"
      eyebrow=""
      summary=""
      :runtime="shellRuntime"
      :links="shellLinks"
      :actions="shellActions"
      :learner-app="learnerApp"
      :runtime-logs="runtimeLogs"
      :runtime-actions-disabled="runtimeInteractionBusy"
      :default-side-panel="defaultSidePanel"
      @runtime-action="handleRuntimeAction"
      @logs-toggle="handleRuntimeLogsToggle"
    >
      <template #instructions>
        <div class="patterns-instructions">
          <header class="patterns-instructions__header">
            <p class="patterns-instructions__title">{{ instructionsHeaderTitle }}</p>
          </header>

          <div class="patterns-instructions__body">
            <p
              v-if="editorStepStatusMessage"
              class="editor-step-status"
              :class="editorStepStatusClass"
              aria-live="polite"
            >
              {{ editorStepStatusMessage }}
            </p>
            <div v-if="contentError" class="content-state content-state--error">
              {{ contentError }}
            </div>
            <div v-else-if="!content" class="content-state">
              Loading workshop instructions...
            </div>
            <template v-else>
              <WorkshopContentRenderer
                :content="content"
                :context="contentContext"
                :show-title="false"
                :show-summary="false"
                :show-stage-title="false"
                @action="handleContentAction"
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
  </div>
</template>

<script>
import {
  getApiUrl,
  getBasePath,
  getCodeEditorFileUrl,
  getCodeEditorUrl,
  getWorkshopHubUrl,
  loadEditorWorkspaceMetadata,
  WorkshopContentRenderer,
  WorkshopShell
} from '../../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

const READY_STATES = new Set(['CHILD_READY', 'READY', 'RUNNING'])
const FAILED_STATES = new Set(['CHILD_FAILED', 'DISABLED', 'FAILED'])
const BUSY_STATES = new Set(['CHILD_STARTING', 'REBUILDING', 'RESTARTING', 'STARTING'])
const POLL_INTERVAL_MS = 2000
const POLL_TIMEOUT_MS = 480000
const MIN_RUNTIME_BUSY_MS = 2000

const PAGES = [
  { id: '0', title: 'Memory model' },
  { id: '1', title: 'Runtime wiring' },
  { id: '2', title: 'Load working memory' },
  { id: '3', title: 'Save working memory' },
  { id: '4', title: 'Long-term retrieval' },
  { id: '5', title: 'Verify memory' }
]

function normalizeRunnerState(state) {
  return String(state || '').trim().toUpperCase()
}

function isBusyRunnerState(state) {
  return BUSY_STATES.has(normalizeRunnerState(state))
}

function isRebuildRunnerState(state) {
  return normalizeRunnerState(state) === 'REBUILDING'
}

const EDITOR_STEP_CONFIG = {
  '2.loadWorkingMemory': {
    fileName: 'AmsChatMemoryRepository.java',
    line: 55,
    transform: content => content.replace(
      /    public List<Message> findByConversationId\(String conversationId\) \{\n        \/\/ Stage 2: replace this method body with the working-memory load block\.\n        return List\.of\(\);\n    \}/,
      `    public List<Message> findByConversationId(String conversationId) {
        WorkingMemoryResponse response = loadWorkingMemory(conversationId);
        if (response == null || response.getMessages() == null) {
            return List.of();
        }

        List<Message> messages = new ArrayList<>();
        for (MemoryMessage message : response.getMessages()) {
            Message springMessage = convertToSpringMessage(message);
            if (springMessage != null) {
                messages.add(springMessage);
            }
        }
        return messages;
    }`
    )
  },
  '3.saveWorkingMemory': {
    fileName: 'AmsChatMemoryRepository.java',
    line: 70,
    transform: content => content.replace(
      /    public void saveAll\(String conversationId, List<Message> messages\) \{\n        \/\/ Stage 3: replace this method body with the working-memory save block\.\n    \}/,
      `    public void saveAll(String conversationId, List<Message> messages) {
        String userId = parseUserId(conversationId);
        String sessionId = parseSessionId(conversationId);
        List<MemoryMessage> existingMessages = existingMessages(conversationId);
        List<MemoryMessage> newMessages = toNewMessages(messages, existingMessages);

        if (newMessages.isEmpty()) {
            return;
        }

        runSafely("save working memory for conversation " + conversationId, () -> {
            boolean firstMessage = existingMessages.isEmpty();
            agentMemoryService.appendMessagesToWorkingMemory(
                    sessionId,
                    newMessages,
                    userId,
                    MEMORY_MODEL
            );

            if (firstMessage) {
                applyTtl(sessionId, userId);
            }
        });
    }`
    )
  },
  '4.enableLongTermAdvisor': {
    fileName: 'LongTermMemoryAdvisor.java',
    line: 45,
    transform: content => content.replace(
      /    public ChatClientRequest before\(ChatClientRequest request, AdvisorChain advisorChain\) \{\n        \/\/ Stage 4: replace this method body with the long-term memory retrieval block\.\n        return request;\n    \}/,
      `    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        memoryRepository.setLastRetrievedMemories(List.of());

        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String userId = parseUserId(conversationId);

        if (userId == null || ANONYMOUS_USER.equals(userId)) {
            return request;
        }

        String userMessage = request.prompt().getUserMessage().getText();
        if (userMessage == null || userMessage.isBlank()) {
            return request;
        }

        if (!hasConversationHistory(conversationId)) {
            return request;
        }

        List<String> memories = searchMemories(userMessage, userId);
        memoryRepository.setLastRetrievedMemories(memories);

        if (memories.isEmpty()) {
            return request;
        }

        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(existingUserMessage ->
                        new UserMessage(augmentUserMessage(existingUserMessage.getText(), memories))))
                .context(RETRIEVED_MEMORIES, memories)
                .build();
    }`
    )
  }
}

export default {
  name: 'MultiAgentPatternsHome',
  components: {
    WorkshopContentRenderer,
    WorkshopShell
  },
  props: {
    pageId: { type: String, default: '0' }
  },
  data() {
    return {
      activeCodeEditorColumn: 1,
      activeCodeEditorFilePath: '',
      activeCodeEditorLine: 0,
      appFrameVersion: 0,
      codeEditorFilePathByName: {},
      codeEditorOpenRequest: 0,
      codeEditorWorkspaceRoot: '',
      content: null,
      contentError: '',
      editorStepStatusMessage: '',
      editorStepStatusTimer: null,
      editorStepStatusType: '',
      frontendState: 'READY',
      backendState: 'READY',
      navigationPageTitles: {},
      runtimeBusy: false,
      runtimeLogs: [],
      runtimeMonitorActive: false,
      runtimeState: 'READY',
      runtimeStatusLoaded: false,
      runtimeStatusMessage: ''
    }
  },
  computed: {
    activePageIndex() {
      return this.pageOrder.findIndex(pageId => pageId === this.pageId)
    },
    appFrameMessage() {
      if (!this.runtimeStatusLoaded) {
        return 'Checking learner app status before loading the frame.'
      }

      if (!this.runtimeInteractionBusy) {
        return ''
      }

      return this.runtimeDisplayState === 'REBUILDING'
        ? 'The learner app is rebuilding. The frame will reload automatically when it is ready.'
        : 'The learner app is restarting. The frame will reload automatically when it is ready.'
    },
    basePath() {
      return getBasePath()
    },
    codeEditorUrl() {
      if (this.activeCodeEditorFilePath) {
        return getCodeEditorFileUrl(this.activeCodeEditorFilePath, {
          workspaceRoot: this.codeEditorWorkspaceRoot,
          line: this.activeCodeEditorLine,
          column: this.activeCodeEditorColumn,
          requestId: this.codeEditorOpenRequest
        })
      }

      return getCodeEditorUrl({ workspaceRoot: this.codeEditorWorkspaceRoot })
    },
    contentActionHandlers() {
      return {
        applyEditorStep: ({ args }) => this.applyEditorStep(args?.stepId),
        openEditor: () => this.openEditor(),
        openFile: ({ args }) => this.openFile(args?.file, args),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => this.openRedisInsightPanel(),
        openRoute: ({ args }) => this.openRoute(args?.route),
        recompileApp: () => this.restartRuntime(true),
        restartRuntime: () => this.restartRuntime(false)
      }
    },
    contentContext() {
      return {
        links: {
          editor: this.codeEditorUrl,
          hub: this.workshopHubUrl,
          learnerApp: this.learnerAppUrl,
          redisInsight: this.redisInsightFrameUrl,
          workshopHub: this.workshopHubUrl
        },
        runtime: this.shellRuntime
      }
    },
    defaultSidePanel() {
      return ['2', '3', '4'].includes(this.pageId) ? 'codeEditor' : 'learnerApp'
    },
    editorStepStatusClass() {
      return {
        'editor-step-status--error': this.editorStepStatusType === 'error',
        'editor-step-status--success': this.editorStepStatusType === 'success'
      }
    },
    hasStageNavigation() {
      return this.pageOrder.length > 1 && this.activePageIndex >= 0
    },
    instructionsHeaderTitle() {
      const title = this.content?.title
        || this.navigationPageTitles[this.pageId]
        || `Stage ${this.pageId}`

      return this.formatInstructionsTitle(title)
    },
    learnerApp() {
      return {
        title: 'Learner app',
        url: this.learnerAppUrl,
        message: this.appFrameMessage
      }
    },
    learnerAppUrl() {
      const route = this.buildRouteUrl('/app/')
      const separator = route.includes('?') ? '&' : '?'
      const params = new URLSearchParams({
        stage: this.pageId,
        frame: String(this.appFrameVersion)
      })
      return `${route}${separator}${params.toString()}`
    },
    nextStage() {
      if (!this.hasStageNavigation) {
        return null
      }

      return this.pageOrder[this.activePageIndex + 1] || null
    },
    nextStageLabel() {
      return this.stageNavigationLabel('nextLabel', this.nextStage)
    },
    nextStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.nextStageLabel, this.nextStage)
    },
    pageOrder() {
      return PAGES.map(page => page.id)
    },
    previousStage() {
      if (!this.hasStageNavigation) {
        return null
      }

      return this.pageOrder[this.activePageIndex - 1] || null
    },
    previousStageLabel() {
      return this.stageNavigationLabel('previousLabel', this.previousStage)
    },
    previousStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.previousStageLabel, this.previousStage)
    },
    redisInsightFrameUrl() {
      const normalizedBasePath = this.basePath && this.basePath !== '/' ? this.basePath : ''
      return `${window.location.origin}${normalizedBasePath}/redis-insight/`
    },
    runtimeDisplayState() {
      if (!this.runtimeStatusLoaded) {
        return 'STARTING'
      }

      return isRebuildRunnerState(this.runtimeState) ? 'REBUILDING' : 'RESTARTING'
    },
    runtimeInteractionBusy() {
      return !this.runtimeStatusLoaded
        || this.runtimeBusy
        || isBusyRunnerState(this.runtimeState)
        || isBusyRunnerState(this.backendState)
    },
    shellActions() {
      return ['restartRuntime', 'rebuildRuntime', 'openRedisInsight', 'openEditor', 'openHub', 'refreshStatus']
    },
    shellLinks() {
      return {
        editor: this.codeEditorUrl,
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightFrameUrl,
        workshopHub: this.workshopHubUrl
      }
    },
    shellRuntime() {
      return {
        state: this.runtimeState,
        frontendState: this.frontendState,
        backendState: this.backendState,
        status: this.runtimeStatusMessage,
        rebuildAvailable: true
      }
    },
    workshopHubUrl() {
      return getWorkshopHubUrl()
    }
  },
  watch: {
    async pageId() {
      this.contentError = ''
      this.content = null
      await Promise.all([
        this.loadContent(),
        this.loadNavigationPageTitles()
      ])
    }
  },
  async mounted() {
    await Promise.all([
      this.loadContent(),
      this.loadCodeEditorFiles(),
      this.loadNavigationPageTitles(),
      this.refreshRuntimeState()
    ])
  },
  beforeUnmount() {
    this.clearEditorStepStatusTimer()
  },
  methods: {
    async applyEditorStep(stepId) {
      const config = EDITOR_STEP_CONFIG[stepId]
      if (!config) {
        this.showEditorStepStatus('No editor step is configured for this button.', 'error')
        return
      }

      try {
        this.showEditorStepStatus(`Applying change to ${config.fileName}...`, 'success', 15000)
        const currentContent = await this.loadEditableFile(config.fileName)
        const nextContent = config.transform(currentContent)
        const changed = nextContent !== currentContent
        if (changed) {
          await this.saveEditableFile(config.fileName, nextContent)
        }
        await this.openFile(config.fileName, { line: config.line, forceReload: true })
        this.showEditorStepStatus(
          changed
            ? `Applied change to ${config.fileName}.`
            : `${config.fileName} already has this change.`,
          'success',
          15000
        )
      } catch (error) {
        console.error('Failed to apply editor step:', error)
        this.showEditorStepStatus(error.message || 'Failed to apply editor step.', 'error')
      }
    },
    applyRuntimeLogs(status) {
      if (Array.isArray(status?.recentLogs)) {
        this.runtimeLogs = status.recentLogs.slice(-80)
      }
    },
    applyRuntimeStatus(status) {
      const state = status?.state
      this.applyRuntimeLogs(status)

      if (!state) {
        return
      }

      this.runtimeState = normalizeRunnerState(state)
      this.backendState = this.runtimeState
      this.frontendState = 'READY'

      if (isBusyRunnerState(this.runtimeState)) {
        this.runtimeStatusMessage = isRebuildRunnerState(this.runtimeState)
          ? 'Rebuilding learner runtime...'
          : 'Restarting learner runtime...'
      } else if (READY_STATES.has(this.runtimeState)) {
        this.runtimeStatusMessage = 'Runtime is ready'
      }
    },
    buildRouteUrl(route) {
      const normalizedRoute = route.startsWith('/') ? route : `/${route}`
      if (!this.basePath || this.basePath === '/') {
        return normalizedRoute
      }
      return `${this.basePath}${normalizedRoute}`
    },
    async fetchSessionStatus() {
      try {
        const response = await fetch(getApiUrl('/internal/session-runner/status'), {
          credentials: 'include'
        })

        if (!response.ok) {
          return null
        }

        const status = await response.json()
        this.applyRuntimeLogs(status)
        return status
      } catch {
        return null
      }
    },
    formatInstructionsTitle(title) {
      const match = String(title || '').match(/^STAGE\s+(\d+):\s*(.+)$/i)
      if (!match) {
        return title
      }

      return `#${match[1]}: ${match[2]}`
    },
    async goToPage(pageId) {
      if (PAGES.some(page => page.id === pageId)) {
        await this.$router.push(`/${pageId}`).catch(() => {})
      }
    },
    handleContentAction(payload) {
      const handler = this.contentActionHandlers[payload.actionId]
      if (typeof handler === 'function') {
        handler(payload)
      }
    },
    handleEditorRecompileStart() {
      this.runtimeBusy = true
      this.runtimeState = 'REBUILDING'
      this.backendState = 'REBUILDING'
      this.runtimeStatusMessage = 'Rebuilding learner runtime...'
    },
    async handleEditorRecompileSuccess() {
      this.runtimeBusy = false
      this.runtimeState = 'READY'
      this.backendState = 'READY'
      this.frontendState = 'READY'
      this.runtimeStatusMessage = 'Runtime is ready'
      this.appFrameVersion += 1
      await this.refreshRuntimeState()
    },
    handleEditorRecompileError(error) {
      this.runtimeBusy = false
      this.runtimeState = 'FAILED'
      this.backendState = 'FAILED'
      this.runtimeStatusMessage = error?.message || 'Recompile failed'
    },
    handleEditorResetSuccess() {
      this.activeCodeEditorFilePath = ''
      this.activeCodeEditorLine = 0
      this.activeCodeEditorColumn = 1
      this.codeEditorOpenRequest += 1
    },
    handleRuntimeAction(action) {
      if (action.type === 'restart') {
        this.restartRuntime(false)
      }

      if (action.type === 'rebuild') {
        this.restartRuntime(true)
      }

      if (action.type === 'refresh') {
        this.refreshLearnerAppFrame()
      }

      if (action.type === 'recompile-start') {
        this.handleEditorRecompileStart()
      }

      if (action.type === 'recompile-success') {
        this.handleEditorRecompileSuccess()
      }

      if (action.type === 'recompile-error') {
        this.handleEditorRecompileError(action.payload)
      }

      if (action.type === 'reset-success') {
        this.handleEditorResetSuccess()
      }
    },
    async handleRuntimeLogsToggle(open) {
      if (open) {
        await this.refreshRuntimeState()
      }
    },
    async loadCodeEditorFiles() {
      try {
        const metadata = await loadEditorWorkspaceMetadata()
        this.codeEditorFilePathByName = metadata.filePathMap
        this.codeEditorWorkspaceRoot = metadata.codeEditorWorkspaceRoot || metadata.workspaceRoot
      } catch (error) {
        console.warn('Failed to load code editor file metadata:', error)
      }
    },
    async loadEditableFile(fileName) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        cache: 'no-store',
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error(`Failed to load ${fileName}: ${response.status}`)
      }

      const payload = await response.json()
      if (payload.error) {
        throw new Error(payload.error)
      }
      if (typeof payload.content !== 'string') {
        throw new Error(`No content returned for ${fileName}`)
      }

      return payload.content
    },
    async loadContent() {
      try {
        this.content = await fetchWorkshopContent(this.pageId)
        this.storeNavigationPageTitle(this.pageId, this.content)
      } catch (error) {
        this.contentError = error.message || 'Failed to load workshop content.'
      }
    },
    async loadNavigationPageTitles() {
      const missingPageIds = this.pageOrder.filter(pageId => !this.navigationPageTitles[pageId])
      if (missingPageIds.length === 0) {
        return
      }

      const titles = {}

      await Promise.all(missingPageIds.map(async pageId => {
        try {
          const content = pageId === this.pageId && this.content
            ? this.content
            : await fetchWorkshopContent(pageId)

          if (content?.title) {
            titles[pageId] = content.title
          }
        } catch (error) {
          console.warn(`Unable to load navigation title for page ${pageId}`, error)
        }
      }))

      if (Object.keys(titles).length > 0) {
        this.navigationPageTitles = {
          ...this.navigationPageTitles,
          ...titles
        }
      }
    },
    async openCodeEditorPanel(fileName = '', options = {}) {
      this.replaceToolQuery('code-editor')

      if (fileName) {
        await this.openEmbeddedEditorFile(fileName, options)
      }
    },
    async openEditor() {
      await this.openCodeEditorPanel()
    },
    async openEmbeddedEditorFile(fileName, options = {}) {
      if (!fileName) {
        return
      }

      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles()
      }

      const workspacePath = this.codeEditorFilePathByName[fileName]
      if (!workspacePath) {
        return
      }

      if (options.forceReload) {
        this.activeCodeEditorFilePath = ''
        this.activeCodeEditorLine = 0
        this.activeCodeEditorColumn = 1
        this.codeEditorOpenRequest += 1
        await this.$nextTick()
      }

      this.codeEditorOpenRequest += 1
      this.activeCodeEditorFilePath = workspacePath
      this.activeCodeEditorLine = this.normalizeEditorLine(options.line)
      this.activeCodeEditorColumn = this.normalizeEditorLine(options.column) || 1
    },
    async openFile(fileName = '', options = {}) {
      await this.openCodeEditorPanel(fileName, options)
    },
    openRedisInsightPanel() {
      this.replaceToolQuery('redis-insight')
    },
    async openRoute(route) {
      if (!route) {
        return
      }

      if (route === '/editor' || route === 'editor' || /^\/6(\?.*)?$/.test(route)) {
        await this.$router.push(route === '/editor' || route === 'editor' ? '/6' : route).catch(() => {})
        return
      }

      if (/^\/[0-5](\?.*)?$/.test(route)) {
        await this.$router.push(route).catch(() => {})
      }
    },
    normalizeEditorLine(value) {
      const number = Number.parseInt(value, 10)
      return Number.isFinite(number) && number > 0 ? number : 0
    },
    async saveEditableFile(fileName, content) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ content })
      })
      const payload = await response.json()

      if (!response.ok || payload.error) {
        throw new Error(payload.error || `Failed to save ${fileName}: ${response.status}`)
      }

      return payload
    },
    async refreshRuntimeState() {
      try {
        const status = await this.fetchSessionStatus()
        const state = status?.state
        if (state) {
          this.applyRuntimeStatus(status)
          if (isBusyRunnerState(state)) {
            this.monitorRuntimeUntilReady()
          }
        } else {
          this.runtimeState = 'READY'
          this.backendState = 'READY'
          this.frontendState = 'READY'
          this.runtimeStatusMessage = 'Runtime is ready'
        }
      } finally {
        this.runtimeStatusLoaded = true
      }
    },
    refreshLearnerAppFrame() {
      this.refreshRuntimeState()
      this.appFrameVersion += 1
    },
    replaceToolQuery(tool) {
      const nextQuery = { ...this.$route.query }
      if (tool) {
        nextQuery.tool = tool
      } else {
        delete nextQuery.tool
      }

      if (nextQuery.tool === this.$route.query.tool) {
        return
      }

      this.$router.replace({
        path: this.$route.path,
        query: nextQuery
      }).catch(() => {})
    },
    async restartRuntime(rebuild) {
      if (this.runtimeInteractionBusy) {
        return
      }

      this.runtimeBusy = true
      this.runtimeState = rebuild ? 'REBUILDING' : 'RESTARTING'
      this.backendState = this.runtimeState
      this.runtimeStatusMessage = rebuild ? 'Rebuilding learner runtime...' : 'Restarting learner runtime...'
      const visibleStateStartedAt = Date.now()

      try {
        await this.$nextTick()
        await this.sendRestartRequest(rebuild)
        await this.waitForReadyState()
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'READY'
        this.backendState = 'READY'
        this.frontendState = 'READY'
        this.runtimeStatusMessage = 'Runtime is ready'
        this.appFrameVersion += 1
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'FAILED'
        this.backendState = 'FAILED'
        this.runtimeStatusMessage = error?.message || 'Restart failed'
      } finally {
        this.runtimeBusy = false
      }
    },
    async monitorRuntimeUntilReady() {
      if (this.runtimeMonitorActive) {
        return
      }

      this.runtimeMonitorActive = true
      this.runtimeBusy = true
      const visibleStateStartedAt = Date.now()

      try {
        await this.waitForReadyState()
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'READY'
        this.backendState = 'READY'
        this.frontendState = 'READY'
        this.runtimeStatusMessage = 'Runtime is ready'
        this.appFrameVersion += 1
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'FAILED'
        this.backendState = 'FAILED'
        this.runtimeStatusMessage = error?.message || 'Restart failed'
      } finally {
        this.runtimeBusy = false
        this.runtimeMonitorActive = false
      }
    },
    async sendRestartRequest(rebuild) {
      const response = await fetch(getApiUrl('/internal/session-runner/restart'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ rebuild, async: true })
      })

      if (!response.ok) {
        throw new Error(await this.readErrorMessage(response, 'Failed to restart session'))
      }
    },
    async readErrorMessage(response, fallback) {
      try {
        const payload = await response.json()
        return payload.error || payload.message || fallback
      } catch {
        return fallback
      }
    },
    sleep(ms) {
      return new Promise(resolve => window.setTimeout(resolve, ms))
    },
    clearEditorStepStatusTimer() {
      if (this.editorStepStatusTimer) {
        window.clearTimeout(this.editorStepStatusTimer)
        this.editorStepStatusTimer = null
      }
    },
    showEditorStepStatus(message, type = 'success', timeoutMs = 5000) {
      this.clearEditorStepStatusTimer()
      this.editorStepStatusMessage = message
      this.editorStepStatusType = type

      if (timeoutMs > 0) {
        this.editorStepStatusTimer = window.setTimeout(() => {
          this.editorStepStatusMessage = ''
          this.editorStepStatusType = ''
          this.editorStepStatusTimer = null
        }, timeoutMs)
      }
    },
    stageNavigationAriaLabel(label, pageId) {
      if (label) {
        return label
      }

      return this.navigationPageTitles[pageId] || `stage ${pageId}`
    },
    stageNavigationLabel(labelKey, pageId) {
      const configuredLabel = this.content?.navigation?.[labelKey]
      if (configuredLabel) {
        return configuredLabel
      }

      return this.navigationPageTitles[pageId] || `Stage ${pageId}`
    },
    storeNavigationPageTitle(pageId, content) {
      if (!content?.title) {
        return
      }

      this.navigationPageTitles = {
        ...this.navigationPageTitles,
        [pageId]: content.title
      }
    },
    async waitForMinimumBusyState(startedAt) {
      const elapsed = Date.now() - startedAt
      const remaining = MIN_RUNTIME_BUSY_MS - elapsed
      if (remaining > 0) {
        await this.sleep(remaining)
      }
    },
    async waitForReadyState() {
      const deadline = Date.now() + POLL_TIMEOUT_MS

      while (Date.now() < deadline) {
        const status = await this.fetchSessionStatus()
        const state = normalizeRunnerState(status?.state)

        if (READY_STATES.has(state)) {
          this.applyRuntimeStatus(status)
          return
        }

        if (FAILED_STATES.has(state)) {
          throw new Error(status?.failureMessage || 'Runtime failed to start')
        }

        await this.sleep(POLL_INTERVAL_MS)
      }

      throw new Error('Timed out waiting for runtime to become ready')
    }
  }
}
</script>

<style scoped>
.patterns-home {
  min-height: 100vh;
}

:deep(.workshop-shell__instructions) {
  display: flex;
  padding: 0;
  overflow: hidden;
}

.patterns-instructions {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.patterns-instructions__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  min-height: 2.75rem;
  padding: var(--spacing-2) var(--spacing-3);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.patterns-instructions__title {
  min-width: 0;
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.2;
}

.patterns-instructions__body {
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

.editor-step-status {
  margin: 0 0 var(--spacing-3);
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: var(--radius-lg);
  background: rgba(74, 222, 128, 0.1);
  color: #bbf7d0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.45;
  padding: 0.65rem 0.85rem;
}

.editor-step-status--error {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.1);
  color: #fecaca;
}

:deep(.content-step-item),
:deep(.content-widget) {
  background: var(--color-dark-800);
}

:deep(.content-step-item__heading h4),
:deep(.workshop-markdown h2),
:deep(.workshop-markdown h3),
:deep(.workshop-markdown h4) {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

:deep(.content-section__header .workshop-markdown),
:deep(.workshop-markdown),
:deep(.content-step-item__heading .workshop-markdown) {
  color: var(--color-text-secondary);
}

.stage-navigation {
  position: relative;
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

@media (max-width: 900px) {
  .patterns-instructions__header {
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
