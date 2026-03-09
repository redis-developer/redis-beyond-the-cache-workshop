<template>
  <div class="reentrant-learn">
    <div class="main-container">
      <!-- Left Column: Learning Content -->
      <div class="content-column">
        <div class="section-header">
          <router-link to="/" class="back-link">← All Lock Types</router-link>
          <div class="title-row">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Reentrant Lock (RLock)</h2>
          </div>
          <span class="difficulty-badge beginner">Beginner</span>
        </div>

        <div v-if="visibleSection < 4" class="skip-link">
          <button @click="showAllSections" class="btn-link">Show all sections</button>
        </div>

        <!-- Section 1: What is a Reentrant Lock? -->
        <div class="step-item">
          <h4>1. What is a Reentrant Lock?</h4>
          <p class="step-description">
            A <strong>reentrant lock</strong> allows the same thread to acquire the lock multiple times without deadlocking itself.
          </p>
          <div class="concept-highlight">
            <p><strong>The key insight:</strong> If Thread A holds the lock and calls a method that also needs the lock, 
            a reentrant lock recognizes "oh, it's still Thread A" and lets it through. A non-reentrant lock would deadlock.</p>
          </div>
          <p class="step-description" style="margin-top: 0.75rem;">
            Redisson's <code>RLock</code> is the distributed version of Java's <code>ReentrantLock</code>—it works across 
            multiple JVMs, containers, or servers using Redis as the coordination layer.
          </p>
          <button v-if="visibleSection === 1" @click="revealSection(2)" class="btn btn-outline" style="margin-top: 1rem;">
            Next: When do I need this? →
          </button>
        </div>

        <!-- Section 2: When Do I Need This? -->
        <div v-if="visibleSection >= 2" class="step-item">
          <h4>2. When Do I Need a Reentrant Lock?</h4>
          <p class="step-description">
            You need reentrancy when your locked code calls other methods that also need the same lock:
          </p>
          <div class="code-flow-diagram">
            <div class="flow-box">checkout()</div>
            <div class="flow-arrow">→ acquires lock →</div>
            <div class="flow-box">validateInventory()</div>
            <div class="flow-arrow">→ needs same lock →</div>
            <div class="flow-box">reserveItem()</div>
          </div>
          <p class="step-description" style="margin-top: 0.75rem;">
            <strong>Real examples:</strong>
          </p>
          <ul class="example-list">
            <li><strong>Nested service calls:</strong> OrderService.process() → InventoryService.validate() → InventoryService.reserve()</li>
            <li><strong>Recursive operations:</strong> Tree traversal where each node needs exclusive access</li>
            <li><strong>Reusable utilities:</strong> A validation method called both standalone and from within transactions</li>
          </ul>
          <button v-if="visibleSection === 2" @click="revealSection(3)" class="btn btn-outline" style="margin-top: 1rem;">
            Next: How to implement it →
          </button>
        </div>

        <!-- Section 3: Implementation -->
        <div v-if="visibleSection >= 3" class="step-item">
          <h4>3. How to Implement RLock</h4>
          <div class="code-explanation">
            <pre><code>RLock lock = redissonClient.getLock("lock:product:123");</code></pre>
            <p>Get a lock reference. The <strong>key</strong> determines what you're locking—use specific keys like <code>lock:product:123</code>, not generic ones.</p>
          </div>
          <div class="code-explanation">
            <pre><code>boolean acquired = lock.tryLock(200, 5000, TimeUnit.MILLISECONDS);</code></pre>
            <p><strong>waitTime (200ms):</strong> How long to wait if someone else holds it.<br/>
            <strong>leaseTime (5000ms):</strong> Auto-release after this time (safety net if you crash).</p>
          </div>
          <div class="code-explanation">
            <pre><code>try {
    // critical section
} finally {
    lock.unlock();
}</code></pre>
            <p><strong>Always unlock in finally!</strong> Otherwise the lock stays held until lease expires.</p>
          </div>
          <button v-if="visibleSection === 3" @click="revealSection(4)" class="btn btn-outline" style="margin-top: 1rem;">
            Next: Implement it yourself →
          </button>
        </div>

        <!-- Section 4: Implement It -->
        <div v-if="visibleSection >= 4" class="step-item highlight-section">
          <h4>4. Implement It Yourself</h4>
          <p class="step-description">
            Now it's your turn! Implement the reentrant lock in the workshop code.
          </p>
          <router-link to="/reentrant/implement" class="btn btn-primary">Go to Implementation →</router-link>
        </div>

        <div class="nav-links">
          <router-link to="/" class="nav-link">← Back to Lock Types</router-link>
        </div>
      </div>

      <!-- Right Column: Quick Reference -->
      <div class="reference-column">
        <div class="reference-card">
          <h3>Quick Reference</h3>
          <div class="reference-item">
            <strong>Lock Type:</strong> Reentrant (RLock)
          </div>
          <div class="reference-item">
            <strong>Redisson Class:</strong> <code>RLock</code>
          </div>
          <div class="reference-item">
            <strong>Key Feature:</strong> Same thread can acquire multiple times
          </div>
          <div class="reference-item">
            <strong>Best For:</strong> Nested method calls needing same lock
          </div>
        </div>

        <div class="progress-card">
          <h3>Your Progress</h3>
          <div class="progress-item" :class="{ done: true }">
            <span class="check">✓</span> Learning concepts
          </div>
          <div class="progress-item" :class="{ done: implemented }">
            <span class="check">{{ implemented ? '✓' : '○' }}</span> Implementation
          </div>
          <div class="progress-item" :class="{ done: testsComplete }">
            <span class="check">{{ testsComplete ? '✓' : '○' }}</span> Tests passed
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { loadProgress, saveProgress, updateLockTypeProgress, getLockTypeProgress, fetchLockStatus } from '../utils/locksWorkshop';

