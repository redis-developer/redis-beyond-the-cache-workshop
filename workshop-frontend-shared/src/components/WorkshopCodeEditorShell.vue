<template>
  <main class="workshop-code-editor-shell">
    <header class="workshop-code-editor-shell__top-bar">
      <div class="workshop-code-editor-shell__top-bar-content">
        <div class="workshop-code-editor-shell__brand" aria-label="Redis workshop">
          <slot name="logo">
            <svg class="workshop-code-editor-shell__logo" width="84" height="28" viewBox="0 0 84 28" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              <g clip-path="url(#workshop-code-editor-shell-redis-logo)">
                <path d="M68.7322 7.08959C69.5294 5.68591 70.8337 4.17426 71.3047 3.70636C73.4786 4.60616 75.5076 6.44173 75.2178 6.90963C74.3845 8.27731 73.1163 9.82496 72.6453 10.2929C70.4714 9.39306 68.4424 7.59347 68.7322 7.08959ZM84.0584 13.2802C83.8048 14.5039 82.283 15.8716 81.6309 16.0875C81.0874 14.9358 80.4714 14.2519 79.8917 14.2519C79.1671 14.2519 79.1309 14.7558 79.1309 15.4037C79.1309 16.5554 79.9642 19.0748 79.9642 21.7022C79.9642 24.5816 77.9352 26.7051 74.8192 26.7051C71.966 26.7051 70.3893 24.8471 69.686 21.8788C67.8221 25.1966 65.0965 26.7051 63.0076 26.7051C59.7418 26.7051 58.9732 24.3072 59.0507 21.8752C57.7384 24.1789 55.2126 26.7051 52.7901 26.7051C50.3173 26.7051 49.4438 24.567 49.6441 22.077C48.1625 24.8184 45.4824 26.7051 42.8988 26.7051C40.0951 26.7051 38.7074 24.4926 39.1565 21.7503C37.2703 24.0532 33.7592 26.7051 30.1089 26.7051C25.9467 26.7051 24.1349 24.4756 23.9202 21.6818C21.9113 24.8801 19.2036 26.8131 15.9783 26.8131C11.3225 26.8131 9.65719 22.7003 9.41486 19.3362C7.68934 21.6315 5.74936 24.0129 3.36958 26.6691C3.11596 26.921 2.89857 27.065 2.64494 27.065C1.8116 27.065 0.108696 23.3938 0 22.0262C0.966448 20.5364 8.86019 12.074 12.1502 8.42954C9.92766 9.09574 7.63616 10.4244 4.7464 12.4883C4.23915 12.8483 2.8261 9.57302 2.86233 7.05359C6.19568 4.60616 11.2682 3.05851 15.3624 3.05851C21.0871 3.05851 24.3842 6.22578 24.3842 10.6168C24.3842 14.2879 21.3045 18.319 16.8117 18.463C14.4756 18.5233 12.9784 17.2206 12.2126 15.612C12.3041 18.0999 13.6062 21.1624 17.1015 21.1624C21.1595 21.1624 22.9711 18.5709 26.0146 14.7918C28.3335 11.9485 31.0147 9.42905 34.9277 9.42905C37.319 9.42905 38.9495 10.9047 38.9495 13.1362C38.9495 15.8356 35.7611 19.5787 31.3045 19.5787C30.5434 19.5787 29.8496 19.4791 29.2639 19.2822C29.2492 19.3957 29.2393 19.5073 29.2393 19.6147C29.2393 20.8744 29.7103 21.6302 31.7755 21.6302C34.819 21.6302 37.6814 19.8307 41.1596 15.6196C44.5654 11.4806 47.1379 9.68099 49.8553 9.68099C51.6897 9.68099 53.0819 10.6686 53.6955 12.3318C57.3374 7.10672 60.4269 3.40454 63.0438 0.934999C65.6163 2.01475 67.4641 4.13826 66.9569 4.57016C65.0366 6.29777 58.6235 13.2442 56.0872 17.3832C55.4351 18.463 54.8191 19.6507 54.8191 20.2266C54.8191 20.7664 55.1452 20.9464 55.5075 20.9464C57.8989 20.9464 63.5873 13.2442 66.8119 9.86095C68.8409 10.6888 70.9062 12.4524 70.3989 13.0642C67.7177 16.2315 65.6888 18.8229 65.6888 20.2986C65.6888 20.6945 65.8337 20.9464 66.3772 20.9464C67.3917 20.9464 68.3337 20.0466 69.8917 18.139C70.2178 17.7431 70.6163 17.7431 70.8699 18.355C71.5583 20.0106 72.5728 20.9104 73.3699 20.9104C74.312 20.9104 74.783 20.0826 74.783 18.8229C74.783 17.3112 74.4569 15.3677 74.4569 14.5039C74.4569 11.5886 76.6308 9.89694 79.3482 9.89694C81.3772 9.89694 83.1888 10.8687 84.0584 13.2802ZM17.0795 8.03019C15.8592 9.92471 14.7234 11.6945 13.576 13.4203C14.1996 13.7682 14.9881 14.036 16.0146 14.036C17.9349 14.036 20.0363 12.9922 20.0363 10.8687C20.0363 9.58003 19.2312 8.39196 17.0795 8.03019ZM29.8151 17.6954C30.1959 17.8415 30.6429 17.9231 31.1234 17.9231C33.6958 17.9231 35.435 15.9795 35.435 14.6838C35.435 14.108 35.0726 13.7121 34.4929 13.7121C33.0393 13.7121 30.8467 15.7385 29.8151 17.6954ZM51.3771 15.0078C51.3771 14.2879 50.9785 13.856 50.3264 13.856C48.1887 13.856 44.964 17.8871 44.964 19.9026C44.964 20.5505 45.3263 20.9824 46.0872 20.9824C48.4423 20.9824 51.3771 16.7354 51.3771 15.0078Z" fill="#FF4438"/>
              </g>
              <defs>
                <clipPath id="workshop-code-editor-shell-redis-logo">
                  <rect width="84" height="28" fill="white"/>
                </clipPath>
              </defs>
            </svg>
          </slot>
          <h1>{{ title }}</h1>
        </div>

        <nav class="workshop-code-editor-shell__header-actions" aria-label="Editor navigation">
          <slot name="header-actions-start"></slot>
          <a
            v-if="showRedisInsightLink"
            class="workshop-code-editor-shell__header-action"
            :href="resolvedRedisInsightUrl"
          >
            {{ redisInsightLabel }}
          </a>
          <a
            class="workshop-code-editor-shell__header-action"
            :href="resolvedBackToWorkshopUrl"
          >
            {{ backToWorkshopLabel }}
          </a>
          <a
            v-if="showHubLink"
            class="workshop-code-editor-shell__header-action"
            :href="resolvedHubUrl"
          >
            {{ hubLabel }}
          </a>
          <slot name="header-actions"></slot>
        </nav>
      </div>
    </header>

    <section
      v-if="useEmbeddedEditor"
      ref="editorBody"
      class="workshop-code-editor-shell__body"
      :class="{
        'workshop-code-editor-shell__body--editor-only': !hasInstructions,
        'workshop-code-editor-shell__body--resizing': isResizingEditorShell
      }"
      :style="editorBodyStyle"
    >
      <aside
        v-if="hasInstructions"
        class="workshop-code-editor-shell__instructions"
      >
        <slot name="instructions">
          <p v-if="summary">{{ summary }}</p>
        </slot>
      </aside>

      <button
        v-if="hasInstructions"
        type="button"
        class="workshop-code-editor-shell__resize-handle"
        :class="{ 'workshop-code-editor-shell__resize-handle--active': isResizingEditorShell }"
        aria-label="Resize instructions and code editor panels"
        title="Drag to resize panels"
        @pointerdown="startEditorShellResize"
      ></button>

      <div class="workshop-code-editor-shell__editor">
        <slot name="editor-frame">
          <WorkshopCodeEditorFrame
            :src="editorSrc"
            :title="editorTitle"
            :eyebrow="editorEyebrow"
            :actions-disabled="actionsDisabled"
            @load="$emit('editor-load', $event)"
            @error="$emit('editor-error', $event)"
            @reset-start="$emit('reset-start', $event)"
            @reset-success="$emit('reset-success', $event)"
            @reset-error="$emit('reset-error', $event)"
            @recompile-start="$emit('recompile-start', $event)"
            @recompile-success="$emit('recompile-success', $event)"
            @recompile-error="$emit('recompile-error', $event)"
          >
            <template #before-actions>
              <slot name="editor-actions-start"></slot>
            </template>
            <template #actions>
              <slot name="editor-actions"></slot>
            </template>
          </WorkshopCodeEditorFrame>
        </slot>
      </div>
    </section>

    <section v-else class="workshop-code-editor-shell__fallback">
      <slot name="fallback"></slot>
    </section>
  </main>
