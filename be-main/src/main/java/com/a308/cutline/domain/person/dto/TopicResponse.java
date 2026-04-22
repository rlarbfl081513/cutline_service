// src/main/java/com/a308/cutline/domain/person/dto/TopicResponse.java
package com.a308.cutline.domain.person.dto;

import com.a308.cutline.common.entity.Topic; // ← Topic 엔티티 패키지에 맞춰 변경
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TopicResponse {
    private Long id;
    private String topic;
    private Integer year;
    private Integer month;
    private Integer count;
    private Long personId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TopicResponse from(Topic t) {
        if (t == null) return null;
        return new TopicResponse(
                t.getId(),
                t.getTopic(),
                t.getYear(),
                t.getMonth(),
                t.getCount(),                                  // 없으면 제거
                (t.getPerson() == null ? null : t.getPerson().getId()),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
