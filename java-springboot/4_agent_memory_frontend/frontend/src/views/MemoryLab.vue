<template>
  <div class="memory-lab">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="headerSteps"
      :current-step="currentStage - 1"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <div class="status-bar" :class="{ connected: serverStatus === 'connected' }">
        <span class="status-dot"></span>
        <span>Agent Memory Server: {{ serverStatus }}</span>
        <button @click="checkHealth" class="btn btn-sm">Refresh</button>
      </div>

      <section class="stage-content">
        <WorkshopContentRenderer
          v-if="content"
          :content="content"
          :active-stage-id="activeStageId"
          :show-title="false"
          :show-summary="currentStage === 1"
          :action-handlers="actionHandlers"
          :widgets="widgets"
          :widget-props="widgetProps"
        />

        <div v-else-if="loadingContent" class="content-status">
          Loading workshop content...
        </div>

        <div v-else class="content-status content-status--error">
          {{ loadError }}
        </div>

        <WorkshopStageNav
          v-if="content"
          :show-prev="currentStage > 1"
          :show-next="currentStage < stageCount"
          :next-text="nextStageLabel"
          :disable-next="currentStage === 1 && !checks.ams"
          @prev="prevStage"
          @next="nextStage"
        />
      </section>
    </div>
  </div>
</template>

<script>
import { WorkshopContentRenderer, WorkshopHeader, WorkshopStageNav } from '../utils/components';
import { getApiUrl, getRedisInsightUrl, getWorkshopHubUrl } from '../utils/basePath';
import { loadProgress, saveProgress } from '../utils/progress';
import { loadWorkshopContentView } from '../utils/workshopContent';
import MemoryLabContextWidget from '../components/widgets/MemoryLabContextWidget.vue';
import MemoryLabLongTermMemoryWidget from '../components/widgets/MemoryLabLongTermMemoryWidget.vue';
import MemoryLabSetupWidget from '../components/widgets/MemoryLabSetupWidget.vue';
import MemoryLabWorkingMemoryWidget from '../components/widgets/MemoryLabWorkingMemoryWidget.vue';

const FALLBACK_STAGE_IDS = ['setup', 'working-memory', 'long-term-memory', 'context'];
const FALLBACK_STEPS = ['Setup', 'Working Memory', 'Long-Term Memory', 'Test'];

