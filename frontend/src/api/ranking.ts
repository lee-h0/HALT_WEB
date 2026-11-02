import client from './client'
import type { RankingResponse } from '../types/ranking'

/** 랭킹. 로그인했으면 토큰이 함께 가서 내 순위도 온다 */
export async function fetchRanking(): Promise<RankingResponse> {
  const response = await client.get<RankingResponse>('/api/ranking')
  return response.data
}
