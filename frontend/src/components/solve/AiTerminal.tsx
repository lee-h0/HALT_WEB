import { useEffect, useRef } from 'react'
import type { KeyboardEvent } from 'react'
import type { ChatMessage } from '../../types/attempt'
import './AiTerminal.css'

interface AiTerminalProps {
  messages: ChatMessage[]
  draft: string
  onDraftChange: (value: string) => void
  onSend: () => void
  pending: boolean
  disabled: boolean
  usagePercent: number
  onOpenFile: (path: string) => void
}

/**
 * 하단 "AI 터미널" 탭.
 * 대화 기록과 입력 중인 지시는 SolvePage가 가지고 있어서, 탭을 바꿔도 사라지지 않는다.
 */
export default function AiTerminal({
  messages,
  draft,
  onDraftChange,
  onSend,
  pending,
  disabled,
  usagePercent,
  onOpenFile,
}: AiTerminalProps) {
  const logRef = useRef<HTMLDivElement>(null)

  // 새 메시지가 오면 맨 아래로 스크롤
  useEffect(() => {
    const log = logRef.current
    if (log) log.scrollTop = log.scrollHeight
  }, [messages, pending])

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter는 전송, Shift+Enter는 줄바꿈.
    // 한글 입력 중(조합 중)에 누른 Enter는 글자 확정용이라 무시한다 (안 하면 마지막 글자가 한 번 더 전송됨)
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return
    event.preventDefault()
    onSend()
  }

  const canSend = !pending && !disabled && draft.trim().length > 0

  return (
    <div className="ai-terminal">
      <div className="ai-log" ref={logRef}>
        {messages.length === 0 && (
          <div className="ai-empty muted">
            <p>AI에게 구현할 내용을 지시하세요. AI가 작업 폴더의 파일을 직접 고칩니다.</p>
            <p>
              AI는 문제 설명과 지킬 조건을 모릅니다. 지켜야 할 조건은 지시에 직접 써야 합니다.
            </p>
          </div>
        )}

        {messages.map((message) => (
          <div key={message.id} className={`ai-message ai-${message.role}`}>
            <span className="ai-role">
              {message.role === 'user' ? '나' : message.role === 'ai' ? 'AI' : '오류'}
            </span>
            <div className="ai-body">
              <p className="ai-text">{message.text}</p>
              {message.changedPaths && message.changedPaths.length > 0 && (
                <div className="ai-changed">
                  변경한 파일:
                  {message.changedPaths.map((path) => (
                    <button key={path} type="button" className="ai-file-link" onClick={() => onOpenFile(path)}>
                      {path}
                    </button>
                  ))}
                </div>
              )}
              {message.role === 'ai' && message.changedPaths?.length === 0 && (
                <div className="ai-changed muted">변경한 파일 없음</div>
              )}
              {message.tokens !== undefined && <div className="ai-tokens muted">{message.tokens.toLocaleString()} 토큰</div>}
            </div>
          </div>
        ))}

        {pending && (
          <div className="ai-message ai-ai">
            <span className="ai-role">AI</span>
            <div className="ai-body">
              <p className="ai-text muted ai-thinking">코드를 작성하는 중입니다...</p>
            </div>
          </div>
        )}
      </div>

      <div className="ai-input">
        {usagePercent >= 100 && (
          <p className="ai-warning">AI 사용량 한도를 넘었습니다. 계속 쓸 수 있지만 맞혀도 최하점입니다.</p>
        )}
        <div className="ai-input-row">
          <textarea
            value={draft}
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={disabled ? '응시가 끝났습니다.' : '예) src/index.js의 함수를 구현해줘. 조건: ... (Enter 전송 · Shift+Enter 줄바꿈)'}
            disabled={disabled}
            rows={3}
            maxLength={4000}
          />
          <button type="button" className="button" onClick={onSend} disabled={!canSend}>
            {pending ? '작업 중...' : '보내기'}
          </button>
        </div>
      </div>
    </div>
  )
}
