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
