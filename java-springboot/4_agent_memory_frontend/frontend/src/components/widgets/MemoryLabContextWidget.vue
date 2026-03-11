<template>
  <div class="exercise-panel">
    <div class="interactive-section">
      <div class="form-group">
        <label>New User Message</label>
        <input
          :value="exercise.newMessage"
          placeholder="Can you schedule a meeting for tomorrow?"
          @input="updateExercise('newMessage', $event.target.value)"
        />
      </div>
      <button @click="buildContext" class="btn btn-primary" :disabled="loading">
        Build Enriched Context
      </button>
    </div>

    <div v-if="exercise.context" class="context-result">
      <h4>Enriched Context for LLM</h4>
      <div class="context-section">
        <h5>Working Memory (Conversation History)</h5>
        <pre>{{ exercise.context.workingMemory }}</pre>
      </div>
      <div class="context-section">
        <h5>Relevant Long-Term Memories</h5>
        <pre>{{ exercise.context.relevantMemories }}</pre>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemoryLabContextWidget',
  props: {
    exercise: { type: Object, required: true },
    loading: { type: Boolean, default: false },
    updateExercise: { type: Function, required: true },
    buildContext: { type: Function, required: true }
  }
};
</script>

<style scoped>
.exercise-panel {
  background: var(--color-dark-800);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
}

.interactive-section {
  margin-bottom: var(--spacing-4);
}

.form-group {
  margin-bottom: var(--spacing-3);
}

.form-group label {
  display: block;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-1);
}

.form-group input {
  width: 100%;
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-md);
  background: var(--color-dark-900);
  border: 1px solid var(--color-border);
  color: var(--color-text);
}

.btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-primary {
  background: #dc382c;
  color: white;
}

.context-result {
  background: var(--color-dark-900);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
}

.context-result h4 {
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-sm);
}

.context-section {
  margin-bottom: var(--spacing-3);
}

.context-section h5 {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-1);
}

.context-section pre {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  overflow-x: auto;
}
</style>
