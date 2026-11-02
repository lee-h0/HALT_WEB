package com.halt.halt_web.submission.dto;

import com.halt.halt_web.attempt.dto.TestRunRequest.FileContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 제출 요청. 지금 에디터의 파일 전체를 보낸다 */
public record SubmitRequest(
        @NotNull(message = "파일 목록이 없습니다.")
        @Size(max = 100, message = "파일은 100개까지 보낼 수 있습니다.")
        List<@Valid FileContent> files
) {
}
