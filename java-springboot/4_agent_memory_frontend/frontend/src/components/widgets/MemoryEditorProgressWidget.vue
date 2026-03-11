<template>
  <div class="progress-tracker">
    <div class="progress-header" @click="toggle">
      <span class="progress-title">Progress</span>
      <span class="progress-summary">{{ completedSteps }}/{{ totalSteps }} steps</span>
      <span class="progress-toggle">{{ expanded ? '-' : '+' }}</span>
    </div>
    <div v-if="expanded" class="progress-details">
      <div class="progress-group">
        <span class="progress-label">AgentMemoryService:</span>
        <span class="progress-count" :class="{ complete: stepsCompleted.service >= 9 }">
          {{ stepsCompleted.service }}/9
        </span>
      </div>
      <div class="progress-group">
        <span class="progress-label">ChatMemoryRepository:</span>
        <span class="progress-count" :class="{ complete: stepsCompleted.repository >= 3 }">
          {{ stepsCompleted.repository }}/3
        </span>
      </div>
      <div class="progress-group">
        <span class="progress-label">ChatService:</span>
        <span class="progress-count" :class="{ complete: stepsCompleted.chat >= 3 }">
          {{ stepsCompleted.chat }}/3
        </span>
      </div>
    </div>
    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: `${(completedSteps / totalSteps) * 100}%` }"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemoryEditorProgressWidget',
  props: {
    completedSteps: { type: Number, required: true },
    totalSteps: { type: Number, default: 15 },
    stepsCompleted: {
      type: Object,
      required: true
    },
    expanded: { type: Boolean, default: false },
    toggle: { type: Function, required: true }
  }
};
</script>

<style scoped>
.progress-tracker {
  background: #1a1a2e;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #333;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
}

.progress-title {
  font-weight: bold;
  color: #e0e0e0;
}

.progress-summary {
  color: #10b981;
  font-size: 0.9rem;
}

.progress-toggle {
  color: #888;
  font-size: 1.2rem;
  width: 20px;
  text-align: center;
}

.progress-details {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #333;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.progress-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-label {
  color: #888;
  font-size: 0.85rem;
}

.progress-count {
  background: #333;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #e0e0e0;
}

.progress-count.complete {
  background: #10b981;
  color: white;
}

.progress-bar {
  margin-top: 12px;
  height: 4px;
  background: #333;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #3b82f6);
  border-radius: 2px;
  transition: width 0.3s ease;
}
</style>
