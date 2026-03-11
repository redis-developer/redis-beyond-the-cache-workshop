<template>
  <div class="search-home">
    <WorkshopHeader
      v-if="content"
      :hub-url="workshopHubUrl"
      :steps="headerSteps"
      :current-step="currentStageIndex"
      clickable
      @step-click="goToStage"
    />

    <div class="welcome-container">
      <div class="instructions-column">
        <div v-if="contentLoading" class="content-status">
          Loading workshop instructions...
        </div>

        <div v-else-if="contentError" class="content-status content-status--error">
          {{ contentError }}
        </div>

        <WorkshopContentRenderer
          v-else-if="content"
          :content="content"
          :active-stage-id="activeStageId"
          :show-title="false"
          :show-summary="false"
          :action-handlers="actionHandlers"
          @render-error="handleRenderIssues"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getWorkshopHubUrl } from '../utils/basePath';
import { WorkshopContentRenderer, WorkshopHeader } from '../utils/components';
import {
  fetchWorkshopContent,
  getContentStageIndex,
  getContentStageNavSteps,
  resolveContentStageId
} from '../utils/workshopContent';

export default {
  name: 'SearchHome',
  components: {
    WorkshopContentRenderer,
    WorkshopHeader
  },
  data() {
    return {
      content: null,
      contentLoading: true,
      contentError: '',
      activeStageId: ''
    };
  },
  async mounted() {
    await this.loadContent();
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    },
    headerSteps() {
      return getContentStageNavSteps(this.content).map(step => step.label);
    },
    currentStageIndex() {
      return getContentStageIndex(this.content, this.activeStageId);
    },
    actionHandlers() {
      return {
        setStage: ({ args }) => this.setStage(args.stageId),
        openEditor: () => this.openRoute('/editor'),
        openRoute: ({ args }) => this.openRoute(args.route),
        openHub: () => this.openHub()
      };
    }
  },
  methods: {
    async loadContent() {
      this.contentLoading = true;
      this.contentError = '';

      try {
        this.content = await fetchWorkshopContent('search-home');
        this.activeStageId = resolveContentStageId(this.content, this.activeStageId);
      } catch (error) {
        console.error('Error loading workshop content:', error);
        this.contentError = error.message || 'Failed to load workshop instructions.';
      } finally {
        this.contentLoading = false;
      }
    },
    setStage(stageId) {
      this.activeStageId = resolveContentStageId(this.content, stageId);
    },
    goToStage(stepNumber) {
      const stageSteps = getContentStageNavSteps(this.content);
      const targetStage = stageSteps[stepNumber - 1];

      if (targetStage) {
        this.setStage(targetStage.stageId);
      }
    },
    openRoute(route) {
      if (!route) {
        return;
      }

      this.$router.push(route).catch(() => {});
    },
    openHub() {
      window.open(this.workshopHubUrl, '_blank', 'noopener');
    },
    handleRenderIssues(issues) {
      if (issues?.length) {
        console.warn('SearchHome content render issues:', issues);
      }
    }
  }
};
</script>

<style scoped>
.search-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.welcome-container {
  max-width: 1080px;
  margin: 0 auto;
}

.instructions-column {
  min-width: 0;
}

.content-status {
  padding: var(--spacing-5);
  border-radius: var(--radius-lg);
  background: rgba(15, 23, 42, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--color-text-secondary);
}

.content-status--error {
  border-color: rgba(220, 56, 44, 0.4);
  color: #fca5a5;
}

@media (max-width: 768px) {
  .search-home {
    padding: var(--spacing-4);
  }
}
</style>
