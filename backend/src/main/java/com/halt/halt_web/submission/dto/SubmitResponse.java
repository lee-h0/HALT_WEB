package com.halt.halt_web.submission.dto;

import java.util.List;

/**
 * 제출 결과. 숨김 테스트의 내용이 드러나지 않도록 실행 로그 대신 실패한 테스트 이름만 보낸다.
 *
 * @param score           이 응시의 점수
 * @param pointsAwarded   이번 제출로 실제 늘어난 포인트 (처음 맞힌 경우에만 0보다 크다)
 * @param newBest         기록이 새로 생겼는지 (현재 규칙에서는 firstSolve와 같다)
 * @param pointsAvailable false면 풀이를 먼저 열람한 문제라 점수가 반영되지 않는다
 * @param overLimit       AI 사용량 100% 또는 제한시간 초과로 최하점 처리됐는지
 */
public record SubmitResponse(
        boolean correct,
        int score,
        int pointsAwarded,
        int totalPoints,
        boolean firstSolve,
        boolean newBest,
        boolean pointsAvailable,
        boolean overLimit,
        int passedCount,
        int totalCount,
        List<String> failedTests,
        String message,
        long elapsedSec,
        int tokenUsagePercent,
        long durationMs
) {
}
