import type { ProblemFile } from './problem'

/** SOLVED: 내가 푼 문제, UNLOCKED: 포인트를 내고 연 문제, LOCKED: 아직 못 봄 */
export type SolutionAccessStatus = 'SOLVED' | 'UNLOCKED' | 'LOCKED'

export interface SolutionAccess {
  status: SolutionAccessStatus
  canView: boolean
  /** 열람 비용 (포인트) */
  unlockCost: number
  myPoints: number
}

export interface SolutionSummary {
  rank: number
  attemptId: number
  nickname: string
  me: boolean
  score: number
  elapsedSec: number
  tokenUsagePercent: number
  promptCount: number
  solvedAt: string
}

/** 백엔드 GET /api/problems/{id}/solutions 응답 */
export interface SolutionListResponse {
  problemId: number
  title: string
  difficulty: string
  access: SolutionAccess
  /** 공개된 풀이 수 (잠겨 있어도 알려 준다) */
  totalCount: number
  /** 잠겨 있으면 빈 배열 */
  solutions: SolutionSummary[]
}

export interface PromptItem {
  prompt: string
  response: string | null
  createdAt: string
}

/** 백엔드 GET /api/solutions/{attemptId} 응답 */
export interface SolutionDetailResponse {
  attemptId: number
  problemId: number
  problemTitle: string
  difficulty: string
  nickname: string
  me: boolean
  score: number
  elapsedSec: number
  tokenUsagePercent: number
  promptCount: number
  fileChangeCount: number
  solvedAt: string
  prompts: PromptItem[]
  files: ProblemFile[]
}

/** 백엔드 POST /api/problems/{id}/solutions/unlock 응답 */
export interface UnlockResponse {
  pointsSpent: number
  totalPoints: number
}
