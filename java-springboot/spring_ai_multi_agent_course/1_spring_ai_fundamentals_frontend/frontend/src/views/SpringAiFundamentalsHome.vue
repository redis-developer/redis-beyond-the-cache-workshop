<template>
  <div class="fundamentals-home">
    <WorkshopShell
      title="Module 1: Spring AI Fundamentals"
      eyebrow="Spring AI Multi Agent Course"
      :summary="activePage.summary"
      :content="content"
      :context="contentContext"
      :links="shellLinks"
      :learner-app="learnerApp"
      :runtime="runtime"
      :actions="shellActions"
      :action-handlers="actionHandlers"
      :show-content-title="true"
      :show-content-summary="true"
      :show-stage-title="false"
    >
      <template #app-frame>
        <section class="try-panel" :aria-label="`${activePage.title} try panel`">
          <header class="try-panel__header">
            <div>
              <p class="try-panel__eyebrow">Try it</p>
              <h2>{{ activePage.title }}</h2>
            </div>
            <span class="try-panel__step">Page {{ pageId }}</span>
          </header>

          <div
            v-if="statusMessage"
            class="status-banner"
            :class="statusClass"
          >
            {{ statusMessage }}
          </div>

          <div v-if="panelKind === 'intro'" class="try-panel__empty">
            <p>Use the pages to test each Spring AI concept against the running backend.</p>
            <button type="button" @click="goToPage('1')">Start with prompts</button>
          </div>

          <form v-else class="try-panel__form" @submit.prevent="runActivePanel">
            <label v-if="needsConversationId">
              Conversation ID
              <input v-model="form.conversationId" type="text" autocomplete="off" />
            </label>

            <label v-if="needsTicker">
              Ticker
              <input v-model="form.ticker" type="text" autocomplete="off" />
            </label>

            <label v-if="needsQuestion">
              Question
              <textarea v-model="form.question" rows="3"></textarea>
            </label>

            <label v-if="needsMessage">
              Message
              <textarea v-model="form.message" rows="4"></textarea>
            </label>

            <button type="submit" :disabled="busy">
              {{ busy ? 'Running...' : activePage.button }}
            </button>
          </form>

          <div v-if="error" class="result result--error">
            <p class="result__label">Error</p>
            <p>{{ error }}</p>
          </div>

          <div v-if="hasResult" class="result">
            <p class="result__label">Response</p>

            <template v-if="panelKind === 'prompt'">
              <p>{{ displayValue(result.response || result.message || result.content || result.answer) }}</p>
            </template>

            <template v-else-if="panelKind === 'structured'">
              <dl>
                <dt>Finish reason</dt>
                <dd>{{ displayValue(structuredResult.finishReason) }}</dd>
                <dt>Resolved ticker</dt>
                <dd>{{ displayValue(structuredResult.resolvedTicker) }}</dd>
                <dt>Resolved question</dt>
                <dd>{{ displayValue(structuredResult.resolvedQuestion) }}</dd>
                <dt>Reasoning</dt>
                <dd>{{ displayValue(structuredResult.reasoning) }}</dd>
              </dl>
            </template>

            <template v-else-if="panelKind === 'tools'">
              <dl>
                <dt>Tool invoked</dt>
                <dd>{{ toolInvokedLabel(result) }}</dd>
                <dt>Snapshot</dt>
                <dd><pre>{{ formatValue(result.snapshot || result.toolSnapshot || result) }}</pre></dd>
              </dl>
            </template>

            <template v-else-if="panelKind === 'memory'">
              <dl>
                <dt>Conversation ID</dt>
                <dd>{{ displayValue(result.conversationId || form.conversationId) }}</dd>
                <dt>Message count</dt>
                <dd>{{ displayValue(result.messageCount) }}</dd>
                <dt>Reply</dt>
                <dd>{{ displayValue(result.response || result.message || result.answer) }}</dd>
              </dl>
            </template>

            <template v-else-if="panelKind === 'helper'">
              <dl>
                <dt>Structured result</dt>
                <dd><pre>{{ formatValue(result.structuredResult || result.structured || result.result) }}</pre></dd>
                <dt>Snapshot</dt>
                <dd><pre>{{ formatValue(result.snapshot || result.toolSnapshot) }}</pre></dd>
                <dt>Answer</dt>
                <dd>{{ displayValue(result.response || result.answer || result.message) }}</dd>
              </dl>
            </template>
          </div>

          <nav class="page-nav" aria-label="Module pages">
            <button
              v-for="page in pages"
              :key="page.id"
              type="button"
              :class="{ 'page-nav__button--active': page.id === pageId }"
              @click="goToPage(page.id)"
            >
              {{ page.id }}
            </button>
            <button type="button" @click="goToEditor">Editor</button>
          </nav>
        </section>
      </template>
    </WorkshopShell>
  </div>
</template>

