<template>
  <main class="workshop-shell">
    <header class="workshop-shell__top-bar">
      <div class="workshop-shell__top-bar-content">
        <div class="workshop-shell__brand" aria-label="Redis workshop">
          <svg class="workshop-shell__logo" width="84" height="28" viewBox="0 0 84 28" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
            <g clip-path="url(#workshop-shell-redis-logo)">
              <path d="M68.7322 7.08959C69.5294 5.68591 70.8337 4.17426 71.3047 3.70636C73.4786 4.60616 75.5076 6.44173 75.2178 6.90963C74.3845 8.27731 73.1163 9.82496 72.6453 10.2929C70.4714 9.39306 68.4424 7.59347 68.7322 7.08959ZM84.0584 13.2802C83.8048 14.5039 82.283 15.8716 81.6309 16.0875C81.0874 14.9358 80.4714 14.2519 79.8917 14.2519C79.1671 14.2519 79.1309 14.7558 79.1309 15.4037C79.1309 16.5554 79.9642 19.0748 79.9642 21.7022C79.9642 24.5816 77.9352 26.7051 74.8192 26.7051C71.966 26.7051 70.3893 24.8471 69.686 21.8788C67.8221 25.1966 65.0965 26.7051 63.0076 26.7051C59.7418 26.7051 58.9732 24.3072 59.0507 21.8752C57.7384 24.1789 55.2126 26.7051 52.7901 26.7051C50.3173 26.7051 49.4438 24.567 49.6441 22.077C48.1625 24.8184 45.4824 26.7051 42.8988 26.7051C40.0951 26.7051 38.7074 24.4926 39.1565 21.7503C37.2703 24.0532 33.7592 26.7051 30.1089 26.7051C25.9467 26.7051 24.1349 24.4756 23.9202 21.6818C21.9113 24.8801 19.2036 26.8131 15.9783 26.8131C11.3225 26.8131 9.65719 22.7003 9.41486 19.3362C7.68934 21.6315 5.74936 24.0129 3.36958 26.6691C3.11596 26.921 2.89857 27.065 2.64494 27.065C1.8116 27.065 0.108696 23.3938 0 22.0262C0.966448 20.5364 8.86019 12.074 12.1502 8.42954C9.92766 9.09574 7.63616 10.4244 4.7464 12.4883C4.23915 12.8483 2.8261 9.57302 2.86233 7.05359C6.19568 4.60616 11.2682 3.05851 15.3624 3.05851C21.0871 3.05851 24.3842 6.22578 24.3842 10.6168C24.3842 14.2879 21.3045 18.319 16.8117 18.463C14.4756 18.5233 12.9784 17.2206 12.2126 15.612C12.3041 18.0999 13.6062 21.1624 17.1015 21.1624C21.1595 21.1624 22.9711 18.5709 26.0146 14.7918C28.3335 11.9485 31.0147 9.42905 34.9277 9.42905C37.319 9.42905 38.9495 10.9047 38.9495 13.1362C38.9495 15.8356 35.7611 19.5787 31.3045 19.5787C30.5434 19.5787 29.8496 19.4791 29.2639 19.2822C29.2492 19.3957 29.2393 19.5073 29.2393 19.6147C29.2393 20.8744 29.7103 21.6302 31.7755 21.6302C34.819 21.6302 37.6814 19.8307 41.1596 15.6196C44.5654 11.4806 47.1379 9.68099 49.8553 9.68099C51.6897 9.68099 53.0819 10.6686 53.6955 12.3318C57.3374 7.10672 60.4269 3.40454 63.0438 0.934999C65.6163 2.01475 67.4641 4.13826 66.9569 4.57016C65.0366 6.29777 58.6235 13.2442 56.0872 17.3832C55.4351 18.463 54.8191 19.6507 54.8191 20.2266C54.8191 20.7664 55.1452 20.9464 55.5075 20.9464C57.8989 20.9464 63.5873 13.2442 66.8119 9.86095C68.8409 10.6888 70.9062 12.4524 70.3989 13.0642C67.7177 16.2315 65.6888 18.8229 65.6888 20.2986C65.6888 20.6945 65.8337 20.9464 66.3772 20.9464C67.3917 20.9464 68.3337 20.0466 69.8917 18.139C70.2178 17.7431 70.6163 17.7431 70.8699 18.355C71.5583 20.0106 72.5728 20.9104 73.3699 20.9104C74.312 20.9104 74.783 20.0826 74.783 18.8229C74.783 17.3112 74.4569 15.3677 74.4569 14.5039C74.4569 11.5886 76.6308 9.89694 79.3482 9.89694C81.3772 9.89694 83.1888 10.8687 84.0584 13.2802ZM17.0795 8.03019C15.8592 9.92471 14.7234 11.6945 13.576 13.4203C14.1996 13.7682 14.9881 14.036 16.0146 14.036C17.9349 14.036 20.0363 12.9922 20.0363 10.8687C20.0363 9.58003 19.2312 8.39196 17.0795 8.03019ZM29.8151 17.6954C30.1959 17.8415 30.6429 17.9231 31.1234 17.9231C33.6958 17.9231 35.435 15.9795 35.435 14.6838C35.435 14.108 35.0726 13.7121 34.4929 13.7121C33.0393 13.7121 30.8467 15.7385 29.8151 17.6954ZM51.3771 15.0078C51.3771 14.2879 50.9785 13.856 50.3264 13.856C48.1887 13.856 44.964 17.8871 44.964 19.9026C44.964 20.5505 45.3263 20.9824 46.0872 20.9824C48.4423 20.9824 51.3771 16.7354 51.3771 15.0078Z" fill="#FF4438"/>
            </g>
            <defs>
              <clipPath id="workshop-shell-redis-logo">
                <rect width="84" height="28" fill="white"/>
              </clipPath>
            </defs>
          </svg>
          <h1>{{ title }}</h1>
        </div>

        <nav
          v-if="shellState.canOpenRedisInsight || shellState.canOpenEditor || shellState.canOpenHub"
          class="workshop-shell__header-actions"
          aria-label="Workshop navigation"
        >
          <a
            v-if="shellState.canOpenRedisInsight && !redisInsightInPlace"
            class="workshop-shell__header-action"
            :href="shellState.redisInsightUrl"
            @click="handleHeaderLinkAction('redisInsight')"
          >
            {{ redisInsightLabel }}
          </a>
          <button
            v-if="shellState.canOpenRedisInsight && redisInsightInPlace"
            type="button"
            class="workshop-shell__header-action"
            @click="handleHeaderLinkAction('redisInsight')"
          >
            {{ redisInsightLabel }}
          </button>
          <a
            v-if="shellState.canOpenEditor"
            class="workshop-shell__header-action"
            :href="shellState.editorUrl"
            @click="handleHeaderLinkAction('editor')"
          >
            Code Editor
          </a>
          <a
            v-if="shellState.canOpenHub"
            class="workshop-shell__header-action"
            :href="shellState.hubUrl"
            @click="handleHeaderLinkAction('hub')"
          >
            Back to Hub
          </a>
        </nav>
      </div>
    </header>

    <section v-if="hasHeroContent" class="workshop-shell__hero">
      <div>
        <p v-if="eyebrow" class="workshop-shell__eyebrow">{{ eyebrow }}</p>
        <p v-if="summary" class="workshop-shell__summary">{{ summary }}</p>
      </div>

      <slot name="hero-right"></slot>
    </section>

    <section
      ref="shellBody"
      class="workshop-shell__body"
      :class="{ 'workshop-shell__body--resizing': isResizingShell }"
      :style="shellBodyStyle"
    >
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

      <button
        type="button"
        class="workshop-shell__resize-handle"
        :class="{ 'workshop-shell__resize-handle--active': isResizingShell }"
        aria-label="Resize instructions and learner app panels"
        title="Drag to resize panels"
        @pointerdown="startShellResize"
      ></button>

      <aside class="workshop-shell__app">
        <slot name="app-frame">
          <WorkshopAppFrame
            :src="shellState.learnerAppUrl"
            :title="resolvedLearnerApp.title || learnerAppTitle"
            :state="appFrameState"
            :message="appFrameMessage"
            :refresh-key="appRefreshKey"
            :can-restart="shellState.canRestart"
            :can-rebuild="shellState.canRebuild"
            :actions-disabled="runtimeActionsDisabled"
            @load="$emit('app-load', $event)"
            @error="$emit('app-error', $event)"
            @retry="handleAppRetry"
            @runtime-action="handleRuntimeAction"
          />
        </slot>
      </aside>
    </section>

    <iframe
      v-if="shouldPreloadEditorFrame"
      class="workshop-shell__editor-preload"
      :src="editorPreloadUrl"
      title="Preloaded code editor"
      aria-hidden="true"
      tabindex="-1"
    ></iframe>
  </main>
