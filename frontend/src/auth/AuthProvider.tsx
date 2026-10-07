import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './AuthContext'
import type { User } from '../types/auth'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken } from '../api/client'

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // 새로고침 직후 토큰으로 회원 정보를 다시 불러오는 동안 true
  const [loading, setLoading] = useState(true)

  // 처음 켜질 때: 저장된 토큰이 있으면 내 정보를 불러와 로그인 상태를 복원한다
  useEffect(() => {
    let cancelled = false

    async function restore() {
      if (!getToken()) {
        setLoading(false)
        return
      }
      try {
        const me = await authApi.fetchMe()
        if (!cancelled) setUser(me)
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

  // api/client.js가 401을 받으면 보내는 신호
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
    clearToken()
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, logout }),
    [user, loading, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
