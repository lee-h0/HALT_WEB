package com.halt.halt_web.ai;

import com.halt.halt_web.ai.dto.PromptRequest;
import com.halt.halt_web.ai.dto.PromptResponse;
import com.halt.halt_web.attempt.dto.TestRunRequest.FileContent;
import com.halt.halt_web.common.ApiException;
import com.halt.halt_web.domain.Attempt;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.PromptLog;
import com.halt.halt_web.problem.dto.ProblemFileResponse;
import com.halt.halt_web.repository.AttemptRepository;
import com.halt.halt_web.repository.DifficultySettingRepository;
import com.halt.halt_web.repository.PromptLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 지시창. 응시자의 지시와 작업 폴더 파일을 Gemini에 보내고, AI가 바꾼 파일을 돌려준다.
 *
 * 중요한 설계
 * - AI에게는 문제 설명과 "지킬 조건"을 주지 않는다. 조건은 응시자가 지시에 직접 써야 한다.
 *   (이 플랫폼이 평가하는 "정확한 조건 제시" 능력)
 * - 숨김 테스트는 보내지 않는다. 응시자 화면에 있는 파일만 보낸다.
 * - AI는 지시받은 것만 구현하도록 한다. 지시에 없는 규칙을 알아서 채워 넣지 않게 해서,
 *   조건을 빠뜨린 지시에서는 "그럴듯하지만 틀린" 코드가 나오게 한다.
 */
@Service
public class AiPromptService {

    private static final Logger log = LoggerFactory.getLogger(AiPromptService.class);

    /** 이전 대화는 최근 몇 번까지 AI에게 다시 보낼지 (토큰을 아끼기 위해 제한) */
    private static final int HISTORY_TURNS = 5;

    private static final String SYSTEM_INSTRUCTION = """
            너는 코드 작성 도우미다. 사용자의 작업 폴더에 있는 파일 내용과 지시가 주어진다.
            규칙:
            1. 사용자가 지시한 내용만 구현하거나 수정한다. 지시에 없는 요구사항, 예외 처리, 제약을 추측해서 추가하지 않는다.
            2. 바꾸거나 새로 만든 파일만 files에 넣고, 각 파일은 일부가 아니라 전체 내용을 넣는다.
            3. 코드를 바꿀 필요가 없는 질문이면 files는 빈 배열로 둔다.
            4. 파일 경로는 주어진 경로를 그대로 쓴다. 새 파일은 작업 폴더 기준 상대 경로로 쓴다.
            5. reply에는 무엇을 했는지 한국어로 2~4문장으로 짧게 쓴다.
            6. 외부 라이브러리는 쓰지 않는다. 실행 환경은 Node.js이고 테스트는 node --test로 실행된다.
            """;

    private final AttemptRepository attemptRepository;
    private final PromptLogRepository promptLogRepository;
    private final DifficultySettingRepository difficultySettingRepository;
    private final GeminiClient geminiClient;
    private final AiUsageRecorder recorder;
    /** AI가 돌려준 JSON 문자열을 읽는 데 쓴다 */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public AiPromptService(AttemptRepository attemptRepository,
                           PromptLogRepository promptLogRepository,
                           DifficultySettingRepository difficultySettingRepository,
                           GeminiClient geminiClient,
                           AiUsageRecorder recorder) {
        this.attemptRepository = attemptRepository;
        this.promptLogRepository = promptLogRepository;
        this.difficultySettingRepository = difficultySettingRepository;
        this.geminiClient = geminiClient;
        this.recorder = recorder;
    }

    /** AI 응답 JSON의 모양 */
    public record AiOutput(String reply, List<AiFile> files) {
    }

    public record AiFile(String path, String content) {
    }

