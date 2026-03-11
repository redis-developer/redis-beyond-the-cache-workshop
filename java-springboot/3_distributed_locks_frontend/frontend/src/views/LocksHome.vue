<template>
  <div class="locks-home">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="stageLabels"
      :current-step="currentStageIndex"
      clickable
      @step-click="goToStage"
    />

    <div class="hub-container">
      <div :class="currentStageId === 'problem' ? 'problem-stage' : 'hub-stage'">
        <div :class="currentStageId === 'problem' ? 'content-shell' : 'content-shell content-shell--hub'">
          <div v-if="currentStageTitle" class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>{{ currentStageTitle }}</h2>
          </div>

          <div v-if="contentError" class="content-error">
            {{ contentError }}
          </div>

          <WorkshopContentRenderer
            v-else-if="homeContent"
            :content="homeContent"
            :active-stage-id="currentStageId"
            :widgets="contentWidgets"
            :widget-props="contentWidgetProps"
            :action-handlers="contentActionHandlers"
            :show-title="false"
            :show-summary="false"
            :show-stage-title="false"
          />
        </div>
      </div>
    </div>

    <WorkshopModal
      v-model="modal.show"
      :title="modal.title"
      :message="modal.message"
      :type="modal.type"
      @confirm="handleModalConfirm"
      @cancel="handleModalCancel"
    />
  </div>
</template>

<script>
import { WorkshopContentRenderer } from '../../../../../workshop-frontend-shared/src/index.js';
import LocksHomeHubWidget from '../components/content/LocksHomeHubWidget.vue';
import LocksHomeProblemDemoWidget from '../components/content/LocksHomeProblemDemoWidget.vue';
import {
  LOCK_TYPES,
  clearProgress,
  getCompletedLockTypesCount,
  getLockTypeProgress,
  loadProgress,
  saveProgress
} from '../utils/locksWorkshop';
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';
import { WorkshopHeader, WorkshopModal } from '../utils/components';
import { fetchWorkshopContentView } from '../utils/workshopContent';

const DEFAULT_STAGES = [
  { stageId: 'problem', label: 'Problem' },
  { stageId: 'solution', label: 'Solution' }
];

const CONTENT_WIDGETS = {
  'locks-home.problem-demo': LocksHomeProblemDemoWidget,
  'locks-home.hub': LocksHomeHubWidget
};