</template>

<script>
import { resolveWorkshopShellState } from '../composables/useWorkshopShellState.js';
import WorkshopAppFrame from './WorkshopAppFrame.vue';
import WorkshopContentRenderer from './WorkshopContentRenderer.vue';

export default {
  name: 'WorkshopShell',
  components: {
    WorkshopAppFrame,
    WorkshopContentRenderer
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
    redisInsightInPlace: { type: Boolean, default: false },
    redisInsightLabel: { type: String, default: 'Redis Insight' },
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
      appRefreshKey: 0,
      instructionsPanelPercent: 42,
      isResizingShell: false,
      editorPreloadEnabled: false,
      editorPreloadTimer: null,
      editorPreloadTimerType: ''
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
    },
    hasHeroContent() {
      return Boolean(this.eyebrow || this.summary || this.$slots['hero-right']);
    },
    shellBodyStyle() {
      return {
        gridTemplateColumns: `minmax(22rem, ${this.instructionsPanelPercent}fr) 0.75rem minmax(32rem, ${100 - this.instructionsPanelPercent}fr)`
      };
    },
    editorPreloadUrl() {
      return this.shellState.editorUrl || '';
    },
    canPreloadEditorFrame() {
      return this.shellState.canOpenEditor && Boolean(this.editorPreloadUrl);
    },
    shouldPreloadEditorFrame() {
      return this.editorPreloadEnabled && this.canPreloadEditorFrame;
    }
  },
  watch: {
    canPreloadEditorFrame() {
      this.scheduleEditorPreload();
    },
    editorPreloadUrl() {
      this.scheduleEditorPreload();
    }
  },
  mounted() {
    this.restoreShellSplit();
    this.scheduleEditorPreload();
  },
  beforeUnmount() {
    this.stopShellResize();
    this.clearEditorPreloadTimer();
  },
  methods: {
    clampShellSplit(percent) {
      const minPercent = 25;
      const maxPercent = 70;
      return Math.min(Math.max(percent, minPercent), maxPercent);
    },
    handleRuntimeAction(action) {
      this.$emit('runtime-action', action);
      this.$emit('action', { source: 'runtime', ...action });
    },
    handleContentAction(action) {
      this.$emit('content-action', action);
      this.$emit('action', { source: 'content', ...action });
    },
    handleHeaderLinkAction(type) {
      this.$emit('runtime-action', { type });
      this.$emit('action', { source: 'runtime', type });
    },
    handleAppRetry() {
      this.appRefreshKey += 1;
      this.$emit('app-retry');
    },
    scheduleEditorPreload() {
      this.clearEditorPreloadTimer();
      this.editorPreloadEnabled = false;

      if (!this.canPreloadEditorFrame) {
        return;
      }

      const preload = () => {
        this.editorPreloadTimer = null;
        this.editorPreloadTimerType = '';

        if (this.canPreloadEditorFrame) {
          this.editorPreloadEnabled = true;
        }
      };

      if (typeof window.requestIdleCallback === 'function') {
        this.editorPreloadTimer = window.requestIdleCallback(preload, { timeout: 2500 });
        this.editorPreloadTimerType = 'idle';
        return;
      }

      this.editorPreloadTimer = window.setTimeout(preload, 1200);
      this.editorPreloadTimerType = 'timeout';
    },
    clearEditorPreloadTimer() {
      if (!this.editorPreloadTimer) {
        return;
      }

      if (this.editorPreloadTimerType === 'idle' && typeof window.cancelIdleCallback === 'function') {
        window.cancelIdleCallback(this.editorPreloadTimer);
      } else {
        window.clearTimeout(this.editorPreloadTimer);
      }

      this.editorPreloadTimer = null;
      this.editorPreloadTimerType = '';
    },
    onShellResize(event) {
      if (!this.isResizingShell || !this.$refs.shellBody) {
        return;
      }

      const rect = this.$refs.shellBody.getBoundingClientRect();
      if (rect.width <= 0) {
        return;
      }

      const nextPercent = ((event.clientX - rect.left) / rect.width) * 100;
      this.instructionsPanelPercent = this.clampShellSplit(nextPercent);
    },
    persistShellSplit() {
      localStorage.setItem('workshopShellInstructionsPanelPercent', String(this.instructionsPanelPercent));
    },
    restoreShellSplit() {
      const savedPercent = Number.parseFloat(localStorage.getItem('workshopShellInstructionsPanelPercent') || '');
      if (!Number.isNaN(savedPercent)) {
        this.instructionsPanelPercent = this.clampShellSplit(savedPercent);
      }
    },
    startShellResize(event) {
      event.preventDefault();
      this.isResizingShell = true;
      window.addEventListener('pointermove', this.onShellResize);
      window.addEventListener('pointerup', this.stopShellResize);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    },
    stopShellResize() {
      if (!this.isResizingShell) {
        return;
      }

      this.isResizingShell = false;
      window.removeEventListener('pointermove', this.onShellResize);
      window.removeEventListener('pointerup', this.stopShellResize);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      this.persistShellSplit();
    }
  }
};
</script>

