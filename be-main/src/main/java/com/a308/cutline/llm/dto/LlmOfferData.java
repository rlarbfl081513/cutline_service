package com.a308.cutline.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LlmOfferData {

    @JsonProperty("price")
    private Integer price;

    @JsonProperty("content")
    private String content;
}

