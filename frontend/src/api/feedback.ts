import client from './client'
import type { FeedbackResponse } from '../types/feedback'

const pending = new Map<number, Promise<FeedbackResponse>>()

/**
 * 풀이 피드백. 처음 볼 때는 서버가 AI로 분석해서 10~30초 걸릴 수 있다.
 * 개발 모드(StrictMode)에서 화면이 두 번 요청해도 AI는 한 번만 부르도록 진행 중인 요청을 같이 쓴다.
 */
export function fetchFeedback(attemptId: number): Promise<FeedbackResponse> {
  const existing = pending.get(attemptId)
  if (existing) return existing

  const request = client
    .get<FeedbackResponse>(`/api/attempts/${attemptId}/feedback`, { timeout: 120_000 })
    .then((response) => response.data)
    .finally(() => {
      pending.delete(attemptId)
    })
  pending.set(attemptId, request)
  return request
}
