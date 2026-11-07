package com.halt.halt_web.feedback;

import com.halt.halt_web.ai.AiException;
import com.halt.halt_web.ai.GeminiClient;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptFeedback;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.DifficultySetting;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.domain.RunType;
import com.halt.halt_web.domain.TestRun;
import com.halt.halt_web.feedback.dto.FeedbackResponse;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Accuracy;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Behavior;
import com.halt.halt_web.feedback.dto.FeedbackResponse.ConstraintCheck;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Efficiency;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Lesson;
import com.halt.halt_web.feedback.dto.FeedbackResponse.PromptReview;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Scores;
import com.halt.halt_web.feedback.dto.FeedbackResponse.Spec;
import com.halt.halt_web.repository.AttemptFeedbackRepository;
import com.halt.halt_web.repository.AttemptFileRepository;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.DifficultySettingRepository;
import com.halt.halt_web.repository.PromptLogRepository;
import com.halt.halt_web.repository.TestRunRepository;
import com.halt.halt_web.sandbox.TestOutputParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 풀이 피드백 (검증 역량 평가).
 *
 * 평가 항목 (각 0~5점, 포인트와 별개)
 * - 조건 명시: 문제의 "지킬 조건"을 지시(프롬프트)에 몇 개나 썼는가. AI가 조건별로 판정하고 점수는 서버가 계산한다.
 *   점수 = 내림(5 x 지시에 쓴 조건 수 / 전체 조건 수)
 * - 지시 효율: 지시가 구체적이고 경제적이었는가. AI가 정해진 기준표로 매긴다.
 * - 결과 정확성: 공개·숨김 테스트 통과율. 서버가 계산한다. 점수 = 내림(5 x 통과 / 전체)
 * AI를 쓰지 않고 직접 풀었으면 조건 명시·지시 효율은 평가하지 않는다 (null).
 *
 * 처음 볼 때 한 번 만들어 attempt_feedbacks에 저장하고, 이후에는 저장된 것을 보여 준다.
 * AI 호출은 몇 초 걸리므로 DB 조회와 저장만 짧은 트랜잭션으로 묶는다.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final int MAX_SCORE_PER_ITEM = 5;

    private static final String SYSTEM_INSTRUCTION = """
            너는 "AI 코드 검증 역량 평가" 플랫폼의 채점관이자 코치다.
            응시자는 AI에게 지시(프롬프트)를 해서 코드를 만들게 하고, 그 코드를 검증해 제출한다.
            AI 코드 작성 도우미는 문제 설명과 지킬 조건을 모르고, 응시자가 지시에 쓴 것만 구현한다.
            그래서 지킬 조건을 지시에 구체적으로 쓰는 것이 핵심 역량이다.

            주어진 자료: 문제 설명, 번호가 붙은 지킬 조건, 응시자의 지시와 AI 답변(순서대로), 제출 코드, 테스트 결과, 행동 지표.

            해야 할 일 (모두 한국어, 응시자에게 말하듯 존댓말):
            1. constraintChecks: 지킬 조건마다 응시자가 지시 중 어디에서든 그 조건의 내용을 명시했는지 판정한다.
               - 조건의 핵심(값, 경계, 오류 코드 등)을 실제로 요구했을 때만 mentioned=true. "조건 지켜줘"처럼 내용 없이 말한 것은 false.
               - evidence에는 근거가 된 지시 문장을 짧게 그대로 인용한다. 없으면 빈 문자열.
               - 모든 조건에 대해 index(1부터)를 빠짐없이 쓴다.
            2. specFeedback: 조건 명시에 대한 총평 2~3문장. 빠뜨린 조건이 결과에 어떤 영향을 줬는지 연결한다.
            3. efficiencyScore (0~5 정수)와 efficiencyFeedback(2~3문장). 기준표:
               5: 1~2번의 구체적인 지시로 해결. 반복이나 모호한 지시가 없다.
               4: 지시가 대체로 구체적이고, 보완 지시가 조금 있다.
               3: 모호한 지시("고쳐줘", "다시 해줘")나 같은 요청 반복이 일부 있다.
               2: 대부분의 지시가 모호하거나, 한 번에 할 일을 여러 번 나눠 요청했다.
               1: 지시가 문제 해결과 거의 관련이 없거나 매우 비효율적이다.
               0: 지시가 문제와 무관하다.
               AI 사용량(토큰 %)과 지시 횟수도 참고한다.
            4. promptReviews: 각 지시마다 promptIndex(1부터), verdict(GOOD 또는 IMPROVE), comment(1~2문장),
               betterPrompt(IMPROVE면 같은 의도를 더 좋게 쓴 지시 예시, GOOD이면 빈 문자열)를 쓴다.
               betterPrompt에 숨김 테스트를 그대로 베끼지 말고, 지킬 조건을 지시로 바꾸는 방법을 보여 준다.
            5. accuracyFeedback: 테스트 결과에 대한 설명 2~3문장. 실패한 테스트가 있으면 코드의 어느 부분 때문인지 짚는다.
            6. headline: 이번 응시를 한 줄로 요약한 결론 (40자 안팎).
            7. lesson: title(이 문제의 핵심 교훈 한 줄), nextTime(다음 응시에서 신경 쓸 점),
               codeProblem(제출 코드의 문제점. 정답이면 더 나아질 점), whyItMatters(실제 서비스에서 왜 중요한지). 각 2~3문장.
            응시자가 AI를 쓰지 않았으면 constraintChecks는 모두 false, promptReviews는 빈 배열, efficiencyScore는 0으로 두고
            specFeedback, efficiencyFeedback에는 AI를 쓰지 않았다고만 쓴다.
            """;

    private static final Map<String, Object> STRING = Map.of("type", "STRING");
    private static final Map<String, Object> FEEDBACK_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "headline", STRING,
                    "constraintChecks", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "index", Map.of("type", "INTEGER"),
                                    "mentioned", Map.of("type", "BOOLEAN"),
                                    "evidence", STRING),
                            "required", List.of("index", "mentioned", "evidence"))),
                    "specFeedback", STRING,
                    "efficiencyScore", Map.of("type", "INTEGER"),
                    "efficiencyFeedback", STRING,
                    "promptReviews", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "promptIndex", Map.of("type", "INTEGER"),
                                    "verdict", Map.of("type", "STRING", "enum", List.of("GOOD", "IMPROVE")),
                                    "comment", STRING,
                                    "betterPrompt", STRING),
                            "required", List.of("promptIndex", "verdict", "comment", "betterPrompt"))),
                    "accuracyFeedback", STRING,
                    "lesson", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "title", STRING,
                                    "nextTime", STRING,
                                    "codeProblem", STRING,
                                    "whyItMatters", STRING),
                            "required", List.of("title", "nextTime", "codeProblem", "whyItMatters"))),
            "required", List.of("headline", "constraintChecks", "specFeedback", "efficiencyScore",
                    "efficiencyFeedback", "promptReviews", "accuracyFeedback", "lesson"));

    /** AI 답변 JSON 모양 */
    public record AiFeedback(String headline, List<AiCheck> constraintChecks, String specFeedback,
                             Integer efficiencyScore, String efficiencyFeedback, List<AiReview> promptReviews,
                             String accuracyFeedback, AiLesson lesson) {
    }

    public record AiCheck(Integer index, Boolean mentioned, String evidence) {
    }

    public record AiReview(Integer promptIndex, String verdict, String comment, String betterPrompt) {
    }

    public record AiLesson(String title, String nextTime, String codeProblem, String whyItMatters) {
    }

    /** 피드백을 만드는 데 필요한 자료 (트랜잭션 안에서 한 번에 읽어 둔다) */
    private record Context(Long attemptId, Long problemId, String problemTitle, String difficulty,
                           String description, List<String> constraints, boolean correct, int points,
                           List<String[]> prompts, Map<String, String> files,
                           int passedCount, int totalCount, List<String> failedTests,
                           int elapsedSec, int tokenUsagePercent, int testRunCount) {
    }

    private final AttemptRepository attemptRepository;
    private final AttemptFeedbackRepository feedbackRepository;
    private final AttemptFileRepository attemptFileRepository;
    private final PromptLogRepository promptLogRepository;
    private final TestRunRepository testRunRepository;
    private final DifficultySettingRepository difficultySettingRepository;
    private final GeminiClient geminiClient;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public FeedbackService(AttemptRepository attemptRepository,
                           AttemptFeedbackRepository feedbackRepository,
                           AttemptFileRepository attemptFileRepository,
                           PromptLogRepository promptLogRepository,
                           TestRunRepository testRunRepository,
                           DifficultySettingRepository difficultySettingRepository,
                           GeminiClient geminiClient,
                           TransactionTemplate transactionTemplate) {
        this.attemptRepository = attemptRepository;
        this.feedbackRepository = feedbackRepository;
        this.attemptFileRepository = attemptFileRepository;
        this.promptLogRepository = promptLogRepository;
        this.testRunRepository = testRunRepository;
        this.difficultySettingRepository = difficultySettingRepository;
        this.geminiClient = geminiClient;
        this.transactionTemplate = transactionTemplate;
    }

    public FeedbackResponse getFeedback(Long userId, Long attemptId) {
        // 1. 권한 확인 + 자료 읽기
        Context context = transactionTemplate.execute(status -> loadContext(userId, attemptId));

        // 2. 이미 만든 피드백이 있으면 그대로
        Optional<FeedbackResponse> saved = findSaved(attemptId);
        if (saved.isPresent()) {
            return saved.get();
        }

        // 3. AI로 만든다 (트랜잭션 밖)
        if (!geminiClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API 키가 설정되지 않아 피드백을 만들 수 없습니다.");
        }
        AiFeedback ai = callAi(context);
        FeedbackResponse response = build(context, ai);

        // 4. 저장. 같은 순간 두 번 요청되어 먼저 저장된 것이 있으면 그것을 쓴다
        try {
            AttemptFeedback entity = new AttemptFeedback();
            entity.setAttemptId(attemptId);
            entity.setSpecScore(response.scores().spec());
            entity.setEfficiencyScore(response.scores().efficiency());
            entity.setAccuracyScore(response.scores().accuracy());
            entity.setTotalScore(response.scores().total());
            entity.setMaxScore(response.scores().max());
            entity.setFeedbackJson(jsonMapper.writeValueAsString(response));
            entity.setModel(geminiClient.model());
            feedbackRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            return findSaved(attemptId).orElse(response);
        }
        return findSaved(attemptId).orElse(response.withCreatedAt(LocalDateTime.now()));
    }

    private Optional<FeedbackResponse> findSaved(Long attemptId) {
        return feedbackRepository.findById(attemptId).map(entity -> {
            try {
                return jsonMapper.readValue(entity.getFeedbackJson(), FeedbackResponse.class)
                        .withCreatedAt(entity.getCreatedAt());
            } catch (JacksonException e) {
                log.error("저장된 피드백을 읽지 못함: attemptId={}", attemptId, e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 피드백을 읽지 못했습니다.");
            }
        });
    }

    private Context loadContext(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findWithProblemByAttemptId(attemptId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() != AttemptStatus.PASSED && attempt.getStatus() != AttemptStatus.FAILED) {
            throw new ApiException(HttpStatus.CONFLICT, "제출한 응시만 피드백을 볼 수 있습니다.");
        }

        Problem problem = attempt.getProblem();
        DifficultySetting difficulty = difficultySettingRepository
                .findById(problem.getDifficultySetting().getDifficulty())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "난이도 설정을 찾을 수 없습니다."));
        int tokenLimit = Math.max(1, difficulty.getTokenLimit());

        List<String[]> prompts = promptLogRepository.findByAttemptAttemptIdOrderByCreatedAtAsc(attemptId).stream()
                .map(p -> new String[] {p.getPromptText(), p.getResponseText() == null ? "" : p.getResponseText()})
                .toList();

        Map<String, String> files = new LinkedHashMap<>();
        attemptFileRepository.findByIdAttemptId(attemptId).stream()
                .filter(file -> !file.getId().getFilePath().startsWith("tests/"))
                .sorted(Comparator.comparing(file -> file.getId().getFilePath()))
                .forEach(file -> files.put(file.getId().getFilePath(), file.getContent()));

        List<TestRun> runs = testRunRepository.findByAttemptAttemptIdOrderByCreatedAtAsc(attemptId);
        int testRunCount = (int) runs.stream().filter(run -> run.getRunType() == RunType.TEST).count();
        TestRun submit = runs.stream().filter(run -> run.getRunType() == RunType.SUBMIT)
                .reduce((first, second) -> second)
                .orElse(null);
        int passed = submit == null ? 0 : submit.getPassedCount();
        int total = submit == null ? 0 : submit.getTotalCount();
        List<String> failed = submit == null || submit.getOutputText() == null
                ? List.of()
                : TestOutputParser.parse(submit.getOutputText()).failedNames();

        return new Context(attemptId, problem.getProblemId(), problem.getTitle(),
                problem.getDifficultySetting().getDifficulty(), problem.getDescription(),
                parseConstraints(problem.getConstraintsText()), attempt.getStatus() == AttemptStatus.PASSED,
                attempt.getScore() == null ? 0 : attempt.getScore(), prompts, files, passed, total, failed,
                attempt.getElapsedSec(),
                (int) Math.min(100, Math.round(attempt.getTokensUsed() * 100.0 / tokenLimit)), testRunCount);
    }

    /** constraints.md의 "- 조건" 줄을 조건 목록으로 */
    private static List<String> parseConstraints(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.replaceFirst("^[-*]\\s*", ""))
                .toList();
    }

    private AiFeedback callAi(Context context) {
        GeminiClient.Result result;
        try {
            result = geminiClient.generate(SYSTEM_INSTRUCTION,
                    List.of(new GeminiClient.Turn("user", buildPrompt(context))), FEEDBACK_SCHEMA, 8192);
        } catch (AiException e) {
            log.warn("피드백 AI 호출 실패: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
        String json = result.text().trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return jsonMapper.readValue(json, AiFeedback.class);
        } catch (JacksonException e) {
            log.warn("피드백 AI 답변을 해석하지 못함: {}", result.text());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 피드백 형식이 올바르지 않습니다. 다시 시도하세요.");
        }
    }

    private static String buildPrompt(Context c) {
        StringBuilder text = new StringBuilder();
        text.append("## 문제: ").append(c.problemTitle()).append(" (").append(c.difficulty()).append(")\n")
                .append(c.description()).append("\n\n## 지킬 조건\n");
        for (int i = 0; i < c.constraints().size(); i++) {
            text.append(i + 1).append(". ").append(c.constraints().get(i)).append('\n');
        }

        text.append("\n## 응시자의 지시와 AI 답변 (").append(c.prompts().size()).append("개)\n");
        if (c.prompts().isEmpty()) {
            text.append("(AI를 쓰지 않고 직접 코드를 작성함)\n");
        }
        for (int i = 0; i < c.prompts().size(); i++) {
            text.append("\n### 지시 ").append(i + 1).append('\n').append(c.prompts().get(i)[0])
                    .append("\n### AI 답변 ").append(i + 1).append('\n').append(c.prompts().get(i)[1]).append('\n');
        }

        text.append("\n## 제출 코드\n");
        c.files().forEach((path, content) ->
                text.append("\n### ").append(path).append("\n```\n").append(content).append("\n```\n"));

        text.append("\n## 테스트 결과\n")
                .append("- 결과: ").append(c.correct() ? "정답" : "오답").append('\n')
                .append("- 통과: ").append(c.passedCount()).append(" / ").append(c.totalCount()).append('\n')
                .append("- 실패한 테스트: ").append(c.failedTests().isEmpty() ? "없음" : String.join(", ", c.failedTests()))
                .append("\n\n## 행동 지표\n")
                .append("- 지시 횟수: ").append(c.prompts().size()).append('\n')
                .append("- 소요 시간: ").append(c.elapsedSec()).append("초 (제한 1200초)\n")
                .append("- AI 사용량: ").append(c.tokenUsagePercent()).append("%\n")
                .append("- 제출 전 테스트 실행 횟수: ").append(c.testRunCount()).append('\n');
        return text.toString();
    }

    /** AI 판정과 서버 계산을 합쳐 응답을 만든다. 점수는 서버가 정한 공식으로만 계산한다 */
    private FeedbackResponse build(Context c, AiFeedback ai) {
        boolean usedAi = !c.prompts().isEmpty();

        // 조건 명시: 조건별 판정 (AI가 빠뜨린 번호는 "안 씀"으로 본다)
        List<ConstraintCheck> checks = new ArrayList<>();
        int mentioned = 0;
        for (int i = 0; i < c.constraints().size(); i++) {
            int index = i + 1;
            AiCheck check = ai.constraintChecks() == null ? null : ai.constraintChecks().stream()
                    .filter(item -> item != null && item.index() != null && item.index() == index)
                    .findFirst().orElse(null);
            boolean ok = usedAi && check != null && Boolean.TRUE.equals(check.mentioned());
            if (ok) {
                mentioned++;
            }
            checks.add(new ConstraintCheck(c.constraints().get(i), ok,
                    ok && check.evidence() != null ? check.evidence() : ""));
        }
        Integer specScore = !usedAi || c.constraints().isEmpty()
                ? null
                : MAX_SCORE_PER_ITEM * mentioned / c.constraints().size();

        Integer efficiencyScore = usedAi
                ? Math.max(0, Math.min(MAX_SCORE_PER_ITEM, ai.efficiencyScore() == null ? 0 : ai.efficiencyScore()))
                : null;

        int accuracyScore = c.correct()
                ? MAX_SCORE_PER_ITEM
                : c.totalCount() == 0 ? 0 : Math.min(MAX_SCORE_PER_ITEM - 1, MAX_SCORE_PER_ITEM * c.passedCount() / c.totalCount());

        int total = accuracyScore + (specScore == null ? 0 : specScore) + (efficiencyScore == null ? 0 : efficiencyScore);
        int max = MAX_SCORE_PER_ITEM * (1 + (specScore == null ? 0 : 1) + (efficiencyScore == null ? 0 : 1));

        List<PromptReview> reviews = new ArrayList<>();
        if (usedAi && ai.promptReviews() != null) {
            for (AiReview review : ai.promptReviews()) {
                if (review == null || review.promptIndex() == null) {
                    continue;
                }
                int index = review.promptIndex();
                if (index < 1 || index > c.prompts().size()) {
                    continue;
                }
                reviews.add(new PromptReview(index, c.prompts().get(index - 1)[0],
                        "GOOD".equals(review.verdict()) ? "GOOD" : "IMPROVE",
                        nullToEmpty(review.comment()), nullToEmpty(review.betterPrompt())));
            }
            reviews.sort(Comparator.comparingInt(PromptReview::index));
        }

        AiLesson lesson = ai.lesson() == null ? new AiLesson("", "", "", "") : ai.lesson();
        String noAi = "AI를 쓰지 않고 직접 코드를 작성해 평가하지 않았습니다.";

        return new FeedbackResponse(c.attemptId(), c.problemId(), c.problemTitle(), c.difficulty(), c.correct(),
                c.points(), nullToEmpty(ai.headline()),
                new Behavior(c.prompts().size(), c.elapsedSec(), c.tokenUsagePercent(), c.testRunCount()),
                new Scores(specScore, efficiencyScore, accuracyScore, total, max),
                new Spec(specScore, mentioned, c.constraints().size(), checks,
                        usedAi ? nullToEmpty(ai.specFeedback()) : noAi),
                new Efficiency(efficiencyScore, usedAi ? nullToEmpty(ai.efficiencyFeedback()) : noAi, reviews),
                new Accuracy(accuracyScore, c.passedCount(), c.totalCount(), c.failedTests(),
                        nullToEmpty(ai.accuracyFeedback())),
                new Lesson(nullToEmpty(lesson.title()), nullToEmpty(lesson.nextTime()),
                        nullToEmpty(lesson.codeProblem()), nullToEmpty(lesson.whyItMatters())),
                geminiClient.model(), null);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
