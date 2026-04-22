package com.ssafya408.cutlineparsing.service.dto;

public record StrategyRecommendation(
        String interestStrategy,
        String uninterestStrategy,
        String maintainStrategy
) {
    public static StrategyRecommendation empty() {
        return new StrategyRecommendation("", "", "");
    }
}
