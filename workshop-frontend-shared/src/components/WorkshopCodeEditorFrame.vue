<template>
  <section class="workshop-code-editor-frame">
    <WorkshopToolFrame
      class="workshop-code-editor-frame__tool"
      :src="resolvedSrc"
      :title="title"
      :eyebrow="eyebrow"
      :allow="iframeAllow"
      @load="$emit('load', $event)"
      @error="$emit('error', $event)"
    >
      <template #actions>
        <slot name="before-actions"></slot>
        <button
          v-if="showReset"
          type="button"
          class="workshop-code-editor-frame__action workshop-code-editor-frame__action--danger"
          :disabled="controlsDisabled"
          @click="resetCode"
        >
          {{ isResetting ? 'Resetting Code' : 'Reset Code' }}
        </button>
        <button
          v-if="showRecompile"
          type="button"
          class="workshop-code-editor-frame__action workshop-code-editor-frame__action--accent"
          :disabled="controlsDisabled"
          @click="recompileApp"
        >
          {{ isRecompiling ? 'Recompiling App' : 'Recompile App' }}
        </button>
        <button
          v-if="showRuntimeLogs"
          type="button"
          class="workshop-code-editor-frame__action"
          :class="{ 'workshop-code-editor-frame__action--active': logsOpen }"
          :aria-pressed="logsOpen ? 'true' : 'false'"
          @click="toggleRuntimeLogs"
        >
          Logs
        </button>
        <slot name="actions"></slot>
      </template>
    </WorkshopToolFrame>

    <p
      v-if="statusMessage"
      class="workshop-code-editor-frame__status"
      :class="statusClass"
      aria-live="polite"
    >
      {{ statusMessage }}
    </p>

    <WorkshopModal
      v-model="showResetConfirmation"
      title="Reset Code"
      :message="resetConfirmationMessage"
      type="confirm"
      confirm-text="Reset Code"
      @confirm="confirmResetCode"
    />

    <aside
      v-if="logsOpen"
      class="workshop-code-editor-frame__logs"
      aria-label="Runtime logs"
    >
      <header class="workshop-code-editor-frame__logs-header">
        <p class="workshop-code-editor-frame__logs-title">Runtime logs</p>
        <button
          type="button"
          class="workshop-code-editor-frame__logs-close"
          aria-label="Close runtime logs"
          @click="toggleRuntimeLogs"
        >
          Close
        </button>
      </header>
      <pre class="workshop-code-editor-frame__logs-body">{{ formattedRuntimeLogs }}</pre>
    </aside>
  </section>
</template>

<script>
import WorkshopModal from './WorkshopModal.vue';
import WorkshopToolFrame from './WorkshopToolFrame.vue';
import { getApiUrl, getCodeEditorUrl } from '../utils/basePath.js';

const READY_STATES = new Set(['CHILD_READY', 'READY', 'RUNNING']);
const FAILED_STATES = new Set(['CHILD_FAILED', 'DISABLED', 'FAILED']);
const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 480000;

function appendReloadToken(src, token) {
  if (!token) {
    return src;
  }

  const hashIndex = src.indexOf('#');
  const beforeHash = hashIndex >= 0 ? src.slice(0, hashIndex) : src;
  const hash = hashIndex >= 0 ? src.slice(hashIndex) : '';
  const separator = beforeHash.includes('?') ? '&' : '?';

  return `${beforeHash}${separator}workshopReload=${encodeURIComponent(token)}${hash}`;
}

function normalizeRunnerState(state) {
  return String(state || '').trim().toUpperCase();
}

