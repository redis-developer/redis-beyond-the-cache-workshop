<template>
  <nav class="workshop-runtime-toolbar" aria-label="Workshop runtime state">
    <div class="runtime-state" aria-live="polite">
      <span class="runtime-state__label">Runtime</span>
      <span class="runtime-state__value" :class="runtimeStateClass">{{ runtimeLabel }}</span>
      <span v-if="backendLabel" class="runtime-state__detail">Backend {{ backendLabel }}</span>
      <span v-if="frontendLabel" class="runtime-state__detail">App {{ frontendLabel }}</span>
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
    actions: { type: [Array, Object], default: null }
  },
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

@media (max-width: 860px) {
  .workshop-runtime-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
