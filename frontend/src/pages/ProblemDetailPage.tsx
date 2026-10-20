import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchProblem } from '../api/problems'
import { getErrorMessage } from '../api/client'
import type { ProblemDetail } from '../types/problem'

/**
 * 문제 상세. 풀이를 시작하기 전에 설명과 조건, 초기 파일을 미리 본다.
 * "문제 풀기"를 누르면 풀이 화면(SolvePage)으로 이동하며 응시가 시작된다.
 */
export default function ProblemDetailPage() {
  const { problemId } = useParams()
  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    const id = Number(problemId)

    if (!Number.isInteger(id)) {
      return
    }

    fetchProblem(id)
      .then((data) => {
        if (!cancelled) setProblem(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '문제를 불러오지 못했습니다.'))
      })

    return () => {
      cancelled = true
    }
  }, [problemId])

  if (!Number.isInteger(Number(problemId))) {
    return <p className="error">잘못된 문제 번호입니다.</p>
  }
  if (error) {
    return <p className="error">{error}</p>
  }
  if (problem === null) {
    return <p className="muted">불러오는 중...</p>
  }

  return (
    <section className="problem">
      <p>
        <Link to="/problems">← 문제 목록</Link>
      </p>

      <div className="problem-title">
        <h1>{problem.title}</h1>
        <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span>
      </div>
      <p className="muted">
        제한시간 {Math.floor(problem.timeLimitSec / 60)}분 · 만점 {problem.maxPoints} P · 최하점{' '}
        {problem.minPoints} P{problem.solved && ' · 풀이 완료'}
      </p>

      <div className="start-box card">
        <div>
          <strong>풀이를 시작하면 20분 타이머가 바로 시작됩니다.</strong>
          <p className="muted">
            풀이 화면을 벗어나면 기록이 초기화되고, 다시 들어오면 처음부터 시작합니다.
          </p>
          {problem.solved && (
            <p className="solved-warning">이미 푼 문제입니다. 다시 풀 수는 있지만 포인트는 지급되지 않습니다.</p>
          )}
        </div>
        <Link to={`/problems/${problem.problemId}/solve`} className="button">
          문제 풀기
        </Link>
      </div>

      <div className="card">
        <h2>문제 설명</h2>
        <p className="prose">{problem.description}</p>

        {problem.constraintsText && (
          <>
            <h2>지킬 조건</h2>
            <p className="prose">{problem.constraintsText}</p>
          </>
        )}

        {problem.outOfScopeText && (
          <>
            <h2>안 해도 되는 것</h2>
            <p className="prose">{problem.outOfScopeText}</p>
          </>
        )}

        <h2>작업 환경</h2>
        <p>
          <code>{problem.testCommand}</code> <span className="muted">테스트</span>
        </p>
      </div>

      <h2>초기 파일</h2>
      {problem.files.map((file) => (
        <details key={file.path} className="card file">
          <summary>{file.path}</summary>
          <pre>{file.content}</pre>
        </details>
      ))}


    </section>
  )
}
