import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import type { LoginResponse } from '../types/auth'

/**
 * Access Token은 메모리(이 변수)에만 둔다. localStorage에 두지 않아 XSS로 훔치기 어렵다.
 * 새로고침하면 사라지지만, HttpOnly 쿠키의 Refresh Token으로 다시 받아 온다 (refreshAccessToken).
 */
let accessToken: string | null = null

export function getToken(): string | null {
  return accessToken
}

export function setToken(token: string): void {
  accessToken = token
}

export function clearToken(): void {
  accessToken = null
}

// 1단계에서 localStorage에 저장하던 토큰이 남아 있으면 지운다
try {
  localStorage.removeItem('accessToken')
} catch {
  // 저장소를 쓸 수 없는 환경이면 무시
}

// 주소를 '/api/...'로만 쓴다. 개발 중에는 vite.config의 proxy가 백엔드(8080)로 넘겨 준다.
// 같은 주소(origin)라 쿠키는 브라우저가 알아서 붙인다.
const client = axios.create({
  headers: { 'Content-Type': 'application/json' },
})

let refreshing: Promise<string | null> | null = null

/**
 * 쿠키의 Refresh Token으로 Access Token을 다시 받는다.
 * 동시에 여러 요청이 401을 받아도 재발급은 한 번만 하도록, 진행 중인 요청을 같이 쓴다.
 *
 * @returns 새 Access Token. 로그인 상태가 아니거나 만료되었으면 null
 */
export function refreshAccessToken(): Promise<string | null> {
  if (refreshing !== null) return refreshing

  // client가 아니라 axios를 직접 써서 아래 401 처리(재발급 → 재시도)에 다시 걸리지 않게 한다
  const request = axios
    .post<LoginResponse | ''>('/api/auth/refresh')
    .then((response) => {
      // 204: 쿠키가 없음 (로그인하지 않은 방문자)
      if (response.status === 204 || !response.data) {
        clearToken()
        return null
      }
      setToken(response.data.accessToken)
      return response.data.accessToken
    })
    .catch(() => {
      clearToken()
      return null
    })
    .finally(() => {
      refreshing = null
    })
  refreshing = request
  return request
}

// 요청마다 토큰이 있으면 Authorization 헤더에 넣는다
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

type RetryConfig = InternalAxiosRequestConfig & { _retried?: boolean }

const AUTH_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout']

/**
 * 401이 오면 (Access Token 만료) Refresh Token으로 재발급받고 원래 요청을 한 번 다시 보낸다.
 * 재발급도 실패하면 로그아웃 처리한다.
 */
client.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
      return Promise.reject(error)
    }
    const config = error.config as RetryConfig
    const isAuthRequest = AUTH_PATHS.some((path) => config.url?.includes(path))
    if (isAuthRequest) {
      return Promise.reject(error)
    }

    if (!config._retried) {
      config._retried = true
      const token = await refreshAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return client(config)
      }
    }

    clearToken()
    window.dispatchEvent(new Event('auth:logout'))
    return Promise.reject(error)
  },
)

/** 서버가 보낸 {"message": "..."}를 꺼낸다. 없으면 기본 문구를 돌려준다 */
export function getErrorMessage(error: unknown, fallback = '요청을 처리하지 못했습니다.'): string {
  if (!axios.isAxiosError(error)) {
    return fallback
  }
  if (!error.response) {
    return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
  }
  const data = error.response.data as { message?: string } | undefined
  return data?.message ?? fallback
}

export default client
