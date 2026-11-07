import client from './client'
import type { SolutionDetailResponse, SolutionListResponse, UnlockResponse } from '../types/solution'

export async function fetchSolutions(problemId: number): Promise<SolutionListResponse> {
  const response = await client.get<SolutionListResponse>(`/api/problems/${problemId}/solutions`)
  return response.data
}

/** 포인트를 내고 못 푼 문제의 풀이를 연다 */
export async function unlockSolutions(problemId: number): Promise<UnlockResponse> {
  const response = await client.post<UnlockResponse>(`/api/problems/${problemId}/solutions/unlock`)
  return response.data
}

export async function fetchSolution(attemptId: number): Promise<SolutionDetailResponse> {
  const response = await client.get<SolutionDetailResponse>(`/api/solutions/${attemptId}`)
  return response.data
}
