import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/client'

export default function SignupPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    loginId: '',
    password: '',
    passwordConfirm: '',
    nickname: '',
    email: '',
  })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/" replace />
  }

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 서로 다릅니다.')
      return
    }

    setSubmitting(true)
    try {
      await signup({
        loginId: form.loginId,
        password: form.password,
        nickname: form.nickname,
        email: form.email,
      })
      // 가입 후에는 로그인 화면으로 이동한다
      navigate('/login', { replace: true, state: { signedUp: true } })
    } catch (err) {
      setError(getErrorMessage(err, '회원가입하지 못했습니다.'))
      setSubmitting(false)
    }
  }

  return (
    <section className="card form-card">
      <h1>회원가입</h1>

      <form onSubmit={handleSubmit}>
        <label>
          아이디
          <input
            type="text"
            name="loginId"
            value={form.loginId}
            onChange={handleChange}
            autoComplete="username"
            minLength={4}
            maxLength={20}
            required
            autoFocus
          />
          <small className="muted">4~20자</small>
        </label>
        <label>
          비밀번호
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="new-password"
            minLength={8}
            maxLength={64}
            required
          />
          <small className="muted">8자 이상</small>
        </label>
        <label>
          비밀번호 확인
          <input
            type="password"
            name="passwordConfirm"
            value={form.passwordConfirm}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />
        </label>
        <label>
          닉네임
          <input
            type="text"
            name="nickname"
            value={form.nickname}
            onChange={handleChange}
            minLength={2}
            maxLength={20}
            required
          />
          <small className="muted">2~20자. 랭킹에 표시됩니다.</small>
        </label>
        <label>
          이메일
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
            maxLength={100}
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" className="button button-block" disabled={submitting}>
          {submitting ? '가입 중...' : '회원가입'}
        </button>
      </form>

      <p className="muted form-footer">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </section>
  )
}
