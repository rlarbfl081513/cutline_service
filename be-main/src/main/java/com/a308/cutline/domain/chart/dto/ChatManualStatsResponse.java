package com.a308.cutline.domain.chart.dto;

import com.a308.cutline.domain.chart.entity.ChatManualStats;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatManualStatsResponse {

    private Long id;
    private Integer monthVolume;
    private Integer monthCount;
    private Integer responseAverage;
    private Integer chatDays;
    private Integer silentDays;
    private Long personValueId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatManualStatsResponse from(ChatManualStats e) {
        if (e == null) return null;
        return new ChatManualStatsResponse(
                e.getId(),
                e.getMonthVolume(),
                e.getMonthCount(),
                e.getResponseAverage(),
                e.getChatDays(),
                e.getSilentDays(),
                e.getPersonValueId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
