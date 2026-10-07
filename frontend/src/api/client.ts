import axios from 'axios'

const TOKEN_KEY = 'accessToken'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

// 주소를 '/api/...'로만 쓴다. 개발 중에는 vite.config.js의 proxy가 백엔드(8080)로 넘겨 준다.
const client = axios.create({
  headers: { 'Content-Type': 'application/json' },
})

// 요청마다 토큰이 있으면 Authorization 헤더에 넣는다
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 토큰이 만료됐거나 잘못돼서 401이 오면 로그아웃 처리한다 (로그인 요청 자체의 401은 제외)
client.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const isLoginRequest = error.config?.url?.includes('/api/auth/login') ?? false
      if (error.response?.status === 401 && !isLoginRequest) {
        clearToken()
        window.dispatchEvent(new Event('auth:logout'))
      }
    }
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
