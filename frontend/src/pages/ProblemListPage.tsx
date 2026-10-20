import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchProblems } from '../api/problems'
import { getErrorMessage } from '../api/client'
import type { ProblemSummary } from '../types/problem'

export default function ProblemListPage() {
  const [problems, setProblems] = useState<ProblemSummary[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    fetchProblems()
      .then((data) => {
        if (!cancelled) setProblems(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '문제 목록을 불러오지 못했습니다.'))
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section>
      <h1>문제 목록</h1>

      {error && <p className="error">{error}</p>}
      {!error && problems === null && <p className="muted">불러오는 중...</p>}
      {problems !== null && problems.length === 0 && (
        <p className="muted">등록된 문제가 없습니다.</p>
      )}

      {problems !== null && problems.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>번호</th>
              <th>제목</th>
              <th>난이도</th>
              <th>만점</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {problems.map((problem) => (
              <tr key={problem.problemId}>
                <td>{problem.problemId}</td>
                <td>
                  <Link to={`/problems/${problem.problemId}`}>{problem.title}</Link>
                </td>
                <td>
                  <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>
                    {problem.difficulty}
                  </span>
                </td>
                <td>{problem.maxPoints} P</td>
                <td>{problem.solved ? <span className="solved">풀이 완료</span> : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
