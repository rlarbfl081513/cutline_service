package com.ssafya408.cutlineparsing.util.accumulator;

import java.time.YearMonth;

public record MonthlyStatsResult(
        YearMonth ym,
        int fMsgCount,
        int fMsgChars,
        long fReplyMinutesSum,
        int fReplyPairsCount,
        int activeDaysCount,
        int maxNoChatStreakDays
) {}
