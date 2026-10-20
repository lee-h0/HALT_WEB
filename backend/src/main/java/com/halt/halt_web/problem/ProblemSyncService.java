package com.halt.halt_web.problem;

import com.halt.halt_web.domain.DifficultySetting;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.ProblemFile;
import com.halt.halt_web.repository.DifficultySettingRepository;
import com.halt.halt_web.repository.ProblemFileRepository;
import com.halt.halt_web.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 문제 폴더 하나를 읽어 DB에 반영한다.
 *
 * <pre>
 * problems/문제이름/
 *   problem.properties   title, difficulty, testCommand
 *   description.md       문제 설명 (필수)
 *   constraints.md       지킬 조건 (선택)
 *   out_of_scope.md      안 해도 되는 것 (선택)
 *   files/               응시자에게 주는 초기 파일
 *   hidden/              숨김 테스트 (응시자에게 보내지 않음)
 *   solution/            모범 답안 (서버는 읽지 않음)
 * </pre>
 */
@Service
public class ProblemSyncService {

    private static final int MAX_PATH_LENGTH = 260;

    private final ProblemRepository problemRepository;
    private final ProblemFileRepository problemFileRepository;
    private final DifficultySettingRepository difficultySettingRepository;

    public ProblemSyncService(ProblemRepository problemRepository,
                              ProblemFileRepository problemFileRepository,
                              DifficultySettingRepository difficultySettingRepository) {
        this.problemRepository = problemRepository;
        this.problemFileRepository = problemFileRepository;
        this.difficultySettingRepository = difficultySettingRepository;
    }

    /** @return 등록한 파일 수 */
    @Transactional
    public int sync(Path problemDir) throws IOException {
        String slug = problemDir.getFileName().toString();
        if (!slug.matches("[a-z0-9-]{1,50}")) {
            throw new IllegalArgumentException("폴더 이름은 영문 소문자, 숫자, 하이픈(-)만 쓰고 50자 이하여야 합니다: " + slug);
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(problemDir.resolve("problem.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String title = required(properties, "title");
        String difficultyName = required(properties, "difficulty");
        String testCommand = properties.getProperty("testCommand", "node --test").trim();

        DifficultySetting difficulty = difficultySettingRepository.findById(difficultyName)
                .orElseThrow(() -> new IllegalArgumentException("difficulty는 EASY, MEDIUM, HARD 중 하나여야 합니다: " + difficultyName));

        Problem problem = problemRepository.findBySlug(slug).orElseGet(Problem::new);
        problem.setSlug(slug);
        problem.setTitle(title);
        problem.setDifficultySetting(difficulty);
        problem.setDescription(readText(problemDir.resolve("description.md")));
        problem.setConstraintsText(readTextOrNull(problemDir.resolve("constraints.md")));
        problem.setOutOfScopeText(readTextOrNull(problemDir.resolve("out_of_scope.md")));
        problem.setTestCommand(testCommand);
        problem.setActive(true);
        problem = problemRepository.saveAndFlush(problem);

        // 파일은 매번 전부 지우고 다시 넣는다
        problemFileRepository.deleteAllByProblemId(problem.getProblemId());
        int count = 0;
        count += saveFiles(problem, problemDir.resolve("files"), false);
        count += saveFiles(problem, problemDir.resolve("hidden"), true);
        return count;
    }

    /** 폴더에서 사라진 문제는 목록에서 숨긴다 (응시 기록이 남아 있을 수 있어 지우지는 않는다) */
    @Transactional
    public void deactivateMissing(Set<String> existingSlugs) {
        for (Problem problem : problemRepository.findAll()) {
            if (problem.isActive() && !existingSlugs.contains(problem.getSlug())) {
                problem.setActive(false);
            }
        }
    }

    private int saveFiles(Problem problem, Path baseDir, boolean hidden) throws IOException {
        if (!Files.isDirectory(baseDir)) {
            return 0;
        }
        List<Path> paths;
        try (Stream<Path> stream = Files.walk(baseDir)) {
            paths = stream.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path path : paths) {
            String relativePath = baseDir.relativize(path).toString().replace('\\', '/');
            if (relativePath.length() > MAX_PATH_LENGTH) {
                throw new IllegalArgumentException("파일 경로가 너무 깁니다: " + relativePath);
            }
            ProblemFile file = new ProblemFile();
            file.setProblem(problem);
            file.setFilePath(relativePath);
            file.setContent(readText(path));
            file.setHidden(hidden);
            problemFileRepository.save(file);
        }
        return paths.size();
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("problem.properties에 " + key + " 값이 없습니다.");
        }
        return value.trim();
    }

    private static String readText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        // 일부 편집기가 파일 맨 앞에 넣는 BOM 문자를 제거한다
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static String readTextOrNull(Path path) throws IOException {
        return Files.isRegularFile(path) ? readText(path) : null;
    }
}
