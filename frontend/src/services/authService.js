import { api } from './api'

const BASE_URL = 'http://localhost:8081/api/v1'

export const authService = {
  register: (data) => api.post('/users/register', data),

  login: (email, password) =>
    fetch(`${BASE_URL}/users/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password }),
    }).then(async (res) => {
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.message || `Giriş başarısız: ${res.status}`)
      }
      return res.json()
    }),

  // refreshToken cookie'yi browser otomatik gönderiyor
  refresh: () =>
    fetch(`${BASE_URL}/users/refresh`, {
      method: 'POST',
      credentials: 'include',
    }).then((res) => {
      if (!res.ok) throw new Error('No session')
      return res.json()
    }),

  logout: () =>
    fetch(`${BASE_URL}/users/logout`, {
      method: 'POST',
      credentials: 'include',
    }),

  getMe: () => api.get('/users/me'),

  getUserById: (id) => api.get(`/users/${id}`),
}
