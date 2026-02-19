<template>
  <div class="workshop-card">
    <div class="workshop-header">
      <h3>{{ workshop.title }}</h3>
      <div class="header-icons">
        <!-- Status Icon with Tooltip -->
        <div class="status-icon">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4zm2-1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H4z"/>
            <path d="M8 5.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5z"/>
          </svg>
          <div class="status-tooltip">
            <div class="status-tooltip-item">
              <StatusIndicator :status="infrastructureStatus.redis" />
              <span class="status-label">Redis</span>
            </div>
            <div class="status-tooltip-item">
              <StatusIndicator :status="infrastructureStatus.redisInsight" />
              <span class="status-label">Redis Insight</span>
            </div>
            <div class="status-tooltip-item">
              <StatusIndicator :status="currentWorkshopStatus" />
              <span class="status-label">Workshop App</span>
            </div>
          </div>
        </div>

        <!-- Info Icon with Tooltip -->
        <div class="info-icon" :data-tooltip="workshop.description">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zM7 5v2h2V5H7zm0 4v4h2V9H7z"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- Restart Progress -->
    <div v-if="isRestarting && restartProgress" class="restart-progress">
      <div class="progress-stages">
        <div
          v-for="stage in progressStages"
          :key="stage.id"
          class="progress-stage"
          :class="{
            'active': restartProgress.stage === stage.id,
            'completed': isStageCompleted(stage.id)
          }"
        >
          <div class="stage-indicator">
            <svg v-if="isStageCompleted(stage.id)" width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
              <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0L2.22 9.28a.75.75 0 011.06-1.06L6 10.94l6.72-6.72a.75.75 0 011.06 0z"/>
            </svg>
            <div v-else-if="restartProgress.stage === stage.id" class="stage-spinner"></div>
            <div v-else class="stage-dot"></div>
          </div>
          <span class="stage-label">{{ stage.label }}</span>
        </div>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>

    <!-- Control Buttons -->
    <div v-else class="workshop-controls" :class="{ 'single-button': showOnlyDeploy }">
      <button
        v-if="showDeployButton"
        class="control-btn deploy-btn"
        @click="handleDeploy"
        :disabled="loading"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M8 0l8 8-8 8V0z"/>
        </svg>
        Deploy Workshop
      </button>

      <button
        v-if="showRestartButton"
        class="control-btn restart-btn"
        @click="handleRestart"
        :disabled="loading"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M8 3V0L4 4l4 4V5a5 5 0 110 6v2a7 7 0 100-10z"/>
        </svg>
        Restart & Rebuild
      </button>

      <button
        v-if="showRestartNoBuildButton"
        class="control-btn restart-btn"
        @click="handleRestartNoBuild"
        :disabled="loading"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M8 3V0L4 4l4 4V5a5 5 0 110 6v2a7 7 0 100-10z"/>
        </svg>
        Restart App Only
      </button>

      <button
        v-if="showStopButton"
        class="control-btn stop-btn"
        @click="handleStop"
        :disabled="loading"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <rect x="4" y="4" width="8" height="8"/>
        </svg>
        Stop
      </button>

      <a
        v-if="showOpenButton"
        :href="workshop.url"
        class="control-btn open-btn"
        target="_blank"
      >
        Open Workshop
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M8 0l8 8-8 8V0z"/>
        </svg>
      </a>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex';
