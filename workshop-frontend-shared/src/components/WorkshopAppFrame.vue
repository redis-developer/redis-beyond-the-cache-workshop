<template>
  <section
    class="workshop-app-frame"
    :class="{
      'workshop-app-frame--blocked': isBlocked,
      'workshop-app-frame--maximized': isMaximized
    }"
  >
    <header class="workshop-app-frame__header">
      <div class="workshop-app-frame__identity">
        <p class="workshop-app-frame__eyebrow">{{ eyebrow }}</p>
      </div>
      <div class="workshop-app-frame__actions">
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--busy': restartInProgress }"
          :disabled="!canRestart || actionsDisabled || Boolean(pendingAction)"
          @click="handleRuntimeAction('restart')"
        >
          {{ restartInProgress ? 'Restarting App' : 'Restart App' }}
        </button>
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--busy': rebuildInProgress }"
          :disabled="!canRebuild || actionsDisabled || Boolean(pendingAction)"
          @click="handleRuntimeAction('rebuild')"
        >
          {{ rebuildInProgress ? 'Recompiling App' : 'Recompile App' }}
        </button>
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--active': logsOpen }"
          :aria-pressed="logsOpen ? 'true' : 'false'"
          @click="toggleLogs"
        >
          Logs
        </button>
        <button
          type="button"
          class="workshop-app-frame__action workshop-app-frame__action--icon"
          :class="{ 'workshop-app-frame__action--busy': refreshInProgress }"
          :disabled="!src || Boolean(pendingAction)"
          :aria-label="refreshInProgress ? 'Refreshing app' : 'Refresh app'"
          :title="refreshInProgress ? 'Refreshing app' : 'Refresh app'"
          @click="handleRetry"
        >
          <svg
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M21 12a9 9 0 0 1-15.5 6.25" />
            <path d="M3 12A9 9 0 0 1 18.5 5.75" />
            <path d="M18 2v4h4" />
            <path d="M6 22v-4H2" />
          </svg>
        </button>
        <button
          type="button"
          class="workshop-app-frame__action workshop-app-frame__action--icon"
          :aria-pressed="isMaximized ? 'true' : 'false'"
          :aria-label="isMaximized ? 'Restore app' : 'Maximize app'"
          :title="isMaximized ? 'Restore app' : 'Maximize app'"
          @click="toggleMaximized"
        >
          <svg
            v-if="isMaximized"
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M9 3v6H3" />
            <path d="M15 3v6h6" />
            <path d="M9 21v-6H3" />
            <path d="M15 21v-6h6" />
          </svg>
          <svg
            v-else
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M8 4H4v4" />
            <path d="M16 4h4v4" />
            <path d="M8 20H4v-4" />
            <path d="M20 16v4h-4" />
          </svg>
        </button>
      </div>
    </header>

    <div class="workshop-app-frame__viewport">
      <iframe
        v-if="src && !isBlocked"
        :key="frameKey"
        class="workshop-app-frame__iframe"
        :src="src"
        :title="title"
        @load="handleLoad"
        @error="handleError"
      ></iframe>

      <div v-if="overlayMessage" class="workshop-app-frame__overlay" aria-live="polite">
        <p class="workshop-app-frame__state">{{ overlayTitle }}</p>
        <p class="workshop-app-frame__message">{{ overlayMessage }}</p>
      </div>

      <aside
        v-if="logsOpen"
        class="workshop-app-frame__logs"
        aria-label="Runtime logs"
      >
        <header class="workshop-app-frame__logs-header">
          <p class="workshop-app-frame__logs-title">Runtime logs</p>
          <button
            type="button"
            class="workshop-app-frame__logs-close"
            aria-label="Close runtime logs"
            @click="toggleLogs"
          >
            Close
          </button>
        </header>
        <pre class="workshop-app-frame__logs-body">{{ formattedRuntimeLogs }}</pre>
      </aside>
    </div>
  </section>
</template>

<script>
import {
  isRuntimeBlocked,
  isRuntimeBusy,
  isRuntimeReady,
  normalizeRuntimeState
} from '../composables/useWorkshopShellState.js';

