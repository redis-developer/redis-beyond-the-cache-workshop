<template>
  <div class="workshop-editor">
    <div class="main-container">
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2>
            <div class="logo-small">
              <img src="@/assets/logo/small.png" alt="Redis Logo" width="24" height="24" />
            </div>
            {{ title }}
          </h2>
        </div>
        <div class="workshop-content">
          <slot name="instructions"></slot>
        </div>
      </div>

      <CodeEditor
        ref="editor"
        :files="files"
        @file-loaded="onFileLoaded"
        @file-saved="onFileSaved"
      />
    </div>
  </div>
</template>

<script>
import CodeEditor from './CodeEditor.vue';
import { getWorkshopHubUrl } from '../utils/basePath.js';

export default {
  name: 'WorkshopEditorLayout',
  components: { CodeEditor },
  props: {
    title: { type: String, required: true },
    files: { type: Array, required: true }
  },
  emits: ['file-loaded', 'file-saved'],
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  methods: {
    onFileLoaded(data) {
      this.$emit('file-loaded', data);
    },
    onFileSaved(data) {
      this.$emit('file-saved', data);
    },
    // Proxy methods to CodeEditor
    loadFile(fileName) { return this.$refs.editor.loadFile(fileName); },
    save() { return this.$refs.editor.save(); },
    reload() { return this.$refs.editor.reload(); },
    showStatus(msg, type) { return this.$refs.editor.showStatus(msg, type); },
    updateContent(content) { return this.$refs.editor.updateContent(content); },
    getCurrentContent() { return this.$refs.editor.getCurrentContent(); },
    getCurrentFile() { return this.$refs.editor.getCurrentFile(); }
  }
};
</script>

