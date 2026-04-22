package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.common.entity.ChatManualStats;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonRelation;
import com.ssafya408.cutlineparsing.common.entity.PersonStatus;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.service.RelationshipValueCalculator;
import com.ssafya408.cutlineparsing.service.RelationshipValueResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipValueCalculatorTest {

    private static final double EPSILON = 1e-9;
    private final RelationshipValueCalculator calculator = new RelationshipValueCalculator();

    private Person samplePerson(PersonRelation relation, PersonStatus status, int durationMonths) {
        return new Person(new User("sample@example.com", "샘플", LocalDate.of(1990, 1, 1), null),
                "샘플", LocalDate.of(1990, 1, 1), null, status, relation, durationMonths);
    }

    private ChatManualStats manual(int bonusChars, int bonusMessages, int bonusDays,
                                   int penaltyResponse, int penaltySilent) {
        return new ChatManualStats(0, 0, 0, 0, 0,
                bonusChars, bonusMessages, bonusDays,
                penaltyResponse, penaltySilent);
    }

    private ChatAutoStats auto(int startChat, int question, int privateStory, int positiveReaction,
                               int getHelp, int meetingSuccess, int noResponse, int giveHelp,
                               int attack, int meetingRejection,
                               int scoreStartChat, int scoreQuestion, int scorePrivateStory,
                               int scorePositiveReaction, int scoreMeetingSuccess, int scoreGiveHelp,
                               int scoreNoResponse, int scoreMeetingRejection, int scoreAttack, int scoreGetHelp) {
        return new ChatAutoStats(startChat, question, privateStory, positiveReaction,
                getHelp, meetingSuccess, noResponse, giveHelp, attack, meetingRejection,
                scoreStartChat, scoreQuestion, scorePrivateStory, scorePositiveReaction,
                scoreMeetingSuccess, scoreGiveHelp, scoreNoResponse, scoreMeetingRejection, scoreAttack, scoreGetHelp);
    }

    @Test
    @DisplayName("첫 달 계산: 연애 관계 + 긍정 시그널 → 정규화 후 승수 적용")
    void calculate_firstMonth_withPositiveSignals() {
        Person person = samplePerson(PersonRelation.LOVER, PersonStatus.INTEREST, 24);
        ChatManualStats manual = manual(1_000_000, 500_000, 200_000, 0, 0);
        ChatAutoStats auto = auto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1_000_000, 0, 0, 0, 0, 0,
                0, 0, 0, 0);

        RelationshipValueResult result = calculator.calculate(person, null, manual, auto);

        // base = 60,000,000 (연애 base 50M + 2년 보너스 10M)
        // blended positive ≈ 0.11 → scaled 2,200,000 → 승수(1.25) 반영 2,750,000
        // current = 60,000,000 + 2,750,000 = 62,750,000 (10,000 단위 반올림 동일)
        assertEquals(62_750_000, result.currentValue());
        assertEquals((62_750_000 - 60_000_000) / 60_000_000.0, result.changeRate(), EPSILON);
        assertEquals(2_750_000, result.weightedDelta());
        assertEquals(2_750_000, result.positiveContribution());
        assertEquals(0, result.negativeContribution());
    }

    @Test
    @DisplayName("기존 값이 있을 때 부정 시그널 → 감소 및 변화율 계산")
    void calculate_withPreviousValue_andNegativeSignals() {
        Person person = samplePerson(PersonRelation.FRIEND, PersonStatus.UNINTEREST, 60);
        ChatManualStats manual = manual(0, 0, 0, 500_000, 200_000);
        ChatAutoStats auto = auto(0, 0, 0, 0, 0, 0, 2, 0, 0, 1,
                0, 0, 0, 0, 0, 0,
                -200_000, -100_000, -5_000_000, 0);

        PersonValue previous = new PersonValue(person, 80_000_000, 2025, 7, "", 0.0);

        RelationshipValueResult result = calculator.calculate(person, previous, manual, auto);

        // blended penalty ≈ 0.1675 → scaled 3,350,000 → 승수(1.0) 반영 → delta -3,350,000
        // current = 80,000,000 - 3,350,000 = 76,650,000
        assertEquals(76_650_000, result.currentValue());
        assertEquals((76_650_000 - 80_000_000) / 80_000_000.0, result.changeRate(), EPSILON);
        assertEquals(-3_350_000, result.weightedDelta());
        assertEquals(0, result.positiveContribution());
        assertEquals(3_350_000, result.negativeContribution());
    }

    @Test
    @DisplayName("관계/상태 미지정 시 기본 프로필 적용")
    void calculate_defaultRelation() {
        Person person = samplePerson(null, PersonStatus.MAINTAIN, 12);
        ChatManualStats manual = manual(0, 0, 0, 0, 0);
        ChatAutoStats auto = auto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0);

        RelationshipValueResult result = calculator.calculate(person, null, manual, auto);

        assertEquals(31_500_000, result.currentValue());
    }
}
