package com.ssafya408.cutlineparsing.service.dto;

import java.time.YearMonth;

public record MonthlySummarySnapshot(
        YearMonth yearMonth,
        String feedback,
        String topicsJson,
        String issuesJson,
        String autoStatsJson
) {}
