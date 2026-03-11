<template>
  <div class="editor-container">
    <div class="file-tabs">
      <button
        v-for="file in files"
        :key="file"
        :class="['file-tab', { active: currentFile === file }]"
        @click="selectFile(file)"
      >
        {{ file }}
      </button>
    </div>

    <div class="editor-panel">
      <div class="editor-header">
        <h3>{{ currentFile || 'Select a file to edit' }}</h3>
        <div class="editor-actions">
          <button @click="save" class="btn btn-primary" :disabled="!currentFile">Save Changes</button>
          <button @click="reload" class="btn btn-secondary" :disabled="!currentFile">Reload</button>
        </div>
      </div>
      <div class="code-editor-wrapper">
        <div class="editor-container-inner">
          <pre class="code-highlight"><code :class="languageClass" v-html="highlightedCode"></code></pre>
          <textarea
            ref="codeTextarea"
            v-model="content"
            class="code-editor"
            :disabled="!currentFile"
            wrap="off"
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
</template>

<script>
import { getBasePath } from '../utils/basePath.js';
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import kotlin from 'highlight.js/lib/languages/kotlin';
import properties from 'highlight.js/lib/languages/properties';

// Register languages
hljs.registerLanguage('java', java);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('properties', properties);

export default {
  name: 'CodeEditor',
  props: {
    files: {
      type: Array,
      required: true
    },
    initialFile: {
      type: String,
      default: null
    }
  },
  emits: ['file-loaded', 'file-saved', 'status'],
  data() {
    return {
      currentFile: null,
      content: '',
      fileLanguage: '',
      statusMessage: '',
      statusType: ''
    };
  },
  computed: {
    basePath() {
      return getBasePath();
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
      if (!this.content) return '';
      try {
        if (this.fileLanguage && hljs.getLanguage(this.fileLanguage)) {
          return hljs.highlight(this.content, { language: this.fileLanguage }).value;
        }
        return this.escapeHtml(this.content);
      } catch (e) {
        return this.escapeHtml(this.content);
      }
    }
  },
  mounted() {
    if (this.initialFile && this.files.includes(this.initialFile)) {
      this.loadFile(this.initialFile);
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
    getLanguageFromFile(fileName) {
      if (fileName.endsWith('.java')) return 'java';
      if (fileName.endsWith('.kts') || fileName.endsWith('.kt')) return 'kotlin';
      if (fileName.endsWith('.properties')) return 'properties';
      return '';
    },
    async selectFile(fileName) {
      await this.loadFile(fileName);
    },
    async loadFile(fileName) {
      try {
        const url = `${this.basePath}/api/editor/file/${fileName}`;
        const response = await fetch(url, { credentials: 'include' });
        const responseText = await response.text();
        
        let data;
        try {
          data = JSON.parse(responseText);
        } catch (parseError) {
          this.showStatus('Server returned invalid response (not JSON)', 'error');
          return;
        }

        if (data.error) {
          this.showStatus(data.error, 'error');
          return;
        }

        this.currentFile = fileName;
        this.content = data.content;
        this.fileLanguage = data.language || this.getLanguageFromFile(fileName);
        this.showStatus('File loaded successfully', 'success');
        this.$emit('file-loaded', { fileName, content: this.content });
      } catch (error) {
        this.showStatus('Failed to load file: ' + error.message, 'error');
      }
    },
    async save() {
      if (!this.currentFile) return;

      try {
        const response = await fetch(`${this.basePath}/api/editor/file/${this.currentFile}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ content: this.content }),
          credentials: 'include'
        });

        const data = await response.json();

        if (data.error) {
          this.showStatus(data.error, 'error');
        } else {
          this.showStatus('File saved! Restart the app to see changes.', 'success');
          this.$emit('file-saved', { fileName: this.currentFile, content: this.content });
        }
      } catch (error) {
        this.showStatus('Failed to save file: ' + error.message, 'error');
      }
    },
    reload() {
      if (this.currentFile) {
        this.loadFile(this.currentFile);
      }
    },
    showStatus(message, type) {
      this.statusMessage = message;
      this.statusType = type;
      this.$emit('status', { message, type });
      setTimeout(() => {
        this.statusMessage = '';
        this.statusType = '';
      }, 3000);
    },
    // Methods for parent components to interact with the editor
    updateContent(newContent) {
      this.content = newContent;
    },
    getCurrentContent() {
      return this.content;
    },
    getCurrentFile() {
      return this.currentFile;
    }
  }
};
</script>

<style scoped>
.editor-container { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.file-tabs { display: flex; background: #252526; border-bottom: 1px solid var(--color-border); overflow-x: auto; }
.file-tab { padding: var(--spacing-3) var(--spacing-6); background: transparent; border: none; color: #969696; cursor: pointer; transition: all var(--transition-base); border-right: 1px solid var(--color-border); white-space: nowrap; font-size: var(--font-size-sm); }
.file-tab:hover { background: #2a2d2e; color: #cccccc; }
.file-tab.active { background: #1e1e1e; color: #ffffff; border-bottom: 2px solid #DC382C; }
.editor-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.editor-header { background: #252526; padding: var(--spacing-3) var(--spacing-4); display: flex; justify-content: space-between; align-items: center; gap: var(--spacing-3); border-bottom: 1px solid var(--color-border); }
.editor-header h3 { color: #cccccc; margin: 0; font-size: var(--font-size-sm); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.editor-actions { display: flex; gap: var(--spacing-2); }
.editor-actions button { padding: var(--spacing-2) var(--spacing-4); font-size: var(--font-size-sm); }
.code-editor-wrapper { flex: 1; width: 100%; min-width: 0; height: 100%; overflow: hidden; }
.editor-container-inner { position: relative; width: 100%; min-width: 0; height: 100%; overflow: hidden; }
.code-highlight { position: absolute; top: 0; left: 0; right: 0; bottom: 0; margin: 0; padding: var(--spacing-4); background: #1e1e1e; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 14px; line-height: 1.5; overflow: auto; pointer-events: none; white-space: pre; color: #d4d4d4; }
.code-highlight code { font-family: inherit; font-size: inherit; line-height: inherit; background: transparent; padding: 0; }
.code-editor { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: transparent; color: transparent; caret-color: #fff; border: none; padding: var(--spacing-4); font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 14px; line-height: 1.5; white-space: pre; overflow: auto; resize: none; z-index: 1; }
.code-editor:focus { outline: none; }
.code-editor:disabled { opacity: 0.5; cursor: not-allowed; }
.status-message { position: fixed; bottom: 20px; right: 20px; padding: var(--spacing-4) var(--spacing-6); background: #252526; color: #fff; text-align: center; font-size: var(--font-size-sm); border-radius: var(--radius-md); box-shadow: var(--shadow-xl); z-index: 1000; transition: opacity var(--transition-base); }
.status-message.success { background: #1e7e34; }
.status-message.error { background: #bd2130; }
.btn { padding: var(--spacing-2) var(--spacing-4); border: none; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); cursor: pointer; transition: all var(--transition-base); }
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover:not(:disabled) { background: #c42f24; }
.btn-secondary { background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover:not(:disabled) { background: var(--color-border); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
/* Highlight.js VS Code Dark+ theme colors */
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
