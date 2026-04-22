package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.ChatAutoStats;
import com.ssafya408.cutlineparsing.service.dto.AutoStatsPayload;
import org.springframework.stereotype.Component;

@Component
public class AutoStatsCalculator {

    private static final int START_CHAT_UNIT = 500_000;
    private static final int QUESTION_UNIT = 50_000;
    private static final int PRIVATE_STORY_UNIT = 600_000;
    private static final int POSITIVE_REACTION_UNIT = 100_000;
    private static final int MEETING_SUCCESS_UNIT = 1_000_000;
    private static final int GIVE_HELP_UNIT = 1_000_000;
    private static final int MEETING_REJECTION_UNIT = -1_000_000;
    private static final int ATTACK_UNIT = -5_000_000;
    private static final int NO_RESPONSE_UNIT = -1_000_000;
    private static final int GET_HELP_UNIT = -500_000;

    private static final int START_CHAT_CAP = 5_000_000;
    private static final int QUESTION_CAP = 2_000_000;
    private static final int PRIVATE_STORY_CAP = 3_000_000;
    private static final int POSITIVE_REACTION_CAP = 2_000_000;
    private static final int MEETING_SUCCESS_CAP = 5_000_000;
    private static final int GIVE_HELP_CAP = 3_000_000;
    private static final int MEETING_REJECTION_CAP = -3_000_000;
    private static final int ATTACK_CAP = -10_000_000;
    private static final int NO_RESPONSE_CAP = -5_000_000;
    private static final int GET_HELP_CAP = -2_000_000;

    public ChatAutoStats calculate(AutoStatsPayload payload) {
        if (payload == null) {
            return new ChatAutoStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int startChatCount = safeInt(payload.startChat());
        int questionCount = safeInt(payload.question());
        int privateStoryCount = safeInt(payload.privateStory());
        int positiveReactionCount = safeInt(payload.positiveReaction());
        int getHelpCount = safeInt(payload.getHelp());
        int meetingSuccessCount = safeInt(payload.meetingSuccess());
        int noResponseCount = safeInt(payload.noResponse());
        int giveHelpCount = safeInt(payload.giveHelp());
        int attackCount = safeInt(payload.attack());
        int meetingRejectionCount = safeInt(payload.meetingRejection());

        int startChatScore = capPositive(startChatCount, START_CHAT_UNIT, START_CHAT_CAP);
        int questionScore = capPositive(questionCount, QUESTION_UNIT, QUESTION_CAP);
        int privateStoryScore = capPositive(privateStoryCount, PRIVATE_STORY_UNIT, PRIVATE_STORY_CAP);
        int positiveReactionScore = capPositive(positiveReactionCount, POSITIVE_REACTION_UNIT, POSITIVE_REACTION_CAP);
        int meetingSuccessScore = capPositive(meetingSuccessCount, MEETING_SUCCESS_UNIT, MEETING_SUCCESS_CAP);
        int giveHelpScore = capPositive(giveHelpCount, GIVE_HELP_UNIT, GIVE_HELP_CAP);

        int noResponseScore = capNegative(noResponseCount, NO_RESPONSE_UNIT, NO_RESPONSE_CAP);
        int meetingRejectionScore = capNegative(meetingRejectionCount, MEETING_REJECTION_UNIT, MEETING_REJECTION_CAP);
        int attackScore = capNegative(attackCount, ATTACK_UNIT, ATTACK_CAP);
        int getHelpScore = capNegative(getHelpCount, GET_HELP_UNIT, GET_HELP_CAP);

        return new ChatAutoStats(
                startChatCount,
                questionCount,
                privateStoryCount,
                positiveReactionCount,
                getHelpCount,
                meetingSuccessCount,
                noResponseCount,
                giveHelpCount,
                attackCount,
                meetingRejectionCount,
                startChatScore,
                questionScore,
                privateStoryScore,
                positiveReactionScore,
                meetingSuccessScore,
                giveHelpScore,
                noResponseScore,
                meetingRejectionScore,
                attackScore,
                getHelpScore
        );
    }

    private int safeInt(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private int capPositive(int count, int unit, int cap) {
        long raw = (long) count * unit;
        return (int) Math.min(raw, cap);
    }

    private int capNegative(int count, int unit, int cap) {
        long raw = (long) count * unit;
        if (cap >= 0) {
            return (int) Math.min(raw, cap);
        }
        return (int) Math.max(raw, cap);
    }
}
