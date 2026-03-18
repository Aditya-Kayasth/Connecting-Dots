import axios from 'axios'

/**
 * Shared axios instance for all calls to the Spring Boot backend.
 * Base URL points to the local dev server; swap for a deployed URL via env var in production.
 */
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30_000, // 30s — Groq LLM calls can take a moment
})

export default apiClient
