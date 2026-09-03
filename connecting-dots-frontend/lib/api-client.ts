/**
 * Centralized API client for authenticated requests with Bearer token
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
}

export function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null
  return sessionStorage.getItem('auth_token')
}

export function setAuthSession(token: string, role?: string, userId?: string, email?: string) {
  if (typeof window === 'undefined') return
  sessionStorage.setItem('auth_token', token)
  if (role) sessionStorage.setItem('auth_role', role)
  if (userId) sessionStorage.setItem('auth_user_id', userId)
  if (email) sessionStorage.setItem('auth_email', email)
  window.dispatchEvent(new Event('storage'))
}

export function clearAuthSession() {
  if (typeof window === 'undefined') return
  sessionStorage.removeItem('auth_token')
  sessionStorage.removeItem('auth_role')
  sessionStorage.removeItem('auth_user_id')
  sessionStorage.removeItem('auth_email')
  window.dispatchEvent(new Event('storage'))
}

export function logout() {
  clearAuthSession()
}

export function getAuthUserId(): string | null {
  if (typeof window === 'undefined') return null
  return sessionStorage.getItem('auth_user_id') || null
}

export function getAuthRole(): string | null {
  if (typeof window === 'undefined') return null
  return sessionStorage.getItem('auth_role') || null
}

export function getAuthEmail(): string | null {
  if (typeof window === 'undefined') return null
  return sessionStorage.getItem('auth_email') || extractEmailFromToken()
}

export function getSavedUser(): { email: string; role: string } | null {
  const email = getAuthEmail()
  const role = getAuthRole() || extractRoleFromToken() || 'GUEST'
  if (!email) return null
  return { email, role }
}

async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getAuthToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const errorText = await response.text()
    let errorMessage = errorText
    try {
      const parsed = JSON.parse(errorText)
      errorMessage = parsed.message || parsed.error || errorText
    } catch {
      // fallback to raw text
    }

    if (response.status === 401 && endpoint.includes('/auth/login')) {
      errorMessage = errorMessage || 'Invalid email or password.'
    }

    throw new Error(
      errorMessage || `API Error [${response.status}]: ${response.statusText}`,
    )
  }

  const contentType = response.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    return response.json()
  }
  return (await response.text()) as unknown as T
}

export async function apiRequest<T>(endpoint: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  return apiFetch<T>(endpoint, { method: options.method || 'GET', ...(options.body === undefined ? {} : { body: JSON.stringify(options.body) }) })
}

export async function apiGet<T>(endpoint: string): Promise<T> {
  return apiFetch<T>(endpoint, { method: 'GET' })
}

export async function apiPost<T>(endpoint: string, body: unknown): Promise<T> {
  return apiFetch<T>(endpoint, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function apiPut<T>(endpoint: string, body: unknown): Promise<T> {
  return apiFetch<T>(endpoint, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export async function apiDelete<T>(endpoint: string): Promise<T> {
  return apiFetch<T>(endpoint, { method: 'DELETE' })
}

/**
 * Extract admin email from Bearer token JWT (assumes email is in payload)
 */
export function extractEmailFromToken(): string | null {
  const token = getAuthToken()
  if (!token) return null

  try {
    const [, payload] = token.split('.')
    const decoded = JSON.parse(atob(payload))
    return decoded.email || decoded.sub || null
  } catch {
    return null
  }
}

/**
 * Extract role from Bearer token JWT
 */
export function extractRoleFromToken(): string | null {
  const token = getAuthToken()
  if (!token) return null

  try {
    const [, payload] = token.split('.')
    const decoded = JSON.parse(atob(payload))
    return decoded.role || null
  } catch {
    return null
  }
}

/**
 * Check if current user is admin based on token email or role
 */
export function isAdminUser(): boolean {
  const email = extractEmailFromToken()
  const role = getAuthRole() || extractRoleFromToken()
  return email === 'admin@connectingdots.org' || role === 'ROLE_ADMIN' || role === 'ADMIN'
}
