package com.halt.halt_web.ai;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.PromptLog;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.PromptLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI 호출이 끝난 뒤 지시 기록과 사용량을 한 번에 저장한다 */
@Service
public class AiUsageRecorder {

    private final AttemptRepository attemptRepository;
    private final PromptLogRepository promptLogRepository;

    public AiUsageRecorder(AttemptRepository attemptRepository, PromptLogRepository promptLogRepository) {
        this.attemptRepository = attemptRepository;
        this.promptLogRepository = promptLogRepository;
    }

    /** @return 갱신된 응시 기록 */
    @Transactional
    public Attempt record(Long attemptId, String prompt, String reply, String model,
                          int inputTokens, int outputTokens, int totalTokens, int changedFileCount) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 응시입니다.");
        }

        PromptLog log = new PromptLog();
        log.setAttempt(attempt);
        log.setPromptText(prompt);
        log.setResponseText(reply);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        promptLogRepository.save(log);

        attempt.setTokensUsed(attempt.getTokensUsed() + totalTokens);
        attempt.setPromptCount(attempt.getPromptCount() + 1);
        attempt.setFileChangeCount(attempt.getFileChangeCount() + changedFileCount);
        return attempt;
    }
}