export default {
  name: 'WorkshopAppFrame',
  props: {
    src: { type: String, default: '' },
    title: { type: String, default: 'Learner application' },
    eyebrow: { type: String, default: 'Learner app' },
    state: { type: String, default: 'unknown' },
    message: { type: String, default: '' },
    refreshKey: { type: [String, Number], default: 0 },
    canRestart: { type: Boolean, default: false },
    canRebuild: { type: Boolean, default: false },
    actionsDisabled: { type: Boolean, default: false },
    runtimeLogs: { type: Array, default: () => [] }
  },
  emits: ['load', 'error', 'retry', 'runtime-action', 'logs-toggle'],
  data() {
    return {
      loading: Boolean(this.src),
      frameError: false,
      localRefreshKey: 0,
      pendingAction: '',
      pendingActionTimer: null,
      logsOpen: false,
      isMaximized: false,
      previousBodyOverflow: ''
    };
  },
  computed: {
    normalizedState() {
      return normalizeRuntimeState(this.state);
    },
    frameKey() {
      return `${this.refreshKey}-${this.localRefreshKey}`;
    },
    isBlocked() {
      return !this.src || isRuntimeBlocked(this.normalizedState);
    },
    restartInProgress() {
      return this.pendingAction === 'restart' || this.normalizedState === 'restarting';
    },
    rebuildInProgress() {
      return this.pendingAction === 'rebuild' || this.normalizedState === 'rebuilding';
    },
    refreshInProgress() {
      return this.pendingAction === 'refresh' || this.loading;
    },
    overlayTitle() {
      if (!this.src) {
        return 'App URL unavailable';
      }

      if (this.frameError) {
        return 'App failed to load';
      }

      if (this.pendingAction === 'restart') {
        return 'Restarting app';
      }

      if (this.pendingAction === 'rebuild') {
        return 'Rebuilding app';
      }

      if (this.pendingAction === 'refresh') {
        return 'Refreshing app';
      }

      if (this.normalizedState === 'restarting') {
        return 'Restarting app';
      }

      if (this.normalizedState === 'rebuilding') {
        return 'Rebuilding app';
      }

      if (isRuntimeBusy(this.normalizedState)) {
        return 'Preparing app';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'App unavailable';
      }

      if (this.loading) {
        return 'Refreshing app';
      }

      return '';
    },
    overlayMessage() {
      if (this.message) {
        return this.message;
      }

      if (!this.src) {
        return 'The platform has not provided a learner app URL yet.';
      }

      if (this.frameError) {
        return 'Reload the app after the runtime is ready.';
      }

      if (this.pendingAction === 'restart') {
        return 'The learner app is restarting. The frame will reload automatically when it is ready.';
      }

      if (this.pendingAction === 'rebuild') {
        return 'The learner app is rebuilding. The frame will reload automatically when it is ready.';
      }

      if (this.pendingAction === 'refresh') {
        return 'Reloading the learner app frame.';
      }

      if (this.normalizedState === 'restarting') {
        return 'The learner app is restarting. The frame will reload automatically when it is ready.';
      }

      if (this.normalizedState === 'rebuilding') {
        return 'The learner app is rebuilding. The frame will reload automatically when it is ready.';
      }

      if (isRuntimeBusy(this.normalizedState)) {
        return 'The shell stays available while the learner app becomes ready.';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'Restart or rebuild the runtime from the learner app header.';
      }

      if (this.loading) {
        return 'Reloading the learner app frame.';
      }

      return '';
    },
    formattedRuntimeLogs() {
      if (!this.runtimeLogs.length) {
        return 'No runtime logs captured yet.';
      }

      return this.runtimeLogs.join('\n');
    }
  },
  watch: {
    src: {
      handler(value) {
        this.loading = Boolean(value);
        this.frameError = false;
      }
    },
    refreshKey() {
      this.loading = Boolean(this.src);
      this.frameError = false;
    },
    normalizedState(value) {
      if (!isRuntimeBusy(value)) {
        this.clearPendingAction();
      }
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown);
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown);
    this.setBodyScrollLock(false);
    this.clearPendingActionTimer();
  },
  methods: {
    handleLoad(event) {
      this.loading = false;
      this.frameError = false;
      if (this.pendingAction === 'refresh' && !isRuntimeBusy(this.normalizedState)) {
        this.clearPendingAction();
      }
      this.$emit('load', event);
    },
    handleError(event) {
      this.loading = false;
      this.frameError = true;
      this.$emit('error', event);
    },
    handleRuntimeAction(type) {
      this.setPendingAction(type);
      this.$emit('runtime-action', {
        type,
        rebuild: type === 'rebuild'
      });
    },
    handleRetry() {
      this.setPendingAction('refresh');
      this.loading = Boolean(this.src);
      this.frameError = false;
      this.$emit('retry');
    },
    toggleLogs() {
      this.logsOpen = !this.logsOpen;
      this.$emit('logs-toggle', this.logsOpen);
    },
    toggleMaximized() {
      this.isMaximized = !this.isMaximized;
      this.setBodyScrollLock(this.isMaximized);
    },
    handleKeydown(event) {
      if (event.key === 'Escape' && this.isMaximized) {
        this.isMaximized = false;
        this.setBodyScrollLock(false);
      }
    },
    setBodyScrollLock(locked) {
      if (locked) {
        if (!this.previousBodyOverflow) {
          this.previousBodyOverflow = document.body.style.overflow || '';
        }
        document.body.style.overflow = 'hidden';
        return;
      }

      document.body.style.overflow = this.previousBodyOverflow;
      this.previousBodyOverflow = '';
    },
    setPendingAction(type) {
      this.pendingAction = type;
      this.clearPendingActionTimer();
      this.pendingActionTimer = window.setTimeout(() => {
        if (!isRuntimeBusy(this.normalizedState)) {
          this.pendingAction = '';
        }
      }, 1500);
    },
    clearPendingAction() {
      this.pendingAction = '';
      this.clearPendingActionTimer();
    },
    clearPendingActionTimer() {
      if (this.pendingActionTimer) {
        window.clearTimeout(this.pendingActionTimer);
        this.pendingActionTimer = null;
      }
    }
  }
};
</script>

