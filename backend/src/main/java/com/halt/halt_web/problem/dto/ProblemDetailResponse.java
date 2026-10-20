package com.halt.halt_web.problem.dto;

import java.util.List;

/** 문제 풀이 화면에 필요한 정보. 숨김 테스트 파일은 포함하지 않는다 */
public record ProblemDetailResponse(
        Long problemId,
        String title,
        String difficulty,
        String description,
        String constraintsText,
        String outOfScopeText,
        String testCommand,
        int timeLimitSec,
        int tokenLimit,
        int maxPoints,
        int minPoints,
        boolean solved,
        List<ProblemFileResponse> files
) {
}
