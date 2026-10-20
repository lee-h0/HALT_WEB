package com.halt.halt_web.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 응시자 코드를 Docker 컨테이너 안에서 실행한다.
 *
 * 격리 설정
 * - 네트워크 차단 (--network none)
 * - 메모리 256MB, CPU 1개, 프로세스 64개 제한
 * - 작업 폴더는 읽기 전용, 일반 사용자(node) 권한으로 실행
 * - 제한시간을 넘기면 컨테이너를 강제 종료
 */
@Component
public class DockerSandboxRunner {

    private static final Logger log = LoggerFactory.getLogger(DockerSandboxRunner.class);
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    private final String dockerCommand;
    private final String image;
    private final int timeoutSec;

    /**
     * @param dockerCommand docker 실행 파일. 보통 "docker"로 충분하고,
     *                      서버가 docker를 찾지 못하면 전체 경로(예: C:\\...\\docker.exe)를 넣는다
     */
    public DockerSandboxRunner(@Value("${app.sandbox.docker-command:docker}") String dockerCommand,
                               @Value("${app.sandbox.image:node:22-alpine}") String image,
                               @Value("${app.sandbox.timeout-sec:30}") int timeoutSec) {
        this.dockerCommand = dockerCommand;
        this.image = image;
        this.timeoutSec = timeoutSec;
    }

    /**
     * @param files       경로 -> 내용. 경로는 이미 검증된 상대 경로여야 한다
     * @param testCommand 컨테이너 안에서 실행할 명령 (예: node --test)
     */
    public SandboxResult run(Map<String, String> files, String testCommand) {
        Path workDir = null;
        String containerName = "halt-run-" + UUID.randomUUID();
        try {
            workDir = Files.createTempDirectory("halt-run-");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Path target = workDir.resolve(entry.getKey()).normalize();
                if (!target.startsWith(workDir)) {
                    throw new IllegalArgumentException("잘못된 파일 경로: " + entry.getKey());
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            }

            List<String> command = List.of(
                    dockerCommand, "run", "--rm",
                    "--name", containerName,
                    "--network", "none",
                    "--memory", "256m",
                    "--cpus", "1",
                    "--pids-limit", "64",
                    "--user", "node",
                    "--mount", "type=bind,source=" + workDir.toAbsolutePath() + ",target=/workspace,readonly",
                    "--tmpfs", "/tmp",
                    "--workdir", "/workspace",
                    "--env", "NO_COLOR=1",
                    image,
                    "sh", "-c", testCommand);

            long startedAt = System.currentTimeMillis();
            Process process;
            try {
                process = new ProcessBuilder(command).redirectErrorStream(true).start();
            } catch (IOException e) {
                log.error("docker 명령 실행 실패 (명령: {}): {}", dockerCommand, e.getMessage());
                throw new SandboxException("Docker를 실행할 수 없습니다. Docker Desktop이 설치되어 있고 실행 중인지 확인하세요.", e);
            }

            OutputCollector collector = new OutputCollector(process.getInputStream());
            collector.start();

            boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                killContainer(containerName);
                process.destroyForcibly();
            }
            collector.join(5000);
            long durationMs = System.currentTimeMillis() - startedAt;

            String output = collector.text();
            int exitCode = finished ? process.exitValue() : -1;

            // docker 자체가 실패한 경우 (Docker가 꺼져 있음, 이미지 없음 등)
            if (finished && exitCode == 125) {
                throw new SandboxException("Docker 컨테이너를 시작하지 못했습니다. Docker Desktop이 실행 중인지 확인하세요.\n" + output);
            }
            if (!finished) {
                output = output + "\n[제한시간 " + timeoutSec + "초를 넘겨 실행을 중단했습니다]";
            }
            return new SandboxResult(exitCode, output, !finished, durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            killContainer(containerName);
            throw new SandboxException("테스트 실행이 중단되었습니다.", e);
        } catch (IOException e) {
            throw new SandboxException("테스트 파일을 준비하지 못했습니다.", e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private void killContainer(String containerName) {
        try {
            new ProcessBuilder(dockerCommand, "kill", containerName).start().waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            log.warn("컨테이너 강제 종료 실패: {}", containerName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 임시 폴더라 남아도 큰 문제는 없다
                }
            });
        } catch (IOException ignored) {
            // 위와 같음
        }
    }

    /** 프로세스 출력을 별도 스레드에서 읽는다 (읽지 않으면 출력이 많을 때 프로세스가 멈춘다) */
    private static final class OutputCollector extends Thread {

        private final InputStream input;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private volatile boolean truncated;

        OutputCollector(InputStream input) {
            this.input = input;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] chunk = new byte[4096];
            try {
                int read;
                while ((read = input.read(chunk)) != -1) {
                    synchronized (buffer) {
                        int room = MAX_OUTPUT_BYTES - buffer.size();
                        if (room > 0) {
                            buffer.write(chunk, 0, Math.min(read, room));
                        }
                        if (read > room) {
                            truncated = true;
                        }
                    }
                }
            } catch (IOException ignored) {
                // 프로세스가 강제 종료되면 스트림이 끊긴다
            }
        }

        String text() {
            synchronized (buffer) {
                String text = buffer.toString(StandardCharsets.UTF_8);
                return truncated ? text + "\n[출력이 너무 길어 일부를 생략했습니다]" : text;
            }
        }
    }
}
