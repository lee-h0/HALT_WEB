import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../api/client'

/** 다른 화면에서 로그인 화면으로 넘어올 때 함께 전달하는 값 */
interface LoginLocationState {
  signedUp?: boolean
  from?: string
}

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // 회원가입 직후 넘어왔으면 안내 문구를 보여준다
  const state = location.state as LoginLocationState | null
  const justSignedUp = state?.signedUp === true
  // 로그인이 필요한 화면에서 넘어왔으면 그 화면으로, 아니면 메인으로 돌아간다
  const redirectTo = state?.from ?? '/'

  if (user) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(loginId, password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '로그인하지 못했습니다.'))
      setSubmitting(false)
    }
  }

  return (
    <section className="card form-card">
      <h1>로그인</h1>
      {justSignedUp && <p className="notice">회원가입이 완료됐습니다. 로그인해 주세요.</p>}

      <form onSubmit={handleSubmit}>
        <label>
          아이디
          <input
            type="text"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            autoComplete="username"
            required
            autoFocus
          />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" className="button button-block" disabled={submitting}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <p className="muted form-footer">
        계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </section>
  )
}
