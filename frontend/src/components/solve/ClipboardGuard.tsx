import { useEffect, useRef, useState } from 'react'
import './ClipboardGuard.css'

const MESSAGE = '문제 풀이 중에는 복사·붙여넣기를 사용할 수 없습니다.'
const VISIBLE_MS = 1500
/** 사라지는 애니메이션 길이 (CSS의 clipboard-toast-out과 맞춘다) */
const FADE_OUT_MS = 250

/** shown: 보이는 중, leaving: 스르륵 사라지는 중, hidden: 없음 */
type ToastState = 'hidden' | 'shown' | 'leaving'

/**
 * 풀이 화면에서 복사, 잘라내기, 붙여넣기를 막고 잠깐 경고를 띄운다.
 * 이 컴포넌트가 화면에 있는 동안만 동작한다 (SolvePage에서만 쓴다).
 *
 * - 단축키: Ctrl/Cmd + C, X, V 와 Ctrl+Insert, Shift+Insert, Shift+Delete
 *   한글 입력 상태에서는 event.key가 'ㅊ'처럼 바뀌므로 자판 위치(event.code)로 판단한다.
 * - 끌어다 놓기(drop)도 붙여넣기와 같아서 막는다.
 * - 이벤트: 브라우저 메뉴 등 단축키가 아닌 경로로 일어나는 copy, cut, paste도 막는다.
 * - capture 단계에서 먼저 가로채야 Monaco 에디터가 처리하기 전에 막을 수 있다.
 */
export default function ClipboardGuard() {
  const [state, setState] = useState<ToastState>('hidden')
  const timerRef = useRef<number | null>(null)

  useEffect(() => {
    function warn() {
      // 연속으로 누르면 (사라지는 중이어도) 다시 보이고 1.5초를 새로 센다
      if (timerRef.current !== null) window.clearTimeout(timerRef.current)
      setState('shown')
      timerRef.current = window.setTimeout(() => {
        setState('leaving')
        timerRef.current = window.setTimeout(() => setState('hidden'), FADE_OUT_MS)
      }, VISIBLE_MS)
    }

    function handleKeyDown(event: KeyboardEvent) {
      const mod = event.ctrlKey || event.metaKey
      const blocked =
        (mod && !event.altKey && (event.code === 'KeyC' || event.code === 'KeyX' || event.code === 'KeyV')) ||
        (event.ctrlKey && event.code === 'Insert') ||
        (event.shiftKey && (event.code === 'Insert' || event.code === 'Delete'))
      if (!blocked) return
      event.preventDefault()
      event.stopPropagation()
      warn()
    }

    // 다른 곳에서 글자를 끌어다 놓는 것도 붙여넣기와 같으므로 막는다
    function handleDrop(event: DragEvent) {
      event.preventDefault()
      event.stopPropagation()
      warn()
    }

    function handleClipboard(event: ClipboardEvent) {
      event.preventDefault()
      event.stopPropagation()
      warn()
    }

    window.addEventListener('keydown', handleKeyDown, true)
    window.addEventListener('copy', handleClipboard, true)
    window.addEventListener('cut', handleClipboard, true)
    window.addEventListener('paste', handleClipboard, true)
    window.addEventListener('drop', handleDrop, true)
    return () => {
      window.removeEventListener('keydown', handleKeyDown, true)
      window.removeEventListener('copy', handleClipboard, true)
      window.removeEventListener('cut', handleClipboard, true)
      window.removeEventListener('paste', handleClipboard, true)
      window.removeEventListener('drop', handleDrop, true)
      if (timerRef.current !== null) window.clearTimeout(timerRef.current)
    }
  }, [])

  if (state === 'hidden') return null
  return (
    <div className={`clipboard-toast${state === 'leaving' ? ' leaving' : ''}`} role="alert">
      {MESSAGE}
    </div>
  )
}
