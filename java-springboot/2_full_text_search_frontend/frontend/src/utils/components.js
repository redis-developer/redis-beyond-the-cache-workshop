/**
 * Re-export shared components for simplified imports.
 * Uses relative path to avoid webpack module resolution issues with file: links.
 *
 * Usage:
 *   import { WorkshopModal, WorkshopHeader } from '../utils/components';
 */
export { default as WorkshopModal } from '../../../../../workshop-frontend-shared/src/components/WorkshopModal.vue';
export { default as WorkshopStageNav } from '../../../../../workshop-frontend-shared/src/components/WorkshopStageNav.vue';
export { default as WorkshopProgressIndicator } from '../../../../../workshop-frontend-shared/src/components/WorkshopProgressIndicator.vue';
export { default as WorkshopHubLink } from '../../../../../workshop-frontend-shared/src/components/WorkshopHubLink.vue';
export { default as WorkshopHeader } from '../../../../../workshop-frontend-shared/src/components/WorkshopHeader.vue';
export { default as WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/components/WorkshopEditorLayout.vue';
export { default as WorkshopContentRenderer } from '../../../../../workshop-frontend-shared/src/components/WorkshopContentRenderer.vue';
