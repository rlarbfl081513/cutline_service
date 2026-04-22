package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.service.AutoStatsCalculator;
import com.ssafya408.cutlineparsing.service.dto.AutoStatsPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoStatsCalculatorTest {

    private final AutoStatsCalculator calculator = new AutoStatsCalculator();

    @Test
    @DisplayName("양수 지표는 상한을 고려해 점수가 계산된다")
    void positiveMetricsRespectCaps() {
        // 20번 선톡 -> 20*500,000=10,000,000 but cap 5,000,000
        // 6번 개인적 이야기 -> 6*600,000=3,600,000 but cap 3,000,000
        AutoStatsPayload payload = new AutoStatsPayload(
                20, 0, 6, 5, 0, 1, 0, 2, 0, 0
        );

        ChatAutoStats stats = calculator.calculate(payload);

        assertEquals(20, stats.getStartChat());
        assertEquals(6, stats.getPrivateStory());
        assertEquals(2, stats.getGiveHelp());

        assertEquals(5_000_000, stats.getScoreStartChat());
        assertEquals(3_000_000, stats.getScorePrivateStory());
        assertEquals(2 * 1_000_000, stats.getScoreGiveHelp());
    }

    @Test
    @DisplayName("음수 지표는 하한을 고려해 점수가 계산된다")
    void negativeMetricsRespectCaps() {
        // attack 3회 -> -15,000,000 but cap -10,000,000
        // 무응답 8회 -> -8,000,000 but cap -5,000,000
        AutoStatsPayload payload = new AutoStatsPayload(
                0, 0, 0, 0, 4, 0, 8, 0, 3, 3
        );

        ChatAutoStats stats = calculator.calculate(payload);

        assertEquals(4, stats.getGetHelp());
        assertEquals(8, stats.getNoResponse());
        assertEquals(3, stats.getAttack());

        assertEquals(-2_000_000, stats.getScoreGetHelp());
        assertEquals(-5_000_000, stats.getScoreNoResponse());
        assertEquals(-10_000_000, stats.getScoreAttack());
    }

    @Test
    @DisplayName("null 입력은 모든 필드를 0으로 초기화한다")
    void nullPayloadProducesZeros() {
        ChatAutoStats stats = calculator.calculate(null);
        assertEquals(0, stats.getStartChat());
        assertEquals(0, stats.getScoreStartChat());
        assertEquals(0, stats.getScoreAttack());
    }
}
