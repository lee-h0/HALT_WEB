import { createContext, useContext } from 'react'
import type { User } from '../types/auth'

export interface AuthContextValue {
  /** 로그인한 회원. 로그인하지 않았으면 null */
  user: User | null
  /** 새로고침 직후 로그인 상태를 복원하는 동안 true */
  loading: boolean
  login: (loginId: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

/** 어느 컴포넌트에서든 const { user, login, logout } = useAuth() 로 쓴다 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return context
}
