package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;

public interface AutoAnalysisService {

    AutoAnalysisResult analyze(MonthOutput monthOutput, MonthlyStatsResult stats, String userDisplayName, String friendDisplayName);
}
