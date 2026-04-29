<template>
  <div v-if="sessionContext" class="session-restart-controls">
    <button
      type="button"
      class="session-restart-button"
      :disabled="isBusy"
      @click="restartSession(false)"
    >
      Restart runtime
    </button>
    <button
      type="button"
      class="session-restart-button session-restart-button--accent"
      :disabled="isBusy"
      @click="restartSession(true)"
    >
      Restart with rebuild
    </button>
    <span v-if="statusMessage" class="session-restart-status" aria-live="polite">
      {{ statusMessage }}
    </span>
  </div>
</template>

<script>
import { getSessionRouteContext } from '../utils/sessionContext.js';

const READY_STATES = new Set(['CHILD_READY']);
const FAILED_STATES = new Set(['CHILD_FAILED', 'DISABLED']);
const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 480000;

export default {
  name: 'WorkshopSessionRestartControls',
  data() {
    return {
      isBusy: false,
      statusMessage: ''
    };
  },
  computed: {
    sessionContext() {
      return getSessionRouteContext();
    }
  },
  methods: {
    async restartSession(rebuild) {
      if (!this.sessionContext || this.isBusy) {
        return;
      }

      this.isBusy = true;
      this.statusMessage = rebuild ? 'Restarting with rebuild...' : 'Restarting runtime...';

      try {
        await this.sendRestartRequest(rebuild);
        await this.waitForReadyState();
        window.location.reload();
      } catch (error) {
        this.statusMessage = error?.message || 'Restart failed';
      } finally {
        this.isBusy = false;
      }
    },
    async sendRestartRequest(rebuild) {
      const restartUrl = this.buildRunnerUrl('/internal/session-runner/restart');
      const response = await fetch(
        restartUrl,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({ rebuild, async: true })
        }
      );

      if (!response.ok) {
        throw new Error(await this.readErrorMessage(response, 'Failed to restart session'));
      }
    },
    async waitForReadyState() {
      const startedAt = Date.now();

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const status = await this.fetchSessionStatus();
        const state = status?.state;

        if (READY_STATES.has(state)) {
          return;
        }
        if (FAILED_STATES.has(state)) {
          throw new Error(status?.lastError || 'Session restart failed');
        }

        await this.sleep(POLL_INTERVAL_MS);
      }

      throw new Error('Timed out waiting for the session to restart');
    },
    async fetchSessionStatus() {
      try {
        const stateUrl = this.buildRunnerUrl('/internal/session-runner/status');
        const response = await fetch(stateUrl, { credentials: 'include' });

        if (!response.ok) {
          return null;
        }

        return await response.json();
      } catch {
        return null;
      }
    },
    buildRunnerUrl(endpointPath) {
      const normalizedEndpointPath = endpointPath.startsWith('/')
        ? endpointPath
        : `/${endpointPath}`;

      return `${this.sessionContext.basePath}${normalizedEndpointPath}`;
    },
    async readErrorMessage(response, fallbackMessage) {
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
        // Fall through to returning the raw text below.
      }

      return `${fallbackMessage}: ${text}`;
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }
  }
};
</script>

<style scoped>
.session-restart-controls {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 0.5rem);
  flex-wrap: wrap;
}

.session-restart-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-full, 9999px);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

.session-restart-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.session-restart-button--accent {
  border-color: rgba(245, 158, 11, 0.45);
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
}

.session-restart-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.session-restart-status {
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  white-space: nowrap;
}
</style>
