<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Implement the Reentrant Lock"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
      <div class="alert">
        <strong>Your Task:</strong> Follow the workshop path
        <code>/reentrant</code> → <code>/reentrant/implement</code> → <code>/reentrant/editor</code> → <code>/reentrant/demo</code>.
      </div>

      <p class="note">
        Type the code yourself for maximum learning. In this editor you will change
        <code>build.gradle.kts</code>, <code>application.properties</code>, and <code>LockManager.java</code>,
        then review <code>PurchaseService.java</code> before rebuilding and testing.
      </p>

      <h4>Step 1: Add Dependencies</h4>
      <div class="concept-box">
        <p>First, we need to add the Redis and Redisson dependencies to connect to Redis and use distributed locks.</p>
      </div>
      <ol>
        <li class="step-with-button">
          <span class="step-content">Open <code>build.gradle.kts</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Gradle build file where project dependencies are declared">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('build.gradle.kts')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the two dependency lines</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="spring-boot-starter-data-redis connects Spring to Redis. redisson-spring-boot-starter provides RLock and other distributed objects.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn hint-btn" @click="enableDependencies">?</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Save the file</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Persists your changes to disk so they're applied on rebuild">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 2: Enable Lock Mode</h4>
      <div class="concept-box">
        <p>The app checks <code>workshop.lock.mode</code> to decide whether to use locks. Currently it's <code>none</code>.</p>
      </div>
      <ol start="4">
        <li class="step-with-button">
          <span class="step-content">Open <code>application.properties</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Spring Boot configuration file where app settings are defined">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('application.properties')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Change <code>workshop.lock.mode=none</code> → <code>redisson</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Enables the Redisson lock mode. The LockManager checks this value to decide whether to use distributed locks.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn hint-btn" @click="enableLockMode">?</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Save the file</span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 3: Implement the Lock</h4>
      <div class="concept-box">
        <p><strong>In LockManager.java, you need to:</strong></p>
        <ol class="impl-checklist">
          <li>Uncomment the <code>RedissonClient</code> import</li>
          <li>Uncomment the <code>redissonClient</code> field</li>
          <li>Uncomment the constructor parameter and assignment</li>
          <li>Update <code>isEnabled()</code> to check the redissonClient</li>
          <li>Implement the lock in <code>withLock()</code></li>
        </ol>
      </div>
      <ol start="7">
        <li class="step-with-button">
          <span class="step-content">Open <code>LockManager.java</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="The LockManager wraps Redisson's RLock to provide a clean withLock() API for the rest of the app">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('LockManager.java')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment all TODOs and implement the lock</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Applies all changes: imports, field, constructor, isEnabled() check, and the full tryLock/unlock implementation">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn hint-btn" @click="applyRedissonLock">?</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Save the file</span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 4: See How Your Lock Gets Used</h4>
      <div class="concept-box review">
        <p>Open <code>PurchaseService.java</code> to see how your <code>withLock()</code> is called:</p>
        <pre><code>lockManager.withLock(
    "lock:inventory:1",  // lock key
    () -> purchaseWithoutLock(...),  // onAcquired
    () -> new PurchaseResult(false, ...)  // onBusy
);</code></pre>
      </div>
      <ol start="10">
        <li class="step-with-button">
          <span class="step-content">Show <code>PurchaseService.java</code> (read-only review)</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="See how withLock() protects the inventory read-modify-write from race conditions">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadPurchaseServiceReview">▶</button>
          </div>
        </li>
      </ol>

      <div v-if="purchaseServiceReview" class="concept-box review-source">
        <p><strong>PurchaseService.java</strong> is shown here as a read-only reference:</p>
        <pre><code>{{ purchaseServiceReview }}</code></pre>
      </div>

      <h4>Step 5: Rebuild & Test</h4>
      <ol start="11">
        <li>Go to <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> → click <strong>Rebuild &amp; Restart</strong></li>
        <li>Wait ~15 seconds for restart</li>
        <li><router-link to="/reentrant/demo" class="link">Test your implementation</router-link> → Lock Status should show <strong>Enabled</strong></li>
      </ol>

      <div class="concept-box warning">
        <strong>Think about it:</strong> What happens if you forget <code>unlock()</code>?
        The lock stays held until <code>leaseTime</code> expires—blocking all other workers!
      </div>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/index.js';
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';