    public PromptResponse prompt(Long userId, Long attemptId, PromptRequest request) {
        if (!geminiClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API 키가 설정되지 않았습니다. 환경 변수 GEMINI_API_KEY를 넣고 서버를 다시 실행하세요.");
        }

        Attempt attempt = attemptRepository.findWithProblemByAttemptId(attemptId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다."));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 응시입니다. 문제를 다시 시작하세요.");
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (FileContent file : request.files()) {
            files.put(file.path().replace('\\', '/'), file.content());
        }

        List<GeminiClient.Turn> turns = new ArrayList<>(history(attemptId));
        turns.add(new GeminiClient.Turn("user", buildUserMessage(files, request.prompt().trim())));

        GeminiClient.Result result;
        try {
            result = geminiClient.generate(SYSTEM_INSTRUCTION, turns);
        } catch (AiException e) {
            log.warn("Gemini 호출 실패: {}", e.getMessage(), e.getCause());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }

        AiOutput output = parse(result.text());
        List<ProblemFileResponse> changed = new ArrayList<>();
        if (output.files() != null) {
            for (AiFile file : output.files()) {
                if (file == null || file.path() == null || file.content() == null) {
                    continue;
                }
                String path = normalizePath(file.path());
                // 경로가 이상하거나, 내용이 그대로인 파일은 변경으로 치지 않는다
                if (path == null || Objects.equals(files.get(path), file.content())) {
                    continue;
                }
                changed.add(new ProblemFileResponse(path, file.content()));
            }
        }

        String reply = output.reply() == null || output.reply().isBlank() ? "작업을 마쳤습니다." : output.reply().trim();
        String logText = changed.isEmpty()
                ? reply
                : reply + "\n\n[변경한 파일] " + String.join(", ", changed.stream().map(ProblemFileResponse::path).toList());

        Attempt updated = recorder.record(attemptId, request.prompt().trim(), logText, geminiClient.model(),
                result.inputTokens(), result.outputTokens(), result.totalTokens(), changed.size());

        int tokenLimit = difficultySettingRepository.findById(attempt.getProblem().getDifficultySetting().getDifficulty())
                .map(setting -> setting.getTokenLimit())
                .orElse(1);
        int percent = (int) Math.min(100, Math.round(updated.getTokensUsed() * 100.0 / tokenLimit));

        return new PromptResponse(reply, changed, result.inputTokens(), result.outputTokens(),
                updated.getTokensUsed(), percent, updated.getPromptCount(), updated.getFileChangeCount());
    }

    /** 이전 지시와 답변 (파일 내용은 빼고 보낸다. 최신 파일은 이번 메시지에 들어 있다) */
    private List<GeminiClient.Turn> history(Long attemptId) {
        List<PromptLog> logs = promptLogRepository.findByAttemptAttemptIdOrderByCreatedAtAsc(attemptId);
        List<PromptLog> recent = logs.subList(Math.max(0, logs.size() - HISTORY_TURNS), logs.size());
        List<GeminiClient.Turn> turns = new ArrayList<>();
        for (PromptLog entry : recent) {
            turns.add(new GeminiClient.Turn("user", entry.getPromptText()));
            turns.add(new GeminiClient.Turn("model", entry.getResponseText() == null ? "" : entry.getResponseText()));
        }
        return turns;
    }

    private static String buildUserMessage(Map<String, String> files, String prompt) {
        StringBuilder message = new StringBuilder("## 작업 폴더의 현재 파일\n");
        files.forEach((path, content) -> message
                .append("\n### ").append(path).append("\n```\n").append(content).append("\n```\n"));
        message.append("\n## 지시\n").append(prompt);
        return message.toString();
    }

    private AiOutput parse(String text) {
        String json = text.trim();
        // 혹시 코드 블록으로 감싸서 오면 벗겨 낸다
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return jsonMapper.readValue(json, AiOutput.class);
        } catch (JacksonException e) {
            log.warn("AI 응답을 해석하지 못함: {}", text);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 답변 형식이 올바르지 않습니다. 다시 시도하세요.");
        }
    }

    /** 작업 폴더 밖이나 절대 경로는 거부한다 (null 반환) */
    private static String normalizePath(String rawPath) {
        String path = rawPath.trim().replace('\\', '/');
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        boolean unsafe = path.isEmpty()
                || path.length() > 260
                || path.startsWith("/")
                || path.contains(":")
                || path.contains("//")
                || path.equals("..")
                || path.startsWith("../")
                || path.contains("/../")
                || path.endsWith("/..")
                || path.endsWith("/");
        return unsafe ? null : path;
    }
}