<script>
import {
  getApiUrl,
  getRedisInsightUrl,
  getWorkshopHubUrl,
  WorkshopShell
} from '../../../../../../workshop-frontend-shared/src/index.js'
import { springAiGet, springAiPost } from '../utils/springAiApi'
import { fetchWorkshopContent } from '../utils/workshopContent'

const PAGES = [
  { id: '0', title: 'Introduction', summary: 'Start with the Spring AI client and the concepts used throughout the module.', kind: 'intro', button: '' },
  { id: '1', title: 'Prompt', summary: 'Send a message through the Spring AI prompt endpoint.', kind: 'prompt', button: 'Send prompt' },
  { id: '2', title: 'Structured output', summary: 'Ask the model to resolve a ticker and question into a structured result.', kind: 'structured', button: 'Create structure' },
  { id: '3', title: 'Tools', summary: 'Let the model answer a market question with a backend tool.', kind: 'tools', button: 'Run tool request' },
  { id: '4', title: 'Memory and advisors', summary: 'Reuse a conversation ID and observe message history behavior.', kind: 'memory', button: 'Send memory message' },
  { id: '5', title: 'Final helper', summary: 'Combine structured output, tool calling, and memory in one helper flow.', kind: 'helper', button: 'Ask helper' }
]

export default {
  name: 'SpringAiFundamentalsHome',
  components: {
    WorkshopShell
  },
  props: {
    pageId: { type: String, default: '0' }
  },
  data() {
    return {
      content: null,
      contentError: '',
      status: null,
      statusError: '',
      busy: false,
      error: '',
      result: null,
      form: {
        conversationId: 'fundamentals-demo',
        message: 'Explain what Spring AI does in one paragraph.',
        question: 'What is Redis used for in AI applications?',
        ticker: 'RDIS'
      }
    }
  },
  computed: {
    pages() {
      return PAGES
    },
    activePage() {
      return PAGES.find(page => page.id === this.pageId) || PAGES[0]
    },
    panelKind() {
      return this.activePage.kind
    },
    needsConversationId() {
      return ['memory', 'helper'].includes(this.panelKind)
    },
    needsMessage() {
      return ['prompt', 'structured', 'memory'].includes(this.panelKind)
    },
    needsQuestion() {
      return ['tools', 'helper'].includes(this.panelKind)
    },
    needsTicker() {
      return ['tools', 'helper'].includes(this.panelKind)
    },
    hasResult() {
      return Boolean(this.result)
    },
    structuredResult() {
      return this.result?.decision || this.result || {}
    },
    statusMissingKey() {
      if (!this.status) {
        return false
      }

      const values = [
        this.status.openAiApiKeyConfigured,
        this.status.openaiApiKeyConfigured,
        this.status.apiKeyConfigured,
        this.status.ready
      ]

      return values.some(value => value === false)
        || this.status.missingOpenAiApiKey === true
        || this.status.missingApiKey === true
        || String(this.status.status || '').toLowerCase().includes('missing')
    },
    statusMessage() {
      if (this.statusError) {
        return this.statusError
      }

      if (!this.status) {
        return 'Checking Spring AI status...'
      }

      if (this.statusMissingKey) {
        return 'OPENAI_API_KEY is missing. Set it before running model backed requests.'
      }

      return this.status.message || 'Spring AI backend is ready.'
    },
    statusClass() {
      return {
        'status-banner--warning': this.statusMissingKey || Boolean(this.statusError),
        'status-banner--ready': Boolean(this.status) && !this.statusMissingKey && !this.statusError
      }
    },
    contentContext() {
      return {
        links: this.shellLinks,
        runtime: this.runtime
      }
    },
    learnerApp() {
      return {
        title: this.activePage.title,
        url: this.learnerAppUrl
      }
    },
    learnerAppUrl() {
      return getApiUrl('/api/learner-app')
    },
    redisInsightUrl() {
      return getRedisInsightUrl()
    },
    runtime() {
      return {
        state: this.statusMissingKey ? 'DISABLED' : 'READY',
        frontendState: 'READY',
        backendState: this.statusMissingKey ? 'DISABLED' : 'READY',
        status: this.statusMissingKey ? 'blocked' : 'ready',
        rebuildAvailable: true
      }
    },
    shellActions() {
      return ['openEditor', 'openRedisInsight', 'openHub']
    },
    shellLinks() {
      return {
        editor: getApiUrl('/editor'),
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightUrl,
        workshopHub: this.workshopHubUrl
      }
    },
    workshopHubUrl() {
      return getWorkshopHubUrl()
    },
    actionHandlers() {
      return {
        openEditor: () => this.goToEditor(),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => this.goToRoute(args?.route)
      }
    }
  },
  watch: {
    pageId: {
      immediate: true,
      async handler() {
        this.result = null
        this.error = ''
        await this.loadContent()
      }
    }
  },
  async mounted() {
    await this.loadStatus()
  },
  methods: {
    async loadContent() {
      this.content = null
      this.contentError = ''

      try {
        this.content = await fetchWorkshopContent(this.pageId)
      } catch (error) {
        this.contentError = error.message || 'Failed to load workshop content.'
        this.content = {
          title: this.activePage.title,
          summary: this.contentError,
          sections: []
        }
      }
    },
    async loadStatus() {
      try {
        this.status = await springAiGet('/api/spring-ai/status')
      } catch (error) {
        this.statusError = error.message || 'Unable to read Spring AI status.'
      }
    },
    async runActivePanel() {
      this.busy = true
      this.error = ''
      this.result = null

      try {
        if (this.panelKind === 'prompt') {
          this.applyResult(await springAiPost('/api/spring-ai/prompt', { message: this.form.message }))
        } else if (this.panelKind === 'structured') {
          this.applyResult(await springAiPost('/api/spring-ai/structured', { message: this.form.message }))
        } else if (this.panelKind === 'tools') {
          this.applyResult(await springAiPost('/api/spring-ai/tools', {
            ticker: this.form.ticker,
            question: this.form.question
          }))
        } else if (this.panelKind === 'memory') {
          this.applyResult(await springAiPost('/api/spring-ai/memory', {
            conversationId: this.form.conversationId,
            message: this.form.message
          }))
        } else if (this.panelKind === 'helper') {
          this.applyResult(await springAiPost('/api/spring-ai/helper', {
            conversationId: this.form.conversationId,
            ticker: this.form.ticker,
            question: this.form.question
          }))
        }
      } catch (error) {
        this.error = error.message || 'The request failed.'
      } finally {
        this.busy = false
      }
    },
    applyResult(payload) {
      this.result = payload
      if (payload?.success === false && payload?.error) {
        this.error = payload.error
      }
    },
    goToPage(pageId) {
      if (PAGES.some(page => page.id === pageId)) {
        this.$router.push(`/${pageId}`).catch(() => {})
      }
    },
    goToEditor() {
      this.$router.push('/editor').catch(() => {})
    },
    goToRoute(route) {
      if (route === '/editor' || route === 'editor') {
        this.goToEditor()
        return
      }

      this.goToPage(String(route || '').replace('/', ''))
    },
    displayValue(value) {
      if (value === null || value === undefined || value === '') {
        return 'Not returned'
      }

      if (typeof value === 'object') {
        return this.formatValue(value)
      }

      return String(value)
    },
    boolLabel(value) {
      if (value === true) {
        return 'Yes'
      }

      if (value === false) {
        return 'No'
      }

      return 'Not returned'
    },
    toolInvokedLabel(result) {
      if (Object.prototype.hasOwnProperty.call(result, 'toolInvoked')) {
        return this.boolLabel(result.toolInvoked)
      }

      if (Object.prototype.hasOwnProperty.call(result, 'invokedTool')) {
        return this.boolLabel(result.invokedTool)
      }

      return this.boolLabel(result.usedTool)
    },
    formatValue(value) {
      if (value === null || value === undefined || value === '') {
        return 'Not returned'
      }

      if (typeof value === 'string') {
        return value
      }

      return JSON.stringify(value, null, 2)
    }
  }
}
</script>

