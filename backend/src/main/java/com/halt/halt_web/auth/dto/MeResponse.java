package com.halt.halt_web.auth.dto;

import com.halt.halt_web.domain.User;

/** 로그인한 회원 정보 (비밀번호 해시는 내보내지 않는다) */
public record MeResponse(Long userId, String loginId, String nickname, String email,
                         int totalPoints, int solvedCount) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getUserId(), user.getLoginId(), user.getNickname(), user.getEmail(),
                user.getTotalPoints(), user.getSolvedCount());
    }
}
