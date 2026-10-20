import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useBlocker, useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { fetchProblem } from '../api/problems'
import { abandonAttempt, abandonAttemptOnUnload, startAttempt } from '../api/attempts'
import { getErrorMessage } from '../api/client'
import FileTree from '../components/solve/FileTree'
import { formatTime, useCountdown } from '../components/solve/useCountdown'
import type { ProblemDetail } from '../types/problem'
import type { StartAttemptResponse } from '../types/attempt'

const LEAVE_MESSAGE = '문제페이지에서 벗어나면 문제 기록이 초기화 됩니다. 그래도 이동하시겠습니까?'

function languageOf(path: string): string {
  if (path.endsWith('.js') || path.endsWith('.mjs') || path.endsWith('.cjs')) return 'javascript'
  if (path.endsWith('.json')) return 'json'
  if (path.endsWith('.md')) return 'markdown'
  return 'plaintext'
}

type BottomTab = 'ai' | 'test'

export default function SolvePage() {
  const { problemId } = useParams()
  const id = Number(problemId)

  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const [attempt, setAttempt] = useState<StartAttemptResponse | null>(null)
  const [error, setError] = useState('')

  /** 경로 -> 현재 내용 (응시 중에만 브라우저 메모리에 둔다. 이탈하면 사라진다) */
  const [files, setFiles] = useState<Record<string, string>>({})
  const [initialFiles, setInitialFiles] = useState<Record<string, string>>({})
  const [openPaths, setOpenPaths] = useState<string[]>([])
  const [activePath, setActivePath] = useState<string | null>(null)
  const [bottomTab, setBottomTab] = useState<BottomTab>('ai')

  /** 응시 시작 시각을 브라우저 시계로 환산한 값 (서버가 알려준 경과 시간을 반영) */
  const [startedAtMs, setStartedAtMs] = useState<number | null>(null)
  const remainingSec = useCountdown(startedAtMs, attempt?.timeLimitSec ?? 1200)

  // 개발 모드(StrictMode)에서는 effect가 두 번 실행되므로 응시가 두 번 시작되지 않게 막는다
  const startedRef = useRef(false)

  useEffect(() => {
    if (!Number.isInteger(id) || startedRef.current) return
    startedRef.current = true

    Promise.all([fetchProblem(id), startAttempt(id)])
      .then(([problemData, attemptData]) => {
        const fileMap: Record<string, string> = {}
        attemptData.files.forEach((file) => {
          fileMap[file.path] = file.content
        })
        const firstPath = attemptData.files.find((file) => file.path.startsWith('src/'))?.path
          ?? attemptData.files[0]?.path
          ?? null

        setProblem(problemData)
        setAttempt(attemptData)
        setFiles(fileMap)
        setInitialFiles(fileMap)
        setOpenPaths(firstPath ? [firstPath] : [])
        setActivePath(firstPath)
        setStartedAtMs(Date.now() - attemptData.elapsedSec * 1000)
      })
      .catch((err: unknown) => setError(getErrorMessage(err, '응시를 시작하지 못했습니다.')))
  }, [id])

  const attemptId = attempt?.attemptId ?? null

  // 사이트 안에서 다른 화면으로 이동할 때: 경고 후 이동하면 응시를 이탈 처리한다
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      attemptId !== null && currentLocation.pathname !== nextLocation.pathname,
  )

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    if (window.confirm(LEAVE_MESSAGE)) {
      if (attemptId !== null) {
        abandonAttempt(attemptId).catch(() => {
          // 실패해도 다음 응시를 시작할 때 서버가 정리한다
        })
      }
      blocker.proceed()
    } else {
      blocker.reset()
    }
  }, [blocker, attemptId])

  // 새로고침, 탭 닫기: 브라우저 기본 경고만 띄울 수 있다 (문구는 지정할 수 없음)
  useEffect(() => {
    if (attemptId === null) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    const handlePageHide = () => abandonAttemptOnUnload(attemptId)

    window.addEventListener('beforeunload', handleBeforeUnload)
    window.addEventListener('pagehide', handlePageHide)
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
      window.removeEventListener('pagehide', handlePageHide)
    }
  }, [attemptId])

  const paths = useMemo(() => Object.keys(files).sort(), [files])
  const modifiedPaths = useMemo(
    () => new Set(paths.filter((path) => files[path] !== initialFiles[path])),
    [paths, files, initialFiles],
  )

  function openFile(path: string) {
    setOpenPaths((prev) => (prev.includes(path) ? prev : [...prev, path]))
    setActivePath(path)
  }

  function closeFile(path: string) {
    setOpenPaths((prev) => {
      const next = prev.filter((p) => p !== path)
      if (activePath === path) {
        setActivePath(next[next.length - 1] ?? null)
      }
      return next
    })
  }

  function updateActiveFile(content: string) {
    if (activePath === null) return
    setFiles((prev) => ({ ...prev, [activePath]: content }))
  }

  if (!Number.isInteger(id)) {
    return <p className="error solve-message">잘못된 문제 번호입니다.</p>
  }
  if (error) {
    return (
      <div className="solve-message">
        <p className="error">{error}</p>
        <Link to="/problems">문제 목록으로</Link>
      </div>
    )
  }
  if (problem === null || attempt === null) {
    return <p className="muted solve-message">응시를 준비하는 중...</p>
  }

  const usagePercent = Math.min(100, Math.round((attempt.tokensUsed / attempt.tokenLimit) * 100))
  const timeOver = remainingSec < 0

  return (
    <div className="solve">
      {/* 상단 바 */}
      <header className="solve-topbar">
        <div className="solve-topbar-left">
          <Link to="/problems">← 문제 목록</Link>
          <span className="divider" />
          <strong>{problem.title}</strong>
        </div>

        <div className="solve-metrics">
          <span className={`timer${timeOver ? ' over' : ''}`} title={timeOver ? '제한시간 초과: 최하점 처리' : '남은 시간'}>
            ⏱ {formatTime(remainingSec)}
          </span>
          <span className="usage">
            AI 사용량
            <span className="usage-bar">
              <span className="usage-fill" style={{ width: `${usagePercent}%` }} />
            </span>
            {usagePercent}%
          </span>
          <span className="muted">
            지시 {attempt.promptCount} · 파일 변경 {modifiedPaths.size}
          </span>
        </div>

        <div className="solve-actions">
          <button type="button" className="button button-ghost" disabled title="다음 단계에서 연결합니다">
            테스트 실행
          </button>
          <button type="button" className="button" disabled title="다음 단계에서 연결합니다">
            제출
          </button>
        </div>
      </header>

      <div className="solve-body">
        {/* 왼쪽: 문제 설명 */}
        <aside className="solve-panel solve-description">
          <div className="notice-box">AI에게 구현을 맡기고, 나온 코드를 검증해 제출하세요.</div>
          <div className="problem-title">
            <h1>{problem.title}</h1>
            <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span>
          </div>
          {!attempt.pointsAvailable && (
            <p className="error">풀이를 먼저 열람한 문제라 맞혀도 점수가 없습니다.</p>
          )}
          <p className="prose">{problem.description}</p>

          {problem.constraintsText && (
            <>
              <h2>지킬 조건</h2>
              <p className="prose">{problem.constraintsText}</p>
            </>
          )}
          <h2>작업 환경</h2>
          <p>
            <code>{problem.testCommand}</code> <span className="muted">테스트</span>
          </p>
          {problem.outOfScopeText && (
            <>
              <h2>안 해도 되는 것</h2>
              <p className="prose muted">{problem.outOfScopeText}</p>
            </>
          )}
        </aside>

        {/* 가운데: 파일 탐색기 */}
        <nav className="solve-panel solve-explorer">
          <div className="panel-title">탐색기</div>
          <FileTree paths={paths} activePath={activePath} modifiedPaths={modifiedPaths} onSelect={openFile} />
        </nav>

        {/* 오른쪽: 에디터 + 하단 패널 */}
        <section className="solve-main">
          <div className="editor-tabs">
            {openPaths.map((path) => (
              <div key={path} className={`editor-tab${path === activePath ? ' active' : ''}`}>
                <button type="button" onClick={() => setActivePath(path)}>
                  {path.split('/').pop()}
                  {modifiedPaths.has(path) && <span className="modified-dot" />}
                </button>
                <button type="button" className="tab-close" onClick={() => closeFile(path)} aria-label="닫기">
                  ×
                </button>
              </div>
            ))}
          </div>

          <div className="editor-area">
            {activePath === null ? (
              <p className="muted editor-empty">왼쪽 탐색기에서 파일을 여세요.</p>
            ) : (
              <Editor
                height="100%"
                theme="vs-dark"
                path={activePath}
                language={languageOf(activePath)}
                value={files[activePath] ?? ''}
                onChange={(value) => updateActiveFile(value ?? '')}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  tabSize: 2,
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                }}
              />
            )}
          </div>

          <div className="bottom-panel">
            <div className="bottom-tabs">
              <button
                type="button"
                className={bottomTab === 'ai' ? 'active' : ''}
                onClick={() => setBottomTab('ai')}
              >
                AI 터미널
              </button>
              <button
                type="button"
                className={bottomTab === 'test' ? 'active' : ''}
                onClick={() => setBottomTab('test')}
              >
                테스트
              </button>
            </div>
            <div className="bottom-content">
              {bottomTab === 'ai' ? (
                <p className="muted">AI 지시창은 다음 단계에서 연결합니다.</p>
              ) : (
                <p className="muted">테스트 실행 결과가 여기에 표시됩니다. (다음 단계에서 연결)</p>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
