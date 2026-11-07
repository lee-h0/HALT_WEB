package com.halt.halt_web.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gemini API (generateContent) 호출.
 * 응답은 우리가 정한 JSON 형식으로 받도록 responseSchema를 지정한다.
 * - AI 지시창: CODE_SCHEMA ({"reply": ..., "files": [...]})
 * - 다른 기능(오늘의 추천 등)은 자기 스키마를 넘겨 쓴다.
 */
@Component
public class GeminiClient {

    /** 한 번의 대화 턴 */
    public record Turn(String role, String text) {
    }

    /** 호출 결과. text는 모델이 돌려준 JSON 문자열 */
    public record Result(String text, int inputTokens, int outputTokens, int totalTokens) {
    }

    /** AI 지시창 응답 형식 */
    public static final Map<String, Object> CODE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "reply", Map.of("type", "STRING"),
                    "files", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "path", Map.of("type", "STRING"),
                                            "content", Map.of("type", "STRING")),
                                    "required", List.of("path", "content")))),
            "required", List.of("reply", "files"));

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${app.ai.api-key:}") String apiKey,
                        @Value("${app.ai.model:gemini-3.5-flash-lite}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * @param systemInstruction AI의 역할과 규칙
     * @param turns             이전 대화 + 이번 지시 (마지막이 이번 지시)
     */
    public Result generate(String systemInstruction, List<Turn> turns) {
        return generate(systemInstruction, turns, CODE_SCHEMA, 8192);
    }

    /**
     * @param responseSchema  돌려받을 JSON 형식 (Gemini responseSchema 문법)
     * @param maxOutputTokens 답변 최대 길이 (생각 토큰 포함)
     */
    @SuppressWarnings("unchecked")
    public Result generate(String systemInstruction, List<Turn> turns,
                           Map<String, Object> responseSchema, int maxOutputTokens) {
        List<Map<String, Object>> contents = turns.stream()
                .map(turn -> Map.<String, Object>of(
                        "role", turn.role(),
                        "parts", List.of(Map.of("text", turn.text()))))
                .toList();

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", contents,
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema,
                        "maxOutputTokens", maxOutputTokens,
                        "temperature", 0.4));

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new AiException(describe(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new AiException("AI 서버에 연결하지 못했습니다. 인터넷 연결을 확인하세요.", e);
        }
        if (response == null) {
            throw new AiException("AI가 응답하지 않았습니다.");
        }

        String text = extractText(response);
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usageMetadata", Map.of());
        int input = number(usage.get("promptTokenCount"));
        int output = number(usage.get("candidatesTokenCount")) + number(usage.get("thoughtsTokenCount"));
        int total = number(usage.get("totalTokenCount"));
        if (total == 0) {
            total = input + output;
        }
        return new Result(text, input, output, total);
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new AiException("AI가 답변을 만들지 못했습니다. 지시를 바꿔 다시 시도하세요.");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw new AiException("AI 답변이 비어 있습니다. (" + candidates.get(0).get("finishReason") + ")");
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.getOrDefault("parts", List.of());
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> part : parts) {
            // 생각 과정(thought) 부분은 건너뛴다
            if (Boolean.TRUE.equals(part.get("thought"))) {
                continue;
            }
            Object value = part.get("text");
            if (value != null) {
                text.append(value);
            }
        }
        if (text.isEmpty()) {
            throw new AiException("AI 답변이 비어 있습니다.");
        }
        return text.toString();
    }

    private static int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static String describe(int status) {
        return switch (status) {
            case 400 -> "AI 요청 형식이 잘못되었습니다. 설정한 모델 이름(app.ai.model)을 확인하세요.";
            case 401, 403 -> "Gemini API 키가 올바르지 않습니다. 환경 변수 GEMINI_API_KEY를 확인하세요.";
            case 404 -> "AI 모델을 찾을 수 없습니다. 설정한 모델 이름(app.ai.model)을 확인하세요.";
            case 429 -> "Gemini 무료 사용 한도에 도달했습니다. 잠시 뒤에 다시 시도하세요.";
            default -> "AI 서버에서 오류가 났습니다. (HTTP " + status + ") 잠시 뒤에 다시 시도하세요.";
        };
    }
}
