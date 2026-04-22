package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;

import java.time.YearMonth;

public record MonthOutput(
        YearMonth ym,
        String dsl,
        MonthlyStatsResult stats
) {}