/**
 * Shared Base Path Utility for Redis Workshops
 * 
 * Dynamically detects the workshop base path from the URL.
 * Works both when running standalone (localhost:8080) and when
 * proxied through the Workshop Hub (/workshop/{service-name}/).
 * 
 * This replaces the hardcoded path regex that was previously
 * duplicated in each workshop's basePath.js.
 */

/**
 * Get the base path for the current workshop.
 * 
 * Automatically detects if running under the Hub proxy by checking
 * for /workshop/{service-name} in the URL path.
 * 
 * @returns {string} The base path (e.g., '/workshop/session-management' or '/')
 */
export function getBasePath() {
  const defaultBase = process.env.BASE_URL || '/';
  const pathname = window.location.pathname || '';
  
  // Match /workshop/{service-name} pattern dynamically
  // This works for any workshop without hardcoding the service name
  const match = pathname.match(/^\/workshop\/[^/]+/);
  
  if (match) {
    // Running under Hub proxy - return the matched path without trailing slash
    return match[0].replace(/\/$/, '');
  }
  
  // Running standalone - return default base path without trailing slash
  return defaultBase.replace(/\/$/, '');
}

/**
 * Get the full API URL for a given endpoint.
 * 
 * Convenience function that combines the base path with an API endpoint.
 * Handles slash normalization automatically.
 * 
 * @param {string} endpoint - The API endpoint (e.g., '/api/session-info' or 'api/session-info')
 * @returns {string} The full API URL
 * 
 * @example
 * // When running at /workshop/session-management/
 * getApiUrl('/api/users') // => '/workshop/session-management/api/users'
 * getApiUrl('api/users')  // => '/workshop/session-management/api/users'
 * 
 * // When running standalone at /
 * getApiUrl('/api/users') // => '/api/users'
 */
export function getApiUrl(endpoint) {
  const basePath = getBasePath();
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  
  if (basePath === '' || basePath === '/') {
    return normalizedEndpoint;
  }
  
  return `${basePath}${normalizedEndpoint}`;
}

