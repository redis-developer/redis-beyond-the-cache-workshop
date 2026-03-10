/**
 * Re-export shared runtime helpers for simplified imports.
 * Uses relative path to avoid webpack module resolution issues with file: links.
 */
export {
  getBasePath,
  getApiUrl,
  getWorkshopHubUrl,
  getRedisInsightUrl
} from '../../../../../workshop-frontend-shared/src/utils/basePath.js';
