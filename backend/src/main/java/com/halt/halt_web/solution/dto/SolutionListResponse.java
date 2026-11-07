package com.halt.halt_web.solution.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 문제별 다른 사람 풀이 목록.
 *
 * @param access     열람 권한
 * @param totalCount 공개된 풀이 수 (잠겨 있어도 알려 준다)
 * @param solutions  잠겨 있으면 빈 배열
 */
public record SolutionListResponse(Long problemId, String title, String difficulty,
                                   Access access, int totalCount, List<SolutionSummary> solutions) {

    /**
     * @param status     SOLVED(내가 푼 문제), UNLOCKED(포인트를 내고 연 문제), LOCKED(아직 못 봄)
     * @param unlockCost 열람 비용 (포인트)
     * @param myPoints   내 보유 포인트
     */
    public record Access(String status, boolean canView, int unlockCost, int myPoints) {
    }

    public record SolutionSummary(int rank, Long attemptId, String nickname, boolean me, int score,
                                  int elapsedSec, int tokenUsagePercent, int promptCount,
                                  LocalDateTime solvedAt) {
    }
}
