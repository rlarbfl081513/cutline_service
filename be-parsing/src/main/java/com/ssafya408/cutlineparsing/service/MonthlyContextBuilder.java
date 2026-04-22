package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyContextBuilder {

    private static final int MISSING_MONTH_PENALTY = 10_000_000;

    private final ManualStatsCalculator manualStatsCalculator;
    private final AutoAnalysisService autoAnalysisService;
    private final AutoStatsCalculator autoStatsCalculator;
    private final RelationshipValueCalculator relationshipValueCalculator;

    public List<MonthlyComputationContext> buildSequential(List<MonthOutput> outputs,
                                                          Person person,
                                                          PersonValue initialPreviousValue,
                                                          String userDisplayName,
                                                          String friendDisplayName) {
        return buildContexts(outputs, person, initialPreviousValue,
                (index, output) -> autoAnalysisService.analyze(output, output.stats(), userDisplayName, friendDisplayName));
    }

    public List<MonthlyComputationContext> buildWithAutoAnalyses(List<MonthOutput> outputs,
                                                                 List<AutoAnalysisResult> autoAnalyses,
                                                                 Person person,
                                                                 PersonValue initialPreviousValue) {
        return buildContexts(outputs, person, initialPreviousValue,
                (index, output) -> index < autoAnalyses.size() ? defaultIfNull(autoAnalyses.get(index)) : AutoAnalysisResult.empty());
    }

    public YearMonth toYearMonth(PersonValue value) {
        if (value == null || value.getYear() == null || value.getMonth() == null) {
            return null;
        }
        return YearMonth.of(value.getYear(), value.getMonth());
    }

    private List<MonthlyComputationContext> buildContexts(List<MonthOutput> outputs,
                                                          Person person,
                                                          PersonValue initialPreviousValue,
                                                          AutoAnalysisProvider autoAnalysisProvider) {
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalArgumentException("대화 분석 결과가 생성되지 않았습니다. 파일 형식을 다시 확인해 주세요.");
        }

        List<MonthlyComputationContext> contexts = new ArrayList<>();
        PersonValue previousValue = initialPreviousValue;
        YearMonth lastProcessedMonth = toYearMonth(previousValue);

        for (int index = 0; index < outputs.size(); index++) {
            MonthOutput output = outputs.get(index);
            YearMonth currentMonth = output.ym();
            log.info("[ 월별 분석 ] >>> 처리중 - 대상월: {}", currentMonth);

            if (lastProcessedMonth != null && lastProcessedMonth.plusMonths(1).isBefore(currentMonth)) {
                previousValue = appendMissingMonthContexts(contexts, person, previousValue, lastProcessedMonth, currentMonth);
                YearMonth updated = toYearMonth(previousValue);
                if (updated != null) {
                    lastProcessedMonth = updated;
                }
            }

            AutoAnalysisResult autoAnalysis = defaultIfNull(autoAnalysisProvider.provide(index, output));

            var manualStats = manualStatsCalculator.calculate(output.stats());
            var autoStats = autoStatsCalculator.calculate(autoAnalysis.autoStats());
            var valueResult = relationshipValueCalculator.calculate(person, previousValue, manualStats, autoStats);

            PersonValue draftValue = new PersonValue(person,
                    valueResult.currentValue(),
                    currentMonth.getYear(),
                    currentMonth.getMonthValue(),
                    autoAnalysis.feedback(),
                    valueResult.changeRate());

            draftValue.setManualStats(manualStats);
            draftValue.setAutoStats(autoStats);

            contexts.add(new MonthlyComputationContext(
                    currentMonth,
                    output,
                    manualStats,
                    autoAnalysis,
                    autoStats,
                    draftValue,
                    valueResult
            ));

            previousValue = draftValue;
            lastProcessedMonth = currentMonth;
            log.info("[ 월별 분석 ] >>> 완료 - 월: {}, 관계값: {}", currentMonth, valueResult.currentValue());
        }

        log.info("[ 월별 분석 ] >>> 전체 완료 - 총 생성된 컨텍스트: {}개", contexts.size());
        return contexts;
    }

    private PersonValue appendMissingMonthContexts(List<MonthlyComputationContext> contexts,
                                                   Person person,
                                                   PersonValue previousValue,
                                                   YearMonth lastProcessedMonth,
                                                   YearMonth targetMonth) {
        if (previousValue == null || lastProcessedMonth == null) {
            return previousValue;
        }

        YearMonth cursor = lastProcessedMonth.plusMonths(1);
        while (cursor.isBefore(targetMonth)) {
            MonthlyComputationContext gapContext = createMissingMonthContext(person, cursor, previousValue);
            contexts.add(gapContext);
            previousValue = gapContext.personValue();
            cursor = cursor.plusMonths(1);
        }
        return previousValue;
    }

    private MonthlyComputationContext createMissingMonthContext(Person person,
                                                                YearMonth month,
                                                                PersonValue previousValue) {
        int previousAmount = safeInt(previousValue.getValue());
        int currentAmount = clampToInt((long) previousAmount - MISSING_MONTH_PENALTY);
        long delta = (long) currentAmount - previousAmount;
        double changeRate = previousAmount != 0 ? delta / (double) previousAmount : 0.0;

        PersonValue draftValue = new PersonValue(person,
                currentAmount,
                month.getYear(),
                month.getMonthValue(),
                "",
                changeRate);

        ChatManualStats manualStats = new ChatManualStats(0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        ChatAutoStats autoStats = new ChatAutoStats(0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);

        draftValue.setManualStats(manualStats);
        draftValue.setAutoStats(autoStats);

        MonthlyStatsResult statsResult = new MonthlyStatsResult(
                month,
                0,
                0,
                0L,
                0,
                0,
                0
        );

        RelationshipValueResult relationship = new RelationshipValueResult(
                currentAmount,
                changeRate,
                delta,
                0,
                delta,
                1.0,
                1.0
        );

        log.info("[ 월 공백 보정 ] >>> month: {}, 감점 전후 값: {} -> {}", month, previousAmount, currentAmount);

        return new MonthlyComputationContext(
                month,
                new MonthOutput(month, "", statsResult),
                manualStats,
                AutoAnalysisResult.empty(),
                autoStats,
                draftValue,
                relationship
        );
    }

    private AutoAnalysisResult defaultIfNull(AutoAnalysisResult result) {
        return result == null ? AutoAnalysisResult.empty() : result;
    }

    private int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @FunctionalInterface
    private interface AutoAnalysisProvider {
        AutoAnalysisResult provide(int index, MonthOutput output);
    }
}
