<template>
  <div class="workshop-card">
    <div class="workshop-header">
      <h3>{{ workshop.title }}</h3>
      <div class="header-icons">
        <div class="status-icon">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M2 4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4zm2-1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H4z"/>
            <path d="M8 5.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5z"/>
          </svg>
          <div class="status-tooltip">
            <div class="status-tooltip-item">
              <StatusIndicator :status="workshop.status" />
              <span class="status-label">Workshop App</span>
              <span class="status-value">{{ statusLabel }}</span>
            </div>
            <div v-if="activeSession" class="status-tooltip-item">
              <span class="session-dot"></span>
              <span class="status-label">Session</span>
              <span class="status-value">{{ shortSessionId }}</span>
            </div>
            <div v-if="activeSession?.expiresAt" class="status-tooltip-item">
              <span class="session-dot"></span>
              <span class="status-label">Expires</span>
              <span class="status-value">{{ expiresLabel }}</span>
            </div>
          </div>
        </div>

        <div class="info-icon" :data-tooltip="workshop.description">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zM7 5v2h2V5H7zm0 4v4h2V9H7z"/>
          </svg>
        </div>
      </div>
    </div>

    <div class="workshop-meta">
      <span v-if="workshop.difficulty">{{ workshop.difficulty }}</span>
      <span v-if="workshop.estimatedMinutes">{{ workshop.estimatedMinutes }} min</span>
      <span v-if="workshop.defaultMode">{{ workshop.defaultMode }}</span>
    </div>

    <div v-if="workshop.topics && workshop.topics.length" class="topic-list">
      <span v-for="topic in workshop.topics" :key="topic">{{ topic }}</span>
    </div>

    <div v-if="activeSession" class="session-panel">
      <div>
        <strong>Session {{ shortSessionId }}</strong>
        <span>{{ statusLabel }}</span>
      </div>
      <div v-if="activeSession.failureMessage" class="failure-message">
        {{ activeSession.failureMessage }}
      </div>
    </div>

    <div class="workshop-controls" :class="{ 'single-button': !activeSession }">
      <button
        v-if="!activeSession"
        class="control-btn deploy-btn"
        :disabled="isBusy"
        @click="handleLaunch"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M8 0l8 8-8 8V0z"/>
        </svg>
        {{ deployLabel }}
      </button>

      <button
        v-if="activeSession"
        class="control-btn restart-btn"
        :disabled="isBusy || !canRestart"
        @click="handleRestart(true)"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M8 3V0L4 4l4 4V5a5 5 0 110 6v2a7 7 0 100-10z"/>
        </svg>
        {{ rebuildLabel }}
      </button>

      <button
        v-if="activeSession"
        class="control-btn restart-btn"
        :disabled="isBusy || !canRestart"
        @click="handleRestart(false)"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M8 3V0L4 4l4 4V5a5 5 0 110 6v2a7 7 0 100-10z"/>
        </svg>
        {{ restartLabel }}
      </button>

      <button
        v-if="activeSession"
        class="control-btn stop-btn"
        :disabled="isBusy"
        @click="handleTerminate"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <rect x="4" y="4" width="8" height="8"/>
        </svg>
        {{ stopLabel }}
      </button>

      <a
        v-if="canOpen"
        :href="activeSession.publicEntryUrl"
        class="control-btn open-btn"
        target="_blank"
        rel="noopener"
      >
        Open Workshop
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M8 0l8 8-8 8V0z"/>
        </svg>
      </a>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex';
import StatusIndicator from '@/components/base/StatusIndicator.vue';

const RESTARTABLE_STATES = new Set(['READY', 'DEGRADED']);

export default {
  name: 'WorkshopCard',
  components: {
    StatusIndicator
  },
  props: {
    workshop: {
      type: Object,
      required: true
    }
  },
  computed: {
    activeSession() {
      return this.workshop.activeSession;
    },

    isBusy() {
      return Boolean(this.workshop.pendingAction);
    },

    canOpen() {
      return this.activeSession?.state === 'READY' && this.activeSession.publicEntryUrl;
    },

    canRestart() {
      return this.activeSession && RESTARTABLE_STATES.has(this.activeSession.state);
    },

    statusLabel() {
      if (this.workshop.pendingAction) {
        return this.workshop.pendingAction;
      }
      if (!this.activeSession) {
        return 'not launched';
      }
      return this.activeSession.state.replace(/_/g, ' ').toLowerCase();
    },

    shortSessionId() {
      return this.activeSession?.sessionId?.slice(0, 8) || '';
    },

    expiresLabel() {
      const expiresAt = new Date(this.activeSession.expiresAt);
      if (Number.isNaN(expiresAt.getTime())) {
        return '';
      }
      return expiresAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },

    deployLabel() {
      return this.workshop.pendingAction === 'launching' ? 'Deploying...' : 'Deploy Workshop';
    },

    rebuildLabel() {
      return this.workshop.pendingAction === 'rebuilding' ? 'Rebuilding...' : 'Restart & Rebuild';
    },

    restartLabel() {
      return this.workshop.pendingAction === 'restarting' ? 'Restarting...' : 'Restart App Only';
    },

    stopLabel() {
      return this.workshop.pendingAction === 'terminating' ? 'Stopping...' : 'Stop';
    }
  },
  methods: {
    ...mapActions(['launchWorkshop', 'restartWorkshop', 'terminateWorkshop']),

    async handleLaunch() {
      await this.launchWorkshop(this.workshop);
    },

    async handleRestart(rebuild) {
      await this.restartWorkshop({ workshopId: this.workshop.id, rebuild });
    },

    async handleTerminate() {
      await this.terminateWorkshop(this.workshop.id);
    }
  }
};
</script>

