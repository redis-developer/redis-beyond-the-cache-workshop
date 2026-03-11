<template>
  <div class="exercise-panel">
    <div class="interactive-section">
      <div class="form-group">
        <label>Session ID</label>
        <input :value="exercise.sessionId" placeholder="my-session-123" @input="updateExercise('sessionId', $event.target.value)" />
      </div>
      <div class="form-group">
        <label>User ID</label>
        <input :value="exercise.userId" placeholder="alice" @input="updateExercise('userId', $event.target.value)" />
      </div>
      <div class="form-group">
        <label>User Message</label>
        <input
          :value="exercise.userMessage"
          placeholder="Hi, I'm Alice and I work at TechCorp"
          @input="updateExercise('userMessage', $event.target.value)"
        />
      </div>
      <div class="form-group">
        <label>Assistant Response</label>
        <input
          :value="exercise.assistantMessage"
          placeholder="Nice to meet you, Alice!"
          @input="updateExercise('assistantMessage', $event.target.value)"
        />
      </div>
      <div class="button-row">
        <button @click="storeWorkingMemory" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Storing...' : 'Store Working Memory' }}
        </button>
        <button @click="retrieveWorkingMemory" class="btn btn-secondary" :disabled="loading">
          Retrieve
        </button>
      </div>
    </div>

    <div v-if="exercise.result" class="result-box">
      <h4>{{ exercise.result.success ? 'Success!' : 'Error' }}</h4>
      <pre><code>{{ JSON.stringify(exercise.result.data, null, 2) }}</code></pre>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemoryLabWorkingMemoryWidget',
  props: {
    exercise: { type: Object, required: true },
    loading: { type: Boolean, default: false },
    updateExercise: { type: Function, required: true },
    storeWorkingMemory: { type: Function, required: true },
    retrieveWorkingMemory: { type: Function, required: true }
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

.button-row {
  display: flex;
  gap: var(--spacing-2);
  margin-top: var(--spacing-3);
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

.btn-secondary {
  background: var(--color-dark-900);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.result-box {
  background: var(--color-dark-900);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  margin-top: var(--spacing-3);
}

.result-box h4 {
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-sm);
}

.result-box pre {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  overflow-x: auto;
}
</style>
