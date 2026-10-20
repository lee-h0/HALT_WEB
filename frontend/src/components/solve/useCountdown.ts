import { useEffect, useState } from 'react'

/**
 * 남은 시간(초)을 1초마다 갱신한다. 0 아래로 내려가면 음수(초과 시간)를 돌려준다.
 * startedAtMs: 응시 시작 시각을 브라우저 시계로 환산한 값
 */
export function useCountdown(startedAtMs: number | null, limitSec: number): number {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (startedAtMs === null) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [startedAtMs])

  if (startedAtMs === null) return limitSec
  return limitSec - Math.floor((now - startedAtMs) / 1000)
}

/** 1199 -> "19:59", -75 -> "+01:15" */
export function formatTime(seconds: number): string {
  const abs = Math.abs(seconds)
  const mm = String(Math.floor(abs / 60)).padStart(2, '0')
  const ss = String(abs % 60).padStart(2, '0')
  return `${seconds < 0 ? '+' : ''}${mm}:${ss}`
}
