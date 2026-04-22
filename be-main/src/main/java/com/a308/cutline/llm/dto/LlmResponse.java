package com.a308.cutline.llm.dto;

import com.a308.cutline.llm.LlmMessage;
import lombok.Getter;

import java.util.List;

@Getter
public class LlmResponse {

    private List<Choice> choices;

    @Getter
    public static class Choice{
        private LlmMessage message;
    }

}
