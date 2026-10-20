package com.halt.halt_web.sandbox;

/** Docker를 실행하지 못한 경우 (Docker가 꺼져 있음, 이미지 없음 등) */
public class SandboxException extends RuntimeException {

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public SandboxException(String message) {
        super(message);
    }
}
