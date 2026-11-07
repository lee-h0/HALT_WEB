import client from './client'
import type { DashboardResponse, RecommendationResponse } from '../types/mypage'

export async function fetchDashboard(): Promise<DashboardResponse> {
  const response = await client.get<DashboardResponse>('/api/mypage/dashboard')
  return response.data
}

/**
 * 오늘의 추천. 그날 첫 요청이면 서버가 AI를 불러서 몇 초 걸릴 수 있다.
 * 개발 모드(StrictMode)에서 화면이 두 번 요청해도 AI는 한 번만 부르도록, 진행 중인 요청을 같이 쓴다.
 */
let pendingRecommendation: Promise<RecommendationResponse> | null = null

export function fetchRecommendation(): Promise<RecommendationResponse> {
  if (pendingRecommendation !== null) return pendingRecommendation

  const request = client
    .get<RecommendationResponse>('/api/mypage/recommendation', { timeout: 60_000 })
    .then((response) => response.data)
    .finally(() => {
      pendingRecommendation = null
    })
  pendingRecommendation = request
  return request
}
