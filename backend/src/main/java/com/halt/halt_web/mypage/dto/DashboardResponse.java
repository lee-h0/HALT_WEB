package com.halt.halt_web.mypage.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 마이페이지 대시보드 (오늘의 추천은 AI 호출이 있어 따로 받는다) */
public record DashboardResponse(Summary summary, Resume resume, List<RecentSubmission> recent) {

    /**
     * @param accuracyPercent 정답률. 제출이 없으면 null
     */
    public record Summary(String nickname, int totalPoints, int solvedCount, int totalProblems,
                          long submitCount, long passedCount, Integer accuracyPercent) {
    }

    /**
     * 이어서 응시: 아직 못 푼 문제 중 가장 최근에 시도한 문제.
     * 응시는 이탈하면 초기화되므로, 같은 문제를 새로 시작하는 바로가기다.
     *
     * @param lastStatus 마지막 응시 상태 (FAILED, ABANDONED, IN_PROGRESS)
     */
    public record Resume(Long problemId, String title, String difficulty, String lastStatus,
                         LocalDateTime lastAt) {
    }

    public record RecentSubmission(Long attemptId, Long problemId, String title, String difficulty,
                                   boolean correct, int score, int elapsedSec, int tokenUsagePercent,
                                   LocalDateTime submittedAt) {
    }
}
