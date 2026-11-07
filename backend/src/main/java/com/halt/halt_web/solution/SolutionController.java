package com.halt.halt_web.solution;

import com.halt.halt_web.solution.dto.SolutionDetailResponse;
import com.halt.halt_web.solution.dto.SolutionListResponse;
import com.halt.halt_web.solution.dto.UnlockResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 다른 사람 풀이 (로그인 필요) */
@RestController
public class SolutionController {

    private final SolutionService solutionService;

    public SolutionController(SolutionService solutionService) {
        this.solutionService = solutionService;
    }

    /** 문제별 풀이 목록 (잠겨 있으면 열람 비용만) */
    @GetMapping("/api/problems/{problemId}/solutions")
    public SolutionListResponse list(@PathVariable("problemId") Long problemId,
                                     @AuthenticationPrincipal Long userId) {
        return solutionService.list(userId, problemId);
    }

    /** 포인트를 내고 풀이 열기 */
    @PostMapping("/api/problems/{problemId}/solutions/unlock")
    public UnlockResponse unlock(@PathVariable("problemId") Long problemId,
                                 @AuthenticationPrincipal Long userId) {
        return solutionService.unlock(userId, problemId);
    }

    /** 풀이 한 건 (지시 기록 + 제출 코드) */
    @GetMapping("/api/solutions/{attemptId}")
    public SolutionDetailResponse detail(@PathVariable("attemptId") Long attemptId,
                                         @AuthenticationPrincipal Long userId) {
        return solutionService.detail(userId, attemptId);
    }
}
