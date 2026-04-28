<template>
  <nav class="workshop-runtime-toolbar" aria-label="Workshop runtime actions">
    <div class="runtime-state" aria-live="polite">
      <span class="runtime-state__label">Runtime</span>
      <span class="runtime-state__value" :class="runtimeStateClass">{{ runtimeLabel }}</span>
      <span v-if="backendLabel" class="runtime-state__detail">Backend {{ backendLabel }}</span>
      <span v-if="frontendLabel" class="runtime-state__detail">App {{ frontendLabel }}</span>
    </div>

    <div class="runtime-actions">
      <button
        type="button"
        class="runtime-action runtime-action--secondary"
        :disabled="!shellState.canRestart || disabled"
        @click="emitAction('restart')"
      >
        Restart runtime
      </button>
      <button
        type="button"
        class="runtime-action runtime-action--accent"
        :disabled="!shellState.canRebuild || disabled"
        @click="emitAction('rebuild')"
      >
        Rebuild runtime
      </button>
      <a
        v-if="shellState.canOpenRedisInsight"
        class="runtime-action runtime-action--link"
        :href="shellState.redisInsightUrl"
        target="_blank"
        rel="noreferrer"
        @click="emitLinkAction('redisInsight')"
      >
        Redis Insight
      </a>
      <a
        v-if="shellState.canOpenHub"
        class="runtime-action runtime-action--link"
        :href="shellState.hubUrl"
        @click="emitLinkAction('hub')"
      >
        Hub
      </a>
      <button
        v-if="shellState.canRefresh"
        type="button"
        class="runtime-action runtime-action--ghost"
        :disabled="disabled"
        @click="emitAction('refresh')"
      >
        Refresh status
      </button>
    </div>
  </nav>
</template>

<script>
import { resolveWorkshopShellState } from '../composables/useWorkshopShellState.js';

export default {
  name: 'WorkshopRuntimeToolbar',
  props: {
    runtime: { type: Object, default: () => ({}) },
    learnerApp: { type: Object, default: () => ({}) },
    links: { type: Object, default: () => ({}) },
    actions: { type: [Array, Object], default: null },
    disabled: { type: Boolean, default: false }
  },
  emits: ['action'],
  computed: {
    shellState() {
      return resolveWorkshopShellState({
        runtime: this.runtime,
        learnerApp: this.learnerApp,
        links: this.links,
        actions: this.actions
      });
    },
    runtimeLabel() {
      return formatState(this.shellState.runtimeState);
    },
    backendLabel() {
      return optionalState(this.shellState.backendState);
    },
    frontendLabel() {
      return optionalState(this.shellState.frontendState);
    },
    runtimeStateClass() {
      return {
        'runtime-state__value--ready': this.shellState.ready,
        'runtime-state__value--busy': this.shellState.busy,
        'runtime-state__value--blocked': this.shellState.learnerAppUnavailable && !this.shellState.busy
      };
    }
  },
  methods: {
    emitAction(type) {
      this.$emit('action', {
        type,
        rebuild: type === 'rebuild'
      });
    },
    emitLinkAction(type) {
      this.$emit('action', { type });
    }
  }
};

function optionalState(state) {
  if (!state || state === 'unknown') {
    return '';
  }

  return formatState(state);
}

function formatState(state) {
  if (!state) {
    return 'Unknown';
  }

  return String(state)
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map(part => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
    .join(' ');
}
</script>

<style scoped>
.workshop-runtime-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-4, 1rem);
  padding: var(--spacing-3, 0.75rem) var(--spacing-4, 1rem);
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background:
    linear-gradient(135deg, rgba(16, 26, 33, 0.95), rgba(10, 21, 27, 0.98)),
    var(--color-dark-800, #0D1A22);
}

.runtime-state {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-2, 0.5rem);
  min-width: 0;
}

.runtime-state__label,
.runtime-state__detail {
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
}

.runtime-state__value {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.55rem;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-full, 9999px);
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
}

.runtime-state__value--ready {
  border-color: rgba(74, 222, 128, 0.35);
  background: rgba(74, 222, 128, 0.12);
  color: var(--color-success, #4ade80);
}

.runtime-state__value--busy {
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.12);
  color: var(--color-warning, #fbbf24);
}

.runtime-state__value--blocked {
  border-color: rgba(252, 165, 165, 0.35);
  background: rgba(252, 165, 165, 0.12);
  color: var(--color-error, #fca5a5);
}

.runtime-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-2, 0.5rem);
}

.runtime-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.runtime-action:hover:not(:disabled) {
  transform: translateY(-1px);
}

.runtime-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.runtime-action--secondary {
  border-color: var(--color-restart-border, rgba(59, 130, 246, 0.4));
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  color: var(--color-restart-text, #93c5fd);
}

.runtime-action--accent {
  border-color: rgba(245, 158, 11, 0.45);
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
}

.runtime-action--link {
  border-color: var(--color-open-border, rgba(0, 188, 212, 0.4));
  background: var(--color-open-bg, rgba(0, 188, 212, 0.2));
  color: var(--color-open-text, #00bcd4);
}

.runtime-action--ghost {
  border-color: var(--color-border, rgba(71, 85, 105, 0.5));
  background: transparent;
  color: var(--color-text-secondary, #94a3b8);
}

@media (max-width: 860px) {
  .workshop-runtime-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .runtime-actions {
    justify-content: flex-start;
  }
}
</style>
