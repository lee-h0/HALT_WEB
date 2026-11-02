package com.halt.halt_web.ranking;

import com.halt.halt_web.ranking.dto.RankingResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 랭킹. 로그인하지 않아도 볼 수 있다 (SecurityConfig에서 permitAll).
     * 로그인했으면 userId가 들어와서 내 순위도 함께 준다.
     */
    @GetMapping("/api/ranking")
    public RankingResponse ranking(@AuthenticationPrincipal Long userId) {
        return rankingService.getRanking(userId);
    }
}
