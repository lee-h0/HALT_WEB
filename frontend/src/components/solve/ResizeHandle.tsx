import type { PointerEvent as ReactPointerEvent } from 'react'

interface ResizeHandleProps {
  axis: 'x' | 'y'
  dragging: boolean
  onPointerDown: (event: ReactPointerEvent<HTMLDivElement>) => void
  onPointerMove: (event: ReactPointerEvent<HTMLDivElement>) => void
  onPointerUp: (event: ReactPointerEvent<HTMLDivElement>) => void
  onDoubleClick: () => void
}

/** 패널 사이의 경계선. 끌면 크기가 바뀌고, 더블클릭하면 기본 크기로 돌아간다 */
export default function ResizeHandle({ axis, dragging, ...handlers }: ResizeHandleProps) {
  return (
    <div
      className={`resize-handle ${axis === 'x' ? 'resize-x' : 'resize-y'}${dragging ? ' dragging' : ''}`}
      role="separator"
      aria-orientation={axis === 'x' ? 'vertical' : 'horizontal'}
      title="끌어서 크기 조절 · 더블클릭하면 기본 크기"
      {...handlers}
    />
  )
}
