package com.a308.cutline.domain.Offer.service;

import com.a308.cutline.common.dao.CategoryRepository;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import com.a308.cutline.common.entity.Topic;
import com.a308.cutline.domain.Offer.dao.GiftRepository;
import com.a308.cutline.domain.Offer.dao.OfferRepository;
import com.a308.cutline.domain.Offer.dto.OfferRequest;
import com.a308.cutline.domain.Offer.entity.Gift;
import com.a308.cutline.domain.Offer.entity.Offer;
import com.a308.cutline.domain.cashflow.service.CashflowService;
import com.a308.cutline.domain.naverShopping.NaverShoppingItem;
import com.a308.cutline.domain.naverShopping.NaverShoppingService;
import com.a308.cutline.domain.person.dao.PersonRepository;
import com.a308.cutline.domain.person.service.TopicService;
import com.a308.cutline.llm.dto.LlmOfferData;
import com.a308.cutline.llm.service.LlmService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final GiftRepository giftRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final LlmService llmService;
    private final NaverShoppingService naverShoppingService;
    private final CashflowService cashflowService;
    private final TopicService topicService;

    // status에 따른 전략 가져오는 메서드 추가
    private String getStrategyByStatus(Person person, String status) {
        switch (status.toUpperCase()) {
            case "INTEREST":
                return person.getInterestStrategy();
            case "UNINTEREST":
                return person.getUninterestStrategy();
            case "MAINTAIN":
                return person.getMaintainStrategy();
            default:
                return "기본 전략: 일반적인 예의에 맞는 선물을 추천합니다.";
        }
    }

    private String removeHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("&apos;", "'")
                .trim();
    }

    // 1. 수정된 LLM 헬퍼 메서드: 하나의 기존 토픽으로 3개의 연관 검색어 생성
    private List<String> generateKeywordsForTopic(Person person, Category category, Integer freeCash, String existingTopicName) {
        // Person의 status 값 가져오기
        String personStatus = String.valueOf(person.getStatus()); // Person 엔티티에 status 필드가 있다고 가정

        // status에 따른 전략 가져오기
        String strategy = getStrategyByStatus(person, personStatus);

        String prompt = String.format(
                """
                - 경조사 종류: %s
                - 사용자가 입력하는 여유금액 (freeCash) : %d
                - 대상 정보: %d세 %s, 관계 %s (%d년 지속)
                - 기존 관심사: "%s"
                - 관계 상태: %s
                - 관계 상태에 따른 관계 전략: %s
                
                규칙:
                   - 주어진 정보를 바탕으로, 대상의 '기존 관심사'와 관련된 선물 아이디어를 '네이버 쇼핑 검색어'로 3개 생성해줘.
                   - 아래의 '관계 상태'별 지침에 따라 검색어의 성격을 다르게 추천해야 해.
                   - '기존 관심사'가 동물과 관련된 것이 아니라면 무조건 사람이 사용하는 물건으로 추천해줘야해. 

                   관계 상태별 검색어 생성 지침:
                   - INTEREST: 가까워지고 싶은 관계이므로, 상대방의 관심사를 더 깊이 파고들거나 개인의 특별한 취향을 저격할 수 있는 선물을 위한 검색어. (예: 관심사가 '커피'일 경우 '핸드드립 세트', '원두 그라인더')
                   - UNINTEREST: 멀어지고 싶은 관계이므로, 최소한의 성의는 보이지만 개인적인 의미는 없는 가장 대중적이고 무난한 선물을 위한 검색어. (예: 관심사가 '영화'일 경우 'CGV 영화관람권', '스타벅스 기프트카드')
                   - MAINTAIN: 현재 관계를 유지하고 싶으므로, 관심사와 관련하여 가장 인기 있고 실용적이어서 실패할 확률이 적은 아이템을 위한 검색어. (예: 관심사가 '게임'일 경우 '게이밍 마우스', '기계식 키보드')
                   
                   관계 상태별 가격대 지침:
                   - INTEREST: '여유금액'에 10~20%%를 더한 금액을 최종 추천 금액으로 제안해줘. (관계를 발전시키고 싶은 긍정적 의도 반영)
                   - UNINTEREST: '여유금액'에서 10~20%%를 뺀 금액과. (관계를 멀리하고 싶은 의도 반영)
                   - MAINTAIN: '여유금액'을 거의 그대로 최종 추천 금액으로 제안해줘. (현재 관계 유지)

                   출력 형식:
                   - 각 검색어는 1-2단어로 간단하게 작성.
                   - 1개의 검색어를 다른 설명 없이 반환.
                """,
                category.getTitle().name(),
                freeCash,
                person.getAge(),
                person.getGender().name(),
                person.getRelation().name(),
                person.getDuration(),
                existingTopicName,
                personStatus,
                strategy
        );

        String response = llmService.getSearchKeywords(prompt);

        return Arrays.stream(response.split(","))
                .map(String::trim)
                .limit(1)
                .collect(Collectors.toList());
    }


    @Transactional
    public Offer createOfferAndGiftList(OfferRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid categoryId"));

        Person person = personRepository.findById(request.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid personId"));

        Integer LatestChangePrice = cashflowService
                .getLatestChangedPrice(person.getId(), category.getId())
                .orElse(0);

        List<Topic> existingTopics = topicService.getMostCountTopicInCurrentMonth(person.getId());
        log.info("관심사 양: {}", existingTopics.size());
        String existingTopicNames = existingTopics.stream()
                .map(Topic::getTopic)
                .collect(Collectors.joining(", "));

        // 1. 모든 LLM 검색어를 미리 생성하고 Map에 저장
        Map<String, List<String>> topicToKeywordsMap = new LinkedHashMap<>();
        for (Topic existingTopic : existingTopics) {
            String originalTopicName = existingTopic.getTopic();
            List<String> llmKeywords = generateKeywordsForTopic(person, category,request.getFreeCash(), originalTopicName);
            topicToKeywordsMap.put(originalTopicName, llmKeywords);
        }

        // 2. 생성된 모든 검색어를 하나의 문자열로 만듦
        String allGeneratedKeywords = topicToKeywordsMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.joining(", "));

        // Person의 status 값 가져오기
        String personStatus = String.valueOf(person.getStatus()); // Person 엔티티에 status 필드가 있다고 가정

        // status에 따른 전략 가져오기
        String strategy = getStrategyByStatus(person, personStatus);

        // 2. 수정된 메인 로직
        // 추천 금액의 '근거' 생성을 위한 프롬프트
        String userPrompt = String.format(
                """
                사용자가 참석하는 행사와 여유 금액에 대해 입력할거야.
                
                입력 정보 :
                - 경조사 종류 (Title): %s
                - 사용자가 입력하는 여유금액 (freeCash) : %d
                
                관련 인물 정보 (참고용):
                    - 이름: %s
                    - 나이 : %d
                    - 성별: %s
                    - 관계: %s
                    - 지속기간: %d년
                    - 내가 상대방에게 도움을 준 횟수: %d
                    - 상대가 나에게 도움을 준 횟수: %d
                    - 관계 상태: %s
                    - 관계 상태에 따른 관계 전략: %s
                
                주고 받은 금액 내역 (참고용):
                    - 가장 최근 받은 내역에 물가상승률을 반영한것: %d
                
                관심사
                    - 상대방의 기존 주요 관심사: %s
                
                규칙 :
                    - 관계 상태가 INTEREST(관심 있음, 가까워지고싶음) UNINTEREST(관심 없음, 멀어지고싶음) MAINTAIN(관계 유지하기, 현재 관계를 유지하고 싶음)이런 의미를 가지고 있어.
                    - 관계 상태 :
                        - INTEREST: '여유금액'에 10~20%%를 더한 금액을 최종 추천 금액으로 제안해줘. (관계를 발전시키고 싶은 긍정적 의도 반영)
                        - UNINTEREST: '여유금액'에서 10~20%%를 뺀 금액과. (관계를 멀리하고 싶은 의도 반영)
                        - MAINTAIN: '여유금액'을 거의 그대로 최종 추천 금액으로 제안해줘. (현재 관계 유지)
        
                    - 추천금액의 필드명은 price로 integer 타입이야.
                    - 근거의 필드명은 content으로 string 타입이야.
                      근거는 인물정보와 주고 받은 금액 내역, 관심사를 참고해.
                      형식 :
                        - 소통관련 정보 근거 1줄 (인물정보에는 나이, 성별, 관계, 지속기간이 있으니 이를 자연스럽게 언급해줘)
                        - 관심사 관련 근거 1문장 (주요 관심사를 자연스럽게 언급해줘)
                        - 이전에 주고받은 금액 관련 근거 1줄 (상대방이 같은 종류의 행사 때 얼마를 나에게 사용했는지를 기반으로 작성)
                        - 근거는 총 길이 제한이 데이터베이스에서 VARCHAR(225)니까 참고해.
                      근거에 대해서는 앞에 제목 같은거 없이 문장으로 이어지게. 하지만 각 근거는 한줄씩 잘라서 작성해. 영어는 사용하지마.
                
                """,
                category.getTitle().name(),
                request.getFreeCash(),
                person.getName(),
                person.getAge(),
                person.getGender().name(),
                person.getRelation().name(),
                person.getDuration(),
                person.getTotalGive(),
                person.getTotalTake(),
                personStatus,
                strategy,
                LatestChangePrice,
                existingTopicNames,
                allGeneratedKeywords
        );

        LlmOfferData llmOfferData = llmService.getOfferData(userPrompt);

        Offer offer = new Offer(
                request.getFreeCash(),
                llmOfferData.getPrice(),
                llmOfferData.getContent(),
                person,
                category
        );

        Offer savedOffer = offerRepository.save(offer);

        // 새로운 선물 생성 로직 (이중 반복문)
        final int ITEMS_PER_KEYWORD = 1;

        // 외부 반복문: 기존 관심사 리스트를 순회
        for (Topic existingTopic : existingTopics) {
            String originalTopicName = existingTopic.getTopic();

            // LLM 호출: 현재 관심사에 대한 3개의 검색어 생성
//            List<String> llmKeywords = generateKeywordsForTopic(person, category, request.getFreeCash(), originalTopicName);
            List<String> llmKeywords = topicToKeywordsMap.get(originalTopicName);

            // 내부 반복문: 생성된 3개의 검색어를 순회
            for (String llmKeyword : llmKeywords) {
                System.out.println(String.format("=== 기존 토픽: '%s' -> LLM 검색어: '%s'로 검색 ===", originalTopicName, llmKeyword));

                List<NaverShoppingItem> topicGifts = naverShoppingService.searchProducts(
                        llmKeyword,
                        llmOfferData.getPrice(),
                        ITEMS_PER_KEYWORD
                );

                if (!topicGifts.isEmpty()) {
                    NaverShoppingItem item = topicGifts.get(0);
                    String cleanTitle = removeHtmlTags(item.getTitle());

                    Gift gift = new Gift(
                            cleanTitle.length() > 100 ? cleanTitle.substring(0, 100) : cleanTitle,
                            Integer.parseInt(item.getLprice()),
                            item.getImage(),
                            originalTopicName,
                            savedOffer,
                            item.getLink()
                    );
                    savedOffer.addGift(gift);
                }
            }
        }

        return savedOffer;
    }

    @Transactional
    public List<Gift> getLatestGiftsByPersonId(Long personId) {
        return giftRepository.findTop15ByOfferPersonIdOrderByOfferCreatedAtDesc(personId);
    }

    @Transactional
    public Offer getLatestOfferWithGiftsByPersonId(Long personId) {
        return offerRepository.findFirstByPersonIdOrderByCreatedAtDesc(personId)
                .orElseThrow(() -> new IllegalArgumentException("No offer found for person"));
    }
}