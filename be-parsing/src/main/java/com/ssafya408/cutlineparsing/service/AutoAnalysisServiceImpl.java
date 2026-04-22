package com.ssafya408.cutlineparsing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisRequest;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.service.dto.AutoStatsPayload;
import com.ssafya408.cutlineparsing.service.dto.IssuePayload;
import com.ssafya408.cutlineparsing.service.dto.TopicPayload;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
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
public class AutoAnalysisServiceImpl implements AutoAnalysisService {

    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public AutoAnalysisResult analyze(MonthOutput monthOutput, MonthlyStatsResult stats, String userDisplayName, String friendDisplayName) {
        Objects.requireNonNull(monthOutput, "monthOutput must not be null");
        Objects.requireNonNull(stats, "stats must not be null");

        // AI 분석 시작 로깅
        log.info("AI 분석 시작: month={}, userDisplayName={}, friendDisplayName={}", 
                monthOutput.ym().format(YM_FORMAT), userDisplayName, friendDisplayName);

        AutoAnalysisRequest request = new AutoAnalysisRequest(
                monthOutput.ym().format(YM_FORMAT),
                userDisplayName,
                friendDisplayName,
                monthOutput.dsl(),
                stats.fMsgCount(),
                stats.fMsgChars(),
                stats.activeDaysCount(),
                stats.maxNoChatStreakDays(),
                stats.fReplyMinutesSum(),
                stats.fReplyPairsCount()
        );

        String rawResponse;
        try {
            rawResponse = chatClient.prompt()
                    .system(systemPrompt())
                    .user(buildUserPrompt(request))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            log.warn("LLM 자동 분석 호출 실패: {}", ex.getMessage());
            return AutoAnalysisResult.empty();
        }

        String jsonPayload = extractJson(rawResponse);
        try {
            AutoAnalysisResponse parsed = objectMapper.readValue(jsonPayload, AutoAnalysisResponse.class);
            AutoAnalysisResult result = new AutoAnalysisResult(
                    parsed.autoStats() == null ? AutoStatsPayload.empty() : parsed.autoStats(),
                    parsed.topics() == null ? List.of() : parsed.topics(),
                    parsed.issues() == null ? List.of() : parsed.issues(),
                    parsed.feedback() == null ? "" : parsed.feedback()
            );
            
            // AI 분석 완료 로깅
            log.info("AI 분석 완료: month={}, topics={}, issues={}, feedback={}", 
                    monthOutput.ym().format(YM_FORMAT), 
                    result.topics().size(), 
                    result.issues().size(), 
                    result.feedback());
            
            return result;
        } catch (JsonProcessingException ex) {
            log.warn("LLM 자동 분석 응답 파싱 실패: {}", ex.getMessage());
            return AutoAnalysisResult.empty();
        }
    }

    private String buildUserPrompt(AutoAnalysisRequest request) {
        return "You are given a month of KakaoTalk conversation exported as a DSL.\n" +
                "Please infer the relationship dynamics and output JSON only.\n" +
                "Month: " + request.month() + "\n" +
                "User (U): " + request.userDisplayName() + "\n" +
                "Friend (F): " + request.friendDisplayName() + "\n" +
                "Friend message count: " + request.friendMessageCount() + "\n" +
                "Friend message chars: " + request.friendMessageChars() + "\n" +
                "Active days: " + request.activeDays() + "\n" +
                "Max silent streak days: " + request.maxSilentStreak() + "\n" +
                "Total reply minutes from U to F: " + request.replyMinutesSum() + "\n" +
                "Reply pair count: " + request.replyPairsCount() + "\n" +
                "Conversation DSL (each line is speaker | deltaMinutes | \"text\"):\n" +
                request.dsl();
    }

