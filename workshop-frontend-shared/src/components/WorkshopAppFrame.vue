<template>
  <section
    class="workshop-app-frame"
    :class="{
      'workshop-app-frame--blocked': isBlocked,
      'workshop-app-frame--maximized': isMaximized
    }"
  >
    <header class="workshop-app-frame__header">
      <div class="workshop-app-frame__identity">
        <p class="workshop-app-frame__eyebrow">{{ eyebrow }}</p>
      </div>
      <div class="workshop-app-frame__actions">
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--busy': restartInProgress }"
          :disabled="!canRestart || actionsDisabled || Boolean(pendingAction)"
          @click="handleRuntimeAction('restart')"
        >
          {{ restartInProgress ? 'Restarting App' : 'Restart App' }}
        </button>
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--busy': rebuildInProgress }"
          :disabled="!canRebuild || actionsDisabled || Boolean(pendingAction)"
          @click="handleRuntimeAction('rebuild')"
        >
          {{ rebuildInProgress ? 'Recompiling App' : 'Recompile App' }}
        </button>
        <button
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--active': logsOpen }"
          :aria-pressed="logsOpen ? 'true' : 'false'"
          @click="toggleLogs"
        >
          Logs
        </button>
        <button
          v-if="showEnvironmentControls"
          type="button"
          class="workshop-app-frame__action"
          :class="{ 'workshop-app-frame__action--active': environmentOpen }"
          :disabled="actionsDisabled || Boolean(pendingAction) || environmentSaving"
          :aria-pressed="environmentOpen ? 'true' : 'false'"
          @click="toggleEnvironment"
        >
          Env
        </button>
        <button
          type="button"
          class="workshop-app-frame__action workshop-app-frame__action--icon"
          :class="{ 'workshop-app-frame__action--busy': refreshInProgress }"
          :disabled="!src || Boolean(pendingAction)"
          :aria-label="refreshInProgress ? 'Refreshing app' : 'Refresh app'"
          :title="refreshInProgress ? 'Refreshing app' : 'Refresh app'"
          @click="handleRetry"
        >
          <svg
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M21 12a9 9 0 0 1-15.5 6.25" />
            <path d="M3 12A9 9 0 0 1 18.5 5.75" />
            <path d="M18 2v4h4" />
            <path d="M6 22v-4H2" />
          </svg>
        </button>
        <button
          type="button"
          class="workshop-app-frame__action workshop-app-frame__action--icon"
          :aria-pressed="isMaximized ? 'true' : 'false'"
          :aria-label="isMaximized ? 'Restore app' : 'Maximize app'"
          :title="isMaximized ? 'Restore app' : 'Maximize app'"
          @click="toggleMaximized"
        >
          <svg
            v-if="isMaximized"
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M9 3v6H3" />
            <path d="M15 3v6h6" />
            <path d="M9 21v-6H3" />
            <path d="M15 21v-6h6" />
          </svg>
          <svg
            v-else
            class="workshop-app-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M8 4H4v4" />
            <path d="M16 4h4v4" />
            <path d="M8 20H4v-4" />
            <path d="M20 16v4h-4" />
          </svg>
        </button>
      </div>
    </header>

    <div class="workshop-app-frame__viewport">
      <iframe
        v-if="src && !frameSuppressed"
        :key="frameKey"
        class="workshop-app-frame__iframe"
        :src="src"
        :title="title"
        @load="handleLoad"
        @error="handleError"
      ></iframe>

      <div v-if="overlayMessage" class="workshop-app-frame__overlay" aria-live="polite">
        <p class="workshop-app-frame__state">{{ overlayTitle }}</p>
        <p class="workshop-app-frame__message">{{ overlayMessage }}</p>
      </div>

      <aside
        v-if="logsOpen"
        class="workshop-app-frame__logs"
        aria-label="Runtime logs"
      >
        <header class="workshop-app-frame__logs-header">
          <p class="workshop-app-frame__logs-title">Runtime logs</p>
          <button
            type="button"
            class="workshop-app-frame__logs-close"
            aria-label="Close runtime logs"
            @click="toggleLogs"
          >
            Close
          </button>
        </header>
        <pre class="workshop-app-frame__logs-body">{{ formattedRuntimeLogs }}</pre>
      </aside>

      <aside
        v-if="environmentOpen"
        class="workshop-app-frame__environment"
        aria-label="Environment variables"
      >
        <header class="workshop-app-frame__environment-header">
          <p class="workshop-app-frame__environment-title">Environment variables</p>
          <button
            type="button"
            class="workshop-app-frame__environment-close"
            aria-label="Close environment variables"
            @click="toggleEnvironment"
          >
            Close
          </button>
        </header>

        <div class="workshop-app-frame__environment-body">
          <p
            v-if="environmentError"
            class="workshop-app-frame__environment-message workshop-app-frame__environment-message--error"
            role="alert"
          >
            {{ environmentError }}
          </p>
          <p
            v-else-if="environmentNotice"
            class="workshop-app-frame__environment-message"
            role="status"
          >
            {{ environmentNotice }}
          </p>

          <div class="workshop-app-frame__environment-rows">
            <div
              v-for="row in environmentRows"
              :key="row.id"
              class="workshop-app-frame__environment-row"
            >
              <label class="workshop-app-frame__environment-field">
                <span>Name</span>
                <input
                  v-model="row.key"
                  type="text"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="OPENAI_API_KEY"
                  :disabled="environmentLoading || environmentSaving"
                />
              </label>
              <label class="workshop-app-frame__environment-field">
                <span>Value</span>
                <input
                  v-model="row.value"
                  type="password"
                  autocomplete="off"
                  spellcheck="false"
                  :placeholder="row.existing ? 'Hidden' : 'Value'"
                  :disabled="environmentLoading || environmentSaving"
                />
              </label>
              <button
                type="button"
                class="workshop-app-frame__environment-remove"
                :disabled="environmentLoading || environmentSaving"
                @click="removeEnvironmentRow(row)"
              >
                Remove
              </button>
            </div>
          </div>

          <div class="workshop-app-frame__environment-footer">
            <button
              type="button"
              class="workshop-app-frame__environment-secondary"
              :disabled="environmentLoading || environmentSaving"
              @click="addEnvironmentRow"
            >
              Add variable
            </button>
            <button
              type="button"
              class="workshop-app-frame__environment-primary"
              :disabled="environmentLoading || environmentSaving"
              @click="saveEnvironment"
            >
              {{ environmentSaving ? 'Applying' : 'Save and restart' }}
            </button>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script>
