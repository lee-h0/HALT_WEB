import client from './client'
import type { LoginRequest, LoginResponse, SignupRequest, User } from '../types/auth'

export async function signup(request: SignupRequest): Promise<void> {
  await client.post('/api/auth/signup', request)
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/api/auth/login', request)
  return response.data
}

export async function fetchMe(): Promise<User> {
  const response = await client.get<User>('/api/auth/me')
  return response.data
}
