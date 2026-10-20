export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** 백엔드 GET /api/problems 응답의 한 줄 */
export interface ProblemSummary {
  problemId: number
  title: string
  difficulty: Difficulty
  maxPoints: number
  solved: boolean
}

export interface ProblemFile {
  path: string
  content: string
}

/** 백엔드 GET /api/problems/{id} 응답 */
export interface ProblemDetail {
  problemId: number
  title: string
  difficulty: Difficulty
  description: string
  constraintsText: string | null
  outOfScopeText: string | null
  testCommand: string
  timeLimitSec: number
  tokenLimit: number
  maxPoints: number
  minPoints: number
  solved: boolean
  files: ProblemFile[]
}