<style scoped>
.fundamentals-home {
  min-height: 100vh;
}

.try-panel {
  min-height: 100%;
  padding: 1.5rem;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.try-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.try-panel__header h2 {
  margin: 0.25rem 0 0;
  font-size: 1.25rem;
}

.try-panel__eyebrow,
.result__label {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.try-panel__step {
  white-space: nowrap;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.status-banner,
.result,
.try-panel__empty {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
  background: rgba(255, 255, 255, 0.04);
}

.status-banner--warning,
.result--error {
  border-color: rgba(239, 68, 68, 0.45);
  background: rgba(239, 68, 68, 0.12);
}

.status-banner--ready {
  border-color: rgba(34, 197, 94, 0.35);
  background: rgba(34, 197, 94, 0.1);
}

.try-panel__form,
.try-panel__empty {
  display: grid;
  gap: 0.9rem;
}

label {
  display: grid;
  gap: 0.35rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 700;
}

input,
textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  background: var(--color-dark-900);
  color: var(--color-text);
  font: inherit;
}

textarea {
  resize: vertical;
}

button {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.7rem 0.95rem;
  background: var(--color-primary-500);
  color: white;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.result {
  display: grid;
  gap: 0.75rem;
}

.result p {
  margin: 0;
}

dl {
  display: grid;
  grid-template-columns: minmax(8rem, 0.45fr) 1fr;
  gap: 0.6rem 1rem;
  margin: 0;
}

dt {
  color: var(--color-text-muted);
  font-weight: 700;
}

dd {
  margin: 0;
  min-width: 0;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.86rem;
}

.page-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: auto;
  padding-top: 0.75rem;
}

.page-nav button {
  min-width: 2.4rem;
  padding: 0.5rem 0.7rem;
  background: transparent;
  color: var(--color-text);
}

.page-nav__button--active {
  background: var(--color-primary-500) !important;
  color: white !important;
}

@media (max-width: 900px) {
  dl {
    grid-template-columns: 1fr;
  }
}
</style>
