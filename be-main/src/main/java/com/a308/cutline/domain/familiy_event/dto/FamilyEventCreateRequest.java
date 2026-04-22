package com.a308.cutline.domain.familiy_event.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FamilyEventCreateRequest {
    private Integer cost;
    private Long categoryId;
    private Long personId;
}
