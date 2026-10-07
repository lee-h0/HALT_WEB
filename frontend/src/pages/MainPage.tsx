import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function MainPage() {
  const { user } = useAuth()

  return (
    <section className="hero">
      <h1>AI에게 시키고, 결과를 검증하세요</h1>
      <p className="muted">
        정확한 조건을 제시하는 능력과 그럴듯한 거짓을 걸러내는 능력을 평가합니다.
      </p>

      <div className="hero-actions">
        <Link to="/problems" className="button">
          문제 풀러 가기
        </Link>
        <Link to="/ranking" className="button button-ghost">
          랭킹 보기
        </Link>
      </div>

      {user && (
        <div className="card summary">
          <div>
            <span className="muted">보유 포인트</span>
            <strong>{user.totalPoints} P</strong>
          </div>
          <div>
            <span className="muted">푼 문제</span>
            <strong>{user.solvedCount} 개</strong>
          </div>
        </div>
      )}
    </section>
  )
}
