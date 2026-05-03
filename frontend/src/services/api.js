import { tokenStore } from './token'

const BASE_URL = 'http://localhost:8081/api/v1'

let _refreshPromise = null

async function refreshAccessToken() {
  if (_refreshPromise) return _refreshPromise

  _refreshPromise = fetch(`${BASE_URL}/users/refresh`, {
    method: 'POST',
    credentials: 'include', // refresh_token cookie'yi otomatik gönderir
  })
    .then(async (res) => {
      if (!res.ok) throw new Error('Refresh failed')
      const data = await res.json()
      tokenStore.set(data.accessToken)
      return data.accessToken
    })
    .finally(() => {
      _refreshPromise = null
    })

  return _refreshPromise
}

async function request(path, options = {}, retry = true) {
  const token = tokenStore.get()

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  })

  if (res.status === 401 && retry) {
    try {
      await refreshAccessToken()
      return request(path, options, false)
    } catch {
      tokenStore.clear()
      window.location.href = '/login'
      throw new Error('Session expired')
    }
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({}))
    throw new Error(error.message || `Request failed: ${res.status}`)
  }

  if (res.status === 204) return null
  return res.json()
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
}
