package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import java.time.YearMonth;

public record MonthlyComputationContext(
        YearMonth yearMonth,
        MonthOutput monthOutput,
        ChatManualStats manualStats,
        com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult autoAnalysis,
        ChatAutoStats autoStats,
        PersonValue personValue,
        RelationshipValueResult relationshipValue
) {}
