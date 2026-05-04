import { getApiUrl } from '../../../../../../workshop-frontend-shared/src/index.js'

async function readJson(response) {
  const text = await response.text()

  if (!text) {
    return {}
  }

  try {
    return JSON.parse(text)
  } catch (error) {
    return { message: text }
  }
}

export async function springAiGet(path) {
  const response = await fetch(getApiUrl(path), {
    credentials: 'include'
  })
  const payload = await readJson(response)

  if (!response.ok) {
    throw new Error(payload.message || `Request failed with status ${response.status}`)
  }

  return payload
}

export async function springAiPost(path, body) {
  const response = await fetch(getApiUrl(path), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })
  const payload = await readJson(response)

  if (!response.ok) {
    throw new Error(payload.message || `Request failed with status ${response.status}`)
  }

  return payload
}
