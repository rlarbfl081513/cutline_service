package com.ssafya408.cutlineparsing.service;

public record RelationshipValueResult(
        int currentValue,
        double changeRate,
        long weightedDelta,
        long positiveContribution,
        long negativeContribution,
        double positiveMultiplier,
        double negativeMultiplier
) {}
