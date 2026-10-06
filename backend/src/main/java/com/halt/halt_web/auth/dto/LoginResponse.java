package com.halt.halt_web.auth.dto;

/** 로그인 성공 응답. React는 accessToken을 보관했다가 요청마다 Authorization 헤더에 넣는다 */
public record LoginResponse(String accessToken, long expiresInSec, Long userId, String nickname) {
}
