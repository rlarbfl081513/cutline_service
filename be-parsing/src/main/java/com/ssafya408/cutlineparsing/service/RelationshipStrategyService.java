package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import com.ssafya408.cutlineparsing.service.dto.StrategyRecommendation;
import java.util.List;

public interface RelationshipStrategyService {

    StrategyRecommendation recommend(Person person, List<MonthlySummarySnapshot> summaries, MonthlyComputationContext latestContext);
}
