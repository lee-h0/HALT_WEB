package com.halt.halt_web.common;

import org.springframework.http.HttpStatus;

/** 서비스에서 던지는 예외. 상태 코드와 사용자에게 보여줄 메시지를 담는다 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
