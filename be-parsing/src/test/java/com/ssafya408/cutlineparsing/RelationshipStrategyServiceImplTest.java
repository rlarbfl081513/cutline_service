package com.ssafya408.cutlineparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonStatus;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.service.MonthOutput;
import com.ssafya408.cutlineparsing.service.MonthlyComputationContext;
import com.ssafya408.cutlineparsing.service.RelationshipStrategyServiceImpl;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.service.dto.AutoStatsPayload;
import com.ssafya408.cutlineparsing.service.dto.IssuePayload;
import com.ssafya408.cutlineparsing.service.dto.TopicPayload;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RelationshipStrategyServiceImplTest {

    private MonthlyComputationContext createContext() {
        ChatManualStats manual = new ChatManualStats(0,0,0,0,0,
                10_000,20_000,30_000,5_000,7_000);
        ChatAutoStats auto = new ChatAutoStats(0,0,0,0,0,0,0,0,0,0,
                100,200,300,400,500,600,-100,-200,-300,-400);
        Person person = new Person(new User("tester@example.com", "Tester", LocalDate.of(1990,1,1), null),
                "Tester", LocalDate.of(1990,1,1), null, PersonStatus.INTEREST, PersonRelation.FRIEND, 24);
        PersonValue value = new PersonValue(person, 123_000_000, 2025, 8, "좋은 분위기", 0.1);
        value.setManualStats(manual);
        value.setAutoStats(auto);

        AutoAnalysisResult autoAnalysis = new AutoAnalysisResult(
                new AutoStatsPayload(0,0,0,0,0,0,0,0,0,0),
                List.of(TopicPayload.of("여행", 3)),
                List.of(IssuePayload.of(null, "갈등 후 화해")),
                "이번 달에는 대화가 활발했습니다."
        );

        MonthlyStatsResult statsResult = new MonthlyStatsResult(YearMonth.of(2025, 8), 0,0,0,0,0,0);
        MonthOutput output = new MonthOutput(YearMonth.of(2025, 8), "U|+0|\"안녕\"", statsResult);

        return new MonthlyComputationContext(
                YearMonth.of(2025, 8),
                output,
                manual,
                autoAnalysis,
                auto,
                value,
                new RelationshipValueResult(123_000_000, 0.1, 0, 0, 0,1.0,1.0)
        );
    }

    @Test
    @DisplayName("LLM 응답을 성공적으로 파싱하면 전략을 반환한다")
    void recommend_success() {
        ChatClient chatClient = Mockito.mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("{\"interestStrategy\":\"관심 유지\",\"uninterestStrategy\":\"거리 두기\",\"maintainStrategy\":\"안정적 유지\"}");

        RelationshipStrategyServiceImpl service = new RelationshipStrategyServiceImpl(chatClient, new ObjectMapper());

        Person person = new Person(new User("tester@example.com", "Tester", LocalDate.of(1990,1,1), null),
                "Tester", LocalDate.of(1990,1,1), null, PersonStatus.MAINTAIN, PersonRelation.FRIEND, 24);

        List<MonthlySummarySnapshot> summaries = List.of();
        MonthlyComputationContext context = createContext();

        var result = service.recommend(person, summaries, context);
        assertEquals("관심 유지", result.interestStrategy());
        assertEquals("거리 두기", result.uninterestStrategy());
        assertEquals("안정적 유지", result.maintainStrategy());
    }

    @Test
    @DisplayName("LLM 실패 시 빈 전략을 반환한다")
    void recommend_failureReturnsEmpty() {
        ChatClient chatClient = Mockito.mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("boom"));

        RelationshipStrategyServiceImpl service = new RelationshipStrategyServiceImpl(chatClient, new ObjectMapper());

        Person person = new Person(new User("tester@example.com", "Tester", LocalDate.of(1990,1,1), null),
                "Tester", LocalDate.of(1990,1,1), null, PersonStatus.MAINTAIN, PersonRelation.FRIEND, 24);

        var result = service.recommend(person, List.of(), createContext());
        assertEquals("", result.interestStrategy());
        assertEquals("", result.uninterestStrategy());
        assertEquals("", result.maintainStrategy());
    }
}
