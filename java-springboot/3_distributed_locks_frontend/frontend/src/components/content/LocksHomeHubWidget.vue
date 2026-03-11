<template>
  <div class="locks-home-hub-widget">
    <div class="progress-summary">
      <span class="progress-count">{{ completedCount }}/{{ lockTypes.length }}</span>
      <span class="progress-label">Completed</span>
    </div>

    <div class="lock-types-grid">
      <div
        v-for="lockType in lockTypes"
        :key="lockType.id"
        class="lock-card"
        :class="{
          available: lockType.available,
          completed: getLockStatus(lockType.id).testsComplete,
          'in-progress': getLockStatus(lockType.id).started && !getLockStatus(lockType.id).testsComplete
        }"
      >
        <div class="lock-card-header">
          <h3>{{ lockType.name }}</h3>
          <span class="difficulty-badge" :class="lockType.difficulty.toLowerCase()">
            {{ lockType.difficulty }}
          </span>
        </div>

        <p class="lock-card-description">{{ lockType.description }}</p>
        <p class="lock-card-usecase"><strong>Use case:</strong> {{ lockType.useCase }}</p>

        <div class="lock-card-footer">
          <div class="lock-status">
            <span v-if="getLockStatus(lockType.id).testsComplete" class="status-complete">Completed</span>
            <span v-else-if="getLockStatus(lockType.id).started" class="status-progress">In Progress</span>
            <span v-else-if="!lockType.available" class="status-soon">Coming Soon</span>
          </div>

          <router-link v-if="lockType.available" :to="`/${lockType.id}`" class="btn btn-primary btn-sm">
            {{ getLockStatus(lockType.id).started ? 'Continue' : 'Start' }}
          </router-link>
          <button v-else class="btn btn-disabled btn-sm" disabled>Coming Soon</button>
        </div>
      </div>
    </div>

    <div class="hub-footer">
      <button class="btn btn-secondary" @click="onBack">See Problem Demo</button>
      <button class="btn btn-warning" :disabled="restartingLab" @click="onRestart">
        {{ restartingLab ? 'Restoring...' : 'Restart Workshop' }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LocksHomeHubWidget',
  props: {
    lockTypes: { type: Array, required: true },
    lockProgress: { type: Object, default: () => ({}) },
    completedCount: { type: Number, default: 0 },
    restartingLab: { type: Boolean, default: false },
    onBack: { type: Function, required: true },
    onRestart: { type: Function, required: true }
  },
  methods: {
    getLockStatus(lockTypeId) {
      return this.lockProgress[lockTypeId] || { started: false, implemented: false, testsComplete: false };
    }
  }
};
</script>

<style scoped>
.locks-home-hub-widget {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-5);
}

.progress-summary {
  display: inline-flex;
  align-self: flex-start;
  flex-direction: column;
  gap: 0.2rem;
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-lg);
  background: rgba(13, 26, 34, 0.72);
  border: 1px solid var(--color-border);
}

.progress-count {
  font-size: 1.5rem;
  font-weight: var(--font-weight-bold);
  color: #10b981;
}

.progress-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.lock-types-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-4);
}

.lock-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.lock-card.completed {
  border-color: rgba(16, 185, 129, 0.45);
}

.lock-card.in-progress {
  border-color: rgba(56, 189, 248, 0.45);
}

.lock-card-header,
.lock-card-footer,
.hub-footer {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-3);
  align-items: center;
}

.lock-card-header {
  align-items: flex-start;
}

.lock-card-header h3,
.lock-card-description,
.lock-card-usecase {
  margin: 0;
}

.difficulty-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
}

.difficulty-badge.beginner {
  background: rgba(16, 185, 129, 0.18);
  color: #34d399;
}

.difficulty-badge.intermediate {
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
}

.difficulty-badge.advanced {
  background: rgba(239, 68, 68, 0.18);
  color: #f87171;
}

.lock-card-description,
.lock-card-usecase {
  color: var(--color-text-secondary);
}

.lock-status {
  min-height: 1.2rem;
  font-size: var(--font-size-sm);
}

.status-complete {
  color: #34d399;
}

.status-progress {
  color: #7dd3fc;
}

.status-soon {
  color: var(--color-text-secondary);
}

.btn {
  padding: 0.6rem 1rem;
  border-radius: var(--radius-md);
  border: none;
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-sm {
  padding: 0.45rem 0.8rem;
  font-size: var(--font-size-sm);
}

.btn-primary {
  background: #dc382c;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c42f24;
}

.btn-secondary {
  background: rgba(15, 23, 42, 0.65);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-warning {
  background: rgba(217, 119, 6, 0.2);
  color: #fdba74;
  border: 1px solid rgba(245, 158, 11, 0.45);
}

.btn-disabled {
  background: rgba(15, 23, 42, 0.65);
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .hub-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .hub-footer .btn {
    width: 100%;
  }
}
</style>
