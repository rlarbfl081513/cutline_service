package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.service.ManualStatsCalculator;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManualStatsCalculatorTest {

    private final ManualStatsCalculator calculator = new ManualStatsCalculator();

    private MonthlyStatsResult stats(int chars, int msgs, long replySum, int replyPairs, int activeDays, int silentDays) {
        return new MonthlyStatsResult(YearMonth.of(2025, 8), msgs, chars, replySum, replyPairs, activeDays, silentDays);
    }

    @Test
    @DisplayName("기본 케이스: 모든 지표가 규칙대로 환산된다")
    void calculate_basic() {
        MonthlyStatsResult stats = stats(450, 27, 45, 3, 12, 4);

        ChatManualStats result = calculator.calculate(stats);

        assertEquals(450, result.getMonthVolume());
        assertEquals(27, result.getMonthCount());
        assertEquals(15, result.getResponseAverage()); // 45/3 = 15분
        assertEquals(12, result.getChatDays());
        assertEquals(4, result.getSilentDays());

        assertEquals(40_000, result.getBonusChars()); // 400자 기준 4만, 나머지 버림
        assertEquals(20_000, result.getBonusMessages()); // 20개 기준 2만
        assertEquals(1_200_000, result.getBonusChatDays()); // 12 * 10만
        assertEquals(50_000, result.getPenaltyResponse()); // 15분 → 5*1만
        assertEquals(1_000_000, result.getPenaltySilent()); // 4 * 25만
    }

    @Test
    @DisplayName("상한/하한이 적용된다")
    void calculate_caps() {
        MonthlyStatsResult stats = stats(100000, 4000, 9000, 1, 100, 100);

        ChatManualStats result = calculator.calculate(stats);

        assertEquals(4_000_000, result.getBonusChars());
        assertEquals(4_000_000, result.getBonusMessages());
        assertEquals(2_000_000, result.getBonusChatDays());
        assertEquals(5_000_000, result.getPenaltyResponse());
        assertEquals(5_000_000, result.getPenaltySilent());
    }

    @Test
    @DisplayName("응답 페어가 없으면 평균은 0, 페널티도 0")
    void calculate_zero_reply_pairs() {
        MonthlyStatsResult stats = stats(0, 0, 100, 0, 0, 2);

        ChatManualStats result = calculator.calculate(stats);

        assertEquals(0, result.getResponseAverage());
        assertEquals(0, result.getPenaltyResponse());
        assertEquals(500_000, result.getPenaltySilent());
    }
}
