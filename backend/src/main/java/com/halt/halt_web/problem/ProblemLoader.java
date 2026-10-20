package com.halt.halt_web.problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** 서버가 시작할 때 problems 폴더를 읽어 문제를 등록한다 */
@Component
public class ProblemLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemLoader.class);

    private final ProblemSyncService problemSyncService;
    private final Path problemsDir;

    public ProblemLoader(ProblemSyncService problemSyncService,
                         @Value("${app.problems-dir:../problems}") String problemsDir) {
        this.problemSyncService = problemSyncService;
        this.problemsDir = Path.of(problemsDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!Files.isDirectory(problemsDir)) {
            log.warn("문제 폴더가 없어 문제를 등록하지 않았습니다: {}", problemsDir);
            return;
        }

        List<Path> problemDirs;
        try (Stream<Path> stream = Files.list(problemsDir)) {
            problemDirs = stream
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve("problem.properties")))
                    .sorted()
                    .toList();
        }

        Set<String> loadedSlugs = new HashSet<>();
        for (Path dir : problemDirs) {
            String slug = dir.getFileName().toString();
            // 잘못된 문제가 있어도 그 문제만 건너뛰고 서버는 계속 켠다. 기존에 등록된 내용은 그대로 둔다.
            loadedSlugs.add(slug);
            try {
                int fileCount = problemSyncService.sync(dir);
                log.info("문제 등록: {} (파일 {}개)", slug, fileCount);
            } catch (Exception e) {
                log.error("문제 등록 실패: {} - {}", slug, e.getMessage());
            }
        }
        problemSyncService.deactivateMissing(loadedSlugs);
        log.info("문제 폴더 읽기 완료: {} ({}개)", problemsDir, problemDirs.size());
    }
}
