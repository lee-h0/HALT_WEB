package com.halt.halt_web.solution.dto;

/**
 * @param pointsSpent 이번에 차감한 포인트 (이미 열람할 수 있었으면 0)
 * @param totalPoints 차감 후 보유 포인트
 */
public record UnlockResponse(int pointsSpent, int totalPoints) {
}
