<template>
  <div class="exercise-panel">
    <div class="interactive-section">
      <div class="form-group">
        <label>User ID</label>
        <input :value="exercise.userId" placeholder="alice" @input="updateExercise('userId', $event.target.value)" />
      </div>
      <div class="form-group">
        <label>Memory to Store</label>
        <input
          :value="exercise.memoryText"
          placeholder="Alice prefers video calls over phone calls"
          @input="updateExercise('memoryText', $event.target.value)"
        />
      </div>
      <button @click="storeLongTermMemory" class="btn btn-primary" :disabled="loading">
        {{ loading ? 'Storing...' : 'Store Memory' }}
      </button>

      <div class="divider"></div>

      <div class="form-group">
        <label>Search Query (try different phrasing!)</label>
        <input
          :value="exercise.searchQuery"
          placeholder="how does the user like to communicate?"
          @input="updateExercise('searchQuery', $event.target.value)"
          @keyup.enter="searchMemories"
        />
      </div>
      <button @click="searchMemories" class="btn btn-secondary" :disabled="loading">
        {{ loading ? 'Searching...' : 'Search Memories' }}
      </button>
    </div>

    <div v-if="exercise.searchResults.length" class="result-box">
      <h4>Search Results ({{ exercise.searchResults.length }})</h4>
      <div v-for="(memory, index) in exercise.searchResults" :key="index" class="memory-result">
        <div class="memory-text">{{ memory.text }}</div>
        <div class="memory-score">Similarity: {{ (1 - memory.dist).toFixed(2) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemoryLabLongTermMemoryWidget',
  props: {
    exercise: { type: Object, required: true },
    loading: { type: Boolean, default: false },
    updateExercise: { type: Function, required: true },
    storeLongTermMemory: { type: Function, required: true },
    searchMemories: { type: Function, required: true }
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

.divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--spacing-4) 0;
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
}

.result-box h4 {
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-sm);
}

.memory-result {
  background: var(--color-dark-800);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-2);
}

.memory-text {
  color: var(--color-text);
  margin-bottom: var(--spacing-1);
}

.memory-score {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}
</style>
