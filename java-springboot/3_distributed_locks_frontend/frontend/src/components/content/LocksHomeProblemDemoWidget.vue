<template>
  <div class="problem-demo-widget">
    <div class="scenario-results">
      <div class="result-item">
        <span class="result-label">Workers Started</span>
        <span class="result-value">{{ jobResult ? jobResult.workers : 5 }}</span>
      </div>
      <div class="result-item" :class="{ 'result-bad': jobResult && jobResult.ran > 1 }">
        <span class="result-label">Actually Ran</span>
        <span class="result-value">{{ jobResult ? jobResult.ran : '-' }}</span>
      </div>
      <div class="result-item">
        <span class="result-label">Skipped</span>
        <span class="result-value">{{ jobResult ? jobResult.skipped : '-' }}</span>
      </div>
    </div>

    <button class="btn btn-primary btn-large" :disabled="runningJob" @click="onRun">
      {{ runningJob ? 'Running...' : 'Run 5 Workers' }}
    </button>

    <div v-if="jobResult && jobResult.ran > 1" class="alert alert-warning">
      <strong>Problem!</strong> All {{ jobResult.ran }} workers executed the job. In production, that means
      duplicate emails, double charges, or corrupted data.
    </div>

    <div v-if="jobResult && jobResult.ran === 1" class="alert alert-success">
      Locks are already working. Only 1 worker ran. Use Restart Workshop to return to the broken starting point.
    </div>
  </div>
</template>

<script>
export default {
  name: 'LocksHomeProblemDemoWidget',
  props: {
    jobResult: { type: Object, default: null },
    runningJob: { type: Boolean, default: false },
    onRun: { type: Function, required: true }
  }
};
</script>

<style scoped>
.problem-demo-widget {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.scenario-results {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--spacing-3);
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: rgba(13, 26, 34, 0.72);
}

.result-item.result-bad {
  border-color: rgba(239, 68, 68, 0.45);
}

.result-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.result-value {
  font-size: 1.5rem;
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
}

.btn {
  padding: 0.75rem 1.2rem;
  border: none;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-primary {
  align-self: flex-start;
  background: #dc382c;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c42f24;
}

.alert {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  border: 1px solid transparent;
}

.alert-warning {
  border-color: rgba(245, 158, 11, 0.45);
  background: rgba(120, 53, 15, 0.24);
}

.alert-success {
  border-color: rgba(16, 185, 129, 0.45);
  background: rgba(6, 95, 70, 0.24);
}
</style>
