import client from './client'
import type { ProblemDetail, ProblemSummary } from '../types/problem'

export async function fetchProblems(): Promise<ProblemSummary[]> {
  const response = await client.get<ProblemSummary[]>('/api/problems')
  return response.data
}

export async function fetchProblem(problemId: number): Promise<ProblemDetail> {
  const response = await client.get<ProblemDetail>(`/api/problems/${problemId}`)
  return response.data
}