export default {
  name: 'ReentrantLearn',
  data() {
    return {
      visibleSection: 1,
      implemented: false,
      testsComplete: false
    };
  },
  mounted() {
    this.loadState();
    this.checkStatus();
  },
  methods: {
    loadState() {
      const progress = loadProgress();
      this.visibleSection = progress.reentrantVisibleSection || 1;
      const lockProgress = getLockTypeProgress('reentrant');
      this.implemented = lockProgress.implemented;
      this.testsComplete = lockProgress.testsComplete;
      // Mark as started
      updateLockTypeProgress('reentrant', { started: true });
    },
    saveState() {
      const progress = loadProgress();
      progress.reentrantVisibleSection = this.visibleSection;
      saveProgress(progress);
    },
    revealSection(n) {
      this.visibleSection = n;
      this.saveState();
    },
    showAllSections() {
      this.visibleSection = 4;
      this.saveState();
    },
    async checkStatus() {
      try {
        const status = await fetchLockStatus();
        this.implemented = status.implemented;
        if (status.implemented) {
          updateLockTypeProgress('reentrant', { implemented: true });
        }
      } catch (e) {
        // ignore
      }
    }
  }
};
</script>

<style scoped>
.reentrant-learn {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--spacing-6);
  align-items: start;
}

.content-column {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
}

.section-header {
  margin-bottom: var(--spacing-4);
}

.back-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  display: inline-block;
  margin-bottom: var(--spacing-2);
}

.back-link:hover {
  color: var(--color-primary);
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.title-row h2 {
  margin: 0;
  color: var(--color-text);
}

.difficulty-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  margin-top: var(--spacing-2);
}

.difficulty-badge.beginner {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.skip-link {
  margin-bottom: var(--spacing-4);
}

.btn-link {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  padding: 0;
}

.btn-link:hover {
  text-decoration: underline;
}

.step-item {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

.step-item h4 {
  margin: 0 0 var(--spacing-3);
  color: var(--color-text);
}

.step-description {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}

.concept-highlight {
  background: rgba(86, 156, 214, 0.1);
  border-left: 3px solid #569cd6;
  padding: var(--spacing-3);
  margin: var(--spacing-3) 0;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.concept-highlight p {
  margin: 0;
  color: var(--color-text-secondary);
}

.code-flow-diagram {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  flex-wrap: wrap;
  background: var(--color-dark-900);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  margin: var(--spacing-3) 0;
}

.flow-box {
  background: var(--color-surface);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-family: monospace;
  color: #ce9178;
  font-size: var(--font-size-sm);
}

.flow-arrow {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.example-list {
  margin: 0;
  padding-left: var(--spacing-4);
  color: var(--color-text-secondary);
}

.example-list li {
  margin-bottom: var(--spacing-2);
}

.code-explanation {
  background: var(--color-dark-900);
  border-radius: var(--radius-sm);
  padding: var(--spacing-3);
  margin: var(--spacing-2) 0;
}

.code-explanation pre {
  margin: 0 0 var(--spacing-2);
  padding: var(--spacing-2);
  background: #1a1a1a;
  border-radius: var(--radius-sm);
  overflow-x: auto;
}

.code-explanation p {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.highlight-section {
  border: 2px solid var(--color-primary);
}

.nav-links {
  margin-top: var(--spacing-4);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-dark-700);
}

.nav-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
}

.nav-link:hover {
  color: var(--color-primary);
}

/* Reference Column */
.reference-column {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.reference-card, .progress-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
}

.reference-card h3, .progress-card h3 {
  margin: 0 0 var(--spacing-3);
  font-size: var(--font-size-base);
  color: var(--color-text);
}

.reference-item {
  padding: var(--spacing-2) 0;
  border-bottom: 1px solid var(--color-dark-700);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.reference-item:last-child {
  border-bottom: none;
}

.reference-item code {
  color: #ce9178;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.progress-item.done {
  color: #10b981;
}

.progress-item .check {
  width: 20px;
  text-align: center;
}

.btn {
  display: inline-block;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover {
  background: var(--color-primary-hover);
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
}

.btn-outline:hover {
  background: rgba(220, 38, 38, 0.1);
}

@media (max-width: 900px) {
  .main-container {
    grid-template-columns: 1fr;
  }
}
</style>

