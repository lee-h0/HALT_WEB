package com.halt.halt_web.submission;

import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptFile;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.RunType;
import com.halt.halt_web.domain.TestRun;
import com.halt.halt_web.domain.User;
import com.halt.halt_web.domain.UserProblemBest;
import com.halt.halt_web.domain.UserProblemId;
import com.halt.halt_web.repository.AttemptFileRepository;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.TestRunRepository;
import com.halt.halt_web.repository.UserProblemBestRepository;
import com.halt.halt_web.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** 채점이 끝난 뒤 DB에 반영하는 부분. 하나의 트랜잭션으로 묶어 포인트와 기록이 어긋나지 않게 한다 */
@Service
public class SubmissionRecorder {

    /** 기록 결과 */
    public record Recorded(int pointsAwarded, int totalPoints, boolean firstSolve, boolean newBest) {
    }

    private final AttemptRepository attemptRepository;
    private final AttemptFileRepository attemptFileRepository;
    private final TestRunRepository testRunRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final UserRepository userRepository;

    public SubmissionRecorder(AttemptRepository attemptRepository,
                              AttemptFileRepository attemptFileRepository,
                              TestRunRepository testRunRepository,
                              UserProblemBestRepository userProblemBestRepository,
                              UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.attemptFileRepository = attemptFileRepository;
        this.testRunRepository = testRunRepository;
        this.userProblemBestRepository = userProblemBestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Recorded record(Long attemptId, boolean correct, int score, boolean pointsAvailable,
                           long elapsedSec, Map<String, String> submittedFiles,
                           int passedCount, int totalCount, String output) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        // 채점하는 동안 다른 요청(중복 제출, 이탈)이 응시를 끝냈을 수 있다
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 응시입니다.");
        }

        attempt.setStatus(correct ? AttemptStatus.PASSED : AttemptStatus.FAILED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setElapsedSec((int) elapsedSec);
        attempt.setScore(score);

        // 다른 사람 풀이 화면에서 보여줄 제출 파일 (숨김 테스트는 저장하지 않는다)
        submittedFiles.forEach((path, content) ->
                attemptFileRepository.save(new AttemptFile(attemptId, path, content)));

        TestRun run = new TestRun();
        run.setAttempt(attempt);
        run.setRunType(RunType.SUBMIT);
        run.setPassed(correct);
        run.setPassedCount(passedCount);
        run.setTotalCount(totalCount);
        run.setOutputText(output);
        testRunRepository.save(run);

        User user = attempt.getUser();
        if (!correct || !pointsAvailable) {
            return new Recorded(0, user.getTotalPoints(), false, false);
        }

        // 재풀이 규칙: 포인트와 기록은 처음 맞힌 제출 1건만 인정한다.
        // 다시 풀어 더 빠르거나 높은 점수가 나와도 포인트와 기록은 바뀌지 않는다.
        Long userId = user.getUserId();
        Long problemId = attempt.getProblem().getProblemId();
        Optional<UserProblemBest> existing = userProblemBestRepository.findById(new UserProblemId(userId, problemId));

        if (existing.isEmpty()) {
            userProblemBestRepository.save(new UserProblemBest(userId, problemId, attempt, score));
            user.setTotalPoints(user.getTotalPoints() + score);
            user.setSolvedCount(user.getSolvedCount() + 1);
            return new Recorded(score, user.getTotalPoints(), true, true);
        }

        return new Recorded(0, user.getTotalPoints(), false, false);
    }
}