    private String systemPrompt() {
        return String.join("\n",
                "You are an expert analyst of KakaoTalk chat logs.",
                "Respond strictly in Korean (Hangul) while keeping numeric fields as numbers.",
                "Return ONLY valid JSON matching this schema:",
                "{",
                "  \"autoStats\": {",
                "    \"startChat\": number,",
                "    \"question\": number,",
                "    \"privateStory\": number,",
                "    \"positiveReaction\": number,",
                "    \"getHelp\": number,",
                "    \"meetingSuccess\": number,",
                "    \"noResponse\": number,",
                "    \"giveHelp\": number,",
                "    \"attack\": number,",
                "    \"meetingRejection\": number",
                "  },",
                "  \"topics\": [",
                "    { \"topic\": string, \"count\": number }  // maximum of 3 entries",
                "  ],",
                "  \"issues\": [",
                "    { \"category\": one of [CONFLICT, RECONCILIATION, TOUCHING, OTHER], \"summary\": string }  // maximum of 5 entries",
                "  ],",
                "  \"feedback\": string  // single-sentence monthly summary",
                "}",
                "Do not include markdown fences, commentary, or additional text.",
                "If information is missing, default to 0 or empty arrays.",
                "",
                "General Rules",
                "Output must start with { and end with }, with all fields present exactly as in the schema.",
                "If information is missing: use 0 for numbers, [] for arrays, \"\" for strings.",
                "Do not add or remove keys, and do not change key names.",
                "topics: maximum 3 entries, each must be a short noun keyword (1–2 words) representing conversation themes or interests, e.g., 영화, 신발, 테니스.",
                "issues: maximum 5 entries, category must be exactly one of [CONFLICT, RECONCILIATION, TOUCHING, OTHER].",
                "feedback: exactly one Korean sentence, ending with \"관계입니다.\"",
                "",
                "Labeling Rules",
                "Priority (if multiple labels apply):",
                "attack > meetingRejection > meetingSuccess > getHelp > giveHelp > question > privateStory > positiveReaction > startChat.",
                "noResponse is evaluated separately.",
                "startChat: Do not rely only on time gap. Mark as 1 only when context clearly shows a new session (long silence, date header, or explicit closing like \"잘자/내일 얘기하자\"), and the new message introduces a new topic or greeting.",
                "question: Count only if the intent to request information or decision is explicit and clear.",
                "privateStory: Count only if the speaker shares personal life, emotions, health, finances, or schedule.",
                "positiveReaction: Count short expressions of thanks, praise, joy, or agreement (e.g., \"고마워\", \"좋아\"), excluding neutral confirmations.",
                "getHelp: Count when asking for an action or resource. If ambiguous with question, prioritize getHelp.",
                "giveHelp: Count when the speaker provides or confirms delivery of requested resources (files, links, answers, money, etc.).",
                "meetingSuccess: Count only when meeting is concretely confirmed with specific time/place.",
                "meetingRejection: Count only when meeting/invitation is clearly declined, with no definite alternative.",
                "attack: Count only when explicit insult, personal attack, or harsh criticism occurs.",
                "noResponse: Count only if a question or getHelp request receives no meaningful reply in context after sufficient time. Emojis, laughter, or one-character replies do not count as valid responses.",
                "",
                "Topics Rules",
                "Extract only conversation themes that can directly inform gift ideas, focusing on hobbies/interests and needed items.",
                "Examples of valid interests: 영화, 테니스, 캠핑, 음악, 독서, 운동, 여행",
                "Examples of valid items: 신발, 가방, 향수, 와인, 화장품, 커피머신, 전자기기",
                "Normalize synonyms/variations into one keyword. Examples:",
                "영화/영화관/무비 → 영화",
                "스니커즈/운동화 → 신발",
                "와인/술/맥주 → 와인",
                "향수/퍼퓸 → 향수",
                "Exclude abstract work/study terms (예: 프로젝트, 코드, 보고서, 시험공부) and all personal names, nicknames, locations, dates, emoticons, system messages, and links.",
                "Weighting rule:",
                "- If the topic is a general interest or hobby, assign count = 1.",
                "- If the topic is an item explicitly mentioned as needed, desired, or lacking (예: 필요하다, 갖고 싶다, 떨어졌다, 새로 사야 한다), assign count = 10.",
                "Count topics at message level (multiple mentions in one message = 1).",
                "Return up to 3 topics, preferring more recent if tied.",
                "Output only short Korean nouns directly usable as gift categories.",
                "",
                "Issues Rules",
                "Only include issues when the event is clearly significant or emotionally impactful within the month.",
                "CONFLICT: Strong or repeated complaints, criticism, arguments, or rejection. Exclude mild one-off tension or jokes.",
                "RECONCILIATION: Genuine apology, forgiveness, or resolution that clearly eased prior conflict. Exclude casual 'sorry' without context.",
                "TOUCHING: Praise, encouragement, consolation, or congratulations that carried notable emotional weight. Exclude trivial compliments.",
                "OTHER: Strictly factual or event-based notes that shifted the conversation meaningfully (e.g., major trip, exam result, sudden news). Exclude minor mentions.",
                "Summary: One short Korean sentence, fact-focused, ending with '했다/있었다' style.",
                "Do not include any personal names, nicknames, relationship titles (형, 동생, 오빠, 언니, 엄마, 친구, etc.), or job/role titles (교수, 선생님, 의사, 팀장, 상사, etc.) in the summary. Summaries must describe only the situation or event itself, without identifying people.",
                "Select only the top 3–5 most important issues for the month; discard less significant events.",
                "",
                "Feedback Rule",
                "Exactly one Korean sentence.",
                "Must end with \"관계입니다.\"",
                "Summarize overall relationship tone (question/response, frequency, conflicts, warmth, etc.).",
                "Length: 20–60 characters.",
                "Examples:",
                "질문이 많았으나 응답이 적은 소원한 관계입니다.",
                "갈등이 있었지만 화해가 잘 이루어진 안정적인 관계입니다.",
                "개인적인 이야기가 많아 따뜻한 관계입니다."
        );
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            if (trimmed.endsWith("```")) {
                String inner = trimmed.substring(3, trimmed.length() - 3).trim();
                if (inner.isEmpty()) {
                    return "{}";
                }
                int firstBraceInner = inner.indexOf('{');
                int lastBraceInner = inner.lastIndexOf('}');
                if (firstBraceInner >= 0 && lastBraceInner >= firstBraceInner) {
                    return inner.substring(firstBraceInner, lastBraceInner + 1);
                }
                return inner;
            }
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace >= firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }

    private record AutoAnalysisResponse(
            AutoStatsPayload autoStats,
            List<TopicPayload> topics,
            List<IssuePayload> issues,
            String feedback
    ) {
    }
}
