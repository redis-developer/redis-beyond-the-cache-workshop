/**
 * Re-export from shared package for backward compatibility.
 * Uses relative path to avoid webpack module resolution issues with file: links.
 */
export { getBasePath, getApiUrl } from '../../../../../workshop-frontend-shared/src/utils/basePath.js';
