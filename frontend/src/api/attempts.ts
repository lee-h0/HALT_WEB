import client, { getToken } from './client'
import type { StartAttemptResponse } from '../types/attempt'

export async function startAttempt(problemId: number): Promise<StartAttemptResponse> {
  const response = await client.post<StartAttemptResponse>(`/api/problems/${problemId}/attempts`)
  return response.data
}

export async function abandonAttempt(attemptId: number): Promise<void> {
  await client.post(`/api/attempts/${attemptId}/abandon`)
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
