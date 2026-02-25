<template>
  <div class="locks-home">
    <div class="hub-container">
      <!-- STAGE 1: See the Problem -->
      <div v-if="currentStage === 1" class="problem-stage">
        <div class="main-content">
          <div class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>The Problem: Race Conditions</h2>
          </div>

          <p class="intro">
            Before we fix anything, let's <strong>see the problem</strong> with our own eyes.
          </p>

          <div class="step-item">
            <h4>Experiment: Run 5 Concurrent Workers</h4>
            <p class="step-description">
              Imagine a scheduled job that should run <strong>exactly once</strong> per minute—like sending a daily report
              or processing a payment batch. What happens when 5 app instances all try to run it at the same time?
            </p>
          </div>

          <div class="demo-box">
            <div class="scenario-results">
              <div class="result-item">
                <span class="result-label">Workers Started</span>
                <span class="result-value">{{ jobResult ? jobResult.workers : 5 }}</span>
              </div>
              <div class="result-item" :class="{ 'result-bad': jobResult && jobResult.ran > 1 }">
                <span class="result-label">Actually Ran</span>
                <span class="result-value">{{ jobResult ? jobResult.ran : '-' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">Skipped</span>
                <span class="result-value">{{ jobResult ? jobResult.skipped : '-' }}</span>
              </div>
            </div>
            <button @click="runJobDemo" class="btn btn-primary btn-large" :disabled="runningJob">
              {{ runningJob ? 'Running...' : 'Run 5 Workers' }}
            </button>
          </div>

          <div class="alert alert-warning" v-if="jobResult && jobResult.ran > 1">
            <strong>Problem!</strong> All {{ jobResult.ran }} workers executed the job.
            In production, this means duplicate emails, double charges, or corrupted data.
          </div>

          <div class="alert alert-success" v-if="jobResult && jobResult.ran === 1">
            Locks are already working! Only 1 worker ran. (Click "Restart Workshop" to see the problem.)
          </div>

          <div class="step-item">
            <h4>Why does this happen?</h4>
            <p class="step-description">
              Each application instance has no idea what the others are doing. They all check "should I run?"
              at the same time, all see "yes", and all execute. <strong>There's no coordination.</strong>
            </p>
          </div>

          <div class="step-item">
            <h4>The Solution: Distributed Locks</h4>
            <p class="step-description">
              A <strong>distributed lock</strong> is a shared "flag" that all instances can see.
              The first one to grab it wins; the others wait or skip. Redis is perfect for this because:
            </p>
            <ul class="reason-list">
              <li><strong>Centralized:</strong> All app instances connect to the same Redis</li>
              <li><strong>Fast:</strong> Lock operations take microseconds</li>
              <li><strong>Safe:</strong> Redis operations are atomic</li>
              <li><strong>TTL:</strong> Locks auto-expire if a process crashes</li>
            </ul>
          </div>

          <button @click="nextStage" class="btn btn-primary">I understand. Show me the lock types →</button>
        </div>
      </div>

      <!-- STAGE 2: Lock Type Hub -->
      <div v-if="currentStage >= 2" class="hub-stage">
        <div class="hub-header">
          <div class="title-row">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="40" height="40" />
            <div>
              <h1>Distributed Locks Workshop</h1>
              <p class="subtitle">Learn different lock types with hands-on exercises</p>
            </div>
          </div>
          <div class="progress-summary">
            <span class="progress-count">{{ completedCount }}/{{ lockTypes.length }}</span>
            <span class="progress-label">Completed</span>
          </div>
        </div>

        <!-- Redisson intro (collapsible) -->
        <div class="redisson-intro" :class="{ collapsed: !showRedissonIntro }">
          <button class="intro-toggle" @click="showRedissonIntro = !showRedissonIntro">
            <span>What is Redisson?</span>
            <span class="toggle-icon">{{ showRedissonIntro ? '−' : '+' }}</span>
          </button>
          <div v-if="showRedissonIntro" class="intro-content">
            <p>
              <strong>Redisson</strong> is a Redis client for Java that provides high-level distributed objects.
              Instead of implementing locks yourself with SET/GET commands, Redisson gives you ready-to-use
              Java objects: RLock, RSemaphore, RReadWriteLock, and more.
            </p>
            <p style="margin-top: 0.5rem;">
              All the lock types below use Redisson's battle-tested implementations that follow best practices
              (like the Redlock algorithm for safety).
            </p>
          </div>
        </div>

        <!-- Lock Type Cards -->
        <h2 class="section-title">Choose a Lock Type</h2>
        <p class="section-subtitle">Start with Reentrant Lock if you're new to distributed locks</p>

        <div class="lock-types-grid">
          <div
            v-for="lockType in lockTypes"
            :key="lockType.id"
            class="lock-card"
            :class="{
              available: lockType.available,
              completed: getLockStatus(lockType.id).testsComplete,
              'in-progress': getLockStatus(lockType.id).started && !getLockStatus(lockType.id).testsComplete
            }"
          >
            <div class="lock-card-header">
              <h3>{{ lockType.name }}</h3>
              <span class="difficulty-badge" :class="lockType.difficulty.toLowerCase()">
                {{ lockType.difficulty }}
              </span>
            </div>
            <p class="lock-card-description">{{ lockType.description }}</p>
            <p class="lock-card-usecase"><strong>Use case:</strong> {{ lockType.useCase }}</p>

            <div class="lock-card-footer">
              <div class="lock-status">
                <span v-if="getLockStatus(lockType.id).testsComplete" class="status-complete">Completed</span>
                <span v-else-if="getLockStatus(lockType.id).started" class="status-progress">In Progress</span>
                <span v-else-if="!lockType.available" class="status-soon">Coming Soon</span>
              </div>
              <router-link
                v-if="lockType.available"
                :to="`/${lockType.id}`"
                class="btn btn-primary btn-sm"
              >
                {{ getLockStatus(lockType.id).started ? 'Continue' : 'Start' }} →
              </router-link>
              <button v-else class="btn btn-disabled btn-sm" disabled>Coming Soon</button>
            </div>
          </div>
        </div>

        <!-- Back button and restart -->
        <div class="hub-footer">
          <button @click="prevStage" class="btn btn-secondary">← See Problem Demo</button>
          <button @click="restartLab" class="btn btn-warning" :disabled="restartingLab">
            {{ restartingLab ? 'Restoring...' : 'Restart Workshop' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { LOCK_TYPES, loadProgress, saveProgress, clearProgress, getLockTypeProgress, getCompletedLockTypesCount } from '../utils/locksWorkshop';

export default {
  name: 'LocksHome',
  data() {
    return {
      lockTypes: LOCK_TYPES,
      currentStage: 1,
      restartingLab: false,
      runningJob: false,
      jobResult: null,
      showRedissonIntro: false,
      lockProgress: {}
    };
  },
  computed: {
    completedCount() {
      return getCompletedLockTypesCount();
    },
    workshopHubUrl() {
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      return `${protocol}//${hostname}:9000`;
    }
  },
  mounted() {
    this.loadState();
    this.refreshLockProgress();
  },
  methods: {
    loadState() {
      const progress = loadProgress();
      this.currentStage = progress.currentStage || 1;
    },
    saveState() {
      const progress = loadProgress();
      progress.currentStage = this.currentStage;
      saveProgress(progress);
    },
    nextStage() {
      if (this.currentStage < 2) {
        this.currentStage++;
        this.saveState();
      }
    },
    prevStage() {
      if (this.currentStage > 1) {
        this.currentStage--;
        this.saveState();
      }
    },
    getLockStatus(lockTypeId) {
      return getLockTypeProgress(lockTypeId);
    },
    refreshLockProgress() {
      // Refresh progress for all lock types
      this.lockProgress = {};
      this.lockTypes.forEach(lt => {
        this.lockProgress[lt.id] = getLockTypeProgress(lt.id);
      });
    },
    async runJobDemo() {
      this.runningJob = true;
      try {
        const response = await fetch('/api/scenarios/jobs/run?workers=5', { method: 'POST' });
        this.jobResult = await response.json();
      } catch (error) {
        console.error('Job demo failed:', error);
      } finally {
        this.runningJob = false;
      }
    },
    async restartLab() {
      this.restartingLab = true;
      try {
        const response = await fetch('/api/editor/restore', { method: 'POST' });
        const data = await response.json();
        if (data.success) {
          clearProgress();
          this.currentStage = 1;
          this.jobResult = null;
          this.refreshLockProgress();
        }
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

/* Problem Stage (Stage 1) */
.problem-stage .main-content {
  max-width: 800px;
  margin: 0 auto;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
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

/* Hub Stage (Stage 2) */
.hub-stage {
  padding-top: var(--spacing-4);
}

.hub-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-6);
  flex-wrap: wrap;
  gap: var(--spacing-4);
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.title-row h1 {
  margin: 0;
  font-size: 1.75rem;
  color: var(--color-text);
}

.title-row .subtitle {
  margin: 0.25rem 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.progress-summary {
  text-align: right;
}

.progress-count {
  display: block;
  font-size: 1.5rem;
  font-weight: var(--font-weight-bold);
  color: #10b981;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Redisson Intro */
.redisson-intro {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-6);
  overflow: hidden;
}

.intro-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-4);
  background: none;
  border: none;
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  text-align: left;
}

.intro-toggle:hover {
  background: var(--color-dark-800);
}

.toggle-icon {
  font-size: 1.25rem;
  color: var(--color-text-secondary);
}

.intro-content {
  padding: 0 var(--spacing-4) var(--spacing-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.intro-content p {
  margin: 0;
}

.section-title {
  margin: 0 0 var(--spacing-2);
  color: var(--color-text);
  font-size: var(--font-size-lg);
}

.section-subtitle {
  margin: 0 0 var(--spacing-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.intro {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-5);
  line-height: 1.6;
}

.step-item {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
  border-left: 3px solid #DC382C;
}

.step-item h4 {
  color: var(--color-text);
  font-size: var(--font-size-base);
  margin-bottom: var(--spacing-2);
  font-weight: var(--font-weight-semibold);
}

.step-description {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  margin: 0;
}

.step-list {
  margin: var(--spacing-2) 0 0 var(--spacing-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.8;
}

.step-list li { margin-bottom: var(--spacing-3); }

.step-list pre {
  background: var(--color-dark-900);
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-sm);
  margin: var(--spacing-2) 0;
  overflow-x: auto;
  font-size: var(--font-size-xs);
}

.button-group {
  display: flex;
  gap: var(--spacing-3);
  margin-top: var(--spacing-4);
  flex-wrap: wrap;
}

.btn {
  padding: var(--spacing-3) var(--spacing-5);
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
.btn-secondary { background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover:not(:disabled) { background: var(--color-border); }
.btn-outline { background: transparent; color: #DC382C; border: 1px solid #DC382C; }
.btn-outline:hover:not(:disabled) { background: rgba(220, 56, 44, 0.1); }
.btn-warning { background: #b45309; color: white; }
.btn-warning:hover:not(:disabled) { background: #92400e; }

.skip-link { text-align: right; margin-bottom: 0.5rem; }
.btn-link {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0;
  text-decoration: underline;
}
.btn-link:hover { color: var(--color-text); }

/* Lock Type Cards Grid */
.lock-types-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
}

.lock-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
}

.lock-card:not(.available) {
  opacity: 0.6;
}

.lock-card.available:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.lock-card.completed {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.lock-card.in-progress {
  border-color: #f59e0b;
}

.lock-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-2);
}

.lock-card-header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  color: var(--color-text);
}

.difficulty-badge {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
}

.difficulty-badge.beginner {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.difficulty-badge.intermediate {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.difficulty-badge.advanced {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.lock-card-description {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
  margin: 0 0 var(--spacing-2);
}

.lock-card-usecase {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  margin: 0 0 var(--spacing-3);
  flex: 1;
}

.lock-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-3);
  border-top: 1px solid var(--color-border);
}

.lock-status {
  font-size: var(--font-size-xs);
}

.status-complete {
  color: #10b981;
  font-weight: var(--font-weight-semibold);
}

.status-progress {
  color: #f59e0b;
}

.status-soon {
  color: var(--color-text-secondary);
}

.btn-sm {
  padding: var(--spacing-2) var(--spacing-3);
  font-size: var(--font-size-xs);
}

.btn-disabled {
  background: var(--color-dark-700);
  color: var(--color-text-secondary);
  cursor: not-allowed;
}

/* Hub Footer */
.hub-footer {
  display: flex;
  justify-content: space-between;
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-border);
}
/* Demo box for problem stage */
.demo-box {
  background: var(--color-dark-900);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin: var(--spacing-4) 0;
  text-align: center;
}

.scenario-results {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.result-item {
  background: var(--color-surface);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
}

.result-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.result-value {
  display: block;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
}

.result-item.result-bad .result-value {
  color: #ef4444;
}

.reason-list {
  margin: var(--spacing-2) 0 0 var(--spacing-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.8;
}

.reason-list li {
  margin-bottom: var(--spacing-2);
}

.btn-large {
  padding: var(--spacing-3) var(--spacing-6);
  font-size: var(--font-size-base);
}

.alert {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-sm);
}

.alert-info { background: #094771; }
.alert-success { background: #065f46; }
.alert-warning { background: #92400e; border-left: 3px solid #f59e0b; }

.editor-link {
  color: #60a5fa;
  font-weight: 500;
  text-decoration: none;
}
.editor-link:hover { text-decoration: underline; }

.intro { margin-bottom: 0.75rem; }

.impl-step {
  background: var(--color-dark-800);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 1rem;
  margin-bottom: 0.75rem;
}
.impl-step h5 {
  margin: 0 0 0.5rem 0;
  color: #DC382C;
  font-size: 0.95rem;
}
.impl-step p {
  margin: 0.5rem 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}
.impl-step pre {
  background: var(--color-dark-900);
  padding: 0.75rem;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0.5rem 0;
}
.impl-step code {
  font-size: 0.85rem;
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

.reentrant-note {
  background: rgba(86, 156, 214, 0.1);
  border: 1px solid rgba(86, 156, 214, 0.3);
  border-radius: var(--radius-sm);
  padding: var(--spacing-3);
  margin-top: var(--spacing-3);
}

.reentrant-note p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.reentrant-note code {
  color: #ce9178;
}

.btn-disabled {
  background: var(--color-dark-800);
  color: var(--color-text-secondary);
  cursor: not-allowed;
  opacity: 0.5;
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

code {
  background: var(--color-dark-900);
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  color: #DC382C;
  font-family: 'Courier New', monospace;
  font-size: 0.85em;
}

.status-line { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.status-ok { color: #10b981; }
.status-missing { color: #f59e0b; }

/* Right column */
.details-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.header h2 { margin: 0; font-size: var(--font-size-lg); color: var(--color-text); }

.demo-section h4 {
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-base);
}

.scenario-results {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-3);
  margin: var(--spacing-4) 0;
}

.result-item {
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  text-align: center;
}

.result-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-1);
}

.result-value {
  display: block;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
}

.result-hint {
  margin-top: var(--spacing-3);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.file-card {
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-3);
}

.file-card strong { color: var(--color-text); font-size: var(--font-size-sm); }
.file-card p { margin: var(--spacing-1) 0 0; font-size: var(--font-size-xs); color: var(--color-text-secondary); }

.recap-item {
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-3);
}

.recap-item strong { color: var(--color-text); }
.recap-item p { margin: var(--spacing-1) 0 0; font-size: var(--font-size-sm); color: var(--color-text-secondary); }

.redis-insight-box {
  background: var(--color-dark-800);
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  margin-top: var(--spacing-4);
}

.redis-insight-box h4 { margin-bottom: var(--spacing-2); color: var(--color-text); }
.redis-insight-box code { display: block; margin: var(--spacing-1) 0; font-size: var(--font-size-xs); }

/* Stage 1 result styling */
.result-bad { background: #450a0a; }
.result-explanation { margin-top: var(--spacing-4); padding: var(--spacing-3); border-radius: var(--radius-md); }
.result-bad-text { color: #fca5a5; }
.result-good-text { color: #86efac; }
.btn-large { font-size: var(--font-size-base); padding: var(--spacing-3) var(--spacing-6); }

/* Stage 2 checklist */
.checklist-item {
  display: flex;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-2);
  border-left: 3px solid var(--color-border);
}

.checklist-item.done {
  border-left-color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.checklist-item .check {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
}

.checklist-item.done .check {
  color: #10b981;
}

.checklist-item strong {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.checklist-item p {
  margin: var(--spacing-1) 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

@media (max-width: 900px) {
  .welcome-container { grid-template-columns: 1fr; }
  .details-column { position: static; }
}
</style>