<style scoped>
.workshop-card {
  background-color: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  overflow: visible;
  padding: var(--spacing-6);
  position: relative;
  transition: all var(--transition-base);
}

.workshop-card:hover {
  border-color: rgba(0, 188, 212, 0.35);
  box-shadow: 0 18px 50px rgba(0, 188, 212, 0.08);
}

.workshop-header {
  align-items: flex-start;
  display: flex;
  gap: var(--spacing-4);
  justify-content: space-between;
  margin-bottom: var(--spacing-5);
}

.workshop-header h3 {
  color: var(--color-text);
  flex: 1;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.01em;
  line-height: 1.4;
  margin: 0;
}

.header-icons {
  align-items: center;
  display: flex;
  flex-shrink: 0;
  gap: var(--spacing-3);
}

.status-icon,
.info-icon {
  align-items: center;
  color: var(--color-text-secondary);
  cursor: help;
  display: flex;
  position: relative;
  transition: all var(--transition-fast);
}

.status-icon:hover,
.info-icon:hover {
  color: var(--color-primary-400);
}

.status-tooltip {
  animation: fadeIn var(--transition-fast);
  background-color: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  display: none;
  margin-top: var(--spacing-2);
  min-width: 220px;
  padding: var(--spacing-3);
  position: absolute;
  right: 0;
  top: 100%;
  z-index: var(--z-tooltip);
}

.status-icon:hover .status-tooltip {
  display: block;
}

.status-tooltip-item {
  align-items: center;
  display: grid;
  gap: var(--spacing-2);
  grid-template-columns: auto 1fr auto;
  padding: var(--spacing-1) 0;
}

.status-label,
.status-value {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-transform: capitalize;
}

.status-value {
  color: var(--color-text);
}

.session-dot {
  background-color: rgba(148, 163, 184, 0.7);
  border-radius: 999px;
  display: inline-block;
  height: 10px;
  width: 10px;
}

.info-icon::after {
  animation: fadeIn var(--transition-fast);
  background-color: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  color: var(--color-text-secondary);
  content: attr(data-tooltip);
  display: none;
  font-size: var(--font-size-sm);
  line-height: 1.6;
  padding: var(--spacing-4);
  pointer-events: none;
  position: absolute;
  right: 0;
  top: calc(100% + var(--spacing-2));
  white-space: normal;
  width: 320px;
  z-index: var(--z-tooltip);
}

.info-icon:hover::after {
  display: block;
}

.workshop-meta,
.topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-4);
}

.workshop-meta span,
.topic-list span {
  background-color: var(--color-dark-700);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  padding: var(--spacing-1) var(--spacing-3);
}

.session-panel {
  background-color: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  display: flex;
  flex-direction: column;
  font-size: var(--font-size-sm);
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-4);
  padding: var(--spacing-4);
}

.session-panel strong {
  color: var(--color-text);
  margin-right: var(--spacing-2);
}

.session-panel span {
  text-transform: capitalize;
}

.failure-message {
  color: rgb(252, 165, 165);
}

.workshop-controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-2);
}

.workshop-controls.single-button {
  display: block;
}

.workshop-controls.single-button .control-btn {
  justify-content: center;
  width: 100%;
}

.control-btn {
  align-items: center;
  background: transparent;
  border: 1px solid;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: inline-flex;
  flex: 1;
  font-family: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  gap: var(--spacing-2);
  justify-content: center;
  min-height: 42px;
  padding: var(--spacing-3) var(--spacing-4);
  text-decoration: none;
  transition: all var(--transition-base);
}

.control-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.deploy-btn {
  background-color: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.4);
  color: rgb(134, 239, 172);
}

.deploy-btn:hover:not(:disabled) {
  background-color: rgba(34, 197, 94, 0.3);
  border-color: rgba(34, 197, 94, 0.6);
}

.restart-btn {
  background-color: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.4);
  color: rgb(147, 197, 253);
}

.restart-btn:hover:not(:disabled) {
  background-color: rgba(59, 130, 246, 0.3);
  border-color: rgba(59, 130, 246, 0.6);
}

.stop-btn {
  background-color: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
  color: rgb(252, 165, 165);
}

.stop-btn:hover:not(:disabled) {
  background-color: rgba(239, 68, 68, 0.3);
  border-color: rgba(239, 68, 68, 0.6);
}

.open-btn {
  background-color: rgba(0, 188, 212, 0.2);
  border-color: rgba(0, 188, 212, 0.4);
  color: var(--color-primary-400);
}

.open-btn:hover:not(:disabled) {
  background-color: rgba(0, 188, 212, 0.3);
  border-color: rgba(0, 188, 212, 0.6);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .workshop-card {
    padding: var(--spacing-4);
  }

  .workshop-header h3 {
    font-size: var(--font-size-lg);
  }

  .workshop-controls {
    flex-direction: column;
  }

  .control-btn {
    width: 100%;
  }

  .info-icon::after {
    right: -50px;
    width: 280px;
  }
}
</style>
