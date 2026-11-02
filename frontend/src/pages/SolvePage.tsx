import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useBlocker, useNavigate, useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { fetchProblem } from '../api/problems'
import { abandonAttempt, abandonAttemptOnUnload, runTests, sendPrompt, startAttempt, submitAttempt } from '../api/attempts'
import { useAuth } from '../auth/AuthContext'
import { getErrorMessage } from '../api/client'
import FileTree from '../components/solve/FileTree'
import { formatTime, useCountdown } from '../components/solve/useCountdown'
import { useResizable } from '../components/solve/useResizable'
import ResizeHandle from '../components/solve/ResizeHandle'
import AiTerminal from '../components/solve/AiTerminal'
import ClipboardGuard from '../components/solve/ClipboardGuard'
import '../components/solve/TestHistory.css'
import type { ProblemDetail } from '../types/problem'
import type { ChatMessage, StartAttemptResponse, SubmitResult, TestRunResult } from '../types/attempt'

const LEAVE_MESSAGE = '문제페이지에서 벗어나면 문제 기록이 초기화 됩니다. 그래도 이동하시겠습니까?'

function languageOf(path: string): string {
  if (path.endsWith('.js') || path.endsWith('.mjs') || path.endsWith('.cjs')) return 'javascript'
  if (path.endsWith('.json')) return 'json'
  if (path.endsWith('.md')) return 'markdown'
  return 'plaintext'
}

type BottomTab = 'ai' | 'test'

