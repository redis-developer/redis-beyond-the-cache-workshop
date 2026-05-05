<template>
  <WorkshopCodeEditorShell
    title="Module 2: Building Multi Agents"
    :editor-title="editorTitle"
    :editor-src="codeEditorUrl"
    :back-to-workshop-url="backToWorkshopUrl"
    :redis-insight-url="redisInsightUrl"
    :hub-url="workshopHubUrl"
    :use-embedded-editor="useEmbeddedEditor"
  >
    <template #instructions>
      <div v-if="contentError" class="content-state content-state--error">
        {{ contentError }}
      </div>
      <div v-else-if="!content" class="content-state">
        Loading editor instructions...
      </div>
      <div v-if="editorNotice" class="content-state content-state--notice">
        {{ editorNotice }}
      </div>
      <WorkshopContentRenderer
        v-if="content"
        :content="content"
        :action-handlers="contentActionHandlers"
        :show-title="false"
        :show-summary="false"
        :show-stage-title="false"
      />
    </template>

    <template #fallback>
      <div class="legacy-editor-fallback">
        <WorkshopEditorLayout
          :title="editorTitle"
          :files="files"
          show-session-restart-controls
        >
          <template #instructions>
            <div v-if="contentError" class="content-state content-state--error">
              {{ contentError }}
            </div>
            <div v-else-if="!content" class="content-state">
              Loading editor instructions...
            </div>
            <WorkshopContentRenderer
              v-else
              :content="content"
              :action-handlers="contentActionHandlers"
              :show-title="false"
              :show-summary="false"
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
  getApiUrl,
  getBasePath,
  getCodeEditorFileUrl,
  getCodeEditorUrl,
  getWorkshopHubUrl,
  loadEditorWorkspaceMetadata,
  WorkshopCodeEditorShell,
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

function buildRouteUrl(routePath) {
  const basePath = getBasePath()
  const normalizedRoutePath = routePath.startsWith('/') ? routePath : `/${routePath}`

  if (!basePath || basePath === '/') {
    return normalizedRoutePath
  }

  return `${basePath}${normalizedRoutePath}`
}

function showEmbeddedEditorNotice(view, target) {
  view.editorNotice = target
    ? `Open ${target} in VS Code and save manual edits there.`
    : 'Use VS Code to edit and save manual changes.'
}

