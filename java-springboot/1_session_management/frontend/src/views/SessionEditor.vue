<template>
  <div class="session-editor">
    <div class="main-container">
      <!-- Workshop Instructions Panel -->
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2>
            <div class="logo-small">
              <img src="@/assets/logo/small.png" alt="Redis Logo" width="24" height="24" />
            </div>
            Stage 2: Enable Redis
          </h2>
        </div>
        <div class="workshop-content">
          <div class="alert">
            <strong>Your Task:</strong> Uncomment the necessary code to enable Redis session management.
          </div>

          <h3>Instructions:</h3>
          <p class="note">
            Click the play button (▶) next to any step to automatically apply that change!
          </p>

          <h4>Step 1: Add Redis Dependencies</h4>
          <ol>
            <li class="step-with-button">
              <span class="step-content">Click on <code>build.gradle.kts</code> tab above</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Opens the Gradle build file where dependencies are managed">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="loadFileStep('build.gradle.kts')">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the Redis dependencies:
                <ul>
                  <li><code>spring-boot-starter-data-redis</code></li>
                  <li><code>spring-session-data-redis</code></li>
                </ul>
              </span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="These libraries provide Redis connectivity and Spring Session integration, allowing your app to store sessions in Redis instead of memory">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="uncommentGradleDependencies">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong> button</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Saves your changes to the build file so they can be applied when the app rebuilds">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 2: Configure Application Properties</h4>
          <ol start="4">
            <li class="step-with-button">
              <span class="step-content">Click on <code>application.properties</code> tab above</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Opens the Spring Boot configuration file where application settings are defined">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="loadFileStep('application.properties')">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Change <code>spring.session.store-type=none</code> to <code>spring.session.store-type=redis</code></span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Tells Spring Session to use Redis as the storage backend instead of in-memory storage">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="changeStoreType">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the 3 Redis session configuration lines below it</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Configures Redis session namespace, flush mode, and repository type for organizing and managing session data in Redis">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="uncommentRedisConfig">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong> button</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Saves your configuration changes so they take effect when the app restarts">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 3: Configure Spring Security</h4>
          <ol start="8">
            <li class="step-with-button">
              <span class="step-content">Click on <code>SecurityConfig.java</code> tab above</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Opens the Spring Security configuration file where authentication and session management are configured">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="loadFileStep('SecurityConfig.java')">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>HttpSessionSecurityContextRepository</code> configuration (imports, bean, and filter chain config)</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Configures Spring Security to store authentication details in HTTP sessions (which will now be backed by Redis), enabling session persistence across restarts">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="uncommentSecurityConfig">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong> button</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Saves your security configuration changes">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 4: Restart and Verify</h4>
          <ol start="11">
            <li>Go to the <a :href="workshopHubUrl" target="_blank" style="color: #569cd6; text-decoration: underline;">Workshop Hub</a> and rebuild & restart the app</li>
            <li><router-link to="/welcome" class="link">Return to workshop</router-link> to verify!</li>
          </ol>
        </div>
      </div>

      <!-- Editor Panel -->
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

// Register languages
hljs.registerLanguage('java', java);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('properties', properties);

