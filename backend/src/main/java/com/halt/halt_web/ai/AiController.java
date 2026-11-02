package com.halt.halt_web.ai;

import com.halt.halt_web.ai.dto.PromptRequest;
import com.halt.halt_web.ai.dto.PromptResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiPromptService aiPromptService;

    public AiController(AiPromptService aiPromptService) {
        this.aiPromptService = aiPromptService;
    }

    /** AI 지시. AI가 바꾼 파일과 갱신된 사용량을 돌려준다 */
    @PostMapping("/api/attempts/{attemptId}/prompt")
    public PromptResponse prompt(@PathVariable("attemptId") Long attemptId,
                                 @AuthenticationPrincipal Long userId,
                                 @Valid @RequestBody PromptRequest request) {
        return aiPromptService.prompt(userId, attemptId, request);
    }
}
