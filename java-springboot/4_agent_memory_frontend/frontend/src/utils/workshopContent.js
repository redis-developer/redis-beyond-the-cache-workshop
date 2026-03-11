import { getApiUrl } from './basePath';

async function parseError(response) {
  try {
    const payload = await response.json();
    return payload?.message || payload?.error || `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}

export async function loadWorkshopContentView(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${viewId}`), {
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return response.json();
}
