package com.a308.cutline.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LlmGiftData{
    @JsonProperty("name")
    private String name;

    @JsonProperty("price")
    private Integer price;

    @JsonProperty("link")
    private String link;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("topic")
    private String topic;


}
