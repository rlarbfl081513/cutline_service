package com.a308.cutline.domain.person.dto;

import com.a308.cutline.domain.chart.dto.ChatManualStatsResponse;
import com.a308.cutline.domain.person.dto.TopicResponse; // ← 패키지 다르면 import만 맞춰줘요
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PersonDetailResponse {

    private PersonResponse person;                        // 기본 인적사항
    private ChatManualStatsResponse latestManualStats;    // 최신 ChatManualStats 1건
    private List<TopicResponse> topics;                   // 전체 Topic 리스트

    public static PersonDetailResponse of(
            PersonResponse person,
            ChatManualStatsResponse latestManualStats,
            List<TopicResponse> topics
    ) {
        return new PersonDetailResponse(person, latestManualStats, topics);
    }
}
