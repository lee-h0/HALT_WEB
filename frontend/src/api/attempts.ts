import client, { getToken } from './client'
import type { PromptResponse, StartAttemptResponse, SubmitResult, TestRunResult } from '../types/attempt'
import type { ProblemFile } from '../types/problem'

export async function startAttempt(problemId: number): Promise<StartAttemptResponse> {
  const response = await client.post<StartAttemptResponse>(`/api/problems/${problemId}/attempts`)
  return response.data
}

export async function abandonAttempt(attemptId: number): Promise<void> {
  await client.post(`/api/attempts/${attemptId}/abandon`)
}

/** 지금 에디터의 파일 전체를 보내 공개 테스트를 실행한다 */
export async function runTests(attemptId: number, files: ProblemFile[]): Promise<TestRunResult> {
  const response = await client.post<TestRunResult>(`/api/attempts/${attemptId}/test`, { files })
  return response.data
}

/** 제출. 공개 테스트와 숨김 테스트로 채점하고, 응시가 끝난다 (파일 변경 횟수는 서버가 센다) */
export async function submitAttempt(attemptId: number, files: ProblemFile[]): Promise<SubmitResult> {
  const response = await client.post<SubmitResult>(`/api/attempts/${attemptId}/submit`, { files })
  return response.data
}

/**
 * AI에게 지시한다. 지금 에디터의 파일 전체를 함께 보내고, AI가 바꾼 파일을 돌려받는다.
 * AI 응답은 길면 수십 초 걸리므로 기본 제한시간보다 넉넉하게 기다린다.
 */
export async function sendPrompt(attemptId: number, prompt: string, files: ProblemFile[]): Promise<PromptResponse> {
  const response = await client.post<PromptResponse>(
    `/api/attempts/${attemptId}/prompt`,
    { prompt, files },
    { timeout: 120_000 },
  )
  return response.data
}

/**
 * 새로고침이나 탭 닫기처럼 페이지가 사라지는 순간에 쓴다.
 * keepalive를 켜면 페이지가 닫혀도 요청이 끝까지 전송된다.
 */
export function abandonAttemptOnUnload(attemptId: number): void {
  const token = getToken()
  fetch(`/api/attempts/${attemptId}/abandon`, {
    method: 'POST',
    keepalive: true,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  }).catch(() => {
    // 페이지가 닫히는 중이라 실패해도 할 수 있는 것이 없다. 다음 응시를 시작할 때 서버가 정리한다.
  })
}
