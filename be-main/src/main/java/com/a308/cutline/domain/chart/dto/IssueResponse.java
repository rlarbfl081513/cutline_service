package com.a308.cutline.domain.chart.dto;

import com.a308.cutline.domain.chart.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class IssueResponse {

    private Long id;
    private String content;
    private Integer year;
    private Integer month;
    private Long personId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IssueResponse from(Issue e) {
        if (e == null) return null;
        return new IssueResponse(
                e.getId(),
                e.getContent(),
                e.getYear(),
                e.getMonth(),
                e.getPersonId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