import {
  isRuntimeBlocked,
  isRuntimeBusy,
  isRuntimeReady,
  normalizeRuntimeState
} from '../composables/useWorkshopShellState.js';
import { getApiUrl } from '../utils/basePath.js';

export default {
  name: 'WorkshopAppFrame',
  props: {
    src: { type: String, default: '' },
    title: { type: String, default: 'Learner application' },
    eyebrow: { type: String, default: 'Learner app' },
    state: { type: String, default: 'unknown' },
    message: { type: String, default: '' },
    refreshKey: { type: [String, Number], default: 0 },
    canRestart: { type: Boolean, default: false },
    canRebuild: { type: Boolean, default: false },
    actionsDisabled: { type: Boolean, default: false },
    runtimeLogs: { type: Array, default: () => [] },
    showEnvironmentControls: { type: Boolean, default: true },
    environmentEndpoint: { type: String, default: '/internal/session-runner/environment' },
    environmentStatusEndpoint: { type: String, default: '/internal/session-runner/status' }
  },
  emits: ['load', 'error', 'retry', 'runtime-action', 'logs-toggle', 'environment-updated'],
  data() {
    return {
      loading: Boolean(this.src),
      frameError: false,
      localRefreshKey: 0,
      pendingAction: '',
      pendingActionTimer: null,
      logsOpen: false,
      environmentOpen: false,
      environmentLoading: false,
      environmentSaving: false,
      environmentRows: [],
      environmentRemovedKeys: [],
      environmentError: '',
      environmentNotice: '',
      nextEnvironmentRowId: 1,
      isMaximized: false,
      previousBodyOverflow: ''
    };
  },
  computed: {
    normalizedState() {
      return normalizeRuntimeState(this.state);
    },
    frameKey() {
      return `${this.refreshKey}-${this.localRefreshKey}`;
    },
    isBlocked() {
      return !this.src || isRuntimeBlocked(this.normalizedState);
    },
    frameSuppressed() {
      return this.isBlocked
        || isRuntimeBusy(this.normalizedState)
        || ['restart', 'rebuild'].includes(this.pendingAction);
    },
    restartInProgress() {
      return this.pendingAction === 'restart' || this.normalizedState === 'restarting';
    },
    rebuildInProgress() {
      return this.pendingAction === 'rebuild' || this.normalizedState === 'rebuilding';
    },
    refreshInProgress() {
      return this.pendingAction === 'refresh' || this.loading;
    },
    overlayTitle() {
      if (!this.src) {
        return 'App URL unavailable';
      }

      if (this.frameError) {
        return 'App failed to load';
      }

      if (this.pendingAction === 'restart') {
        return 'Restarting app';
      }

      if (this.pendingAction === 'rebuild') {
        return 'Rebuilding app';
      }

      if (this.pendingAction === 'refresh') {
        return 'Refreshing app';
      }

      if (this.normalizedState === 'restarting') {
        return 'Restarting app';
      }

      if (this.normalizedState === 'rebuilding') {
        return 'Rebuilding app';
      }

      if (isRuntimeBusy(this.normalizedState)) {
        return 'Preparing app';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'App unavailable';
      }

      if (this.loading) {
        return 'Refreshing app';
      }

      return '';
    },
    overlayMessage() {
      if (this.message) {
        return this.message;
      }

      if (!this.src) {
        return 'The platform has not provided a learner app URL yet.';
      }

      if (this.frameError) {
        return 'Reload the app after the runtime is ready.';
      }

      if (this.pendingAction === 'restart') {
        return 'The learner app is restarting. The frame will reload automatically when it is ready.';
      }

      if (this.pendingAction === 'rebuild') {
        return 'The learner app is rebuilding. The frame will reload automatically when it is ready.';
      }

      if (this.pendingAction === 'refresh') {
        return 'Reloading the learner app frame.';
      }

      if (this.normalizedState === 'restarting') {
        return 'The learner app is restarting. The frame will reload automatically when it is ready.';
      }

      if (this.normalizedState === 'rebuilding') {
        return 'The learner app is rebuilding. The frame will reload automatically when it is ready.';
      }

      if (isRuntimeBusy(this.normalizedState)) {
        return 'The shell stays available while the learner app becomes ready.';
      }

      if (isRuntimeBlocked(this.normalizedState)) {
        return 'Restart or rebuild the runtime from the learner app header.';
      }

      if (this.loading) {
        return 'Reloading the learner app frame.';
      }

      return '';
    },
    formattedRuntimeLogs() {
      if (!this.runtimeLogs.length) {
        return 'No runtime logs captured yet.';
      }

      return this.runtimeLogs.join('\n');
    }
  },
  watch: {
    src: {
      handler(value) {
        this.loading = Boolean(value);
        this.frameError = false;
      }
    },
    refreshKey() {
      this.loading = Boolean(this.src);
      this.frameError = false;
    },
    normalizedState(value) {
      if (!isRuntimeBusy(value)) {
        this.clearPendingAction();
      }
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown);
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown);
    this.setBodyScrollLock(false);
    this.clearPendingActionTimer();
  },
  methods: {
    handleLoad(event) {
      this.loading = false;
      this.frameError = false;
      if (this.pendingAction === 'refresh' && !isRuntimeBusy(this.normalizedState)) {
        this.clearPendingAction();
      }
      this.$emit('load', event);
    },
    handleError(event) {
      this.loading = false;
      this.frameError = true;
      this.$emit('error', event);
    },
    handleRuntimeAction(type) {
      this.setPendingAction(type);
      this.$emit('runtime-action', {
        type,
        rebuild: type === 'rebuild'
      });
    },
    handleRetry() {
      this.setPendingAction('refresh');
      this.loading = Boolean(this.src);
      this.frameError = false;
      this.$emit('retry');
    },
    toggleLogs() {
      this.logsOpen = !this.logsOpen;
      if (this.logsOpen) {
        this.environmentOpen = false;
      }
      this.$emit('logs-toggle', this.logsOpen);
    },
    async toggleEnvironment() {
      this.environmentOpen = !this.environmentOpen;
      this.environmentError = '';
      this.environmentNotice = '';

      if (!this.environmentOpen) {
        return;
      }

      if (this.logsOpen) {
        this.logsOpen = false;
        this.$emit('logs-toggle', false);
      }

      await this.loadEnvironment();
    },
    createEnvironmentRow(key = '', value = '', existing = false) {
      return {
        id: this.nextEnvironmentRowId++,
        key,
        value,
        existing,
        originalKey: key
      };
    },
    addEnvironmentRow() {
      this.environmentRows = [
        ...this.environmentRows,
        this.createEnvironmentRow()
      ];
    },
    removeEnvironmentRow(row) {
      const key = String(row.originalKey || row.key || '').trim();
      if (row.existing && key) {
        this.environmentRemovedKeys = [
          ...new Set([...this.environmentRemovedKeys, key])
        ];
      }

      this.environmentRows = this.environmentRows.filter(item => item.id !== row.id);
      if (this.environmentRows.length === 0) {
        this.addEnvironmentRow();
      }
    },
    async loadEnvironment() {
      this.environmentLoading = true;
      this.environmentError = '';

      try {
        const response = await fetch(getApiUrl(this.environmentEndpoint), {
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error(await this.readEnvironmentError(response, 'Unable to load environment variables'));
        }

        const body = await response.json();
        const names = Array.isArray(body?.variableNames) ? body.variableNames : [];
        this.environmentRows = names.map(name => this.createEnvironmentRow(name, '', true));
        this.environmentRemovedKeys = [];

        if (this.environmentRows.length === 0) {
          this.addEnvironmentRow();
        }
      } catch (error) {
        this.environmentError = error.message || 'Unable to load environment variables';
        if (this.environmentRows.length === 0) {
          this.addEnvironmentRow();
        }
      } finally {
        this.environmentLoading = false;
      }
    },
    buildEnvironmentPayload() {
      const upsert = {};
      const remove = [...this.environmentRemovedKeys];
      const seenKeys = new Set();

      for (const row of this.environmentRows) {
        const key = String(row.key || '').trim();
        const value = row.value ?? '';
        const originalKey = String(row.originalKey || '').trim();

        if (!key && !value) {
          continue;
        }

        if (!key) {
          throw new Error('Enter a variable name before saving.');
        }

        if (seenKeys.has(key)) {
          throw new Error(`Remove the duplicate ${key} entry before saving.`);
        }
        seenKeys.add(key);

        if (row.existing && originalKey && originalKey !== key) {
          remove.push(originalKey);
          if (!value) {
            throw new Error(`Enter a value for ${key} before saving.`);
          }
        }

        if (row.existing && originalKey === key && !value) {
          continue;
        }

        if (!value) {
          throw new Error(`Enter a value for ${key} before saving.`);
        }

        upsert[key] = value;
      }

      return {
        upsert,
        remove: [...new Set(remove)]
      };
    },
    async saveEnvironment() {
      this.environmentSaving = true;
      this.environmentError = '';
      this.environmentNotice = '';

      try {
        const payload = this.buildEnvironmentPayload();
        if (Object.keys(payload.upsert).length === 0 && payload.remove.length === 0) {
          this.environmentNotice = 'No changes to save.';
          return;
        }

        this.pendingAction = 'restart';
        this.clearPendingActionTimer();

        const response = await fetch(getApiUrl(this.environmentEndpoint), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({
            ...payload,
            restart: true,
            async: true
          })
        });

        if (!response.ok) {
          throw new Error(await this.readEnvironmentError(response, 'Unable to save environment variables'));
        }

        const requestedStatus = await response.json().catch(() => null);
        const status = await this.waitForEnvironmentReady(requestedStatus);
        const names = Array.isArray(status?.environmentVariableNames)
          ? status.environmentVariableNames
          : Object.keys(payload.upsert);
        this.environmentRows = names.map(name => this.createEnvironmentRow(name, '', true));
        this.environmentRemovedKeys = [];
        this.environmentNotice = 'Environment saved. Learner app restarted.';
        this.localRefreshKey += 1;
        this.$emit('environment-updated', status);
      } catch (error) {
        this.environmentError = error.message || 'Unable to save environment variables';
      } finally {
        this.environmentSaving = false;
        this.clearPendingAction();
      }
    },
    async waitForEnvironmentReady(initialStatus) {
      if (initialStatus && isRuntimeReady(initialStatus.state)) {
        return initialStatus;
      }

      const deadline = Date.now() + 480000;
      let status = initialStatus;
      while (Date.now() < deadline) {
        await this.sleep(2000);
        status = await this.fetchEnvironmentStatus();

        if (isRuntimeReady(status?.state)) {
          return status;
        }

        if (isRuntimeBlocked(status?.state)) {
          throw new Error(status?.lastError || 'Environment update failed.');
        }
      }

      throw new Error('Timed out waiting for the learner app to restart.');
    },
    async fetchEnvironmentStatus() {
      const response = await fetch(getApiUrl(this.environmentStatusEndpoint), {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(await this.readEnvironmentError(response, 'Unable to read runtime status'));
      }

      return response.json();
    },
    sleep(ms) {
      return new Promise(resolve => window.setTimeout(resolve, ms));
    },
    async readEnvironmentError(response, fallbackMessage) {
      const text = await response.text();

      if (!text) {
        return fallbackMessage;
      }

      try {
        const data = JSON.parse(text);
        if (data?.message) {
          return `${fallbackMessage}: ${data.message}`;
        }
        if (data?.detail) {
          return `${fallbackMessage}: ${data.detail}`;
        }
        if (data?.error) {
          return `${fallbackMessage}: ${data.error}`;
        }
      } catch {
        return `${fallbackMessage}: ${text}`;
      }

      return fallbackMessage;
    },
    toggleMaximized() {
      this.isMaximized = !this.isMaximized;
      this.setBodyScrollLock(this.isMaximized);
    },
    handleKeydown(event) {
      if (event.key === 'Escape' && this.isMaximized) {
        this.isMaximized = false;
        this.setBodyScrollLock(false);
      }
    },
    setBodyScrollLock(locked) {
      if (locked) {
        if (!this.previousBodyOverflow) {
          this.previousBodyOverflow = document.body.style.overflow || '';
        }
        document.body.style.overflow = 'hidden';
        return;
      }

      document.body.style.overflow = this.previousBodyOverflow;
      this.previousBodyOverflow = '';
    },
    setPendingAction(type) {
      this.pendingAction = type;
      this.clearPendingActionTimer();
      this.pendingActionTimer = window.setTimeout(() => {
        if (!isRuntimeBusy(this.normalizedState)) {
          this.pendingAction = '';
        }
      }, 1500);
    },
    clearPendingAction() {
      this.pendingAction = '';
      this.clearPendingActionTimer();
    },
    clearPendingActionTimer() {
      if (this.pendingActionTimer) {
        window.clearTimeout(this.pendingActionTimer);
        this.pendingActionTimer = null;
      }
    }
  }
};
</script>