export default {
  name: 'SessionEditor',
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'SecurityConfig.java'
      ],
      currentFile: null,
      fileContent: '',
      fileLanguage: '',
      statusMessage: '',
      statusType: ''
    };
  },
  computed: {
    basePath() {
      // Get base path from Vue config (set via VUE_APP_BASE_PATH at build time)
      const base = process.env.BASE_URL || '/';
      return base.replace(/\/$/, ''); // Remove trailing slash
    },
    workshopHubUrl() {
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      return `${protocol}//${hostname}:9000`;
    },
    languageClass() {
      const langMap = {
        'java': 'language-java',
        'kotlin': 'language-kotlin',
        'properties': 'language-properties'
      };
      return langMap[this.fileLanguage] || '';
    },
    highlightedCode() {
      if (!this.fileContent) return '';
      try {
        if (this.fileLanguage && hljs.getLanguage(this.fileLanguage)) {
          return hljs.highlight(this.fileContent, { language: this.fileLanguage }).value;
        }
        return this.escapeHtml(this.fileContent);
      } catch (e) {
        return this.escapeHtml(this.fileContent);
      }
    }
  },
  methods: {
    escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    },
    syncScroll() {
      const textarea = this.$refs.codeTextarea;
      const pre = textarea?.parentElement?.querySelector('.code-highlight');
      if (pre) {
        pre.scrollTop = textarea.scrollTop;
        pre.scrollLeft = textarea.scrollLeft;
      }
    },
    async loadFile(fileName) {
      try {
        console.log('Loading file:', fileName);
        const url = `${this.basePath}/api/editor/file/${fileName}`;
        console.log('Fetching URL:', url);

        const response = await fetch(url, {
          credentials: 'include'
        });
        console.log('Response status:', response.status);
        console.log('Response Content-Type:', response.headers.get('content-type'));

        // Get the response text first to see what we're actually getting
        const responseText = await response.text();
        console.log('Response text (first 500 chars):', responseText.substring(0, 500));

        // Try to parse as JSON
        let data;
        try {
          data = JSON.parse(responseText);
          console.log('Response data:', data);
        } catch (parseError) {
          console.error('Failed to parse JSON:', parseError);
          console.error('Full response text:', responseText);
          this.showStatus('Server returned invalid response (not JSON)', 'error');
          return;
        }

        if (data.error) {
          console.error('Server returned error:', data.error);
          this.showStatus(data.error, 'error');
          return;
        }

        this.currentFile = fileName;
        this.fileContent = data.content;
        this.fileLanguage = data.language || this.getLanguageFromFile(fileName);
        this.showStatus('File loaded successfully', 'success');
      } catch (error) {
        console.error('Failed to load file:', error);
        this.showStatus('Failed to load file: ' + error.message, 'error');
      }
    },
    async saveFile() {
      if (!this.currentFile) return;

      try {
        const response = await fetch(`${this.basePath}/api/editor/file/${this.currentFile}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ content: this.fileContent }),
          credentials: 'include'
        });

        const data = await response.json();

        if (data.error) {
          this.showStatus(data.error, 'error');
        } else {
          this.showStatus('File saved successfully! Restart the application to see changes.', 'success');
        }
      } catch (error) {
        this.showStatus('Failed to save file: ' + error.message, 'error');
      }
    },
    reloadFile() {
      if (this.currentFile) {
        this.loadFile(this.currentFile);
      }
    },
    showStatus(message, type) {
      this.statusMessage = message;
      this.statusType = type;
      setTimeout(() => {
        this.statusMessage = '';
        this.statusType = '';
      }, 3000);
    },

    // Auto-fix functions for play buttons
    async loadFileStep(fileName) {
      await this.loadFile(fileName);
      this.showStatus(`Opened ${fileName}`, 'success');
    },

    uncommentGradleDependencies() {
      if (!this.currentFile || this.currentFile !== 'build.gradle.kts') {
        this.showStatus('Please open build.gradle.kts first!', 'error');
        return;
      }

      // Replace commented dependencies with uncommented versions
      this.fileContent = this.fileContent.replace(
        '// implementation("org.springframework.boot:spring-boot-starter-data-redis")',
        'implementation("org.springframework.boot:spring-boot-starter-data-redis")'
      );
      this.fileContent = this.fileContent.replace(
        '// implementation("org.springframework.session:spring-session-data-redis")',
        'implementation("org.springframework.session:spring-session-data-redis")'
      );

      this.showStatus('Redis dependencies uncommented! Click Save!', 'success');
    },

    changeStoreType() {
      if (!this.currentFile || this.currentFile !== 'application.properties') {
        this.showStatus('Please open application.properties first!', 'error');
        return;
      }

      this.fileContent = this.fileContent.replace(
        'spring.session.store-type=none',
        'spring.session.store-type=redis'
      );

      this.showStatus('Store type changed to redis! Click Save!', 'success');
    },

    uncommentRedisConfig() {
      if (!this.currentFile || this.currentFile !== 'application.properties') {
        this.showStatus('Please open application.properties first!', 'error');
        return;
      }

      // Uncomment each Redis config line (note: no space after #)
      this.fileContent = this.fileContent.replace(
        '#spring.session.redis.namespace=spring:session',
        'spring.session.redis.namespace=spring:session'
      );
      this.fileContent = this.fileContent.replace(
        '#spring.session.redis.flush-mode=immediate',
        'spring.session.redis.flush-mode=immediate'
      );
      this.fileContent = this.fileContent.replace(
        '#spring.session.redis.repository-type=default',
        'spring.session.redis.repository-type=default'
      );

      this.showStatus('Redis config uncommented! Click Save!', 'success');
    },

    uncommentSecurityConfig() {
      if (!this.currentFile || this.currentFile !== 'SecurityConfig.java') {
        this.showStatus('Please open SecurityConfig.java first!', 'error');
        return;
      }

      // Uncomment the imports
      this.fileContent = this.fileContent.replace(
        '// import org.springframework.security.web.context.HttpSessionSecurityContextRepository;',
        'import org.springframework.security.web.context.HttpSessionSecurityContextRepository;'
      );
      this.fileContent = this.fileContent.replace(
        '// import org.springframework.security.web.context.SecurityContextRepository;',
        'import org.springframework.security.web.context.SecurityContextRepository;'
      );

      // Uncomment the securityContext configuration in filter chain
      this.fileContent = this.fileContent.replace(
        '            // .securityContext(context -> context\n            //     .securityContextRepository(securityContextRepository())\n            // )',
        '            .securityContext(context -> context\n                .securityContextRepository(securityContextRepository())\n            )'
      );

      // Uncomment the bean
      this.fileContent = this.fileContent.replace(
        '    // @Bean\n    // public SecurityContextRepository securityContextRepository() {\n    //     return new HttpSessionSecurityContextRepository();\n    // }',
        '    @Bean\n    public SecurityContextRepository securityContextRepository() {\n        return new HttpSessionSecurityContextRepository();\n    }'
      );

      this.showStatus('Security config uncommented! Click Save!', 'success');
    },

    getLanguageFromFile(fileName) {
      if (fileName.endsWith('.java')) return 'java';
      if (fileName.endsWith('.kts') || fileName.endsWith('.kt')) return 'kotlin';
      if (fileName.endsWith('.properties')) return 'properties';
      return '';
    }
  }
};
</script>

<style scoped>
.session-editor {
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

.step-content {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.step-content code {
  word-break: break-all;
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

.link {
  color: #569cd6;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
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