<style scoped>
.workshop-app-frame {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: var(--color-dark-900, #0A151B);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-app-frame--maximized {
  position: fixed;
  inset: calc(72px + var(--spacing-4, 1rem)) var(--spacing-4, 1rem) var(--spacing-4, 1rem);
  z-index: 1000;
  width: auto;
  height: auto;
  min-height: 0;
  border-radius: var(--radius-xl, 0.75rem);
  box-shadow:
    0 0 0 100vmax var(--color-background, #0A151B),
    0 24px 80px rgba(0, 0, 0, 0.5);
}

.workshop-app-frame__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  min-height: 2.75rem;
  padding: var(--spacing-2, 0.5rem) var(--spacing-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.workshop-app-frame__identity {
  min-width: 0;
}

.workshop-app-frame__eyebrow {
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-app-frame__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-2, 0.5rem);
}

.workshop-app-frame__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.85rem;
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.workshop-app-frame__action--icon {
  width: 1.85rem;
  padding-right: 0;
  padding-left: 0;
}

.workshop-app-frame__icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
}

.workshop-app-frame__action:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-app-frame__action--busy,
.workshop-app-frame__action--busy:disabled {
  background: rgba(251, 191, 36, 0.16);
  border-color: rgba(251, 191, 36, 0.48);
  color: var(--color-warning, #fbbf24);
  opacity: 1;
}

.workshop-app-frame__action--active {
  background: rgba(59, 130, 246, 0.32);
  border-color: rgba(59, 130, 246, 0.68);
  color: #bfdbfe;
}

.workshop-app-frame__action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.workshop-app-frame__viewport {
  position: relative;
  flex: 1;
  min-height: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(0, 188, 212, 0.08), transparent 28rem),
    var(--color-dark-900, #0A151B);
}

.workshop-app-frame--maximized .workshop-app-frame__viewport {
  background: var(--color-dark-900, #0A151B);
}

.workshop-app-frame__iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  background: white;
}

.workshop-app-frame__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2, 0.5rem);
  padding: var(--spacing-6, 1.5rem);
  text-align: center;
  background: rgba(10, 21, 27, 0.88);
  backdrop-filter: blur(8px);
}

.workshop-app-frame__state {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: var(--font-weight-semibold, 600);
}

.workshop-app-frame__message {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1.6;
}

.workshop-app-frame__logs {
  position: absolute;
  right: var(--spacing-3, 0.75rem);
  bottom: var(--spacing-3, 0.75rem);
  left: var(--spacing-3, 0.75rem);
  z-index: 4;
  display: flex;
  flex-direction: column;
  max-height: min(22rem, 45%);
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.6);
  border-radius: var(--radius-lg, 8px);
  background: rgba(2, 8, 13, 0.94);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.42);
}

.workshop-app-frame__logs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  padding: 0.55rem 0.75rem;
  border-bottom: 1px solid rgba(71, 85, 105, 0.42);
  background: rgba(13, 26, 34, 0.96);
}

.workshop-app-frame__logs-title {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-app-frame__logs-close {
  border: 0;
  background: transparent;
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  cursor: pointer;
}

.workshop-app-frame__logs-close:hover {
  color: #bfdbfe;
}

.workshop-app-frame__logs-body {
  margin: 0;
  padding: 0.75rem;
  overflow: auto;
  color: #cbd5e1;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 0.72rem;
  line-height: 1.55;
  white-space: pre-wrap;
}

@media (max-width: 720px) {
  .workshop-app-frame--maximized {
    inset: calc(72px + var(--spacing-2, 0.5rem)) var(--spacing-2, 0.5rem) var(--spacing-2, 0.5rem);
  }

  .workshop-app-frame__logs {
    max-height: 55%;
  }
}
</style>
