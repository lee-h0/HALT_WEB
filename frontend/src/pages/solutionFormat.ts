/** 다른 사람 풀이 화면에서 같이 쓰는 표시 형식 */
export function formatDuration(sec: number): string {
  const minutes = Math.floor(sec / 60)
  const seconds = sec % 60
  return minutes > 0 ? `${minutes}분 ${seconds}초` : `${seconds}초`
}

export function formatDate(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`
}

export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${formatDate(iso)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
