<template>
  <div v-if="visible" class="session-comparison" :class="{ success: showSuccess }">
    <div class="session-old">
      <span class="label">{{ previousLabel }}</span>
      <code>{{ previousSessionId }}</code>
    </div>
    <div class="session-new">
      <span class="label">{{ currentLabel }}</span>
      <code>{{ currentSessionId }}</code>
    </div>
    <p class="comparison-note" :class="noteClass">{{ noteMessage }}</p>
  </div>
</template>

<script>
export default {
  name: 'SessionComparisonWidget',
  props: {
    visible: { type: Boolean, default: false },
    showSuccess: { type: Boolean, default: false },
    previousLabel: { type: String, default: 'Previous Session ID:' },
    currentLabel: { type: String, default: 'Current Session ID:' },
    previousSessionId: { type: String, default: '' },
    currentSessionId: { type: String, default: '' },
    sessionIdChanged: { type: Boolean, default: false },
    changedMessage: { type: String, default: '' },
    unchangedMessage: { type: String, default: '' },
    changedClass: { type: String, default: '' },
    unchangedClass: { type: String, default: '' }
  },
  computed: {
    noteClass() {
      return this.sessionIdChanged ? this.changedClass : this.unchangedClass;
    },
    noteMessage() {
      return this.sessionIdChanged ? this.changedMessage : this.unchangedMessage;
    }
  }
};
</script>
