package com.halt.halt_web.ai.dto;

import com.halt.halt_web.attempt.dto.TestRunRequest.FileContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** AI 지시 요청. 응시자의 지시와 지금 에디터의 파일 전체를 보낸다 */
public record PromptRequest(
        @NotBlank(message = "지시 내용을 입력하세요.")
        @Size(max = 4000, message = "지시는 4,000자까지 입력할 수 있습니다.")
        String prompt,

        @NotNull(message = "파일 목록이 없습니다.")
        @Size(max = 100, message = "파일은 100개까지 보낼 수 있습니다.")
        List<@Valid FileContent> files
) {
}
