<template>
  <div class="locks-editor">
    <div class="main-container">
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2>
            <div class="logo-small">
              <img src="@/assets/logo/small.png" alt="Redis Logo" width="24" height="24" />
            </div>
            Enable Redisson Locks
          </h2>
        </div>
        <div class="workshop-content">
          <div class="alert">
            <strong>Your Task:</strong> Implement distributed locking step-by-step.
          </div>

          <p class="note">Type the code yourself for maximum learning. Hover over <span class="info-icon-inline">i</span> for explanations, use hint buttons (?) if stuck.</p>

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
              <span class="step-content">Open <code>PurchaseService.java</code> (read-only review)</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="See how withLock() protects the inventory read-modify-write from race conditions">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="loadFileStep('PurchaseService.java')">▶</button>
              </div>
            </li>
          </ol>

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
        </div>
      </div>

      <div class="editor-container">
        <div class="file-tabs">
          <button
            v-for="file in files"
            :key="file"
            :class="['file-tab', { active: currentFile === file }]"
            @click="loadFile(file)"
          >
            {{ file }}
          </button>
        </div>

        <div class="editor-panel">
          <div class="editor-header">
            <h3>{{ currentFile || 'Select a file to edit' }}</h3>
            <div class="editor-actions">
              <button @click="saveFile" class="btn btn-primary" :disabled="!currentFile">Save Changes</button>
              <button @click="reloadFile" class="btn btn-secondary" :disabled="!currentFile">Reload</button>
            </div>
          </div>
          <div class="code-editor-wrapper">
            <div class="editor-container-inner">
              <pre class="code-highlight"><code :class="languageClass" v-html="highlightedCode"></code></pre>
              <textarea
                ref="codeTextarea"
                v-model="fileContent"
                class="code-editor"
                :disabled="!currentFile"
                spellcheck="false"
                @scroll="syncScroll"
              ></textarea>
            </div>
          </div>
          <div v-if="statusMessage" :class="['status-message', statusType]">
            {{ statusMessage }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import kotlin from 'highlight.js/lib/languages/kotlin';
import properties from 'highlight.js/lib/languages/properties';

hljs.registerLanguage('java', java);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('properties', properties);

export default {
  name: 'LocksEditor',
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'LockManager.java',
        'PurchaseService.java'
      ],
      currentFile: null,
      fileContent: '',
      statusMessage: '',
      statusType: ''
    };
  },
  computed: {
    workshopHubUrl() {
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      return `${protocol}//${hostname}:9000`;
    },
    languageClass() {
      if (!this.currentFile) {
        return '';
      }
      if (this.currentFile.endsWith('.java')) {
        return 'language-java';
      }
      if (this.currentFile.endsWith('.kts')) {
        return 'language-kotlin';
      }
      if (this.currentFile.endsWith('.properties')) {
        return 'language-properties';
      }
      return 'language-text';
    },
    highlightedCode() {
      if (!this.fileContent) {
        return '';
      }
      return hljs.highlightAuto(this.fileContent, ['java', 'kotlin', 'properties']).value;
    }
  },
  methods: {
    async loadFile(fileName) {
      this.currentFile = fileName;
      this.statusMessage = '';
      try {
        const response = await fetch(`/api/editor/file/${encodeURIComponent(fileName)}`);
        const data = await response.json();
        if (data.error) {
          this.statusMessage = data.error;
          this.statusType = 'error';
          return;
        }
        this.fileContent = data.content || '';
      } catch (error) {
        this.statusMessage = 'Failed to load file.';
        this.statusType = 'error';
      }
    },
    loadFileStep(fileName) {
      this.loadFile(fileName);
    },
    async saveFile() {
      if (!this.currentFile) {
        return;
      }
      try {
        const response = await fetch(`/api/editor/file/${encodeURIComponent(this.currentFile)}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ content: this.fileContent })
        });
        const data = await response.json();
        if (data.error) {
          this.statusMessage = data.error;
          this.statusType = 'error';
          return;
        }
        this.statusMessage = 'Saved successfully.';
        this.statusType = 'success';
      } catch (error) {
        this.statusMessage = 'Failed to save file.';
        this.statusType = 'error';
      }
    },
    reloadFile() {
      if (this.currentFile) {
        this.loadFile(this.currentFile);
      }
    },
    async enableDependencies() {
      // Auto-load the file if not already loaded
      if (!this.currentFile || !this.currentFile.endsWith('build.gradle.kts')) {
        await this.loadFile('build.gradle.kts');
      }
      if (!this.fileContent) {
        this.statusMessage = 'Failed to load build.gradle.kts.';
        this.statusType = 'error';
        return;
      }
      // Check if already uncommented
      if (!this.fileContent.includes('// implementation("org.springframework.boot:spring-boot-starter-data-redis")')) {
        this.statusMessage = 'Dependencies already enabled! Click Save to persist.';
        this.statusType = 'success';
        return;
      }
      // Uncomment the dependencies
      this.fileContent = this.fileContent
        .replace('// implementation("org.springframework.boot:spring-boot-starter-data-redis")', 'implementation("org.springframework.boot:spring-boot-starter-data-redis")')
        .replace('// implementation("org.redisson:redisson-spring-boot-starter:3.27.2")', 'implementation("org.redisson:redisson-spring-boot-starter:3.27.2")');
      this.statusMessage = 'Dependencies enabled. Click Save to persist.';
      this.statusType = 'success';
    },
    enableLockMode() {
      if (!this.currentFile || !this.fileContent.includes('workshop.lock.mode')) {
        this.statusMessage = 'Open application.properties first.';
        this.statusType = 'error';
        return;
      }
      this.fileContent = this.fileContent.replace('workshop.lock.mode=none', 'workshop.lock.mode=redisson');
      this.statusMessage = 'Lock mode updated. Click Save to persist.';
      this.statusType = 'success';
    },
    async applyRedissonLock() {
      // Auto-load the file if not already loaded
      if (!this.currentFile || !this.currentFile.endsWith('LockManager.java')) {
        await this.loadFile('LockManager.java');
      }
      if (!this.fileContent) {
        this.statusMessage = 'Failed to load LockManager.java.';
        this.statusType = 'error';
        return;
      }
      // Check if already implemented
      if (this.fileContent.includes('lock.tryLock(') && this.fileContent.includes('lock.unlock()')) {
        this.statusMessage = 'Lock already implemented! Click Save to persist.';
        this.statusType = 'success';
        return;
      }

      // Step 1: Uncomment the RedissonClient import
      this.fileContent = this.fileContent.replace(
        '// import org.redisson.api.RedissonClient;',
        'import org.redisson.api.RedissonClient;'
      );

      // Step 2: Uncomment the redissonClient field
      this.fileContent = this.fileContent.replace(
        '// private final RedissonClient redissonClient;',
        'private final RedissonClient redissonClient;'
      );

      // Step 3: Uncomment the constructor parameter
      this.fileContent = this.fileContent.replace(
        '// @Autowired(required = false) RedissonClient redissonClient,',
        '@Autowired(required = false) RedissonClient redissonClient,'
      );

      // Step 4: Uncomment the field assignment
      this.fileContent = this.fileContent.replace(
        '// this.redissonClient = redissonClient;',
        'this.redissonClient = redissonClient;'
      );

      // Step 5: Update isEnabled() method
      this.fileContent = this.fileContent.replace(
        /public boolean isEnabled\(\) \{\s*\/\/ TODO: Update this check after implementing the lock\s*\/\/ return "redisson"\.equalsIgnoreCase\(lockMode\) && redissonClient != null;\s*return false;\s*\}/,
        `public boolean isEnabled() {
        return "redisson".equalsIgnoreCase(lockMode) && redissonClient != null;
    }`
      );

      // Step 6: Add imports for RLock and TimeUnit
      this.fileContent = this.ensureImport(this.fileContent, 'import java.util.concurrent.TimeUnit;');
      this.fileContent = this.ensureImport(this.fileContent, 'import org.redisson.api.RLock;');

      // Step 7: Implement the lock in withLock()
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

      this.fileContent = this.fileContent.replace(
        /\/\/ TODO: Implement Redisson lock here\s*\n\s*return onAcquired\.get\(\);/,
        lockImpl
      );

      this.statusMessage = 'All changes applied! Click Save to persist.';
      this.statusType = 'success';
    },
    ensureImport(content, importLine) {
      if (content.includes(importLine)) {
        return content;
      }
      const lines = content.split('\n');
      let lastImportIndex = -1;
      for (let i = 0; i < lines.length; i += 1) {
        if (lines[i].startsWith('import ')) {
          lastImportIndex = i;
        }
      }
      if (lastImportIndex >= 0) {
        lines.splice(lastImportIndex + 1, 0, importLine);
        return lines.join('\n');
      }
      return `${importLine}\n${content}`;
    },
    syncScroll() {
      const textarea = this.$refs.codeTextarea;
      const pre = textarea?.previousElementSibling;
      if (textarea && pre) {
        pre.scrollTop = textarea.scrollTop;
        pre.scrollLeft = textarea.scrollLeft;
      }
    }
  }
};
</script>

<style scoped>
.locks-editor {
  margin: 0;
  padding: 0;
  background: #1a1a1a;
  overflow: hidden;
}

.main-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}

.workshop-panel {
  width: 400px;
  background: #1e1e1e;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.workshop-header {
  background: #252526;
  padding: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.workshop-header h2 {
  margin: 0;
  color: #DC382C;
  font-size: var(--font-size-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.logo-small {
  display: inline-block;
}

.workshop-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-6);
  color: #cccccc;
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.workshop-content h3 {
  color: #fff;
  font-size: var(--font-size-base);
  margin-top: 0;
}

.workshop-content h4 {
  color: #4fc3f7;
  font-size: var(--font-size-sm);
  margin-top: var(--spacing-6);
  margin-bottom: var(--spacing-3);
  font-weight: var(--font-weight-semibold);
}

.workshop-content ol {
  padding-left: var(--spacing-6);
}

.workshop-content li {
  margin-bottom: var(--spacing-3);
}

.step-with-button {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-3);
}

.step-with-button .button-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.step-content code {
  word-break: break-all;
}

.tooltip-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  cursor: help;
}

.tooltip-wrapper::after {
  content: attr(data-tooltip);
  position: absolute;
  bottom: 100%;
  right: 0;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 0.8rem;
  white-space: normal;
  width: 220px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  border: 1px solid #3c3c3c;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s, visibility 0.2s;
  z-index: 100;
  pointer-events: none;
  margin-bottom: 8px;
}

.tooltip-wrapper:hover::after {
  opacity: 1;
  visibility: visible;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  min-width: 18px;
  background: transparent;
  border: 1.5px solid #569cd6;
  border-radius: 50%;
  color: #569cd6;
  font-size: 0.7rem;
  font-weight: bold;
  font-style: italic;
  font-family: serif;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.tooltip-wrapper:hover .info-icon {
  opacity: 1;
}

.info-icon-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: transparent;
  border: 1.5px solid #569cd6;
  border-radius: 50%;
  color: #569cd6;
  font-size: 0.65rem;
  font-weight: bold;
  font-style: italic;
  font-family: serif;
  margin: 0 2px;
  vertical-align: middle;
}

.step-content ul {
  margin-top: var(--spacing-2);
  padding-left: var(--spacing-5);
}

.button-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  flex-shrink: 0;
}

.tooltip-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  cursor: help;
}

.tooltip-wrapper::after {
  content: attr(data-tooltip);
  position: absolute;
  bottom: 100%;
  right: 0;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 0.8rem;
  white-space: normal;
  width: 220px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  border: 1px solid #3c3c3c;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s, visibility 0.2s;
  z-index: 1000;
  margin-bottom: 8px;
  line-height: 1.4;
}

.tooltip-wrapper:hover::after {
  opacity: 1;
  visibility: visible;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  min-width: 18px;
  background: transparent;
  border: 1.5px solid #569cd6;
  border-radius: 50%;
  color: #569cd6;
  font-size: 0.7rem;
  font-weight: bold;
  font-style: italic;
  font-family: serif;
  opacity: 0.7;
  transition: opacity var(--transition-base);
}

.tooltip-wrapper:hover .info-icon {
  opacity: 1;
}

.play-btn {
  width: 28px;
  height: 28px;
  min-width: 28px;
  background: #1e7e34;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 0.7rem;
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
  margin-top: 2px;
}

.play-btn:hover {
  background: #28a745;
  transform: scale(1.1);
}

.play-btn:active {
  transform: scale(0.95);
}

.workshop-content code {
  background: #2d2d2d;
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  color: #ce9178;
  font-size: 0.85rem;
}

.workshop-content .alert {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-4);
  background: #094771;
  border-left: 3px solid #569cd6;
}

.note {
  margin-bottom: var(--spacing-4);
  color: #999;
  font-size: 0.85rem;
}

.step-description {
  margin-bottom: var(--spacing-3);
  color: #bbb;
  font-size: 0.9rem;
  line-height: 1.5;
}

.code-steps {
  margin-top: var(--spacing-2);
  margin-bottom: 0;
  padding-left: var(--spacing-4);
  list-style-type: disc;
}

.code-steps li {
  margin-bottom: var(--spacing-1);
  font-size: 0.85rem;
}

.link {
  color: #569cd6;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.concept-box {
  background: #1e3a5f;
  border-left: 3px solid #4fc3f7;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin: var(--spacing-3) 0;
}

.concept-box p {
  margin: 0;
  font-size: 0.85rem;
  color: #b3d9f2;
}

.concept-box pre {
  background: #0d1f30;
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-2);
  overflow-x: auto;
  font-size: 0.8rem;
}

.concept-box pre code {
  background: transparent;
  padding: 0;
  color: #9cdcfe;
}

.concept-box.warning {
  background: #3d2814;
  border-left-color: #f59e0b;
}

.concept-box.warning p {
  color: #fcd9a8;
}

.concept-box.review {
  background: rgba(59, 130, 246, 0.1);
  border-left-color: #3b82f6;
}

.hint-btn {
  background: #b45309 !important;
}

.hint-btn:hover {
  background: #d97706 !important;
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.file-tabs {
  display: flex;
  background: #252526;
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

.file-tab {
  padding: var(--spacing-3) var(--spacing-6);
  background: transparent;
  border: none;
  color: #969696;
  cursor: pointer;
  transition: all var(--transition-base);
  border-right: 1px solid var(--color-border);
  white-space: nowrap;
  font-size: var(--font-size-sm);
}

.file-tab:hover {
  background: #2a2d2e;
  color: #cccccc;
}

.file-tab.active {
  background: #1e1e1e;
  color: #ffffff;
  border-bottom: 2px solid #DC382C;
}

.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.editor-header {
  background: #252526;
  padding: var(--spacing-3) var(--spacing-4);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
}

.editor-header h3 {
  color: #cccccc;
  margin: 0;
  font-size: var(--font-size-sm);
}

.editor-actions {
  display: flex;
  gap: var(--spacing-2);
}

.editor-actions button {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--font-size-sm);
}

.code-editor-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.editor-container-inner {
  position: relative;
  width: 100%;
  height: 100%;
}

.code-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  padding: var(--spacing-4);
  background: #1e1e1e;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  overflow: auto;
  pointer-events: none;
  white-space: pre;
  color: #d4d4d4;
}

.code-highlight code {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  background: transparent;
  padding: 0;
}

.code-editor {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  color: transparent;
  caret-color: #fff;
  border: none;
  padding: var(--spacing-4);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  z-index: 1;
}

.code-editor:focus {
  outline: none;
}

.code-editor:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-message {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: var(--spacing-4) var(--spacing-6);
  background: #252526;
  color: #fff;
  text-align: center;
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
  z-index: 1000;
  transition: opacity var(--transition-base);
}

.status-message.success {
  background: #1e7e34;
}

.status-message.error {
  background: #bd2130;
}

.btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
}

.btn-primary {
  background: #DC382C;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c42f24;
}

.btn-secondary {
  background: var(--color-dark-800);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-border);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .workshop-panel {
    width: 300px;
  }
}

@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
  }

  .workshop-panel {
    width: 100%;
    max-height: 40vh;
  }
}

/* Highlight.js VS Code Dark+ theme colors - use :deep() for dynamically generated content */
.code-highlight :deep(.hljs-keyword) { color: #569cd6; }
.code-highlight :deep(.hljs-built_in) { color: #4ec9b0; }
.code-highlight :deep(.hljs-type) { color: #4ec9b0; }
.code-highlight :deep(.hljs-literal) { color: #569cd6; }
.code-highlight :deep(.hljs-number) { color: #b5cea8; }
.code-highlight :deep(.hljs-string) { color: #ce9178; }
.code-highlight :deep(.hljs-comment) { color: #6a9955; }
.code-highlight :deep(.hljs-class) { color: #4ec9b0; }
.code-highlight :deep(.hljs-function) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title.function_) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title.class_) { color: #4ec9b0; }
.code-highlight :deep(.hljs-params) { color: #9cdcfe; }
.code-highlight :deep(.hljs-variable) { color: #9cdcfe; }
.code-highlight :deep(.hljs-attr) { color: #9cdcfe; }
.code-highlight :deep(.hljs-property) { color: #9cdcfe; }
.code-highlight :deep(.hljs-punctuation) { color: #d4d4d4; }
.code-highlight :deep(.hljs-operator) { color: #d4d4d4; }
.code-highlight :deep(.hljs-meta) { color: #c586c0; }
.code-highlight :deep(.hljs-decorator) { color: #c586c0; }
.code-highlight :deep(.hljs-annotation) { color: #c586c0; }
</style>
