import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './AuthContext'
import type { User } from '../types/auth'
import * as authApi from '../api/auth'
import { clearToken, getToken, refreshAccessToken, setToken } from '../api/client'

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // 새로고침 직후 로그인 상태를 복원하는 동안 true
  const [loading, setLoading] = useState(true)

  // 처음 켜질 때: Access Token은 메모리에만 있어서 새로고침하면 사라진다.
  // HttpOnly 쿠키의 Refresh Token으로 다시 받아 로그인 상태를 복원한다.
  useEffect(() => {
    let cancelled = false

    async function restore() {
      try {
        const token = await refreshAccessToken()
        if (token) {
          const me = await authApi.fetchMe()
          if (!cancelled) setUser(me)
        }
      } catch {
        clearToken()
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    restore()
    return () => {
      cancelled = true
    }
  }, [])

  // api/client.ts가 재발급까지 실패하면 보내는 신호
  useEffect(() => {
    const handleLogout = () => setUser(null)
    window.addEventListener('auth:logout', handleLogout)
    return () => window.removeEventListener('auth:logout', handleLogout)
  }, [])

  const login = useCallback(async (loginId: string, password: string) => {
    const result = await authApi.login({ loginId, password })
    setToken(result.accessToken)
    const me = await authApi.fetchMe()
    setUser(me)
  }, [])

  const logout = useCallback(() => {
    // 서버 쪽 Refresh Token 무효화는 기다리지 않는다 (실패해도 화면은 로그아웃)
    authApi.logout().catch(() => {
      // 쿠키가 이미 없거나 서버가 꺼져 있어도 무시
    })
    clearToken()
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    if (!getToken()) return
    setUser(await authApi.fetchMe())
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, logout, refreshUser }),
    [user, loading, login, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
