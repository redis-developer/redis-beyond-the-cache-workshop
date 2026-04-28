<template>
  <main class="workshop-shell">
    <section class="workshop-shell__hero">
      <div>
        <p v-if="eyebrow" class="workshop-shell__eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p v-if="summary" class="workshop-shell__summary">{{ summary }}</p>
      </div>

      <slot name="hero-right"></slot>
    </section>

    <WorkshopRuntimeToolbar
      :runtime="runtime"
      :learner-app="resolvedLearnerApp"
      :links="links"
      :actions="actions"
      :disabled="runtimeActionsDisabled"
      @action="handleRuntimeAction"
    />

    <section class="workshop-shell__body">
      <article class="workshop-shell__instructions">
        <slot name="instructions">
          <WorkshopContentRenderer
            v-if="content"
            :content="content"
            :tokens="tokens"
            :context="context"
            :allowed-placeholder-paths="allowedPlaceholderPaths"
            :active-stage-id="activeStageId"
            :action-handlers="actionHandlers"
            :widgets="widgets"
            :widget-props="widgetProps"
            :show-title="showContentTitle"
            :show-summary="showContentSummary"
            :show-stage-title="showStageTitle"
            @action="handleContentAction"
            @render-error="$emit('render-error', $event)"
          />

          <div v-else class="workshop-shell__empty">
            <p>No workshop instructions are available yet.</p>
          </div>
        </slot>
      </article>

      <aside class="workshop-shell__app">
        <slot name="app-frame">
          <WorkshopAppFrame
            :src="shellState.learnerAppUrl"
            :title="resolvedLearnerApp.title || learnerAppTitle"
            :state="appFrameState"
            :message="appFrameMessage"
            :refresh-key="appRefreshKey"
            @load="$emit('app-load', $event)"
            @error="$emit('app-error', $event)"
            @retry="handleAppRetry"
          />
        </slot>
      </aside>
    </section>
  </main>
</template>

<script>
import { resolveWorkshopShellState } from '../composables/useWorkshopShellState.js';
import WorkshopAppFrame from './WorkshopAppFrame.vue';
import WorkshopContentRenderer from './WorkshopContentRenderer.vue';
import WorkshopRuntimeToolbar from './WorkshopRuntimeToolbar.vue';

export default {
  name: 'WorkshopShell',
  components: {
    WorkshopAppFrame,
    WorkshopContentRenderer,
    WorkshopRuntimeToolbar
  },
  props: {
    title: { type: String, required: true },
    eyebrow: { type: String, default: 'Workshop' },
    summary: { type: String, default: '' },
    content: { type: Object, default: null },
    tokens: { type: Object, default: () => ({}) },
    context: { type: Object, default: () => ({}) },
    allowedPlaceholderPaths: { type: Array, default: () => [] },
    activeStageId: { type: String, default: '' },
    actionHandlers: { type: Object, default: () => ({}) },
    widgets: { type: Object, default: () => ({}) },
    widgetProps: { type: Object, default: () => ({}) },
    runtime: { type: Object, default: () => ({}) },
    links: { type: Object, default: () => ({}) },
    actions: { type: [Array, Object], default: null },
    learnerApp: { type: Object, default: () => ({}) },
    learnerAppTitle: { type: String, default: 'Learner application' },
    runtimeActionsDisabled: { type: Boolean, default: false },
    showContentTitle: { type: Boolean, default: false },
    showContentSummary: { type: Boolean, default: true },
    showStageTitle: { type: Boolean, default: true }
  },
  emits: [
    'action',
    'runtime-action',
    'content-action',
    'render-error',
    'app-load',
    'app-error',
    'app-retry'
  ],
  data() {
    return {
      appRefreshKey: 0
    };
  },
  computed: {
    resolvedLearnerApp() {
      return {
        ...this.learnerApp,
        url: this.learnerApp.url ?? this.links.learnerApp
      };
    },
    shellState() {
      return resolveWorkshopShellState({
        runtime: this.runtime,
        learnerApp: this.resolvedLearnerApp,
        links: this.links,
        actions: this.actions
      });
    },
    appFrameState() {
      if (this.shellState.busy) {
        return this.shellState.runtimeState;
      }

      return this.shellState.frontendState;
    },
    appFrameMessage() {
      return this.resolvedLearnerApp.message || '';
    }
  },
  methods: {
    handleRuntimeAction(action) {
      this.$emit('runtime-action', action);
      this.$emit('action', { source: 'runtime', ...action });
    },
    handleContentAction(action) {
      this.$emit('content-action', action);
      this.$emit('action', { source: 'content', ...action });
    },
    handleAppRetry() {
      this.appRefreshKey += 1;
      this.$emit('app-retry');
    }
  }
};
</script>

<style scoped>
.workshop-shell {
  min-height: 100vh;
  padding: var(--spacing-6, 1.5rem);
  background:
    radial-gradient(circle at 8% 0%, rgba(0, 188, 212, 0.12), transparent 24rem),
    radial-gradient(circle at 100% 12%, rgba(220, 56, 44, 0.1), transparent 26rem),
    var(--color-background, #0A151B);
  color: var(--color-text, #e2e8f0);
}

.workshop-shell__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--spacing-4, 1rem);
  max-width: 112rem;
  margin: 0 auto var(--spacing-4, 1rem);
}

.workshop-shell__eyebrow {
  margin: 0 0 var(--spacing-2, 0.5rem);
  color: var(--color-primary-300, #4dd0e1);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.workshop-shell__hero h1 {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: clamp(2rem, 4vw, 4rem);
  line-height: 1;
}

.workshop-shell__summary {
  max-width: 48rem;
  margin: var(--spacing-3, 0.75rem) 0 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-base, 1rem);
  line-height: 1.7;
}

.workshop-shell > .workshop-runtime-toolbar,
.workshop-shell__body {
  max-width: 112rem;
  margin-right: auto;
  margin-left: auto;
}

.workshop-shell__body {
  display: grid;
  grid-template-columns: minmax(22rem, 0.9fr) minmax(32rem, 1.25fr);
  gap: var(--spacing-4, 1rem);
  align-items: stretch;
  margin-top: var(--spacing-4, 1rem);
}

.workshop-shell__instructions {
  min-width: 0;
  padding: var(--spacing-4, 1rem);
  overflow: auto;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: rgba(13, 26, 34, 0.88);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-shell__app {
  min-width: 0;
}

.workshop-shell__empty {
  display: grid;
  min-height: 18rem;
  place-items: center;
  color: var(--color-text-secondary, #94a3b8);
  text-align: center;
}

@media (max-width: 1180px) {
  .workshop-shell__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .workshop-shell {
    padding: var(--spacing-4, 1rem);
  }

  .workshop-shell__hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
