package com.halt.halt_web.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * node --test 출력에서 결과를 읽는다.
 * TAP 형식("# pass 3")과 사람이 읽는 형식("ℹ pass 3")의 요약 줄을 모두 처리한다.
 */
public final class TestOutputParser {

    private static final Pattern SUMMARY = Pattern.compile("^\\s*(?:#|ℹ)\\s+(tests|pass|fail)\\s+(\\d+)\\s*$", Pattern.MULTILINE);

    /** 들여쓰기 없는 최상위 결과 줄 (TAP): "ok 3 - 이름", "not ok 4 - 이름" */
    private static final Pattern TOP_LEVEL_RESULT = Pattern.compile("^(not )?ok \\d+ - (.+)$", Pattern.MULTILINE);

    /** 테스트 이름이 파일 경로라면 그 파일은 테스트를 하나도 실행하지 못하고 끝난 것이다 */
    private static final Pattern TEST_FILE_NAME = Pattern.compile("(^|/)[^/]+\\.test\\.[cm]?js$");

    /**
     * @param failedNames      실패한 최상위 테스트 이름
     * @param skippedFileCount 테스트를 실행하지 않고 끝난 테스트 파일 수.
     *                         예를 들어 코드에서 process.exit(0)을 부르면 테스트가 돌지 않았는데도 "통과"로 나온다
     */
    public record Summary(int total, int passed, int failed, List<String> failedNames, int skippedFileCount) {
    }

    private TestOutputParser() {
    }

    /** 요약 줄이 없으면(테스트가 시작도 못 한 경우) 0, 0, 0 */
    public static Summary parse(String output) {
        int total = 0;
        int passed = 0;
        int failed = 0;
        Matcher matcher = SUMMARY.matcher(output);
        // 하위 테스트가 있으면 요약이 여러 번 나올 수 있어 마지막 값을 쓴다
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(2));
            switch (matcher.group(1)) {
                case "tests" -> total = value;
                case "pass" -> passed = value;
                case "fail" -> failed = value;
                default -> { }
            }
        }

        List<String> failedNames = new ArrayList<>();
        int skippedFileCount = 0;
        Matcher result = TOP_LEVEL_RESULT.matcher(output);
        while (result.find()) {
            String name = result.group(2).trim();
            if (TEST_FILE_NAME.matcher(name).find()) {
                skippedFileCount++;
            } else if (result.group(1) != null) {
                failedNames.add(name);
            }
        }
        return new Summary(total, passed, failed, failedNames, skippedFileCount);
    }
}