export default {
  name: 'WorkshopCodeEditorFrame',
  components: { WorkshopModal, WorkshopToolFrame },
  props: {
    src: { type: String, default: '' },
    title: { type: String, default: 'VS Code editor' },
    eyebrow: { type: String, default: 'Code editor' },
    iframeAllow: { type: String, default: 'clipboard-read; clipboard-write' },
    restoreEndpoint: { type: String, default: '/internal/session-runner/restore' },
    restartEndpoint: { type: String, default: '/internal/session-runner/restart' },
    statusEndpoint: { type: String, default: '/internal/session-runner/status' },
    resetConfirmationMessage: {
      type: String,
      default: 'Reset all workshop code files to their original state? This will overwrite your saved changes.'
    },
    showReset: { type: Boolean, default: true },
    showRecompile: { type: Boolean, default: true },
    showRuntimeLogs: { type: Boolean, default: true },
    actionsDisabled: { type: Boolean, default: false }
  },
  emits: [
    'error',
    'load',
    'reset-start',
    'reset-success',
    'reset-error',
    'recompile-start',
    'recompile-success',
    'recompile-error'
  ],
  data() {
    return {
      isResetting: false,
      isRecompiling: false,
      showResetConfirmation: false,
      editorReloadRequest: 0,
      logsOpen: false,
      runtimeLogs: [],
      runtimeLogsTimer: null,
      statusMessage: '',
      statusType: '',
      statusTimer: null
    };
  },
  computed: {
    resolvedSrc() {
      const editorSrc = this.src || getCodeEditorUrl();

      return appendReloadToken(editorSrc, this.editorReloadRequest);
    },
    controlsDisabled() {
      return this.actionsDisabled || this.isResetting || this.isRecompiling;
    },
    statusClass() {
      return {
        'workshop-code-editor-frame__status--error': this.statusType === 'error',
        'workshop-code-editor-frame__status--success': this.statusType === 'success'
      };
    },
    formattedRuntimeLogs() {
      if (!this.runtimeLogs.length) {
        return 'No runtime logs captured yet.';
      }

      return this.runtimeLogs.join('\n');
    }
  },
  beforeUnmount() {
    this.clearStatusTimer();
    this.stopRuntimeLogPolling();
  },
  methods: {
    async resetCode() {
      if (this.controlsDisabled) {
        return;
      }

      this.showResetConfirmation = true;
    },
    async confirmResetCode() {
      if (this.controlsDisabled) {
        return;
      }

      this.isResetting = true;
      this.showStatus('Resetting code...', 'info');
      this.$emit('reset-start');

      try {
        const response = await fetch(getApiUrl(this.restoreEndpoint), {
          method: 'POST',
          credentials: 'include'
        });
        const data = await this.readResponseBody(response);

        if (!response.ok || data.error) {
          throw new Error(data.error || data.message || 'Failed to reset code');
        }

        this.editorReloadRequest += 1;
        this.showStatus('Code reset. Recompile App to apply it.', 'success');
        this.$emit('reset-success', data);
      } catch (error) {
        this.showStatus(`Failed to reset code: ${error.message}`, 'error');
        this.$emit('reset-error', error);
      } finally {
        this.isResetting = false;
      }
    },
    async recompileApp() {
      if (this.controlsDisabled) {
        return;
      }

      this.isRecompiling = true;
      this.showStatus('Starting app recompile...', 'info', 0);
      this.$emit('recompile-start');

      try {
        const response = await fetch(getApiUrl(this.restartEndpoint), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ rebuild: true, async: true })
        });
        const data = await this.readResponseBody(response);

        if (!response.ok || data.error) {
          throw new Error(data.error || data.message || 'Failed to recompile app');
        }

        this.showStatus('Recompiling app. Open Logs if this takes longer than expected.', 'info', 0);
        await this.waitForRuntimeReady();
        this.showStatus('Recompile complete. Return to the learner app to verify.', 'success');
        this.$emit('recompile-success', data);
      } catch (error) {
        this.showStatus(`Failed to recompile app: ${error.message}`, 'error');
        this.$emit('recompile-error', error);
      } finally {
        this.isRecompiling = false;
        if (!this.logsOpen) {
          this.stopRuntimeLogPolling();
        }
      }
    },
    async readResponseBody(response) {
      const text = await response.text();

      if (!text) {
        return {};
      }

      try {
        return JSON.parse(text);
      } catch {
        return {
          error: response.ok ? '' : text
        };
      }
    },
    async waitForRuntimeReady() {
      const startedAt = Date.now();

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const status = await this.fetchRuntimeStatus();
        const state = normalizeRunnerState(status?.state);

        if (READY_STATES.has(state)) {
          return;
        }
        if (FAILED_STATES.has(state)) {
          throw new Error(status?.lastError || 'Session recompile failed');
        }

        await this.sleep(POLL_INTERVAL_MS);
      }

      throw new Error('Timed out waiting for the app to recompile');
    },
    async fetchRuntimeStatus() {
      try {
        const response = await fetch(getApiUrl(this.statusEndpoint), {
          credentials: 'include'
        });

        if (!response.ok) {
          return null;
        }

        const status = await response.json();
        this.applyRuntimeLogs(status);
        return status;
      } catch {
        return null;
      }
    },
    applyRuntimeLogs(status) {
      if (Array.isArray(status?.recentLogs)) {
        this.runtimeLogs = status.recentLogs.slice(-80);
      }
    },
    toggleRuntimeLogs() {
      this.logsOpen = !this.logsOpen;
      if (this.logsOpen) {
        this.fetchRuntimeStatus();
        this.startRuntimeLogPolling();
        return;
      }

      if (!this.isRecompiling) {
        this.stopRuntimeLogPolling();
      }
    },
    startRuntimeLogPolling() {
      if (this.runtimeLogsTimer) {
        return;
      }

      this.runtimeLogsTimer = window.setInterval(() => {
        this.fetchRuntimeStatus();
      }, POLL_INTERVAL_MS);
    },
    stopRuntimeLogPolling() {
      if (!this.runtimeLogsTimer) {
        return;
      }

      window.clearInterval(this.runtimeLogsTimer);
      this.runtimeLogsTimer = null;
    },
    sleep(ms) {
      return new Promise(resolve => window.setTimeout(resolve, ms));
    },
    showStatus(message, type, durationMs = 5000) {
      this.statusMessage = message;
      this.statusType = type;
      this.clearStatusTimer();
      if (durationMs <= 0) {
        return;
      }
      this.statusTimer = window.setTimeout(() => {
        this.statusMessage = '';
        this.statusType = '';
        this.statusTimer = null;
      }, durationMs);
    },
    clearStatusTimer() {
      if (this.statusTimer) {
        window.clearTimeout(this.statusTimer);
        this.statusTimer = null;
      }
    }
  }
};
</script>

