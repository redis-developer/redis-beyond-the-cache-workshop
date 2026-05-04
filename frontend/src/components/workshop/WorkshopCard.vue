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
              <span class="status-value">Active</span>
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
      <div class="session-summary">
        <div>
          <strong>Runtime session</strong>
          <span>{{ statusLabel }}</span>
        </div>
        <span v-if="expiresLabel" class="session-expiry">Expires {{ expiresLabel }}</span>
      </div>
      <div class="session-progress" aria-label="Session launch progress">
        <div
          v-for="step in lifecycleSteps"
          :key="step.key"
          :class="['session-step', step.status]"
        >
          <span class="session-step-marker"></span>
          <span class="session-step-label">{{ step.label }}</span>
        </div>
      </div>
      <div v-if="activeSession.failureMessage" class="failure-message">
        {{ activeSession.failureMessage }}
      </div>
    </div>

    <div class="workshop-controls" :class="{ 'single-button': isSingleControl }">
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
    </div>

    <div
      v-if="showBlockedPopup"
      class="active-workshop-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="active-workshop-title"
      @click.self="closeBlockedPopup"
    >
      <div class="active-workshop-dialog">
        <h4 id="active-workshop-title">Workshop already active</h4>
        <p>{{ blockedMessage }}</p>
        <button class="modal-close-btn" type="button" @click="closeBlockedPopup">
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex';
import StatusIndicator from '@/components/base/StatusIndicator.vue';

const LIFECYCLE_STEPS = [
  { key: 'REQUESTED', label: 'Requested' },
  { key: 'ADMITTED', label: 'Admitted' },
  { key: 'PROVISIONING', label: 'Provisioning' },
  { key: 'INITIALIZING', label: 'Initializing' },
  { key: 'READY', label: 'Ready' }
];
const STEP_INDEX_BY_STATE = {
  REQUESTED: 0,
  ADMITTED: 1,
  PROVISIONING: 2,
  INITIALIZING: 3,
  READY: 4,
  DEGRADED: 4,
  TERMINATING: 4,
  CLEANUP_PENDING: 4
};
const COMPLETED_LAUNCH_STATES = new Set(['READY', 'DEGRADED', 'TERMINATING', 'CLEANUP_PENDING']);

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
  data() {
    return {
      showBlockedPopup: false
    };
  },
  computed: {
    activeSession() {
      return this.workshop.activeSession;
    },

    blockingSession() {
      return this.workshop.blockingSession;
    },

    isBusy() {
      return Boolean(this.workshop.pendingAction);
    },

    canOpen() {
      return this.activeSession?.state === 'READY' && this.activeSession.publicEntryUrl;
    },

    isSingleControl() {
      return !this.activeSession || !this.canOpen;
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

    currentLifecycleStepIndex() {
      const state = this.activeSession?.state;
      if (!state) {
        return -1;
      }
      return STEP_INDEX_BY_STATE[state] ?? -1;
    },

    lifecycleSteps() {
      const currentIndex = this.currentLifecycleStepIndex;
      const launchComplete = COMPLETED_LAUNCH_STATES.has(this.activeSession?.state);
      return LIFECYCLE_STEPS.map((step, index) => {
        let status = 'pending';
        if (index < currentIndex || launchComplete) {
          status = 'complete';
        } else if (index === currentIndex) {
          status = 'current';
        }
        return {
          ...step,
          status
        };
      });
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

    stopLabel() {
      return this.workshop.pendingAction === 'terminating' ? 'Stopping...' : 'Stop';
    },

    blockedMessage() {
      if (this.workshop.blockingWorkshopTitle) {
        return `Stop ${this.workshop.blockingWorkshopTitle} before deploying this workshop.`;
      }
      return 'Stop your active workshop before deploying this workshop.';
    }
  },
  methods: {
    ...mapActions(['launchWorkshop', 'terminateWorkshop']),

    async handleLaunch() {
      if (this.blockingSession) {
        this.showBlockedPopup = true;
        return;
      }

      try {
        await this.launchWorkshop(this.workshop);
      } catch (error) {
        if (error.code === 'active_session_exists' || error.data?.code === 'active_session_exists') {
          this.showBlockedPopup = true;
        }
      }
    },

    closeBlockedPopup() {
      this.showBlockedPopup = false;
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
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: visible;
  padding: var(--spacing-6);
  position: relative;
  transition: all var(--transition-base);
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

.session-summary {
  align-items: center;
  display: flex;
  gap: var(--spacing-3);
  justify-content: space-between;
}

.session-panel strong {
  color: var(--color-text);
  margin-right: var(--spacing-2);
}

.session-panel span {
  text-transform: capitalize;
}

.session-expiry {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.session-progress {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-top: var(--spacing-1);
}

.session-step {
  align-items: center;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  font-size: 0.66rem;
  font-weight: var(--font-weight-medium);
  gap: var(--spacing-1);
  min-width: 0;
  position: relative;
  text-align: center;
}

.session-step:not(:last-child)::after {
  background-color: rgba(71, 85, 105, 0.7);
  content: '';
  height: 2px;
  left: calc(50% + 8px);
  position: absolute;
  top: 6px;
  width: calc(100% - 16px);
  z-index: 0;
}

.session-step.complete:not(:last-child)::after {
  background-color: rgba(134, 239, 172, 0.7);
}

.session-step-marker {
  background-color: var(--color-dark-700);
  border: 2px solid rgba(71, 85, 105, 0.9);
  border-radius: 999px;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.9);
  height: 14px;
  position: relative;
  width: 14px;
  z-index: 1;
}

.session-step.complete,
.session-step.current {
  color: var(--color-text);
}

.session-step.complete .session-step-marker {
  background-color: rgb(134, 239, 172);
  border-color: rgba(134, 239, 172, 0.8);
}

.session-step.current .session-step-marker {
  background-color: rgb(56, 189, 248);
  border-color: rgba(125, 211, 252, 0.9);
}

.session-step-label {
  overflow: hidden;
  text-overflow: ellipsis;
  text-transform: none;
  white-space: nowrap;
  width: 100%;
}

.failure-message {
  color: rgb(252, 165, 165);
}

.workshop-controls {
  display: flex;
  flex-wrap: nowrap;
  gap: var(--spacing-2);
  margin-top: auto;
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

.active-workshop-modal {
  align-items: center;
  background-color: rgba(2, 6, 23, 0.72);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: var(--spacing-6);
  position: fixed;
  z-index: var(--z-modal);
}

.active-workshop-dialog {
  background-color: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  color: var(--color-text-secondary);
  max-width: 440px;
  padding: var(--spacing-6);
  width: min(100%, 440px);
}

.active-workshop-dialog h4 {
  color: var(--color-text);
  font-size: var(--font-size-lg);
  margin: 0 0 var(--spacing-3);
}

.active-workshop-dialog p {
  line-height: 1.6;
  margin: 0 0 var(--spacing-5);
}

.modal-close-btn {
  background-color: rgba(0, 188, 212, 0.2);
  border: 1px solid rgba(0, 188, 212, 0.4);
  border-radius: var(--radius-md);
  color: var(--color-primary-400);
  cursor: pointer;
  font-family: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  min-height: 42px;
  padding: var(--spacing-3) var(--spacing-5);
  transition: all var(--transition-base);
}

.modal-close-btn:hover {
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

  .control-btn {
    min-width: 0;
  }

  .info-icon::after {
    right: -50px;
    width: 280px;
  }
}
</style>
