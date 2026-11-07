package com.halt.halt_web.mypage;

import com.halt.halt_web.ai.GeminiClient;
import com.halt.halt_web.domain.AttemptStatus;
import com.halt.halt_web.domain.DailyRecommendation;
import com.halt.halt_web.domain.DailyRecommendationId;
import com.halt.halt_web.domain.Problem;
import com.halt.halt_web.mypage.dto.RecommendationResponse;
import com.halt.halt_web.repository.DailyRecommendationRepository;
import com.halt.halt_web.repository.MyPageQueryRepository;
import com.halt.halt_web.repository.UserProblemBestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 추천 오늘의 문제.
 * - 회원마다 하루(한국 시간) 1번만 만들고 daily_recommendations에 저장해 그날은 다시 쓴다.
 * - 후보는 아직 못 푼 문제. Gemini가 내 기록을 보고 하나를 고르고 이유를 쓴다.
 * - Gemini 키가 없거나 호출이 실패하면 규칙(실패했던 문제 → 쉬운 난이도 순)으로 대신 고른다.
 *
 * AI 호출은 몇 초 걸릴 수 있으므로 이 클래스는 트랜잭션을 길게 잡지 않는다.
 * (조회는 join fetch로 한 번에 읽고, 저장은 저장소 메서드 하나로 끝낸다)
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<AttemptStatus> SUBMITTED = List.of(AttemptStatus.PASSED, AttemptStatus.FAILED);
    private static final int REASON_MAX = 300;

    private static final String SYSTEM_INSTRUCTION = """
            너는 AI 코드 검증 역량 평가 플랫폼의 학습 코치다.
            회원의 풀이 기록과 아직 못 푼 문제 후보 목록이 주어진다.
            후보 중에서 오늘 풀기 가장 좋은 문제 하나를 고르고, 그 이유를 회원에게 말하듯 한국어 1~2문장(80자 이내)으로 쓴다.
            규칙:
            1. problemId는 반드시 후보 목록에 있는 번호만 쓴다.
            2. 실력에 비해 너무 어렵거나 쉬운 문제는 피한다. 처음이면 쉬운 문제를, 실패했던 문제가 있으면 다시 도전을 권할 수 있다.
            3. 이유에는 문제의 정답이나 풀이 방법을 쓰지 않는다.
            """;

    private static final Map<String, Object> PICK_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "problemId", Map.of("type", "INTEGER"),
                    "reason", Map.of("type", "STRING")),
            "required", List.of("problemId", "reason"));

    /** AI 답변 JSON 모양 */
    public record AiPick(Long problemId, String reason) {
    }

    /** 문제별 내 제출 결과 */
    private record Stat(long passed, long failed) {
        long submitted() {
            return passed + failed;
        }
    }

    private final MyPageQueryRepository queryRepository;
    private final DailyRecommendationRepository recommendationRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final GeminiClient geminiClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public RecommendationService(MyPageQueryRepository queryRepository,
                                 DailyRecommendationRepository recommendationRepository,
                                 UserProblemBestRepository userProblemBestRepository,
                                 GeminiClient geminiClient) {
        this.queryRepository = queryRepository;
        this.recommendationRepository = recommendationRepository;
        this.userProblemBestRepository = userProblemBestRepository;
        this.geminiClient = geminiClient;
    }

    public RecommendationResponse today(Long userId) {
        LocalDate today = LocalDate.now(SEOUL);
        Set<Long> solvedIds = userProblemBestRepository.findByIdUserId(userId).stream()
                .map(best -> best.getId().getProblemId())
                .collect(Collectors.toSet());

        // 1. 오늘 이미 만든 추천이 있으면 그대로 쓴다
        Optional<DailyRecommendation> saved = queryRepository.findRecommendation(userId, today);
        if (saved.isPresent() && saved.get().getProblem().isActive()) {
            return toResponse(saved.get().getProblem(), saved.get().getReason(), solvedIds);
        }

        // 2. 후보: 공개 중인 문제 중 아직 못 푼 문제
        List<Problem> problems = queryRepository.findActiveProblemsWithDifficulty();
        if (problems.isEmpty()) {
            return RecommendationResponse.none("아직 등록된 문제가 없습니다.");
        }
        List<Problem> candidates = problems.stream().filter(p -> !solvedIds.contains(p.getProblemId())).toList();
        if (candidates.isEmpty()) {
            // 저장하지 않는다. 오늘 새 문제가 추가되면 그때 추천한다
            return RecommendationResponse.none("모든 문제를 풀었습니다! 새 문제가 추가되면 추천해 드릴게요.");
        }

        Map<Long, Stat> stats = loadStats(userId);
        Pick pick = pickWithAi(candidates, problems, solvedIds, stats)
                .orElseGet(() -> pickByRule(candidates, stats));

        // 3. 저장. 같은 순간 두 번 요청되어 먼저 저장된 추천이 있으면 그것을 쓴다
        if (saved.isEmpty()) {
            try {
                DailyRecommendation recommendation = new DailyRecommendation();
                recommendation.setId(new DailyRecommendationId(userId, today));
                recommendation.setProblem(pick.problem());
                recommendation.setReason(pick.reason());
                recommendationRepository.saveAndFlush(recommendation);
            } catch (DataIntegrityViolationException e) {
                Optional<DailyRecommendation> existing = queryRepository.findRecommendation(userId, today);
                if (existing.isPresent()) {
                    return toResponse(existing.get().getProblem(), existing.get().getReason(), solvedIds);
                }
            }
        } else {
            // 저장된 추천의 문제가 비공개로 바뀐 경우: 새 추천으로 바꿔 둔다
            DailyRecommendation recommendation = saved.get();
            recommendation.setProblem(pick.problem());
            recommendation.setReason(pick.reason());
            recommendationRepository.save(recommendation);
        }
        return toResponse(pick.problem(), pick.reason(), solvedIds);
    }

    private record Pick(Problem problem, String reason) {
    }

    private Map<Long, Stat> loadStats(Long userId) {
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : queryRepository.countSubmissionsByProblem(userId, SUBMITTED)) {
            Long problemId = (Long) row[0];
            AttemptStatus status = (AttemptStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] pair = counts.computeIfAbsent(problemId, id -> new long[2]);
            pair[status == AttemptStatus.PASSED ? 0 : 1] += count;
        }
        Map<Long, Stat> stats = new HashMap<>();
        counts.forEach((id, pair) -> stats.put(id, new Stat(pair[0], pair[1])));
        return stats;
    }

    /** Gemini에게 고르게 한다. 키가 없거나 실패하거나 엉뚱한 번호를 주면 빈 값 */
    private Optional<Pick> pickWithAi(List<Problem> candidates, List<Problem> all, Set<Long> solvedIds,
                                      Map<Long, Stat> stats) {
        if (!geminiClient.isConfigured()) {
            return Optional.empty();
        }
        try {
            GeminiClient.Result result = geminiClient.generate(SYSTEM_INSTRUCTION,
                    List.of(new GeminiClient.Turn("user", buildPrompt(candidates, all, solvedIds, stats))),
                    PICK_SCHEMA, 2048);
            AiPick aiPick = jsonMapper.readValue(stripFence(result.text()), AiPick.class);
            Optional<Problem> chosen = candidates.stream()
                    .filter(p -> p.getProblemId().equals(aiPick.problemId()))
                    .findFirst();
            if (chosen.isEmpty() || aiPick.reason() == null || aiPick.reason().isBlank()) {
                log.warn("AI 추천 결과를 쓸 수 없어 규칙으로 추천: {}", result.text());
                return Optional.empty();
            }
            return Optional.of(new Pick(chosen.get(), truncate(aiPick.reason().trim())));
        } catch (RuntimeException e) {
            log.warn("AI 추천 실패, 규칙으로 추천: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String buildPrompt(List<Problem> candidates, List<Problem> all, Set<Long> solvedIds,
                                      Map<Long, Stat> stats) {
        long submitted = stats.values().stream().mapToLong(Stat::submitted).sum();
        long passed = stats.values().stream().mapToLong(Stat::passed).sum();
        Map<String, Long> solvedByDifficulty = all.stream()
                .filter(p -> solvedIds.contains(p.getProblemId()))
                .collect(Collectors.groupingBy(p -> p.getDifficultySetting().getDifficulty(), Collectors.counting()));

        StringBuilder text = new StringBuilder("## 회원 기록\n");
        text.append("- 푼 문제: ").append(solvedIds.size()).append("개 ")
                .append(solvedByDifficulty.isEmpty() ? "(없음)" : solvedByDifficulty.toString()).append('\n');
        text.append("- 제출: ").append(submitted).append("회, 정답: ").append(passed).append("회\n");
        text.append("\n## 후보 (아직 못 푼 문제)\n");
        for (Problem problem : candidates) {
            Stat stat = stats.getOrDefault(problem.getProblemId(), new Stat(0, 0));
            text.append("- problemId=").append(problem.getProblemId())
                    .append(", 제목=").append(problem.getTitle())
                    .append(", 난이도=").append(problem.getDifficultySetting().getDifficulty())
                    .append(", 만점=").append(problem.getDifficultySetting().getMaxPoints()).append("P")
                    .append(", 내 제출=").append(stat.submitted()).append("회(오답 ").append(stat.failed()).append("회)\n");
        }
        return text.toString();
    }

    /** 규칙: 실패했던 문제 먼저 → 쉬운 난이도(만점이 낮은) 순 → 번호 순 */
    private static Pick pickByRule(List<Problem> candidates, Map<Long, Stat> stats) {
        Problem problem = candidates.stream()
                .min(Comparator
                        .comparing((Problem p) -> stats.getOrDefault(p.getProblemId(), new Stat(0, 0)).failed() == 0)
                        .thenComparingInt(p -> p.getDifficultySetting().getMaxPoints())
                        .thenComparing(Problem::getProblemId))
                .orElseThrow();
        boolean retried = stats.getOrDefault(problem.getProblemId(), new Stat(0, 0)).failed() > 0;
        String reason = retried
                ? "지난번에 아쉽게 놓친 문제예요. 지킬 조건을 하나씩 지시에 넣어 다시 도전해 보세요."
                : "아직 도전하지 않은 " + problem.getDifficultySetting().getDifficulty() + " 문제예요. 가볍게 시작해 보세요.";
        return new Pick(problem, reason);
    }

    private static RecommendationResponse toResponse(Problem problem, String reason, Set<Long> solvedIds) {
        return new RecommendationResponse(problem.getProblemId(), problem.getTitle(),
                problem.getDifficultySetting().getDifficulty(), problem.getDifficultySetting().getMaxPoints(),
                reason, solvedIds.contains(problem.getProblemId()), null);
    }

    private static String stripFence(String text) {
        String json = text.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return json;
    }

    private static String truncate(String text) {
        return text.length() <= REASON_MAX ? text : text.substring(0, REASON_MAX);
    }
}
