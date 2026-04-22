package com.a308.cutline.domain.familiy_event.dto;

import com.a308.cutline.common.entity.Title;
import com.a308.cutline.common.entity.Type;
import com.a308.cutline.domain.familiy_event.entity.FamilyEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FamilyEventResponse {
    
    private Long id;
    private Long personId;
    private Integer cost;
    private Boolean attendance;
    private Integer price;
    private String content;
    private CategoryResponse category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Getter
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private Title title;
        private Type type;
    }
    
    public static FamilyEventResponse from(FamilyEvent familyEvent) {
        CategoryResponse categoryResponse = null;
        if (familyEvent.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                familyEvent.getCategory().getId(),
                familyEvent.getCategory().getTitle(),
                familyEvent.getCategory().getType()
            );
        }
        
        return new FamilyEventResponse(
            familyEvent.getId(),
            familyEvent.getPerson().getId(),
            familyEvent.getCost(),
            familyEvent.getAttendance(),
            familyEvent.getPrice(),
            familyEvent.getContent(),
            categoryResponse,
            familyEvent.getCreatedAt(),
            familyEvent.getUpdatedAt()
        );
    }
}