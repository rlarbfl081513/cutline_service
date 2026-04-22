package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import org.springframework.stereotype.Component;

@Component
public class ManualStatsCalculator {

    private static final int BONUS_PER_100_CHARS = 10_000;
    private static final int BONUS_PER_10_MSGS = 10_000;
    private static final int BONUS_PER_CHAT_DAY = 100_000;
    private static final int PENALTY_PER_3_MINUTES = 10_000;
    private static final int PENALTY_PER_SILENT_DAY = 250_000;

    private static final int MAX_CHAR_BONUS = 4_000_000;
    private static final int MAX_MSG_BONUS = 4_000_000;
    private static final int MAX_CHAT_DAY_BONUS = 2_000_000;
    private static final int MAX_RESPONSE_PENALTY = 5_000_000;
    private static final int MAX_SILENT_PENALTY = 5_000_000;

    public ChatManualStats calculate(MonthlyStatsResult stats) {
        if (stats == null) {
            throw new IllegalArgumentException("월별 통계 결과가 필요합니다.");
        }

        int responseAverageMinutes = computeAverageResponseMinutes(stats);

        int charBonus = clampMax((stats.fMsgChars() / 100) * BONUS_PER_100_CHARS, MAX_CHAR_BONUS);
        int msgBonus = clampMax((stats.fMsgCount() / 10) * BONUS_PER_10_MSGS, MAX_MSG_BONUS);
        int chatDayBonus = clampMax(stats.activeDaysCount() * BONUS_PER_CHAT_DAY, MAX_CHAT_DAY_BONUS);
        int responsePenalty = clampMax((responseAverageMinutes / 3) * PENALTY_PER_3_MINUTES, MAX_RESPONSE_PENALTY);
        int silentPenalty = clampMax(stats.maxNoChatStreakDays() * PENALTY_PER_SILENT_DAY, MAX_SILENT_PENALTY);

        return new ChatManualStats(
                stats.fMsgChars(),
                stats.fMsgCount(),
                responseAverageMinutes,
                stats.activeDaysCount(),
                stats.maxNoChatStreakDays(),
                charBonus,
                msgBonus,
                chatDayBonus,
                responsePenalty,
                silentPenalty
        );
    }

    private int computeAverageResponseMinutes(MonthlyStatsResult stats) {
        if (stats.fReplyPairsCount() <= 0) {
            return 0;
        }
        double average = (double) stats.fReplyMinutesSum() / stats.fReplyPairsCount();
        return (int) Math.round(average);
    }

    private int clampMax(int value, int max) {
        return Math.min(Math.max(value, 0), max);
    }
}
