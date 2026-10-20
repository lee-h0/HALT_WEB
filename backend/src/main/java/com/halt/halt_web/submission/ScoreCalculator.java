package com.halt.halt_web.submission;

/**
 * 점수 계산 (확정 규칙)
 * - 오답: 0점
 * - 정답이지만 AI 사용량 100% 이상 또는 제한시간 초과: 최하점
 * - 정답이고 한도 이내: 최하점 + (만점 - 최하점) x (1 - (AI 사용량 비율 + 시간 비율) / 2), 반올림
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    public static int calculate(boolean correct, int tokensUsed, int tokenLimit, long elapsedSec, int timeLimitSec,
                                int minPoints, int maxPoints) {
        if (!correct) {
            return 0;
        }
        double tokenRatio = tokenLimit > 0 ? (double) tokensUsed / tokenLimit : 0;
        double timeRatio = timeLimitSec > 0 ? (double) elapsedSec / timeLimitSec : 0;
        if (tokenRatio >= 1 || timeRatio > 1) {
            return minPoints;
        }
        double score = minPoints + (maxPoints - minPoints) * (1 - (tokenRatio + timeRatio) / 2);
        return (int) Math.round(score);
    }
}