<style scoped>
.workshop-code-editor-frame {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: 100%;
}

.workshop-code-editor-frame__tool {
  flex: 1;
  min-height: 0;
}

.workshop-code-editor-frame__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.85rem;
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  font-family: inherit;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, opacity 0.2s ease;
}

.workshop-code-editor-frame__action:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-code-editor-frame__action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.workshop-code-editor-frame__action--accent {
  border-color: rgba(245, 158, 11, 0.45);
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
}

.workshop-code-editor-frame__action--danger {
  border-color: rgba(239, 68, 68, 0.42);
  background: rgba(239, 68, 68, 0.16);
  color: #fca5a5;
}

.workshop-code-editor-frame__action--active {
  background: rgba(59, 130, 246, 0.32);
  border-color: rgba(59, 130, 246, 0.68);
  color: #bfdbfe;
}

.workshop-code-editor-frame__status {
  position: fixed;
  right: var(--spacing-5, 1.25rem);
  bottom: var(--spacing-5, 1.25rem);
  z-index: 1200;
  max-width: min(30rem, calc(100vw - 2rem));
  margin: 0;
  padding: var(--spacing-3, 0.75rem) var(--spacing-4, 1rem);
  border: 1px solid rgba(59, 130, 246, 0.42);
  border-radius: var(--radius-lg, 8px);
  background: rgba(13, 26, 34, 0.96);
  box-shadow: var(--shadow-xl, 0 20px 25px -5px rgba(0, 0, 0, 0.35));
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1.5;
}

.workshop-code-editor-frame__status--success {
  border-color: rgba(74, 222, 128, 0.4);
  color: #bbf7d0;
}

.workshop-code-editor-frame__status--error {
  border-color: rgba(239, 68, 68, 0.46);
  color: #fecaca;
}

.workshop-code-editor-frame__logs {
  position: absolute;
  right: var(--spacing-3, 0.75rem);
  bottom: var(--spacing-3, 0.75rem);
  left: var(--spacing-3, 0.75rem);
  z-index: 4;
  display: flex;
  flex-direction: column;
  max-height: min(22rem, 45%);
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.6);
  border-radius: var(--radius-lg, 8px);
  background: rgba(2, 8, 13, 0.94);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.42);
}

.workshop-code-editor-frame__logs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  padding: 0.55rem 0.75rem;
  border-bottom: 1px solid rgba(71, 85, 105, 0.42);
  background: rgba(13, 26, 34, 0.96);
}

.workshop-code-editor-frame__logs-title {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-code-editor-frame__logs-close {
  border: 0;
  background: transparent;
  color: var(--color-restart-text, #93c5fd);
  font-family: inherit;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  cursor: pointer;
}

.workshop-code-editor-frame__logs-close:hover {
  color: #bfdbfe;
}

.workshop-code-editor-frame__logs-body {
  margin: 0;
  padding: 0.75rem;
  overflow: auto;
  color: #cbd5e1;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 0.72rem;
  line-height: 1.55;
  white-space: pre-wrap;
}

@media (max-width: 720px) {
  .workshop-code-editor-frame__status {
    right: var(--spacing-3, 0.75rem);
    bottom: var(--spacing-3, 0.75rem);
  }

  .workshop-code-editor-frame__logs {
    max-height: 55%;
  }
}
</style>
