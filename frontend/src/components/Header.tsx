import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Header() {
  const { user, loading, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="header">
      <Link to="/" className="logo">
        HALT
      </Link>

      <nav className="nav">
        <NavLink to="/problems">문제풀이</NavLink>
        <NavLink to="/ranking">랭킹</NavLink>
        {user && <NavLink to="/mypage">마이페이지</NavLink>}
      </nav>

      <div className="header-right">
        {loading ? null : user ? (
          <>
            <span className="nickname">{user.nickname} 님</span>
            <span className="points">{user.totalPoints} P</span>
            <button type="button" className="button button-ghost" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="button button-ghost">
              로그인
            </Link>
            <Link to="/signup" className="button">
              회원가입
            </Link>
          </>
        )}
      </div>
    </header>
  )
}
