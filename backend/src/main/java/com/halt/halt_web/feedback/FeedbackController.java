package com.halt.halt_web.feedback;

import com.halt.halt_web.feedback.dto.FeedbackResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 제출한 응시의 피드백. 처음 요청이면 AI가 분석하므로 10~30초 걸릴 수 있다 */
    @GetMapping("/api/attempts/{attemptId}/feedback")
    public FeedbackResponse feedback(@PathVariable("attemptId") Long attemptId,
                                     @AuthenticationPrincipal Long userId) {
        return feedbackService.getFeedback(userId, attemptId);
    }
}
