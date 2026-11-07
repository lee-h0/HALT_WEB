import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { fetchSolutions, unlockSolutions } from '../api/solutions'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { SolutionListResponse } from '../types/solution'
import { formatDate, formatDuration } from './solutionFormat'
import './Solutions.css'

/** 문제별 다른 사람 풀이 목록. 못 푼 문제는 포인트를 내야 열린다 */
export default function SolutionListPage() {
  const { problemId } = useParams()
  const id = Number(problemId)
  const navigate = useNavigate()
  const { refreshUser } = useAuth()

  const [data, setData] = useState<SolutionListResponse | null>(null)
  const [error, setError] = useState('')
  const [unlocking, setUnlocking] = useState(false)
  const [unlockError, setUnlockError] = useState('')

  const load = useCallback(() => {
    return fetchSolutions(id)
      .then((result) => {
        setData(result)
        setError('')
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '풀이 목록을 불러오지 못했습니다.')))
  }, [id])

  useEffect(() => {
    if (Number.isInteger(id)) load()
  }, [id, load])

  async function handleUnlock() {
    if (data === null || unlocking) return
    const { unlockCost } = data.access
    const ok = window.confirm(
      `${unlockCost} P를 내고 풀이를 열까요?\n\n열람한 문제는 나중에 맞혀도 점수, 포인트, 푼 문제 수가 인정되지 않습니다.`,
    )
    if (!ok) return

    setUnlocking(true)
    setUnlockError('')
    try {
      await unlockSolutions(id)
      await load()
      refreshUser().catch(() => {
        // 상단 포인트 표시만 늦게 바뀔 뿐이다
      })
    } catch (err: unknown) {
      setUnlockError(getErrorMessage(err, '풀이를 열지 못했습니다.'))
    } finally {
      setUnlocking(false)
    }
  }

  if (!Number.isInteger(id)) return <p className="error">잘못된 문제 번호입니다.</p>
  if (error) return <p className="error">{error}</p>
  if (data === null) return <p className="muted">불러오는 중...</p>

  const { access } = data
  const notEnough = access.myPoints < access.unlockCost

  return (
    <section className="solutions">
      <p>
        <Link to={`/problems/${data.problemId}`}>← 문제로 돌아가기</Link>
      </p>
      <div className="problem-title">
        <h1>다른 사람 풀이</h1>
      </div>
      <p className="solutions-sub">
        <strong>{data.title}</strong>
        <span className={`badge badge-${data.difficulty.toLowerCase()}`}>{data.difficulty}</span>
        <span className="muted">공개된 풀이 {data.totalCount}개</span>
        {access.status === 'UNLOCKED' && <span className="unlocked-chip">포인트로 열람함</span>}
      </p>

      {!access.canView ? (
        <div className="card lock-card">
          <div className="lock-icon" aria-hidden="true">🔒</div>
          <h2>아직 풀지 않은 문제입니다</h2>
          <p>
            문제를 맞히면 무료로 볼 수 있습니다. 지금 보려면 <strong>{access.unlockCost} P</strong>가 차감됩니다.
          </p>
          <p className="lock-warning">열람한 문제는 나중에 맞혀도 점수, 포인트, 푼 문제 수가 인정되지 않습니다.</p>
          <p className="muted">보유 포인트 {access.myPoints} P</p>
          {unlockError && <p className="error">{unlockError}</p>}
          <div className="lock-actions">
            <button type="button" className="button button-ghost" onClick={() => navigate(`/problems/${data.problemId}`)}>
              먼저 풀어 보기
            </button>
            <button type="button" className="button" onClick={handleUnlock} disabled={unlocking || notEnough}>
              {unlocking ? '여는 중...' : notEnough ? '포인트가 부족합니다' : `${access.unlockCost} P 내고 열람하기`}
            </button>
          </div>
        </div>
      ) : data.solutions.length === 0 ? (
        <div className="card">
          <p className="muted">아직 공개된 풀이가 없습니다. 이 문제를 처음 맞힌 사람이 되어 보세요.</p>
        </div>
      ) : (
        <div className="card">
          <p className="muted solutions-hint">
            각 회원이 처음 맞힌 풀이입니다. 줄을 누르면 AI에게 한 지시와 제출한 코드를 볼 수 있습니다.
          </p>
          <div className="table-scroll">
            <table className="table solutions-table">
              <thead>
                <tr>
                  <th>순위</th>
                  <th>닉네임</th>
                  <th className="num">점수</th>
                  <th className="num">소요 시간</th>
                  <th className="num">AI 사용량</th>
                  <th className="num">지시</th>
                  <th className="num">푼 날</th>
                </tr>
              </thead>
              <tbody>
                {data.solutions.map((item) => (
                  <tr
                    key={item.attemptId}
                    className={`clickable${item.me ? ' me' : ''}`}
                    onClick={() => navigate(`/solutions/${item.attemptId}`)}
                  >
                    <td>{item.rank}</td>
                    <td>
                      <Link to={`/solutions/${item.attemptId}`} onClick={(e) => e.stopPropagation()}>
                        {item.nickname}
                      </Link>
                      {item.me && <span className="me-tag">나</span>}
                    </td>
                    <td className="num">{item.score} P</td>
                    <td className="num">{formatDuration(item.elapsedSec)}</td>
                    <td className="num">{item.tokenUsagePercent}%</td>
                    <td className="num">{item.promptCount}회</td>
                    <td className="num muted">{formatDate(item.solvedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}
