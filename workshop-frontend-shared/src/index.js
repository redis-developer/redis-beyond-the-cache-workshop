// Shared styles and components for Redis workshops
// This package provides common design tokens, themes, and reusable components

// Styles are imported directly via path:
// import '@redis-workshop/shared/styles/tokens.css'
// import '@redis-workshop/shared/styles/dark-theme.css'

// Utilities
export {
  getBasePath,
  getApiUrl,
  getWorkshopHubUrl,
  getRedisInsightUrl,
  getCodeEditorUrl,
  getCodeEditorFileUrl
} from './utils/basePath.js';

export {
  buildSessionApiUrl,
  getSessionRouteContext
} from './utils/sessionContext.js';

export {
  loadEditorFilePathMap,
  loadEditorWorkspaceMetadata,
  resolveEditorFilePath
} from './utils/codeEditorActions.js';

export {
  bindContentAction,
  createContentRenderModel,
  getStageNavItems
} from './content/renderModel.js';

export {
  DEFAULT_ALLOWED_PLACEHOLDER_PATHS,
  createContentInterpolationContext,
  createStringInterpolator,
  interpolateString
} from './content/interpolation.js';

export {
  createWidgetBlock,
  expandMarkdownFirstBlocks,
  parseWidgetDeclaration
} from './content/markdownFirst.js';

export { renderMarkdown } from './content-renderer/markdown.js';

// Components - Editor
export { default as CodeEditor } from './components/CodeEditor.vue';
export { default as WorkshopCodeEditorFrame } from './components/WorkshopCodeEditorFrame.vue';
export { default as WorkshopCodeEditorShell } from './components/WorkshopCodeEditorShell.vue';
export { default as WorkshopEditorLayout } from './components/WorkshopEditorLayout.vue';

// Components - UI (shared across workshop home/views)
export { default as WorkshopModal } from './components/WorkshopModal.vue';
export { default as WorkshopStageNav } from './components/WorkshopStageNav.vue';
export { default as WorkshopProgressIndicator } from './components/WorkshopProgressIndicator.vue';
export { default as WorkshopHubLink } from './components/WorkshopHubLink.vue';
export { default as WorkshopHeader } from './components/WorkshopHeader.vue';
export { default as WorkshopSessionRestartControls } from './components/WorkshopSessionRestartControls.vue';
export { default as WorkshopContentRenderer } from './components/WorkshopContentRenderer.vue';
export { default as WorkshopMarkdownRenderer } from './components/WorkshopMarkdownRenderer.vue';
export { default as WorkshopAppFrame } from './components/WorkshopAppFrame.vue';
export { default as WorkshopToolFrame } from './components/WorkshopToolFrame.vue';
export { default as WorkshopRuntimeToolbar } from './components/WorkshopRuntimeToolbar.vue';
export { default as WorkshopShell } from './components/WorkshopShell.vue';

export {
  createActionSet,
  isRuntimeBlocked,
  isRuntimeBusy,
  isRuntimeReady,
  normalizeRuntimeState,
  resolveWorkshopShellState
} from './composables/useWorkshopShellState.js';

export default {};
