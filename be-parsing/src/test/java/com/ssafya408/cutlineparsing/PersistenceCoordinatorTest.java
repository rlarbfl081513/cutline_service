package com.ssafya408.cutlineparsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonStatus;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.dao.PersonValueRepository;
import com.ssafya408.cutlineparsing.service.MonthOutput;
import com.ssafya408.cutlineparsing.service.MonthlyComputationContext;
import com.ssafya408.cutlineparsing.service.PersistenceCoordinator;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.service.dto.AutoStatsPayload;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import com.ssafya408.cutlineparsing.service.dto.IssuePayload;
import com.ssafya408.cutlineparsing.service.dto.TopicPayload;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistenceCoordinatorTest {

    @Mock
    private PersonValueRepository personValueRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PersistenceCoordinator persistenceCoordinator;

    @Test
    @DisplayName("계산 결과가 PersonValue 저장 후 월별 스냅샷을 반환한다")
    void persist_savesEntities() throws Exception {
        Person person = new Person(new User("tester@example.com", "홍길동", LocalDate.of(1990, 1, 1), null),
                "홍길동", LocalDate.of(1990, 1, 1), null, PersonStatus.MAINTAIN, PersonRelation.FRIEND, 12);

        ChatManualStats manual = new ChatManualStats(0, 0, 0, 0, 0,
                10_000, 20_000, 30_000,
                5_000, 7_000);

        ChatAutoStats auto = new ChatAutoStats(1,2,3,4,5,6,7,8,9,10,
                100,200,300,400,500,600,-100,-200,-300,-400);

        PersonValue value = new PersonValue(person, 123_000_000, 2025, 8, "좋은 분위기", 0.1);
        value.setManualStats(manual);
        value.setAutoStats(auto);

        AutoAnalysisResult autoAnalysis = new AutoAnalysisResult(
                new AutoStatsPayload(1,2,3,4,5,6,7,8,9,10),
                List.of(TopicPayload.of("여행", 5)),
                List.of(IssuePayload.of(null, "갈등 후 화해")),
                "이번 달에는 만나서 대화가 많았습니다."
        );

        MonthlyStatsResult statsResult = new MonthlyStatsResult(YearMonth.of(2025, 8), 10, 100, 200, 3, 5, 2);
        MonthOutput output = new MonthOutput(YearMonth.of(2025, 8), "U|+0|\"안녕\"", statsResult);

        MonthlyComputationContext context = new MonthlyComputationContext(
                YearMonth.of(2025, 8),
                output,
                manual,
                autoAnalysis,
                auto,
                value,
                new RelationshipValueResult(123_000_000, 0.1, 0, 0, 0, 1.0, 1.0)
        );

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        List<MonthlySummarySnapshot> summaries = persistenceCoordinator.persist(person, List.of(context));

        verify(personValueRepository).save(value);

        assertEquals(1, person.getPersonValues().size());
        assertEquals(1, summaries.size());
        assertEquals(YearMonth.of(2025, 8), summaries.get(0).yearMonth());
    }

    @Test
    @DisplayName("직렬화 오류가 발생해도 저장은 계속된다")
    void persist_handlesSerializationError() throws Exception {
        Person person = new Person(new User("tester@example.com", "홍길동", LocalDate.of(1990, 1, 1), null),
                "홍길동", LocalDate.of(1990, 1, 1), null, PersonStatus.MAINTAIN, PersonRelation.FRIEND, 12);

        ChatManualStats manual = new ChatManualStats(0,0,0,0,0,
                0,0,0,0,0);
        ChatAutoStats auto = new ChatAutoStats(0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0);
        PersonValue value = new PersonValue(person, 100_000_000, 2025, 8, "", 0.0);
        value.setManualStats(manual);
        value.setAutoStats(auto);

        AutoAnalysisResult autoAnalysis = new AutoAnalysisResult(
                new AutoStatsPayload(0,0,0,0,0,0,0,0,0,0), List.of(), List.of(), "");
        MonthlyStatsResult statsResult = new MonthlyStatsResult(YearMonth.of(2025, 8), 0,0,0,0,0,0);
        MonthOutput output = new MonthOutput(YearMonth.of(2025, 8), "", statsResult);

        MonthlyComputationContext context = new MonthlyComputationContext(
                YearMonth.of(2025, 8),
                output,
                manual,
                autoAnalysis,
                auto,
                value,
                new RelationshipValueResult(100_000_000, 0.0, 0, 0, 0,1.0,1.0)
        );

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        List<MonthlySummarySnapshot> summaries = persistenceCoordinator.persist(person, List.of(context));

        verify(personValueRepository).save(value);
        assertTrue(summaries.isEmpty());
    }
}
