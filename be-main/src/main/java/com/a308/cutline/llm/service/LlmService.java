// src/main/java/com/a308/cutline.llm/OpenAIService.java (Spring AI 버전)

package com.a308.cutline.llm.service;

import com.a308.cutline.llm.dto.LlmFamilyEventData;
import com.a308.cutline.llm.dto.LlmGiftData;
import com.a308.cutline.llm.dto.LlmOfferData;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient; // 👈 Spring AI의 ChatClient
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱용

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final ChatClient chatClient; //
    private final ObjectMapper objectMapper; // JSON 파싱 도구

    // 유저가 이동거리와 이벤트에 대한 입력을 하면 llm이 정보를 작성해줌
    public LlmFamilyEventData getFamilyEventData(String userPrompt) {

        // 1. LLM에게 역할과 응답 형식을 정의하는 템플릿 작성
        String systemPrompt = """
                
                너는 경조사 관련 정보를 추천해주는 전문가야.
                다음 사용자 정보를 기반으로 정확히 이 형식의 JSON만 반환해.
                예시는 이거야. {"attendance": true, "price": 50000, "content": "친밀도가 높아 참석하는 것이 좋습니다."}

                규칙:
                - 참석 여부 : attendance 반드시 true 또는 false (boolean)으로 문자열 아니야
                - 추천 금액 : price는 반드시 숫자 (integer) 
                - 근거 : content는 반드시 문자열 (string), 참석여부와 추천금액을 왜 그렇게 정했는지에 대한 근거를 말하는 거야.
                - 다른 텍스트나 설명 없이 JSON만 반환
                - 필드명은 정확히 attendance, price, content 사용

         
                """;

        // 3. ChatClient를 이용한 API 호출
        String jsonResponse = chatClient.prompt()
                .system(systemPrompt) // 시스템 메시지는 ChatClient의 메서드로 분리
                .user(userPrompt)
                .call()
                .content(); // LLM 응답 (JSON 문자열)을 바로 받아옵니다.

        // 4. 응답 파싱
        try {
            return objectMapper.readValue(jsonResponse, LlmFamilyEventData.class);
        } catch (Exception e) {
            throw new RuntimeException("fail LLM JSON 응답 파싱 실패: " + jsonResponse, e);
        }
    }

    public String getSearchKeywords(String prompt) {
        try {
            String systemPrompt = """
                너는 선물 검색에 적합한 검색어를 생성하는 전문가야.
                주어진 정보를 바탕으로 네이버 쇼핑 검색에 적합한 검색어 3개를 생성해줘.
                검색어는 쉼표로 구분해서 반환해줘.
                JSON 형식이 아닌 일반 텍스트로만 응답해줘.
                """;

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new RuntimeException("LLM 검색어 생성 실패: " + e.getMessage());
        }
    }

    // 선물 리스트 생성
    public LlmOfferData getOfferData(String userPrompt) {

        // llm에게 전달한 것
        String systemPrompt = """
                너는 선물 추천해주는 사람이야.
                name은 길이가 length=100 제한이야.
                모든 키는 큰따옴표로 감싸야 해, 반드시 중괄호로 시작하고 끝나야 해
                유효한 json형식으로 보내줘야해.
                """;

        String jsonResponse = chatClient.prompt()
                .system(systemPrompt) // 시스템 메시지는 ChatClient의 메서드로 분리
                .user(userPrompt)
                .call()
                .content(); // LLM 응답 (JSON 문자열)을 바로 받아옵니다.

        // 응답 파싱
        try {
            return objectMapper.readValue(jsonResponse, LlmOfferData.class);
        } catch (Exception e) {
            throw new RuntimeException("fail LLM JSON 응답 파싱 실패: " + jsonResponse, e);
        }


    }

    public List<LlmGiftData> getGiftList(String userPrompt){
        String systemPrompt = """
                선물 리스트를 다음 JSON 형식과 길이로 응답해.
                name은 길이가 length=100 제한이야.
                사용자에게 입력받은 정보들을 기반으로 상품을 선택해서 작성해줘.

                [
                    {"name": "선물1", "price": 20000, "link": "링크1", "imageUrl": "이미지1", "topic": "주제1"},
                    {"name": "선물2", "price": 30000, "link": "링크2", "imageUrl": "이미지2", "topic": "주제2"},
                    {"name": "선물2", "price": 30000, "link": "링크2", "imageUrl": "이미지2", "topic": "주제2"},
                    {"name": "선물2", "price": 30000, "link": "링크2", "imageUrl": "이미지2", "topic": "주제2"},
                    {"name": "선물2", "price": 30000, "link": "링크2", "imageUrl": "이미지2", "topic": "주제2"}
                ]
                """;

        String jsonResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        try {
            return objectMapper.readValue(jsonResponse,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LlmGiftData.class));
        } catch (Exception e) {
            throw new RuntimeException("선물 리스트 파싱 실패: " + jsonResponse, e);
        }
    }
}