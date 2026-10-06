package com.halt.halt_web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디를 입력하세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력하세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력하세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력하세요.")
        String password,

        @NotBlank(message = "닉네임을 입력하세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력하세요.")
        String nickname,

        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력하세요.")
        String email
) {
}
