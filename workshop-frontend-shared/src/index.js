// Shared styles and components for Redis workshops
// This package provides common design tokens, themes, and reusable components

// Styles are imported directly via path:
// import '@redis-workshop/shared/styles/tokens.css'
// import '@redis-workshop/shared/styles/dark-theme.css'

// Utilities
export { getBasePath, getApiUrl } from './utils/basePath.js';

// Components
export { default as CodeEditor } from './components/CodeEditor.vue';
export { default as WorkshopEditorLayout } from './components/WorkshopEditorLayout.vue';

export default {};

