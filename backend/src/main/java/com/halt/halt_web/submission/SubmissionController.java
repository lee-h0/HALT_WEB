package com.halt.halt_web.submission;

import com.halt.halt_web.submission.dto.SubmitRequest;
import com.halt.halt_web.submission.dto.SubmitResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** 제출. 공개 테스트와 숨김 테스트로 채점하고 점수를 반영한다. 응시는 여기서 끝난다 */
    @PostMapping("/api/attempts/{attemptId}/submit")
    public SubmitResponse submit(@PathVariable("attemptId") Long attemptId,
                                 @AuthenticationPrincipal Long userId,
                                 @Valid @RequestBody SubmitRequest request) {
        return submissionService.submit(userId, attemptId, request);
    }
}
