package com.halt.halt_web.problem;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.DifficultySetting;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.UserProblemBest;
import com.halt.halt_web.domain.UserProblemId;
import com.halt.halt_web.problem.dto.ProblemDetailResponse;
import com.halt.halt_web.problem.dto.ProblemFileResponse;
import com.halt.halt_web.problem.dto.ProblemSummaryResponse;
import com.halt.halt_web.repository.ProblemFileRepository;
import com.halt.halt_web.repository.ProblemRepository;
import com.halt.halt_web.repository.UserProblemBestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    /** 제한시간 20분 (전 문제 공통) */
    public static final int TIME_LIMIT_SEC = 20 * 60;

    private final ProblemRepository problemRepository;
    private final ProblemFileRepository problemFileRepository;
    private final UserProblemBestRepository userProblemBestRepository;

    public ProblemService(ProblemRepository problemRepository,
                          ProblemFileRepository problemFileRepository,
                          UserProblemBestRepository userProblemBestRepository) {
        this.problemRepository = problemRepository;
        this.problemFileRepository = problemFileRepository;
        this.userProblemBestRepository = userProblemBestRepository;
    }

    /** 문제 목록. 쉬운 문제부터 보여준다 */
    @Transactional(readOnly = true)
    public List<ProblemSummaryResponse> list(Long userId) {
        Set<Long> solvedProblemIds = userProblemBestRepository.findByIdUserId(userId).stream()
                .map(UserProblemBest::getId)
                .map(UserProblemId::getProblemId)
                .collect(Collectors.toSet());

        return problemRepository.findByActiveTrue().stream()
                .map(problem -> new ProblemSummaryResponse(
                        problem.getProblemId(),
                        problem.getTitle(),
                        problem.getDifficultySetting().getDifficulty(),
                        problem.getDifficultySetting().getMaxPoints(),
                        solvedProblemIds.contains(problem.getProblemId())))
                .sorted(Comparator.comparingInt(ProblemSummaryResponse::maxPoints)
                        .thenComparing(ProblemSummaryResponse::problemId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemDetailResponse get(Long problemId, Long userId) {
        Problem problem = problemRepository.findById(problemId)
                .filter(Problem::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));
        DifficultySetting difficulty = problem.getDifficultySetting();

        List<ProblemFileResponse> files = problemFileRepository.findByProblemProblemIdAndHiddenFalse(problemId).stream()
                .map(file -> new ProblemFileResponse(file.getFilePath(), file.getContent()))
                .sorted(Comparator.comparing(ProblemFileResponse::path))
                .toList();

        boolean solved = userProblemBestRepository.existsById(new UserProblemId(userId, problemId));

        return new ProblemDetailResponse(
                problem.getProblemId(),
                problem.getTitle(),
                difficulty.getDifficulty(),
                problem.getDescription(),
                problem.getConstraintsText(),
                problem.getOutOfScopeText(),
                problem.getTestCommand(),
                TIME_LIMIT_SEC,
                difficulty.getTokenLimit(),
                difficulty.getMaxPoints(),
                difficulty.getMinPoints(),
                solved,
                files);
    }
}
