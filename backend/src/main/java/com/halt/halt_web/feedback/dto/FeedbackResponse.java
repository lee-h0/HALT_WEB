package com.halt.halt_web.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 풀이 피드백 (검증 역량 평가).
 * 포인트(정답·시간·AI 사용량)와는 별개로, AI를 어떻게 쓰고 결과를 얼마나 맞혔는지를 항목별 0~5점으로 평가한다.
 */
public record FeedbackResponse(
        Long attemptId,
        Long problemId,
        String problemTitle,
        String difficulty,
        boolean correct,
        /** 이번 제출의 포인트 점수 */
        int points,
        String headline,
        Behavior behavior,
        Scores scores,
        Spec spec,
        Efficiency efficiency,
        Accuracy accuracy,
        Lesson lesson,
        String model,
        LocalDateTime createdAt
) {

    /** 행동 로그 요약 */
    public record Behavior(int promptCount, int elapsedSec, int tokenUsagePercent, int testRunCount) {
    }

    /** 항목별 점수. AI를 쓰지 않았으면 spec, efficiency는 null */
    public record Scores(Integer spec, Integer efficiency, int accuracy, int total, int max) {
    }

    /** 조건 명시: 지킬 조건을 지시에 얼마나 썼는지 */
    public record Spec(Integer score, int mentionedCount, int constraintCount,
                       List<ConstraintCheck> checks, String feedback) {
    }

    /** @param evidence 그 조건을 쓴 지시 문장 (없으면 빈 문자열) */
    public record ConstraintCheck(String constraint, boolean mentioned, String evidence) {
    }

    /** 지시 효율: 지시를 구체적이고 경제적으로 했는지 */
    public record Efficiency(Integer score, String feedback, List<PromptReview> reviews) {
    }

    /**
     * @param verdict      GOOD(좋은 지시) 또는 IMPROVE(고칠 점이 있는 지시)
     * @param betterPrompt 이렇게 지시하면 더 좋다는 예시 (GOOD이면 빈 문자열일 수 있다)
     */
    public record PromptReview(int index, String prompt, String verdict, String comment, String betterPrompt) {
    }

    /** 결과 정확성: 공개·숨김 테스트 결과 */
    public record Accuracy(int score, int passedCount, int totalCount, List<String> failedTests, String feedback) {
    }

    /** 이번 문제로 배우기 */
    public record Lesson(String title, String nextTime, String codeProblem, String whyItMatters) {
    }

    public FeedbackResponse withCreatedAt(LocalDateTime value) {
        return new FeedbackResponse(attemptId, problemId, problemTitle, difficulty, correct, points, headline,
                behavior, scores, spec, efficiency, accuracy, lesson, model, value);
    }
}
