import type { ProblemFile } from './problem'

/** 백엔드 POST /api/problems/{id}/attempts 응답 */
export interface StartAttemptResponse {
  attemptId: number
  problemId: number
  /** 서버 기준 경과 시간(초) */
  elapsedSec: number
  timeLimitSec: number
  tokenLimit: number
  tokensUsed: number
  promptCount: number
  fileChangeCount: number
  /** false면 풀이를 먼저 열람한 문제라 맞혀도 점수가 없다 */
  pointsAvailable: boolean
  files: ProblemFile[]
}

/** 백엔드 POST /api/attempts/{id}/test 응답 */
export interface TestRunResult {
  passed: boolean
  passedCount: number
  totalCount: number
  timedOut: boolean
  durationMs: number
  output: string
}

/** 백엔드 POST /api/attempts/{id}/submit 응답 */
export interface SubmitResult {
  correct: boolean
  score: number
  /** 이번 제출로 실제 늘어난 포인트 (처음 맞힌 경우에만 0보다 크다) */
  pointsAwarded: number
  totalPoints: number
  firstSolve: boolean
  newBest: boolean
  pointsAvailable: boolean
  /** AI 사용량 100% 또는 제한시간 초과로 최하점 처리됨 */
  overLimit: boolean
  passedCount: number
  totalCount: number
  failedTests: string[]
  message: string
  elapsedSec: number
  tokenUsagePercent: number
  durationMs: number
}

/** 백엔드 POST /api/attempts/{id}/prompt 응답 */
export interface PromptResponse {
  /** AI가 한 일을 짧게 설명한 답변 */
  reply: string
  /** AI가 바꾸거나 새로 만든 파일 (전체 내용) */
  changedFiles: ProblemFile[]
  inputTokens: number
  outputTokens: number
  /** 이번 응시에서 지금까지 쓴 토큰 합계 */
  tokensUsed: number
  tokenUsagePercent: number
  promptCount: number
  fileChangeCount: number
}

/** AI 터미널에 보이는 대화 한 줄 */
export interface ChatMessage {
  id: number
  role: 'user' | 'ai' | 'error'
  text: string
  /** AI 답변일 때 바뀐 파일 경로 */
  changedPaths?: string[]
  /** AI 답변일 때 이번 호출에 쓴 토큰 */
  tokens?: number
}
