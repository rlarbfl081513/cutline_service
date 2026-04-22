package com.a308.cutline.domain.person.service;

import com.a308.cutline.common.entity.Person;
import com.a308.cutline.domain.person.dao.PersonRepository;
import com.a308.cutline.domain.person.dto.DashBoardResponse;
import com.a308.cutline.domain.person.dto.PersonResponse;
import com.a308.cutline.domain.personvalue.dao.PersonValueRepository;
import com.a308.cutline.domain.personvalue.dto.PersonValueResponse;
import com.a308.cutline.domain.familiy_event.dao.FamilyEventRepository;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventResponse;
import com.a308.cutline.domain.familiy_event.entity.FamilyEvent;
import com.a308.cutline.domain.Offer.dao.OfferRepository;
import com.a308.cutline.domain.Offer.dto.OfferResponse;
import com.a308.cutline.domain.Offer.entity.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashBoardService {
    
    private final PersonRepository personRepository;
    private final PersonValueRepository personValueRepository;
    private final FamilyEventRepository familyEventRepository;
    private final OfferRepository offerRepository;
    
    public DashBoardResponse getDashBoard(Long userId, Long personId) {
        // 1. Person 정보 조회 및 권한 확인
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 인물입니다."));
        
        // 해당 Person이 요청한 User의 소유인지 확인
        if (!person.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        PersonResponse personResponse = PersonResponse.from(person);
        
        // 2. PersonValue 최신 12개 조회
        List<PersonValueResponse> personValueResponses = personValueRepository.findLast12ByPersonId(personId)
            .stream()
            .map(PersonValueResponse::from)
            .toList();
        
        // 3. 최신 FamilyEvent 조회
        FamilyEventResponse latestFamilyEvent = familyEventRepository.findLatestByPersonId(personId)
            .map(FamilyEventResponse::from)
            .orElse(null);
        
        // 4. 최신 Offer 조회 (Gift 포함)
        OfferResponse latestOffer = offerRepository.findFirstByPersonIdOrderByCreatedAtDesc(personId)
            .map(OfferResponse::from)
            .orElse(null);
        
        return new DashBoardResponse(
            personResponse,
            personValueResponses,
            latestFamilyEvent,
            latestOffer
        );
    }
}