<style scoped>
.workshop-editor { margin: 0; padding: 0; background: #1a1a1a; overflow: hidden; }
.main-container { display: flex; height: 100vh; width: 100vw; }
.workshop-panel { width: 400px; background: #1e1e1e; border-right: 1px solid var(--color-border); display: flex; flex-direction: column; overflow: hidden; }
.workshop-header { background: #252526; padding: var(--spacing-4); border-bottom: 1px solid var(--color-border); }
.workshop-header h2 { margin: 0; color: #DC382C; font-size: var(--font-size-lg); display: flex; align-items: center; gap: var(--spacing-2); }
.logo-small { display: inline-block; }
.workshop-content { flex: 1; overflow-y: auto; padding: var(--spacing-6); color: #cccccc; font-size: var(--font-size-sm); line-height: 1.6; }
.workshop-content :deep(h3) { color: #fff; font-size: var(--font-size-base); margin-top: 0; }
.workshop-content :deep(h4) { color: #4fc3f7; font-size: var(--font-size-sm); margin-top: var(--spacing-6); margin-bottom: var(--spacing-3); font-weight: var(--font-weight-semibold); }
.workshop-content :deep(ol) { padding-left: var(--spacing-6); }
.workshop-content :deep(li) { margin-bottom: var(--spacing-3); }
.workshop-content :deep(.step-with-button) { display: flex; align-items: flex-start; gap: var(--spacing-2); margin-bottom: var(--spacing-3); }
.workshop-content :deep(.step-content) { flex: 1; min-width: 0; word-break: break-word; }
.workshop-content :deep(.step-content code) { word-break: break-all; }
.workshop-content :deep(.step-content ul) { margin-top: var(--spacing-2); padding-left: var(--spacing-5); }
.workshop-content :deep(.button-group) { display: flex; align-items: center; gap: var(--spacing-2); flex-shrink: 0; }
.workshop-content :deep(.tooltip-wrapper) { position: relative; display: flex; align-items: center; cursor: help; }
.workshop-content :deep(.tooltip-wrapper::after) { content: attr(data-tooltip); position: absolute; bottom: 100%; right: 0; background: #1e1e1e; color: #d4d4d4; padding: 8px 12px; border-radius: 4px; font-size: 0.8rem; white-space: normal; width: 250px; max-width: 250px; text-align: left; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3); border: 1px solid #3c3c3c; opacity: 0; visibility: hidden; transition: opacity 0.2s, visibility 0.2s; z-index: 1000; margin-bottom: 8px; line-height: 1.4; pointer-events: none; word-wrap: break-word; overflow-wrap: break-word; }
.workshop-content :deep(.tooltip-wrapper:hover::after) { opacity: 1; visibility: visible; }
.workshop-content :deep(.info-icon) { display: flex; align-items: center; justify-content: center; width: 18px; height: 18px; min-width: 18px; background: transparent; border: 1.5px solid #569cd6; border-radius: 50%; color: #569cd6; font-size: 0.7rem; font-weight: bold; font-style: italic; font-family: serif; opacity: 0.7; transition: opacity 0.2s; }
.workshop-content :deep(.tooltip-wrapper:hover .info-icon) { opacity: 1; }
.workshop-content :deep(.info-icon-inline) { display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; background: transparent; border: 1.5px solid #569cd6; border-radius: 50%; color: #569cd6; font-size: 0.65rem; font-weight: bold; font-style: italic; font-family: serif; margin: 0 2px; vertical-align: middle; }
.workshop-content :deep(.play-btn) { width: 28px; height: 28px; min-width: 28px; background: #1e7e34; color: white; border: none; border-radius: 50%; cursor: pointer; font-size: 0.7rem; transition: all 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; padding: 0; margin-top: 2px; }
.workshop-content :deep(.play-btn:hover) { background: #28a745; transform: scale(1.1); }
.workshop-content :deep(.play-btn:active) { transform: scale(0.95); }
.workshop-content :deep(.play-btn.hint-btn) { background: #b45309; }
.workshop-content :deep(.play-btn.hint-btn:hover) { background: #d97706; }
.workshop-content :deep(code) { background: #2d2d2d; padding: 0.2rem 0.4rem; border-radius: var(--radius-sm); color: #ce9178; font-size: 0.85rem; white-space: normal; overflow-wrap: anywhere; word-break: break-word; }
.workshop-content :deep(.alert) { padding: var(--spacing-4); border-radius: var(--radius-md); margin-bottom: var(--spacing-4); background: #094771; border-left: 3px solid #569cd6; }
.workshop-content :deep(.note) { margin-bottom: var(--spacing-4); color: #999; font-size: 0.85rem; }
.workshop-content :deep(.link) { color: #569cd6; text-decoration: none; }
.workshop-content :deep(.link:hover) { text-decoration: underline; }
.workshop-content :deep(.concept-box) { background: #1e3a5f; border-left: 3px solid #4fc3f7; border-radius: var(--radius-md); padding: var(--spacing-4); margin: var(--spacing-3) 0; }
.workshop-content :deep(.concept-box p) { margin: 0; font-size: 0.85rem; color: #b3d9f2; }
.workshop-content :deep(.concept-box pre) { background: #0d1f30; padding: var(--spacing-3); border-radius: var(--radius-sm); margin-top: var(--spacing-2); overflow-x: auto; font-size: 0.8rem; }
.workshop-content :deep(.concept-box pre code) { background: transparent; padding: 0; color: #9cdcfe; }
.workshop-content :deep(.concept-box.warning) { background: #3d2814; border-left-color: #f59e0b; }
.workshop-content :deep(.concept-box.warning p) { color: #fcd9a8; }
.workshop-content :deep(.concept-box.review) { background: rgba(59, 130, 246, 0.1); border-left-color: #3b82f6; }
.workshop-content :deep(.completion-banner) { margin-top: var(--spacing-6); padding: var(--spacing-4); background: #1e7e34; color: white; border-radius: var(--radius-md); text-align: center; font-weight: var(--font-weight-semibold); }
@media (max-width: 1024px) { .workshop-panel { width: 300px; } }
@media (max-width: 768px) { .main-container { flex-direction: column; } .workshop-panel { width: 100%; max-height: 40vh; } }
</style>
