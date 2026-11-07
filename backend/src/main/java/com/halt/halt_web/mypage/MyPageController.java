package com.halt.halt_web.mypage;

import com.halt.halt_web.mypage.dto.DashboardResponse;
import com.halt.halt_web.mypage.dto.RecommendationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 (로그인 필요) */
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    private final DashboardService dashboardService;
    private final RecommendationService recommendationService;

    public MyPageController(DashboardService dashboardService, RecommendationService recommendationService) {
        this.dashboardService = dashboardService;
        this.recommendationService = recommendationService;
    }

    /** 요약, 이어서 응시, 최근 풀이 기록 */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal Long userId) {
        return dashboardService.getDashboard(userId);
    }

    /** AI 추천 오늘의 문제 (그날 처음 요청할 때만 AI를 부르므로 몇 초 걸릴 수 있다) */
    @GetMapping("/recommendation")
    public RecommendationResponse recommendation(@AuthenticationPrincipal Long userId) {
        return recommendationService.today(userId);
    }
}
