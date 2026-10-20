package com.halt.halt_web.problem.dto;

/** 문제 목록의 한 줄 */
public record ProblemSummaryResponse(Long problemId, String title, String difficulty, int maxPoints, boolean solved) {
}
