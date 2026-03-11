<template>
  <div class="memory-challenges">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="headerSteps"
      :current-step="currentStage - 1"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <section class="stage-content">
        <WorkshopContentRenderer
          v-if="content"
          :content="content"
          :active-stage-id="activeStageId"
          :show-title="false"
          :show-summary="currentStage === 1"
          :action-handlers="actionHandlers"
        />

        <div v-else-if="loading" class="content-status">
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
          @prev="prevStage"
          @next="nextStage"
        />
      </section>
    </div>
  </div>
</template>

<script>
import { WorkshopContentRenderer, WorkshopHeader, WorkshopStageNav } from '../utils/components';
import { getRedisInsightUrl, getWorkshopHubUrl } from '../utils/basePath';
import { loadProgress, saveProgress } from '../utils/progress';
import { loadWorkshopContentView } from '../utils/workshopContent';

const FALLBACK_STAGE_IDS = ['challenges', 'redis', 'ams'];
const FALLBACK_STEPS = ['Challenges', 'Why Redis', 'Agent Memory Server'];

export default {
  name: 'MemoryChallenges',
  components: { WorkshopContentRenderer, WorkshopHeader, WorkshopStageNav },
  data() {
    return {
      currentStage: 1,
      content: null,
      loading: true,
      loadError: ''
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
    actionHandlers() {
      return {
        openEditor: () => this.navigateTo('/editor'),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => window.open(getRedisInsightUrl(), '_blank', 'noopener'),
        openRoute: ({ args }) => this.navigateTo(args?.route),
        setStage: ({ args }) => this.setStageById(args?.stageId)
      };
    }
  },
  async mounted() {
    const progress = loadProgress();
    this.currentStage = progress.challengesStage || 1;
    await this.loadContent();
  },
  methods: {
    async loadContent() {
      this.loading = true;
      this.loadError = '';

      try {
        this.content = await loadWorkshopContentView('memory-challenges');
      } catch (error) {
        this.loadError = error.message || 'Failed to load workshop content.';
      } finally {
        this.loading = false;
      }
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
      progress.challengesStage = this.currentStage;
      saveProgress(progress);
    },
    navigateTo(route) {
      if (route) {
        this.$router.push(route);
      }
    }
  }
};
</script>

<style scoped>
.memory-challenges {
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
