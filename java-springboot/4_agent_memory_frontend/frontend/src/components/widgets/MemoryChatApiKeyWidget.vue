<template>
  <div class="api-key-setup">
    <input
      :value="tempApiKey"
      type="password"
      placeholder="sk-..."
      @input="updateTempApiKey($event.target.value)"
      @keyup.enter="validateKey"
    />
    <button @click="validateKey" class="btn btn-primary" :disabled="validating">
      {{ validating ? 'Validating...' : 'Continue' }}
    </button>
    <p v-if="keyError" class="error">{{ keyError }}</p>
  </div>
</template>

<script>
export default {
  name: 'MemoryChatApiKeyWidget',
  props: {
    tempApiKey: { type: String, default: '' },
    validating: { type: Boolean, default: false },
    keyError: { type: String, default: '' },
    updateTempApiKey: { type: Function, required: true },
    validateKey: { type: Function, required: true }
  }
};
</script>

<style scoped>
.api-key-setup {
  background: #252526;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
}

.api-key-setup input {
  width: 100%;
  padding: var(--spacing-2);
  margin: 0 0 var(--spacing-3);
  background: var(--color-dark-800);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
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

.error {
  color: #ef4444;
  font-size: var(--font-size-sm);
  margin-top: var(--spacing-2);
}
</style>
