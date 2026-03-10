<template>
  <div class="locks-implement">
    <div class="implement-container">
      <div class="instructions-column">
        <div class="section-header">
          <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
          <h2>Implement the Distributed Lock</h2>
        </div>

        <div class="alert alert-info">
          <strong>Option 1: Use the In-Browser Code Editor</strong>
          <p style="margin: 0.5rem 0 0 0;">
            <router-link to="/reentrant/editor" class="editor-link">Open Code Editor →</router-link>
          </p>
        </div>

        <p class="intro"><strong>Option 2: Edit files in your IDE</strong></p>

        <p class="intro">
          Follow the same path shown in the workshop UI: update <code>build.gradle.kts</code>, then
          <code>application.properties</code>, then <code>LockManager.java</code>, review
          <code>PurchaseService.java</code>, rebuild from the Workshop Hub, and validate in the demo.
        </p>

        <div class="impl-step">
          <h4>Step 1: Add Dependencies</h4>
          <p>Open <code>build.gradle.kts</code> and uncomment the Redis and Redisson dependencies:</p>
          <pre><code>// TODO: Uncomment the lines below to enable Redisson distributed locks
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.redisson:redisson-spring-boot-starter:3.27.2")</code></pre>
          <p><strong>Why these dependencies?</strong></p>
          <ul class="dep-list">
            <li><code>spring-boot-starter-data-redis</code> — Connects Spring Boot to Redis</li>
            <li><code>redisson-spring-boot-starter</code> — Provides RLock and other distributed objects</li>
          </ul>
        </div>

        <div class="impl-step">
          <h4>Step 2: Enable Redisson in Configuration</h4>
          <p>Open <code>application.properties</code> and change:</p>
          <pre><code>workshop.lock.mode=none</code></pre>
          <p>to:</p>
          <pre><code>workshop.lock.mode=redisson</code></pre>
        </div>

        <div class="impl-step">
          <h4>Step 3: Implement the Lock in LockManager.java</h4>
          <p>Open <code>LockManager.java</code> and make the following changes:</p>

          <p><strong>3a. Uncomment the import:</strong></p>
          <pre><code>import org.redisson.api.RedissonClient;</code></pre>

          <p><strong>3b. Uncomment the field:</strong></p>
          <pre><code>private final RedissonClient redissonClient;</code></pre>

          <p><strong>3c. Uncomment the constructor parameter and assignment:</strong></p>
          <pre><code>@Autowired(required = false) RedissonClient redissonClient,
...
this.redissonClient = redissonClient;</code></pre>

          <p><strong>3d. Update <code>isEnabled()</code>:</strong></p>
          <pre><code>public boolean isEnabled() {
    return "redisson".equalsIgnoreCase(lockMode) && redissonClient != null;
}</code></pre>

          <p><strong>3e. Add imports and implement <code>withLock()</code>:</strong></p>
          <pre><code>import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
...
RLock lock = redissonClient.getLock(lockKey);
boolean acquired = false;
try {
    acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
    if (!acquired) {
        return onBusy.get();
    }
    return onAcquired.get();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return onBusy.get();
} finally {
    if (acquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}</code></pre>
        </div>

        <div class="impl-step review-step">
          <h4>Step 4: See How Your Lock Gets Used</h4>
          <p>Open <code>PurchaseService.java</code> and find the <code>purchase()</code> method. Notice:</p>
          <pre><code>lockManager.withLock(
    LOCK_KEY,                              // "lock:inventory:1"
    () -> purchaseWithoutLock(modeLabel),  // onAcquired: do the work
    () -> new PurchaseResult(false, ...)   // onBusy: lock was taken
);</code></pre>
          <p class="review-note">Your <code>withLock()</code> implementation protects the read-modify-write in <code>purchaseWithoutLock()</code> from race conditions.</p>
        </div>

        <div class="impl-step">
          <h4>Step 5: Rebuild and Restart</h4>
          <p>Go to the <a :href="workshopHubUrl" target="_blank">Workshop Hub</a> and rebuild & restart the distributed locks service.</p>
        </div>

        <div class="impl-step">
          <h4>Verify Your Implementation</h4>
          <p>After rebuilding, check that both pieces are in place:</p>
          <button @click="checkLockStatus" class="btn btn-secondary" :disabled="checkingLock" style="margin-top: 0.5rem;">
            {{ checkingLock ? 'Checking...' : 'Check Lock Status' }}
          </button>
          <p class="status-line" style="margin-top: 0.5rem;">
            Config (<code>workshop.lock.mode</code>): <strong :class="lockConfigured ? 'status-ok' : 'status-missing'">{{ lockConfigured ? 'redisson' : 'none' }}</strong><br/>
            Code (<code>tryLock + unlock</code>): <strong :class="lockImplemented ? 'status-ok' : 'status-missing'">{{ lockImplemented ? 'implemented' : 'missing' }}</strong>
          </p>
        </div>

        <div class="button-group">
          <router-link to="/reentrant" class="btn btn-secondary">← Back to Learn</router-link>
          <router-link to="/reentrant/demo" class="btn btn-primary" v-if="lockConfigured && lockImplemented">Test Your Implementation →</router-link>
          <span v-else class="btn btn-disabled">Complete implementation first</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getWorkshopHubUrl } from '../utils/basePath';
import { fetchLockStatus, updateLockTypeProgress } from '../utils/locksWorkshop';

export default {
  name: 'LocksImplement',
  data() {
    return {
      lockConfigured: false,
      lockImplemented: false,
      checkingLock: false
    };
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  mounted() {
    this.checkLockStatus();
  },
  methods: {
    async checkLockStatus() {
      this.checkingLock = true;
      try {
        const status = await fetchLockStatus();
        this.lockConfigured = status.configured;
        this.lockImplemented = status.implemented;
        // Update progress if implemented
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
  max-width: 800px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-6);
}

.section-header h2 {
  margin: 0;
  font-size: 1.5rem;
}

.alert {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-sm);
}

.alert-info { background: #094771; }

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
.impl-step h4 {
  margin: 0 0 0.5rem 0;
  color: #DC382C;
  font-size: 1rem;
}
.impl-step p {
  margin: 0.5rem 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}
.impl-step.review-step {
  border-left: 3px solid #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}
.impl-step.review-step h4 {
  color: #3b82f6;
}
.impl-step .review-note {
  font-style: italic;
  margin-top: 0.75rem;
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
.impl-step ul.dep-list {
  margin: 0.5rem 0 0 1.25rem;
  padding: 0;
}
.impl-step ul.dep-list li {
  margin-bottom: 0.25rem;
}

.status-line { font-size: 0.9rem; }
.status-ok { color: #22c55e; }
.status-missing { color: #ef4444; }

.button-group {
  display: flex;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  font-weight: 500;
  cursor: pointer;
  border: none;
  text-decoration: none;
  display: inline-block;
}
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover { background: #c42f24; }
.btn-secondary { background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover { background: var(--color-border); }
.btn-disabled { background: var(--color-dark-800); color: var(--color-text-muted); cursor: not-allowed; }
</style>
