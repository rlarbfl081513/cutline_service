package com.ssafya408.cutlineparsing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempDto {
    
    private Long id;
    private String name;
    private String description;
    private String category;
    
    public static TempDto of(Long id, String name, String description, String category) {
        return TempDto.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .build();
    }
}
