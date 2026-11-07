package com.halt.halt_web.mypage.dto;

/**
 * 오늘의 추천 문제.
 * 추천할 문제가 없으면(모두 풀었거나 문제가 없음) problemId가 null이고 message에 안내가 들어간다.
 *
 * @param solved 추천받은 뒤 오늘 이미 푼 경우 true
 */
public record RecommendationResponse(Long problemId, String title, String difficulty, int maxPoints,
                                     String reason, boolean solved, String message) {

    public static RecommendationResponse none(String message) {
        return new RecommendationResponse(null, null, null, 0, null, false, message);
    }
}