export default {
  name: 'MemoryLab',
  components: { WorkshopContentRenderer, WorkshopHeader, WorkshopStageNav },
  data() {
    return {
      currentStage: 1,
      content: null,
      loadingContent: true,
      loadError: '',
      serverStatus: 'checking...',
      loading: false,
      checks: { ams: false, redis: false },
      exercise1: {
        sessionId: `lab-session-${Date.now()}`,
        userId: 'alice',
        userMessage: "Hi, I'm Alice and I work at TechCorp",
        assistantMessage: 'Nice to meet you, Alice! How can I help you today?',
        result: null
      },
      exercise2: {
        userId: 'alice',
        memoryText: 'Alice prefers video calls over phone calls',
        searchQuery: 'how does the user like to communicate?',
        searchResults: []
      },
      exercise3: {
        newMessage: 'Can you schedule a meeting for tomorrow?',
        context: null
      }
    };
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    },
    headerSteps() {
      return this.content?.header?.stageNav?.steps?.map(step => step.label) || FALLBACK_STEPS;
    },
    stageIds() {
      return this.content?.stages?.map(stage => stage.stageId) || FALLBACK_STAGE_IDS;
    },
    stageCount() {
      return this.stageIds.length;
    },
    activeStageId() {
      return this.stageIds[this.currentStage - 1] || this.stageIds[0] || '';
    },
    nextStageLabel() {
      return this.headerSteps[this.currentStage] || 'Continue';
    },
    widgets() {
      return {
        'memory-lab.setup': MemoryLabSetupWidget,
        'memory-lab.working-memory': MemoryLabWorkingMemoryWidget,
        'memory-lab.long-term-memory': MemoryLabLongTermMemoryWidget,
        'memory-lab.context-builder': MemoryLabContextWidget
      };
    },
    widgetProps() {
      return {
        'memory-lab.setup': {
          checks: this.checks,
          checkAMS: this.checkAMS
        },
        'memory-lab.working-memory': {
          exercise: this.exercise1,
          loading: this.loading,
          updateExercise: this.updateExercise1,
          storeWorkingMemory: this.storeWorkingMemory,
          retrieveWorkingMemory: this.retrieveWorkingMemory
        },
        'memory-lab.long-term-memory': {
          exercise: this.exercise2,
          loading: this.loading,
          updateExercise: this.updateExercise2,
          storeLongTermMemory: this.storeLongTermMemory,
          searchMemories: this.searchMemories
        },
        'memory-lab.context-builder': {
          exercise: this.exercise3,
          loading: this.loading,
          updateExercise: this.updateExercise3,
          buildContext: this.buildContext
        }
      };
    },
    actionHandlers() {
      return {
        openEditor: () => this.$router.push('/editor'),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => window.open(getRedisInsightUrl(), '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route);
          }
        },
        setStage: ({ args }) => this.setStageById(args?.stageId)
      };
    }
  },
  async mounted() {
    const progress = loadProgress();
    this.currentStage = progress.labStage || 1;
    await this.loadContent();
    this.checkHealth();
  },
  methods: {
    async loadContent() {
      this.loadingContent = true;
      this.loadError = '';

      try {
        this.content = await loadWorkshopContentView('memory-lab');
      } catch (error) {
        this.loadError = error.message || 'Failed to load workshop content.';
      } finally {
        this.loadingContent = false;
      }
    },
    async checkHealth() {
      try {
        const response = await fetch(getApiUrl('/api/memory/health'));
        const data = await response.json();
        this.serverStatus = data.status === 'ok' ? 'connected' : 'error';
        this.checks.ams = data.status === 'ok';
        this.checks.redis = data.status === 'ok';
      } catch {
        this.serverStatus = 'disconnected';
        this.checks.ams = false;
        this.checks.redis = false;
      }
    },
    async checkAMS() {
      await this.checkHealth();
    },
    nextStage() {
      if (this.currentStage < this.stageCount) {
        this.currentStage += 1;
        this.saveState();
      }
    },
    prevStage() {
      if (this.currentStage > 1) {
        this.currentStage -= 1;
        this.saveState();
      }
    },
    goToStage(step) {
      if (step >= 1 && step <= this.stageCount) {
        this.currentStage = step;
        this.saveState();
      }
    },
    setStageById(stageId) {
      const stageIndex = this.stageIds.indexOf(stageId);
      if (stageIndex >= 0) {
        this.currentStage = stageIndex + 1;
        this.saveState();
      }
    },
    saveState() {
      const progress = loadProgress();
      progress.labStage = this.currentStage;
      saveProgress(progress);
    },
    updateExercise1(field, value) {
      this.exercise1[field] = value;
    },
    updateExercise2(field, value) {
      this.exercise2[field] = value;
    },
    updateExercise3(field, value) {
      this.exercise3[field] = value;
    },
    async storeWorkingMemory() {
      this.loading = true;
      try {
        await fetch(
          getApiUrl(`/api/demo/conversation?userId=${encodeURIComponent(this.exercise1.userId)}&sessionId=${encodeURIComponent(this.exercise1.sessionId)}`),
          { method: 'POST' }
        );
        await fetch(
          getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}/message?role=user&content=${encodeURIComponent(this.exercise1.userMessage)}`),
          { method: 'POST' }
        );
        await fetch(
          getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}/message?role=assistant&content=${encodeURIComponent(this.exercise1.assistantMessage)}`),
          { method: 'POST' }
        );
        this.exercise1.result = {
          success: true,
          data: { sessionId: this.exercise1.sessionId, messages: 2 }
        };
      } catch (error) {
        this.exercise1.result = {
          success: false,
          data: { error: error.message }
        };
      } finally {
        this.loading = false;
      }
    },
    async retrieveWorkingMemory() {
      this.loading = true;
      try {
        const response = await fetch(
          getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}`)
        );
        const data = await response.json();
        this.exercise1.result = { success: data.success, data };
      } catch (error) {
        this.exercise1.result = {
          success: false,
          data: { error: error.message }
        };
      } finally {
        this.loading = false;
      }
    },
    async storeLongTermMemory() {
      this.loading = true;
      try {
        await fetch(
          getApiUrl(`/api/demo/preference?userId=${encodeURIComponent(this.exercise2.userId)}&preference=${encodeURIComponent(this.exercise2.memoryText)}`),
          { method: 'POST' }
        );
      } finally {
        this.loading = false;
      }
    },
    async searchMemories() {
      this.loading = true;
      try {
        const response = await fetch(
          getApiUrl(`/api/demo/preferences?userId=${encodeURIComponent(this.exercise2.userId)}&query=${encodeURIComponent(this.exercise2.searchQuery)}`)
        );
        const data = await response.json();
        this.exercise2.searchResults = data.memories || [];
      } finally {
        this.loading = false;
      }
    },
    async buildContext() {
      this.loading = true;
      try {
        const workingMemoryResponse = await fetch(
          getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}`)
        );
        const workingMemoryData = await workingMemoryResponse.json();
        const memoryResponse = await fetch(
          getApiUrl(`/api/demo/preferences?userId=${encodeURIComponent(this.exercise2.userId)}&query=${encodeURIComponent(this.exercise3.newMessage)}`)
        );
        const memoryData = await memoryResponse.json();
        this.exercise3.context = {
          workingMemory: JSON.stringify(workingMemoryData.messages || [], null, 2),
          relevantMemories: JSON.stringify((memoryData.memories || []).map(memory => memory.text), null, 2)
        };
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.memory-lab {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.main-container {
  max-width: 900px;
  margin: 0 auto;
  padding-top: var(--spacing-4);
}

.stage-content {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.status-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  background: var(--color-surface);
  padding: var(--spacing-3);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-4);
  color: var(--color-text-secondary);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
}

.status-bar.connected .status-dot {
  background: #10b981;
}

.btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-sm {
  padding: var(--spacing-1) var(--spacing-2);
  font-size: var(--font-size-xs);
  background: var(--color-dark-800);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.content-status {
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  background: rgba(59, 130, 246, 0.12);
  color: var(--color-text);
}

.content-status--error {
  background: rgba(239, 68, 68, 0.18);
}
</style>
