import { useCallback, useEffect, useRef, useState } from 'react'
import type { PointerEvent as ReactPointerEvent } from 'react'

interface ResizableOptions {
  /** 크기를 브라우저에 기억해 둘 이름. 다음에 들어와도 같은 크기로 열린다 */
  storageKey: string
  /** 기본 크기(px). 경계선을 더블클릭하면 이 크기로 돌아간다 */
  initial: number
  min: number
  /** 최대 크기. 창 크기에 따라 달라지면 함수로 넘긴다 */
  max: number | (() => number)
  /** x: 좌우로 끌기(넓이), y: 위아래로 끌기(높이) */
  axis: 'x' | 'y'
  /** true면 반대 방향으로 끌 때 커진다 (아래쪽 패널을 위로 끌어 키울 때) */
  reverse?: boolean
}

function readSaved(key: string): number | null {
  try {
    const value = Number(localStorage.getItem(key))
    return Number.isFinite(value) && value > 0 ? value : null
  } catch {
    return null
  }
}

function save(key: string, value: number) {
  try {
    localStorage.setItem(key, String(Math.round(value)))
  } catch {
    // 저장소를 쓸 수 없는 환경이면 기억하지 않을 뿐 동작에는 문제없다
  }
}

/** VS Code처럼 경계선을 마우스로 끌어 패널 크기를 바꾼다 */
export function useResizable({ storageKey, initial, min, max, axis, reverse = false }: ResizableOptions) {
  const clamp = useCallback(
    (value: number) => {
      const upper = typeof max === 'function' ? max() : max
      return Math.min(Math.max(value, min), Math.max(min, upper))
    },
    [min, max],
  )

  const [size, setSize] = useState(() => clamp(readSaved(storageKey) ?? initial))
  const [dragging, setDragging] = useState(false)
  const dragStart = useRef<{ pointer: number; size: number } | null>(null)

  const onPointerDown = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      dragStart.current = { pointer: axis === 'x' ? event.clientX : event.clientY, size }
      setDragging(true)
    },
    [axis, size],
  )

  const onPointerMove = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      const start = dragStart.current
      if (start === null) return
      const pointer = axis === 'x' ? event.clientX : event.clientY
      const delta = reverse ? start.pointer - pointer : pointer - start.pointer
      setSize(clamp(start.size + delta))
    },
    [axis, reverse, clamp],
  )

  const onPointerUp = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      if (dragStart.current === null) return
      dragStart.current = null
      setDragging(false)
      event.currentTarget.releasePointerCapture(event.pointerId)
    },
    [],
  )

  const onDoubleClick = useCallback(() => setSize(clamp(initial)), [clamp, initial])

  // 끄는 동안 글자가 선택되지 않게 하고, 커서 모양을 유지한다
  useEffect(() => {
    if (!dragging) return
    const { cursor, userSelect } = document.body.style
    document.body.style.cursor = axis === 'x' ? 'col-resize' : 'row-resize'
    document.body.style.userSelect = 'none'
    return () => {
      document.body.style.cursor = cursor
      document.body.style.userSelect = userSelect
    }
  }, [dragging, axis])

  // 크기가 정해지면 기억해 둔다
  useEffect(() => {
    if (!dragging) save(storageKey, size)
  }, [dragging, size, storageKey])

  // 창 크기가 줄어 최대 크기를 넘으면 맞춰 줄인다
  useEffect(() => {
    const handleResize = () => setSize((current) => clamp(current))
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [clamp])

  return {
    size,
    dragging,
    handleProps: { onPointerDown, onPointerMove, onPointerUp, onDoubleClick },
  }
}
