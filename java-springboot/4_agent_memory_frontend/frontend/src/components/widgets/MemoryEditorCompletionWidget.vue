<template>
  <div v-if="visible" class="completion-banner">
    All steps completed! Restart the app to test your memory implementation.
  </div>
</template>

<script>
export default {
  name: 'MemoryEditorCompletionWidget',
  props: {
    visible: { type: Boolean, default: false }
  }
};
</script>

<style scoped>
.completion-banner {
  padding: var(--spacing-4);
  background: #1e7e34;
  color: white;
  border-radius: var(--radius-md);
  text-align: center;
  font-weight: var(--font-weight-semibold);
}
</style>
