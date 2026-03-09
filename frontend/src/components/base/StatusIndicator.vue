<template>
  <span :class="['status-indicator', status]"></span>
</template>

<script>
export default {
  name: 'StatusIndicator',
  props: {
    status: {
      type: String,
      required: true,
      validator: (value) => ['running', 'stopped', 'starting', 'restarting'].includes(value)
    }
  }
}
</script>

<style scoped>
.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.status-indicator.running {
  background-color: #4ade80;
  box-shadow: 0 0 6px #4ade80;
}

.status-indicator.stopped {
  background-color: #666;
}

.status-indicator.starting {
  background-color: #fbbf24;
  animation: pulse 1s infinite;
}

.status-indicator.restarting {
  background-color: #f59e0b;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>

