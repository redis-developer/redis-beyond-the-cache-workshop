<template>
  <section class="workshop-app-frame" :class="{ 'workshop-app-frame--blocked': isBlocked }">
    <header class="workshop-app-frame__header">
      <div>
        <p class="workshop-app-frame__eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
      </div>
      <button
        type="button"
        class="workshop-app-frame__retry"
        :disabled="!src"
        @click="$emit('retry')"
      >
        Reload app
      </button>
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
    refreshKey: { type: [String, Number], default: 0 }
  },
  emits: ['load', 'error', 'retry'],
  data() {
    return {
      loading: Boolean(this.src),
      frameError: false,
      localRefreshKey: 0
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
    overlayTitle() {
      if (!this.src) {
        return 'App URL unavailable';
      }

      if (this.frameError) {
        return 'App failed to load';
      }

      if (isRuntimeBusy(this.normalizedState)) {
        return 'Runtime is changing';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'App unavailable';
      }

      if (this.loading && !isRuntimeReady(this.normalizedState)) {
        return 'Loading app';
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

      if (isRuntimeBusy(this.normalizedState)) {
        return 'The shell stays available while the learner runtime restarts or rebuilds.';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'Restart or rebuild the runtime from the toolbar above.';
      }

      if (this.loading && !isRuntimeReady(this.normalizedState)) {
        return 'Waiting for the learner app to become ready.';
      }

      return '';
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
    }
  },
  methods: {
    handleLoad(event) {
      this.loading = false;
      this.frameError = false;
      this.$emit('load', event);
    },
    handleError(event) {
      this.loading = false;
      this.frameError = true;
      this.$emit('error', event);
    }
  }
};
</script>

<style scoped>
.workshop-app-frame {
  display: flex;
  flex-direction: column;
  min-height: 34rem;
  overflow: hidden;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: var(--color-dark-900, #0A151B);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-app-frame__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-4, 1rem);
  padding: var(--spacing-4, 1rem);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.workshop-app-frame__header h2 {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-lg, 1.125rem);
}

.workshop-app-frame__eyebrow {
  margin: 0 0 var(--spacing-1, 0.25rem);
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-app-frame__retry {
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  cursor: pointer;
}

.workshop-app-frame__retry:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.workshop-app-frame__viewport {
  position: relative;
  flex: 1;
  min-height: 28rem;
  background:
    radial-gradient(circle at 20% 20%, rgba(0, 188, 212, 0.08), transparent 28rem),
    var(--color-dark-900, #0A151B);
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
</style>
