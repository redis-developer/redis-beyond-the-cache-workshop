import { getApiUrl } from './basePath';

export async function fetchWorkshopContentView(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${viewId}`));
  let payload = null;

  try {
    payload = await response.json();
  } catch (error) {
    payload = null;
  }

  if (!response.ok) {
    const message = payload?.message || payload?.error || `Failed to load content view ${viewId}.`;
    throw new Error(message);
  }

  return payload;
}
