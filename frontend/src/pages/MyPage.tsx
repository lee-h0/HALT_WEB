import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchDashboard, fetchRecommendation } from '../api/mypage'
import { getErrorMessage } from '../api/client'
import type { DashboardResponse, RecommendationResponse, ResumeProblem } from '../types/mypage'
import './MyPage.css'

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDuration(sec: number): string {
  const minutes = Math.floor(sec / 60)
  const seconds = sec % 60
  return minutes > 0 ? `${minutes}분 ${seconds}초` : `${seconds}초`
}

const STATUS_LABEL: Record<string, string> = {
  FAILED: '오답으로 끝난 문제',
  ABANDONED: '중간에 나간 문제',
  IN_PROGRESS: '풀던 중이던 문제',
}

export default function MyPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    fetchDashboard()
      .then((data) => {
        if (!cancelled) setDashboard(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '마이페이지를 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section className="mypage">
      <h1>마이페이지</h1>

      {error && <p className="error">{error}</p>}
      {!error && dashboard === null && <p className="muted">불러오는 중...</p>}

      {dashboard !== null && (
        <>
          <div className="stat-grid">
            <StatTile label="보유 포인트" value={`${dashboard.summary.totalPoints} P`} accent />
            <StatTile
              label="푼 문제"
              value={`${dashboard.summary.solvedCount}`}
              sub={`/ ${dashboard.summary.totalProblems}문제`}
            />
            <StatTile label="제출 횟수" value={`${dashboard.summary.submitCount}회`} />
            <StatTile
              label="정답률"
              value={dashboard.summary.accuracyPercent === null ? '-' : `${dashboard.summary.accuracyPercent}%`}
              sub={dashboard.summary.submitCount > 0 ? `${dashboard.summary.passedCount}회 정답` : undefined}
            />
          </div>

          <div className="mypage-row">
            <RecommendationCard />
            <ResumeCard resume={dashboard.resume} />
          </div>

          <div className="card">
            <h2>최근 풀이 기록</h2>
            {dashboard.recent.length === 0 ? (
              <p className="muted">
                아직 제출한 기록이 없습니다. <Link to="/problems">문제 풀러 가기</Link>
              </p>
            ) : (
              <div className="table-scroll">
                <table className="table recent-table">
                  <thead>
                    <tr>
                      <th>제출 시각</th>
                      <th>문제</th>
                      <th>결과</th>
                      <th className="num">점수</th>
                      <th className="num">소요 시간</th>
                      <th className="num">AI 사용량</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.recent.map((item) => (
                      <tr key={item.attemptId}>
                        <td className="muted nowrap">{formatDateTime(item.submittedAt)}</td>
                        <td>
                          <Link to={`/problems/${item.problemId}`}>{item.title}</Link>{' '}
                          <span className={`badge badge-${item.difficulty.toLowerCase()}`}>{item.difficulty}</span>
                        </td>
                        <td>
                          <span className={item.correct ? 'result-pass' : 'result-fail'}>
                            {item.correct ? '정답' : '오답'}
                          </span>
                        </td>
                        <td className="num">{item.score} P</td>
                        <td className="num">{formatDuration(item.elapsedSec)}</td>
                        <td className="num">{item.tokenUsagePercent}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </section>
  )
}

function StatTile({ label, value, sub, accent }: { label: string; value: string; sub?: string; accent?: boolean }) {
  return (
    <div className="card stat-tile">
      <span className="muted">{label}</span>
      <strong className={accent ? 'accent' : undefined}>
        {value}
        {sub && <span className="stat-sub"> {sub}</span>}
      </strong>
    </div>
  )
}

/** 오늘의 추천은 AI 호출로 늦게 올 수 있어 따로 불러온다 */
function RecommendationCard() {
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    fetchRecommendation()
      .then((data) => {
        if (!cancelled) setRecommendation(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '오늘의 추천을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="card feature-card">
      <div className="feature-head">
        <h2>오늘의 추천 문제</h2>
        <span className="ai-chip">AI 추천</span>
      </div>

      {error && <p className="error">{error}</p>}
      {!error && recommendation === null && <p className="muted thinking">AI가 오늘의 문제를 고르는 중...</p>}

      {recommendation !== null && recommendation.problemId === null && (
        <p className="muted">{recommendation.message}</p>
      )}

      {recommendation !== null && recommendation.problemId !== null && (
        <>
          <div className="feature-title">
            <strong>{recommendation.title}</strong>
            {recommendation.difficulty && (
              <span className={`badge badge-${recommendation.difficulty.toLowerCase()}`}>
                {recommendation.difficulty}
              </span>
            )}
            <span className="muted">만점 {recommendation.maxPoints} P</span>
          </div>
          <p className="feature-reason">{recommendation.reason}</p>
          <div className="feature-actions">
            {recommendation.solved ? (
              <span className="result-pass">✔ 오늘의 추천 문제를 풀었습니다</span>
            ) : (
              <Link to={`/problems/${recommendation.problemId}`} className="button">
                문제 보러 가기
              </Link>
            )}
          </div>
        </>
      )}
    </div>
  )
}

function ResumeCard({ resume }: { resume: ResumeProblem | null }) {
  return (
    <div className="card feature-card">
      <div className="feature-head">
        <h2>이어서 응시</h2>
      </div>

      {resume === null ? (
        <p className="muted">이어서 풀 문제가 없습니다. 못 푼 문제를 시도하면 여기에 표시됩니다.</p>
      ) : (
        <>
          <div className="feature-title">
            <strong>{resume.title}</strong>
            <span className={`badge badge-${resume.difficulty.toLowerCase()}`}>{resume.difficulty}</span>
          </div>
          <p className="feature-reason">
            {STATUS_LABEL[resume.lastStatus] ?? '최근에 시도한 문제'} · {formatDateTime(resume.lastAt)}
          </p>
          <p className="muted small">응시 기록은 초기화되어 있어 처음부터 다시 시작합니다.</p>
          <div className="feature-actions">
            <Link to={`/problems/${resume.problemId}`} className="button button-ghost">
              다시 도전하기
            </Link>
          </div>
        </>
      )}
    </div>
  )
}