<style scoped>
.workshop-app-frame {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: var(--color-dark-900, #0A151B);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-app-frame--maximized {
  position: fixed;
  inset: calc(72px + var(--spacing-4, 1rem)) var(--spacing-4, 1rem) var(--spacing-4, 1rem);
  z-index: 1000;
  width: auto;
  height: auto;
  min-height: 0;
  border-radius: var(--radius-xl, 0.75rem);
  box-shadow:
    0 0 0 100vmax var(--color-background, #0A151B),
    0 24px 80px rgba(0, 0, 0, 0.5);
}

.workshop-app-frame__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  min-height: 2.75rem;
  padding: var(--spacing-2, 0.5rem) var(--spacing-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.workshop-app-frame__identity {
  min-width: 0;
}

.workshop-app-frame__eyebrow {
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-app-frame__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-2, 0.5rem);
}

.workshop-app-frame__action {
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
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.workshop-app-frame__action--icon {
  width: 1.85rem;
  padding-right: 0;
  padding-left: 0;
}

.workshop-app-frame__icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
}

.workshop-app-frame__action:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-app-frame__action--busy,
.workshop-app-frame__action--busy:disabled {
  background: rgba(251, 191, 36, 0.16);
  border-color: rgba(251, 191, 36, 0.48);
  color: var(--color-warning, #fbbf24);
  opacity: 1;
}

.workshop-app-frame__action--active {
  background: rgba(59, 130, 246, 0.32);
  border-color: rgba(59, 130, 246, 0.68);
  color: #bfdbfe;
}

.workshop-app-frame__action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.workshop-app-frame__viewport {
  position: relative;
  flex: 1;
  min-height: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(0, 188, 212, 0.08), transparent 28rem),
    var(--color-dark-900, #0A151B);
}

.workshop-app-frame--maximized .workshop-app-frame__viewport {
  background: var(--color-dark-900, #0A151B);
}

.workshop-app-frame__iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  background: white;
}

.workshop-app-frame__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2, 0.5rem);
  padding: var(--spacing-6, 1.5rem);
  text-align: center;
  background: rgba(10, 21, 27, 0.88);
  backdrop-filter: blur(8px);
}

.workshop-app-frame__state {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: var(--font-weight-semibold, 600);
}

.workshop-app-frame__message {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1.6;
}

.workshop-app-frame__logs,
.workshop-app-frame__environment {
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

.workshop-app-frame__environment {
  max-height: min(28rem, 78%);
}

.workshop-app-frame__logs-header,
.workshop-app-frame__environment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  padding: 0.55rem 0.75rem;
  border-bottom: 1px solid rgba(71, 85, 105, 0.42);
  background: rgba(13, 26, 34, 0.96);
}

.workshop-app-frame__logs-title,
.workshop-app-frame__environment-title {
  margin: 0;
  color: var(--color-text, #e2e8f0);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-app-frame__logs-close,
.workshop-app-frame__environment-close {
  border: 0;
  background: transparent;
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  cursor: pointer;
}

.workshop-app-frame__logs-close:hover,
.workshop-app-frame__environment-close:hover {
  color: #bfdbfe;
}

.workshop-app-frame__logs-body {
  margin: 0;
  padding: 0.75rem;
  overflow: auto;
  color: #cbd5e1;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 0.72rem;
  line-height: 1.55;
  white-space: pre-wrap;
}

.workshop-app-frame__environment-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.75rem;
  overflow: auto;
}

.workshop-app-frame__environment-message {
  margin: 0;
  color: #bfdbfe;
  font-size: 0.78rem;
  line-height: 1.45;
}

.workshop-app-frame__environment-message--error {
  color: #fecaca;
}

.workshop-app-frame__environment-rows {
  display: grid;
  gap: 0.6rem;
}

.workshop-app-frame__environment-row {
  display: grid;
  grid-template-columns: minmax(9rem, 0.9fr) minmax(12rem, 1.1fr) auto;
  gap: 0.55rem;
  align-items: end;
}

.workshop-app-frame__environment-field {
  display: grid;
  gap: 0.25rem;
  min-width: 0;
  color: #94a3b8;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.workshop-app-frame__environment-field input {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  border: 1px solid rgba(71, 85, 105, 0.68);
  border-radius: 6px;
  background: rgba(10, 21, 27, 0.92);
  color: #e2e8f0;
  font: inherit;
  font-size: 0.82rem;
  letter-spacing: 0;
  outline: none;
  padding: 0.5rem 0.6rem;
}

.workshop-app-frame__environment-field input:focus {
  border-color: rgba(96, 165, 250, 0.9);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.22);
}

.workshop-app-frame__environment-field input::placeholder {
  color: rgba(148, 163, 184, 0.72);
}

.workshop-app-frame__environment-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
}

