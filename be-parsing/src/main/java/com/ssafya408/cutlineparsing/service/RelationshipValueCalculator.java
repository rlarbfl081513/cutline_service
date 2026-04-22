package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonStatus;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RelationshipValueCalculator {

    private static final double MANUAL_POSITIVE_MAX = 10_000_000d; // 4M + 4M + 2M
    private static final double MANUAL_PENALTY_MAX = 10_000_000d; // 5M + 5M
    private static final double AUTO_POSITIVE_MAX = 20_000_000d;  // 5M+2M+3M+2M+5M+3M
    private static final double AUTO_PENALTY_MAX = 20_000_000d;   // 5M+3M+10M+2M

    private static final double POSITIVE_SCALE = 20_000_000d;
    private static final double NEGATIVE_SCALE = 20_000_000d;

    private static final double MANUAL_WEIGHT = 1.0;
    private static final double AUTO_WEIGHT = 1.0;

    public RelationshipValueResult calculate(Person person,
                                             PersonValue previousValue,
                                             ChatManualStats manualStats,
                                             ChatAutoStats autoStats) {
        Objects.requireNonNull(person, "person must not be null");

        RelationProfile relation = resolveRelationProfile(person.getRelation());
        double statusBoost = resolveStatusGrade(person.getStatus()).weight();

        double manualPositiveRaw = sumManualPositive(manualStats);
        double manualPenaltyRaw = sumManualPenalties(manualStats);
        double autoPositiveRaw = sumAutoPositive(autoStats);
        double autoPenaltyRaw = sumAutoPenalties(autoStats);

        double positiveNorm = blendedNormalisation(manualPositiveRaw, MANUAL_POSITIVE_MAX,
                autoPositiveRaw, AUTO_POSITIVE_MAX);
        double negativeNorm = blendedNormalisation(manualPenaltyRaw, MANUAL_PENALTY_MAX,
                autoPenaltyRaw, AUTO_PENALTY_MAX);

        double positiveTotal = positiveNorm * POSITIVE_SCALE;
        double negativeTotal = negativeNorm * NEGATIVE_SCALE;

        double positiveMultiplier = Math.max(0.0,
                1.0 + relation.positiveGrade().weight() + statusBoost);
        double negativeMultiplier = Math.max(0.0,
                1.0 + relation.negativeGrade().weight() - statusBoost);

        double weightedPositive = positiveTotal * positiveMultiplier;
        double weightedNegative = negativeTotal * negativeMultiplier;
        double delta = weightedPositive - weightedNegative;

        double baseComponent = relation.baseValue() + computeTenureBonus(relation, person.getDuration());
        double startingValue = previousValue == null ? baseComponent : safeInt(previousValue.getValue());
        startingValue = clampToRange(startingValue, 0.0, relation.totalCap());

        double currentRaw = startingValue + delta;
        double cappedCurrent = clampToRange(currentRaw, 0.0, relation.totalCap());
        long roundedCurrent = roundToTenThousand(cappedCurrent);

        double changeRate = 0.0;
        if (previousValue != null) {
            int prevValue = safeInt(previousValue.getValue());
            if (prevValue != 0) {
                changeRate = (roundedCurrent - prevValue) / (double) prevValue;
            }
        } else if (startingValue != 0.0) {
            changeRate = (roundedCurrent - startingValue) / startingValue;
        }

        long weightedDelta = Math.round(delta);
        long positiveContribution = Math.round(weightedPositive);
        long negativeContribution = Math.round(weightedNegative);

        return new RelationshipValueResult(
                Math.toIntExact(roundedCurrent),
                changeRate,
                weightedDelta,
                positiveContribution,
                negativeContribution,
                positiveMultiplier,
                negativeMultiplier
        );
    }

    private double blendedNormalisation(double manualValue, double manualMax,
                                        double autoValue, double autoMax) {
        double manualNorm = normalise(manualValue, manualMax);
        double autoNorm = normalise(autoValue, autoMax);
        double weightSum = MANUAL_WEIGHT + AUTO_WEIGHT;
        if (weightSum == 0.0) {
            return 0.0;
        }
        return ((manualNorm * MANUAL_WEIGHT) + (autoNorm * AUTO_WEIGHT)) / weightSum;
    }

    private double normalise(double value, double max) {
        if (max <= 0.0) {
            return 0.0;
        }
        double clamped = clampToRange(value, 0.0, max);
        return clamped / max;
    }

    private double sumManualPositive(ChatManualStats stats) {
        if (stats == null) {
            return 0.0;
        }
        return safeInt(stats.getBonusChars())
                + safeInt(stats.getBonusMessages())
                + safeInt(stats.getBonusChatDays());
    }

    private double sumManualPenalties(ChatManualStats stats) {
        if (stats == null) {
            return 0.0;
        }
        return safeInt(stats.getPenaltyResponse())
                + safeInt(stats.getPenaltySilent());
    }

    private double sumAutoPositive(ChatAutoStats stats) {
        if (stats == null) {
            return 0.0;
        }
        return safeInt(stats.getScoreStartChat())
                + safeInt(stats.getScoreQuestion())
                + safeInt(stats.getScorePrivateStory())
                + safeInt(stats.getScorePositiveReaction())
                + safeInt(stats.getScoreMeetingSuccess())
                + safeInt(stats.getScoreGiveHelp());
    }

    private double sumAutoPenalties(ChatAutoStats stats) {
        if (stats == null) {
            return 0.0;
        }
        return Math.abs(safeInt(stats.getScoreNoResponse()))
                + Math.abs(safeInt(stats.getScoreMeetingRejection()))
                + Math.abs(safeInt(stats.getScoreAttack()))
                + Math.abs(safeInt(stats.getScoreGetHelp()));
    }

    private double computeTenureBonus(RelationProfile relation, Integer durationMonths) {
        if (durationMonths == null || durationMonths <= 0) {
            return 0.0;
        }
        double years = durationMonths / 12.0;
        double bonus = Math.round(years * relation.tenureUnit());
        return Math.min(bonus, relation.tenureCap());
    }

    private RelationProfile resolveRelationProfile(PersonRelation relation) {
        if (relation == null) {
            return RelationProfile.COWORKER;
        }
        return switch (relation) {
            case LOVER -> RelationProfile.LOVER;
            case FRIEND -> RelationProfile.FRIEND;
            case COWORKER -> RelationProfile.COWORKER;
        };
    }

    private WeightGrade resolveStatusGrade(PersonStatus status) {
        if (status == null) {
            return WeightGrade.MEDIUM;
        }
        return switch (status) {
            case INTEREST -> WeightGrade.HIGH;
            case MAINTAIN -> WeightGrade.MEDIUM;
            case UNINTEREST -> WeightGrade.LOW;
        };
    }

    private long roundToTenThousand(double value) {
        return Math.round(value / 10_000.0) * 10_000;
    }

    private double clampToRange(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private enum WeightGrade {
        HIGH(0.25),
        MEDIUM(0.0),
        LOW(-0.25);

        private final double weight;

        WeightGrade(double weight) {
            this.weight = weight;
        }

        public double weight() {
            return weight;
        }
    }

    private enum RelationProfile {
        LOVER(50_000_000L, 5_000_000L, 50_000_000L, 100_000_000L, WeightGrade.MEDIUM, WeightGrade.HIGH),
        FRIEND(50_000_000L, 2_500_000L, 50_000_000L, 100_000_000L, WeightGrade.MEDIUM, WeightGrade.LOW),
        COWORKER(30_000_000L, 1_500_000L, 30_000_000L, 60_000_000L, WeightGrade.HIGH, WeightGrade.HIGH);

        private final long baseValue;
        private final long tenureUnit;
        private final long tenureCap;
        private final long totalCap;
        private final WeightGrade positiveGrade;
        private final WeightGrade negativeGrade;

        RelationProfile(long baseValue,
                        long tenureUnit,
                        long tenureCap,
                        long totalCap,
                        WeightGrade positiveGrade,
                        WeightGrade negativeGrade) {
            this.baseValue = baseValue;
            this.tenureUnit = tenureUnit;
            this.tenureCap = tenureCap;
            this.totalCap = totalCap;
            this.positiveGrade = positiveGrade;
            this.negativeGrade = negativeGrade;
        }

        public long baseValue() {
            return baseValue;
        }

        public long tenureUnit() {
            return tenureUnit;
        }

        public long tenureCap() {
            return tenureCap;
        }

        public long totalCap() {
            return totalCap;
        }

        public WeightGrade positiveGrade() {
            return positiveGrade;
        }

        public WeightGrade negativeGrade() {
            return negativeGrade;
        }
    }
}