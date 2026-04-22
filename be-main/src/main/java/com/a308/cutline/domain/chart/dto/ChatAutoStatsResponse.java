package com.a308.cutline.domain.chart.dto;

import com.a308.cutline.domain.chart.entity.ChatAutoStats;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatAutoStatsResponse {

    private Long id;
    private Integer startChat;
    private Integer question;
    private Integer privateStory;
    private Integer positiveReaction;
    private Integer getHelp;
    private Integer meetingSuccess;
    private Integer noResponse;
    private Integer giveHelp;
    private Integer attack;
    private Integer meetingRejection;

    private Long personValueId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatAutoStatsResponse from(ChatAutoStats e) {
        if (e == null) return null;
        return new ChatAutoStatsResponse(
                e.getId(),
                e.getStartChat(),
                e.getQuestion(),
                e.getPrivateStory(),
                e.getPositiveReaction(),
                e.getGetHelp(),
                e.getMeetingSuccess(),
                e.getNoResponse(),
                e.getGiveHelp(),
                e.getAttack(),
                e.getMeetingRejection(),
                e.getPersonValueId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