export default {
  name: 'LocksEditor',
  components: { WorkshopEditorLayout },
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'LockManager.java'
      ],
      currentFile: null,
      fileContent: '',
      purchaseServiceReview: ''
    };
  },
  computed: {
    workshopHubUrl() {
      return this.$refs.layout?.workshopHubUrl || getWorkshopHubUrl();
    }
  },
  methods: {
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
    },
    async loadPurchaseServiceReview() {
      try {
        const response = await fetch(getApiUrl('/api/review/purchase-service'));
        const data = await response.json();
        if (data.error) {
          this.$refs.layout.showStatus(data.error, 'error');
          return;
        }
        this.purchaseServiceReview = data.content || '';
        this.$refs.layout.showStatus('Read-only review loaded.', 'success');
      } catch (error) {
        this.$refs.layout.showStatus('Failed to load PurchaseService.java review.', 'error');
      }
    },
    saveFile() {
      this.$refs.layout.save();
    },
    async enableDependencies() {
      if (!this.currentFile?.endsWith('build.gradle.kts')) {
        await this.$refs.layout.loadFile('build.gradle.kts');
      }
      const content = this.$refs.layout.getCurrentContent();
      if (!content.includes('// implementation("org.springframework.boot:spring-boot-starter-data-redis")')) {
        this.$refs.layout.showStatus('Dependencies already enabled! Click Save to persist.', 'success');
        return;
      }
      const updated = content
        .replace('// implementation("org.springframework.boot:spring-boot-starter-data-redis")', 'implementation("org.springframework.boot:spring-boot-starter-data-redis")')
        .replace('// implementation("org.redisson:redisson-spring-boot-starter:3.27.2")', 'implementation("org.redisson:redisson-spring-boot-starter:3.27.2")');
      this.$refs.layout.updateContent(updated);
      this.$refs.layout.showStatus('Dependencies enabled. Click Save to persist.', 'success');
    },
    enableLockMode() {
      const content = this.$refs.layout.getCurrentContent();
      if (!content.includes('workshop.lock.mode')) {
        this.$refs.layout.showStatus('Open application.properties first.', 'error');
        return;
      }
      this.$refs.layout.updateContent(content.replace('workshop.lock.mode=none', 'workshop.lock.mode=redisson'));
      this.$refs.layout.showStatus('Lock mode updated. Click Save to persist.', 'success');
    },
    async applyRedissonLock() {
      if (!this.currentFile?.endsWith('LockManager.java')) {
        await this.$refs.layout.loadFile('LockManager.java');
      }
      let content = this.$refs.layout.getCurrentContent();
      if (content.includes('lock.tryLock(') && content.includes('lock.unlock()')) {
        this.$refs.layout.showStatus('Lock already implemented! Click Save to persist.', 'success');
        return;
      }

      content = content.replace('// import org.redisson.api.RedissonClient;', 'import org.redisson.api.RedissonClient;');
      content = content.replace('// private final RedissonClient redissonClient;', 'private final RedissonClient redissonClient;');
      content = content.replace('// @Autowired(required = false) RedissonClient redissonClient,', '@Autowired(required = false) RedissonClient redissonClient,');
      content = content.replace('// this.redissonClient = redissonClient;', 'this.redissonClient = redissonClient;');
      content = content.replace(
        /public boolean isEnabled\(\) \{\s*\/\/ TODO: Update this check after implementing the lock\s*\/\/ return "redisson"\.equalsIgnoreCase\(lockMode\) && redissonClient != null;\s*return false;\s*\}/,
        `public boolean isEnabled() {\n        return "redisson".equalsIgnoreCase(lockMode) && redissonClient != null;\n    }`
      );
      content = this.ensureImport(content, 'import java.util.concurrent.TimeUnit;');
      content = this.ensureImport(content, 'import org.redisson.api.RLock;');

      const lockImpl = `RLock lock = redissonClient.getLock(lockKey);
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
        }`;

      content = content.replace(/\/\/ TODO: Implement Redisson lock here\s*\n\s*return onAcquired\.get\(\);/, lockImpl);
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('All changes applied! Click Save to persist.', 'success');
    },
    ensureImport(content, importLine) {
      if (content.includes(importLine)) return content;
      const lines = content.split('\n');
      let lastImportIndex = -1;
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].startsWith('import ')) lastImportIndex = i;
      }
      if (lastImportIndex >= 0) {
        lines.splice(lastImportIndex + 1, 0, importLine);
        return lines.join('\n');
      }
      return `${importLine}\n${content}`;
    }
  }
};
</script>

<style scoped>
/* All styling is now provided by WorkshopEditorLayout */
/* impl-checklist is specific to this workshop */
.impl-checklist {
  padding-left: var(--spacing-5);
  margin-top: var(--spacing-2);
}
</style>