export default {
  name: 'BuildingMultiAgentsEditor',
  components: {
    WorkshopCodeEditorShell,
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      activeCodeEditorFilePath: '',
      codeEditorFilePathByName: {},
      codeEditorOpenRequest: 0,
      codeEditorWorkspaceRoot: '',
      content: null,
      contentError: '',
      editorNotice: '',
      files: [
        'build.gradle.kts',
        'application.properties',
        'SpringAiMultiAgentsApplication.java',
        'MultiAgentsService.java',
        'MarketDataTools.java',
        'FundamentalsTools.java',
        'NewsTools.java',
        'MarketDataAgentConfig.java',
        'MarketDataAgent.java',
        'MarketDataResult.java',
        'MarketSnapshot.java',
        'FundamentalsAgentConfig.java',
        'FundamentalsAgent.java',
        'FundamentalsResult.java',
        'FundamentalsSnapshot.java',
        'NewsAgentConfig.java',
        'NewsAgent.java',
        'NewsResult.java',
        'NewsSnapshot.java',
        'SynthesisAgentConfig.java',
        'SynthesisAgent.java',
        'SynthesisResponse.java',
        'SynthesisResult.java',
        'RoutingDecision.java',
        'CoordinatorRoutingAgentConfig.java',
        'CoordinatorRoutingAgent.java',
        'CoordinatorAgent.java',
        'AgentOrchestrationService.java',
        'AgentType.java',
        'AnalysisRequest.java',
        'AnalysisResponse.java'
      ]
    }
  },
  computed: {
    backToWorkshopRoute() {
      const route = this.$route.query.returnTo
      if (typeof route === 'string' && /^\/[0-5]$/.test(route)) {
        return route
      }

      return '/5'
    },
    backToWorkshopUrl() {
      return buildRouteUrl(this.backToWorkshopRoute)
    },
    codeEditorUrl() {
      if (this.activeCodeEditorFilePath) {
        return getCodeEditorFileUrl(this.activeCodeEditorFilePath, {
          workspaceRoot: this.codeEditorWorkspaceRoot,
          requestId: this.codeEditorOpenRequest
        })
      }

      return getCodeEditorUrl({ workspaceRoot: this.codeEditorWorkspaceRoot })
    },
    contentActionHandlers() {
      return {
        openFile: ({ args }) => this.openEmbeddedEditorFile(args?.file),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route)
          }
        },
        recompileApp: () => this.recompileLearnerApp(),
        saveFile: () => showEmbeddedEditorNotice(this)
      }
    },
    editorContentId() {
      const requestedContentId = this.$route.query.content
      if (typeof requestedContentId === 'string' && /^[0-5]$/.test(requestedContentId)) {
        return requestedContentId
      }

      const returnTo = this.$route.query.returnTo
      const returnToMatch = typeof returnTo === 'string'
        ? returnTo.match(/^\/([0-5])$/)
        : null

      return returnToMatch?.[1] || '5'
    },
    editorTitle() {
      return this.content?.title || 'Building Multi Agents Editor'
    },
    redisInsightUrl() {
      return buildRouteUrl(`${this.backToWorkshopRoute}?tool=redis-insight`)
    },
    useEmbeddedEditor() {
      return this.$route.query.editor !== 'legacy'
    },
    workshopHubUrl() {
      return getWorkshopHubUrl()
    }
  },
  watch: {
    '$route.query.file': {
      async handler(fileName) {
        if (typeof fileName === 'string' && fileName) {
          await this.openEmbeddedEditorFile(fileName)
        }
      }
    },
    editorContentId: {
      async handler() {
        this.contentError = ''
        this.content = null
        await this.loadContent()
      }
    }
  },
  async mounted() {
    await Promise.all([
      this.loadContent(),
      this.loadCodeEditorFiles()
    ])

    const requestedFile = this.$route.query.file
    if (typeof requestedFile === 'string' && requestedFile) {
      await this.openEmbeddedEditorFile(requestedFile)
    }
  },
  methods: {
    async loadCodeEditorFiles() {
      if (!this.useEmbeddedEditor) {
        return
      }

      try {
        const metadata = await loadEditorWorkspaceMetadata()
        this.codeEditorFilePathByName = metadata.filePathMap
        this.codeEditorWorkspaceRoot = metadata.codeEditorWorkspaceRoot || metadata.workspaceRoot
      } catch (error) {
        console.warn('Failed to load code editor file metadata:', error)
      }
    },
    async loadContent() {
      try {
        this.content = await fetchWorkshopContent(this.editorContentId)
      } catch (error) {
        console.error('Error loading editor content:', error)
        this.contentError = 'Unable to load editor instructions. Refresh the page and try again.'
      }
    },
    async openEmbeddedEditorFile(fileName) {
      if (!this.useEmbeddedEditor) {
        showEmbeddedEditorNotice(this, fileName)
        return
      }

      if (!fileName) {
        showEmbeddedEditorNotice(this)
        return
      }

      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles()
      }

      const workspacePath = this.codeEditorFilePathByName[fileName]
      if (!workspacePath) {
        this.editorNotice = `Could not resolve ${fileName} from the workshop manifest. Open it manually in VS Code.`
        return
      }

      this.codeEditorOpenRequest += 1
      this.activeCodeEditorFilePath = workspacePath
      this.editorNotice = `Opening ${fileName} in VS Code.`
    },
    async recompileLearnerApp() {
      this.editorNotice = 'Recompiling and restarting the learner app...'

      try {
        const response = await fetch(getApiUrl('/internal/session-runner/restart'), {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ rebuild: true, async: true })
        })
        const data = await this.readResponseBody(response)

        if (!response.ok || data.error) {
          throw new Error(data.error || data.message || 'Failed to recompile app')
        }

        this.editorNotice = 'Recompile started. Return to the learner app after it is ready.'
      } catch (error) {
        this.editorNotice = `Could not recompile app: ${error.message}`
      }
    },
    async readResponseBody(response) {
      const text = await response.text()

      if (!text) {
        return {}
      }

      try {
        return JSON.parse(text)
      } catch {
        return {
          error: response.ok ? '' : text
        }
      }
    }
  }
}
</script>

<style scoped>
.content-state {
  color: #cccccc;
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
}

.content-state--notice {
  background: rgba(59, 130, 246, 0.14);
  border: 1px solid rgba(59, 130, 246, 0.32);
  border-radius: var(--radius-lg, 8px);
  color: #bfdbfe;
  margin-bottom: var(--spacing-4, 1rem);
  padding: var(--spacing-3, 0.75rem);
}

.legacy-editor-fallback {
  height: calc(100vh - 72px);
}

.legacy-editor-fallback :deep(.workshop-editor),
.legacy-editor-fallback :deep(.main-container) {
  height: 100%;
  width: 100%;
}

:deep(.content-editor-step-item__hint),
:deep(.content-section__header .workshop-markdown),
:deep(.workshop-markdown) {
  color: #cccccc;
}
</style>
