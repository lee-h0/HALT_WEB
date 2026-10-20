package com.halt.halt_web.attempt.dto;

import com.halt.halt_web.problem.dto.ProblemFileResponse;

import java.util.List;

/**
 * 응시 시작 응답. 응시마다 초기 파일에서 새로 시작한다.
 *
 * @param elapsedSec      서버 기준 경과 시간. 브라우저 시계와 상관없이 타이머를 맞추는 데 쓴다
 * @param pointsAvailable false면 풀이를 먼저 열람한 문제라 맞혀도 점수가 없다
 */
public record StartAttemptResponse(
        Long attemptId,
        Long problemId,
        long elapsedSec,
        int timeLimitSec,
        int tokenLimit,
        int tokensUsed,
        int promptCount,
        int fileChangeCount,
        boolean pointsAvailable,
        List<ProblemFileResponse> files
) {
}
