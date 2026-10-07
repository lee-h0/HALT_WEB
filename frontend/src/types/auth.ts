/** 백엔드 /api/auth/me 응답 */
export interface User {
  userId: number
  loginId: string
  nickname: string
  email: string
  totalPoints: number
  solvedCount: number
}

/** 백엔드 /api/auth/login 응답 */
export interface LoginResponse {
  accessToken: string
  expiresInSec: number
  userId: number
  nickname: string
}

export interface LoginRequest {
  loginId: string
  password: string
}

export interface SignupRequest {
  loginId: string
  password: string
  nickname: string
  email: string
}
