package com.halt.halt_web.ai.dto;

import com.halt.halt_web.problem.dto.ProblemFileResponse;

import java.util.List;

/**
 * AI 지시 결과
 *
 * @param reply        AI의 답변 (무엇을 했는지)
 * @param changedFiles AI가 바꾸거나 새로 만든 파일. 화면은 이 내용으로 파일을 덮어쓴다
 */
public record PromptResponse(
        String reply,
        List<ProblemFileResponse> changedFiles,
        int inputTokens,
        int outputTokens,
        int tokensUsed,
        int tokenUsagePercent,
        int promptCount,
        int fileChangeCount
) {
}
