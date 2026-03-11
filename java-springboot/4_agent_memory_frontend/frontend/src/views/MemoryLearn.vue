<template>
  <div class="memory-learn">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Home', 'Learn']"
      :current-step="1"
    />

    <div class="main-container">
      <section class="doc-surface">
        <WorkshopContentRenderer
          v-if="content"
          :content="content"
          :show-title="true"
          :show-summary="true"
          :show-stage-title="false"
          :action-handlers="actionHandlers"
        />

        <div v-else-if="loading" class="content-status">
          Loading workshop content...
        </div>

        <div v-else class="content-status content-status--error">
          {{ loadError }}
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { WorkshopContentRenderer, WorkshopHeader } from '../utils/components';
import { getRedisInsightUrl, getWorkshopHubUrl } from '../utils/basePath';
import { loadWorkshopContentView } from '../utils/workshopContent';

export default {
  name: 'MemoryLearn',
  components: { WorkshopContentRenderer, WorkshopHeader },
  data() {
    return {
      content: null,
      loading: true,
      loadError: ''
    };
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    },
    actionHandlers() {
      return {
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => window.open(getRedisInsightUrl(), '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route);
          }
        }
      };
    }
  },
  async mounted() {
    try {
      this.content = await loadWorkshopContentView('memory-learn');
    } catch (error) {
      this.loadError = error.message || 'Failed to load workshop content.';
    } finally {
      this.loading = false;
    }
  }
};
</script>

<style scoped>
.memory-learn {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.main-container {
  max-width: 900px;
  margin: 0 auto;
}

.doc-surface {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-5);
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
