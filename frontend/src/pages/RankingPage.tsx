import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchRanking } from '../api/ranking'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { MyRank, RankEntry, RankingResponse } from '../types/ranking'
import './RankingPage.css'

export default function RankingPage() {
  const { user, loading } = useAuth()
  const [ranking, setRanking] = useState<RankingResponse | null>(null)
  const [error, setError] = useState('')

  // 로그인 상태가 정해진 뒤에 불러온다 (로그인/로그아웃하면 내 순위가 바뀌므로 다시 불러온다)
  const userId = user?.userId ?? null
  useEffect(() => {
    if (loading) return
    let cancelled = false

    fetchRanking()
      .then((data) => {
        if (!cancelled) {
          setRanking(data)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '랭킹을 불러오지 못했습니다.'))
      })

    return () => {
      cancelled = true
    }
  }, [loading, userId])

  return (
    <section>
      <h1>랭킹</h1>

      {error && <p className="error">{error}</p>}
      {!error && ranking === null && <p className="muted">불러오는 중...</p>}

      {ranking !== null && (
        <>
          {ranking.me ? (
            <MyRankCard me={ranking.me} pointsRanked={ranking.pointsRanked} solvedRanked={ranking.solvedRanked} />
          ) : (
            <p className="muted ranking-login-hint">
              <Link to="/login">로그인</Link>하면 내 순위를 볼 수 있습니다.
            </p>
          )}

          <div className="ranking-grid">
            <RankingTable title="포인트 랭킹" entries={ranking.points} valueLabel="포인트" valueOf={(e) => `${e.totalPoints} P`} />
            <RankingTable title="푼 문제 수 랭킹" entries={ranking.solved} valueLabel="푼 문제" valueOf={(e) => `${e.solvedCount}문제`} />
          </div>
        </>
      )}
    </section>
  )
}

function MyRankCard({ me, pointsRanked, solvedRanked }: { me: MyRank; pointsRanked: number; solvedRanked: number }) {
  return (
    <div className="card my-rank">
      <div className="my-rank-name">
        <span className="muted">닉네임</span>
        <strong>{me.nickname}</strong>
      </div>
      <div className="my-rank-item">
        <span className="muted">포인트</span>
        <strong>{me.pointsRank === null ? '순위 없음' : `${me.pointsRank}위`}</strong>
        <span className="muted">
          {me.totalPoints} P{me.pointsRank !== null && ` · ${pointsRanked}명 중`}
        </span>
      </div>
      <div className="my-rank-item">
        <span className="muted">푼 문제 수</span>
        <strong>{me.solvedRank === null ? '순위 없음' : `${me.solvedRank}위`}</strong>
        <span className="muted">
          {me.solvedCount}문제{me.solvedRank !== null && ` · ${solvedRanked}명 중`}
        </span>
      </div>
    </div>
  )
}

function RankingTable({
  title,
  entries,
  valueLabel,
  valueOf,
}: {
  title: string
  entries: RankEntry[]
  valueLabel: string
  valueOf: (entry: RankEntry) => string
}) {
  return (
    <div className="card ranking-card">
      <h2>{title}</h2>
      {entries.length === 0 ? (
        <p className="muted">아직 순위에 오른 회원이 없습니다.</p>
      ) : (
        <table className="table ranking-table">
          <thead>
            <tr>
              <th>순위</th>
              <th>닉네임</th>
              <th className="num">{valueLabel}</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry, index) => (
              <tr key={`${entry.nickname}-${index}`} className={entry.me ? 'me' : undefined}>
                <td>
                  <span className={`rank-badge${entry.rank <= 3 ? ` top${entry.rank}` : ''}`}>{entry.rank}</span>
                </td>
                <td>
                  {entry.nickname}
                  {entry.me && <span className="me-tag">나</span>}
                </td>
                <td className="num">{valueOf(entry)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
