package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.dao.PersonRepository;
import com.ssafya408.cutlineparsing.dao.PersonValueRepository;
import com.ssafya408.cutlineparsing.service.AutoAnalysisService;
import com.ssafya408.cutlineparsing.service.MonthOutput;
import com.ssafya408.cutlineparsing.service.MonthlyComputationContext;
import com.ssafya408.cutlineparsing.service.MonthlyContextBuilder;
import com.ssafya408.cutlineparsing.service.OnePassCore;
import com.ssafya408.cutlineparsing.service.ParsingService;
import com.ssafya408.cutlineparsing.service.PersonUpdateCoordinator;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParsingServiceTest {

    @Test
    @DisplayName("process는 MonthlyContextBuilder와 PersonUpdateCoordinator를 호출한다")
    void process_delegatesToCollaborators() throws Exception {
        OnePassCore onePassCore = mock(OnePassCore.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonValueRepository personValueRepository = mock(PersonValueRepository.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        MonthlyContextBuilder contextBuilder = mock(MonthlyContextBuilder.class);
        PersonUpdateCoordinator updateCoordinator = mock(PersonUpdateCoordinator.class);

        ParsingService service = new ParsingService(
                onePassCore,
                personRepository,
                personValueRepository,
                autoAnalysisService,
                contextBuilder,
                updateCoordinator
        );

        Person person = createPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        MonthOutput output = createMonthOutput(YearMonth.of(2025, 8));
        when(onePassCore.run(any(), any(), any())).thenReturn(List.of(output));
        when(personValueRepository.findLatestBefore(anyLong(), any(YearMonth.class))).thenReturn(Optional.empty());

        MonthlyComputationContext context = createContext(person, YearMonth.of(2025, 8));
        when(contextBuilder.buildSequential(any(), any(), any(), any(), any())).thenReturn(List.of(context));

        MockMultipartFile file = new MockMultipartFile("file", "chat.txt", "text/plain", "dummy".getBytes(StandardCharsets.UTF_8));
        service.process(1L, file, "홍길동");

        verify(contextBuilder).buildSequential(any(), any(), any(), any(), any());
        verify(updateCoordinator).persistAndUpdate(eq(person), eq(List.of(context)));
    }

    @Test
    @DisplayName("processWithBatchAutoAnalysis는 사전 계산된 자동 분석을 사용한다")
    void processWithBatchAutoAnalysis_usesPrecomputedAnalyses() throws Exception {
        OnePassCore onePassCore = mock(OnePassCore.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonValueRepository personValueRepository = mock(PersonValueRepository.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        MonthlyContextBuilder contextBuilder = mock(MonthlyContextBuilder.class);
        PersonUpdateCoordinator updateCoordinator = mock(PersonUpdateCoordinator.class);

        ParsingService service = new ParsingService(
                onePassCore,
                personRepository,
                personValueRepository,
                autoAnalysisService,
                contextBuilder,
                updateCoordinator
        );

        Person person = createPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        MonthOutput output = createMonthOutput(YearMonth.of(2025, 8));
        when(onePassCore.run(any(), any(), any())).thenReturn(List.of(output));
        when(personValueRepository.findLatestBefore(anyLong(), any(YearMonth.class))).thenReturn(Optional.empty());

        MonthlyComputationContext context = createContext(person, YearMonth.of(2025, 8));
        when(contextBuilder.buildWithAutoAnalyses(any(), any(), any(), any())).thenReturn(List.of(context));

        AutoAnalysisResult autoResult = AutoAnalysisResult.empty();
        when(autoAnalysisService.analyze(any(), any(), any(), any())).thenReturn(autoResult);

        MockMultipartFile file = new MockMultipartFile("file", "chat.txt", "text/plain", "dummy".getBytes(StandardCharsets.UTF_8));
        service.processWithBatchAutoAnalysis(1L, file, "홍길동");

        verify(contextBuilder).buildWithAutoAnalyses(any(), any(), any(), any());
        verify(updateCoordinator).persistAndUpdate(eq(person), eq(List.of(context)));
    }

    @Test
    @DisplayName("update는 기존 데이터가 없으면 신규 파싱처럼 동작한다")
    void update_withoutExistingDataFallsBackToFreshParse() throws Exception {
        OnePassCore onePassCore = mock(OnePassCore.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonValueRepository personValueRepository = mock(PersonValueRepository.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        MonthlyContextBuilder contextBuilder = mock(MonthlyContextBuilder.class);
        PersonUpdateCoordinator updateCoordinator = mock(PersonUpdateCoordinator.class);

        ParsingService service = new ParsingService(
                onePassCore,
                personRepository,
                personValueRepository,
                autoAnalysisService,
                contextBuilder,
                updateCoordinator
        );

        Person person = createPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personValueRepository.findLatest(1L)).thenReturn(Optional.empty());
        when(personValueRepository.findLatestBefore(anyLong(), any(YearMonth.class))).thenReturn(Optional.empty());

        MonthOutput output = createMonthOutput(YearMonth.of(2025, 8));
        when(onePassCore.run(any(), any(), any())).thenReturn(List.of(output));

        MonthlyComputationContext context = createContext(person, YearMonth.of(2025, 8));
        when(contextBuilder.buildSequential(any(), any(), any(), any(), any())).thenReturn(List.of(context));

        MockMultipartFile file = new MockMultipartFile("file", "chat.txt", "text/plain", "dummy".getBytes(StandardCharsets.UTF_8));
        service.update(1L, file, "홍길동");

        verify(updateCoordinator).persistAndUpdate(eq(person), eq(List.of(context)));
    }

    @Test
    @DisplayName("update는 기존 월 이후 데이터를 삭제하고 재계산한다")
    void update_recalculatesFromLastMonth() throws Exception {
        OnePassCore onePassCore = mock(OnePassCore.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonValueRepository personValueRepository = mock(PersonValueRepository.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        MonthlyContextBuilder contextBuilder = mock(MonthlyContextBuilder.class);
        PersonUpdateCoordinator updateCoordinator = mock(PersonUpdateCoordinator.class);

        ParsingService service = new ParsingService(
                onePassCore,
                personRepository,
                personValueRepository,
                autoAnalysisService,
                contextBuilder,
                updateCoordinator
        );

        Person person = createPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        PersonValue latestValue = new PersonValue(person, 130_000_000, 2025, 5, "", 0.0);
        when(personValueRepository.findLatest(1L)).thenReturn(Optional.of(latestValue));
        when(personValueRepository.findLatestBefore(1L, YearMonth.of(2025, 5))).thenReturn(Optional.of(new PersonValue(person, 120_000_000, 2025, 4, "", 0.0)));

        MonthOutput mayOutput = createMonthOutput(YearMonth.of(2025, 5));
        MonthOutput julyOutput = createMonthOutput(YearMonth.of(2025, 7));
        when(onePassCore.run(any(), any(), any())).thenReturn(List.of(mayOutput, julyOutput));

        MonthlyComputationContext mayContext = createContext(person, YearMonth.of(2025, 5));
        MonthlyComputationContext juneContext = createContext(person, YearMonth.of(2025, 6));
        MonthlyComputationContext julyContext = createContext(person, YearMonth.of(2025, 7));
        when(contextBuilder.toYearMonth(latestValue)).thenReturn(YearMonth.of(2025, 5));
        when(contextBuilder.buildSequential(any(), any(), any(), any(), any())).thenReturn(List.of(mayContext, juneContext, julyContext));

        MockMultipartFile file = new MockMultipartFile("file", "chat.txt", "text/plain", "dummy".getBytes(StandardCharsets.UTF_8));
        service.update(1L, file, "홍길동");

        verify(updateCoordinator).purgeFromMonth(person, YearMonth.of(2025, 5));

        ArgumentCaptor<List<MonthlyComputationContext>> captor = ArgumentCaptor.forClass(List.class);
        verify(updateCoordinator).persistAndUpdate(eq(person), captor.capture());
        List<MonthlyComputationContext> contexts = captor.getValue();

        assertThat(contexts)
                .extracting(MonthlyComputationContext::yearMonth)
                .containsExactly(YearMonth.of(2025, 5), YearMonth.of(2025, 6), YearMonth.of(2025, 7));
    }

    private Person createPerson() {
        return new Person(new User("user@example.com", "홍길동", LocalDate.of(1990, 1, 1), null),
                "홍길동", LocalDate.of(1990, 1, 1), null, null, PersonRelation.FRIEND, 12);
    }

    private MonthOutput createMonthOutput(YearMonth ym) {
        MonthlyStatsResult statsResult = new MonthlyStatsResult(ym, 10, 100, 200, 3, 5, 2);
        return new MonthOutput(ym, "U|+0|\"안녕\"", statsResult);
    }

    private MonthlyComputationContext createContext(Person person, YearMonth ym) {
        PersonValue personValue = new PersonValue(person, 100_000_000, ym.getYear(), ym.getMonthValue(), "", 0.0);
        ChatManualStats manualStats = new ChatManualStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        ChatAutoStats autoStats = new ChatAutoStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        personValue.setManualStats(manualStats);
        personValue.setAutoStats(autoStats);
        RelationshipValueResult relationshipValue = new RelationshipValueResult(100_000_000, 0.0, 0, 0, 0, 1.0, 1.0);
        return new MonthlyComputationContext(
                ym,
                createMonthOutput(ym),
                manualStats,
                AutoAnalysisResult.empty(),
                autoStats,
                personValue,
                relationshipValue
        );
    }
}
