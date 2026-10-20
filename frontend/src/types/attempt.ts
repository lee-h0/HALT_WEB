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
