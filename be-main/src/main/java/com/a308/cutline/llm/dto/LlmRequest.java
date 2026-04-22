package com.a308.cutline.llm.dto;

import com.a308.cutline.llm.LlmMessage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LlmRequest {

    private String model;
    private List<LlmMessage> llmMessages;

    // 응답을 json 형식으로 받기위한 설정
    private LlmResponseFormat llmResponseFormat;

    // json 응답 형식을 위한 내부 클래스
    @Getter
    @Builder
    public static class LlmResponseFormat{
        private final String type = "json_object";
    }
}
