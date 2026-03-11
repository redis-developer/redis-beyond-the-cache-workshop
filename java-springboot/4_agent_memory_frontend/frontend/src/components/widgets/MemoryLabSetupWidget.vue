<template>
  <div class="setup-widget">
    <div class="checklist">
      <div class="check-item" :class="{ done: checks.ams }">
        <span class="check-icon">{{ checks.ams ? '[x]' : '[ ]' }}</span>
        <div class="check-content">
          <strong>Agent Memory Server</strong>
          <p>Running on port 8000</p>
        </div>
        <button @click="checkAMS" class="btn btn-sm">Check</button>
      </div>
      <div class="check-item" :class="{ done: checks.redis }">
        <span class="check-icon">{{ checks.redis ? '[x]' : '[ ]' }}</span>
        <div class="check-content">
          <strong>Redis</strong>
          <p>Required by AMS for storage</p>
        </div>
      </div>
    </div>

    <div v-if="!checks.ams" class="alert alert-info">
      <strong>Not Connected?</strong> Start the services with:
      <pre><code>cd java-springboot/4_agent_memory
docker-compose up -d</code></pre>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemoryLabSetupWidget',
  props: {
    checks: { type: Object, required: true },
    checkAMS: { type: Function, required: true }
  }
};
</script>

<style scoped>
.checklist {
  margin-bottom: var(--spacing-4);
}

.check-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-2);
}

.check-item.done {
  background: rgba(16, 185, 129, 0.1);
  border-left: 3px solid #10b981;
}

.check-icon {
  font-family: monospace;
  color: var(--color-text-secondary);
}

.check-item.done .check-icon {
  color: #10b981;
}

.check-content {
  flex: 1;
}

.check-content strong {
  color: var(--color-text);
  display: block;
}

.check-content p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
}

.btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-sm {
  padding: var(--spacing-1) var(--spacing-2);
  font-size: var(--font-size-xs);
  background: var(--color-dark-900);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.alert {
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
}

.alert-info {
  background: rgba(59, 130, 246, 0.1);
  border-left: 3px solid #3b82f6;
}

.alert pre {
  background: var(--color-dark-900);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-2);
  overflow-x: auto;
}

.alert code {
  color: #e0e0e0;
  background: transparent;
  padding: 0;
}
</style>
