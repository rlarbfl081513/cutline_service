package com.ssafya408.cutlineparsing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import com.ssafya408.cutlineparsing.service.dto.StrategyRecommendation;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelationshipStrategyServiceImpl implements RelationshipStrategyService {

    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private record StrategyResponse(String interestStrategy, String uninterestStrategy, String maintainStrategy) {}

    @Override
    public StrategyRecommendation recommend(Person person, List<MonthlySummarySnapshot> summaries, MonthlyComputationContext latestContext) {
        Objects.requireNonNull(person, "person must not be null");
        Objects.requireNonNull(latestContext, "latestContext must not be null");

        // 전략 생성 시작 로깅
        log.info("전략 생성 시작: personId={}, summaries={}", person.getId(), summaries != null ? summaries.size() : 0);

        String prompt = buildUserPrompt(person, summaries, latestContext);

        try {
            String content = chatClient.prompt()
                    .system(systemPrompt())
                    .user(prompt)
                    .call()
                    .content();

            StrategyResponse response = objectMapper.readValue(extractJson(content), StrategyResponse.class);
            StrategyRecommendation result = new StrategyRecommendation(
                    response.interestStrategy() == null ? "" : response.interestStrategy().trim(),
                    response.uninterestStrategy() == null ? "" : response.uninterestStrategy().trim(),
                    response.maintainStrategy() == null ? "" : response.maintainStrategy().trim()
            );
            
            // 전략 생성 완료 로깅
            log.info("전략 생성 완료: personId={}, strategies=3", person.getId());
            
            return result;
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("전략 추천 실패: personId={} - {}", person.getId(), e.getMessage());
            return StrategyRecommendation.empty();
        }
    }

    private String buildUserPrompt(Person person, List<MonthlySummarySnapshot> summaries, MonthlyComputationContext latestContext) {
        String relation = relationLabel(person.getRelation());
        String status = person.getStatus() == null ? "" : person.getStatus().name();
        StringBuilder sb = new StringBuilder();

        sb.append("You are advising on relationship strategies for the user.\n");
        sb.append("Relationship type: ").append(relation).append("\n");
        sb.append("Current status: ").append(status).append("\n");
        sb.append("Connection duration (months): ").append(person.getDuration()).append("\n\n");

        sb.append("Recent monthly summaries (latest first):\n");
        if (summaries != null && !summaries.isEmpty()) {
            summaries.stream()
                    .sorted((a, b) -> b.yearMonth().compareTo(a.yearMonth()))
                    .limit(12)
                    .forEach(summary -> sb.append("- Month ")
                            .append(summary.yearMonth().format(YM_FORMAT))
                            .append(": feedback=")
                            .append(summary.feedback() == null ? "" : summary.feedback())
                            .append(", topics=")
                            .append(summary.topicsJson())
                            .append(", issues=")
                            .append(summary.issuesJson())
                            .append("\n"));
        } else {
            sb.append("(no history)\n");
        }

        sb.append("\nLatest month detail: ")
                .append(latestContext.yearMonth().format(YM_FORMAT))
                .append("\n");
        sb.append("Manual stats: ")
                .append(latestContext.manualStats() == null ? "{}" : latestContext.manualStats().toString())
                .append("\n");
        sb.append("Auto analysis JSON: ");
        try {
            sb.append(objectMapper.writeValueAsString(latestContext.autoAnalysis()));
        } catch (JsonProcessingException e) {
            sb.append(latestContext.autoAnalysis());
        }
        sb.append("\nConversation DSL:\n")
                .append(latestContext.monthOutput().dsl());

        return sb.toString();
    }

    private String relationLabel(PersonRelation relation) {
        if (relation == null) {
            return "COWORKER";
        }
        return switch (relation) {
            case LOVER -> "LOVER";
            case FRIEND -> "FRIEND";
            case COWORKER -> "COWORKER";
        };
    }

    private String systemPrompt() {
        return String.join("\n",
                "You are a relationship strategist.",
                "Respond strictly in Korean (Hangul) for all text values, while keeping JSON field names in English.",
                "Return ONLY valid JSON (no markdown, no comments, no extra text) with this exact schema:",
                "",
                "{",
                "\"interestStrategy\": string,",
                "\"uninterestStrategy\": string,",
                "\"maintainStrategy\": string",
                "}",
                "",
                "Formatting rules (must follow all):",
                "",
                "Each field value is a single multiline Korean string composed of exactly 4 lines separated by \\n.",
                "",
                "No bullets, no numbering, no emojis, no extra punctuation (avoid trailing periods); write concise imperative actions per line.",
                "",
                "No leading/trailing spaces and no blank lines at start or end of the string.",
                "",
                "Lines should be practical, actionable behaviors the user can do right away.",
                "",
                "Keep lines short (ideally 6–16 Korean characters each), and avoid duplicating the same action across lines.",
                "",
                "Do not add or remove JSON keys. If unsure, still output valid JSON with empty strings.",
                "",
                "Content rules:",
                "",
                "interestStrategy: Actions to increase closeness and shared engagement (e.g., regular meetups, thoughtful milestones, shared hobbies).",
                "Style examples (not to output verbatim):",
                "월 2회 개인적 만남 / 생일·기념 선물 챙기기(5~7만원) / 공통 관심사 활동 함께 / 소소한 일상 자주 공유",
                "",
                "uninterestStrategy: Actions for low interest / distance situations (e.g., light touch, low-pressure check-ins, boundary-respecting).",
                "Style examples:",
                "격주 간단 안부 남기기 / 부담 없는 링크 공유 / 요청 전 선택지 제시 / 과한 연락 자제",
                "",
                "maintainStrategy: Actions to stabilize and sustain a healthy relationship (e.g., rhythm, reciprocity, predictable care).",
                "Style examples:",
                "월간 일정 한 번 맞추기 / 작은 도움 즉시 보답 / 분기 1회 깊은 대화 / 중요 일정 미리 축하",
                "",
                "Output constraints:",
                "",
                "Output must start with { and end with }.",
                "",
                "Field names must remain exactly: interestStrategy, uninterestStrategy, maintainStrategy.",
                "",
                "Values must be Korean multiline strings as specified; do not include backticks or markdown fences."
        );
    }

    private String extractJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            if (trimmed.endsWith("```")) {
                String inner = trimmed.substring(3, trimmed.length() - 3).trim();
                return inner.isEmpty() ? "{}" : inner;
            }
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace >= firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }
}
