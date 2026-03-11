<template>
  <div class="locks-implement-status-widget">
    <button class="btn btn-secondary" :disabled="checkingLock" @click="onCheck">
      {{ checkingLock ? 'Checking...' : 'Check Lock Status' }}
    </button>

    <p class="status-line">
      Config (<code>workshop.lock.mode</code>):
      <strong :class="lockConfigured ? 'status-ok' : 'status-missing'">
        {{ lockConfigured ? 'redisson' : 'none' }}
      </strong>
      <br>
      Code (<code>tryLock + unlock</code>):
      <strong :class="lockImplemented ? 'status-ok' : 'status-missing'">
        {{ lockImplemented ? 'implemented' : 'missing' }}
      </strong>
    </p>

    <div class="button-group">
      <button class="btn btn-secondary" @click="onBack">Back to Learn</button>
      <button v-if="readyForDemo" class="btn btn-primary" @click="onDemo">Test Your Implementation</button>
      <span v-else class="btn btn-disabled">Complete implementation first</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LocksImplementStatusWidget',
  props: {
    checkingLock: { type: Boolean, default: false },
    lockConfigured: { type: Boolean, default: false },
    lockImplemented: { type: Boolean, default: false },
    onCheck: { type: Function, required: true },
    onBack: { type: Function, required: true },
    onDemo: { type: Function, required: true }
  },
  computed: {
    readyForDemo() {
      return this.lockConfigured && this.lockImplemented;
    }
  }
};
</script>

<style scoped>
.locks-implement-status-widget {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.status-line {
  margin: 0;
  color: var(--color-text-secondary);
}

.status-ok {
  color: #22c55e;
}

.status-missing {
  color: #ef4444;
}

.button-group {
  display: flex;
  gap: var(--spacing-3);
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.65rem 1rem;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  border: none;
  text-decoration: none;
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-primary {
  background: #dc382c;
  color: white;
}

.btn-secondary {
  background: rgba(15, 23, 42, 0.65);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-disabled {
  background: rgba(15, 23, 42, 0.65);
  color: var(--color-text-secondary);
}
</style>
