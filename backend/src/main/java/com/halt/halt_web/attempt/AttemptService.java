package com.halt.halt_web.attempt;

import com.halt.halt_web.attempt.dto.StartAttemptResponse;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.UserProblemId;
import com.halt.halt_web.problem.ProblemService;
import com.halt.halt_web.problem.dto.ProblemFileResponse;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.ProblemFileRepository;
import com.halt.halt_web.repository.ProblemRepository;
import com.halt.halt_web.repository.SolutionUnlockRepository;
import com.halt.halt_web.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final ProblemRepository problemRepository;
    private final ProblemFileRepository problemFileRepository;
    private final UserRepository userRepository;
    private final SolutionUnlockRepository solutionUnlockRepository;

    public AttemptService(AttemptRepository attemptRepository,
                          ProblemRepository problemRepository,
                          ProblemFileRepository problemFileRepository,
                          UserRepository userRepository,
                          SolutionUnlockRepository solutionUnlockRepository) {
        this.attemptRepository = attemptRepository;
        this.problemRepository = problemRepository;
        this.problemFileRepository = problemFileRepository;
        this.userRepository = userRepository;
        this.solutionUnlockRepository = solutionUnlockRepository;
    }

    /**
     * 응시를 시작한다. 진행 중인 응시가 있으면(다른 문제여도) 이탈 처리하고 새로 시작한다.
     * 응시는 항상 초기 파일, 0초, AI 사용량 0에서 시작한다.
     */
    @Transactional
    public StartAttemptResponse start(Long userId, Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .filter(Problem::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));

        // 회원당 진행 중 응시는 1건만 허용된다 (DB의 고유 인덱스).
        // 새 응시를 넣기 전에 이전 응시의 상태 변경을 먼저 DB에 반영해야 한다.
        attemptRepository.findByUserUserIdAndStatus(userId, AttemptStatus.IN_PROGRESS)
                .ifPresent(previous -> {
                    finish(previous, AttemptStatus.ABANDONED);
                    attemptRepository.saveAndFlush(previous);
                });

        Attempt attempt = new Attempt();
        attempt.setUser(userRepository.getReferenceById(userId));
        attempt.setProblem(problem);
        attempt = attemptRepository.saveAndFlush(attempt);

        List<ProblemFileResponse> files = problemFileRepository.findByProblemProblemIdAndHiddenFalse(problemId).stream()
                .map(file -> new ProblemFileResponse(file.getFilePath(), file.getContent()))
                .sorted(Comparator.comparing(ProblemFileResponse::path))
                .toList();

        boolean unlocked = solutionUnlockRepository.existsById(new UserProblemId(userId, problemId));

        return new StartAttemptResponse(
                attempt.getAttemptId(),
                problemId,
                elapsedSec(attempt),
                ProblemService.TIME_LIMIT_SEC,
                problem.getDifficultySetting().getTokenLimit(),
                attempt.getTokensUsed(),
                attempt.getPromptCount(),
                attempt.getFileChangeCount(),
                !unlocked,
                files);
    }

    /** 문제 페이지를 벗어날 때 호출한다. 이미 끝난 응시면 아무것도 하지 않는다 */
    @Transactional
    public void abandon(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            finish(attempt, AttemptStatus.ABANDONED);
        }
    }

    private static void finish(Attempt attempt, AttemptStatus status) {
        attempt.setStatus(status);
        attempt.setElapsedSec((int) elapsedSec(attempt));
    }

    private static long elapsedSec(Attempt attempt) {
        if (attempt.getStartedAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds());
    }
}
