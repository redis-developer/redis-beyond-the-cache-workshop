<template>
  <div class="locks-demo">
    <div class="main-container">
      <!-- Left Column: Tests -->
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2><img src="@/assets/logo/small.png" alt="Redis Logo" width="28" height="28" class="logo-small" /> Test Your Implementation</h2>
        </div>
        <div class="workshop-content">
          <div class="lock-status">
            <span class="status-label">Lock Status:</span>
            <span :class="['status-value', lockEnabled ? 'enabled' : 'disabled']">
              {{ lockEnabled ? 'Redisson Enabled ✓' : 'Not Enabled' }}
            </span>
            <button @click="checkLockStatus" class="btn btn-sm btn-secondary" :disabled="checkingLock">
              {{ checkingLock ? '...' : 'Refresh' }}
            </button>
          </div>

          <!-- Test 1: Jobs Scenario -->
          <div class="test-item" :class="{ completed: testsPassed.jobs }">
            <div class="test-header">
              <h4>Test 1: Single-Runner Job</h4>
              <span v-if="testsPassed.jobs" class="test-check">✓ Passed</span>
            </div>
            <p class="test-description">
              Run 5 concurrent workers trying to execute the same job.
              <strong v-if="!lockEnabled">Without locks, all workers will run.</strong>
              <strong v-else>With locks, only 1 should run.</strong>
            </p>
            <div class="scenario-results">
              <div class="result-item">
                <span class="result-label">Workers</span>
                <span class="result-value">{{ jobResult ? jobResult.workers : 5 }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Ran</span>
                <span class="result-value">{{ jobResult ? jobResult.ran : '-' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Skipped</span>
                <span class="result-value">{{ jobResult ? jobResult.skipped : '-' }}</span>
              </div>
            </div>
            <button @click="runJobTest" class="btn btn-primary" :disabled="runningJob">
              {{ runningJob ? 'Running...' : 'Run 5 Workers' }}
            </button>
            <p v-if="jobResult" class="result-hint">
              {{ jobResult.ran === 1 ? '✓ Only 1 worker ran - lock working!' : '⚠️ Multiple workers ran - lock not working.' }}
            </p>
          </div>

          <!-- Test 2: Inventory Scenario -->
          <div class="test-item" :class="{ completed: testsPassed.inventory }">
            <div class="test-header">
              <h4>Test 2: Inventory Oversell</h4>
              <span v-if="testsPassed.inventory" class="test-check">✓ Passed</span>
            </div>
            <p class="test-description">
              Reset inventory to 5, then run 8 concurrent purchases.
              <strong v-if="!lockEnabled">Without locks, more than the starting stock may succeed (overselling).</strong>
              <strong v-else>With locks, inventory should never go below zero. Some attempts may be rejected if the lock is busy.</strong>
            </p>
            <div class="scenario-results">
              <div class="result-item">
                <span class="result-label">Starting Stock</span>
                <span class="result-value">{{ inventoryResult ? inventoryResult.baseline : (inventory ? inventory.quantity : '-') }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Accepted</span>
                <span class="result-value">{{ inventoryResult ? inventoryResult.successes : '-' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Rejected</span>
                <span class="result-value">{{ inventoryResult ? inventoryResult.failures : '-' }}</span>
              </div>
            </div>
            <div class="button-row">
              <button @click="resetInventory" class="btn btn-secondary" :disabled="loading">Reset to 5</button>
              <button @click="runInventoryTest" class="btn btn-primary" :disabled="runningBatch || loading">
                {{ runningBatch ? 'Running...' : 'Run 8 Purchases' }}
              </button>
            </div>
            <p v-if="inventoryResult" class="result-hint">
              {{
                inventoryResult.successes <= inventoryResult.baseline
                  ? `✓ No overselling - accepted ${inventoryResult.successes} of ${inventoryResult.baseline}.`
                  : `⚠️ Oversold! Accepted ${inventoryResult.successes} of ${inventoryResult.baseline}.`
              }}
            </p>
          </div>

          <!-- Test 3: Reentrant Lock (Checkout) -->
          <div class="test-item" :class="{ completed: testsPassed.checkout }">
            <div class="test-header">
              <h4>Test 3: Reentrant Lock</h4>
              <span v-if="testsPassed.checkout" class="test-check">Passed</span>
            </div>
            <p class="test-description">
              Demonstrates why <strong>reentrancy</strong> matters. The checkout method acquires a lock, then calls
              validateInventory() and reserveItem() which <em>also</em> try to acquire the same lock.
            </p>
            <div class="reentrant-explainer">
              <div class="code-flow">
                <code>checkout()</code> <span class="arrow">→</span> <code>validateInventory()</code> <span class="arrow">→</span> <code>reserveItem()</code>
              </div>
              <p class="flow-note">All three methods need the same lock. Without reentrancy, this would deadlock.</p>
            </div>
            <div class="scenario-results">
              <div class="result-item">
                <span class="result-label">Validations</span>
                <span class="result-value">{{ checkoutResult ? checkoutResult.validations : '-' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Reservations</span>
                <span class="result-value">{{ checkoutResult ? checkoutResult.reservations : '-' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Status</span>
                <span class="result-value">{{ checkoutResult ? (checkoutResult.success ? 'Success' : 'Failed') : '-' }}</span>
              </div>
            </div>
            <div class="button-row">
              <button @click="resetInventory" class="btn btn-secondary" :disabled="loading">Reset Inventory</button>
              <button @click="runCheckoutTest" class="btn btn-primary" :disabled="runningCheckout || loading">
                {{ runningCheckout ? 'Running...' : 'Run Checkout' }}
              </button>
            </div>
            <p v-if="checkoutResult" class="result-hint">
              {{ checkoutResult.success
                ? `Nested locks worked. Validations: ${checkoutResult.validations}, Reservations: ${checkoutResult.reservations}`
                : checkoutResult.wouldDeadlock
                  ? 'Deadlock! Non-reentrant lock cannot be acquired twice by same thread.'
                  : checkoutResult.message
              }}
            </p>
          </div>

          <!-- Completion -->
          <div v-if="allTestsPassed" class="completion-box">
            <h4>All Tests Passed!</h4>
            <p>Your Reentrant Lock implementation is working correctly.</p>
            <div class="button-row">
              <router-link to="/" class="btn btn-primary">Back to Lock Types →</router-link>
            </div>
          </div>

          <div class="nav-links">
            <router-link to="/reentrant" class="nav-link">← Back to Reentrant Lock</router-link>
            <router-link to="/reentrant/editor" class="nav-link">Open Code Editor →</router-link>
          </div>
        </div>
      </div>

      <!-- Right Column: Live Log -->
      <div class="demo-panel">
        <div class="info-card">
          <h3>Lock Mode</h3>
          <p :class="['lock-mode', lockEnabled ? 'enabled' : 'disabled']">
            {{ lockEnabled ? 'redisson' : 'none' }}
          </p>
        </div>

        <div class="log-card">
          <h3>Activity Log</h3>
          <pre>{{ logText }}</pre>
        </div>

        <div class="snippet-card">
          <h3>Redisson Snippet</h3>
          <code>RLock lock = redissonClient.getLock(key);</code>
          <code>lock.tryLock(waitTime, leaseTime, TimeUnit.MS);</code>
          <code>try { ... } finally { lock.unlock(); }</code>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { fetchLockStatus, updateLockTypeProgress } from '../utils/locksWorkshop';
import { getApiUrl } from '../utils/basePath';

export default {
  name: 'LocksDemo',
  data() {
    return {
      inventory: null,
      logLines: [],
      loading: false,
      runningBatch: false,
      runningJob: false,
      runningCheckout: false,
      checkingLock: false,
      lockEnabled: false,
      jobResult: null,
      inventoryResult: null,
      checkoutResult: null,
      testsPassed: { jobs: false, inventory: false, checkout: false }
    };
  },
  computed: {
    logText() {
      return this.logLines.join('\n') || 'No activity yet...';
    },
    allTestsPassed() {
      return this.testsPassed.jobs && this.testsPassed.inventory && this.testsPassed.checkout;
    }
  },
  async mounted() {
    await this.checkLockStatus();
    await this.fetchInventory();
  },
  methods: {
    async checkLockStatus() {
      this.checkingLock = true;
      try {
        const status = await fetchLockStatus();
        this.lockEnabled = status.enabled;
      } finally {
        this.checkingLock = false;
      }
    },
    async fetchInventory() {
      try {
        const response = await fetch(getApiUrl('/api/inventory'));
        this.inventory = await response.json();
      } catch (error) {
        this.appendLog('Failed to fetch inventory');
      }
    },
    async resetInventory() {
      this.loading = true;
      try {
        await fetch(getApiUrl('/api/reset'), { method: 'POST' });
        this.appendLog('Reset inventory to 5');
        this.inventoryResult = null;
        await this.fetchInventory();
      } finally {
        this.loading = false;
      }
    },
    async runJobTest() {
      this.runningJob = true;
      this.jobResult = null;
      try {
        const response = await fetch(getApiUrl('/api/scenarios/jobs/run?workers=5'), { method: 'POST' });
        this.jobResult = await response.json();
        this.appendLog(`Jobs: ran=${this.jobResult.ran}, skipped=${this.jobResult.skipped}, lock=${this.jobResult.lockMode}`);
        if (this.jobResult.ran === 1) {
          this.testsPassed.jobs = true;
          this.updateProgress();
        }
      } catch (error) {
        this.appendLog('Job test failed');
      } finally {
        this.runningJob = false;
      }
    },
    async runInventoryTest() {
      this.runningBatch = true;
      this.inventoryResult = null;
      await this.fetchInventory();
      const baseline = this.inventory ? this.inventory.quantity : 5;
      try {
        const results = await Promise.all(
          Array.from({ length: 8 }, (_, i) => this.purchase(`buy-${i + 1}`))
        );
        const successes = results.filter(r => r && r.success).length;
        const failures = results.filter(r => r && !r.success).length;
        this.inventoryResult = { baseline, successes, failures };
        await this.fetchInventory();
        if (successes <= baseline) {
          this.testsPassed.inventory = true;
          this.updateProgress();
        }
      } finally {
        this.runningBatch = false;
      }
    },
    async purchase(label) {
      try {
        const response = await fetch(getApiUrl('/api/purchase'), { method: 'POST' });
        const data = await response.json();
        this.appendLog(`${label}: ${data.message} (remaining=${data.remaining})`);
        return data;
      } catch (error) {
        this.appendLog(`${label}: failed`);
        return null;
      }
    },
    async runCheckoutTest() {
      this.runningCheckout = true;
      this.checkoutResult = null;
      await this.fetchInventory();
      try {
        const response = await fetch(getApiUrl('/api/checkout'), { method: 'POST' });
        const data = await response.json();
        this.checkoutResult = data;
        this.appendLog(`Checkout: ${data.success ? 'SUCCESS' : 'FAILED'} - validations=${data.validations}, reservations=${data.reservations}, lock=${data.lockMode}`);
        if (data.success && data.validations > 0 && data.reservations > 0) {
          this.testsPassed.checkout = true;
          this.updateProgress();
        }
        await this.fetchInventory();
      } catch (error) {
        this.appendLog('Checkout test failed');
      } finally {
        this.runningCheckout = false;
      }
    },
    appendLog(message) {
      this.logLines = [message, ...this.logLines].slice(0, 15);
    },
    updateProgress() {
      if (this.allTestsPassed) {
        // Update reentrant lock progress to mark tests as complete
        updateLockTypeProgress('reentrant', { testsComplete: true });
      }
    }
  }
};
</script>

<style scoped>
.locks-demo {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: var(--spacing-6);
}

.main-container {
  width: 100%;
  max-width: 1200px;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--spacing-6);
  align-items: start;
  padding-top: var(--spacing-6);
}

.workshop-panel {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.workshop-header h2 {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin: 0 0 var(--spacing-4);
  color: var(--color-text);
  font-size: var(--font-size-lg);
}

.workshop-content { color: var(--color-text-secondary); font-size: var(--font-size-sm); line-height: 1.6; }

.lock-status {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-4);
}

.status-label { color: var(--color-text-secondary); }
.status-value { font-weight: var(--font-weight-semibold); }
.status-value.enabled { color: #10b981; }
.status-value.disabled { color: #f59e0b; }

.test-item {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
  border-left: 3px solid var(--color-border);
}

.test-item.completed { border-left-color: #10b981; }

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2);
}

.test-header h4 { margin: 0; color: var(--color-text); font-size: var(--font-size-base); }
.test-check { color: #10b981; font-weight: var(--font-weight-semibold); font-size: var(--font-size-sm); }

.test-description { margin: 0 0 var(--spacing-3); }
.test-description strong { color: var(--color-text); }

.scenario-results {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-2);
  margin: var(--spacing-3) 0;
}

.result-item {
  background: var(--color-surface);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  text-align: center;
}

.result-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.result-value { display: block; font-size: var(--font-size-lg); font-weight: var(--font-weight-bold); color: var(--color-text); }

.result-hint {
  margin: var(--spacing-3) 0 0;
  font-size: var(--font-size-sm);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  background: rgba(16, 185, 129, 0.1);
}

.reentrant-explainer {
  background: rgba(86, 156, 214, 0.1);
  border: 1px solid rgba(86, 156, 214, 0.3);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  margin: var(--spacing-3) 0;
}

.code-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  flex-wrap: wrap;
  margin-bottom: var(--spacing-2);
}

.code-flow code {
  background: var(--color-dark-800);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: #ce9178;
}

.code-flow .arrow {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.flow-note {
  margin: 0;
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.completion-box {
  background: #065f46;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

.completion-box h4 { margin: 0 0 var(--spacing-2); color: white; }
.completion-box p { margin: 0; color: rgba(255, 255, 255, 0.8); }

.button-row { display: flex; gap: var(--spacing-3); margin-top: var(--spacing-3); flex-wrap: wrap; }

.nav-links {
  margin-top: var(--spacing-4);
  padding-top: var(--spacing-3);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
}

.nav-link { color: var(--color-text-secondary); text-decoration: none; font-size: var(--font-size-sm); }
.nav-link:hover { color: #DC382C; }

/* Right Panel */
.demo-panel {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-5);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
  display: grid;
  gap: var(--spacing-4);
  position: sticky;
  top: var(--spacing-6);
}

.info-card, .log-card, .snippet-card {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
}

.info-card h3, .log-card h3, .snippet-card h3 {
  margin: 0 0 var(--spacing-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.lock-mode { margin: 0; font-size: var(--font-size-lg); font-weight: var(--font-weight-bold); }
.lock-mode.enabled { color: #10b981; }
.lock-mode.disabled { color: #f59e0b; }

pre {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--spacing-3);
  color: var(--color-text);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: var(--font-size-xs);
  min-height: 120px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  margin: 0;
}

.snippet-card code {
  display: block;
  background: var(--color-surface);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  color: #DC382C;
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-xs);
  margin-bottom: var(--spacing-2);
}

.btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
  text-align: center;
  text-decoration: none;
  display: inline-block;
}

.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover:not(:disabled) { background: #c42f24; }
.btn-secondary { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover:not(:disabled) { background: var(--color-border); }
.btn-sm { padding: var(--spacing-1) var(--spacing-2); font-size: var(--font-size-xs); }

@media (max-width: 900px) {
  .main-container { grid-template-columns: 1fr; }
  .demo-panel { position: static; }
}
</style>
