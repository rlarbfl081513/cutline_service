package com.ssafya408.cutlineparsing.service.dto;

public record AutoStatsPayload(
        Integer startChat,
        Integer question,
        Integer privateStory,
        Integer positiveReaction,
        Integer getHelp,
        Integer meetingSuccess,
        Integer noResponse,
        Integer giveHelp,
        Integer attack,
        Integer meetingRejection
) {
    public static AutoStatsPayload empty() {
        return new AutoStatsPayload(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
