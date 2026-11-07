import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { fetchSolution } from '../api/solutions'
import { getErrorMessage } from '../api/client'
import type { SolutionDetailResponse } from '../types/solution'
import { formatDate, formatDateTime, formatDuration } from './solutionFormat'
import './Solutions.css'

function languageOf(path: string): string {
  if (path.endsWith('.js') || path.endsWith('.mjs') || path.endsWith('.cjs')) return 'javascript'
  if (path.endsWith('.json')) return 'json'
  if (path.endsWith('.md')) return 'markdown'
  return 'plaintext'
}

/** 풀이 한 건: AI에게 한 지시(위)와 제출한 코드(아래) */
export default function SolutionDetailPage() {
  const { attemptId } = useParams()
  const id = Number(attemptId)

  const [data, setData] = useState<SolutionDetailResponse | null>(null)
  const [error, setError] = useState('')
  const [activePath, setActivePath] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isInteger(id)) return
    let cancelled = false
    fetchSolution(id)
      .then((result) => {
        if (cancelled) return
        setData(result)
        setActivePath(result.files.find((f) => f.path.startsWith('src/'))?.path ?? result.files[0]?.path ?? null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(getErrorMessage(err, '풀이를 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [id])

  if (!Number.isInteger(id)) return <p className="error">잘못된 풀이 번호입니다.</p>
  if (error) return <p className="error">{error}</p>
  if (data === null) return <p className="muted">불러오는 중...</p>

  const activeFile = data.files.find((file) => file.path === activePath) ?? null

  return (
    <section className="solutions">
      <p>
        <Link to={`/problems/${data.problemId}/solutions`}>← 풀이 목록</Link>
      </p>
      <div className="problem-title">
        <h1>
          {data.nickname} 님의 풀이{data.me && <span className="me-tag">나</span>}
        </h1>
      </div>
      <p className="solutions-sub">
        <strong>{data.problemTitle}</strong>
        <span className={`badge badge-${data.difficulty.toLowerCase()}`}>{data.difficulty}</span>
        <span className="muted">{formatDate(data.solvedAt)} 풀이</span>
      </p>

      <div className="detail-stats">
        <Stat label="점수" value={`${data.score} P`} accent />
        <Stat label="소요 시간" value={formatDuration(data.elapsedSec)} />
        <Stat label="AI 사용량" value={`${data.tokenUsagePercent}%`} />
        <Stat label="지시 횟수" value={`${data.promptCount}회`} />
        <Stat label="파일 변경" value={`${data.fileChangeCount}회`} />
      </div>

      <div className="card">
        <h2>AI에게 한 지시</h2>
        {data.prompts.length === 0 ? (
          <p className="muted">AI를 쓰지 않고 직접 코드를 작성했습니다.</p>
        ) : (
          <ol className="prompt-timeline">
            {data.prompts.map((item, index) => (
              <li key={index} className="prompt-item">
                <div className="prompt-head">
                  <span className="prompt-no">지시 {index + 1}</span>
                  <span className="muted">{formatDateTime(item.createdAt)}</span>
                </div>
                <p className="prompt-text">{item.prompt}</p>
                {item.response && (
                  <div className="prompt-response">
                    <span className="prompt-ai">AI</span>
                    <p>{item.response}</p>
                  </div>
                )}
              </li>
            ))}
          </ol>
        )}
      </div>

      <div className="card">
        <h2>제출한 코드</h2>
        {data.files.length === 0 ? (
          <p className="muted">저장된 코드가 없습니다.</p>
        ) : (
          <>
            <div className="code-tabs">
              {data.files.map((file) => (
                <button
                  key={file.path}
                  type="button"
                  className={file.path === activePath ? 'active' : ''}
                  onClick={() => setActivePath(file.path)}
                >
                  {file.path}
                </button>
              ))}
            </div>
            {activeFile && (
              <div className="code-view">
                <Editor
                  height="480px"
                  theme="vs-dark"
                  path={`solution-${data.attemptId}/${activeFile.path}`}
                  language={languageOf(activeFile.path)}
                  value={activeFile.content}
                  options={{
                    readOnly: true,
                    minimap: { enabled: false },
                    fontSize: 14,
                    tabSize: 2,
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                  }}
                />
              </div>
            )}
          </>
        )}
      </div>
    </section>
  )
}

function Stat({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="card detail-stat">
      <span className="muted">{label}</span>
      <strong className={accent ? 'accent' : undefined}>{value}</strong>
    </div>
  )
}
