package com.a308.cutline.domain.personvalue.dto;

import com.a308.cutline.domain.personvalue.entity.PersonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PersonValueResponse {
    
    private Long id;
    private Long personId;
    private Integer value;
    private Integer year;
    private Integer month;
    private String feedback;
    private Float changeRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PersonValueResponse from(PersonValue personValue) {
        return new PersonValueResponse(
            personValue.getId(),
            personValue.getPerson().getId(),
            personValue.getValue(),
            personValue.getYear(),
            personValue.getMonth(),
            personValue.getFeedback(),
            personValue.getChangeRate(),
            personValue.getCreatedAt(),
            personValue.getUpdatedAt()
        );
    }
}