<style scoped>
.workshop-shell {
  --workshop-shell-gutter: var(--spacing-6, 1.5rem);
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background: var(--color-background, #0A151B);
  color: var(--color-text, #e2e8f0);
}

.workshop-shell__top-bar {
  align-items: center;
  background-color: var(--color-dark-800, #0D1A22);
  border-bottom: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  display: flex;
  flex: 0 0 auto;
  height: 72px;
  position: sticky;
  top: 0;
  z-index: var(--z-sticky, 100);
}

.workshop-shell__top-bar-content {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  justify-content: space-between;
  padding: 0 var(--workshop-shell-gutter);
  width: 100%;
}

.workshop-shell__brand {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  min-width: 0;
}

.workshop-shell__logo {
  flex: 0 0 auto;
}

.workshop-shell__brand h1 {
  color: var(--color-text, #e2e8f0);
  font-size: clamp(1.125rem, 2vw, 1.5rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: -0.01em;
  line-height: 1.2;
  margin: 0;
  min-width: 0;
}

.workshop-shell__header-actions {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  gap: var(--spacing-2, 0.5rem);
}

.workshop-shell__header-action {
  align-items: center;
  appearance: none;
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  cursor: pointer;
  display: inline-flex;
  font-family: inherit;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: var(--font-weight-semibold, 600);
  justify-content: center;
  line-height: 1;
  min-height: 2.5rem;
  padding: 0.65rem 0.95rem;
  text-decoration: none;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.workshop-shell__header-action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-shell__hero {
  display: flex;
  flex: 0 0 auto;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--spacing-4, 1rem);
  max-width: 112rem;
  margin: var(--spacing-6, 1.5rem) auto var(--spacing-4, 1rem);
  width: min(calc(100% - (var(--workshop-shell-gutter) * 2)), 112rem);
}

.workshop-shell__eyebrow {
  margin: 0 0 var(--spacing-2, 0.5rem);
  color: var(--color-primary-300, #4dd0e1);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.workshop-shell__summary {
  max-width: 48rem;
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-base, 1rem);
  line-height: 1.7;
}

.workshop-shell__body {
  flex: 1;
  max-width: 112rem;
  min-height: 0;
  margin-right: auto;
  margin-left: auto;
  width: min(calc(100% - (var(--workshop-shell-gutter) * 2)), 112rem);
}

.workshop-shell__body {
  display: grid;
  grid-template-columns: minmax(22rem, 0.9fr) minmax(32rem, 1.25fr);
  gap: var(--spacing-4, 1rem);
  align-items: stretch;
  grid-auto-rows: minmax(0, 1fr);
  margin-top: var(--spacing-4, 1rem);
  margin-bottom: var(--spacing-4, 1rem);
  overflow: hidden;
}

.workshop-shell__body--resizing .workshop-shell__instructions,
.workshop-shell__body--resizing .workshop-shell__app {
  pointer-events: none;
}

.workshop-shell__instructions {
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: var(--spacing-4, 1rem);
  overflow: auto;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: rgba(13, 26, 34, 0.88);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-shell__app {
  display: flex;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.workshop-shell__resize-handle {
  align-self: stretch;
  width: 0.75rem;
  min-width: 0.75rem;
  padding: 0;
  border: 0;
  border-radius: var(--radius-full, 999px);
  background: transparent;
  cursor: col-resize;
  position: relative;
}

.workshop-shell__resize-handle::before {
  content: '';
  position: absolute;
  inset: 0.75rem 0.25rem;
  border-radius: var(--radius-full, 999px);
  background: rgba(59, 130, 246, 0.16);
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.workshop-shell__resize-handle:hover::before,
.workshop-shell__resize-handle--active::before {
  background: rgba(96, 165, 250, 0.48);
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.28);
}

.workshop-shell__empty {
  display: grid;
  min-height: 18rem;
  place-items: center;
  color: var(--color-text-secondary, #94a3b8);
  text-align: center;
}

.workshop-shell__editor-preload {
  border: 0;
  height: 1px;
  left: -10000px;
  opacity: 0;
  pointer-events: none;
  position: fixed;
  top: -10000px;
  width: 1px;
}

@media (max-width: 1180px) {
  .workshop-shell__body {
    grid-template-columns: 1fr !important;
    grid-template-rows: minmax(0, 0.45fr) minmax(0, 1fr);
  }

  .workshop-shell__resize-handle {
    display: none;
  }
}

@media (max-width: 720px) {
  .workshop-shell {
    --workshop-shell-gutter: var(--spacing-4, 1rem);
  }

  .workshop-shell__top-bar {
    height: auto;
    min-height: 72px;
  }

  .workshop-shell__top-bar-content {
    align-items: flex-start;
    flex-direction: column;
    padding-bottom: var(--spacing-3, 0.75rem);
    padding-top: var(--spacing-3, 0.75rem);
  }

  .workshop-shell__brand {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--spacing-2, 0.5rem);
  }

  .workshop-shell__brand h1 {
    font-size: var(--font-size-lg, 1.125rem);
  }

  .workshop-shell__header-actions {
    flex-wrap: wrap;
    width: 100%;
  }

  .workshop-shell__hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
