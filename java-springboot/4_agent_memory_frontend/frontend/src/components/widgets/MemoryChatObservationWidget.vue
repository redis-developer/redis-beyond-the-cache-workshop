<template>
  <div class="observations">
    <h5>What to observe:</h5>
    <label v-for="item in items" :key="item.itemId" class="obs-item">
      <input
        type="checkbox"
        :checked="Boolean(observations[item.itemId])"
        @change="toggleObservation(item.itemId, $event.target.checked)"
      />
      <div class="obs-copy">
        <div v-if="item.title" class="obs-title">{{ item.title }}</div>
        <WorkshopMarkdownContent :body="item.body" />
      </div>
    </label>
    <button @click="advance" class="btn btn-primary" :disabled="!canProceed">
      {{ completeLabel }}
    </button>
  </div>
</template>

<script>
import WorkshopMarkdownContent from '../../../../../../workshop-frontend-shared/src/components/WorkshopMarkdownContent.vue';

export default {
  name: 'MemoryChatObservationWidget',
  components: { WorkshopMarkdownContent },
  props: {
    items: { type: Array, default: () => [] },
    observations: { type: Object, required: true },
    canProceed: { type: Boolean, default: false },
    completeLabel: { type: String, default: 'Next Test' },
    toggleObservation: { type: Function, required: true },
    advance: { type: Function, required: true }
  }
};
</script>

<style scoped>
.observations {
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  border-left: 3px solid #4fc3f7;
}

.observations h5 {
  color: #4fc3f7;
  margin: 0 0 var(--spacing-2);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.obs-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  margin-bottom: var(--spacing-2);
}

.obs-item input[type="checkbox"] {
  margin-top: 3px;
  accent-color: #4caf50;
}

.obs-copy {
  flex: 1;
  min-width: 0;
}

.obs-title {
  color: var(--color-text);
  font-weight: 600;
  margin-bottom: 4px;
}

.btn {
  margin-top: var(--spacing-3);
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-primary {
  background: #dc382c;
  color: white;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