export default {
  name: 'LocksHome',
  components: {
    WorkshopContentRenderer,
    WorkshopHeader,
    WorkshopModal
  },
  data() {
    return {
      lockTypes: LOCK_TYPES,
      currentStage: 1,
      restartingLab: false,
      runningJob: false,
      jobResult: null,
      lockProgress: {},
      homeContent: null,
      contentError: '',
      modal: {
        show: false,
        type: 'alert',
        title: '',
        message: '',
        onConfirm: null
      }
    };
  },
  computed: {
    completedCount() {
      return getCompletedLockTypesCount();
    },
    contentActionHandlers() {
      return {
        setStage: ({ args }) => this.setStageById(args.stageId),
        restartLab: () => this.confirmRestartLab()
      };
    },
    contentWidgetProps() {
      return {
        'locks-home.problem-demo': {
          jobResult: this.jobResult,
          runningJob: this.runningJob,
          onRun: () => this.runJobDemo()
        },
        'locks-home.hub': {
          lockTypes: this.lockTypes,
          lockProgress: this.lockProgress,
          completedCount: this.completedCount,
          restartingLab: this.restartingLab,
          onBack: () => this.prevStage(),
          onRestart: () => this.confirmRestartLab()
        }
      };
    },
    contentWidgets() {
      return CONTENT_WIDGETS;
    },
    currentStageId() {
      return this.stageDefinitions[this.currentStage - 1]?.stageId || this.stageDefinitions[0]?.stageId || 'problem';
    },
    currentStageIndex() {
      return Math.max(this.currentStage - 1, 0);
    },
    currentStageTitle() {
      return this.homeContent?.stages?.find(stage => stage.stageId === this.currentStageId)?.title || '';
    },
    stageDefinitions() {
      const steps = this.homeContent?.header?.stageNav?.steps;
      if (Array.isArray(steps) && steps.length) {
        return steps.map(step => ({
          stageId: step.stageId,
          label: step.label || step.stageId
        }));
      }

      return DEFAULT_STAGES;
    },
    stageLabels() {
      return this.stageDefinitions.map(step => step.label);
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    this.loadState();
    this.refreshLockProgress();
    await this.loadContent();
  },
  methods: {
    async loadContent() {
      this.contentError = '';

      try {
        this.homeContent = await fetchWorkshopContentView('locks-home');
        this.clampCurrentStage();
      } catch (error) {
        this.contentError = error.message;
      }
    },
    clampCurrentStage() {
      if (this.currentStage < 1 || this.currentStage > this.stageDefinitions.length) {
        this.currentStage = 1;
        this.saveState();
      }
    },
    loadState() {
      const progress = loadProgress();
      this.currentStage = progress.currentStage || 1;
    },
    saveState() {
      const progress = loadProgress();
      progress.currentStage = this.currentStage;
      saveProgress(progress);
    },
    goToStage(step) {
      if (step >= 1 && step <= this.stageDefinitions.length) {
        this.currentStage = step;
        this.saveState();
      }
    },
    setStageById(stageId) {
      const index = this.stageDefinitions.findIndex(stage => stage.stageId === stageId);
      if (index >= 0) {
        this.currentStage = index + 1;
        this.saveState();
      }
    },
    prevStage() {
      if (this.currentStage > 1) {
        this.currentStage--;
        this.saveState();
      }
    },
    refreshLockProgress() {
      const nextProgress = {};
      this.lockTypes.forEach(lockType => {
        nextProgress[lockType.id] = getLockTypeProgress(lockType.id);
      });
      this.lockProgress = nextProgress;
    },
    async runJobDemo() {
      this.runningJob = true;
      try {
        const response = await fetch(getApiUrl('/api/scenarios/jobs/run?workers=5'), { method: 'POST' });
        this.jobResult = await response.json();
      } catch (error) {
        console.error('Job demo failed:', error);
      } finally {
        this.runningJob = false;
      }
    },
    showModal(type, title, message, onConfirm = null) {
      this.modal = {
        show: true,
        type,
        title,
        message,
        onConfirm
      };
    },
    handleModalConfirm() {
      if (this.modal.onConfirm) {
        this.modal.onConfirm();
      }
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    handleModalCancel() {
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    confirmRestartLab() {
      this.showModal(
        'confirm',
        'Restart Workshop',
        'Are you sure you want to restart the workshop? This will restore all files to their original state and reset your progress. You will need to restart the application after this.',
        () => {
          this.restartLab();
        }
      );
    },
    async restartLab() {
      this.restartingLab = true;
      try {
        const response = await fetch(getApiUrl('/api/editor/restore'), { method: 'POST' });
        const data = await response.json();
        if (data.success) {
          clearProgress();
          this.currentStage = 1;
          this.jobResult = null;
          this.refreshLockProgress();
          this.showModal(
            'alert',
            'Workshop Reset',
            'Workshop reset! Please restart the application from the Workshop Hub.\n\nThen refresh this page to start from Stage 1.'
          );
        } else {
          this.showModal('alert', 'Error', 'Error: ' + (data.error || 'Failed to restore files'));
        }
      } catch (error) {
        console.error('Error restarting workshop:', error);
        this.showModal('alert', 'Error', 'Failed to restore files. Please try again.');
      } finally {
        this.restartingLab = false;
      }
    }
  }
};
</script>

<style scoped>
.locks-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.hub-container {
  max-width: 1200px;
  margin: 0 auto;
}

.content-shell {
  max-width: 860px;
  margin: 0 auto;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
}

.content-shell--hub {
  max-width: 1200px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.section-header h2 {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-xl);
}

.content-error {
  padding: var(--spacing-4);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(239, 68, 68, 0.45);
  background: rgba(127, 29, 29, 0.2);
  color: #fecaca;
}

:deep(.content-section__header h3) {
  color: var(--color-text);
}

@media (max-width: 768px) {
  .locks-home {
    padding: var(--spacing-4);
  }

  .content-shell {
    padding: var(--spacing-4);
  }
}
</style>
