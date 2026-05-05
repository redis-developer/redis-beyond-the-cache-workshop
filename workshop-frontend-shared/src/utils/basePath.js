/**
 * Shared Base Path Utility for Redis Workshops
 * 
 * Dynamically detects the workshop base path from the URL.
 * Works both when running standalone (localhost:8080) and when
 * proxied through the Workshop Hub (/workshop/{service-name}/)
 * or a session route (/session/{sessionId}/).
 * 
 * This replaces the hardcoded path regex that was previously
 * duplicated in each workshop's basePath.js.
 */

/**
 * Get the base path for the current workshop.
 * 
 * Automatically detects if running under the Hub proxy by checking
 * for /workshop/{service-name} or /session/{sessionId} in the URL path.
 * 
 * @returns {string} The base path (e.g., '/workshop/session-management' or '/')
 */
export function getBasePath() {
  const defaultBase = getDefaultBasePath();
  const pathname = window.location.pathname || '';

  const proxyContext = getWorkshopProxyContext(pathname);

  if (proxyContext) {
    return proxyContext.basePath;
  }

  // Running standalone - return default base path without trailing slash
  return defaultBase;
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

/**
 * Resolve the embedded VS Code proxy URL for the current runtime.
 *
 * The editor is always exposed through the same origin at /code/. When the
 * workshop runs behind a session route, the proxy stays session scoped at
 * /session/{sessionId}/code/.
 *
 * @param {Object} [options]
 * @param {Location} [options.location] - Override location, mainly for tests
 * @param {string} [options.basePath] - Explicit base path override
 * @returns {string} The same origin editor proxy URL
 */
export function getCodeEditorUrl(options = {}) {
  const runtimeLocation = getRuntimeLocation(options.location);
  const proxyContext = getWorkshopProxyContext(runtimeLocation.pathname || '');
  const basePath = normalizeBasePath(
    options.basePath !== undefined
      ? options.basePath
      : (proxyContext?.basePath || getDefaultBasePath())
  );
  const editorUrl = new URL(`${basePath}/code/`, getRuntimeOrigin(runtimeLocation));
  const normalizedWorkspaceRoot = normalizeCodeEditorFilePath(options.workspaceRoot);

  if (normalizedWorkspaceRoot) {
    editorUrl.searchParams.set('folder', normalizedWorkspaceRoot);
  }

  return toSameOriginPath(editorUrl, runtimeLocation);
}

/**
 * Resolve a code-server URL that opens a specific file in the embedded editor.
 *
 * code-server's web workbench does not support a plain `file=` query parameter.
 * It accepts an initial workbench payload with an `openFile` entry instead.
 *
 * @param {string} filePath - Absolute path inside the code-server workspace
 * @param {Object} [options]
 * @param {Location} [options.location] - Override location, mainly for tests
 * @param {string} [options.basePath] - Explicit base path override
 * @param {string} [options.workspaceRoot] - Absolute code-server workspace folder
 * @param {number|string} [options.line] - Optional one-based line number to reveal
 * @param {number|string} [options.column] - Optional one-based column number to reveal
 * @param {string|number} [options.requestId] - Optional value to force iframe reloads
 * @returns {string} The same origin editor proxy URL with open-file payload
 */
export function getCodeEditorFileUrl(filePath, options = {}) {
  const runtimeLocation = getRuntimeLocation(options.location);
  const editorUrl = new URL(
    getCodeEditorUrl(options),
    getRuntimeOrigin(runtimeLocation)
  );
  const normalizedFilePath = normalizeCodeEditorFilePath(filePath);

  if (!normalizedFilePath) {
    return toSameOriginPath(editorUrl, runtimeLocation);
  }

  const payload = [['openFile', toCodeServerRemoteUri(normalizedFilePath, options)]];
  if (normalizeCodeEditorLineNumber(options.line)) {
    payload.push(['gotoLineMode', 'true']);
  }

  editorUrl.searchParams.set('payload', JSON.stringify(payload));
  const normalizedWorkspaceRoot = normalizeCodeEditorFilePath(options.workspaceRoot);
  if (normalizedWorkspaceRoot) {
    editorUrl.searchParams.set('folder', normalizedWorkspaceRoot);
  }

  if (options.requestId !== undefined && options.requestId !== null) {
    editorUrl.searchParams.set('workshopOpen', String(options.requestId));
  }

  return toSameOriginPath(editorUrl, runtimeLocation);
}

function getDefaultBasePath() {
  return (process.env.BASE_URL || '/').replace(/\/$/, '');
}

function normalizeBasePath(basePath) {
  if (!basePath || basePath === '/') {
    return '';
  }

  const withLeadingSlash = basePath.startsWith('/') ? basePath : `/${basePath}`;
  return withLeadingSlash.replace(/\/$/, '');
}

function getRuntimeLocation(location = globalThis.window?.location) {
  return location || { pathname: '', protocol: 'http:', hostname: 'localhost' };
}

function getRuntimeOrigin(location = globalThis.window?.location) {
  const runtimeLocation = getRuntimeLocation(location);

  if (runtimeLocation.origin && runtimeLocation.origin !== 'null') {
    return runtimeLocation.origin.replace(/\/$/, '');
  }

  const protocol = runtimeLocation.protocol || 'http:';
  const host = runtimeLocation.host || runtimeLocation.hostname || 'localhost';

  return `${protocol}//${host}`;
}

function normalizeCodeEditorFilePath(filePath) {
  if (typeof filePath !== 'string') {
    return '';
  }

  const trimmed = filePath.trim();
  if (!trimmed || !trimmed.startsWith('/')) {
    return '';
  }

  return trimmed.replace(/\/+/g, '/');
}

function toCodeServerRemoteUri(filePath, options = {}) {
  const line = normalizeCodeEditorLineNumber(options.line);
  const column = normalizeCodeEditorLineNumber(options.column) || 1;
  const encodedPath = filePath.split('/').map(encodeURIComponent).join('/');
  const selection = line ? `:${line}:${column}` : '';

  return `vscode-remote://remote${encodedPath}${selection}`;
}

function normalizeCodeEditorLineNumber(value) {
  const number = Number.parseInt(value, 10);
  return Number.isFinite(number) && number > 0 ? number : 0;
}

function toSameOriginPath(url, location = globalThis.window?.location) {
  const runtimeLocation = getRuntimeLocation(location);
  const runtimeOrigin = getRuntimeOrigin(runtimeLocation);

  if (url.origin === runtimeOrigin) {
    return `${url.pathname}${url.search}${url.hash}`;
  }

  return url.toString();
}

function getWorkshopProxyContext(pathname = '') {
  const markers = ['/workshop/', '/session/'];

  for (const marker of markers) {
    const markerIndex = pathname.indexOf(marker);
    if (markerIndex === -1) {
      continue;
    }

    const routeToken = pathname
      .slice(markerIndex + marker.length)
      .split('/')[0];

    if (!routeToken) {
      continue;
    }

    const prefix = pathname.slice(0, markerIndex).replace(/\/$/, '');
    const basePath = `${prefix}${marker}${routeToken}`;

    return {
      routeType: marker === '/session/' ? 'session' : 'workshop',
      basePath,
      hubUrl: prefix ? `${prefix}/` : '/'
    };
  }

  return null;
}

function resolveRuntimeUrl({ location, port, getProxyUrl }) {
  const runtimeLocation = getRuntimeLocation(location);
  const proxyContext = getWorkshopProxyContext(runtimeLocation.pathname || '');

  if (proxyContext) {
    return getProxyUrl(proxyContext);
  }

  return `${runtimeLocation.protocol}//${runtimeLocation.hostname}:${port}/`;
}

/**
 * Resolve the Workshop Hub URL for the current runtime.
 *
 * When a workshop is running behind the hub proxy, this returns the hub root
 * path on the current origin. When a workshop is running standalone, it falls
 * back to the local hub port so workshop UIs do not need to hardcode `:9000`.
 *
 * @param {Object} [options]
 * @param {Location} [options.location] - Override location, mainly for tests
 * @param {number|string} [options.port=9000] - Local Workshop Hub port
 * @returns {string} The Workshop Hub URL for the current runtime
 */
export function getWorkshopHubUrl(options = {}) {
  return resolveRuntimeUrl({
    location: options.location,
    port: options.port || 9000,
    getProxyUrl: ({ hubUrl }) => hubUrl
  });
}

/**
 * Resolve the Redis Insight URL for the current runtime.
 *
 * When a workshop is running inside a launched session, Redis Insight should
 * be accessed through the session scoped hub route so learners only inspect
 * their own runtime data. Outside session routes, this falls back to the
 * explicit local Redis Insight port for standalone development.
 *
 * @param {Object} [options]
 * @param {Location} [options.location] - Override location, mainly for tests
 * @param {number|string} [options.port=5540] - Local Redis Insight port
 * @returns {string} The Redis Insight URL for the current runtime
 */
export function getRedisInsightUrl(options = {}) {
  const runtimeLocation = getRuntimeLocation(options.location);
  const proxyContext = getWorkshopProxyContext(runtimeLocation.pathname || '');

  if (proxyContext?.routeType === 'session') {
    return `${getRuntimeOrigin(options.location)}${proxyContext.basePath}/redis-insight/`;
  }

  return `${runtimeLocation.protocol}//${runtimeLocation.hostname}:${options.port || 5540}/`;
}
