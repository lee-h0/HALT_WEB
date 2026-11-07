/** 백엔드 GET /api/attempts/{id}/feedback 응답 (검증 역량 평가) */
export interface FeedbackBehavior {
  promptCount: number
  elapsedSec: number
  tokenUsagePercent: number
  /** 제출 전 "테스트 실행" 횟수 */
  testRunCount: number
}

/** 항목별 0~5점. AI를 쓰지 않았으면 spec, efficiency는 null */
export interface FeedbackScores {
  spec: number | null
  efficiency: number | null
  accuracy: number
  total: number
  max: number
}

export interface ConstraintCheck {
  constraint: string
  mentioned: boolean
  /** 근거가 된 지시 문장 */
  evidence: string
}

export interface PromptReview {
  index: number
  prompt: string
  verdict: 'GOOD' | 'IMPROVE'
  comment: string
  betterPrompt: string
}

export interface FeedbackResponse {
  attemptId: number
  problemId: number
  problemTitle: string
  difficulty: string
  correct: boolean
  points: number
  headline: string
  behavior: FeedbackBehavior
  scores: FeedbackScores
  spec: {
    score: number | null
    mentionedCount: number
    constraintCount: number
    checks: ConstraintCheck[]
    feedback: string
  }
  efficiency: {
    score: number | null
    feedback: string
    reviews: PromptReview[]
  }
  accuracy: {
    score: number
    passedCount: number
    totalCount: number
    failedTests: string[]
    feedback: string
  }
  lesson: {
    title: string
    nextTime: string
    codeProblem: string
    whyItMatters: string
  }
  model: string
  createdAt: string
}
