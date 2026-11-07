import client from './client'
import type { LoginRequest, LoginResponse, SignupRequest, User } from '../types/auth'

export async function signup(request: SignupRequest): Promise<void> {
  await client.post('/api/auth/signup', request)
}

/** 로그인. Access Token은 응답 본문으로, Refresh Token은 HttpOnly 쿠키로 받는다 */
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/api/auth/login', request)
  return response.data
}

/** 로그아웃. 서버가 Refresh Token을 무효로 하고 쿠키를 지운다 */
export async function logout(): Promise<void> {
  await client.post('/api/auth/logout')
}

export async function fetchMe(): Promise<User> {
  const response = await client.get<User>('/api/auth/me')
  return response.data
}
