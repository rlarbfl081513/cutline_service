package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.service.AutoAnalysisService;
import com.ssafya408.cutlineparsing.service.AutoStatsCalculator;
import com.ssafya408.cutlineparsing.service.ManualStatsCalculator;
import com.ssafya408.cutlineparsing.service.MonthOutput;
import com.ssafya408.cutlineparsing.service.MonthlyComputationContext;
import com.ssafya408.cutlineparsing.service.MonthlyContextBuilder;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import com.ssafya408.cutlineparsing.service.RelationshipValueCalculator;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyContextBuilderTest {

    @Test
    @DisplayName("buildSequential은 공백 월을 감점 컨텍스트로 채운다")
    void buildSequential_insertsPenaltyForMissingMonths() {
        ManualStatsCalculator manualStatsCalculator = mock(ManualStatsCalculator.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        AutoStatsCalculator autoStatsCalculator = mock(AutoStatsCalculator.class);
        RelationshipValueCalculator relationshipValueCalculator = mock(RelationshipValueCalculator.class);

        MonthlyContextBuilder builder = new MonthlyContextBuilder(
                manualStatsCalculator,
                autoAnalysisService,
                autoStatsCalculator,
                relationshipValueCalculator
        );

        Person person = new Person(new User("user@example.com", "홍길동", LocalDate.of(1990, 1, 1), null),
                "홍길동2", LocalDate.of(1990, 1, 1), null, null, PersonRelation.FRIEND, 12);

        PersonValue aprilValue = new PersonValue(person, 120_000_000, 2025, 4, "", 0.0);

        MonthOutput mayOutput = createMonthOutput(YearMonth.of(2025, 5));
        MonthOutput julyOutput = createMonthOutput(YearMonth.of(2025, 7));

        ChatManualStats manualStats = new ChatManualStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        ChatAutoStats autoStats = new ChatAutoStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        when(manualStatsCalculator.calculate(any())).thenReturn(manualStats);
        when(autoStatsCalculator.calculate(any())).thenReturn(autoStats);
        when(autoAnalysisService.analyze(any(), any(), any(), any())).thenReturn(AutoAnalysisResult.empty());
        when(relationshipValueCalculator.calculate(any(), any(), any(), any()))
                .thenReturn(new RelationshipValueResult(130_000_000, 0.08, 10_000_000, 0, 0, 1.0, 1.0))
                .thenReturn(new RelationshipValueResult(140_000_000, 0.07, 10_000_000, 0, 0, 1.0, 1.0));

        List<MonthlyComputationContext> contexts = builder.buildSequential(
                List.of(mayOutput, julyOutput),
                person,
                aprilValue,
                "홍길동",
                "홍길동2"
        );

        assertThat(contexts)
                .extracting(MonthlyComputationContext::yearMonth)
                .containsExactly(
                        YearMonth.of(2025, 5),
                        YearMonth.of(2025, 6),
                        YearMonth.of(2025, 7)
                );

        MonthlyComputationContext juneContext = contexts.get(1);
        assertThat(juneContext.personValue().getValue()).isEqualTo(120_000_000);
        assertThat(juneContext.manualStats().getMonthVolume()).isZero();
        assertThat(juneContext.autoStats().getStartChat()).isZero();
    }

    @Test
    @DisplayName("buildWithAutoAnalyses도 공백 월 감점을 유지한다")
    void buildWithAutoAnalyses_respectsPenaltyFilling() {
        ManualStatsCalculator manualStatsCalculator = mock(ManualStatsCalculator.class);
        AutoAnalysisService autoAnalysisService = mock(AutoAnalysisService.class);
        AutoStatsCalculator autoStatsCalculator = mock(AutoStatsCalculator.class);
        RelationshipValueCalculator relationshipValueCalculator = mock(RelationshipValueCalculator.class);

        MonthlyContextBuilder builder = new MonthlyContextBuilder(
                manualStatsCalculator,
                autoAnalysisService,
                autoStatsCalculator,
                relationshipValueCalculator
        );

        Person person = new Person(new User("user@example.com", "홍길동", LocalDate.of(1990, 1, 1), null),
                "홍길동2", LocalDate.of(1990, 1, 1), null, null, PersonRelation.FRIEND, 12);

        PersonValue aprilValue = new PersonValue(person, 120_000_000, 2025, 4, "", 0.0);

        MonthOutput mayOutput = createMonthOutput(YearMonth.of(2025, 5));
        MonthOutput julyOutput = createMonthOutput(YearMonth.of(2025, 7));

        ChatManualStats manualStats = new ChatManualStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        ChatAutoStats autoStats = new ChatAutoStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        when(manualStatsCalculator.calculate(any())).thenReturn(manualStats);
        when(autoStatsCalculator.calculate(any())).thenReturn(autoStats);
        when(relationshipValueCalculator.calculate(any(), any(), any(), any()))
                .thenReturn(new RelationshipValueResult(130_000_000, 0.08, 10_000_000, 0, 0, 1.0, 1.0))
                .thenReturn(new RelationshipValueResult(140_000_000, 0.07, 10_000_000, 0, 0, 1.0, 1.0));

        List<AutoAnalysisResult> analyses = List.of(AutoAnalysisResult.empty(), AutoAnalysisResult.empty());

        List<MonthlyComputationContext> contexts = builder.buildWithAutoAnalyses(
                List.of(mayOutput, julyOutput),
                analyses,
                person,
                aprilValue
        );

        assertThat(contexts)
                .extracting(MonthlyComputationContext::yearMonth)
                .containsExactly(
                        YearMonth.of(2025, 5),
                        YearMonth.of(2025, 6),
                        YearMonth.of(2025, 7)
                );

        MonthlyComputationContext juneContext = contexts.get(1);
        assertThat(juneContext.personValue().getValue()).isEqualTo(120_000_000);
    }

    private MonthOutput createMonthOutput(YearMonth ym) {
        MonthlyStatsResult statsResult = new MonthlyStatsResult(ym, 10, 100, 200, 3, 5, 2);
        return new MonthOutput(ym, "U|+0|\"안녕\"", statsResult);
    }
}