</template>

<script>
import WorkshopCodeEditorFrame from './WorkshopCodeEditorFrame.vue';
import {
  getBasePath,
  getRedisInsightUrl,
  getWorkshopHubUrl
} from '../utils/basePath.js';

export default {
  name: 'WorkshopCodeEditorShell',
  components: { WorkshopCodeEditorFrame },
  props: {
    title: { type: String, required: true },
    summary: { type: String, default: '' },
    editorSrc: { type: String, default: '' },
    editorTitle: { type: String, default: 'VS Code editor' },
    editorEyebrow: { type: String, default: 'Code editor' },
    backToWorkshopUrl: { type: String, default: '' },
    backToWorkshopLabel: { type: String, default: 'Back to Workshop' },
    redisInsightUrl: { type: String, default: '' },
    redisInsightLabel: { type: String, default: 'Redis Insight' },
    hubUrl: { type: String, default: '' },
    hubLabel: { type: String, default: 'Back to Hub' },
    showRedisInsight: { type: Boolean, default: true },
    showHub: { type: Boolean, default: true },
    useEmbeddedEditor: { type: Boolean, default: true },
    actionsDisabled: { type: Boolean, default: false }
  },
  emits: [
    'editor-error',
    'editor-load',
    'reset-start',
    'reset-success',
    'reset-error',
    'recompile-start',
    'recompile-success',
    'recompile-error'
  ],
  data() {
    return {
      instructionsPanelPercent: 36,
      isResizingEditorShell: false
    };
  },
  computed: {
    editorBodyStyle() {
      if (!this.hasInstructions) {
        return {};
      }

      return {
        gridTemplateColumns: `minmax(20rem, ${this.instructionsPanelPercent}fr) 0.75rem minmax(36rem, ${100 - this.instructionsPanelPercent}fr)`
      };
    },
    hasInstructions() {
      return Boolean(this.summary || this.$slots.instructions);
    },
    resolvedBackToWorkshopUrl() {
      if (this.backToWorkshopUrl) {
        return this.backToWorkshopUrl;
      }

      const returnTo = new URLSearchParams(window.location.search || '').get('returnTo');
      if (returnTo && returnTo.startsWith('/') && !returnTo.startsWith('//')) {
        return this.buildRouteUrl(returnTo);
      }

      return getBasePath() || '/';
    },
    resolvedHubUrl() {
      return this.hubUrl || getWorkshopHubUrl();
    },
    resolvedRedisInsightUrl() {
      return this.redisInsightUrl || getRedisInsightUrl();
    },
    showHubLink() {
      return this.showHub && Boolean(this.resolvedHubUrl);
    },
    showRedisInsightLink() {
      return this.showRedisInsight && Boolean(this.resolvedRedisInsightUrl);
    }
  },
  mounted() {
    this.restoreEditorShellSplit();
  },
  beforeUnmount() {
    this.stopEditorShellResize();
  },
  methods: {
    buildRouteUrl(routePath) {
      const basePath = getBasePath();
      const normalizedRoutePath = routePath.startsWith('/') ? routePath : `/${routePath}`;

      if (!basePath || basePath === '/') {
        return normalizedRoutePath;
      }

      return `${basePath}${normalizedRoutePath}`;
    },
    clampEditorShellSplit(percent) {
      const minPercent = 25;
      const maxPercent = 70;
      return Math.min(Math.max(percent, minPercent), maxPercent);
    },
    onEditorShellResize(event) {
      if (!this.isResizingEditorShell || !this.$refs.editorBody) {
        return;
      }

      const rect = this.$refs.editorBody.getBoundingClientRect();
      if (rect.width <= 0) {
        return;
      }

      const nextPercent = ((event.clientX - rect.left) / rect.width) * 100;
      this.instructionsPanelPercent = this.clampEditorShellSplit(nextPercent);
    },
    persistEditorShellSplit() {
      localStorage.setItem('workshopCodeEditorInstructionsPanelPercent', String(this.instructionsPanelPercent));
    },
    restoreEditorShellSplit() {
      const savedPercent = Number.parseFloat(localStorage.getItem('workshopCodeEditorInstructionsPanelPercent') || '');
      if (!Number.isNaN(savedPercent)) {
        this.instructionsPanelPercent = this.clampEditorShellSplit(savedPercent);
      }
    },
    startEditorShellResize(event) {
      event.preventDefault();
      this.isResizingEditorShell = true;
      window.addEventListener('pointermove', this.onEditorShellResize);
      window.addEventListener('pointerup', this.stopEditorShellResize);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    },
    stopEditorShellResize() {
      if (!this.isResizingEditorShell) {
        return;
      }

      this.isResizingEditorShell = false;
      window.removeEventListener('pointermove', this.onEditorShellResize);
      window.removeEventListener('pointerup', this.stopEditorShellResize);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      this.persistEditorShellSplit();
    }
  }
};
</script>

