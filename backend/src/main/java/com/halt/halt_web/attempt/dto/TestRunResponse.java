package com.halt.halt_web.attempt.dto;

public record TestRunResponse(
        boolean passed,
        int passedCount,
        int totalCount,
        boolean timedOut,
        long durationMs,
        String output
) {
}
