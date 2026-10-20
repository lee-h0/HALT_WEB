package com.halt.halt_web.attempt;

import com.halt.halt_web.attempt.dto.StartAttemptResponse;
import com.halt.halt_web.attempt.dto.TestRunRequest;
import com.halt.halt_web.attempt.dto.TestRunResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttemptController {

    private final AttemptService attemptService;
    private final TestRunService testRunService;

    public AttemptController(AttemptService attemptService, TestRunService testRunService) {
        this.attemptService = attemptService;
        this.testRunService = testRunService;
    }

    /** 응시 시작 (문제 풀이 화면에 들어갈 때) */
    @PostMapping("/api/problems/{problemId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public StartAttemptResponse start(@PathVariable("problemId") Long problemId,
                                      @AuthenticationPrincipal Long userId) {
        return attemptService.start(userId, problemId);
    }

    /** 응시 이탈 (문제 풀이 화면을 벗어날 때) */
    @PostMapping("/api/attempts/{attemptId}/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable("attemptId") Long attemptId,
                        @AuthenticationPrincipal Long userId) {
        attemptService.abandon(userId, attemptId);
    }

    /** 테스트 실행 (공개 테스트만). Docker에서 실행하므로 수 초에서 수십 초 걸린다 */
    @PostMapping("/api/attempts/{attemptId}/test")
    public TestRunResponse test(@PathVariable("attemptId") Long attemptId,
                                @AuthenticationPrincipal Long userId,
                                @Valid @RequestBody TestRunRequest request) {
        return testRunService.runVisibleTests(userId, attemptId, request);
    }
}
