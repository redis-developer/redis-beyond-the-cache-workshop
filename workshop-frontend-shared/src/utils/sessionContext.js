import { getWorkshopHubUrl } from './basePath.js';

function getRuntimeLocation(location = globalThis.window?.location) {
  return location || { pathname: '', protocol: 'http:', hostname: 'localhost' };
}

function normalizeBaseUrl(baseUrl) {
  if (!baseUrl || baseUrl === '/') {
    return '';
  }

  return baseUrl.replace(/\/$/, '');
}

export function getSessionRouteContext(options = {}) {
  const runtimeLocation = getRuntimeLocation(options.location);
  const pathname = runtimeLocation.pathname || '';
  const marker = '/session/';
  const markerIndex = pathname.indexOf(marker);

  if (markerIndex === -1) {
    return null;
  }

  const sessionId = pathname.slice(markerIndex + marker.length).split('/')[0];

  if (!sessionId) {
    return null;
  }

  const prefix = pathname.slice(0, markerIndex).replace(/\/$/, '');

  return {
    sessionId,
    basePath: `${prefix}${marker}${sessionId}`,
    hubUrl: prefix ? `${prefix}/` : '/'
  };
}

export function buildSessionApiUrl(sessionId, endpointPath = '', options = {}) {
  const hubUrl = options.hubUrl || getWorkshopHubUrl(options);
  const normalizedBaseUrl = normalizeBaseUrl(hubUrl);
  const normalizedEndpointPath = endpointPath
    ? (endpointPath.startsWith('/') ? endpointPath : `/${endpointPath}`)
    : '';

  return `${normalizedBaseUrl}/api/sessions/${encodeURIComponent(sessionId)}${normalizedEndpointPath}`;
}
