import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// Attach JWT from localStorage on every request
apiClient.interceptors.request.use(config => {
  try {
    const stored = localStorage.getItem('cd_auth_user')
    if (stored) {
      const { token } = JSON.parse(stored)
      if (token) config.headers.Authorization = `Bearer ${token}`
    }
  } catch {
    // ignore parse errors
  }
  return config
})

export default apiClient
