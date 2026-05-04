import { getApiUrl } from '../../../../../../workshop-frontend-shared/src/utils/basePath.js'

export async function fetchWorkshopContent(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${encodeURIComponent(viewId)}`), {
    credentials: 'include'
  })

  if (!response.ok) {
    throw new Error(`Failed to load workshop content (${response.status})`)
  }

  return response.json()
}
