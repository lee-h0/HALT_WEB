package com.halt.halt_web.attempt;

import com.halt.halt_web.attempt.dto.TestRunRequest;
import com.halt.halt_web.attempt.dto.TestRunResponse;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.RunType;
import com.halt.halt_web.domain.TestRun;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.TestRunRepository;
import com.halt.halt_web.sandbox.DockerSandboxRunner;
import com.halt.halt_web.sandbox.SandboxException;
import com.halt.halt_web.sandbox.SandboxResult;
import com.halt.halt_web.sandbox.TestOutputParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "테스트 실행" 버튼. 응시자가 보낸 파일로 공개 테스트만 돌린다.
 * Docker 실행이 수십 초 걸릴 수 있어서, 그동안 DB 트랜잭션을 붙잡지 않도록 일부러 @Transactional을 붙이지 않았다.
 */
@Service
public class TestRunService {

    private static final int MAX_TOTAL_BYTES = 1024 * 1024;

    private final AttemptRepository attemptRepository;
    private final TestRunRepository testRunRepository;
    private final DockerSandboxRunner sandboxRunner;

    public TestRunService(AttemptRepository attemptRepository,
                          TestRunRepository testRunRepository,
                          DockerSandboxRunner sandboxRunner) {
        this.attemptRepository = attemptRepository;
        this.testRunRepository = testRunRepository;
        this.sandboxRunner = sandboxRunner;
    }

    public TestRunResponse runVisibleTests(Long userId, Long attemptId, TestRunRequest request) {
        Attempt attempt = attemptRepository.findWithProblemByAttemptId(attemptId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 응시입니다. 문제를 다시 시작하세요.");
        }

        Map<String, String> files = toSafeFileMap(request);

        SandboxResult result;
        try {
            result = sandboxRunner.run(files, attempt.getProblem().getTestCommand());
        } catch (SandboxException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }

        TestOutputParser.Summary summary = TestOutputParser.parse(result.output());
        boolean passed = result.exitCode() == 0 && summary.total() > 0 && summary.failed() == 0;

        TestRun run = new TestRun();
        run.setAttempt(attempt);
        run.setRunType(RunType.TEST);
        run.setPassed(passed);
        run.setPassedCount(summary.passed());
        run.setTotalCount(Math.max(summary.total(), summary.passed()));
        run.setOutputText(result.output());
        testRunRepository.save(run);

        return new TestRunResponse(passed, run.getPassedCount(), run.getTotalCount(),
                result.timedOut(), result.durationMs(), result.output());
    }

    /** 경로를 검사하고 정리한다. 절대 경로, 상위 폴더(..) 접근, 지나치게 큰 요청은 거부한다 */
    private static Map<String, String> toSafeFileMap(TestRunRequest request) {
        Map<String, String> files = new LinkedHashMap<>();
        long totalBytes = 0;
        for (TestRunRequest.FileContent file : request.files()) {
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
            files.put(path, file.content());
        }
        return files;
    }
}
