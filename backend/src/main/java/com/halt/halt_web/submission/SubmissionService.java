package com.halt.halt_web.submission;

import com.halt.halt_web.attempt.dto.TestRunRequest;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.DifficultySetting;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.ProblemFile;
import com.halt.halt_web.domain.UserProblemId;
import com.halt.halt_web.problem.ProblemService;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.DifficultySettingRepository;
import com.halt.halt_web.repository.ProblemFileRepository;
import com.halt.halt_web.repository.SolutionUnlockRepository;
import com.halt.halt_web.sandbox.DockerSandboxRunner;
import com.halt.halt_web.sandbox.SandboxException;
import com.halt.halt_web.sandbox.SandboxResult;
import com.halt.halt_web.sandbox.TestOutputParser;
import com.halt.halt_web.submission.dto.SubmitRequest;
import com.halt.halt_web.submission.dto.SubmitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "제출" 버튼. 공개 테스트와 숨김 테스트를 함께 실행해 채점한다.
 *
 * 채점에 쓰는 파일
 * - 응시자가 보낸 파일 (src 등)
 * - 문제의 원래 tests/ 파일로 덮어쓴다 (응시자가 공개 테스트를 고쳐도 영향 없음)
 * - 숨김 테스트를 추가한다
 *
 * Docker 실행에 시간이 걸리므로 이 클래스에는 트랜잭션을 걸지 않고, DB 반영만 SubmissionRecorder에서 한 번에 한다.
 */
@Service
public class SubmissionService {

    private static final int MAX_TOTAL_BYTES = 1024 * 1024;

    private final AttemptRepository attemptRepository;
    private final ProblemFileRepository problemFileRepository;
    private final DifficultySettingRepository difficultySettingRepository;
    private final SolutionUnlockRepository solutionUnlockRepository;
    private final DockerSandboxRunner sandboxRunner;
    private final SubmissionRecorder recorder;

    public SubmissionService(AttemptRepository attemptRepository,
                             ProblemFileRepository problemFileRepository,
                             DifficultySettingRepository difficultySettingRepository,
                             SolutionUnlockRepository solutionUnlockRepository,
                             DockerSandboxRunner sandboxRunner,
                             SubmissionRecorder recorder) {
        this.attemptRepository = attemptRepository;
        this.problemFileRepository = problemFileRepository;
        this.difficultySettingRepository = difficultySettingRepository;
        this.solutionUnlockRepository = solutionUnlockRepository;
        this.sandboxRunner = sandboxRunner;
        this.recorder = recorder;
    }

    public SubmitResponse submit(Long userId, Long attemptId, SubmitRequest request) {
        Attempt attempt = attemptRepository.findWithProblemByAttemptId(attemptId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 응시입니다. 문제를 다시 시작하세요.");
        }

        Problem problem = attempt.getProblem();
        DifficultySetting difficulty = difficultySettingRepository.findById(problem.getDifficultySetting().getDifficulty())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "난이도 설정을 찾을 수 없습니다."));

        // 소요 시간은 제출 버튼을 누른 시점 기준 (채점 시간은 넣지 않는다)
        long elapsedSec = Math.max(0, Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds());

        Map<String, String> submittedFiles = toSafeFileMap(request.files());
        Map<String, String> gradingFiles = new LinkedHashMap<>(submittedFiles);
        // 응시자가 tests/ 아래에 넣은 파일은 채점에서 제외하고, 문제의 원래 테스트와 숨김 테스트를 쓴다
        gradingFiles.keySet().removeIf(path -> path.startsWith("tests/"));
        List<ProblemFile> problemFiles = problemFileRepository.findByProblemProblemId(problem.getProblemId());
        for (ProblemFile file : problemFiles) {
            if (file.isHidden() || file.getFilePath().startsWith("tests/")) {
                gradingFiles.put(file.getFilePath(), file.getContent());
            }
        }

        SandboxResult result;
        try {
            result = sandboxRunner.run(gradingFiles, problem.getTestCommand());
        } catch (SandboxException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }

        TestOutputParser.Summary summary = TestOutputParser.parse(result.output());
        boolean skipped = summary.skippedFileCount() > 0;
        boolean correct = result.exitCode() == 0
                && !result.timedOut()
                && !skipped
                && summary.total() > 0
                && summary.failed() == 0;

        boolean pointsAvailable = !solutionUnlockRepository.existsById(new UserProblemId(userId, problem.getProblemId()));
        int tokenLimit = difficulty.getTokenLimit();
        boolean overLimit = attempt.getTokensUsed() >= tokenLimit || elapsedSec > ProblemService.TIME_LIMIT_SEC;
        int score = pointsAvailable
                ? ScoreCalculator.calculate(correct, attempt.getTokensUsed(), tokenLimit, elapsedSec,
                        ProblemService.TIME_LIMIT_SEC, difficulty.getMinPoints(), difficulty.getMaxPoints())
                : 0;

        int totalCount = Math.max(summary.total(), summary.passed());
        SubmissionRecorder.Recorded recorded = recorder.record(attemptId, correct, score, pointsAvailable,
                elapsedSec, submittedFiles, summary.passed(), totalCount, result.output());

        int tokenUsagePercent = tokenLimit > 0
                ? (int) Math.min(100, Math.round(attempt.getTokensUsed() * 100.0 / tokenLimit))
                : 0;

        return new SubmitResponse(
                correct,
                score,
                recorded.pointsAwarded(),
                recorded.totalPoints(),
                recorded.firstSolve(),
                recorded.newBest(),
                pointsAvailable,
                correct && overLimit,
                summary.passed(),
                totalCount,
                summary.failedNames(),
                message(correct, result.timedOut(), skipped, summary.total()),
                elapsedSec,
                tokenUsagePercent,
                result.durationMs());
    }

    private static String message(boolean correct, boolean timedOut, boolean skipped, int total) {
        if (correct) {
            return "정답입니다.";
        }
        if (timedOut) {
            return "채점 시간이 초과되었습니다. 무한 반복이 있는지 확인하세요.";
        }
        if (skipped) {
            return "테스트가 끝까지 실행되지 않았습니다. 코드가 프로세스를 종료하거나 테스트 실행을 막고 있지 않은지 확인하세요.";
        }
        if (total == 0) {
            return "테스트를 실행하지 못했습니다. 문법 오류가 있는지 확인하세요.";
        }
        return "통과하지 못한 테스트가 있습니다.";
    }

    /** 테스트 실행과 같은 규칙으로 경로를 검사한다 */
    private static Map<String, String> toSafeFileMap(List<TestRunRequest.FileContent> files) {
        Map<String, String> result = new LinkedHashMap<>();
        long totalBytes = 0;
        for (TestRunRequest.FileContent file : files) {
            String path = file.path().replace('\\', '/');
            boolean unsafe = path.startsWith("/")
                    || path.contains(":")
                    || path.contains("//")
                    || path.equals("..")
                    || path.startsWith("../")
                    || path.contains("/../")
                    || path.endsWith("/..");
            if (unsafe) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 경로입니다: " + file.path());
            }
            totalBytes += file.content().getBytes(StandardCharsets.UTF_8).length;
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다. (전체 1MB 이하)");
            }
            result.put(path, file.content());
        }
        return result;
    }
}