import StatusIndicator from '@/components/base/StatusIndicator.vue';

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
      loading: false
    };
  },
  computed: {
    ...mapGetters(['infrastructureStatus', 'workshopStatus', 'isWorkshopRestarting', 'getRestartProgress']),

    currentWorkshopStatus() {
      if (this.isRestarting) return 'restarting';
      return this.workshopStatus(this.workshop.id);
    },

    isRestarting() {
      return this.isWorkshopRestarting(this.workshop.id);
    },

    restartProgress() {
      return this.getRestartProgress(this.workshop.id);
    },

    progressStages() {
      if (!this.restartProgress) return [];
      // Deploy: no stopping phase
      if (this.restartProgress.isDeploy) {
        return [
          { id: 'building', label: 'Building' },
          { id: 'starting', label: 'Starting' },
          { id: 'initializing', label: 'Initializing' },
          { id: 'ready', label: 'Ready' }
        ];
      } else if (this.restartProgress.isRebuild) {
        return [
          { id: 'stopping', label: 'Stopping' },
          { id: 'building', label: 'Building' },
          { id: 'starting', label: 'Starting' },
          { id: 'initializing', label: 'Initializing' },
          { id: 'ready', label: 'Ready' }
        ];
      } else {
        return [
          { id: 'stopping', label: 'Stopping' },
          { id: 'starting', label: 'Starting' },
          { id: 'initializing', label: 'Initializing' },
          { id: 'ready', label: 'Ready' }
        ];
      }
    },

    progressPercent() {
      if (!this.restartProgress) return 0;
      const stageIndex = this.progressStages.findIndex(s => s.id === this.restartProgress.stage);
      if (stageIndex === -1) return 0;
      // Each stage represents equal progress, current stage is "in progress"
      return Math.min(100, ((stageIndex + 0.5) / this.progressStages.length) * 100);
    },

    workshopRunning() {
      return this.workshopStatus(this.workshop.id) === 'running';
    },

    allInfraRunning() {
      return this.infrastructureStatus.allRunning;
    },

    showDeployButton() {
      return !this.isRestarting && (!this.allInfraRunning || !this.workshopRunning);
    },

    showRestartButton() {
      return !this.isRestarting && this.allInfraRunning && this.workshopRunning;
    },

    showRestartNoBuildButton() {
      return !this.isRestarting && this.allInfraRunning && this.workshopRunning;
    },

    showStopButton() {
      return !this.isRestarting && this.allInfraRunning && this.workshopRunning;
    },

    showOpenButton() {
      return !this.isRestarting && this.workshopRunning;
    },

    showOnlyDeploy() {
      return this.showDeployButton && !this.showRestartButton && !this.showRestartNoBuildButton && !this.showStopButton && !this.showOpenButton;
    }
  },
  methods: {
    ...mapActions(['startInfrastructure', 'startWorkshop', 'stopInfrastructure', 'stopWorkshop', 'restartWorkshop', 'restartWorkshopNoBuild']),

    isStageCompleted(stageId) {
      if (!this.restartProgress) return false;
      const currentIndex = this.progressStages.findIndex(s => s.id === this.restartProgress.stage);
      const stageIndex = this.progressStages.findIndex(s => s.id === stageId);
      return stageIndex < currentIndex;
    },

    async handleDeploy() {
      this.loading = true;
      try {
        if (!this.allInfraRunning) {
          await this.startInfrastructure();
        }
        await this.startWorkshop(this.workshop.id);
      } catch (error) {
        console.error('Error deploying workshop:', error);
      } finally {
        this.loading = false;
      }
    },

    async handleRestart() {
      this.loading = true;
      try {
        await this.restartWorkshop(this.workshop.id);
      } catch (error) {
        console.error('Error restarting workshop:', error);
      } finally {
        this.loading = false;
      }
    },

	    async handleRestartNoBuild() {
	      this.loading = true;
	      try {
	        await this.restartWorkshopNoBuild(this.workshop.id);
	      } catch (error) {
	        console.error('Error restarting workshop without rebuild:', error);
	      } finally {
	        this.loading = false;
	      }
	    },

    async handleStop() {
      this.loading = true;
      try {
        await this.stopWorkshop(this.workshop.id);
        await this.stopInfrastructure();
      } catch (error) {
        console.error('Error stopping workshop:', error);
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.workshop-card {
  background-color: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--spacing-6);
  transition: all var(--transition-base);
  position: relative;
  overflow: visible;
}



.workshop-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-6);
  gap: var(--spacing-4);
}

.workshop-header h3 {
  font-size: var(--font-size-xl);
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
  margin: 0;
  flex: 1;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

.header-icons {
  display: flex;
  gap: var(--spacing-3);
  align-items: center;
  flex-shrink: 0;
}

.status-icon,
.info-icon {
  color: var(--color-text-secondary);
  cursor: help;
  position: relative;
  display: flex;
  align-items: center;
  transition: all var(--transition-fast);
}

.status-icon:hover,
.info-icon:hover {
  color: var(--color-primary-400);
}

/* Status Tooltip */
.status-tooltip {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: var(--spacing-2);
  background-color: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  min-width: 180px;
  z-index: var(--z-tooltip);
  box-shadow: var(--shadow-lg);
  animation: fadeIn var(--transition-fast);
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

.status-icon:hover .status-tooltip {
  display: block;
}

.status-tooltip-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-1) 0;
}

.status-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

/* Info Tooltip */
.info-icon::after {
  content: attr(data-tooltip);
  display: none;
  position: absolute;
  top: calc(100% + var(--spacing-2));
  right: 0;
  background-color: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  width: 320px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
  z-index: var(--z-tooltip);
  box-shadow: var(--shadow-lg);
  white-space: normal;
  animation: fadeIn var(--transition-fast);
  pointer-events: none;
}

.info-icon:hover::after {
  display: block;
}

/* Responsive tooltip positioning */
@media (max-width: 768px) {
  .info-icon::after {
    width: 280px;
    right: -50px;
  }
}

/* Workshop Controls */
.workshop-controls {
  display: flex;
  gap: var(--spacing-2);
  flex-wrap: wrap;
}

.workshop-controls.single-button {
  display: block;
}

.workshop-controls.single-button .control-btn {
  width: 100%;
  justify-content: center;
}

.control-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  border: 1px solid;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  background: transparent;
  font-family: inherit;
  text-decoration: none;
  flex: 1;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* Restart Progress */
.restart-progress {
  padding: var(--spacing-4);
  background: rgba(59, 130, 246, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.progress-stages {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-3);
}

.progress-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-1);
  flex: 1;
}

.stage-indicator {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stage-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-secondary);
  opacity: 0.4;
}

.stage-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(59, 130, 246, 0.3);
  border-top-color: rgb(59, 130, 246);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.progress-stage.completed .stage-indicator {
  color: rgb(34, 197, 94);
}

.progress-stage.active .stage-label {
  color: rgb(147, 197, 253);
  font-weight: var(--font-weight-medium);
}

.stage-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.progress-stage.completed .stage-label {
  color: rgb(134, 239, 172);
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: rgb(34, 197, 94);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* Responsive Design */
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
    justify-content: center;
  }

  .stage-label {
    font-size: 10px;
  }
}
</style>

