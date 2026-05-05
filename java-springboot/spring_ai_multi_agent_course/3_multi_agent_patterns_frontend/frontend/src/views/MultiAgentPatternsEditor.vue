<template>
  <WorkshopCodeEditorShell
    title="Module 3: Redis Agent Memory"
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

export default {
  name: 'MultiAgentPatternsEditor',
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
      files: [
        'build.gradle.kts',
        'application.properties',
        'SpringAiMultiAgentsApplication.java',
        'MultiAgentsService.java',
        'AgentMemoryConfig.java',
        'AmsChatMemoryRepository.java',
        'LongTermMemoryAdvisor.java',
        'AgentMemoryService.java',
        'CoordinatorRoutingAgentConfig.java'
      ]
    }
  },
  computed: {
    backToWorkshopRoute() {
      const route = this.$route.query.returnTo
      if (typeof route === 'string' && /^\/[0-5]$/.test(route)) {
        return route
      }

      return '/0'
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
        recompileApp: () => this.recompileLearnerApp()
      }
    },
    editorContentId() {
      const requestedContentId = this.$route.query.content
      if (typeof requestedContentId === 'string' && /^[0-6]$/.test(requestedContentId)) {
        return requestedContentId
      }

      const returnTo = this.$route.query.returnTo
      const returnToMatch = typeof returnTo === 'string'
        ? returnTo.match(/^\/([0-5])$/)
        : null

      return returnToMatch?.[1] || '6'
    },
    editorTitle() {
      return this.content?.title || 'Redis Agent Memory Editor'
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
      if (!this.useEmbeddedEditor || !fileName) {
        return
      }

      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles()
      }

      const workspacePath = this.codeEditorFilePathByName[fileName]
      if (!workspacePath) {
        return
      }

      this.activeCodeEditorFilePath = workspacePath
      this.codeEditorOpenRequest += 1
    },
    async recompileLearnerApp() {
      const response = await fetch(getApiUrl('/internal/session-runner/restart'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ rebuild: true, async: true })
      })

      if (!response.ok) {
        throw new Error('Failed to recompile learner app')
      }
    }
  }
}
</script>

<style scoped>
.content-state {
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 0.5rem;
  padding: 1rem;
  color: var(--color-muted, #9fb0bf);
}

.content-state--error {
  border-color: rgba(220, 56, 44, 0.5);
  color: #ffb4ad;
}
</style>
