package com.ssafya408.cutlineparsing.service.dto;

public record AutoAnalysisRequest(
        String month,
        String userDisplayName,
        String friendDisplayName,
        String dsl,
        int friendMessageCount,
        int friendMessageChars,
        int activeDays,
        int maxSilentStreak,
        long replyMinutesSum,
        int replyPairsCount
) {}
