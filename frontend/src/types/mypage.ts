/** 백엔드 GET /api/mypage/dashboard 응답 */
export interface DashboardSummary {
  nickname: string
  totalPoints: number
  solvedCount: number
  /** 공개 중인 전체 문제 수 */
  totalProblems: number
  submitCount: number
  passedCount: number
  /** 제출이 없으면 null */
  accuracyPercent: number | null
}

export interface ResumeProblem {
  problemId: number
  title: string
  difficulty: string
  /** FAILED, ABANDONED, IN_PROGRESS */
  lastStatus: string
  /** ISO 날짜 문자열 */
  lastAt: string
}

export interface RecentSubmission {
  attemptId: number
  problemId: number
  title: string
  difficulty: string
  correct: boolean
  score: number
  elapsedSec: number
  tokenUsagePercent: number
  submittedAt: string
}

export interface DashboardResponse {
  summary: DashboardSummary
  /** 못 푼 문제를 시도한 적이 없으면 null */
  resume: ResumeProblem | null
  recent: RecentSubmission[]
}

/** 백엔드 GET /api/mypage/recommendation 응답 */
export interface RecommendationResponse {
  /** 추천할 문제가 없으면 null (message에 안내) */
  problemId: number | null
  title: string | null
  difficulty: string | null
  maxPoints: number
  reason: string | null
  /** 추천받은 문제를 이미 풀었으면 true */
  solved: boolean
  message: string | null
}