/** 하단 패널은 에디터가 최소 150px은 보이도록 제한한다 (상단 바와 탭 높이 약 100px 제외) */
const maxBottomHeight = () => window.innerHeight - 250

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

  const [testRunning, setTestRunning] = useState(false)

  const [submitting, setSubmitting] = useState(false)
  const [submitResult, setSubmitResult] = useState<SubmitResult | null>(null)
  const [submitError, setSubmitError] = useState('')
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  /** 테스트 실행 기록. 최신이 맨 앞 (응시 중에만 메모리에 둔다) */
  const [testRuns, setTestRuns] = useState<TestRun[]>([])

  // AI 터미널 (대화 기록은 응시 중에만 메모리에 둔다)
  const [aiMessages, setAiMessages] = useState<ChatMessage[]>([])
  const [aiDraft, setAiDraft] = useState('')
  const [aiPending, setAiPending] = useState(false)
  const messageIdRef = useRef(0)

  /** 응시 시작 시각을 브라우저 시계로 환산한 값 (서버가 알려준 경과 시간을 반영) */
  const [startedAtMs, setStartedAtMs] = useState<number | null>(null)
  const remainingSec = useCountdown(startedAtMs, attempt?.timeLimitSec ?? 1200)

  // 패널 크기 (경계선을 끌어 조절, 브라우저에 기억됨)
  const descriptionPanel = useResizable({
    storageKey: 'solve.descriptionWidth', initial: 420, min: 260, max: 700, axis: 'x',
  })
  const explorerPanel = useResizable({
    storageKey: 'solve.explorerWidth', initial: 300, min: 160, max: 500, axis: 'x',
  })
  const bottomPanel = useResizable({
    storageKey: 'solve.bottomHeight', initial: 450, min: 120, max: maxBottomHeight, axis: 'y', reverse: true,
  })

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

  // 제출이 끝나면 응시가 종료되므로 이탈 경고와 이탈 처리를 하지 않는다
  const finished = submitResult !== null
  const attemptId = attempt !== null && !finished ? attempt.attemptId : null

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

  async function handleSubmit() {
    if (attempt === null || submitting || finished || aiPending) return
    if (!window.confirm('제출하면 응시가 끝나고 숨김 테스트까지 채점합니다. 제출할까요?')) return

    setSubmitting(true)
    setSubmitError('')
    try {
      const fileList = Object.entries(files).map(([path, content]) => ({ path, content }))
      const result = await submitAttempt(attempt.attemptId, fileList)
      setSubmitResult(result)
      refreshUser().catch(() => {
        // 상단의 포인트 표시만 늦게 바뀔 뿐이라 무시한다
      })
    } catch (err: unknown) {
      setSubmitError(getErrorMessage(err, '제출하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRunTests() {
    if (attempt === null || testRunning) return
    setBottomTab('test')
    setTestRunning(true)
    const runNumber = testRuns.length + 1
    const ranAt = new Date()
    try {
      const fileList = Object.entries(files).map(([path, content]) => ({ path, content }))
      const result = await runTests(attempt.attemptId, fileList)
      setTestRuns((prev) => [{ runNumber, ranAt, result }, ...prev])
    } catch (err: unknown) {
      setTestRuns((prev) => [
        { runNumber, ranAt, error: getErrorMessage(err, '테스트를 실행하지 못했습니다.') },
        ...prev,
      ])
    } finally {
      setTestRunning(false)
    }
  }

  function addMessage(message: Omit<ChatMessage, 'id'>) {
    messageIdRef.current += 1
    const id = messageIdRef.current
    setAiMessages((prev) => [...prev, { ...message, id }])
  }

  async function handleSendPrompt() {
    const prompt = aiDraft.trim()
    if (attempt === null || aiPending || finished || prompt === '') return

    addMessage({ role: 'user', text: prompt })
    setAiDraft('')
    setAiPending(true)
    try {
      const fileList = Object.entries(files).map(([path, content]) => ({ path, content }))
      const response = await sendPrompt(attempt.attemptId, prompt, fileList)

      // AI가 바꾼 파일을 작업 폴더에 바로 반영한다 (새 파일은 추가)
      if (response.changedFiles.length > 0) {
        setFiles((prev) => {
          const next = { ...prev }
          response.changedFiles.forEach((file) => {
            next[file.path] = file.content
          })
          return next
        })
        const changedPaths = response.changedFiles.map((file) => file.path)
        setOpenPaths((prev) => [...prev, ...changedPaths.filter((path) => !prev.includes(path))])
        setActivePath(changedPaths[0])
      }

      setAttempt((prev) =>
        prev === null
          ? prev
          : {
              ...prev,
              tokensUsed: response.tokensUsed,
              promptCount: response.promptCount,
              fileChangeCount: response.fileChangeCount,
            },
      )
      addMessage({
        role: 'ai',
        text: response.reply,
        changedPaths: response.changedFiles.map((file) => file.path),
        tokens: response.inputTokens + response.outputTokens,
      })
    } catch (err: unknown) {
      addMessage({ role: 'error', text: getErrorMessage(err, 'AI 응답을 받지 못했습니다.') })
      // 보내지 못한 지시는 다시 고쳐 보낼 수 있게 입력창에 되돌린다
      setAiDraft((current) => (current === '' ? prompt : current))
    } finally {
      setAiPending(false)
    }
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
      <ClipboardGuard />
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
            지시 {attempt.promptCount} · 파일 변경 {attempt.fileChangeCount}
          </span>
        </div>

        <div className="solve-actions">
          <button type="button" className="button button-ghost" onClick={handleRunTests} disabled={testRunning}>
            {testRunning ? '테스트 중...' : '테스트 실행'}
          </button>
          <button
            type="button"
            className="button"
            onClick={handleSubmit}
            disabled={submitting || finished || aiPending}
            title={aiPending ? 'AI 작업이 끝난 뒤 제출할 수 있습니다' : undefined}
          >
            {submitting ? '채점 중...' : finished ? '제출 완료' : '제출'}
          </button>
        </div>
      </header>

      <div className="solve-body">
        {/* 왼쪽: 문제 설명 */}
        <aside className="solve-panel solve-description" style={{ width: descriptionPanel.size }}>
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

        <ResizeHandle axis="x" dragging={descriptionPanel.dragging} {...descriptionPanel.handleProps} />

        {/* 가운데: 파일 탐색기 */}
        <nav className="solve-panel solve-explorer" style={{ width: explorerPanel.size }}>
          <div className="panel-title">탐색기</div>
          <FileTree paths={paths} activePath={activePath} modifiedPaths={modifiedPaths} onSelect={openFile} />
        </nav>

        <ResizeHandle axis="x" dragging={explorerPanel.dragging} {...explorerPanel.handleProps} />

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
                  // AI가 파일을 고치는 동안 직접 수정한 내용이 덮어써지지 않도록 잠근다
                  readOnly: aiPending,
                  // 오른쪽 클릭 메뉴(복사·붙여넣기 포함)와 에디터 안 끌어다 놓기를 끈다
                  contextmenu: false,
                  dragAndDrop: false,
                }}
              />
            )}
          </div>

          <ResizeHandle axis="y" dragging={bottomPanel.dragging} {...bottomPanel.handleProps} />

          <div className="bottom-panel" style={{ height: bottomPanel.size }}>
            <div className="bottom-tabs">
              <button
                type="button"
                className={bottomTab === 'ai' ? 'active' : ''}
                onClick={() => setBottomTab('ai')}
              >
                AI 터미널{aiPending && ' ●'}
              </button>
              <button
                type="button"
                className={bottomTab === 'test' ? 'active' : ''}
                onClick={() => setBottomTab('test')}
              >
                테스트
              </button>
            </div>
            <div className={`bottom-content${bottomTab === 'ai' ? ' ai-mode' : ''}`}>
              {bottomTab === 'ai' ? (
                <AiTerminal
                  messages={aiMessages}
                  draft={aiDraft}
                  onDraftChange={setAiDraft}
                  onSend={handleSendPrompt}
                  pending={aiPending}
                  disabled={finished}
                  usagePercent={usagePercent}
                  onOpenFile={openFile}
                />
              ) : (
                <TestResultView running={testRunning} runs={testRuns} />
              )}
            </div>
          </div>
        </section>
      </div>

      {submitError && (
        <div className="submit-error" role="alert">
          {submitError}
          <button type="button" onClick={() => setSubmitError('')} aria-label="닫기">
            ×
          </button>
        </div>
      )}

      {submitResult && (
        <SubmitResultDialog
          result={submitResult}
          problemTitle={problem.title}
          onClose={() => navigate('/problems')}
          onRetry={() => navigate(`/problems/${problem.problemId}`)}
        />
      )}
    </div>
  )
}

