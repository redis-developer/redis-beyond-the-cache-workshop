<template>
  <div class="locks-implement">
    <div class="implement-container">
      <div v-if="contentError" class="content-error">
        {{ contentError }}
      </div>

      <WorkshopContentRenderer
        v-else-if="implementContent"
        :content="implementContent"
        :widgets="contentWidgets"
        :widget-props="contentWidgetProps"
        :action-handlers="contentActionHandlers"
        :show-title="true"
        :show-summary="true"
        :show-stage-title="false"
      />
    </div>
  </div>
</template>

<script>
import { WorkshopContentRenderer } from '../../../../../workshop-frontend-shared/src/index.js';
import LocksImplementStatusWidget from '../components/content/LocksImplementStatusWidget.vue';
import { getWorkshopHubUrl } from '../utils/basePath';
import { fetchLockStatus, updateLockTypeProgress } from '../utils/locksWorkshop';
import { fetchWorkshopContentView } from '../utils/workshopContent';

const CONTENT_WIDGETS = {
  'locks-implement.status': LocksImplementStatusWidget
};

export default {
  name: 'LocksImplement',
  components: {
    WorkshopContentRenderer
  },
  data() {
    return {
      lockConfigured: false,
      lockImplemented: false,
      checkingLock: false,
      implementContent: null,
      contentError: ''
    };
  },
  computed: {
    contentActionHandlers() {
      return {
        openEditor: () => this.$router.push('/reentrant/editor'),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args.route) {
            this.$router.push(args.route);
          }
        }
      };
    },
    contentWidgetProps() {
      return {
        'locks-implement.status': {
          checkingLock: this.checkingLock,
          lockConfigured: this.lockConfigured,
          lockImplemented: this.lockImplemented,
          onCheck: () => this.checkLockStatus(),
          onBack: () => this.$router.push('/reentrant'),
          onDemo: () => this.$router.push('/reentrant/demo')
        }
      };
    },
    contentWidgets() {
      return CONTENT_WIDGETS;
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  async mounted() {
    await Promise.all([
      this.loadContent(),
      this.checkLockStatus()
    ]);
  },
  methods: {
    async loadContent() {
      this.contentError = '';

      try {
        this.implementContent = await fetchWorkshopContentView('locks-implement');
      } catch (error) {
        this.contentError = error.message;
      }
    },
    async checkLockStatus() {
      this.checkingLock = true;
      try {
        const status = await fetchLockStatus();
        this.lockConfigured = status.configured;
        this.lockImplemented = status.implemented;

        if (status.implemented) {
          updateLockTypeProgress('reentrant', { implemented: true });
        }
      } finally {
        this.checkingLock = false;
      }
    }
  }
};
</script>

<style scoped>
.locks-implement {
  min-height: 100vh;
  background: var(--color-dark-900);
  color: var(--color-text);
  padding: var(--spacing-6);
}

.implement-container {
  max-width: 860px;
  margin: 0 auto;
}

.content-error {
  padding: var(--spacing-4);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(239, 68, 68, 0.45);
  background: rgba(127, 29, 29, 0.2);
  color: #fecaca;
}
</style>
