import { getApiUrl } from './basePath';

export async function loadWorkshopContentView(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${encodeURIComponent(viewId)}`), {
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error(`Failed to load workshop content view ${viewId}: ${response.status}`);
  }

  return response.json();
}
