package com.halt.halt_web.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 테스트 실행 요청. 응시자가 지금 에디터에 가진 파일 전체를 보낸다 */
public record TestRunRequest(
        @NotNull(message = "파일 목록이 없습니다.")
        @Size(max = 100, message = "파일은 100개까지 보낼 수 있습니다.")
        List<@Valid FileContent> files
) {

    public record FileContent(
            @NotBlank(message = "파일 경로가 없습니다.")
            @Size(max = 260, message = "파일 경로가 너무 깁니다.")
            String path,

            @NotNull(message = "파일 내용이 없습니다.")
            String content
    ) {
    }
}
