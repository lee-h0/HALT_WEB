package com.halt.halt_web.problem;

import com.halt.halt_web.problem.dto.ProblemDetailResponse;
import com.halt.halt_web.problem.dto.ProblemSummaryResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /** 문제 목록 */
    @GetMapping
    public List<ProblemSummaryResponse> list(@AuthenticationPrincipal Long userId) {
        return problemService.list(userId);
    }

    /** 문제 상세 (설명, 조건, 공개 파일) */
    @GetMapping("/{problemId}")
    public ProblemDetailResponse get(@PathVariable("problemId") Long problemId,
                                     @AuthenticationPrincipal Long userId) {
        return problemService.get(problemId, userId);
    }
}
