import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchFeedback } from '../api/feedback'
import { getErrorMessage } from '../api/client'
import type { FeedbackResponse } from '../types/feedback'
import './Feedback.css'

function formatDuration(sec: number): string {
  const minutes = Math.floor(sec / 60)
  const seconds = sec % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

/** 점수 칩 색: 4~5 좋음, 2~3 보통, 0~1 부족 */
function level(score: number | null): string {
  if (score === null) return 'none'
  if (score >= 4) return 'good'
  if (score >= 2) return 'mid'
  return 'low'
}

function ScoreChip({ score }: { score: number | null }) {
  return <span className={`score-chip ${level(score)}`}>{score === null ? '평가 안 함' : `${score} / 5`}</span>
}

/** 평가 기준표 (교수님 피드백: 구체적인 채점 기준을 응시자에게도 보여 준다) */
const RUBRIC = [
  {
    key: 'spec',
    name: '조건 명시',
    summary: '지킬 조건을 AI 지시에 얼마나 구체적으로 썼는가',
    detail: '점수 = 내림(5 × 지시에 쓴 조건 수 ÷ 전체 조건 수). AI는 문제의 조건을 모르기 때문에, 응시자가 지시에 쓴 조건만 코드에 반영됩니다.',
  },
  {
    key: 'efficiency',
    name: '지시 효율',
    summary: '적은 지시로 구체적이고 경제적으로 AI를 활용했는가',
    detail: '5: 1~2번의 구체적 지시로 해결 · 4: 대체로 구체적, 보완 지시 조금 · 3: 모호하거나 반복된 지시 일부 · 2: 대부분 모호하거나 쪼개서 요청 · 1: 문제와 거의 무관하거나 매우 비효율 · 0: 문제와 무관',
  },
  {
    key: 'accuracy',
    name: '결과 정확성',
    summary: '공개·숨김 테스트를 얼마나 통과했는가',
    detail: '정답이면 5점. 오답이면 내림(5 × 통과 ÷ 전체), 최대 4점. 숨김 테스트는 지킬 조건을 실제로 지켰는지 검사합니다.',
  },
] as const

export default function FeedbackPage() {
  const { attemptId } = useParams()
  const id = Number(attemptId)

  const [data, setData] = useState<FeedbackResponse | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    fetchFeedback(id)
      .then(setData)
      .catch((err: unknown) => setError(getErrorMessage(err, '피드백을 불러오지 못했습니다.')))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (Number.isInteger(id)) load()
  }, [id, load])

  if (!Number.isInteger(id)) return <p className="error">잘못된 응시 번호입니다.</p>

  if (loading) {
    return (
      <section className="feedback">
        <div className="card feedback-loading">
          <div className="spinner" aria-hidden="true" />
          <strong>AI가 풀이 과정을 분석하는 중입니다</strong>
          <p className="muted">지시, 제출 코드, 테스트 결과를 함께 살펴봅니다. 처음에는 10~30초 걸릴 수 있습니다.</p>
        </div>
      </section>
    )
  }

  if (error || data === null) {
    return (
      <section className="feedback">
        <div className="card feedback-loading">
          <p className="error">{error || '피드백을 불러오지 못했습니다.'}</p>
          <button type="button" className="button" onClick={load}>
            다시 시도
          </button>
        </div>
      </section>
    )
  }

  const { scores, behavior, spec, efficiency, accuracy, lesson } = data
  const scoreOf = { spec: scores.spec, efficiency: scores.efficiency, accuracy: scores.accuracy }

  return (
    <section className="feedback">
      <p>
        <Link to={`/problems/${data.problemId}`}>← 문제로 돌아가기</Link>
      </p>
      <div className="problem-title">
        <h1>풀이 피드백</h1>
      </div>
      <p className="feedback-sub">
        <strong>{data.problemTitle}</strong>
        <span className={`badge badge-${data.difficulty.toLowerCase()}`}>{data.difficulty}</span>
      </p>

      {/* 결론 */}
      <div className={`card verdict ${data.correct ? 'pass' : 'fail'}`}>
        <span className="verdict-icon" aria-hidden="true">
          {data.correct ? '✔' : '⚠'}
        </span>
        <div>
          <span className="muted small-label">이번 응시 결론</span>
          <p className="verdict-text">{data.headline}</p>
        </div>
      </div>

      {/* 행동 로그 요약 */}
      <div className="card">
        <h2 className="section-label">행동 로그 요약</h2>
        <div className="behavior-grid">
          <Metric label="결과" value={data.correct ? '정답' : '오답'} tone={data.correct ? 'pass' : 'fail'} />
          <Metric label="포인트 점수" value={`${data.points} P`} />
          <Metric label="지시" value={`${behavior.promptCount}회`} />
          <Metric label="사용한 시간" value={formatDuration(behavior.elapsedSec)} />
          <Metric label="AI 사용량" value={`${behavior.tokenUsagePercent}%`} />
          <Metric label="테스트 실행" value={`${behavior.testRunCount}회`} />
        </div>
      </div>

      {/* 검증 역량 점수 */}
      <div className="card">
        <div className="section-head">
          <h2>검증 역량 점수</h2>
          <span className="total-score">
            <strong>{scores.total}</strong> / {scores.max}
          </span>
        </div>
        <p className="muted section-desc">포인트와는 별개로, AI를 어떻게 쓰고 결과를 얼마나 검증했는지 평가합니다.</p>
        <div className="rubric-rows">
          {RUBRIC.map((item) => {
            const score = scoreOf[item.key]
            return (
              <div key={item.key} className="rubric-row">
                <div className="rubric-name">
                  <strong>{item.name}</strong>
                  <span className="muted">{item.summary}</span>
                </div>
                <div className="rubric-bar">
                  <span
                    className={`rubric-fill ${level(score)}`}
                    style={{ width: `${score === null ? 0 : (score / 5) * 100}%` }}
                  />
                </div>
                <ScoreChip score={score} />
              </div>
            )
          })}
        </div>
        <details className="rubric-detail">
          <summary>평가 기준 보기</summary>
          <ul>
            {RUBRIC.map((item) => (
              <li key={item.key}>
                <strong>{item.name}</strong> — {item.detail}
              </li>
            ))}
          </ul>
        </details>
      </div>

      {/* 조건 명시 */}
      <div className="card">
        <div className="section-head">
          <h2>조건 명시</h2>
          <ScoreChip score={spec.score} />
        </div>
        <p className="section-desc">
          지킬 조건 {spec.constraintCount}개 중 <strong>{spec.mentionedCount}개</strong>를 지시에 썼습니다.
        </p>
        {spec.feedback && <p className="feedback-text">{spec.feedback}</p>}
        <ul className="check-list">
          {spec.checks.map((check, index) => (
            <li key={index} className={check.mentioned ? 'ok' : 'miss'}>
              <span className="check-mark" aria-hidden="true">
                {check.mentioned ? '✔' : '✘'}
              </span>
              <div>
                <p>{check.constraint}</p>
                {check.mentioned && check.evidence && <p className="evidence">“{check.evidence}”</p>}
                {!check.mentioned && <p className="evidence muted">지시에 없었습니다.</p>}
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* 지시 효율 */}
      <div className="card">
        <div className="section-head">
          <h2>지시 효율</h2>
          <ScoreChip score={efficiency.score} />
        </div>
        {efficiency.feedback && <p className="feedback-text">{efficiency.feedback}</p>}
        {efficiency.reviews.length > 0 && (
          <ol className="review-list">
            {efficiency.reviews.map((review) => (
              <li key={review.index} className="review-item">
                <div className="review-head">
                  <span className="review-no">지시 {review.index}</span>
                  <span className={`verdict-chip ${review.verdict === 'GOOD' ? 'good' : 'improve'}`}>
                    {review.verdict === 'GOOD' ? '좋은 지시' : '고칠 점 있음'}
                  </span>
                </div>
                <pre className="review-prompt">{review.prompt}</pre>
                <p className="feedback-text">{review.comment}</p>
                {review.betterPrompt && (
                  <div className="better-prompt">
                    <span>이렇게 지시하면 더 좋아요</span>
                    <pre>{review.betterPrompt}</pre>
                  </div>
                )}
              </li>
            ))}
          </ol>
        )}
      </div>

      {/* 결과 정확성 */}
      <div className="card">
        <div className="section-head">
          <h2>결과 정확성</h2>
          <ScoreChip score={accuracy.score} />
        </div>
        <p className="section-desc">
          공개·숨김 테스트 {accuracy.totalCount}개 중 <strong>{accuracy.passedCount}개</strong> 통과
        </p>
        {accuracy.feedback && <p className="feedback-text">{accuracy.feedback}</p>}
        {accuracy.failedTests.length > 0 && (
          <ul className="failed-list">
            {accuracy.failedTests.map((name) => (
              <li key={name}>✘ {name}</li>
            ))}
          </ul>
        )}
      </div>

      {/* 이번 문제로 배우기 */}
      <div className="card lesson">
        <h2>📖 이번 문제로 배우기</h2>
        {lesson.title && <div className="lesson-title">💡 {lesson.title}</div>}
        <LessonBox tone="next" title="다음 응시에서 신경 쓸 점" text={lesson.nextTime} />
        <LessonBox tone="problem" title={data.correct ? '더 나아질 점' : '이 코드의 문제점'} text={lesson.codeProblem} />
        <LessonBox tone="why" title="왜 중요한가" text={lesson.whyItMatters} />
      </div>

      <div className="feedback-actions">
        <Link to={`/problems/${data.problemId}`} className="button button-ghost">
          다시 풀기
        </Link>
        {data.correct && (
          <Link to={`/problems/${data.problemId}/solutions`} className="button button-ghost">
            다른 사람 풀이
          </Link>
        )}
        <Link to="/mypage" className="button">
          마이페이지
        </Link>
      </div>
      <p className="muted model-note">AI({data.model})가 작성한 피드백입니다. 점수 계산 방식은 위의 평가 기준을 따릅니다.</p>
    </section>
  )
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: 'pass' | 'fail' }) {
  return (
    <div className="metric">
      <span className="muted">{label}</span>
      <strong className={tone}>{value}</strong>
    </div>
  )
}

function LessonBox({ tone, title, text }: { tone: 'next' | 'problem' | 'why'; title: string; text: string }) {
  if (!text) return null
  return (
    <div className={`lesson-box ${tone}`}>
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  )
}
