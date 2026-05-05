<template>
  <div class="fundamentals-home">
    <WorkshopShell
      title="Module 1: Spring AI Fundamentals"
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
        <div class="fundamentals-instructions">
          <header class="fundamentals-instructions__header">
            <p class="fundamentals-instructions__title">{{ instructionsHeaderTitle }}</p>
          </header>

          <div class="fundamentals-instructions__body">
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
  { id: '0', title: 'Introduction' },
  { id: '1', title: 'Prompt' },
  { id: '2', title: 'Structured output' },
  { id: '3', title: 'Tools' },
  { id: '4', title: 'Memory and advisors' },
  { id: '5', title: 'Final helper' }
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
  '1.enableImports': {
    fileName: 'SpringAiDemoService.java',
    line: 6,
    transform: content => content
      .replace(
        '// import org.springframework.ai.chat.client.ChatClient;',
        'import org.springframework.ai.chat.client.ChatClient;'
      )
      .replace(
        '// import org.springframework.ai.chat.model.ChatModel;',
        'import org.springframework.ai.chat.model.ChatModel;'
      )
  },
  '1.enableFields': {
    fileName: 'SpringAiDemoService.java',
    line: 34,
    transform: content => content
      .replace('// private final ChatModel chatModel;', 'private final ChatModel chatModel;')
      .replace('// private volatile ChatClient chatClient;', 'private volatile ChatClient chatClient;')
  },
  '1.enableConstructor': {
    fileName: 'SpringAiDemoService.java',
    line: 49,
    transform: content => content
      .replace('// , ChatModel chatModel', ', ChatModel chatModel')
      .replace('// this.chatModel = chatModel;', 'this.chatModel = chatModel;')
  },
  '1.enablePrompt': {
    fileName: 'SpringAiDemoService.java',
    line: 72,
    transform: content => content
      .replace(
        '        return new PromptResponse(true, PLACEHOLDER_ANSWER.trim(), null);',
        '        // return new PromptResponse(true, PLACEHOLDER_ANSWER.trim(), null);'
      )
      .replace(
        `        /*
        Stage 1 prompt block to enable:
        Comment out the hardcoded return above, then uncomment this block.

        try {`,
        `        // Stage 1 prompt block enabled.

        try {`
      )
      .replace(
        `        }
        */
    }

    public StructuredResponse`,
        `        }
    }

    public StructuredResponse`
      )
  },
  '1.enableClientHelper': {
    fileName: 'SpringAiDemoService.java',
    line: 209,
    transform: content => content
      .replace(
        `    /*
    Stage 1 client helper to enable:
    Uncomment this method after enabling the Stage 1 imports, fields, constructor parameter, and assignment.

    private ChatClient client() {`,
        `    // Stage 1 client helper enabled.

    private ChatClient client() {`
      )
      .replace(
        `    }
    */

    /*
    Stage 4 memory client helper to enable:`,
        `    }

    /*
    Stage 4 memory client helper to enable:`
      )
  },
  '2.enableImports': {
    fileName: 'SpringAiDemoService.java',
    line: 10,
    transform: content => content
      .replace(
        '// import org.springframework.ai.chat.client.AdvisorParams;',
        'import org.springframework.ai.chat.client.AdvisorParams;'
      )
      .replace(
        '// import org.springframework.ai.chat.client.ResponseEntity;',
        'import org.springframework.ai.chat.client.ResponseEntity;'
      )
      .replace(
        '// import org.springframework.ai.chat.model.ChatResponse;',
        'import org.springframework.ai.chat.model.ChatResponse;'
      )
  },
  '2.enableStructuredBlock': {
    fileName: 'SpringAiDemoService.java',
    line: 91,
    transform: content => content
      .replace(
        /^        return new StructuredResponse\(false, null, NOT_IMPLEMENTED\);$/m,
        '        // return new StructuredResponse(false, null, NOT_IMPLEMENTED);'
      )
      .replace(
        `        /*
        Stage 2 structured output block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {`,
        `        // Stage 2 structured output block enabled.

        try {`
      )
      .replace(
        `        }
        */
    }

    public ToolsResponse`,
        `        }
    }

    public ToolsResponse`
      )
  },
  '3.enableToolImports': {
    fileName: 'MarketDataTools.java',
    line: 5,
    transform: content => content
      .replace(
        `/*
Stage 3 imports to enable later:

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
*/`,
        `import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;`
      )
  },
  '3.enableToolImplementation': {
    fileName: 'MarketDataTools.java',
    line: 28,
    transform: content => content
      .replace(
        `    /*
    Stage 3 will turn this scaffold into a Spring AI tool.

    private final AtomicBoolean invoked = new AtomicBoolean(false);`,
        `    // Stage 3 tool implementation enabled.

    private final AtomicBoolean invoked = new AtomicBoolean(false);`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '3.enableToolDependency': {
    fileName: 'SpringAiDemoService.java',
    line: 37,
    transform: content => content
      .replace(
        '// private final MarketDataTools marketDataTools;',
        'private final MarketDataTools marketDataTools;'
      )
      .replace('// , MarketDataTools marketDataTools', ', MarketDataTools marketDataTools')
      .replace('// this.marketDataTools = marketDataTools;', 'this.marketDataTools = marketDataTools;')
  },
  '3.enableToolsBlock': {
    fileName: 'SpringAiDemoService.java',
    line: 119,
    transform: content => content
      .replace(
        /^        return new ToolsResponse\(false, null, null, false, NOT_IMPLEMENTED\);$/m,
        '        // return new ToolsResponse(false, null, null, false, NOT_IMPLEMENTED);'
      )
      .replace(
        `        /*
        Stage 3 tools block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {`,
        `        // Stage 3 tools block enabled.

        try {`
      )
      .replace(
        `        }
        */
    }

    public MemoryResponse`,
        `        }
    }

    public MemoryResponse`
      )
  },
  '4.enableConfigImports': {
    fileName: 'SpringAiFundamentalsAiConfig.java',
    line: 4,
    transform: content => content
      .replace(
        /\/\*\nStage 4 imports to enable later:\n\nimport org\.springframework\.ai\.chat\.memory\.ChatMemory;\nimport org\.springframework\.ai\.chat\.memory\.ChatMemoryRepository;\nimport org\.springframework\.ai\.chat\.memory\.MessageWindowChatMemory;\nimport org\.springframework\.ai\.chat\.memory\.repository\.redis\.RedisChatMemoryRepository;\nimport org\.springframework\.beans\.factory\.annotation\.Value;\n(?:import org\.springframework\.context\.annotation\.Bean;\n)?import redis\.clients\.jedis\.JedisPooled;\n\*\//,
        `import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;`
      )
  },
  '4.enableRedisMemoryBeans': {
    fileName: 'SpringAiFundamentalsAiConfig.java',
    line: 19,
    transform: content => content
      .replace(
        `    /*
    Stage 4 will enable Redis chat memory.

    @Bean
    public JedisPooled jedisPooled(`,
        `    // Stage 4 Redis chat memory enabled.

    @Bean
    public JedisPooled jedisPooled(`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '4.enableMemoryImports': {
    fileName: 'SpringAiDemoService.java',
    line: 16,
    transform: content => content
      .replace(
        '// import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;',
        'import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;'
      )
      .replace(
        '// import org.springframework.ai.chat.memory.ChatMemory;',
        'import org.springframework.ai.chat.memory.ChatMemory;'
      )
      .replace(
        '// import org.springframework.ai.chat.memory.ChatMemoryRepository;',
        'import org.springframework.ai.chat.memory.ChatMemoryRepository;'
      )
  },
  '4.enableMemoryDependency': {
    fileName: 'SpringAiDemoService.java',
    line: 41,
    transform: content => content
      .replace('// private final ChatMemory chatMemory;', 'private final ChatMemory chatMemory;')
      .replace('// private final ChatMemoryRepository chatMemoryRepository;', 'private final ChatMemoryRepository chatMemoryRepository;')
      .replace('// private volatile ChatClient memoryChatClient;', 'private volatile ChatClient memoryChatClient;')
      .replace('// , ChatMemory chatMemory', ', ChatMemory chatMemory')
      .replace('// , ChatMemoryRepository chatMemoryRepository', ', ChatMemoryRepository chatMemoryRepository')
      .replace('// this.chatMemory = chatMemory;', 'this.chatMemory = chatMemory;')
      .replace('// this.chatMemoryRepository = chatMemoryRepository;', 'this.chatMemoryRepository = chatMemoryRepository;')
  },
  '4.enableMemoryBlock': {
    fileName: 'SpringAiDemoService.java',
    line: 142,
    transform: content => content
      .replace(
        /^        return new MemoryResponse\(false, conversationId, null, 0, NOT_IMPLEMENTED\);$/m,
        '        // return new MemoryResponse(false, conversationId, null, 0, NOT_IMPLEMENTED);'
      )
      .replace(
        `        /*
        Stage 4 memory block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {`,
        `        // Stage 4 memory block enabled.

        try {`
      )
      .replace(
        `        }
        */
    }

    public HelperResponse`,
        `        }
    }

    public HelperResponse`
      )
  },
  '4.enableMemoryClientHelper': {
    fileName: 'SpringAiDemoService.java',
    line: 215,
    transform: content => content
      .replace(
        `    /*
    Stage 4 memory client helper to enable:

    private ChatClient memoryClient() {`,
        `    // Stage 4 memory client helper enabled.

    private ChatClient memoryClient() {`
      )
      .replace(
        `    }
    */

    private boolean apiKeyConfigured()`,
        `    }

    private boolean apiKeyConfigured()`
      )
  },
  '5.enableUuidImport': {
    fileName: 'SpringAiDemoService.java',
    line: 14,
    transform: content => content
      .replace('// import java.util.UUID;', 'import java.util.UUID;')
  },
  '5.enableHelperBlock': {
    fileName: 'SpringAiDemoService.java',
    line: 163,
    transform: content => content
      .replace(
        /^        return new HelperResponse\(false, null, null, false, NOT_IMPLEMENTED\);$/m,
        '        // return new HelperResponse(false, null, null, false, NOT_IMPLEMENTED);'
      )
      .replace(
        `        /*
        Stage 5 final helper block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {`,
        `        // Stage 5 final helper block enabled.

        try {`
      )
      .replace(
        `        }
        */
    }

    // Stage 1 client helper enabled.`,
        `        }
    }

    // Stage 1 client helper enabled.`
      )
      .replace(
        `        }
        */
    }

    /*
    Stage 1 client helper to enable:`,
        `        }
    }

    /*
    Stage 1 client helper to enable:`
      )
  }
}

export default {
  name: 'SpringAiFundamentalsHome',
  components: {
    WorkshopContentRenderer,
    WorkshopShell
  },
  props: {
    pageId: { type: String, default: '0' }
  },
  data() {
    return {
      activeCodeEditorFilePath: '',
      activeCodeEditorLine: 0,
      activeCodeEditorColumn: 1,
      appFrameVersion: 0,
      codeEditorFilePathByName: {},
      codeEditorOpenRequest: 0,
      codeEditorWorkspaceRoot: '',
      content: null,
      contentError: '',
      navigationPageTitles: {},
      runtimeBusy: false,
      runtimeLogs: [],
      runtimeMonitorActive: false,
      runtimeState: 'READY',
      frontendState: 'READY',
      backendState: 'READY',
      runtimeStatusLoaded: false,
      runtimeStatusMessage: '',
      springAiStatus: null,
      springAiStatusError: ''
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
        runtime: this.shellRuntime,
        springAi: {
          status: this.springAiStatus,
          error: this.springAiStatusError
        }
      }
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
    defaultSidePanel() {
      return this.stageUsesInlineEditor ? 'codeEditor' : 'learnerApp'
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
    stageUsesInlineEditor() {
      return ['1', '2', '3', '4', '5'].includes(this.pageId)
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
    },
  },
  async mounted() {
    await Promise.all([
      this.loadContent(),
      this.loadCodeEditorFiles(),
      this.loadNavigationPageTitles(),
      this.loadSpringAiStatus(),
      this.refreshRuntimeState()
    ])
  },
  methods: {
    async applyEditorStep(stepId) {
      const config = EDITOR_STEP_CONFIG[stepId]
      if (!config) {
        return
      }

      try {
        const currentContent = await this.loadEditableFile(config.fileName)
        const nextContent = config.transform(currentContent)
        if (nextContent !== currentContent) {
          await this.saveEditableFile(config.fileName, nextContent)
        }
        await this.openFile(config.fileName, { line: config.line })
      } catch (error) {
        console.error('Failed to apply editor step:', error)
      }
    },
    applyRuntimeLogs(status) {
      if (!Array.isArray(status?.recentLogs)) {
        return
      }

      this.runtimeLogs = status.recentLogs.slice(-80)
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
    async goToEditor(fileName = '', contentId = this.pageId) {
      const query = {
        returnTo: `/${this.pageId}`,
        content: contentId
      }

      if (fileName) {
        query.file = fileName
      }

      await this.$router.push({
        path: '/6',
        query
      }).catch(() => {})
    },
    async openEditor() {
      await this.openCodeEditorPanel()
    },
    async openFile(fileName = '', options = {}) {
      await this.openCodeEditorPanel(fileName, options)
    },
    async openCodeEditorPanel(fileName = '', options = {}) {
      this.replaceToolQuery('code-editor')

      if (fileName) {
        await this.openEmbeddedEditorFile(fileName, options)
      }
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

      this.codeEditorOpenRequest += 1
      this.activeCodeEditorFilePath = workspacePath
      this.activeCodeEditorLine = this.normalizeEditorLine(options.line)
      this.activeCodeEditorColumn = this.normalizeEditorLine(options.column) || 1
    },
    async goToPage(pageId) {
      if (PAGES.some(page => page.id === pageId)) {
        await this.$router.push(`/${pageId}`).catch(() => {})
      }
    },
    async openRoute(route) {
      if (!route) {
        return
      }

      if (route === '/editor' || route === 'editor' || route === '/6') {
        await this.openEditor()
        return
      }

      if (/^\/[0-5]\?tool=learner-app$/.test(route)) {
        await this.$router.push(route).catch(() => {})
        this.showLearnerAppPanel()
        return
      }

      if (/^\/[0-5](\?.*)?$/.test(route)) {
        await this.$router.push(route).catch(() => {})
        return
      }

      await this.goToPage(String(route).replace('/', ''))
    },
    openRedisInsightPanel() {
      this.replaceToolQuery('redis-insight')
    },
    refreshLearnerAppFrame() {
      this.showLearnerAppPanel()
      this.refreshRuntimeState()
      this.loadSpringAiStatus()
      this.appFrameVersion += 1
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
      await this.loadSpringAiStatus()
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
      })
    },
    showLearnerAppPanel() {
      this.replaceToolQuery(this.stageUsesInlineEditor ? 'learner-app' : null)
    },
    normalizeEditorLine(value) {
      const number = Number.parseInt(value, 10)
      return Number.isFinite(number) && number > 0 ? number : 0
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
    handleContentAction(payload) {
      const handler = this.contentActionHandlers[payload.actionId]
      if (typeof handler === 'function') {
        handler(payload)
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
    async loadSpringAiStatus() {
      try {
        const response = await fetch(getApiUrl('/api/spring-ai/status'), {
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`Status request failed with ${response.status}`)
        }

        this.springAiStatus = await response.json()
        this.springAiStatusError = ''
      } catch (error) {
        this.springAiStatusError = error.message || 'Unable to read Spring AI status.'
      }
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
        await this.loadSpringAiStatus()
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
        await this.loadSpringAiStatus()
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'FAILED'
        this.backendState = 'FAILED'
        this.runtimeStatusMessage = error?.message || 'Restart failed'
      } finally {
        this.runtimeBusy = false
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
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    stageNavigationAriaLabel(label, pageId) {
      return label || this.stageNavigationFallbackLabel(pageId)
    },
    stageNavigationFallbackLabel(pageId) {
      if (!pageId) {
        return ''
      }

      return this.navigationPageTitles[pageId] || PAGES.find(page => page.id === pageId)?.title || `Stage ${pageId}`
    },
    stageNavigationLabel(labelKey, pageId) {
      if (!pageId) {
        return ''
      }

      if (
        this.content?.navigation
        && Object.prototype.hasOwnProperty.call(this.content.navigation, labelKey)
      ) {
        return this.content.navigation[labelKey] || ''
      }

      return this.stageNavigationFallbackLabel(pageId)
    },
    storeNavigationPageTitle(pageId, content) {
      if (!pageId || !content?.title) {
        return
      }

      this.navigationPageTitles = {
        ...this.navigationPageTitles,
        [pageId]: content.title
      }
    },
    async waitForMinimumBusyState(startedAt) {
      const remainingMs = MIN_RUNTIME_BUSY_MS - (Date.now() - startedAt)
      if (remainingMs > 0) {
        await this.sleep(remainingMs)
      }
    },
    async waitForReadyState() {
      const startedAt = Date.now()

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const status = await this.fetchSessionStatus()
        const state = normalizeRunnerState(status?.state)

        if (READY_STATES.has(state)) {
          return
        }
        if (FAILED_STATES.has(state)) {
          throw new Error(status?.lastError || 'Session restart failed')
        }

        await this.sleep(POLL_INTERVAL_MS)
      }

      throw new Error('Timed out waiting for the session to restart')
    },
    async readErrorMessage(response, fallbackMessage) {
      const text = await response.text()

      if (!text) {
        return fallbackMessage
      }

      try {
        const data = JSON.parse(text)
        if (data?.message) {
          return `${fallbackMessage}: ${data.message}`
        }
        if (data?.detail) {
          return `${fallbackMessage}: ${data.detail}`
        }
        if (data?.error) {
          return `${fallbackMessage}: ${data.error}`
        }
      } catch {
        return `${fallbackMessage}: ${text}`
      }

      return fallbackMessage
    }
  }
}
</script>

<style scoped>
.fundamentals-home {
  min-height: 100vh;
}

:deep(.workshop-shell__instructions) {
  display: flex;
  padding: 0;
  overflow: hidden;
}

.fundamentals-instructions {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.fundamentals-instructions__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  min-height: 2.75rem;
  padding: var(--spacing-2) var(--spacing-3);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.fundamentals-instructions__title {
  min-width: 0;
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.2;
}

.fundamentals-instructions__body {
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
  .fundamentals-instructions__header {
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
