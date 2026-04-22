package com.a308.cutline.domain.familiy_event.service;

import com.a308.cutline.common.dao.CategoryRepository;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import com.a308.cutline.domain.cashflow.dao.CashflowRepository;
import com.a308.cutline.domain.cashflow.entity.Cashflow;
import com.a308.cutline.domain.cashflow.service.CashflowService;
import com.a308.cutline.domain.familiy_event.dao.FamilyEventRepository;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventCreateRequest;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventResponse;
import com.a308.cutline.domain.familiy_event.entity.FamilyEvent;
import com.a308.cutline.domain.person.dao.PersonRepository;
import com.a308.cutline.llm.service.LlmService;
import com.a308.cutline.llm.dto.LlmFamilyEventData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyEventService {

    private final FamilyEventRepository familyEventRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final CashflowService cashflowService;
    private final CashflowRepository cashflowRepository;
    private final LlmService llmService;

    // create 기능
    @Transactional // DB 작업에 트랜잭션 적용
    public FamilyEvent createFamilyEvent(FamilyEventCreateRequest request) {

        // 1. DTO의 ID로 Category 엔티티 조회 (외래키 처리)
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid categoryId"));

        // 2. DTO의 ID로 Person 엔티티 조회 (외래키 처리)
        Person person = personRepository.findById(request.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid personId"));

        Integer LatestChangePrice = cashflowService
                .getLatestChangedPrice(person.getId(), category.getId())
                .orElse(0);

        // llm에 전달항 사용자 입력 프롬르트 생성
        String userPrompt = String.format(
                """
                다음 정보를 바탕으로 참석 여부와 추천 금액, 근거를 JSON 형식으로 추천해줘.
               
                사용자 입력 정보:
                     - 경조사 종류 (Title): %s
                    - 카테고리 타입 (Type): %s
                    - 사용자가 입력한 이동시간: %d
                
                    관련 인물 정보 (참고용):
                    - 이름: %s
                    - 나이 : %d
                    - 성별: %s
                    - 관계: %s
                    - 지속기간: %d년
                    - 내가 상대방에게 도움을 준 횟수: %d원
                    - 상대가 나에게 도움을 준 횟수: %d원
                
                    주고 받은 금액 내역 (참고용):
                    - 가장 최근 받은 내역에 물가상승률을 반영한것: %d
                    
                
                    주고 받은 금액 내역을 참고해서 추천금액을 결정해줘.
                    위 인물 정보와 금액내역을 참고하여 사용자가 입력한 비용과 카테고리 정보를 바탕으로 참석 여부와 추천 금액, 근거를 작성해줘. 입력받은 이동시간은 분 단위야.
                   
                    형식:
                        - 교류기간 관련 근거 1줄 (인물정보에는 나이, 성별, 관계, 지속기간이 있으니 이를 언급하면서 금액 결정에 대한 근거 작성해줘)
                        - 소통빈도 관련 근거 1줄
                        - 이동시간 관련 근거 1줄
                        - 이전에 주고받은 금액 관련 근거 1줄 (상대방이 같은 종류의 행사 때 얼마를 나에게 사용했는지를 기반으로 작성)
                        - 근거는 총 길이 제한이 데이터베이스에서 VARCHAR(225)니까 참고해.
                    근거에 대해서는 앞에 제목 같은거 없이 문장으로 이어지게. 하지만 각 근거는 한줄씩 잘라서 작성해.
                
                
                규칙:
                - 참석 여부 : attendance는 타입이 boolean이야.  반드시 true(참석) 또는 false(불참석)로 값을 줘(문자열 아님). 
                - 추천 금액 : price는 반드시 숫자 (integer) 
                - 근거 : content는 반드시 문자열 (string), 참석여부와 추천금액을 왜 그렇게 정했는지에 대한 근거를 말하는 거야. 사용자가 입력한 이동시간(분 단위)을 고려해서 근거를 작성해줘. 그리고 참석여부에 대한 것도 언급해줘.이  
                - 다른 텍스트나 설명 없이 JSON만 반환
                - 필드명은 정확히 attendance, price, content 사용
                
                - 전체 JSON 예시는  { "cost": 20,
                            "categoryId": 1,
                            "price": null,
                            "content": null,
                            "createdAt": "2025-09-18T08:48:48.141542",
                            "updatedAt": "2025-09-18T08:48:48.141542",
                            "id": 25,
                            "attendance": null }
                   이거야. 너는 여기서 price, content, attendance 대한 값을 구해주는 거야.
                
               
                """,
                category.getTitle().name(),     // 1번째: String
                category.getType().name(),      // 2번째: String
                request.getCost(),              // 3번째: Integer (%d)
                person.getName(),               // 4번째: String
                person.getAge(),
                person.getGender().name(),      // 6번째: String
                person.getRelation().name(),    // 7번째: String
                person.getDuration(),           // 8번째: Integer (%d)
                person.getTotalGive(),          // 9번째: Integer (%d)
                person.getTotalTake(),
                LatestChangePrice

        );

        // llm 호출 및 응답 데이터 객체로 받기
        LlmFamilyEventData llmFamilyEventData = llmService.getFamilyEventData(userPrompt);

        // 엔티티 생성자에게 llm이 채워준 값을 전달
        FamilyEvent familyEvent = new FamilyEvent(
                request.getCost(),
                llmFamilyEventData.getAttendance(),
                llmFamilyEventData.getPrice(),
                llmFamilyEventData.getContent(),
                person,
                category
        );
        // 4. JPA 리포지토리 인스턴스를 사용해 저장합니다. (static 호출 오류 수정)
        return familyEventRepository.save(familyEvent);
    }

    // 모든 FamiliyEvent 전체 조회 기능 (READ)
    @Transactional
    public List<FamilyEventResponse> getAllFamilyEvents(Long personId) {
        List<FamilyEvent> familyEvents = familyEventRepository.findByPersonId(personId);

        return familyEvents.stream()
                .map(FamilyEventResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FamilyEventResponse getFamilyEvent(Long id){
        FamilyEvent familyEvent = familyEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FamiliyEvent not found with ID"));

        return FamilyEventResponse.from(familyEvent);
    }

}
