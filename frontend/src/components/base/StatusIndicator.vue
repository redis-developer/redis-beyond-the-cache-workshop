<template>
  <span :class="['status-indicator', normalizedStatus]"></span>
</template>

<script>
export default {
  name: 'StatusIndicator',
  props: {
    status: {
      type: String,
      default: 'stopped'
    }
  },
  computed: {
    normalizedStatus() {
      return (this.status || 'stopped').toLowerCase();
    }
  }
};
</script>

<style scoped>
.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.status-indicator.ready,
.status-indicator.running {
  background-color: #4ade80;
  box-shadow: 0 0 6px #4ade80;
}

.status-indicator.stopped,
.status-indicator.terminated,
.status-indicator.expired {
  background-color: #666;
}

.status-indicator.requested,
.status-indicator.admitted,
.status-indicator.provisioning,
.status-indicator.initializing,
.status-indicator.cleanup_pending {
  background-color: #fbbf24;
  animation: pulse 1s infinite;
}

.status-indicator.degraded {
  background-color: #fb923c;
  animation: pulse 1s infinite;
}

.status-indicator.terminating {
  background-color: #f59e0b;
  animation: pulse 1s infinite;
}

.status-indicator.failed {
  background-color: #ef4444;
  box-shadow: 0 0 6px #ef4444;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
