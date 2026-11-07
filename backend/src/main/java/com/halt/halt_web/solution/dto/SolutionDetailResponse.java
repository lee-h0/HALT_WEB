package com.halt.halt_web.solution.dto;

import com.halt.halt_web.problem.dto.ProblemFileResponse;

import java.time.LocalDateTime;
import java.util.List;

/** 풀이 한 건: AI에게 한 지시와 제출한 코드 */
public record SolutionDetailResponse(Long attemptId, Long problemId, String problemTitle, String difficulty,
                                     String nickname, boolean me, int score, int elapsedSec,
                                     int tokenUsagePercent, int promptCount, int fileChangeCount,
                                     LocalDateTime solvedAt, List<PromptItem> prompts,
                                     List<ProblemFileResponse> files) {

    public record PromptItem(String prompt, String response, LocalDateTime createdAt) {
    }
}