.workshop-app-frame__environment-primary,
.workshop-app-frame__environment-secondary,
.workshop-app-frame__environment-remove {
  min-height: 2.1rem;
  border: 1px solid rgba(71, 85, 105, 0.68);
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.workshop-app-frame__environment-primary {
  background: var(--color-accent, #22c55e);
  border-color: rgba(34, 197, 94, 0.7);
  color: #03120a;
  padding: 0 0.85rem;
}

.workshop-app-frame__environment-secondary,
.workshop-app-frame__environment-remove {
  background: rgba(15, 23, 42, 0.62);
  color: #cbd5e1;
  padding: 0 0.75rem;
}

.workshop-app-frame__environment-primary:hover,
.workshop-app-frame__environment-secondary:hover,
.workshop-app-frame__environment-remove:hover {
  filter: brightness(1.08);
}

.workshop-app-frame__environment-primary:disabled,
.workshop-app-frame__environment-secondary:disabled,
.workshop-app-frame__environment-remove:disabled {
  cursor: not-allowed;
  filter: none;
  opacity: 0.58;
}

@media (max-width: 720px) {
  .workshop-app-frame--maximized {
    inset: calc(72px + var(--spacing-2, 0.5rem)) var(--spacing-2, 0.5rem) var(--spacing-2, 0.5rem);
  }

  .workshop-app-frame__logs,
  .workshop-app-frame__environment {
    max-height: 55%;
  }

  .workshop-app-frame__environment-row {
    grid-template-columns: 1fr;
  }

  .workshop-app-frame__environment-footer {
    flex-direction: column;
  }
}
</style>
