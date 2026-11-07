package com.halt.halt_web.mypage;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.User;
import com.halt.halt_web.mypage.dto.DashboardResponse;
import com.halt.halt_web.mypage.dto.DashboardResponse.RecentSubmission;
import com.halt.halt_web.mypage.dto.DashboardResponse.Resume;
import com.halt.halt_web.mypage.dto.DashboardResponse.Summary;
import com.halt.halt_web.repository.MyPageQueryRepository;
import com.halt.halt_web.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private static final int RECENT_SIZE = 10;
    private static final List<AttemptStatus> SUBMITTED = List.of(AttemptStatus.PASSED, AttemptStatus.FAILED);

    private final UserRepository userRepository;
    private final MyPageQueryRepository queryRepository;

    public DashboardService(UserRepository userRepository, MyPageQueryRepository queryRepository) {
        this.userRepository = userRepository;
        this.queryRepository = queryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        long passed = queryRepository.countByUserUserIdAndStatus(userId, AttemptStatus.PASSED);
        long failed = queryRepository.countByUserUserIdAndStatus(userId, AttemptStatus.FAILED);
        long submitted = passed + failed;
        Integer accuracy = submitted == 0 ? null : (int) Math.round(passed * 100.0 / submitted);
        int totalProblems = queryRepository.findActiveProblemsWithDifficulty().size();

        Summary summary = new Summary(user.getNickname(), user.getTotalPoints(), user.getSolvedCount(),
                totalProblems, submitted, passed, accuracy);

        Resume resume = queryRepository.findLatestUnsolvedAttempts(userId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(attempt -> {
                    Problem problem = attempt.getProblem();
                    return new Resume(problem.getProblemId(), problem.getTitle(),
                            problem.getDifficultySetting().getDifficulty(), attempt.getStatus().name(),
                            attempt.getStartedAt());
                })
                .orElse(null);

        List<RecentSubmission> recent = queryRepository
                .findRecentSubmissions(userId, SUBMITTED, PageRequest.of(0, RECENT_SIZE)).stream()
                .map(DashboardService::toRecent)
                .toList();

        return new DashboardResponse(summary, resume, recent);
    }

    private static RecentSubmission toRecent(Attempt attempt) {
        Problem problem = attempt.getProblem();
        int tokenLimit = Math.max(1, problem.getDifficultySetting().getTokenLimit());
        int usage = (int) Math.min(100, Math.round(attempt.getTokensUsed() * 100.0 / tokenLimit));
        return new RecentSubmission(attempt.getAttemptId(), problem.getProblemId(), problem.getTitle(),
                problem.getDifficultySetting().getDifficulty(), attempt.getStatus() == AttemptStatus.PASSED,
                attempt.getScore() == null ? 0 : attempt.getScore(), attempt.getElapsedSec(), usage,
                attempt.getSubmittedAt());
    }
}