/** 테스트 실행 한 번의 기록 */
interface TestRun {
  runNumber: number
  ranAt: Date
  result?: TestRunResult
  error?: string
}

/** node --test 출력(TAP)에서 실패한 테스트 이름만 뽑는다 */
function failedTestNames(output: string): string[] {
  const names = output
    .split('\n')
    .map((line) => /^not ok \d+ - (.+)$/.exec(line.trim())?.[1])
    .filter((name): name is string => name !== undefined)
  return [...new Set(names)]
}

function formatClock(date: Date): string {
  return date.toLocaleTimeString('ko-KR', { hour12: false })
}

/** 하단 "테스트" 탭의 내용. 최신 결과가 맨 위, 이전 결과는 아래로 쌓인다 */
function TestResultView({ running, runs }: { running: boolean; runs: TestRun[] }) {
  if (!running && runs.length === 0) {
    return <p className="muted">상단의 "테스트 실행"을 누르면 공개 테스트 결과가 여기에 표시됩니다.</p>
  }

  return (
    <div className="test-history">
      {running && <p className="muted test-run">테스트를 실행하는 중입니다...</p>}
      {runs.map((run, index) => (
        <TestRunItem key={run.runNumber} run={run} latest={index === 0 && !running} />
      ))}
    </div>
  )
}

function TestRunItem({ run, latest }: { run: TestRun; latest: boolean }) {
  const { result, error } = run
  const failed = result && !result.passed ? failedTestNames(result.output) : []

  return (
    <div className={`test-run${latest ? '' : ' old'}`}>
      <span className="test-run-meta">
        #{run.runNumber} {formatClock(run.ranAt)}
      </span>
      {error !== undefined && <span className="error">{error}</span>}
      {result && (
        <div className="test-run-body">
          <span className={result.passed ? 'test-summary pass' : 'test-summary fail'}>
            {result.passed ? '✔ 공개 테스트 통과' : '✘ 공개 테스트 실패'} · {result.passedCount}/{result.totalCount} 통과 ·{' '}
            {(result.durationMs / 1000).toFixed(1)}초{result.timedOut && ' · 제한시간 초과'}
          </span>
          {!result.passed && result.totalCount === 0 && !result.timedOut && (
            <span className="test-failed">코드 오류로 테스트가 실행되지 않았습니다.</span>
          )}
          {failed.map((name) => (
            <span key={name} className="test-failed">
              ✘ {name}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

/** 제출 결과 창 */
function SubmitResultDialog({
  result,
  problemTitle,
  onClose,
  onRetry,
}: {
  result: SubmitResult
  problemTitle: string
  onClose: () => void
  onRetry: () => void
}) {
  const minutes = Math.floor(result.elapsedSec / 60)
  const seconds = result.elapsedSec % 60

  return (
    <div className="dialog-backdrop">
      <div className="dialog" role="dialog" aria-modal="true">
        <p className="muted">{problemTitle}</p>
        <h1 className={result.correct ? 'result-title pass' : 'result-title fail'}>
          {result.correct ? '정답입니다' : '오답입니다'}
        </h1>
        {!result.correct && <p>{result.message}</p>}

        <div className="result-grid">
          <div>
            <span className="muted">테스트</span>
            <strong>
              {result.passedCount} / {result.totalCount}
            </strong>
          </div>
          <div>
            <span className="muted">점수</span>
            <strong>{result.score} P</strong>
          </div>
          <div>
            <span className="muted">소요 시간</span>
            <strong>
              {minutes}분 {seconds}초
            </strong>
          </div>
          <div>
            <span className="muted">AI 사용량</span>
            <strong>{result.tokenUsagePercent}%</strong>
          </div>
        </div>

        {result.correct && (
          <p className="result-points">
            {!result.pointsAvailable
              ? '풀이를 먼저 열람한 문제라 점수가 반영되지 않습니다.'
              : result.firstSolve
                ? `처음 푼 문제입니다. +${result.pointsAwarded} P (보유 ${result.totalPoints} P)`
                : '이미 푼 문제입니다. 포인트는 처음 맞혔을 때만 지급됩니다.'}
          </p>
        )}
        {result.overLimit && (
          <p className="muted">제한시간 또는 AI 사용량 한도를 넘겨 최하점으로 처리되었습니다.</p>
        )}

        {result.failedTests.length > 0 && (
          <div className="failed-tests">
            <strong>통과하지 못한 테스트</strong>
            <ul>
              {result.failedTests.map((name) => (
                <li key={name}>{name}</li>
              ))}
            </ul>
            <p className="muted">공개 테스트를 통과해도 지킬 조건을 어기면 숨김 테스트에서 실패합니다.</p>
          </div>
        )}

        <div className="dialog-actions">
          <button type="button" className="button button-ghost" onClick={onRetry}>
            다시 풀기
          </button>
          <button type="button" className="button" onClick={onClose}>
            문제 목록
          </button>
        </div>
      </div>
    </div>
  )
}
