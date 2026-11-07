package com.halt.halt_web.solution;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.SolutionUnlock;
import com.halt.halt_web.domain.User;
import com.halt.halt_web.domain.UserProblemBest;
import com.halt.halt_web.domain.UserProblemId;
import com.halt.halt_web.problem.dto.ProblemFileResponse;
import com.halt.halt_web.repository.AttemptFileRepository;
import com.halt.halt_web.repository.PromptLogRepository;
import com.halt.halt_web.repository.SolutionQueryRepository;
import com.halt.halt_web.repository.SolutionUnlockRepository;
import com.halt.halt_web.repository.UserProblemBestRepository;
import com.halt.halt_web.repository.UserRepository;
import com.halt.halt_web.solution.dto.SolutionDetailResponse;
import com.halt.halt_web.solution.dto.SolutionDetailResponse.PromptItem;
import com.halt.halt_web.solution.dto.SolutionListResponse;
import com.halt.halt_web.solution.dto.SolutionListResponse.Access;
import com.halt.halt_web.solution.dto.SolutionListResponse.SolutionSummary;
import com.halt.halt_web.solution.dto.UnlockResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 다른 사람 풀이.
 * - 내가 푼 문제: 자유롭게 본다.
 * - 못 푼 문제: 난이도별 열람 비용(포인트)을 내야 본다. 연 문제는 이후 맞혀도 점수·포인트·풀이 수가 없다.
 *   (제출할 때 SubmissionService가 solution_unlocks를 보고 점수를 0으로 처리한다)
 * - 공개하는 풀이는 각 회원이 처음 맞힌 제출(user_problem_best)뿐이다.
 */
@Service
public class SolutionService {

    public static final String SOLVED = "SOLVED";
    public static final String UNLOCKED = "UNLOCKED";
    public static final String LOCKED = "LOCKED";

    private final SolutionQueryRepository queryRepository;
    private final UserRepository userRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final SolutionUnlockRepository solutionUnlockRepository;
    private final PromptLogRepository promptLogRepository;
    private final AttemptFileRepository attemptFileRepository;

    public SolutionService(SolutionQueryRepository queryRepository,
                           UserRepository userRepository,
                           UserProblemBestRepository userProblemBestRepository,
                           SolutionUnlockRepository solutionUnlockRepository,
                           PromptLogRepository promptLogRepository,
                           AttemptFileRepository attemptFileRepository) {
        this.queryRepository = queryRepository;
        this.userRepository = userRepository;
        this.userProblemBestRepository = userProblemBestRepository;
        this.solutionUnlockRepository = solutionUnlockRepository;
        this.promptLogRepository = promptLogRepository;
        this.attemptFileRepository = attemptFileRepository;
    }

    /** 문제별 풀이 목록. 잠겨 있으면 목록 없이 열람 비용만 알려 준다 */
    @Transactional(readOnly = true)
    public SolutionListResponse list(Long userId, Long problemId) {
        Problem problem = findProblem(problemId);
        User user = findUser(userId);
        String status = accessStatus(userId, problemId);
        boolean canView = !LOCKED.equals(status);
        Access access = new Access(status, canView, problem.getDifficultySetting().getUnlockCost(), user.getTotalPoints());

        List<UserProblemBest> bests = queryRepository.findSolutions(problemId);
        List<SolutionSummary> solutions = new ArrayList<>();
        if (canView) {
            int tokenLimit = problem.getDifficultySetting().getTokenLimit();
            for (int i = 0; i < bests.size(); i++) {
                UserProblemBest best = bests.get(i);
                Attempt attempt = best.getBestAttempt();
                solutions.add(new SolutionSummary(i + 1, attempt.getAttemptId(), attempt.getUser().getNickname(),
                        Objects.equals(attempt.getUser().getUserId(), userId), best.getBestScore(),
                        attempt.getElapsedSec(), usagePercent(attempt.getTokensUsed(), tokenLimit),
                        attempt.getPromptCount(), best.getFirstSolvedAt()));
            }
        }
        return new SolutionListResponse(problem.getProblemId(), problem.getTitle(),
                problem.getDifficultySetting().getDifficulty(), access, bests.size(), solutions);
    }

    /** 풀이 한 건: 지시 기록과 제출 코드 */
    @Transactional(readOnly = true)
    public SolutionDetailResponse detail(Long userId, Long attemptId) {
        UserProblemBest best = queryRepository.findSolutionByAttemptId(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공개된 풀이를 찾을 수 없습니다."));
        Attempt attempt = best.getBestAttempt();
        Problem problem = attempt.getProblem();
        if (!problem.isActive()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "공개된 풀이를 찾을 수 없습니다.");
        }
        if (LOCKED.equals(accessStatus(userId, problem.getProblemId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 문제를 풀거나 포인트를 내고 열람해야 볼 수 있습니다.");
        }

        List<PromptItem> prompts = promptLogRepository.findByAttemptAttemptIdOrderByCreatedAtAsc(attemptId).stream()
                .map(log -> new PromptItem(log.getPromptText(), log.getResponseText(), log.getCreatedAt()))
                .toList();

        // 문제에 원래 있던 테스트(tests/)는 빼고, 응시자가 만든 코드만 보여 준다
        List<ProblemFileResponse> files = attemptFileRepository.findByIdAttemptId(attemptId).stream()
                .filter(file -> !file.getId().getFilePath().startsWith("tests/"))
                .sorted(Comparator.comparing(file -> file.getId().getFilePath()))
                .map(file -> new ProblemFileResponse(file.getId().getFilePath(), file.getContent()))
                .toList();

        return new SolutionDetailResponse(attemptId, problem.getProblemId(), problem.getTitle(),
                problem.getDifficultySetting().getDifficulty(), attempt.getUser().getNickname(),
                Objects.equals(attempt.getUser().getUserId(), userId), best.getBestScore(), attempt.getElapsedSec(),
                usagePercent(attempt.getTokensUsed(), problem.getDifficultySetting().getTokenLimit()),
                attempt.getPromptCount(), attempt.getFileChangeCount(), best.getFirstSolvedAt(), prompts, files);
    }

    /**
     * 못 푼 문제의 풀이를 포인트를 내고 연다.
     * 이미 볼 수 있는 문제면 차감하지 않는다.
     */
    @Transactional
    public UnlockResponse unlock(Long userId, Long problemId) {
        Problem problem = findProblem(problemId);
        User user = findUser(userId);
        if (!LOCKED.equals(accessStatus(userId, problemId))) {
            return new UnlockResponse(0, user.getTotalPoints());
        }

        int cost = problem.getDifficultySetting().getUnlockCost();
        if (user.getTotalPoints() < cost) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "포인트가 부족합니다. (필요 " + cost + " P, 보유 " + user.getTotalPoints() + " P)");
        }

        user.setTotalPoints(user.getTotalPoints() - cost);
        solutionUnlockRepository.save(new SolutionUnlock(userId, problemId, cost));
        return new UnlockResponse(cost, user.getTotalPoints());
    }

    private String accessStatus(Long userId, Long problemId) {
        UserProblemId id = new UserProblemId(userId, problemId);
        if (userProblemBestRepository.existsById(id)) {
            return SOLVED;
        }
        if (solutionUnlockRepository.existsById(id)) {
            return UNLOCKED;
        }
        return LOCKED;
    }

    private Problem findProblem(Long problemId) {
        return queryRepository.findActiveProblem(problemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    private static int usagePercent(int tokensUsed, int tokenLimit) {
        return tokenLimit <= 0 ? 0 : (int) Math.min(100, Math.round(tokensUsed * 100.0 / tokenLimit));
    }
}