<style scoped>
.workshop-code-editor-shell {
  --workshop-code-editor-shell-gutter: var(--spacing-6, 1.5rem);
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background: var(--color-background, #0A151B);
  color: var(--color-text, #e2e8f0);
}

.workshop-code-editor-shell__top-bar {
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

.workshop-code-editor-shell__top-bar-content {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  justify-content: space-between;
  padding: 0 var(--workshop-code-editor-shell-gutter);
  width: 100%;
}

.workshop-code-editor-shell__brand {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  min-width: 0;
}

.workshop-code-editor-shell__logo {
  flex: 0 0 auto;
}

.workshop-code-editor-shell__brand h1 {
  color: var(--color-text, #e2e8f0);
  font-size: clamp(1.125rem, 2vw, 1.5rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: -0.01em;
  line-height: 1.2;
  margin: 0;
  min-width: 0;
}

.workshop-code-editor-shell__header-actions {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  gap: var(--spacing-2, 0.5rem);
}

.workshop-code-editor-shell__header-action {
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

.workshop-code-editor-shell__header-action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-code-editor-shell__body {
  display: grid;
  grid-template-columns: minmax(20rem, 0.8fr) 0.75rem minmax(36rem, 1.45fr);
  gap: var(--spacing-4, 1rem);
  flex: 1;
  min-height: 0;
  width: min(calc(100% - (var(--workshop-code-editor-shell-gutter) * 2)), 112rem);
  margin: var(--spacing-4, 1rem) auto;
  overflow: hidden;
}

.workshop-code-editor-shell__body--editor-only {
  grid-template-columns: minmax(0, 1fr);
}

.workshop-code-editor-shell__body--resizing .workshop-code-editor-shell__instructions,
.workshop-code-editor-shell__body--resizing .workshop-code-editor-shell__editor {
  pointer-events: none;
}

.workshop-code-editor-shell__instructions {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: var(--spacing-4, 1rem);
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: rgba(13, 26, 34, 0.88);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
  color: var(--color-text, #e2e8f0);
  line-height: 1.6;
}

.workshop-code-editor-shell__instructions p {
  margin-top: 0;
}

.workshop-code-editor-shell__resize-handle {
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

.workshop-code-editor-shell__resize-handle::before {
  content: '';
  position: absolute;
  inset: 0.75rem 0.25rem;
  border-radius: var(--radius-full, 999px);
  background: rgba(59, 130, 246, 0.16);
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.workshop-code-editor-shell__resize-handle:hover::before,
.workshop-code-editor-shell__resize-handle--active::before {
  background: rgba(96, 165, 250, 0.48);
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.28);
}

.workshop-code-editor-shell__editor {
  display: flex;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.workshop-code-editor-shell__fallback {
  flex: 1;
  min-height: 0;
}

@media (max-width: 1180px) {
  .workshop-code-editor-shell__body {
    grid-template-columns: 1fr !important;
    grid-template-rows: minmax(0, 0.45fr) minmax(0, 1fr);
  }

  .workshop-code-editor-shell__resize-handle {
    display: none;
  }

  .workshop-code-editor-shell__instructions {
    max-height: none;
  }

  .workshop-code-editor-shell__editor {
    min-height: 0;
  }
}

@media (max-width: 720px) {
  .workshop-code-editor-shell {
    --workshop-code-editor-shell-gutter: var(--spacing-4, 1rem);
  }

  .workshop-code-editor-shell__top-bar {
    height: auto;
    min-height: 72px;
  }

  .workshop-code-editor-shell__top-bar-content {
    align-items: flex-start;
    flex-direction: column;
    padding-bottom: var(--spacing-3, 0.75rem);
    padding-top: var(--spacing-3, 0.75rem);
  }

  .workshop-code-editor-shell__brand {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--spacing-2, 0.5rem);
  }

  .workshop-code-editor-shell__brand h1 {
    font-size: var(--font-size-lg, 1.125rem);
  }

  .workshop-code-editor-shell__header-actions {
    flex-wrap: wrap;
    width: 100%;
  }
}
</style>
