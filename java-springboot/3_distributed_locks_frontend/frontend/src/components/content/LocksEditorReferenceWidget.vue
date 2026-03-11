<template>
  <div v-if="referenceContent" class="reference-widget">
    <p class="reference-title"><strong>PurchaseService.java</strong> is shown here as a read-only reference:</p>
    <pre><code>{{ referenceContent }}</code></pre>
  </div>
</template>

<script>
export default {
  name: 'LocksEditorReferenceWidget',
  props: {
    referenceContent: { type: String, default: '' }
  }
};
</script>

<style scoped>
.reference-widget {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.reference-title {
  margin: 0;
  color: var(--color-text-secondary);
}

pre {
  margin: 0;
  padding: var(--spacing-4);
  overflow-x: auto;
  border-radius: var(--radius-md);
  background: rgba(2, 6, 23, 0.8);
}

pre code {
  color: #e2e8f0;
}
</style>
