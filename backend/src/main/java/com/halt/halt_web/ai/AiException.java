package com.halt.halt_web.ai;

/** AI 호출 실패. 사용자에게 보여줄 수 있는 메시지를 담는다 */
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
